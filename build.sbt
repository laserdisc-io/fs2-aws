import TaglessGen.taglessGenSettings
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.sns.SnsAsyncClient
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient

organization := "io.laserdisc"
name         := "fs2-aws"

lazy val scala213 = "2.13.10"
lazy val scala3   = "3.1.1"

lazy val supportedScalaVersions = List(scala213, scala3)

ThisBuild / crossScalaVersions := supportedScalaVersions

ThisBuild / scalaVersion := scala3

lazy val root = (project in file("."))
  .aggregate(
    `fs2-aws-kinesis`,
    `fs2-aws-s3`,
    `fs2-aws-sqs`,
    `fs2-aws-testkit`,
    `fs2-aws-dynamodb`,
    `fs2-aws-sns`,
    `fs2-aws-core`,
    `fs2-aws-examples`,
    `fs2-aws-ciris`,
    `fs2-aws-benchmarks`,
    `pure-sqs-tagless`,
    `pure-sns-tagless`,
    `pure-s3-tagless`,
    `pure-cloudwatch-tagless`,
    `pure-dynamodb-tagless`,
    `pure-kinesis-tagless`
  )
  .settings(
    publishArtifact    := false,
    crossScalaVersions := Nil
  )

lazy val `fs2-aws-core` = (project in file("fs2-aws-core"))
  .settings(
    name := "fs2-aws-core",
    Dependencies.Fs2Core,
    Dependencies.Testing,
    coverageMinimumStmtTotal := 40,
    coverageFailOnMinimum    := true
  )
  .settings(commonSettings)

lazy val `fs2-aws-ciris` = (project in file("fs2-aws-ciris"))
  .settings(
    name := "fs2-aws-ciris",
    Dependencies.Ciris,
    Dependencies.KinesisClient,
    Dependencies.Testing,
    coverageMinimumStmtTotal := 40,
    coverageFailOnMinimum    := true
  )
  .settings(commonSettings)

lazy val `fs2-aws-dynamodb` = (project in file("fs2-aws-dynamodb"))
  .dependsOn(`fs2-aws-core`, `pure-dynamodb-tagless`)
  .settings(
    name                     := "fs2-aws-dynamodb",
    coverageMinimumStmtTotal := 40,
    coverageFailOnMinimum    := true,
    Dependencies.Fs2Core,
    Dependencies.DynamoStreamAdapter,
    Dependencies.ScanamoCirce("1.0.8"),
    Dependencies.Testing
  )
  .settings(commonSettings)

lazy val `fs2-aws-examples` = (project in file("fs2-aws-examples"))
  .dependsOn(
    `fs2-aws-dynamodb`,
    `pure-s3-tagless`,
    `pure-sns-tagless`,
    `pure-sqs-tagless`,
    `pure-kinesis-tagless`,
    `pure-dynamodb-tagless`,
    `pure-cloudwatch-tagless`,
    `fs2-aws-kinesis`,
    `fs2-aws-s3`
  )
  .settings(
    name                     := "fs2-aws-examples",
    coverageMinimumStmtTotal := 0,
    Dependencies.Logging,
    Dependencies.Trace4Cats,
    Dependencies.ScanamoCirce("2.1.0")
  )
  .settings(commonSettings)
  .settings(
    publish / skip := true
  )

lazy val `fs2-aws-s3` = (project in file("fs2-aws-s3"))
  .settings(
    name := "fs2-aws-s3",
    Dependencies.Fs2Core,
    Dependencies.Refined,
    Dependencies.AWS("s3"),
    Dependencies.Testing,
    coverageMinimumStmtTotal := 0,
    coverageFailOnMinimum    := true
  )
  .settings(commonSettings)
  .dependsOn(`pure-s3-tagless`)

