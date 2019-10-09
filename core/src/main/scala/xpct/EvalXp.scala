package xpct

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

import cats.effect.IO

trait EvalXp[F[_]]
{
  def sync[A](fa: F[A]): A
}

object EvalXp
{
  def apply[F[_]](implicit instance: EvalXp[F]): EvalXp[F] = instance

  implicit val EvalXpct_IO: EvalXp[IO] =
    new EvalXp[IO] {
      def sync[A](fa: IO[A]) = fa.unsafeRunSync()
    }

  implicit val EvalXpct_Future: EvalXp[Future] =
    new EvalXp[Future] {
      def sync[A](fa: Future[A]) = Await.result(fa, Duration.Inf)
    }
}
