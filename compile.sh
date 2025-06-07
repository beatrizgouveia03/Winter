#!/bin/bash
mkdir -p build
javac -cp ".;lib/gson-2.10.1.jar" -d build $(find ./src -name "*.java")
java -cp ".;lib/gson-2.10.1.jar:build" app.App