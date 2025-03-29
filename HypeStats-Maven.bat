@echo off
echo Starting HypeStats with Maven JavaFX plugin...

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

REM Check for Maven
mvn -version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Maven is not installed or not in the PATH.
    echo.
    echo Please install Maven from: https://maven.apache.org/download.cgi
    pause
    exit /b 1
)

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