#!/bin/bash
mkdir -p build
javac -d build $(find ./src -name "*.java")
java -cp build app.App
