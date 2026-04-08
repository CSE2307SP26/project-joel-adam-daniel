#!/bin/bash
set -e

cd "$(dirname "$0")"
rm -rf bin
mkdir -p bin

JUNIT_JAR="test-lib/junit-platform-console-standalone-1.13.0-M3.jar"

javac -cp "$JUNIT_JAR" -d bin -sourcepath src src/bank/*.java src/main/*.java src/test/*.java
java -jar "$JUNIT_JAR" execute \
  --class-path "bin:$JUNIT_JAR" \
  --select-package test
