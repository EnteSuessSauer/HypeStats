@echo off
setlocal enabledelayedexpansion
title HypeStats Launcher
color 0a

REM Create a debug log file
echo HypeStats Launcher Log > hypestats_debug.log
echo %date% %time% >> hypestats_debug.log
echo System: %OS% >> hypestats_debug.log
echo ---------------------------------------- >> hypestats_debug.log

REM Check if test mode flag is passed
set TEST_MODE=false
if "%1"=="--test" set TEST_MODE=true
if "%1"=="-t" set TEST_MODE=true

if "%TEST_MODE%"=="true" (
    echo Starting HypeStats in TEST MODE...
    echo This mode simulates API calls and log file reading for testing purposes.
    echo [TEST MODE] >> hypestats_debug.log
) else (
    echo Starting HypeStats - Hypixel Bedwars Stats Companion...
    echo [NORMAL MODE] >> hypestats_debug.log
)

REM Try different launch methods in order of preference
set LAUNCH_SUCCESS=false

echo Checking for available launch methods...
echo Checking launch methods... >> hypestats_debug.log

REM Check Java is installed first
echo Checking Java... >> hypestats_debug.log
java -version > java_version.tmp 2>&1
type java_version.tmp >> hypestats_debug.log
set JAVA_FOUND=false
findstr /i "version" java_version.tmp > nul
if %errorlevel% equ 0 (
    set JAVA_FOUND=true
    echo Java found >> hypestats_debug.log
    echo Java found
) else (
    echo ERROR: Java is not installed or not in the PATH. >> hypestats_debug.log
    color 0c
    echo.
    echo Java is not installed or not accessible from the command line.
    echo Please install Java 17 or higher and try again.
    echo Visit: https://adoptium.net/
    echo.
    echo This error has been logged to hypestats_debug.log
    echo.
    pause
    exit /b 1
)

REM Try Method 1: Maven JavaFX Plugin (preferred)
echo Checking Maven... >> hypestats_debug.log
call mvn -version > maven_version.tmp 2>&1
type maven_version.tmp >> hypestats_debug.log
set MAVEN_FOUND=false

REM Properly check if Maven is installed by searching for Apache Maven in output
findstr /i "Apache Maven" maven_version.tmp > nul
if %errorlevel% equ 0 (
    set MAVEN_FOUND=true
    echo Maven found >> hypestats_debug.log
    echo Maven found

    REM Make sure target directory exists
    if not exist "target" (
        echo Creating target directory >> hypestats_debug.log
        mkdir target
    )
    
    echo.
    echo Launch Method: Maven JavaFX Plugin
    echo Using Maven JavaFX Plugin >> hypestats_debug.log
    
    if "%TEST_MODE%"=="true" (
        echo.
        echo Executing: mvn javafx:run -Djavafx.args="--test" >> hypestats_debug.log
        echo Building and running with Maven (Test Mode)...
        call mvn javafx:run -Djavafx.args="--test" > maven_run.tmp 2>&1
    ) else (
        echo.
        echo Executing: mvn javafx:run >> hypestats_debug.log
        echo Building and running with Maven...
        call mvn javafx:run > maven_run.tmp 2>&1
    )
    
    type maven_run.tmp >> hypestats_debug.log
    
    if %errorlevel% equ 0 (
        set LAUNCH_SUCCESS=true
        echo Maven launch successful >> hypestats_debug.log
        goto :end
    ) else (
        echo Maven launch failed with errorlevel: %errorlevel% >> hypestats_debug.log
        echo Maven build or run failed. See hypestats_debug.log for details.
        echo.
        echo Trying alternative methods...
    )
) else (
    echo Maven not found >> hypestats_debug.log
    echo Maven not found or not in PATH
    echo We will try alternative methods...
)

REM Try Method 2: Check for JavaFX SDK
set JAVAFX_FOUND=false

