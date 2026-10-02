# AGENTS.md

Notes for coding agents and contributors working in this repository.

## Build and test

- `sbt +Test/compile +doc` compiles every module, its tests and its Scaladoc on all cross-built Scala versions. It needs no AWS access.
- `sbt siteDocs/mdoc` compiles the examples in `docs/`.
- `sbt format` formats the code with the scalafmt version in `.scalafmt.conf`.
- The tests run against LocalStack from `docker-compose.yml`. CI runs them like this:

  ```sh
  docker compose up -d
  AWS_ACCESS_KEY_ID=dummy AWS_SECRET_ACCESS_KEY=dummy sbt +clean +test +doc
  docker compose down
  ```

## The pure-aws sources are generated

The `pure-aws/pure-*-tagless` modules wrap six AWS SDK v2 async clients: CloudWatch, DynamoDB, Kinesis, S3, SNS and SQS. Each module has two generated files. For SQS, they are:

- `SqsAsyncClientOp.scala`, the tagless-final algebra
- `SqsInterpreter.scala`, which implements the algebra over the SDK client

Don't edit these files by hand. To change what gets generated, change `project/TaglessGen.scala` and regenerate.

The `taglessGen` sbt task writes the files. It reflects over the SDK on the sbt meta-build classpath. So it sees `AwsSdk` in `project/build.sbt`, not `V.AwsSdk` in `project/Dependencies.scala`, which the modules compile against.

### Regenerate after an SDK version change

1. Set `AwsSdk` in `project/build.sbt` and `V.AwsSdk` in `project/Dependencies.scala` to the new version.
2. Run `make generate-pure-aws`. It runs `taglessGen` for each client, then formats the code.
3. Run `sbt +Test/compile`.
4. If code in this repository fails to compile, update it. A hand-written implementation of an algebra needs the new methods added and the removed ones deleted. `S3OpsStub` in `fs2-aws-benchmarks` is one, and `???` bodies are fine there.
5. Commit the version change and the regenerated files together.
6. In the PR description, list the added and removed algebra methods.

Each new SDK method becomes a new abstract method on an algebra. `taglessGen` skips deprecated SDK methods, so a method that the SDK deprecates disappears from the algebra. Added methods break code outside this repository that implements an algebra. Removed methods break code that calls or overrides them.

### CI checks

- sbt refuses to load when the two SDK versions differ.
- The Build workflow runs `make generate-pure-aws`. It fails if that modifies or adds any file under `pure-aws/`.

Mergify merges a Scala Steward PR only when the Build workflow passes. When a Steward SDK bump fails either check, fix it on the Steward branch:

1. Run `gh pr checkout <number>`.
2. Follow the regeneration steps above.
3. Push. Maintainers can push to Steward branches.
