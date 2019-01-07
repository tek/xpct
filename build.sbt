import ReleaseTransformations._

scalaVersion in ThisBuild := "2.12.7"
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

val fs2Version = "1.0.0-RC1"
val catsVersion = "1.4.0"
val catsEffectVersion = "1.0.0"
val specs2Version = "4.0.0-RC4"
val scalatestVersion = "3.0.1"
val utestVersion = "0.6.3"

val core =
  pro("core")
    .settings(
      libraryDependencies ++= List(
        "org.typelevel" %% "cats-core" % catsVersion,
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

val unit =
  pro("unit")
    .dependsOn(specs2)

val root =
  basicProject(project.in(file(".")))
    .aggregate(core, specs2, scalatest, utest)
    .settings(noPublish)
