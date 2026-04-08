#!/bin/bash
set -e

cd "$(dirname "$0")"
rm -rf bin
mkdir -p bin

javac -d bin -sourcepath src src/bank/*.java src/main/*.java
java -cp bin main.MainMenu
