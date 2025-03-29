@echo off
echo Starting HypeStats with Maven...
echo.

where mvn >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Maven not found in PATH
    echo Please install Maven from https://maven.apache.org/download.cgi
    echo Make sure it's in your PATH
    pause
    exit /b 1
)

echo Building and running HypeStats...
call mvn clean javafx:run

echo.
if %errorlevel% neq 0 (
    echo Failed to run HypeStats.
    echo See error messages above.
) else (
    echo HypeStats has exited successfully.
)

pause 