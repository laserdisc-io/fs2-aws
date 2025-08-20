#!/bin/bash

export AWS_ACCESS_KEY_ID=dummy
export AWS_SECRET_ACCESS_KEY=dummy
export AWS_ENDPOINT_URL=http://localhost:4566

set -e -o pipefail

echo "Loading available streams..."

available_streams=$(
  aws kinesis list-streams --output text --query 'StreamNames[]' \
  | xargs -n1 -I{} aws kinesis describe-stream-summary --stream-name {} \
  | jq -r '.StreamDescriptionSummary | "Created \(.StreamCreationTimestamp | split(".")[0]+"Z" | fromdateiso8601 | strflocaltime("%b %d %H:%M:%S"))   shards:\(.OpenShardCount)  status:\(.StreamStatus)   name:\(.StreamName)"' \
  | sort
)

[ -z "$available_streams" ] && { echo "No streams available, exiting."; exit 1; }

# prompt to select a stream
selected_line=$(echo "$available_streams" | fzf --prompt "Select stream to dump" --height 40% --layout=reverse --border)

stream_name=$(echo "$selected_line" | sed -n 's/.*name:\([^ ]*\).*/\1/p')

echo "Selected: $stream_name"

shard_iterator=$(
  aws kinesis get-shard-iterator \
  --shard-id shardId-000000000000 \
  --shard-iterator-type TRIM_HORIZON \
  --stream-name "$stream_name" \
  --query 'ShardIterator' \
  --output text
)
echo "Loaded shard iterator (${shard_iterator:0:30}...), looking for records."

records=$(aws kinesis get-records --shard-iterator "$shard_iterator" --limit 10000 | jq '.Records[]')


num_records=$(echo "$records" | jq -s 'length')
printf "\n\nFound %s record(s):\n\n" "$num_records"

# iterate over records
echo "$records" | jq '.' | while read -r record; do
  partition_key=$(echo "$record" | jq)
  echo $partition_key
#  sequence_number=$(echo "$record" | jq -r '.SequenceNumber')
#  data=$(echo "$record" | jq -r '.Data')
#  printf "PartitionKey: %s\nSequenceNumber: %s\nData (base64): %s\n\n" "$partition_key" "$sequence_number" "$data"
done

#echo data above is base64 encoded, example to decode:
#echo 'echo "eyJza3VfaWQiOjI0MjY4MTcsInZlbmRvcl9zdHlsZV9pZCI6NzMwNzEwfQ==" | base64 --decode'