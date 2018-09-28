package xpct

import scala.util.Random

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

import cats.effect.IO
import cats.implicits._

class Specs2Spec(implicit ec: ExecutionContext)
extends XpctSpec
{
  def name = "spec"

  val threads = 5

  def is = s2"""
  mixed effects $mixed
  nested for comprehension $nested
  """

  import matcher._

  def mixed = {
    for {
      a <- IO.pure(Either.right(1)) must contain(1)
      b <- IO.pure(Option(2)) must beSome(2)
    } yield ()
  }

  val rand: IO[Int] = IO(Random.nextInt().abs % 100)

  var x = false

  def failOnce = IO(if (x) 1 else { x = true; sys.error("boom") })

  def nested = {
    for {
      _ <- failOnce must_== 1 retry 1
      r <- {
        for {
          target <- IO.pure(Option(2)) must beASome[Int]
          a <- rand.xp
          b <- IO(a) must be_>=(1)
          c <- IO(Option(b)) must contain(a)
          _ <- IO(b) must_== target
        } yield (target, b, c)
      }.retryEvery(1000, 1.millisecond)
      (target, b, c) = r
      d <- IO(List(-1, -2, b, -3)) must contain(be_>=(0))
      _ <- IO(b) must_== target
      _ <- IO(b) mustNot be(0)
      _ <- IO(None: Option[Int]) mustNot contain(1)
    } yield ()
  }
}
