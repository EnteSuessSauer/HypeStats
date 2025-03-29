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

REM Check if JavaFX SDK folder exists, if not download it
if not exist "javafx-sdk-17.0.2" (
    if not exist "javafx-sdk" (
        echo JavaFX SDK not found. Downloading now...
        echo This will download about 60 MB of data.
        echo.
        
        powershell -Command "Invoke-WebRequest -Uri https://download2.gluonhq.com/openjfx/17.0.2/openjfx-17.0.2_windows-x64_bin-sdk.zip -OutFile javafx.zip"
        if errorlevel 1 (
            echo Failed to download JavaFX SDK.
            echo Please check your internet connection and try again.
            pause
            exit /b 1
        )
        
        echo Extracting JavaFX SDK...
        powershell -Command "Expand-Archive -Path javafx.zip -DestinationPath . -Force"
        if errorlevel 1 (
            echo Failed to extract JavaFX SDK.
            pause
            exit /b 1
        )
        
        del javafx.zip
        echo JavaFX SDK installed successfully.
    )
)

REM Set the JavaFX home directory based on what folder exists
if exist "javafx-sdk-17.0.2" (
    set JAVAFX_HOME=%CD%\javafx-sdk-17.0.2
) else if exist "javafx-sdk" (
    set JAVAFX_HOME=%CD%\javafx-sdk
) else (
    echo ERROR: JavaFX SDK not found.
    pause
    exit /b 1
)

REM Check if pom.xml exists
if not exist "pom.xml" (
    echo ERROR: pom.xml not found.
    echo Make sure you are in the correct directory.
    pause
    exit /b 1
)

REM Find maven executable (first check in system path, then check in common locations)
set MVN_PATH=mvn
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
    
    echo Maven not found. Would you like to build without Maven? (Y/N)
    set /p BUILD_CHOICE=
    if /i "%BUILD_CHOICE%"=="Y" (
        goto :run_with_javafx
    ) else (
        echo Please install Maven from https://maven.apache.org/download.cgi
        pause
        exit /b 1
    )
)

:maven_found
echo Building with Maven...
call %MVN_PATH% clean package
if errorlevel 1 (
    echo Maven build failed.
    goto :run_with_javafx
)

echo.
echo Running HypeStats with Maven...
call %MVN_PATH% javafx:run
if errorlevel 1 (
    echo Maven javafx:run failed. Trying alternative method...
    goto :run_with_javafx
)
goto :end

:run_with_javafx
echo.
echo Running HypeStats with JavaFX modules...

REM Check if JAR file exists
if not exist "target\hypestats-1.0-SNAPSHOT.jar" (
    echo ERROR: No JAR file found in target directory.
    echo Would you like to try downloading a pre-built JAR? (Y/N)
    set /p DOWNLOAD_JAR=
    if /i "%DOWNLOAD_JAR%"=="Y" (
        echo This feature is not implemented yet.
        echo Please install Maven to build the application.
        pause
        exit /b 1
    ) else (
        echo Cannot continue without a JAR file.
        pause
        exit /b 1
    )
)

REM Run with JavaFX modules
echo Using JavaFX SDK at: %JAVAFX_HOME%
java --module-path "%JAVAFX_HOME%\lib" --add-modules javafx.controls,javafx.fxml,javafx.web -jar target\hypestats-1.0-SNAPSHOT.jar

:end
echo.
pause 