echo Checking for JavaFX SDK... >> hypestats_debug.log
if defined JAVAFX_HOME (
    echo JAVAFX_HOME is set to: %JAVAFX_HOME% >> hypestats_debug.log
    if exist "%JAVAFX_HOME%\lib" (
        set JAVAFX_FOUND=true
        echo JavaFX found at JAVAFX_HOME >> hypestats_debug.log
        echo Found JavaFX SDK at: %JAVAFX_HOME%
    ) else (
        echo JAVAFX_HOME directory does not exist or has no lib folder >> hypestats_debug.log
        echo WARNING: JAVAFX_HOME is set but the directory doesn't exist
    )
) else (
    echo JAVAFX_HOME is not set, searching common locations... >> hypestats_debug.log
    REM Try to find JavaFX in common locations
    for %%p in (
        "C:\Program Files\JavaFX\javafx-sdk*"
        "C:\javafx-sdk*"
        "%USERPROFILE%\javafx-sdk*"
        "%ProgramFiles%\Java\javafx-sdk*"
        "%CD%\javafx-sdk*"
    ) do (
        echo Checking: %%~p >> hypestats_debug.log
        if exist "%%~p\lib\javafx.base.jar" (
            set JAVAFX_HOME=%%~p
            set JAVAFX_FOUND=true
            echo Found JavaFX SDK at: !JAVAFX_HOME! >> hypestats_debug.log
            echo Found JavaFX SDK at: !JAVAFX_HOME!
            goto :javafx_found
        )
    )
    echo JavaFX not found in common locations >> hypestats_debug.log
    echo JavaFX SDK not found in common locations
)

:javafx_found
if "%JAVAFX_FOUND%"=="true" (
    echo Launch Method: Java with JavaFX modules
    echo Using Java with JavaFX modules >> hypestats_debug.log
    
    echo Checking for JAR file... >> hypestats_debug.log
    if not exist "target\hypestats-1.0-SNAPSHOT.jar" (
        echo JAR file doesn't exist >> hypestats_debug.log
        echo The application JAR file is missing.
        
        if "%MAVEN_FOUND%"=="true" (
            echo We need to build it first. Building with Maven...
            echo Attempting to build with Maven... >> hypestats_debug.log
            call mvn clean package > maven_build.tmp 2>&1
            type maven_build.tmp >> hypestats_debug.log
            
            if %errorlevel% equ 0 (
                echo Maven build successful >> hypestats_debug.log
                echo Maven build successful
            ) else (
                echo Maven build failed >> hypestats_debug.log
                echo Maven build failed. See hypestats_debug.log for details.
                goto :try_download_javafx
            )
        ) else (
            echo.
            echo Maven is required to build the application.
            goto :try_download_javafx
        )
    )
    
    if "%TEST_MODE%"=="true" (
        echo Executing Java with JavaFX modules (test mode) >> hypestats_debug.log
        echo Running with JavaFX modules (test mode)...
        java --module-path "%JAVAFX_HOME%\lib" --add-modules javafx.controls,javafx.fxml,javafx.web -jar target\hypestats-1.0-SNAPSHOT.jar --test > javafx_run.tmp 2>&1
    ) else (
        echo Executing Java with JavaFX modules >> hypestats_debug.log
        echo Running with JavaFX modules...
        java --module-path "%JAVAFX_HOME%\lib" --add-modules javafx.controls,javafx.fxml,javafx.web -jar target\hypestats-1.0-SNAPSHOT.jar > javafx_run.tmp 2>&1
    )
    
    type javafx_run.tmp >> hypestats_debug.log
    
    findstr /i "Exception Error" javafx_run.tmp > nul
    if %errorlevel% neq 0 (
        set LAUNCH_SUCCESS=true
        echo JavaFX launch successful >> hypestats_debug.log
        goto :end
    ) else (
        echo JavaFX launch failed >> hypestats_debug.log
        echo JavaFX launch failed. See hypestats_debug.log for details.
        echo.
        echo Trying next method...
    )
)

