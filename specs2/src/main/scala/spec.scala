package xpct

import cats.MonadError
import org.specs2.execute.{AsResult, Failure => SpecsFailure, Result, Success => SpecsSuccess}
import org.specs2.specification.core.ImmutableSpecificationStructure
import org.specs2.specification.create.SpecificationCreation

trait Specs2Instances
{
  implicit def AsResult_Xpct[F[_]: EvalXpct: Sleep, A]
  (implicit ME: MonadError[F, Throwable])
  : AsResult[Xpct[F, A]] =
    new AsResult[Xpct[F, A]] {
      def asResult(t: => Xpct[F, A]): Result = {
        EvalXpct[F].sync(t.run) match {
          case Right(_) => SpecsSuccess()
          case Left(err) => SpecsFailure(err)
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
