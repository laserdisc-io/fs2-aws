generate-pure-aws:
	sbt \
	  pure-cloudwatch-tagless/taglessGen \
	  pure-dynamodb-tagless/taglessGen \
	  pure-kinesis-tagless/taglessGen \
	  pure-s3-tagless/taglessGen \
	  pure-sns-tagless/taglessGen \
	  pure-sqs-tagless/taglessGen \
	  format
