package xpct

import scala.concurrent.duration.{FiniteDuration, Duration}
import scala.concurrent.{Future, Await, ExecutionContext}

import cats.kernel.Comparison
import cats.{Functor, FlatMap, Applicative, Monad, Id, Eq, Order, Foldable, MonadError, ApplicativeError}
import cats.syntax.either._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.applicativeError._

import cats.effect.IO

import simulacrum.typeclass

trait Match[A, G[_], B, C]
{
  def apply(a: A, fb: G[B]): Either[String, C]
}

object Match
{
  case class IsAny[A]()

  object IsAny
  {
    implicit def Match_IsAny[A]: Match[A, IsAny, A, A] =
      new Match[A, IsAny, A, A] {
        def apply(a: A, fb: IsAny[A]): Either[String, A] = {
          Right(a)
        }
      }
  }

  case class Not[A](value: A)

  object Not
  {
    implicit def Match_Not[A, G[_], B, C]
    (implicit nested: Match[A, G, B, C])
    : Match[A, Not, G[B], A] =
      new Match[A, Not, G[B], A] {
        def apply(a: A, fb: Not[G[B]]): Either[String, A] = {
          nested(a, fb.value) match {
            case Right(b) => Left(s"$a matched $b in $fb, should have failed")
            case Left(_) => Right(a)
          }
        }
      }
  }

  case class Equals[A](value: A)

  object Equals
  {
    implicit def Match_Equals[A]: Match[A, Equals, A, A] =
      new Match[A, Equals, A, A] {
        def apply(a: A, fb: Equals[A]): Either[String, A] = {
          val b = fb.value
          if (a == b) Right(b)
          else Left(s"$a != $b")
        }
      }
  }

  case class IsSome[A](value: A)

  object IsSome
  {
    implicit def Match_IsSome[A: Eq]: Match[Option[A], IsSome, A, A] =
      new Match[Option[A], IsSome, A, A] {
        def apply(fa: Option[A], fb: IsSome[A]): Either[String, A] = {
          val a1 = fb.value
          fa match {
            case Some(v) =>
              if (Eq[A].eqv(v, a1)) Right(v)
              else Left(s"$v != $a1")
            case None => Left(s"got `None` for $fb")
          }
        }
      }

    implicit def Match_IsSome_Match[A, G[_], B, C]
    (implicit nested: Match[A, G, B, C])
    : Match[Option[A], IsSome, G[B], C] =
      new Match[Option[A], IsSome, G[B], C] {
        def apply(fa: Option[A], fb: IsSome[G[B]]): Either[String, C] = {
          fa match {
            case Some(a) => nested(a, fb.value)
            case None => Left(s"got `None` for $fb")
          }
        }
      }
  }

  case class Compares[A](value: A, comp: PartialFunction[Comparison, Boolean], desc: String)

  object Compares
  {
    implicit def Match_Compares[A: Order]: Match[A, Compares, A, A] =
      new Match[A, Compares, A, A] {
        def apply(a: A, fb: Compares[A]): Either[String, A] = {
          fb.comp.lift(Order[A].comparison(a, fb.value)) match {
            case Some(_) => Right(a)
            case None => Left(s"$a is not ${fb.desc} ${fb.value}")
          }
        }
      }
  }

  case class Contains[A](value: A)

  object Contains
  {
    implicit def Match_Contains_mono[F[_]: Foldable, A: Eq]: Match[F[A], Contains, A, A] =
      new Match[F[A], Contains, A, A] {
        def apply(fa: F[A], fb: Contains[A]): Either[String, A] = {
          Foldable[F].find(fa)(Eq[A].eqv(_, fb.value)) match {
            case Some(a) => Right(a)
            case None => Left(s"$fa does not contain ${fb.value}")
          }
        }
      }

    implicit def Match_Contains_Match[F[_]: Foldable, A, G[_], B, C]
    (implicit nested: Match[A, G, B, C])
    : Match[F[A], Contains, G[B], C] =
      new Match[F[A], Contains, G[B], C] {
        def apply(fa: F[A], fb: Contains[G[B]]): Either[String, C] = {
          Foldable[F]
            .foldLeft(fa, Either.left[List[String], C](Nil)) {
              case (Right(a), _) => Right(a)
              case (Left(err), a) =>
                nested(a, fb.value) match {
                  case Right(a) => Right(a)
                  case Left(e) => Left(e :: err)
                }
            }
            match {
              case Right(a) => Right(a)
              case Left(Nil) => Left(s"empty $fa cannot contain ${fb.value}")
              case Left(es) => Left(s"no element in $fa matched:\n${es.mkString("; ")}")
            }
        }
      }
  }
}

trait MatcherCons
{
  import Match._

  def be[A](a: A): Equals[A] = Equals(a)

  def not[A](a: A): Not[A] = Not(a)

  def beSome[A](a: A): IsSome[A] = IsSome(a)

  def beASome[A]: IsSome[IsAny[A]] = IsSome(IsAny[A]())

  def be_>=[A](a: A): Compares[A] =
    Compares(a, { case Comparison.GreaterThan | Comparison.EqualTo => true }, ">=")

  def contain[A](a: A): Contains[A] = Contains(a)
}

object matcher
extends MatcherCons

