package xpct

import scala.concurrent.duration.FiniteDuration

import cats.{Functor, Applicative, Monad, Foldable, MonadError, ApplicativeError}
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.applicativeError._

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
