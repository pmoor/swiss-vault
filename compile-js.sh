#!/bin/bash
set -euo pipefail

COMPILER_JAR=third-party/closure-compiler/compiler.jar
CLOSURE_DIR=third-party/closure-1819

# closurebuilder.py's own compile step no longer works on python3, so we only
# use it to compute the dependency-ordered list of sources.
SOURCES=$(python3 ${CLOSURE_DIR}/closure/bin/build/closurebuilder.py \
  --root=${CLOSURE_DIR} \
  --root=src/main/js \
  --namespace=swissvault.onLoad \
  -olist)

exec java -jar ${COMPILER_JAR} \
  $(printf -- '--js %s ' ${SOURCES}) \
  --js_output_file=src/main/webapp/js/compiled.js
