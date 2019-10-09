package xpct
package klk

import _root_.klk.{Compile, KlkResult, SimpleTest, TestResult}
import cats.{Functor, MonadError}
import cats.data.NonEmptyList
import cats.effect.Timer
import cats.implicits._

object XpctKlk
{
  def successes[T[_]: Functor](results: T[XpSuccess]): T[KlkResult] =
    results.map {
      case XpSuccess(message) =>
        KlkResult.Single(true, KlkResult.Details.Simple(List(message)))
    }

  def failure: XpFailure => KlkResult.Details = {
    case XpFailure.NoAsserts => KlkResult.Details.Simple(List("no assertions were made"))
    case XpFailure.Assert(message) => KlkResult.Details.Simple(List(message))
    case XpFailure.Fatal(exception) => KlkResult.Details.Fatal(exception)
  }

  def convertResult: XpResult => KlkResult = {
    case XpResult.Success(results) =>
      KlkResult.Multi(XpctKlk.successes(results))
    case XpResult.Failure(results, failure) =>
      KlkResult.Multi(NonEmptyList(KlkResult.failure(XpctKlk.failure(failure)), XpctKlk.successes(results)))
  }
}

private[klk] trait XpctKlkInstances
{
  implicit def TestResult_Xp: TestResult[XpResult] =
    new TestResult[XpResult] {
      def apply(output: XpResult): KlkResult =
        XpctKlk.convertResult(output)
    }

  implicit def Compile_Xp[F[_]: MonadError[*[_], Throwable]]
  : Compile[Xp[F, ?], F, Unit] =
    new Compile[Xp[F, ?], F, Unit] {
      def apply(fa: Xp[F, Unit]): F[KlkResult] =
        RunXp(fa).map(XpctKlk.convertResult)
    }
}

object `package`
extends XpctKlkInstances

trait XpctKlkTest[F[_]]
extends SimpleTest[F]
with XpctKlkInstances
