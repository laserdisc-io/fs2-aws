import TaglessGen.{taglessAwsService, taglessGenClasses, taglessGenDir, taglessGenPackage, taglessGenSettings}
import sbt.Keys.scalaSource
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.sns.SnsAsyncClient
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient

organization := "io.laserdisc"
name         := "fs2-aws"

lazy val scala213 = "2.13.8"
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
    libraryDependencies ++= Seq(
      "co.fs2"        %% "fs2-core"     % V.Fs2,
      "co.fs2"        %% "fs2-io"       % V.Fs2,
      "org.mockito"    % "mockito-core" % V.MockitoCore % Test,
      "org.scalatest" %% "scalatest"    % V.ScalaTest   % Test
    ),
    coverageMinimumStmtTotal := 40,
    coverageFailOnMinimum    := true
  )
  .settings(commonSettings)

lazy val `fs2-aws-ciris` = (project in file("fs2-aws-ciris"))
  .settings(
    name := "fs2-aws-ciris",
    libraryDependencies ++= Seq(
      "org.scalatest"          %% "scalatest"             % V.ScalaTest   % Test,
      "org.mockito"             % "mockito-core"          % V.MockitoCore % Test,
      "is.cir"                 %% "ciris"                 % "2.3.3",
      "software.amazon.kinesis" % "amazon-kinesis-client" % "2.4.3",
      "org.typelevel"          %% "cats-effect"           % V.CE          % Test
    ),
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
    libraryDependencies ++= Seq(
      "co.fs2"        %% "fs2-core"                         % V.Fs2,
      "co.fs2"        %% "fs2-io"                           % V.Fs2,
      "org.scalatest" %% "scalatest"                        % V.ScalaTest   % Test,
      "org.mockito"    % "mockito-core"                     % V.MockitoCore % Test,
      "com.amazonaws"  % "dynamodb-streams-kinesis-adapter" % "1.5.3"
    ),
    libraryDependencies ++= Seq("io.laserdisc" %% "scanamo-circe" % "1.0.8")
      .filterNot(_ => scalaVersion.value.startsWith("3."))
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
    libraryDependencies ++= Seq(
      "ch.qos.logback"    % "logback-classic"   % "1.4.4",
      "ch.qos.logback"    % "logback-core"      % "1.4.4",
      "org.slf4j"         % "jcl-over-slf4j"    % "2.0.0",
      "org.slf4j"         % "jul-to-slf4j"      % "2.0.0",
      "org.typelevel"    %% "log4cats-slf4j"    % "2.4.0",
      "io.janstenpickle" %% "trace4cats-inject" % "0.13.1"
    ),
    libraryDependencies ++= Seq(
      "io.laserdisc" %% "scanamo-circe" % "2.1.0"
    ).filterNot(_ => scalaVersion.value.startsWith("3."))
  )
  .settings(commonSettings)
  .settings(
    publish / skip := true
  )

lazy val `fs2-aws-s3` = (project in file("fs2-aws-s3"))
  .settings(
    name := "fs2-aws-s3",
    libraryDependencies ++= Seq(
      "co.fs2"                %% "fs2-core"            % V.Fs2,
      "co.fs2"                %% "fs2-io"              % V.Fs2,
      "eu.timepit"            %% "refined"             % V.Refined,
      "software.amazon.awssdk" % "s3"                  % V.AwsSdk,
      "org.scalameta"         %% "munit"               % V.Munit % Test,
      "org.typelevel"         %% "munit-cats-effect-3" % "1.0.7" % Test
    ),
    testFrameworks += new TestFramework("munit.Framework"),
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
    libraryDependencies ++= Seq(
      "co.fs2"                 %% "fs2-core"                % V.Fs2,
      "co.fs2"                 %% "fs2-io"                  % V.Fs2,
      "com.amazonaws"           % "amazon-kinesis-producer" % "0.14.11",
      "software.amazon.kinesis" % "amazon-kinesis-client"   % "2.4.3",
      "eu.timepit"             %% "refined"                 % V.Refined,
      "org.scalatest"          %% "scalatest"               % V.ScalaTest   % Test,
      "org.mockito"             % "mockito-core"            % V.MockitoCore % Test,
      "ch.qos.logback"          % "logback-classic"         % "1.4.4"       % Test,
      "ch.qos.logback"          % "logback-core"            % "1.4.4"       % Test
    ),
    coverageMinimumStmtTotal := 40,
    coverageFailOnMinimum    := true
  )
  .settings(commonSettings)