:try_download_javafx
REM Try Method 3: Download JavaFX SDK automatically
echo Checking for local JavaFX SDK... >> hypestats_debug.log
if not exist "javafx-sdk" (
    echo Local JavaFX SDK not found >> hypestats_debug.log
    echo.
    echo JavaFX not found. Would you like to download it automatically? (Y/N)
    set /p DOWNLOAD_CHOICE=
    if /i "!DOWNLOAD_CHOICE!"=="Y" (
        echo User chose to download JavaFX >> hypestats_debug.log
        echo.
        echo Downloading JavaFX SDK...
        echo Downloading JavaFX SDK... >> hypestats_debug.log
        powershell -Command "Invoke-WebRequest -Uri https://download2.gluonhq.com/openjfx/17.0.2/openjfx-17.0.2_windows-x64_bin-sdk.zip -OutFile javafx.zip" > download.tmp 2>&1
        type download.tmp >> hypestats_debug.log
        
        findstr /i "Exception Error" download.tmp > nul
        if %errorlevel% equ 0 (
            echo JavaFX download failed >> hypestats_debug.log
            echo Download failed. Please check your internet connection.
            goto :end
        )
        
        echo Extracting JavaFX SDK...
        echo Extracting JavaFX SDK... >> hypestats_debug.log
        powershell -Command "Expand-Archive -Path javafx.zip -DestinationPath . -Force" > extract.tmp 2>&1
        type extract.tmp >> hypestats_debug.log
        
        findstr /i "Exception Error" extract.tmp > nul
        if %errorlevel% equ 0 (
            echo JavaFX extraction failed >> hypestats_debug.log
            echo Extraction failed.
            goto :end
        )
        
        rename openjfx-17.0.2_windows-x64_bin-sdk javafx-sdk
        del javafx.zip
        set JAVAFX_HOME=%CD%\javafx-sdk
        echo JavaFX downloaded and extracted to: %JAVAFX_HOME% >> hypestats_debug.log
        echo JavaFX downloaded and extracted to: %JAVAFX_HOME%
        
        echo Launch Method: Java with downloaded JavaFX modules
        echo Using Java with downloaded JavaFX modules >> hypestats_debug.log
        
        if not exist "target\hypestats-1.0-SNAPSHOT.jar" (
            echo JAR file doesn't exist, need to build first >> hypestats_debug.log
            echo The application JAR file is missing.
            
            if "%MAVEN_FOUND%"=="true" (
                echo Building with Maven...
                echo User chose to build with Maven >> hypestats_debug.log
                call mvn clean package > maven_build2.tmp 2>&1
                type maven_build2.tmp >> hypestats_debug.log
                
                if %errorlevel% equ 0 (
                    echo Maven build successful >> hypestats_debug.log
                ) else (
                    echo Maven build failed >> hypestats_debug.log
                    echo Maven build failed. See hypestats_debug.log for details.
                    goto :end
                )
            ) else (
                echo.
                echo Maven is required to build the application.
                echo Please install Maven from: https://maven.apache.org/download.cgi
                echo.
                echo After installing Maven, add it to your PATH and try again.
                echo.
                goto :end
            )
        )
        
        if "%TEST_MODE%"=="true" (
            echo Executing Java with downloaded JavaFX modules (test mode) >> hypestats_debug.log
            echo Running with downloaded JavaFX modules (test mode)...
            java --module-path "%JAVAFX_HOME%\lib" --add-modules javafx.controls,javafx.fxml,javafx.web -jar target\hypestats-1.0-SNAPSHOT.jar --test > javafx_run2.tmp 2>&1
        ) else (
            echo Executing Java with downloaded JavaFX modules >> hypestats_debug.log
            echo Running with downloaded JavaFX modules...
            java --module-path "%JAVAFX_HOME%\lib" --add-modules javafx.controls,javafx.fxml,javafx.web -jar target\hypestats-1.0-SNAPSHOT.jar > javafx_run2.tmp 2>&1
        )
        
        type javafx_run2.tmp >> hypestats_debug.log
        
        findstr /i "Exception Error" javafx_run2.tmp > nul
        if %errorlevel% neq 0 (
            set LAUNCH_SUCCESS=true
            echo JavaFX downloaded launch successful >> hypestats_debug.log
            goto :end
        ) else (
            echo JavaFX downloaded launch failed >> hypestats_debug.log
            echo JavaFX launch failed. See hypestats_debug.log for details.
        )
    ) else (
        echo User declined to download JavaFX >> hypestats_debug.log
    )
)

:end
if "%LAUNCH_SUCCESS%"=="false" (
    color 0c
    echo.
    echo ============================================================
    echo Launch failed. Please try one of the following:
    echo.
    if "%MAVEN_FOUND%"=="false" (
        echo 1. Install Maven: https://maven.apache.org/download.cgi
        echo    - Download the Binary zip archive
        echo    - Extract to a folder (e.g., C:\Program Files\Maven)
        echo    - Add the bin directory to your PATH:
        echo      * Right-click 'This PC' -^> Properties -^> Advanced system settings
        echo      * Click 'Environment Variables'
        echo      * Edit 'Path' variable and add the bin directory path
    )
    if "%JAVAFX_FOUND%"=="false" (
        echo 2. Install JavaFX SDK: https://openjfx.io/
        echo    - Download the SDK for your platform
        echo    - Extract to a folder
        echo    - Set the JAVAFX_HOME environment variable to that folder
    )
    echo.
    echo Debug information has been saved to hypestats_debug.log
    echo ============================================================
    echo Launch failed >> hypestats_debug.log
)

echo.
echo Debug information has been saved to hypestats_debug.log
echo.
pause 