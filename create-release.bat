@echo off
echo Creating HypeStats release package...
echo.

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
    echo Please install Maven first.
    echo You can download Maven from: https://maven.apache.org/download.cgi
    pause
    exit /b 1
)

echo Building application with Maven...
call mvn clean package
if errorlevel 1 (
    echo.
    echo Maven build failed.
    echo Check the output above for errors.
    pause
    exit /b 1
)

echo.
echo Creating release directory...
if exist "release" rmdir /s /q "release"
mkdir "release"
mkdir "release\logs"

echo.
echo Copying files to release directory...
copy "target\hypestats-1.0-SNAPSHOT.jar" "release\hypestats.jar"
copy "README.md" "release\"
copy "LICENSE" "release\" 2>nul
copy "run.bat" "release\"
copy "run.sh" "release\"
copy "HypeStats.bat" "release\"
copy "HypeStats-Test.bat" "release\"

echo.
echo Creating no-compile launcher for release...
(
echo @echo off
echo echo Starting HypeStats...
echo.
echo REM Check for Java
echo java -version ^>nul 2^>^&1
echo if errorlevel 1 ^(
echo     echo ERROR: Java is not installed or not in the PATH.
echo     echo Please install Java 17 or higher and try again.
echo     pause
echo     exit /b 1
echo ^)
echo.
echo REM Run the application
echo java -jar hypestats.jar
echo.
echo REM If there was an error, pause to show the message
echo if errorlevel 1 ^(
echo     echo.
echo     echo Application terminated with an error.
echo     pause
echo ^)
) > "release\HypeStats-Run.bat"

(
echo @echo off
echo echo Starting HypeStats in TEST MODE...
echo echo.
echo echo This mode simulates API calls and log file reading for testing purposes.
echo echo All errors and events are logged to the ./logs directory.
echo echo.
echo.
echo REM Check for Java
echo java -version ^>nul 2^>^&1
echo if errorlevel 1 ^(
echo     echo ERROR: Java is not installed or not in the PATH.
echo     echo Please install Java 17 or higher and try again.
echo     pause
echo     exit /b 1
echo ^)
echo.
echo REM Run the application in test mode
echo java -jar hypestats.jar --test
echo.
echo REM If there was an error, pause to show the message
echo if errorlevel 1 ^(
echo     echo.
echo     echo Application terminated with an error.
echo     echo Check the logs directory for details.
echo     pause
echo ^)
) > "release\HypeStats-Test-Run.bat"

echo.
echo Creating release ZIP file...
powershell -command "Compress-Archive -Path 'release\*' -DestinationPath 'HypeStats-Release.zip' -Force"

echo.
echo Release package created successfully.
echo Files are available in the 'release' directory and in 'HypeStats-Release.zip'
echo.
pause 