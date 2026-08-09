#!/usr/bin/env bash
# Runs automatically inside the LocalStack container once its services are
# ready (LocalStack init hook: /etc/localstack/init/ready.d/). No manual
# step is needed after `docker compose up`.
set -euo pipefail

BUCKET_NAME="${S3_BUCKET_NAME:-meeting-ai-recordings}"
QUEUE_NAME="${SQS_QUEUE_NAME:-meeting-ai-processing}"

echo "[init-aws] creating S3 bucket: ${BUCKET_NAME}"
awslocal s3 mb "s3://${BUCKET_NAME}"

echo "[init-aws] creating SQS queue: ${QUEUE_NAME}"
awslocal sqs create-queue --queue-name "${QUEUE_NAME}"

echo "[init-aws] done"
