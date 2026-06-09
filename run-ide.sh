#!/usr/bin/env bash
set -e
javac --release 8 ProjectStructure.java ProjectImperativeIDE.java
java ProjectImperativeIDE
