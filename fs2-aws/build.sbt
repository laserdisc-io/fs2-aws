name := "fs2-aws"

// coverage
coverageMinimum := 40
coverageFailOnMinimum := true

scalaVersion := "2.12.7"

pgpPublicRing := file(".travis/local.pubring.asc")
pgpSecretRing := file(".travis/local.secring.asc")
releasePublishArtifactsAction := PgpKeys.publishSigned.value
pgpPassphrase := sys.env.get("PGP_PASS").map(_.toCharArray)

val fs2Version    = "1.0.2"
val AwsSdkVersion = "1.11.456"
val cirisVersion  = "0.11.0"

libraryDependencies ++= Seq(
  "co.fs2"        %% "fs2-core"                     % fs2Version,
  "co.fs2"        %% "fs2-io"                       % fs2Version,
  "org.typelevel" %% "alleycats-core"               % "1.4.0",
  "com.amazonaws" % "aws-java-sdk-kinesis"          % AwsSdkVersion,
  "com.amazonaws" % "aws-java-sdk-s3"               % AwsSdkVersion,
  "com.amazonaws" % "aws-java-sdk-sqs"              % AwsSdkVersion,
  "com.amazonaws" % "amazon-kinesis-producer"       % "0.12.9",
  "com.amazonaws" % "amazon-kinesis-client"         % "1.9.2",
  "org.scalatest" %% "scalatest"                    % "3.0.5" % Test,
  "org.mockito"   % "mockito-core"                  % "2.23.4" % Test,
  "com.amazonaws" % "aws-java-sdk-sqs"              % AwsSdkVersion excludeAll ("commons-logging", "commons-logging"),
  "com.amazonaws" % "amazon-sqs-java-messaging-lib" % "1.0.4" excludeAll ("commons-logging", "commons-logging"),
  "is.cir"        %% "ciris-core"                   % cirisVersion,
  "is.cir"        %% "ciris-enumeratum"             % cirisVersion,
  "is.cir"        %% "ciris-refined"                % cirisVersion,
  "eu.timepit"    %% "refined"                      % "0.9.2"
)
