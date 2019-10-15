package xpct
package klk

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

import _root_.klk.{ConsTest, KlkResult, TestResources}
import _root_.klk.KlkResult.{Multi, Single}
import _root_.klk.KlkResult.Details.Simple
import cats.Id
import cats.data.NonEmptyList
import cats.effect.{IO, Timer}
import cats.effect.concurrent.Ref
import cats.implicits._
import org.specs2.matcher.MatchResult
import org.specs2.mutable.Specification
import shapeless.HNil

class XpctKlkTest
extends Specification
{
  implicit def timer: Timer[IO] =
    IO.timer(ExecutionContext.global)

  def multi(head: KlkResult, results: KlkResult*): KlkResult =
    Multi(NonEmptyList.of(head, results: _*))

  def test[A](head: KlkResult, results: KlkResult*)(xp: Xp[IO, A]): MatchResult[Any] =
    ConsTest[IO, HNil, Id](TestResources.empty)(xp.void).unsafeRunSync must_== multi(head, results: _*)

  def single(success: Boolean)(message: String): KlkResult =
    Single(success, Simple(List(message)))

  "xpct klk success" >> test(single(true)("1 == 1"), single(true)("2 == 2"),
    single(true)("Not(Equals(2)) failed for 1 (Assert(1 /= 2))")) {
    for {
      a <- Xp.assert(Match.Equals(1))(IO.pure(1))
      _ <- Xp.assert(Match.Equals(2))(IO.pure(2))
      _ <- Xp.assert(Match.Not(Match.Equals(2)))(IO.pure(a))
    } yield ()
  }

  "xpct klk failure" >> test(single(false)("1 /= 2")) {
    Xp.assert(Match.Equals(2))(IO.pure(1))
  }

  "xpct klk retry" >> test(single(true)("5 == 5")) {
    for {
      counter <- Xp.suspend(Ref.of[IO, Int](0))
      _ <- Xp.retry(4)(Xp.assert(Match.Equals(5))(counter.update(_ + 1) *> counter.get))
    } yield ()
  }

  "xpct klk attempt retry" >> test(single(true)("5 == 5")) {
    import ops._
    def run(counter: Ref[IO, Int]): IO[Int] =
      counter.update(_ + 1) *> counter.get.map {
        case 5 => 5
        case _ => sys.error("ouch")
      }
    for {
      counter <- Xp.suspend(Ref.of[IO, Int](0))
      _ <- retryEvery(100.milli)(4)(attempt(assert(Match.Equals(5))(run(counter))))
    } yield ()
  }

  "xpct klk must matcher" >> test(single(true)("contains 2")) {
    import must._
    IO.pure(List(1, 2, 3)).must(matcher.contain(2))
  }

  "xpct klk adt covariance" >> test(single(true)("Right contains 2")) {
    import ops._
    val v: Option[Either[String, Int]] = Some(Right(2))
    assert(matcher.beSome(matcher.beRight(2)))(IO.pure(v))
  }
}
