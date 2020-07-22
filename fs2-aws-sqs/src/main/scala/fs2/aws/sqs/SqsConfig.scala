package fs2.aws.sqs

import eu.timepit.refined.types.string.TrimmedString

case class SqsConfig(queueName: TrimmedString)
