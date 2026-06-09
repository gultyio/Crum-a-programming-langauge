@echo off
if "%~1"=="" (
    echo Usage: run.bat sample.pi
    exit /b 1
)
java ProjectStructure %~1