lazy val `fs2-aws-kinesis` = (project in file("fs2-aws-kinesis"))
  .dependsOn(
    `fs2-aws-core`,
    `pure-cloudwatch-tagless`,
    `pure-dynamodb-tagless`,
    `pure-kinesis-tagless`
  )
  .settings(
    name := "fs2-aws-kinesis",
    Dependencies.Fs2Core,
    Dependencies.KinesisClient,
    Dependencies.KinesisProducer,
    Dependencies.Refined,
    Dependencies.Testing,
    coverageMinimumStmtTotal := 40,
    coverageFailOnMinimum    := true
  )
  .settings(commonSettings)

lazy val `fs2-aws-sqs` = (project in file("fs2-aws-sqs"))
  .settings(
    name := "fs2-aws-sqs",
    Dependencies.Fs2Core,
    Dependencies.AWS("sqs"),
    Dependencies.Refined,
    Dependencies.Testing,
    coverageMinimumStmtTotal := 55.80,
    coverageFailOnMinimum    := true
  )
  .settings(commonSettings)
  .dependsOn(`pure-sqs-tagless`)

lazy val `fs2-aws-sns` = (project in file("fs2-aws-sns"))
  .settings(
    name := "fs2-aws-sns",
    Dependencies.Fs2Core,
    Dependencies.AWS("sns"),
    Dependencies.AWS("sqs", Test),
    Dependencies.Testing,
    Dependencies.Refined,
    coverageMinimumStmtTotal := 55.80,
    coverageFailOnMinimum    := true
  )
  .settings(commonSettings)
  .dependsOn(`pure-sqs-tagless`, `pure-sns-tagless`)

lazy val `pure-sqs-tagless` = (project in file("pure-aws/pure-sqs-tagless"))
  .settings(
    name := "pure-sqs-tagless",
    Dependencies.AWS("sqs"),
    Dependencies.CatsEffect,
    taglessGenSettings[SqsAsyncClient]("sqs")
  )
  .settings(commonSettings)

lazy val `pure-s3-tagless` = (project in file("pure-aws/pure-s3-tagless"))
  .settings(
    name := "pure-s3-tagless",
    Dependencies.AWS("s3"),
    Dependencies.CatsEffect,
    taglessGenSettings[S3AsyncClient]("s3")
  )
  .settings(commonSettings)

lazy val `pure-sns-tagless` = (project in file("pure-aws/pure-sns-tagless"))
  .settings(
    name := "pure-sns-tagless",
    Dependencies.AWS("sns"),
    Dependencies.CatsEffect,
    taglessGenSettings[SnsAsyncClient]("sns")
  )
  .settings(commonSettings)

lazy val `pure-kinesis-tagless` = (project in file("pure-aws/pure-kinesis-tagless"))
  .settings(
    name := "pure-kinesis-tagless",
    Dependencies.AWS("kinesis"),
    Dependencies.CatsEffect,
    taglessGenSettings[KinesisAsyncClient]("kinesis")
  )
  .settings(commonSettings)

lazy val `pure-dynamodb-tagless` = (project in file("pure-aws/pure-dynamodb-tagless"))
  .settings(
    name := "pure-dynamodb-tagless",
    Dependencies.AWS("dynamodb"),
    Dependencies.CatsEffect,
    taglessGenSettings[DynamoDbAsyncClient]("dynamodb")
  )
  .settings(commonSettings)

lazy val `pure-cloudwatch-tagless` = (project in file("pure-aws/pure-cloudwatch-tagless"))
  .settings(
    name := "pure-cloudwatch-tagless",
    Dependencies.AWS("cloudwatch"),
    Dependencies.CatsEffect,
    taglessGenSettings[CloudWatchAsyncClient]("cloudwatch")
  )
  .settings(commonSettings)

lazy val `fs2-aws-testkit` = (project in file("fs2-aws-testkit"))
  .dependsOn(`fs2-aws-kinesis`)
  .settings(
    name := "fs2-aws-testkit",
    Dependencies.Circe,
    Dependencies.ScalaTest,
    Dependencies.Mockito
  )
  .settings(commonSettings)

