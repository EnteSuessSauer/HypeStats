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
echo.

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
    echo Maven not found in PATH, checking common locations...
    
    for %%i in (
        "%ProgramFiles%\apache-maven\bin\mvn.cmd"
        "%ProgramFiles%\Maven\bin\mvn.cmd"
        "%ProgramFiles(x86)%\apache-maven\bin\mvn.cmd"
        "%ProgramFiles(x86)%\Maven\bin\mvn.cmd"
        "%USERPROFILE%\scoop\apps\maven\current\bin\mvn.cmd"
    ) do (
        if exist %%~i (
            set MVN_PATH=%%~i
            echo Found Maven at: %%~i
            goto :maven_found
        )
    )
    
    color 0c
    echo Maven not found. Please install Maven from:
    echo https://maven.apache.org/download.cgi
    echo Make sure it's added to your PATH environment variable.
    pause
    exit /b 1
)

:maven_found
if not defined MVN_PATH set MVN_PATH=mvn

REM Create target directory if it doesn't exist
if not exist "target" mkdir target

echo Building and running the application...
echo.

if "%TEST_MODE%"=="true" (
    call %MVN_PATH% clean javafx:run -Djavafx.args="--test"
) else (
    call %MVN_PATH% clean javafx:run
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