import sbt._

import LibVersions._

object LibVersions
{
  val fs2Version = settingKey[String]("fs2 version")
  val catsVersion = settingKey[String]("cats version")
  val catsEffectVersion = settingKey[String]("cats-effect version")
  val simulacrumVersion = settingKey[String]("simulacrum version")
  val specs2Version = settingKey[String]("specs2 version")
  val scalatestVersion = settingKey[String]("scalatest version")
  val utestVersion = settingKey[String]("utest version")
}

object LibVersionsPlugin
extends AutoPlugin
{
  val autoImport = LibVersions
}

object Libs
extends tryp.Libs
{
  val core = ids(
    "org.typelevel" %% "cats-core" % catsVersion.value,
    "org.typelevel" %% "cats-effect" % catsEffectVersion.value,
    "com.github.mpilquist" %% "simulacrum" % simulacrumVersion.value
  )

  val fs2 = ids(
    "co.fs2" %% "fs2-core" % fs2Version.value
  )

  val specs2 = ids(
    "org.specs2" %% "specs2-core" % specs2Version.value
  )

  val scalatest = ids(
    "org.scalatest" %% "scalatest" % scalatestVersion.value
  )

  val utest = ids(
    "com.lihaoyi" %% "utest" % utestVersion.value
  )
}
