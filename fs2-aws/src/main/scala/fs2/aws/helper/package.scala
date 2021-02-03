package fs2.aws

import cats.effect.Async

import java.util.concurrent.{CompletableFuture, CompletionException}

package object helper {
  object CompletableFutureLift {
    def eff[F[_] : Async, A <: AnyRef](fut: => CompletableFuture[A]): F[A] =
      Async[F].async { cb =>
        fut.handle[Unit] { (a, x) =>
          if (a eq null)
            x match {
              case t: CompletionException => cb(Left(t.getCause))
              case t => cb(Left(t))
            }
          else
            cb(Right(a))
        }
        ()
      }
  }
}
