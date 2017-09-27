import sbt._
import Keys._

import sbtrelease.ReleasePlugin.autoImport.{ReleaseTransformations, releaseProcess, ReleaseStep}
import com.typesafe.sbt.pgp.PgpKeys.publishSigned
import ReleaseTransformations._

object Publish
{
  val github = "https://github.com/tek"
  val repo = s"$github/xpct"

  def pubTo(snapshot: Boolean) =
    if (snapshot) Opts.resolver.sonatypeSnapshots
    else Resolver.url("sonatype staging", url("https://oss.sonatype.org/service/local/staging/deploy/maven2"))

  def publishSettings = List(
    publishMavenStyle := true,
    publishTo := Some(pubTo(isSnapshot.value)),
    licenses := List("MIT" -> url("http://opensource.org/licenses/MIT")),
    homepage := Some(url(repo)),
    scmInfo := Some(ScmInfo(url(repo), "scm:git@github.com:tek/xpct")),
    developers := List(Developer(id="tryp", name="Torsten Schmits", email="torstenschmits@gmail.com", url=url(github)))
  )

  def releaseSettings = List(
    publish := (),
    publishLocal := (),
    publishSigned := (),
    publishTo := None,
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      setReleaseVersion,
      ReleaseStep(action = Command.process("publishSigned", _), enableCrossBuild = true),
      commitReleaseVersion,
      tagRelease,
      setNextVersion,
      commitNextVersion,
      ReleaseStep(action = Command.process("sonatypeReleaseAll", _), enableCrossBuild = true),
      pushChanges
    )
  )
}
