generate-pure-aws-algebras:
	sbt "project pure-sns-tagless" taglessGen
	sbt "project pure-sqs-tagless" taglessGen
	sbt "project pure-kinesis-tagless" taglessGen
	sbt "project pure-dynamodb-tagless" taglessGen
	sbt "project pure-cloudwatch-tagless" taglessGen
	sbt "project pure-s3-tagless" taglessGen
