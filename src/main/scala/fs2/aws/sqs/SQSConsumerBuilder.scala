package fs2.aws.sqs

import cats.effect.Effect
import com.amazon.sqs.javamessaging.{ProviderConfiguration, SQSConnection, SQSConnectionFactory}
import com.amazonaws.services.sqs.AmazonSQSClientBuilder
import eu.timepit.refined.auto._
import javax.jms.{MessageListener, Session}

import scala.language.higherKinds

class SQSConsumerBuilder[F[_]](val sqsConfig: SqsConfig, val listener: MessageListener)(
    implicit F: Effect[F]) {

  val start: F[SQSConsumer[F]] = {
    F.delay {
      new SQSConsumer[F] {
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

  def serve[A](stream: fs2.Stream[F, A]): fs2.Stream[F, A] = {
    fs2.Stream
      .bracket(start)(con => F.delay(con.shutdown()))
      .flatMap(con => fs2.Stream.eval(F.delay(con.startConsumer())).drain ++ stream)
  }
}

object SQSConsumerBuilder {
  def apply[F[_]](sqsConfig: SqsConfig, listener: MessageListener)(
      implicit F: Effect[F]): SQSConsumerBuilder[F] = {
    new SQSConsumerBuilder[F](sqsConfig, listener)
  }
}
