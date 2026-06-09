@echo off
setlocal
set "JAVA_HOME=%~dp0jdk-26.0.1"
set "PATH=%JAVA_HOME%bin;%PATH%"
cd /d "%~dp0"

rem Compile the IDE and interpreter sources using the bundled JDK
"%JAVA_HOME%bin\javac" --release 8 ProjectStructure.java ProjectImperativeIDE.java
if errorlevel 1 exit /b %errorlevel%

rem Prepare a clean build directory for the executable jar
if exist build rmdir /s /q build
mkdir build
copy /y *.class build >nul

rem Create a runnable jar for the IDE
"%JAVA_HOME%bin\jar" cfe build\ProjectImperativeIDE.jar ProjectImperativeIDE -C build .
if errorlevel 1 exit /b %errorlevel%

rem Create a native Windows app-image using jpackage
if exist dist rmdir /s /q dist
mkdir dist
"%JAVA_HOME%bin\jpackage" --type app-image --input build --main-jar ProjectImperativeIDE.jar --main-class ProjectImperativeIDE --name ProjectImperativeIDE --app-version 0.1.0 --dest dist
if errorlevel 1 exit /b %errorlevel%

echo ProjectImperativeIDE app image created in dist\ProjectImperativeIDE\
endlocal
