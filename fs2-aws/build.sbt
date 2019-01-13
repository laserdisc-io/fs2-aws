name := "fs2-aws"

// coverage
coverageMinimum := 40
coverageFailOnMinimum := true

scalaVersion := "2.12.7"

releasePublishArtifactsAction := PgpKeys.publishSigned.value

val fs2Version    = "1.0.2"
val AwsSdkVersion = "2.2.0"
val cirisVersion  = "0.11.0"

libraryDependencies ++= Seq(
  "co.fs2"                 %% "fs2-core"                     % fs2Version,
  "co.fs2"                 %% "fs2-io"                       % fs2Version,
  "org.typelevel"          %% "alleycats-core"               % "1.4.0",
  "software.amazon.awssdk" % "s3"                            % AwsSdkVersion,
  "com.amazonaws"          % "amazon-kinesis-producer"       % "0.12.11",
  "com.amazonaws"          % "amazon-kinesis-client"         % "2.0.5",
  "com.amazonaws"          % "amazon-sqs-java-messaging-lib" % "1.0.4",
  "org.scalatest"          %% "scalatest"                    % "3.0.5" % Test,
  "org.mockito"            % "mockito-core"                  % "2.23.4" % Test,
  "is.cir"                 %% "ciris-core"                   % cirisVersion,
  "is.cir"                 %% "ciris-enumeratum"             % cirisVersion,
  "is.cir"                 %% "ciris-refined"                % cirisVersion,
  "eu.timepit"             %% "refined"                      % "0.9.2"
)
