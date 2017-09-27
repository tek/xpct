package xpct

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import cats.effect.IO
import cats.implicits._

import org.scalatest.WordSpec

class BaseSpec
extends WordSpec
with XpctSpec
{
  "test" in {
    xpct {
      for {
        a <- Future(1) must_== 2
      } yield ()
    }
  }
}
