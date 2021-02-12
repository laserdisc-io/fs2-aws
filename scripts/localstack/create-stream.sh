#!/usr/bin/env bash
set -x
awslocal kinesis create-stream --stream-name test --shard-count 1
set +x
