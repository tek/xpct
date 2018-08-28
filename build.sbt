import ReleaseTransformations._

scalaVersion in ThisBuild := "2.12.6"
releaseCrossBuild := true
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  setReleaseVersion,
  releaseStepCommandAndRemaining("+publishSigned"),
  releaseStepCommand("sonatypeReleaseAll"),
  commitReleaseVersion,
  tagRelease,
  setNextVersion,
  commitNextVersion
)

val fs2Version = "0.10.0-M6"
val catsVersion = "1.0.0-MF"
val catsEffectVersion = "0.4"
val simulacrumVersion = "0.10.0"
val specs2Version = "4.0.0-RC4"
val scalatestVersion = "3.0.1"
val utestVersion = "0.5.3"

val core =
  pro("core")
    .settings(
      addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full),
      libraryDependencies ++= List(
        "org.typelevel" %% "cats-core" % catsVersion,
        "org.typelevel" %% "cats-effect" % catsEffectVersion,
        "com.github.mpilquist" %% "simulacrum" % simulacrumVersion,
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

val fs2 =
  pro("fs2")
    .dependsOn(core)
    .settings(
      libraryDependencies ++= List(
        "co.fs2" %% "fs2-core" % fs2Version
      )
    )

val unit =
  pro("unit")
    .dependsOn(fs2, specs2)

val root =
  basicProject(project.in(file(".")))
    .aggregate(core, specs2, scalatest, fs2)
    .settings(noPublish)
