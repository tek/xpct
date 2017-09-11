import sbt._
import Keys._

import tryp.TrypBuildKeys._

object ZZ
extends AutoPlugin
{
  override def requires = tryp.Tryp
  override def trigger = allRequirements

  override def projectSettings = Publish.publishSettings ++ List(
    setScala := true,
    twelve := true,
		doc in Compile := target.value / "none",
    organization := "io.tryp",
    crossScalaVersions += "2.11.11-bin-typelevel-4"
  )
}
