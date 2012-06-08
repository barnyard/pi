#!/bin/bash

mkdir -p build/classes

find src -name \*.java -print > file.list
CLASSPATH=lib/*:

javac  -cp $CLASSPATH -d build/classes @file.list
rm file.list
cp src/main/resources/* build/classes