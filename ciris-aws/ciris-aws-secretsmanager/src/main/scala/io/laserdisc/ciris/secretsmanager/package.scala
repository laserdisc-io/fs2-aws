package io.laserdisc.ciris

import cats.effect.Async
import cats.implicits.{catsSyntaxApplicativeError, toFunctorOps}
import ciris.{ConfigKey, ConfigValue, Secret}
import io.laserdisc.pure.secretsmanager.tagless.SecretsManagerAsyncClientOp
import software.amazon.awssdk.services.secretsmanager.model.{
  GetSecretValueRequest,
  GetSecretValueResponse,
  ResourceNotFoundException
}

package object secretsmanager {

  implicit final class SecretsManagerOps[F[_]](private val client: SecretsManagerAsyncClientOp[F]) extends AnyVal {

    def secret(secretId: String)(implicit F: Async[F]): ConfigValue[F, Secret[String]] = {

      val configKey = ConfigKey(s"AWS Secrets Manager Key: $secretId")

      def toConfigValue(resp: GetSecretValueResponse): ConfigValue[F, Secret[String]] =
        Option(resp.secretString)
          .fold(ConfigValue.missing[String](configKey)) {
            ConfigValue.loaded(configKey, _)
          }
          .covary[F] // covary needed for scala 2.13
          .secret

      val getSecretReq = GetSecretValueRequest.builder().secretId(secretId).build()

      ConfigValue.eval {
        client
          .getSecretValue(getSecretReq)
          .map(toConfigValue)
          .recover { case _: ResourceNotFoundException =>
            ConfigValue.missing(configKey)
          }
      }
    }

  }

}
