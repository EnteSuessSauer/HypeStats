@echo off
title HypeStats Launcher
echo HypeStats - Hypixel Bedwars Stats Companion
echo.

REM Check for Java
java -version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Java is not installed or not in the PATH.
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
    
    echo Maven not found. Please install Maven from:
    echo https://maven.apache.org/download.cgi
    pause
    exit /b 1
)

:maven_found
if not defined MVN_PATH set MVN_PATH=mvn

echo Building and running HypeStats...
call %MVN_PATH% clean javafx:run

if errorlevel 1 (
    echo.
    echo Failed to run HypeStats.
    echo See error messages above.
    pause
    exit /b 1
)

echo.
echo HypeStats has exited successfully.
echo.
pause 