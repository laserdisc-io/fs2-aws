package fs2.aws.sqs

import javax.jms.{Message, MessageListener}

import cats.syntax.either._
import scala.util.control.Exception

class ReceiverCallback[A](f: A => Unit)(implicit messageParser: Message => A)
    extends MessageListener {
  def onMessage(message: Message): Unit = {
    Exception.nonFatalCatch either {
      messageParser.andThen(f)(message)
      message.acknowledge()
    } leftMap (e => System.err.println("Error processing message: " + e.getMessage))
  }

}
