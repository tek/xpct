package xpct

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.ExecutionContext

import cats.effect.Async

import fs2.Scheduler

trait Fs2Sleep
{
  implicit def Sleep_IO[F[_]: Async](implicit ec: ExecutionContext): Sleep[F] =
    new Sleep[F] {
      def sleep(d: FiniteDuration): F[Unit] =
        Scheduler[F](corePoolSize = 1).flatMap(_.sleep[F](d)).run
    }
}

object Fs2Sleep
extends Fs2Sleep
