package sqs
import software.amazon.awssdk.services.sqs.model.*

import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*

case class SqsConfig(
    override val queueUrl: String,
    override val pollRate: FiniteDuration = 3.seconds,
    override val fetchMessageCount: Int = 100,
    override val messageAttributeNames: Option[List[String]] = None
) extends SqsConfig.Interface

object SqsConfig {
  trait Interface {
    def queueUrl: String
    def pollRate: FiniteDuration
    def fetchMessageCount: Int
    def messageAttributeNames: Option[List[String]]

    def receiveMessageRequest: ReceiveMessageRequest = buildReceiveRequest.build()

    protected def buildReceiveRequest: ReceiveMessageRequest.Builder = {
      val blder = ReceiveMessageRequest
        .builder()
        .queueUrl(queueUrl)
        .maxNumberOfMessages(fetchMessageCount)

      messageAttributeNames.fold(blder)(names => blder.messageAttributeNames(names.asJava))
    }
  }
}
