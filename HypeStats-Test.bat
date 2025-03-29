@echo off
echo Starting HypeStats in TEST MODE...
echo.
echo This mode simulates API calls and log file reading for testing purposes.
echo All errors and events are logged to the ./logs directory.
echo.

REM Determine if this is the first run
if not exist "target\hypestats-1.0-SNAPSHOT.jar" (
    echo First run - compiling the application...
    call mvn clean package
    if errorlevel 1 (
        echo Maven build failed. Please make sure Maven is installed correctly.
        echo.
        pause
        exit /b 1
    )
)

REM Run the application in test mode
java -jar target\hypestats-1.0-SNAPSHOT.jar --test

REM If there was an error, pause to show the message
if errorlevel 1 (
    echo.
    echo Application terminated with an error.
    echo Check the logs directory for details.
    pause
) 