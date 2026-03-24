#!/bin/bash
set -e

cd "$(dirname "$0")" #cd into the directory of the script (credit: Dave Dopson from Stack Overflow)
mkdir -p bin

javac -d bin -sourcepath src src/main/*.java #compile
java -cp bin main.MainMenu #run the main menu


