package xpct

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import cats.implicits._
import utest._

object BaseSpec
extends TestSuite
with ToXpctMust
with XpctSpec
{
  val tests = Tests {
    "test" - {
      xpct {
        Future(2) must_== 2
      }
    }
  }
}
