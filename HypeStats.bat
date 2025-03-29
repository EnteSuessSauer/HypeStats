@echo off
title HypeStats - Hypixel Bedwars Stats Companion
color 0a

REM Check if test mode flag is passed
set TEST_MODE=false
if "%1"=="--test" set TEST_MODE=true
if "%1"=="-t" set TEST_MODE=true

if "%TEST_MODE%"=="true" (
    echo Starting HypeStats in TEST MODE...
    echo This mode simulates API calls and log file reading for testing purposes.
) else (
    echo Starting HypeStats - Hypixel Bedwars Stats Companion...
)

REM Check for Java
java -version >nul 2>&1
if errorlevel 1 (
    color 0c
    echo ERROR: Java not found!
    echo Please install Java 17 or higher from https://adoptium.net/
    pause
    exit /b 1
)

REM Check for Maven
where mvn >nul 2>&1
if errorlevel 1 (
    color 0c
    echo ERROR: Maven not found in PATH!
    echo Please install Maven from https://maven.apache.org/download.cgi
    echo Make sure it's added to your PATH environment variable.
    pause
    exit /b 1
)

REM Create target directory if it doesn't exist
if not exist "target" mkdir target

echo Building and running the application...
echo.

if "%TEST_MODE%"=="true" (
    call mvn clean javafx:run -Djavafx.args="--test"
) else (
    call mvn clean javafx:run
)

if errorlevel 1 (
    color 0c
    echo.
    echo Failed to run HypeStats.
    echo See error messages above.
    echo.
    pause
    exit /b 1
)

echo.
echo HypeStats has exited successfully.
echo.
pause 