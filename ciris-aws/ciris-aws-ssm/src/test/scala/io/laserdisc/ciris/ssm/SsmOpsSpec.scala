package io.laserdisc.ciris.ssm

import cats.effect.IO
import ciris.ConfigException
import io.laserdisc.pure.ssm.tagless.*
import munit.CatsEffectSuite
import software.amazon.awssdk.services.ssm.model.*

class SsmOpsSpec extends CatsEffectSuite {

  val ssm: SsmAsyncClientOp[IO] = new MockSsmAsyncClientOp[IO] {
    override def getParameter(req: GetParameterRequest): IO[GetParameterResponse] =
      req.name() match {
        case "/cat" =>
          IO.pure(GetParameterResponse.builder().parameter(Parameter.builder().value("miaow").build()).build())
        case "/dog" =>
          IO.pure(GetParameterResponse.builder().parameter(Parameter.builder().value("woof").build()).build())
        case _ => IO.raiseError(ParameterNotFoundException.builder().message("kaboom").build())
      }
  }

  test("should load params") {
    ssm.param("/cat").load[IO].map(v => assertEquals(v, "miaow"))
    ssm.param("/dog").load[IO].map(v => assertEquals(v, "woof"))
  }

  test("should throw a ciris error for non-existent param") {
    ssm.param("/does-not-exist").load[IO].attempt.map {
      case Left(e: ConfigException) =>
        assertEquals(e.error.messages.headOption.get, "Missing aWS SSM Key: /does-not-exist")
      case other =>
        fail("Expected a ciris ConfigException, but got: " + other)
    }
  }
}