lazy val `fs2-aws-benchmarks` = (project in file("fs2-aws-benchmarks"))
  .dependsOn(`fs2-aws-kinesis`)
  .dependsOn(`fs2-aws-testkit`)
  .settings(
    name := "fs2-aws-benchmarks",
    Dependencies.Logging,
    publishArtifact := false,
    scalacOptions ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, minor)) if minor >= 13 =>
          Seq("-Xsource:3", "-Xlint:-byname-implicit")
        case _ => Seq.empty
      }
    }
  )
  .enablePlugins(JmhPlugin)

addCommandAlias("format", ";scalafmt;test:scalafmt;scalafmtSbt")
addCommandAlias("checkFormat", ";scalafmtCheck;test:scalafmtCheck;scalafmtSbtCheck")
addCommandAlias("build", ";checkFormat;clean;+test;coverage")

lazy val commonSettings = Def.settings(
  organization := "io.laserdisc",
  developers := List(
    Developer(
      "semenodm",
      "Dmytro Semenov",
      "sdo.semenov@gmail.com",
      url("https://github.com/semenodm")
    )
  ),
  licenses ++= Seq(("MIT", url("http://opensource.org/licenses/MIT"))),
  homepage           := Some(url("https://github.com/laserdisc-io/fs2-aws")),
  crossScalaVersions := supportedScalaVersions,
  scalaVersion       := scala3,
  Test / fork        := true,
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding",
    "UTF-8",
    "-feature",
    "-language:existentials,experimental.macros,higherKinds,implicitConversions,postfixOps",
    "-unchecked",
    "-Xfatal-warnings"
  ),
  scalacOptions ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, minor)) if minor >= 13 =>
        Seq(
          "-Xlint:-unused,_",
          "-Ywarn-numeric-widen",
          "-Ywarn-value-discard",
          "-Ywarn-unused:implicits",
          "-Ywarn-unused:imports",
          "-Xsource:3",
          "-Xlint:-byname-implicit",
          "-P:kind-projector:underscore-placeholders",
          "-Xlint",             // enable handy linter warnings
          "-Ywarn-macros:after" // allows the compiler to resolve implicit imports being flagged as unused
        )
      case _ => Seq.empty
    }
  },
  scalacOptions ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((3, _)) =>
        Seq(
          "-Ykind-projector:underscores",
          "-source:future",
          "-language:adhocExtensions",
          "-Wconf:msg=`= _` has been deprecated; use `= uninitialized` instead.:s"
        )
      case _ => Seq.empty
    }
  },
  Test / console / scalacOptions := (Compile / console / scalacOptions).value,
  Test / scalacOptions           := (Compile / scalacOptions).value,
  Test / scalacOptions += "-Wconf:msg=is not declared `infix`:s,msg=is declared 'open':s",
  libraryDependencies += "org.scala-lang.modules" %% "scala-collection-compat" % "2.9.0",
  libraryDependencies ++= Seq(
    compilerPlugin(("org.typelevel" %% "kind-projector" % "0.13.2").cross(CrossVersion.full)),
    compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
  ).filterNot(_ => scalaVersion.value.startsWith("3.")),
  Seq(Compile, Test).map { config =>
    (config / unmanagedSourceDirectories) ++= {
      (config / unmanagedSourceDirectories).value.flatMap { dir: File =>
        dir.getName match {
          case "scala" =>
            CrossVersion.partialVersion(scalaVersion.value) match {
              case Some((2, 12)) => Seq(file(dir.getPath + "-3.0-"))
              case Some((2, 13)) => Seq(file(dir.getPath + "-3.0-"))
              case Some((0, _))  => Seq(file(dir.getPath + "-3.0+"))
              case Some((3, _))  => Seq(file(dir.getPath + "-3.0+"))
              case other         => sys.error(s"unmanagedSourceDirectories for scalaVersion $other not set")
            }

          case _ => Seq(dir)
        }
      }
    }
  }
)
