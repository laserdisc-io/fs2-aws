name := "fs2-aws-core"

// coverage
coverageMinimum       := 40
coverageFailOnMinimum := true

scalaVersion := "2.12.10"

val fs2Version    = "2.2.2"

libraryDependencies ++= Seq(
  "co.fs2"        %% "fs2-core"                        % fs2Version,
  "co.fs2"        %% "fs2-io"                          % fs2Version,
  "org.typelevel" %% "alleycats-core"                  % "2.1.0",
  "org.scalatest" %% "scalatest"                       % "3.1.0" % Test,
  "org.mockito"   % "mockito-core"                     % "3.2.4" % Test,
  "org.mockito"   %% "mockito-scala-scalatest"         % "1.11.2" % Test,
)
