package io.laserdisc.ciris

import cats.effect.Async
import cats.implicits.{catsSyntaxApplicativeError, toFunctorOps}
import ciris.{ConfigKey, ConfigValue}
import io.laserdisc.pure.ssm.tagless.SsmAsyncClientOp
import software.amazon.awssdk.services.ssm.model.{GetParameterRequest, GetParameterResponse, ParameterNotFoundException}

package object ssm {

  implicit final class SsmOps[F[_]](private val client: SsmAsyncClientOp[F]) extends AnyVal {

    def param(ssmKey: String)(implicit F: Async[F]): ConfigValue[F, String] = {

      val configKey = ConfigKey(s"AWS SSM Key: $ssmKey")

      def toConfigValue(resp: GetParameterResponse): ConfigValue[F, String] =
        Option(resp.parameter)
          .flatMap(p => Option(p.value))
          .fold(ConfigValue.missing[String](configKey)) {
            ConfigValue.loaded(configKey, _)
          }
          .covary[F] // covary needed for scala 2.13

      val getParamReq = GetParameterRequest.builder().withDecryption(true).name(ssmKey).build()

      ConfigValue.eval {
        client
          .getParameter(getParamReq)
          .map(toConfigValue)
          .recover { case _: ParameterNotFoundException =>
            ConfigValue.missing(configKey)
          }
      }
    }

  }

}
