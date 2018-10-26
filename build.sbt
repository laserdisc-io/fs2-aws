name := "fs2-aws"
organization := "com.hbc"
version := "0.0.2-SNAPSHOT"

scalaVersion := "2.12.7"

scalacOptions ++= Seq(
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

val fs2Version = "1.0.0"

libraryDependencies ++= Seq(
  "co.fs2"        %% "fs2-core"               % fs2Version,
  "co.fs2"        %% "fs2-io"                 % fs2Version,
  "com.amazonaws" % "aws-java-sdk"            % "1.11.427",
  "com.amazonaws" % "amazon-kinesis-producer" % "0.12.9",
  "org.scalatest" %% "scalatest"              % "3.0.4" % "test"
)

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.3")
