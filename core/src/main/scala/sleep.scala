package xpct

import scala.concurrent.{Future, ExecutionContext}
import scala.concurrent.duration.FiniteDuration

import cats.effect.IO

trait Sleep[F[_]]
{
  def sleep(d: FiniteDuration): F[Unit]
}

object Sleep
{
  def apply[F[_]](implicit instance: Sleep[F]): Sleep[F] = instance

  implicit def Sleep_Future(implicit ec: ExecutionContext): Sleep[Future] =
    new Sleep[Future] {
      def sleep(d: FiniteDuration): Future[Unit] =
        Future(Thread.sleep(d.toMillis))
    }

  implicit def Sleep_IO(implicit ec: ExecutionContext): Sleep[IO] =
    new Sleep[IO] {
      def sleep(d: FiniteDuration): IO[Unit] =
        IO.timer(ec).sleep(d)
    }
}
