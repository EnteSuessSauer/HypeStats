@echo off
setlocal enabledelayedexpansion
echo Starting HypeStats with JavaFX...

REM Check if test mode flag is passed
set TEST_MODE=false
if "%1"=="--test" set TEST_MODE=true
if "%1"=="-t" set TEST_MODE=true

REM Check for Java
java -version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Java is not installed or not in the PATH.
    echo Please install Java 17 or higher and try again.
    pause
    exit /b 1
)

REM Check for the target folder and create it if needed
if not exist "target" mkdir target

REM Determine if this is the first run or recompilation needed
if not exist "target\hypestats-1.0-SNAPSHOT.jar" (
    echo First run - compiling the application...
    
    REM Check for Maven
    mvn -version >nul 2>&1
    if errorlevel 1 (
        echo ERROR: Maven is not installed or not in the PATH.
        echo.
        echo Please install Maven or use a pre-built version of HypeStats.
        echo You can download Maven from: https://maven.apache.org/download.cgi
        pause
        exit /b 1
    )
    
    call mvn clean package
    if errorlevel 1 (
        echo.
        echo Maven build failed. Here are possible reasons:
        echo 1. Java version incompatibility - Make sure you're using Java 17+
        echo 2. Dependency issues - Check if you're connected to the internet
        echo 3. Compiler errors - Check the output above for specific errors
        echo.
        echo To see more detailed error messages, try running:
        echo mvn clean package -X
        echo.
        pause
        exit /b 1
    )
)

REM Build class path with all dependencies
set CP=target\hypestats-1.0-SNAPSHOT.jar
for %%i in (target\libs\*.jar) do call set CP=!CP!;%%i

REM Run the application with JavaFX modules using Maven
if "%TEST_MODE%"=="true" (
    echo Running in TEST MODE with JavaFX - API and log file reading will be simulated
    mvn javafx:run -Djavafx.args="--test"
) else (
    mvn javafx:run
)

REM If there was an error, pause to show the message
if errorlevel 1 (
    echo.
    echo Application terminated with an error.
    pause
) 