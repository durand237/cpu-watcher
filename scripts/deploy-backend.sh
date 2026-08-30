#!/usr/bin/env bash
set -euo pipefail

for required_var in AWS_REGION ECR_REGISTRY ECR_REPOSITORY RELEASE_TAG PARAMETER_PREFIX; do
  : "${!required_var:?$required_var must be set}"
done

if ! docker compose version >/dev/null 2>&1; then
  dnf install -y docker-compose-plugin
fi

systemctl enable --now docker

collector_api_key="$(aws ssm get-parameter \
  --region "$AWS_REGION" \
  --name "/$PARAMETER_PREFIX/collector/api-key" \
  --with-decryption \
  --query 'Parameter.Value' \
  --output text)"

postgres_password="$(aws ssm get-parameter \
  --region "$AWS_REGION" \
  --name "/$PARAMETER_PREFIX/postgres/password" \
  --with-decryption \
  --query 'Parameter.Value' \
  --output text)"

test -n "$collector_api_key"
test -n "$postgres_password"

aws ecr get-login-password --region "$AWS_REGION" \
  | docker login --username AWS --password-stdin "$ECR_REGISTRY"

export COLLECTOR_API_KEY="$collector_api_key"
export POSTGRES_PASSWORD="$postgres_password"

docker compose -f compose.production.yaml pull
docker compose -f compose.production.yaml up -d --remove-orphans
docker image prune -f
