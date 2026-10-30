#!/bin/bash
# Runs the App Engine dev server on the exploded WAR built by `mvn package`.
# Usage: ./devserver.sh [port] [generated files dir]
set -euo pipefail
cd "$(dirname "$0")"

SDK=target/appengine-java-sdk/google_appengine_java_delta/google/appengine/tools/java
PORT=${1:-8080}
GENERATED_DIR=${2:-target/devserver}

# unapplied_job_pct=0: queries are strongly consistent, like in production (Firestore in Datastore
# mode) rather than the eventually consistent Datastore the dev server simulates by default
exec java \
  --add-opens java.base/java.net=ALL-UNNAMED \
  --add-opens java.base/sun.net.www.protocol.http=ALL-UNNAMED \
  --add-opens java.base/sun.net.www.protocol.https=ALL-UNNAMED \
  -Dappengine.sdk.root=${SDK} \
  -Ddatastore.default_high_rep_job_policy_unapplied_job_pct=0 \
  -cp ${SDK}/lib/appengine-tools-api.jar \
  com.google.appengine.tools.development.DevAppServerMain \
  --sdk_root=${SDK} \
  --port=${PORT} \
  --generated_dir=${GENERATED_DIR} \
  target/swiss-vault
