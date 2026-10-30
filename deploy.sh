#!/bin/bash
# Builds, tests and uploads a new version without sending traffic to it.
# Extra arguments are passed on to `gcloud app deploy`.
set -euo pipefail
cd "$(dirname "$0")"

PROJECT=swiss-vault
SDK=target/appengine-java-sdk/google_appengine_java_delta/google/appengine/tools/java

test -f src/main/resources/production.properties || {
  echo "missing src/main/resources/production.properties" >&2
  exit 1
}

mvn -B clean verify

# the project version from pom.xml; App Engine versions cannot contain dots nor uppercase characters
VERSION=$(mvn -q help:evaluate -Dexpression=project.version -DforceStdout 2>/dev/null | tr 'A-Z.' 'a-z-')

# converts appengine-web.xml into the app.yaml gcloud expects
java -cp ${SDK}/lib/appengine-tools-api.jar com.google.appengine.tools.admin.AppCfg \
  stage target/swiss-vault target/appengine-staging

gcloud app deploy target/appengine-staging/app.yaml \
  --project=${PROJECT} --version=${VERSION} --no-promote "$@"
