package fs2.aws

import fs2.aws.kinesis.ChunkedRecordProcessor

package object testkit {
  val TestRecordProcessor = new ChunkedRecordProcessor(_ => ())
}
