# fs2-aws
[![Build Status](https://travis-ci.com/dmateusp/fs2-aws.svg?branch=master)](https://travis-ci.com/dmateusp/fs2-aws)


fs2 Streaming utilities for interacting with AWS

## S3
* Downloading / reading an `S3` file to `Byte`s, the size of each part downloaded is the `chunkSize`
`readS3FileMultipart[F[_]](bucket: String, key: String, chunkSize: Int, s3Client: S3Client[F] = new S3Client[F] {})(implicit F: Effect[F]): fs2.Stream[F, Byte]`

* Uploading multipart `Byte`s to `S3`, the size of each part uploaded is the `chunkSize` `uploadS3FileMultipart[F[_]](bucket: String, key: String, chunkSize: Int, s3Client: S3Client[F] = new S3Client[F] {})(implicit F: Effect[F]): fs2.Sink[F, Byte]`

## Kinesis
**TODO:** Stream get data, Stream send data

## Kinesis Firehose
**TODO:** Stream get data, Stream send data

## SQS
**TODO:** Stream get SQS messages, Stream send SQS messages