Swiss Vault - Secure Cloud Based Password Manager

[![build](https://github.com/pmoor/swiss-vault/actions/workflows/build.yml/badge.svg)](https://github.com/pmoor/swiss-vault/actions/workflows/build.yml)

## Building

Requires JDK 25, Maven and python3.

    ./compile-js.sh   # writes src/main/webapp/js/compiled.js
    mvn package       # unit tests + WAR in target/

## Running locally

Running and deploying need the App Engine Java component of the Google Cloud CLI
(`google-cloud-cli-app-engine-java` on Fedora/RHEL). Then:

    mvn appengine:run   # http://localhost:8080, uses src/main/resources/development.properties

`mvn verify` additionally starts the dev server and runs the integration tests.

## Deploying

Production configuration lives in `src/main/resources/production.properties`,
which is not checked in. It needs the same keys as `development.properties`.

    gcloud auth login
    mvn package appengine:deploy   # uploads version 1-0-snapshot without promoting it

`https://1-0-snapshot-dot-swiss-vault.appspot.com/` should now redirect to the
Google login page. Logging in only works on the configured `hostname`, so to
really try it out send traffic to it:

    gcloud app services set-traffic default --splits=1-0-snapshot=1 --project=swiss-vault

and to roll back, route traffic to the previous version again (`gcloud app versions list`).
