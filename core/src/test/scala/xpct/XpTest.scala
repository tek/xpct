package xpct

import cats.effect.IO
import cats.implicits._
import org.specs2.matcher.MatchResult
import org.specs2.mutable.Specification

object XpTest
{
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
    CompileXp[IO]
      .apply(XpTest.test)
      .value
      .run
      .map(_ must_== (List(XpSuccess(""), XpSuccess("")), Left(XpFailure.Assert("1 != 2"))))

  "test" >> run.unsafeRunSync()
}
