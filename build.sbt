projectName in ThisBuild := Some("xpct")
libs in ThisBuild := Libs

fs2Version := "0.10.0-M6"
catsVersion := "1.0.0-MF"
catsEffectVersion := "0.4"
simulacrumVersion := "0.10.0"
specs2Version := "4.0.0-RC4"

val core: Project = "core".paradise("2.+") / "xpct core"
val specs2: Project = "specs2" / "specs2 extensions" << core
val scalatest: Project = "scalatest" / "scalatest extensions" << core
val fs2: Project = "fs2" / "fs2 sleep extension" << core
val unit: Project = "unit" / "unit tests" << fs2 << specs2
val xpct: Project = "root".settingsV(publish := (), publishLocal := (), publishTo := None)

Publish.releaseSettings
