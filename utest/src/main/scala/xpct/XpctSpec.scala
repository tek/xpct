package xpct

import cats.{Applicative, MonadError}

case class XpctFailed(failure: XpFailure)
extends Exception(failure.toString)

trait XpctSpec
extends ToXpctMust
{
  def xpct[F[_]: EvalXp, A](x: => Xp[F, A])
  (implicit ME: MonadError[F, Throwable])
  : Unit = {
    EvalXp[F].sync(RunXp(x)) match {
      case XpResult.Success(_) =>
      case XpResult.Failure(_, failure) => throw XpctFailed(failure)
    }
  }

  def xpctIO[F[_]: EvalXp, A](x: => Xp[F, A])
  (implicit ME: MonadError[F, Throwable])
  : F[Unit] = {
    EvalXp[F].sync(RunXp(x)) match {
      case XpResult.Success(_) => Applicative[F].pure(())
      case XpResult.Failure(_, failure) => MonadError[F, Throwable].raiseError(XpctFailed(failure))
    }
  }
}
