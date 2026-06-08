#!/usr/bin/env bash
set -e
if [ "$#" -ne 1 ]; then
  echo "Usage: ./run.sh sample.pi"
  exit 1
fi
javac --release 8 ProjectStructure.java
java ProjectStructure "$1"
