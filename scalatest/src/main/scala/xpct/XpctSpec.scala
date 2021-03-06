package xpct

import cats.MonadError
import org.scalatest.exceptions.TestFailedException

trait XpctSpec
extends XpctTest
{
  def xpct[F[_]: EvalXp, A](x: Xp[F, A])
  (implicit ME: MonadError[F, Throwable])
  : Unit = {
    EvalXp[F].apply(RunXp[F, A](x)) match {
      case XpResult.Success(_) =>
      case XpResult.Failure(_, failure) => throw new TestFailedException(failure.toString, 0)
    }
  }
}
