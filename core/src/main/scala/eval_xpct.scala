package xpct

import scala.concurrent.duration.Duration
import scala.concurrent.{Future, Await}

import cats.effect.IO

trait EvalXpct[F[_]]
{
  def sync[A](fa: F[A]): A
}

object EvalXpct
{
  def apply[F[_]](implicit instance: EvalXpct[F]): EvalXpct[F] = instance

  implicit val EvalXpct_IO: EvalXpct[IO] =
    new EvalXpct[IO] {
      def sync[A](fa: IO[A]) = fa.unsafeRunSync()
    }

  implicit val EvalXpct_Future: EvalXpct[Future] =
    new EvalXpct[Future] {
      def sync[A](fa: Future[A]) = Await.result(fa, Duration.Inf)
    }
}
