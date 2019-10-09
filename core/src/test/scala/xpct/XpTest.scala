package xpct

import scala.concurrent.ExecutionContext

import cats.effect.{IO, Timer}
import cats.implicits._
import org.specs2.matcher.MatchResult
import org.specs2.mutable.Specification

object XpTest
{
  implicit def timer: Timer[IO] =
    IO.timer(ExecutionContext.global)

  def test: Xp[IO, Unit] =
    for {
      a <- Xp.assert(IO.pure(1), Match.Equals(1))
      _ <- Xp.assert(IO.pure(2), Match.Equals(2))
      _ <- Xp.retry(Xp.attempt(Xp.assert(IO.pure(a), Match.Equals(2))), 2, None)
    } yield ()
}

class XpTest
extends Specification
{
  def run: IO[MatchResult[Any]] =
    CompileXp[IO, Unit](XpTest.test)
      .value
      .run
      .map(_ must_== (List(XpSuccess("1 == 1"), XpSuccess("2 == 2")), Left(XpFailure.Assert("1 /= 2"))))

  "test" >> run.unsafeRunSync()
}
