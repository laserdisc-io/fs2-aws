package fs2
package aws
package sqs

import eu.timepit.refined.types.string.TrimmedString

case class SqsConfig(queueName: TrimmedString)
