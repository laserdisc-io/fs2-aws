package fs2.aws.sqs

import cats.effect.Effect
import com.amazon.sqs.javamessaging.{ProviderConfiguration, SQSConnection, SQSConnectionFactory}
import com.amazonaws.services.sqs.AmazonSQSClientBuilder
import eu.timepit.refined.auto._
import javax.jms.{MessageListener, Session}

import scala.language.higherKinds

class SQSConsumerBuilder[F[_]](val sqsConfig: SqsConfig, val listener: MessageListener)(
    implicit F: Effect[F])
    extends ConsumerBuilder[F] {

  val start: F[SQSConsumer] = {
    F.delay {
      new SQSConsumer {
        override val callback: MessageListener = listener

        val connectionFactory = new SQSConnectionFactory(
          new ProviderConfiguration(),
          AmazonSQSClientBuilder.defaultClient()
        )
        override val connection: SQSConnection = connectionFactory.createConnection

        override def startConsumer: Unit = {
          val session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)
          val cons    = session.createConsumer(session.createQueue(sqsConfig.queueName))
          cons.setMessageListener(callback)
          connection.start()
        }

        override def shutdown: Unit = {
          connection.stop()
        }
      }
    }
  }
}

object SQSConsumerBuilder {
  def apply[F[_]](sqsConfig: SqsConfig, listener: MessageListener)(
      implicit F: Effect[F]): SQSConsumerBuilder[F] = {
    new SQSConsumerBuilder[F](sqsConfig, listener)
  }
}
