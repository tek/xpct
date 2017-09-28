package xpct

import scala.concurrent.{Future, ExecutionContext}
import scala.concurrent.duration.FiniteDuration

import simulacrum.typeclass

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
