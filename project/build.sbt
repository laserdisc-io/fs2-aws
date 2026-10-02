// taglessGen (project/TaglessGen.scala) reflects over this SDK to generate the pure-aws sources.
// AwsSdk must equal V.AwsSdk in project/Dependencies.scala, or the root build.sbt refuses to load. See AGENTS.md.
val AwsSdk = "2.54.16"

libraryDependencies += "software.amazon.awssdk" % "sqs"        % AwsSdk
libraryDependencies += "software.amazon.awssdk" % "s3"         % AwsSdk
libraryDependencies += "software.amazon.awssdk" % "sns"        % AwsSdk
libraryDependencies += "software.amazon.awssdk" % "kinesis"    % AwsSdk
libraryDependencies += "software.amazon.awssdk" % "dynamodb"   % AwsSdk
libraryDependencies += "software.amazon.awssdk" % "cloudwatch" % AwsSdk
