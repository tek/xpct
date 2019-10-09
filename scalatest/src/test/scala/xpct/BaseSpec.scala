package xpct

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import cats.implicits._
import org.scalatest.WordSpec

class BaseSpec
extends WordSpec
with XpctSpec
{
  "test" in {
    xpct {
      Future(2) must_== 2
    }
  }
}
