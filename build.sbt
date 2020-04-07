import scoverage.ScoverageKeys.coverageMinimum

organization := "io.laserdisc"
name         := "fs2-aws"

lazy val scala212               = "2.12.10"
lazy val scala213               = "2.13.1"
lazy val supportedScalaVersions = List(scala212, scala213)

crossScalaVersions in ThisBuild := supportedScalaVersions

scalaVersion in ThisBuild := scala213

val fs2Version    = "2.2.2"
val AwsSdkVersion = "1.11.759"
val cirisVersion  = "0.12.1"
val circeVersion  = "0.13.0"

lazy val root = (project in file("."))
  .aggregate(`fs2-aws`, `fs2-aws-testkit`, `fs2-aws-dynamodb`, `fs2-aws-core`, `fs2-aws-examples`)
  .settings(
    publishArtifact    := false,
    crossScalaVersions := Nil
  )

lazy val `fs2-aws-core` = (project in file("fs2-aws-core"))
  .settings(
    name := "fs2-aws-core",
    libraryDependencies ++= Seq(
      "co.fs2"        %% "fs2-core"                % "2.3.0",
      "co.fs2"        %% "fs2-io"                  % "2.3.0",
      "org.mockito"   % "mockito-core"             % "3.3.3" % Test,
      "org.mockito"   %% "mockito-scala-scalatest" % "1.13.5" % Test,
      "org.scalatest" %% "scalatest"               % "3.1.1" % Test
    ),
    coverageMinimum       := 40,
    coverageFailOnMinimum := true
  )
  .settings(commonSettings)
  .settings(scalacOptions := commonOptions(scalaVersion.value))

lazy val `fs2-aws-dynamodb` = (project in file("fs2-aws-dynamodb"))
  .dependsOn(`fs2-aws-core`)
  .settings(
    name                  := "fs2-aws-dynamodb",
    coverageMinimum       := 40,
    coverageFailOnMinimum := true,
    libraryDependencies ++= Seq(
      "co.fs2"        %% "fs2-core"                        % fs2Version,
      "co.fs2"        %% "fs2-io"                          % fs2Version,
      "org.mockito"   % "mockito-core"                     % "3.3.3" % Test,
      "org.scalatest" %% "scalatest"                       % "3.1.1" % Test,
      "org.mockito"   %% "mockito-scala-scalatest"         % "1.11.3" % Test,
      "com.amazonaws" % "dynamodb-streams-kinesis-adapter" % "1.5.1",
      "io.laserdisc"  %% "scanamo-circe"                   % "1.0.8"
    )
  )
  .settings(commonSettings)
  .settings(scalacOptions := commonOptions(scalaVersion.value))

lazy val `fs2-aws-examples` = (project in file("fs2-aws-examples"))
  .dependsOn(`fs2-aws-dynamodb`)
  .settings(
    name            := "fs2-aws-examples",
    coverageMinimum := 0,
    libraryDependencies ++= Seq(
      "org.mockito"       % "mockito-core"             % "3.3.3" % Test,
      "org.mockito"       %% "mockito-scala-scalatest" % "1.11.3" % Test,
      "ch.qos.logback"    % "logback-classic"          % "1.2.3",
      "ch.qos.logback"    % "logback-core"             % "1.2.3",
      "org.slf4j"         % "jcl-over-slf4j"           % "1.7.30",
      "org.slf4j"         % "jul-to-slf4j"             % "1.7.30",
      "io.chrisdavenport" %% "log4cats-slf4j"          % "1.0.1",
      "io.laserdisc"      %% "scanamo-circe"           % "1.0.8"
    )
  )
  .settings(commonSettings)
  .settings(scalacOptions := commonOptions(scalaVersion.value))
  .settings(
    skip in publish := true
  )

