name := "fs2-aws-testkit"

scalaVersion := "2.12.9"

releasePublishArtifactsAction := PgpKeys.publishSigned.value

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest"   % "3.0.8",
  "org.mockito"   % "mockito-core" % "3.1.0"
)
