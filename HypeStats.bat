@echo off
setlocal enabledelayedexpansion
title HypeStats Launcher
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

REM Try different launch methods in order of preference
set LAUNCH_SUCCESS=false

echo Checking for available launch methods...

REM Try Method 1: Maven JavaFX Plugin (preferred)
mvn -version >nul 2>&1
if not errorlevel 1 (
    echo Launch Method: Maven JavaFX Plugin
    
    if "%TEST_MODE%"=="true" (
        mvn javafx:run -Djavafx.args="--test"
    ) else (
        mvn javafx:run
    )
    
    if not errorlevel 1 (
        set LAUNCH_SUCCESS=true
        goto :end
    ) else (
        echo Maven launch failed, trying alternative methods...
    )
)

REM Try Method 2: Check for JavaFX SDK
set JAVAFX_FOUND=false

if defined JAVAFX_HOME (
    if exist "%JAVAFX_HOME%\lib" set JAVAFX_FOUND=true
) else (
    REM Try to find JavaFX in common locations
    for %%p in (
        "C:\Program Files\JavaFX\javafx-sdk*"
        "C:\javafx-sdk*"
        "%USERPROFILE%\javafx-sdk*"
        "%ProgramFiles%\Java\javafx-sdk*"
    ) do (
        if exist "%%~p\lib\javafx.base.jar" (
            set JAVAFX_HOME=%%~p
            set JAVAFX_FOUND=true
            echo Found JavaFX SDK at: !JAVAFX_HOME!
            goto :javafx_found
        )
    )
)

:javafx_found
if "%JAVAFX_FOUND%"=="true" (
    echo Launch Method: Java with JavaFX modules
    
    if "%TEST_MODE%"=="true" (
        java --module-path "%JAVAFX_HOME%\lib" --add-modules javafx.controls,javafx.fxml,javafx.web -jar target\hypestats-1.0-SNAPSHOT.jar --test
    ) else (
        java --module-path "%JAVAFX_HOME%\lib" --add-modules javafx.controls,javafx.fxml,javafx.web -jar target\hypestats-1.0-SNAPSHOT.jar
    )
    
    if not errorlevel 1 (
        set LAUNCH_SUCCESS=true
        goto :end
    ) else (
        echo JavaFX launch failed, trying next method...
    )
)

REM Try Method 3: Download JavaFX SDK automatically
if not exist "javafx-sdk" (
    echo JavaFX not found. Would you like to download it automatically? (Y/N)
    set /p DOWNLOAD_CHOICE=
    if /i "!DOWNLOAD_CHOICE!"=="Y" (
        echo Downloading JavaFX SDK...
        powershell -Command "Invoke-WebRequest -Uri https://download2.gluonhq.com/openjfx/17.0.2/openjfx-17.0.2_windows-x64_bin-sdk.zip -OutFile javafx.zip"
        echo Extracting JavaFX SDK...
        powershell -Command "Expand-Archive -Path javafx.zip -DestinationPath . -Force"
        rename openjfx-17.0.2_windows-x64_bin-sdk javafx-sdk
        del javafx.zip
        set JAVAFX_HOME=%CD%\javafx-sdk
        
        echo Launch Method: Java with downloaded JavaFX modules
        
        if "%TEST_MODE%"=="true" (
            java --module-path "%JAVAFX_HOME%\lib" --add-modules javafx.controls,javafx.fxml,javafx.web -jar target\hypestats-1.0-SNAPSHOT.jar --test
        ) else (
            java --module-path "%JAVAFX_HOME%\lib" --add-modules javafx.controls,javafx.fxml,javafx.web -jar target\hypestats-1.0-SNAPSHOT.jar
        )
        
        if not errorlevel 1 (
            set LAUNCH_SUCCESS=true
            goto :end
        )
    )
)

:end
if "%LAUNCH_SUCCESS%"=="false" (
    color 0c
    echo.
    echo ============================================================
    echo Launch failed. Please try one of the following:
    echo.
    echo 1. Install Maven: https://maven.apache.org/download.cgi
    echo 2. Install JavaFX SDK: https://openjfx.io/
    echo    and set JAVAFX_HOME environment variable
    echo 3. Manually run: mvn javafx:run
    echo ============================================================
    echo.
)

echo.
pause 