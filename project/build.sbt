// Required for the freegen definition in ../build.sbt
val AwsSdk = "2.29.20"

libraryDependencies += "software.amazon.awssdk" % "sqs"        % AwsSdk
libraryDependencies += "software.amazon.awssdk" % "s3"         % AwsSdk
libraryDependencies += "software.amazon.awssdk" % "sns"        % AwsSdk
libraryDependencies += "software.amazon.awssdk" % "kinesis"    % AwsSdk
libraryDependencies += "software.amazon.awssdk" % "dynamodb"   % AwsSdk
libraryDependencies += "software.amazon.awssdk" % "cloudwatch" % AwsSdk