lazy val `fs2-aws-sqs` = (project in file("fs2-aws-sqs"))
  .settings(
    name := "fs2-aws-sqs",
    libraryDependencies ++= Seq(
      "co.fs2"                %% "fs2-core"     % V.Fs2,
      "co.fs2"                %% "fs2-io"       % V.Fs2,
      "software.amazon.awssdk" % "sqs"          % V.AwsSdk,
      "org.mockito"            % "mockito-core" % V.MockitoCore % Test,
      "org.scalatest"         %% "scalatest"    % V.ScalaTest   % Test,
      "eu.timepit"            %% "refined"      % V.Refined
    ),
    coverageMinimumStmtTotal := 55.80,
    coverageFailOnMinimum    := true
  )
  .settings(commonSettings)
  .dependsOn(`pure-sqs-tagless`)

lazy val `fs2-aws-sns` = (project in file("fs2-aws-sns"))
  .settings(
    name := "fs2-aws-sns",
    libraryDependencies ++= Seq(
      "co.fs2"                %% "fs2-core"     % V.Fs2,
      "co.fs2"                %% "fs2-io"       % V.Fs2,
      "software.amazon.awssdk" % "sns"          % V.AwsSdk,
      "org.mockito"            % "mockito-core" % V.MockitoCore % Test,
      "org.scalatest"         %% "scalatest"    % V.ScalaTest   % Test,
      "software.amazon.awssdk" % "sqs"          % V.AwsSdk      % Test,
      "eu.timepit"            %% "refined"      % V.Refined,
      "org.typelevel"         %% "cats-effect"  % V.CE
    ),
    coverageMinimumStmtTotal := 55.80,
    coverageFailOnMinimum    := true
  )
  .settings(commonSettings)
  .dependsOn(`pure-sqs-tagless`, `pure-sns-tagless`)

lazy val `pure-sqs-tagless` = (project in file("pure-aws/pure-sqs-tagless"))
  .settings(taglessGenSettings)
  .settings(
    name := "pure-sqs-tagless",
    libraryDependencies ++= Seq(
      "software.amazon.awssdk" % "sqs"         % V.AwsSdk,
      "org.typelevel"         %% "cats-effect" % V.CE
    ),
    taglessGenDir     := (Compile / scalaSource).value / "io" / "laserdisc" / "pure" / "sqs" / "tagless",
    taglessGenPackage := "io.laserdisc.pure.sqs.tagless",
    taglessAwsService := "sqs",
    taglessGenClasses := {
      List[Class[_]](
        classOf[SqsAsyncClient]
      )
    }
  )
  .settings(commonSettings)

lazy val `pure-s3-tagless` = (project in file("pure-aws/pure-s3-tagless"))
  .settings(taglessGenSettings)
  .settings(
    name := "pure-s3-tagless",
    libraryDependencies ++= Seq(
      "software.amazon.awssdk" % "s3"          % V.AwsSdk,
      "org.typelevel"         %% "cats-effect" % V.CE
    ),
    taglessGenDir     := (Compile / scalaSource).value / "io" / "laserdisc" / "pure" / "s3" / "tagless",
    taglessGenPackage := "io.laserdisc.pure.s3.tagless",
    taglessAwsService := "s3",
    taglessGenClasses := {
      List[Class[_]](
        classOf[S3AsyncClient]
      )
    }
  )
  .settings(commonSettings)

lazy val `pure-sns-tagless` = (project in file("pure-aws/pure-sns-tagless"))
  .settings(taglessGenSettings)
  .settings(
    name := "pure-sns-tagless",
    libraryDependencies ++= Seq(
      "software.amazon.awssdk" % "sns"         % V.AwsSdk,
      "org.typelevel"         %% "cats-effect" % V.CE
    ),
    taglessGenDir     := (Compile / scalaSource).value / "io" / "laserdisc" / "pure" / "sns" / "tagless",
    taglessGenPackage := "io.laserdisc.pure.sns.tagless",
    taglessAwsService := "sns",
    taglessGenClasses := {
      List[Class[_]](
        classOf[SnsAsyncClient]
      )
    }
  )
  .settings(commonSettings)