lazy val `fs2-aws` = (project in file("fs2-aws"))
  .dependsOn(`fs2-aws-core`)
  .settings(
    name := "fs2-aws",
    libraryDependencies ++= Seq(
      "co.fs2"                  %% "fs2-core"                     % fs2Version,
      "co.fs2"                  %% "fs2-io"                       % fs2Version,
      "com.amazonaws"           % "aws-java-sdk-kinesis"          % AwsSdkVersion,
      "com.amazonaws"           % "aws-java-sdk-s3"               % AwsSdkVersion,
      "com.amazonaws"           % "aws-java-sdk-sqs"              % AwsSdkVersion,
      "com.amazonaws"           % "amazon-kinesis-producer"       % "0.14.0",
      "software.amazon.kinesis" % "amazon-kinesis-client"         % "2.2.10",
      "org.mockito"             % "mockito-core"                  % "3.3.3" % Test,
      "org.scalatest"           %% "scalatest"                    % "3.1.1" % Test,
      "software.amazon.awssdk"  % "sts"                           % "2.11.11",
      "org.mockito"             %% "mockito-scala-scalatest"      % "1.11.3" % Test,
      "com.amazonaws"           % "aws-java-sdk-sqs"              % AwsSdkVersion excludeAll ("commons-logging", "commons-logging"),
      "com.amazonaws"           % "amazon-sqs-java-messaging-lib" % "1.0.8" excludeAll ("commons-logging", "commons-logging"),
      "eu.timepit"              %% "refined"                      % "0.9.13"
    ),
    coverageMinimum       := 40,
    coverageFailOnMinimum := true
  )
  .settings(commonSettings)
  .settings(scalacOptions := commonOptions(scalaVersion.value))

lazy val `fs2-aws-testkit` = (project in file("fs2-aws-testkit"))
  .dependsOn(`fs2-aws`)
  .settings(
    name := "fs2-aws-testkit",
    libraryDependencies ++= Seq(
      "io.circe"      %% "circe-core"              % circeVersion,
      "io.circe"      %% "circe-generic"           % circeVersion,
      "io.circe"      %% "circe-generic-extras"    % circeVersion,
      "io.circe"      %% "circe-parser"            % circeVersion,
      "org.mockito"   % "mockito-core"             % "3.3.3",
      "org.scalatest" %% "scalatest"               % "3.1.1",
      "org.mockito"   %% "mockito-scala-scalatest" % "1.11.3"
    )
  )
  .settings(commonSettings)
  .settings(scalacOptions := commonOptions(scalaVersion.value))

addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3")
addCommandAlias("format", ";scalafmt;test:scalafmt;scalafmtSbt")
addCommandAlias("checkFormat", ";scalafmtCheck;test:scalafmtCheck;scalafmtSbtCheck")

def commonOptions(scalaVersion: String) =
  CrossVersion.partialVersion(scalaVersion) match {
    case Some((2, 12)) =>
      Seq("-Ypartial-unification")
    case _ => Seq.empty
  }

lazy val commonSettings = Seq(
  organization       := "io.laserdisc",
  crossScalaVersions := supportedScalaVersions,
  scalaVersion       := scala213,
  scalacOptions ++= Seq(
    "-target:jvm-1.8",
    "-encoding",
    "UTF-8",                         // source files are in UTF-8
    "-deprecation",                  // warn about use of deprecated APIs
    "-unchecked",                    // warn about unchecked type parameters
    "-feature",                      // warn about misused language features
    "-language:higherKinds",         // allow higher kinded types without `import scala.language.higherKinds`
    "-language:implicitConversions", // allow use of implicit conversions
    "-Xlint",                        // enable handy linter warnings
    "-Xfatal-warnings"               // turn compiler warnings into errors
  ),
  addCompilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.1"),
  addCompilerPlugin("org.typelevel" %% "kind-projector"     % "0.10.3")
)

addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3")

lazy val publishSettings = Seq(
  )

inThisBuild(
  List(
    licenses := Seq(
      "MIT" -> url("https://raw.githubusercontent.com/laserdisc-io/fs2-aws/master/LICENSE")
    ),
    homepage := Some(url("https://github.com/laserdisc-io/fs2-aws/")),
    developers := List(
      Developer(
        "dmateusp",
        "Daniel Mateus Pires",
        "dmateusp@gmail.com",
        url("https://github.com/dmateusp")
      ),
      Developer("semenodm", "Dmytro Semenov", "", url("https://github.com/semenodm"))
    ),
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/laserdisc-io/fs2-aws/tree/master"),
        "scm:git:git@github.com:laserdisc-io/fs2-aws.git",
        "scm:git:git@github.com:laserdisc-io/fs2-aws.git"
      )
    ),
    publishMavenStyle      := true,
    Test / publishArtifact := true,
    pomIncludeRepository   := (_ => false),
    pgpPublicRing          := file(".travis/local.pubring.asc"),
    pgpSecretRing          := file(".travis/local.secring.asc"),
    releaseEarlyWith       := SonatypePublisher
  )
)
