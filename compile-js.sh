#!/bin/bash

COMPILER_JAR=third-party/closure-compiler/compiler.jar
CLOSURE_DIR=third-party/closure-1819

exec ${CLOSURE_DIR}/closure/bin/build/closurebuilder.py \
  --root=${CLOSURE_DIR} \
  --root=src/main/js \
  --namespace=swissvault.onLoad \
  -ocompiled \
  --compiler_jar=${COMPILER_JAR} \
  --output_file=src/main/webapp/js/compiled.js
