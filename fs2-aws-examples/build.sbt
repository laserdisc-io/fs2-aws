name := "fs2-aws-examples"

// coverage
coverageMinimum       := 40
coverageFailOnMinimum := true

scalaVersion := "2.12.10"

releasePublishArtifactsAction := PgpKeys.publishSigned.value

val fs2Version    = "2.2.2"
val AwsSdkVersion = "1.11.716"
val cirisVersion  = "0.12.1"

libraryDependencies ++= Seq(
  "org.mockito"       % "mockito-core"             % "3.2.4" % Test,
  "org.mockito"       %% "mockito-scala-scalatest" % "1.11.2" % Test,
  "ch.qos.logback"    % "logback-classic"          % "1.2.3",
  "ch.qos.logback"    % "logback-core"             % "1.2.3",
  "org.slf4j"         % "jcl-over-slf4j"           % "1.7.30",
  "org.slf4j"         % "jul-to-slf4j"             % "1.7.30",
  "io.chrisdavenport" %% "log4cats-slf4j"          % "1.0.1",
  "io.github.howardjohn" %% "scanamo-circe"       % "0.2.2-SNAPSHOT"
)

addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
addCompilerPlugin("org.spire-math" %% "kind-projector"     % "0.9.3")
