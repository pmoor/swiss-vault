#!/bin/bash
# Runs the App Engine dev server on the exploded WAR built by `mvn package`.
# Usage: ./devserver.sh [port] [generated files dir]
set -euo pipefail
cd "$(dirname "$0")"

SDK=target/appengine-java-sdk/google_appengine_java_delta/google/appengine/tools/java
PORT=${1:-8080}
GENERATED_DIR=${2:-target/devserver}

exec java \
  --add-opens java.base/java.net=ALL-UNNAMED \
  --add-opens java.base/sun.net.www.protocol.http=ALL-UNNAMED \
  --add-opens java.base/sun.net.www.protocol.https=ALL-UNNAMED \
  -Dappengine.sdk.root=${SDK} \
  -cp ${SDK}/lib/appengine-tools-api.jar \
  com.google.appengine.tools.development.DevAppServerMain \
  --sdk_root=${SDK} \
  --port=${PORT} \
  --generated_dir=${GENERATED_DIR} \
  target/swiss-vault-1.0-SNAPSHOT
