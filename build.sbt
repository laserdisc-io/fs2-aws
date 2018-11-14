name := "fs2-aws"
organization in ThisBuild := "io.github.dmateusp"

scalaVersion := "2.12.7"

scalacOptions in ThisBuild ++= Seq(
  "-encoding",
  "UTF-8", // source files are in UTF-8
  "-deprecation", // warn about use of deprecated APIs
  "-unchecked", // warn about unchecked type parameters
  "-feature", // warn about misused language features
  "-language:higherKinds", // allow higher kinded types without `import scala.language.higherKinds`
  "-language:implicitConversions", // allow use of implicit conversions
  "-Xlint", // enable handy linter warnings
  "-Xfatal-warnings", // turn compiler warnings into errors
  "-Ypartial-unification" // allow the compiler to unify type constructors of different arities
)
lazy val root = (project in file("."))
  .aggregate(`fs2-aws`, `fs2-aws-testkit`)
  .settings(
    skip in publish := true
  )

lazy val `fs2-aws`         = (project in file("fs2-aws"))
lazy val `fs2-aws-testkit` = (project in file("fs2-aws-testkit")).dependsOn(`fs2-aws`)

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.3")

// publish
publishTo in ThisBuild := Some(
  "Sonatype Nexus" at "https://oss.sonatype.org/service/local/staging/deploy/maven2")

licenses := Seq("MIT" -> url("https://github.com/dmateusp/fs2-aws/blob/master/LICENSE"))
developers := List(
  Developer(id = "dmateusp",
            name = "Daniel Mateus Pires",
            email = "dmateusp@gmail.com",
            url = url("https://github.com/dmateusp"))
)
homepage := Some(url("https://github.com/dmateusp/fs2-aws"))
scmInfo := Some(
  ScmInfo(url("https://github.com/dmateusp/fs2-aws"),
          "scm:git:git@github.com:dmateusp/fs2-aws.git"))

// release
import ReleaseTransformations._

// signed releases

pgpPublicRing := file(".travis/local.pubring.asc")
pgpSecretRing := file(".travis/local.secring.asc")
releasePublishArtifactsAction := PgpKeys.publishSigned.value
pgpPassphrase := sys.env.get("PGP_PASS").map(_.toCharArray)
credentials in ThisBuild += Credentials("Sonatype Nexus Repository Manager",
                                        "oss.sonatype.org",
                                        sys.env.getOrElse("SONATYPE_USERNAME", ""),
                                        sys.env.getOrElse("SONATYPE_PASSWORD", ""))

publishArtifact in ThisBuild in Test := true

// release steps
releaseProcess := Seq[ReleaseStep](
  inquireVersions,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  publishArtifacts,
  pushChanges
)

releaseTagComment := s"Releasing ${(version in ThisBuild).value}"
releaseCommitMessage := s"[skip travis] Setting version to ${(version in ThisBuild).value}"
