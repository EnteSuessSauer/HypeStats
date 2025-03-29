@echo off
echo Starting HypeStats...

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

REM Run the application with Java
java -jar target\hypestats-1.0-SNAPSHOT.jar

REM If there was an error, pause to show the message
if errorlevel 1 (
    echo.
    echo Application terminated with an error.
    pause
) 