@echo off
REM Installer batch file for class files downloaded from GitHub
REM This script installs .class files to the correct project directories

setlocal enabledelayedexpansion

echo.
echo ====================================
echo Class Files Installer
echo ====================================
echo.

REM Check if running from correct directory
if not exist "ProjectStructure.java" (
    echo Error: ProjectStructure.java not found in current directory.
    echo Please run this installer from the project root directory.
    pause
    exit /b 1
)

REM Create necessary directories
if not exist "target\classes" mkdir "target\classes"
if not exist "build\classes" mkdir "build\classes"

echo Installing class files...
echo.

REM Copy .class files from current directory to target/classes
if exist "*.class" (
    echo Copying .class files to target\classes...
    copy "*.class" "target\classes\" /Y >nul
    if !errorlevel! equ 0 (
        echo [OK] Class files copied to target\classes
    ) else (
        echo [ERROR] Failed to copy class files to target\classes
        pause
        exit /b 1
    )
    
    REM Also copy to build/classes for consistency
    copy "*.class" "build\classes\" /Y >nul
    if !errorlevel! equ 0 (
        echo [OK] Class files copied to build\classes
    )
) else (
    echo Warning: No .class files found in current directory.
    echo Please extract downloaded class files to this directory first.
    pause
    exit /b 1
)

echo.
echo ====================================
echo Installation Complete!
echo ====================================
echo.
echo You can now run the program with:
echo   run.bat sample.pi
echo.
pause
exit /b 0