lazy val `pure-kinesis-tagless` = (project in file("pure-aws/pure-kinesis-tagless"))
  .settings(taglessGenSettings)
  .settings(
    name := "pure-kinesis-tagless",
    libraryDependencies ++= Seq(
      "software.amazon.awssdk" % "kinesis"     % V.AwsSdk,
      "org.typelevel"         %% "cats-effect" % V.CE
    ),
    taglessGenDir     := (Compile / scalaSource).value / "io" / "laserdisc" / "pure" / "kinesis" / "tagless",
    taglessGenPackage := "io.laserdisc.pure.kinesis.tagless",
    taglessAwsService := "kinesis",
    taglessGenClasses := {
      List[Class[_]](
        classOf[KinesisAsyncClient]
      )
    }
  )
  .settings(commonSettings)

lazy val `pure-dynamodb-tagless` = (project in file("pure-aws/pure-dynamodb-tagless"))
  .settings(taglessGenSettings)
  .settings(
    name := "pure-dynamodb-tagless",
    libraryDependencies ++= Seq(
      "software.amazon.awssdk" % "dynamodb"    % V.AwsSdk,
      "org.typelevel"         %% "cats-effect" % V.CE
    ),
    taglessGenDir     := (Compile / scalaSource).value / "io" / "laserdisc" / "pure" / "dynamodb" / "tagless",
    taglessGenPackage := "io.laserdisc.pure.dynamodb.tagless",
    taglessAwsService := "dynamodb",
    taglessGenClasses := {
      List[Class[_]](
        classOf[DynamoDbAsyncClient]
      )
    }
  )
  .settings(commonSettings)

lazy val `pure-cloudwatch-tagless` = (project in file("pure-aws/pure-cloudwatch-tagless"))
  .settings(taglessGenSettings)
  .settings(
    name := "pure-cloudwatch-tagless",
    libraryDependencies ++= Seq(
      "software.amazon.awssdk" % "cloudwatch"  % V.AwsSdk,
      "org.typelevel"         %% "cats-effect" % V.CE
    ),
    taglessGenDir     := (Compile / scalaSource).value / "io" / "laserdisc" / "pure" / "cloudwatch" / "tagless",
    taglessGenPackage := "io.laserdisc.pure.cloudwatch.tagless",
    taglessAwsService := "cloudwatch",
    taglessGenClasses := {
      List[Class[_]](
        classOf[CloudWatchAsyncClient]
      )
    }
  )
  .settings(commonSettings)

lazy val `fs2-aws-testkit` = (project in file("fs2-aws-testkit"))
  .dependsOn(`fs2-aws-kinesis`)
  .settings(
    name := "fs2-aws-testkit",
    libraryDependencies ++= Seq(
      "io.circe"      %% "circe-core"    % V.Circe,
      "io.circe"      %% "circe-generic" % V.Circe,
      "io.circe"      %% "circe-parser"  % V.Circe,
      "org.scalatest" %% "scalatest"     % V.ScalaTest,
      "org.mockito"    % "mockito-core"  % V.MockitoCore
    )
  )
  .settings(commonSettings)

lazy val `fs2-aws-benchmarks` = (project in file("fs2-aws-benchmarks"))
  .dependsOn(`fs2-aws-kinesis`)
  .dependsOn(`fs2-aws-testkit`)
  .settings(
    name := "fs2-aws-benchmarks",
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % "1.4.4",
      "ch.qos.logback" % "logback-core"    % "1.4.4",
      "org.slf4j"      % "jcl-over-slf4j"  % "2.0.0",
      "org.slf4j"      % "jul-to-slf4j"    % "2.0.0"
    ),
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
  libraryDependencies += "org.scala-lang.modules" %% "scala-collection-compat" % "2.8.1",
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
