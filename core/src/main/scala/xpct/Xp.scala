package xpct

import scala.concurrent.duration.FiniteDuration

import cats.{Applicative, Monad, MonadError, StackSafeMonad, ~>}
import cats.arrow.FunctionK
import cats.data.{EitherT, NonEmptyList, WriterT}
import cats.effect.Timer
import cats.implicits._

case class XpSuccess(message: String)

sealed trait XpFailure

object XpFailure
{
  case class Assert(message: String)
  extends XpFailure

  case class Fatal(exception: Throwable)
  extends XpFailure

  case object NoAsserts
  extends XpFailure

  object NonFatal
  {
    def unapply(a: XpFailure): Option[XpFailure] =
      a match {
        case Fatal(_) => None
        case _ => Some(a)
      }
  }
}

sealed trait AssertResult[A]

object AssertResult
{
  case class Success[A](a: A, message: XpSuccess)
  extends AssertResult[A]

  case class Failure[A](failure: XpFailure)
  extends AssertResult[A]

  def success[A](message: String)(a: A): AssertResult[A] =
    Success(a, XpSuccess(message))

  def failure[A](message: String): AssertResult[A] =
    Failure(XpFailure.Assert(message))
}

sealed trait Xp[F[_], A]

object Xp
extends XpInstances
with XpFunctions
{
  type XpW[F[_], A] = WriterT[F, List[XpSuccess], A]
  type XpM[F[_], A] = EitherT[XpW[F, ?], XpFailure, A]

  case class Assert[F[_], A, B](thunk: F[A], assert: A => AssertResult[B])
  extends Xp[F, B]

  case class Attempt[F[_], A](inner: Xp[F, A])
  extends Xp[F, A]

  case class Retry[F[_], A](inner: Xp[F, A], times: Int, interval: Option[(FiniteDuration, Timer[F])])
  extends Xp[F, A]

  case class Pure[F[_], A](a: A)
  extends Xp[F, A]

  case class Thunk[F[_], A](fa: F[A])
  extends Xp[F, A]

  case class FlatMapped[F[_], A, B](head: Xp[F, A], tail: A => Xp[F, B])
  extends Xp[F, B]
}

private[xpct] trait XpFunctions
{
  def assert[F[_], Predicate[_], Target, Subject, Output]
  (fa: F[Subject], pred: Predicate[Target])
  (implicit m: Match[Predicate, Target, Subject, Output]): Xp[F, Output] =
    Xp.Assert(fa, (a: Subject) => m(a, pred))

  def attempt[F[_], A](inner: Xp[F, A]): Xp[F, A] =
    Xp.Attempt(inner)

  def retry[F[_], A](times: Int)(inner: Xp[F, A]): Xp[F, A] =
    Xp.Retry(inner, times, None)

  def retryEvery[F[_]: Timer, A](interval: FiniteDuration)(times: Int)(inner: Xp[F, A]): Xp[F, A] =
    Xp.Retry(inner, times, Some((interval, Timer[F])))

  def suspend[F[_], A](fa: F[A]): Xp[F, A] =
    Xp.Thunk(fa)

  def liftW[F[_]: Applicative, A](fa: Xp.XpW[F, A]): Xp.XpM[F, A] =
    EitherT.liftF(fa)

  def liftF[F[_]: Applicative, A](fa: F[A]): Xp.XpM[F, A] =
    liftW(WriterT.liftF(fa))
}

private[xpct] trait XpInstances
{
  implicit def Monad_Xp[F[_]]: Monad[Xp[F, ?]] =
    new StackSafeMonad[Xp[F, ?]] {
      def flatMap[A, B](fa: Xp[F, A])(f: A => Xp[F, B]): Xp[F, B] =
        Xp.FlatMapped(fa, f)
      def pure[A](a: A): Xp[F, A] =
        Xp.Pure(a)
    }
}

object CompileXp
{
  def assert[F[_]: MonadError[*[_], Throwable], A, B](thunk: F[A], f: A => AssertResult[B])
  : Xp.XpM[F, B] =
    Xp.liftF(thunk.map(f).attempt)
      .flatMap {
        case Right(AssertResult.Success(a, message)) =>
          Xp.liftW[F, Unit](WriterT.tell(List(message))).as(a)
        case Right(AssertResult.Failure(failure)) =>
          EitherT.leftT(failure)
        case Left(exception) =>
          EitherT.leftT(XpFailure.Fatal(exception))
      }

  def retry[F[_]: MonadError[*[_], Throwable], A]
  (xp: Xp[F, A], limit: Int, interval: Option[(FiniteDuration, Timer[F])])
  : Xp.XpM[F, A] = {
    val sleep: Xp.XpM[F, Unit] =
      Xp.liftF(interval.traverse_ { case (i, timer) => timer.sleep(i) })
    def loop(iteration: Int): Xp.XpM[F, A] = {
      CompileXp(xp).recoverWith {
        case XpFailure.NonFatal(_) if iteration < limit =>
          sleep *> loop(iteration + 1)
      }
    }
    loop(0)
  }

  def attempt[A, F[_]: MonadError[*[_], Throwable]]
  (xp: Xp[F, A])
  : Xp.XpM[F, A] =
    CompileXp(xp).recoverWith {
      case XpFailure.Fatal(exception) =>
        EitherT.leftT(XpFailure.Assert(s"recovered $exception"))
    }

  def compiler[F[_]: MonadError[*[_], Throwable]]
  : Xp[F, ?] ~> Xp.XpM[F, ?] =
    new FunctionK[Xp[F, ?], Xp.XpM[F, ?]] {
      def apply[A](fa: Xp[F, A]): Xp.XpM[F, A] =
        fa match {
          case Xp.Assert(thunk, f) =>
            assert(thunk, f)
          case Xp.Retry(inner, times, interval) =>
            retry(inner, times, interval)
          case Xp.Attempt(inner) =>
            attempt(inner)
          case Xp.Pure(a) =>
            EitherT.pure(a)
          case Xp.Thunk(fa) =>
            Xp.liftF(fa)
          case Xp.FlatMapped(fa, f) =>
            apply(fa).flatMap(a => apply(f(a)))
        }
    }

  def apply[F[_]: MonadError[*[_], Throwable], A](xp: Xp[F, A]): Xp.XpM[F, A] =
    compiler[F].apply(xp)
}

sealed trait XpResult

object XpResult
{
  case class Success(results: NonEmptyList[XpSuccess])
  extends XpResult

  case class Failure(results: List[XpSuccess], failure: XpFailure)
  extends XpResult
}

object RunXp
{
  def apply[F[_]: MonadError[*[_], Throwable], A](fa: Xp[F, A])
  : F[XpResult] =
    CompileXp[F, A](fa).value.run.map {
      case (resultHead :: resultTail, Right(_)) => XpResult.Success(NonEmptyList(resultHead, resultTail))
      case (Nil, Right(_)) => XpResult.Failure(Nil, XpFailure.NoAsserts)
      case (results, Left(failure)) => XpResult.Failure(results, failure)
    }
}
