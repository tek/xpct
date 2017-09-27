package xpct

import cats.MonadError

case class XpctFailed(err: String)
extends Exception(err)

trait XpctSpec
extends ToXpctMust
{
  def xpct[F[_]: EvalXpct: Sleep, A, G[_], B](x: => Xpct[F, B])
  (implicit ME: MonadError[F, Throwable])
  : Unit = {
    EvalXpct[F].sync(x.run) match {
      case Right(a) =>
      case Left(err) => throw new XpctFailed(err)
    }
  }
}
