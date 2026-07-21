# fs2-aws

![Build](https://github.com/laserdisc-io/fs2-aws/workflows/Build/badge.svg)
![Release](https://github.com/laserdisc-io/fs2-aws/workflows/Release/badge.svg)
[![Maven Central](https://img.shields.io/maven-central/v/io.laserdisc/fs2-aws-s3_3)](https://central.sonatype.com/search?q=io.laserdisc.fs2-aws)
[![Coverage Status](https://coveralls.io/repos/github/laserdisc-io/fs2-aws/badge.svg?branch=main)](https://coveralls.io/github/laserdisc-io/fs2-aws?branch=main)

Provides [FS2](https://fs2.io) streaming utilities for interacting with AWS.

This project wraps the [AWS SDK v2](https://github.com/aws/aws-sdk-java-v2) S3, SQS, SNS, DynamoDB and Cloudwatch clients in purely functional, resource-safe APIs built
on [cats-effect](https://typelevel.org/cats-effect/), and offers an FS2 streaming interface for common operations.

In addition, the kinesis module provides FS2-streaming support for consuming and producing records via:
* The [Kinesis Client Library (KCL)](https://github.com/awslabs/amazon-kinesis-client)
* The [Kinesis Producer Library (KPL)](https://github.com/awslabs/amazon-kinesis-producer)

For full details, visit [fs2aws.laserdisc.io](https://fs2aws.laserdisc.io).

> [!NOTE]
> `main` contains the unreleased 7.x series. For the currently published 6.x library,
> see [`series/6.x`](https://github.com/laserdisc-io/fs2-aws/tree/series/6.x).

## License

This software is licensed under the [MIT license](LICENSE).

## Acknowledgments

[![YourKit](https://www.yourkit.com/images/yklogo.png)](https://www.yourkit.com/)

Special thanks to [YourKit](https://www.yourkit.com/) for supporting this project with
their innovative and intelligent tools for monitoring and profiling Java and .NET
applications: [YourKit Java Profiler](https://www.yourkit.com/java/profiler/),
[YourKit .NET Profiler](https://www.yourkit.com/.net/profiler/), and
[YourKit YouMonitor](https://www.yourkit.com/youmonitor/).
