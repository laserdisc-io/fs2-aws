package fs2.aws

import scala.concurrent.duration._

package object testkit {
  val TestRecordProcessor = new kinesis.SingleRecordProcessor(_ => (), 10.seconds)
}
