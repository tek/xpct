package xpct

import cats.MonadError

import org.scalatest.exceptions.TestFailedException

trait XpctSpec
extends ToXpctMust
{
  def xpct[F[_]: EvalXpct: Sleep, A, G[_], B](x: => Xpct[F, B])
  (implicit ME: MonadError[F, Throwable])
  : Unit = {
    EvalXpct[F].sync(x.run) match {
      case Right(_) =>
      case Left(err) => throw new TestFailedException(err, 0)
    }
  }
}
