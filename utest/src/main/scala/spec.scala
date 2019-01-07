package xpct

import cats.{MonadError, Applicative}

case class XpctFailed(err: String)
extends Exception(err)

trait XpctSpec
extends ToXpctMust
{
  def xpct[F[_]: EvalXpct: Sleep, A](x: => Xpct[F, A])
  (implicit ME: MonadError[F, Throwable])
  : Unit = {
    EvalXpct[F].sync(x.run) match {
      case Right(_) =>
      case Left(err) => throw new XpctFailed(err)
    }
  }

  def xpctIO[F[_]: EvalXpct: Sleep, A](x: => Xpct[F, A])
  (implicit ME: MonadError[F, Throwable])
  : F[A] = {
    EvalXpct[F].sync(x.run) match {
      case Right(a) => Applicative[F].pure(a)
      case Left(err) => MonadError[F, Throwable].raiseError(new XpctFailed(err))
    }
  }
}
