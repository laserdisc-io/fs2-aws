package fs2.aws.testkit
import cats.effect.Effect
import com.amazon.sqs.javamessaging.SQSConnection
import fs2.aws.testkit.TestSqsConsumerBuilder.TestSQSConsumer
import fs2.aws.sqs.{ConsumerBuilder, SQSConsumer}
import javax.jms.MessageListener
import org.scalatestplus.mockito.MockitoSugar

class TestSqsConsumerBuilder[F[_]: Effect] extends ConsumerBuilder[F] {
  override def start: F[SQSConsumer] =
    Effect[F].delay(new TestSQSConsumer)
}

object TestSqsConsumerBuilder extends MockitoSugar {
  class TestSQSConsumer extends SQSConsumer {
    override def callback: MessageListener = mock[MessageListener]
    override def startConsumer(): Unit     = ()
    override def shutdown(): Unit          = ()
    override def connection: SQSConnection = mock[SQSConnection]
  }
}
