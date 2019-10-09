package xpct

import cats.MonadError
import cats.effect.IO
import cats.implicits._
import org.specs2.matcher.MatchResult
import org.specs2.mutable.Specification

object XpTest
{
  import xpct.matcher._
  import xpct.must._
  import xpct.ops._

  def compose: Xp[IO, Unit] =
    for {
      a <- Xp.assert(IO.pure(1), Match.Equals(1))
      _ <- Xp.assert(IO.pure(2), Match.Equals(2))
      _ <- Xp.retry(2)(Xp.attempt(Xp.assert(IO.pure(a), Match.Equals(2))))
    } yield ()

  def must: Xp[IO, Unit] =
    IO.pure(13)
      .must(gte(10))
      .void

  def extract: Xp[IO, Unit] =
    IO.pure(List(Right(Some(27))))
      .extract { case List(Right(Some(a))) => a }
      .assert(gte(25))
      .must(equal(29))
      .void

  def either: Xp[Either[Throwable, ?], Unit] =
    Either.right[Throwable, Int](1).assert(gte(0)).void
}

class XpTest
extends Specification
{
  def run[F[_]: MonadError[*[_], Throwable]]
  (test: Xp[F, Unit])
  (target: (List[XpSuccess], Either[XpFailure, Unit]))
  (implicit eval: EvalXp[F])
  : MatchResult[Any] =
    eval(
      CompileXp[F, Unit](test)
        .value
        .run
        .map(_ must_== target)
    )

  "compose" >> run(XpTest.compose)(List(XpSuccess("1 == 1"), XpSuccess("2 == 2")), Left(XpFailure.Assert("1 /= 2")))

  "must" >> run(XpTest.must)(List(XpSuccess("13 >= 10")), Right(()))

  "extract" >> run(XpTest.extract)(
      List(XpSuccess("extracted 27 from List(Right(Some(27)))"), XpSuccess("27 >= 25")),
      Left(XpFailure.Assert("27 /= 29"))
  )

  "either" >> run(XpTest.either)(List(XpSuccess("1 >= 0")), Right(()))
}
