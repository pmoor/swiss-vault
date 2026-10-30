#!/bin/bash
# Builds, tests and uploads a new version without sending traffic to it.
# Extra arguments are passed on to `gcloud app deploy`.
set -euo pipefail
cd "$(dirname "$0")"

PROJECT=swiss-vault
# App Engine version cannot contain any dots nor any uppercase characters
VERSION=1-0-snapshot
SDK=target/appengine-java-sdk/google_appengine_java_delta/google/appengine/tools/java

test -f src/main/resources/production.properties || {
  echo "missing src/main/resources/production.properties" >&2
  exit 1
}

./compile-js.sh
mvn -B clean verify

# converts appengine-web.xml into the app.yaml gcloud expects
java -cp ${SDK}/lib/appengine-tools-api.jar com.google.appengine.tools.admin.AppCfg \
  stage target/swiss-vault-1.0-SNAPSHOT target/appengine-staging

gcloud app deploy target/appengine-staging/app.yaml \
  --project=${PROJECT} --version=${VERSION} --no-promote "$@"
