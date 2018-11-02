package fs2.aws.sqs

import com.amazon.sqs.javamessaging.SQSConnection
import javax.jms.MessageListener

trait SQSConsumer[F[_]] {
  def callback: MessageListener
  def startConsumer()
  def shutdown()
  def connection: SQSConnection
}
