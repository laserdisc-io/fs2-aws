name := "fs2-aws-testkit"

scalaVersion := "2.12.7"
val fs2Version    = "1.0.0"
val AwsSdkVersion = "1.11.427"
val cirisVersion  = "0.11.0"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest"   % "3.0.4",
  "org.mockito"   % "mockito-core" % "2.23.0"
)
