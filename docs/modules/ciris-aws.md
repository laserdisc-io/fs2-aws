# ciris-aws

[Ciris](https://cir.is) config loaders for AWS Secrets Manager and SSM Parameter Store. 



* Each loader is an extension method on the [pure-aws](pure-aws.md) algebra, so a value becomes a
`ConfigValue[F, A]` that you can combine with other Ciris sources.
* Additionally, having our own ciris helpers helps downstream projects avoid 
dependency conflicts within the AWS SDK v2.
* These modules were inspired by Kaluza's helpful, but now unmaintained [ciris-aws-ssm](https://github.com/kaluza-platform/ciris-aws-ssm) and
  [ciris-aws-secretsmanager](https://github.com/kaluza-platform/ciris-aws-secretsmanager) libraries.


@:include(_disclaimer.md)


## Loading AWS Secrets Manager secrets

Import:
```sbt
libraryDependencies += "io.laserdisc" %% "ciris-aws-secretsmanager" % "@VERSION@"
```

Example usage:
```scala mdoc:compile-only
import cats.effect.{IO, IOApp}
import cats.syntax.all.*
import ciris.{env, Secret}
import io.laserdisc.ciris.secretsmanager.*
import io.laserdisc.pure.secretsmanager.tagless.SecretsManagerInterpreter

object SecretsManagerExample extends IOApp.Simple {

  final case class MyAPICreds(user: Secret[String], password: Secret[String])
  
  override def run: IO[Unit] =
    SecretsManagerInterpreter[IO].resource.use { sm =>
      (
        sm.secret("/app/myapi/user"),
        sm.secret("/app/myapi/password")
      ).parMapN(MyAPICreds.apply)
        .load[IO] 
        .flatMap(cfg => IO.println(s"Loaded config: $cfg")) // note: Secret[A] values print redacted
    }
}
```

## Loading parameters from AWS SSM Parameter Store

Import:
```sbt
libraryDependencies += "io.laserdisc" %% "ciris-aws-ssm" % "@VERSION@"
```

Example usage:
```scala mdoc:compile-only
import cats.effect.{IO, IOApp}
import cats.syntax.all.*
import ciris.env
import io.laserdisc.ciris.ssm.*
import io.laserdisc.pure.ssm.tagless.SsmInterpreter

import scala.concurrent.duration.FiniteDuration

object SsmExample extends IOApp.Simple {

  final case class ServiceConfig(host: String, port: Int)

  override def run: IO[Unit] =
    SsmInterpreter[IO].resource.use { ssm =>
      (
        ssm.param("/myservice/hostname"),
        ssm.param("/myservice/port").as[Int]
      ).parMapN(ServiceConfig.apply)
        .load[IO]
        .flatMap(cfg => IO.println(s"Loaded config: $cfg"))
    }

}
```


