@echo off
javac --release 8 ProjectStructure.java
if errorlevel 1 exit /b %errorlevel%
echo Compilation complete.
