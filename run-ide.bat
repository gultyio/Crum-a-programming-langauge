@echo off
javac --release 8 ProjectStructure.java ProjectImperativeIDE.java
if errorlevel 1 exit /b %errorlevel%
java ProjectImperativeIDE
