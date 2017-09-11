import sbt._
import Keys._

import tryp.TrypBuildKeys._

object Basic
extends AutoPlugin
{
  override def trigger = allRequirements

  override def projectSettings = List(
    crossScalaVersions += "2.11.11-bin-typelevel-4"
  )
}
