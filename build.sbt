ThisBuild / scalaVersion := "2.13.1"

val catsVersion = "2.0.0"
val catsEffectVersion = "2.0.0"
val specs2Version = "4.7.1"
val scalatestVersion = "3.0.8"
val utestVersion = "0.6.9"
val klkVersion = "0.1.1"

val core =
  pro("core")
    .settings(
      libraryDependencies ++= List(
        "org.typelevel" %% "cats-free" % catsVersion,
        "org.typelevel" %% "cats-effect" % catsEffectVersion,
      )
    )

val specs2 =
  pro("specs2")
    .dependsOn(core)
    .settings(
      libraryDependencies ++= List(
        "org.specs2" %% "specs2-core" % specs2Version
      )
    )

val scalatest =
  pro("scalatest")
    .dependsOn(core)
    .settings(
      libraryDependencies ++= List(
        "org.scalatest" %% "scalatest" % scalatestVersion
      )
    )

val utest =
  pro("utest")
    .dependsOn(core)
    .settings(
      testFrameworks += new TestFramework("utest.runner.Framework"),
      libraryDependencies ++= List(
        "com.lihaoyi" %% "utest" % utestVersion
      )
    )

val klk =
  pro("klk")
    .dependsOn(core)
    .settings(
      libraryDependencies ++= List(
        "io.tryp" %% "kallikrein-core" % klkVersion
      )
    )

val unit =
  pro("unit")
    .dependsOn(specs2)

val root =
  basicProject(project.in(file(".")))
    .aggregate(core, specs2, scalatest, utest, klk)
    .settings(noPublish)

import ReleaseTransformations._

releaseCrossBuild := true
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  setReleaseVersion,
  releaseStepCommandAndRemaining("+publish"),
  releaseStepCommand("sonatypeReleaseAll"),
  commitReleaseVersion,
  tagRelease,
  setNextVersion,
  commitNextVersion
)
