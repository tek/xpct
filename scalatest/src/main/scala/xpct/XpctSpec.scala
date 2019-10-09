package xpct

import cats.MonadError
import org.scalatest.exceptions.TestFailedException

trait XpctSpec
extends ToXpctMust
{
  def xpct[F[_]: EvalXp, A](x: Xp[F, A])
  (implicit ME: MonadError[F, Throwable])
  : Unit = {
    EvalXp[F].sync(RunXp[F, A](x)) match {
      case XpResult.Success(_) =>
      case XpResult.Failure(_, failure) => throw new TestFailedException(failure.toString, 0)
    }
  }
}
