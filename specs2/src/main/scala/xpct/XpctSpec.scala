package xpct

import cats.MonadError
import org.specs2.execute.{AsResult, Failure => SpecsFailure, Result, Success => SpecsSuccess}
import org.specs2.specification.core.ImmutableSpecificationStructure
import org.specs2.specification.create.SpecificationCreation

trait Specs2Instances
{
  implicit def AsResult_Xpct[F[_]: EvalXp, A]
  (implicit ME: MonadError[F, Throwable])
  : AsResult[Xp[F, A]] =
    new AsResult[Xp[F, A]] {
      def asResult(t: => Xp[F, A]): Result = {
        EvalXp[F].sync(RunXp(t)) match {
          case XpResult.Success(_) => SpecsSuccess()
          case XpResult.Failure(_, failure) => SpecsFailure(failure.toString)
        }
      }
    }
}

object Specs2Instances
extends Specs2Instances

trait XpctSpec
extends ImmutableSpecificationStructure
with SpecificationCreation
with ToXpctMust
with Specs2Instances
