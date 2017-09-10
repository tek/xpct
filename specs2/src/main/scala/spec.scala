package xpct

import cats.MonadError

import org.specs2.specification.core.ImmutableSpecificationStructure
import org.specs2.specification.create.SpecificationCreation
import org.specs2.matcher.{Matcher, BeEqualTo, MatchResultCombinators}
import org.specs2.execute.{Result, AsResult, Success => SpecsSuccess, Failure => SpecsFailure}
import MatchResultCombinators._

trait Specs2Instances
{
  implicit def AsResult_Xpct[F[_]: EvalXpct: Sleep, A, G[_], B]
  (implicit ME: MonadError[F, Throwable])
  : AsResult[Xpct[F, B]] =
    new AsResult[Xpct[F, B]] {
      def asResult(t: => Xpct[F, B]): Result = {
        EvalXpct[F].sync(t.run) match {
          case Right(a) => SpecsSuccess()
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
