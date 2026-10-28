package io.laserdisc.ciris.secretsmanager;

import cats.effect.IO
import ciris.ConfigException
import io.laserdisc.pure.secretsmanager.tagless.*
import munit.CatsEffectSuite
import software.amazon.awssdk.services.secretsmanager.model.*

class SecretsManagerOpsSpec extends CatsEffectSuite {

  val sm: SecretsManagerAsyncClientOp[IO] = new MockSecretsManagerAsyncClientOp[IO] {
    override def getSecretValue(req: GetSecretValueRequest): IO[GetSecretValueResponse] =
      req.secretId() match {
        case "/cat" => IO.pure(GetSecretValueResponse.builder().secretString("miaow").build())
        case "/dog" => IO.pure(GetSecretValueResponse.builder().secretString("woof").build())
        case _      => IO.raiseError(ResourceNotFoundException.builder().message("kaboom").build())
      }
  }

  test("should load secrets") {
    sm.secret("/cat").load[IO].map(s => assertEquals(s.value, "miaow"))
    sm.secret("/dog").load[IO].map(s => assertEquals(s.value, "woof"))
  }

  test("should throw a ciris error for non-existent secret") {
    sm.secret("/does-not-exist").load[IO].attempt.map {
      case Left(e: ConfigException) =>
        assertEquals(e.error.messages.headOption.get, "Missing aWS Secrets Manager Key: /does-not-exist")
      case other =>
        fail("Expected a ciris ConfigException, but got: " + other)
    }
  }
}