@typeclass
trait Sleep[F[_]]
{
  def sleep(d: FiniteDuration): F[Unit]
}

object Sleep
{
  implicit def Sleep_Future(implicit ec: ExecutionContext): Sleep[Future] =
    new Sleep[Future] {
      def sleep(d: FiniteDuration): Future[Unit] =
        Future(Thread.sleep(d.toMillis))
    }
}

sealed trait Xpct[F[_], A]
{
  import Xpct._

  def run(implicit M: MonadError[F, Throwable], S: Sleep[F]): F[Either[String, A]]

  def retry(limit: Int): Xpct[F, A] = Retry(this, limit, None)

  def retryEvery(limit: Int, every: FiniteDuration): Xpct[F, A] = Retry(this, limit, Some(every))
}

object Xpct
extends XpctInstances
with XpctFunctions
{
  case class Strict[F[_], A, G[_], B, C](io: F[A], m: G[B], mtch: Match[A, G, B, C])
  extends Xpct[F, C]
  {
    def run(implicit M: MonadError[F, Throwable], S: Sleep[F]): F[Either[String, C]] =
      Xpct.result(io, m, mtch)
  }

  case class Retry[F[_], A, G[_], B](xp: Xpct[F, B], limit: Int, every: Option[FiniteDuration])
  extends Xpct[F, B]
  {
    def run(implicit M: MonadError[F, Throwable], S: Sleep[F]): F[Either[String, B]] =
      Xpct.retryResult(xp, limit, every)
  }

  case class Concat[F[_], A, B](head: Xpct[F, A], tail: A => Xpct[F, B])
  extends Xpct[F, B]
  {
    def run(implicit M: MonadError[F, Throwable], S: Sleep[F]) =
      head.run.flatMap {
        case Right(b) => tail(b).run
        case Left(err) => M.pure(Left(err))
      }
  }

  case class Value[F[_], A](io: F[A])
  extends Xpct[F, A]
  {
    def run(implicit M: MonadError[F, Throwable], S: Sleep[F]) = io.map(Right(_))
  }
}

trait XpctInstances
{
  import Xpct._

  implicit def Monad_Xpct[F[_]: Applicative, A, G[_]]: Monad[Xpct[F, ?]] =
    new Monad[Xpct[F, ?]] {
      def pure[B](b: B): Xpct[F, B] = Value(Applicative[F].pure(b))

      def flatMap[B, C](a: Xpct[F, B])(f: B => Xpct[F, C]): Xpct[F, C] =
        Concat(a, f)

      def tailRecM[B, C](a: B)(f: B => Xpct[F, Either[B, C]]): Xpct[F, C] = ???
    }
}

trait XpctFunctions
{
  def result[F[_]: Functor, A, G[_], B, C]
  (io: F[A], m: G[B], mtch: Match[A, G, B, C])
  (implicit AE: ApplicativeError[F, Throwable])
  : F[Either[String, C]] =
    io.attempt.map {
      case Right(a) => mtch(a, m)
      case Left(t) => Left(s"$io threw $t:\n${t.getStackTrace.mkString("\n\t")}")
    }

  // remember to use stack-safe F#flatMap!
  def retryResult[A, F[_]: Sleep, G[_], B, C]
  (xp: Xpct[F, C], limit: Int, every: Option[FiniteDuration])
  (implicit ME: MonadError[F, Throwable])
  : F[Either[String, C]] = {
    val interval = every.map(Sleep[F].sleep).getOrElse(Monad[F].pure(()))
    def loop(it: Int): F[Either[String, C]] = {
      xp.run.flatMap {
        case Right(b) => Monad[F].pure(Right(b))
        case Left(err) =>
          if (it >= limit) Monad[F].pure(Left(err))
          else interval.flatMap(_ => loop(it + 1))
      }
    }
    loop(0)
  }
}

@typeclass
trait EvalXpct[F[_]]
{
  def sync[A](fa: F[A]): A
}

object EvalXpct
{
  implicit val EvalXpct_IO: EvalXpct[IO] =
    new EvalXpct[IO] {
      def sync[A](fa: IO[A]) = fa.unsafeRunSync()
    }

  implicit val EvalXpct_Future: EvalXpct[Future] =
    new EvalXpct[Future] {
      def sync[A](fa: Future[A]) = Await.result(fa, Duration.Inf)
    }
}

class XpctMust[A, F[_]](io: F[A])
{
  import Match.Not
  import matcher._

  def must_==(a: A): Xpct[F, A] = must(be(a))

  def must[G[_], B, C](b: G[B])(implicit mtch: Match[A, G, B, C]): Xpct[F, C] = Xpct.Strict(io, b, mtch)

  def mustNot[G[_], B, C](b: G[B])(implicit mtch: Match[A, Not, G[B], C]): Xpct[F, C] = Xpct.Strict(io, not(b), mtch)

  def xp: Xpct[F, A] = Xpct.Value(io)
}

trait ToXpctMust
{
  implicit def ToXpctMust[A, F[_]](io: F[A])(implicit ME: MonadError[F, Throwable]): XpctMust[A, F] = new XpctMust(io)
}

trait LiftAnyIOMust
{
  implicit def LiftAnyIOMust[A](a: => A): XpctMust[A, IO] = new XpctMust(IO(a))
}
