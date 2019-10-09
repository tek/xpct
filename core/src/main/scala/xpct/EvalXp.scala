package xpct

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

import cats.effect.IO
import cats.implicits._

trait EvalXp[F[_]]
{
  def apply[A](fa: F[A]): A
}

object EvalXp
{
  def apply[F[_]](implicit instance: EvalXp[F]): EvalXp[F] = instance

  implicit val EvalXpct_IO: EvalXp[IO] =
    new EvalXp[IO] {
      def apply[A](fa: IO[A]) = fa.unsafeRunSync()
    }

  implicit val EvalXpct_Future: EvalXp[Future] =
    new EvalXp[Future] {
      def apply[A](fa: Future[A]) = Await.result(fa, Duration.Inf)
    }

  implicit val EvalXpct_Either: EvalXp[Either[Throwable, ?]] =
    new EvalXp[Either[Throwable, ?]] {
      def apply[A](fa: Either[Throwable,A]): A = fa.valueOr(throw _)
    }
}
