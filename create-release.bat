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
copy "run.sh" "release\"

echo.
echo Creating launcher scripts for release...

REM Create Windows launcher
(
echo @echo off
echo title HypeStats - Hypixel Bedwars Stats Companion
echo color 0a
echo.
echo REM Check if test mode flag is passed
echo set TEST_MODE=false
echo if "%%1"=="--test" set TEST_MODE=true
echo if "%%1"=="-t" set TEST_MODE=true
echo.
echo if "%%TEST_MODE%%"=="true" ^(
echo     echo Starting HypeStats in TEST MODE...
echo     echo This mode simulates API calls and log file reading for testing purposes.
echo ^) else ^(
echo     echo Starting HypeStats - Hypixel Bedwars Stats Companion...
echo ^)
echo.
echo REM Check for Java
echo java -version ^>nul 2^>^&1
echo if errorlevel 1 ^(
echo     color 0c
echo     echo ERROR: Java not found!
echo     echo Please install Java 17 or higher from https://adoptium.net/
echo     pause
echo     exit /b 1
echo ^)
echo.
echo if "%%TEST_MODE%%"=="true" ^(
echo     java -jar hypestats.jar --test
echo ^) else ^(
echo     java -jar hypestats.jar
echo ^)
echo.
echo if errorlevel 1 ^(
echo     color 0c
echo     echo.
echo     echo Failed to run HypeStats.
echo     echo See error messages above.
echo     echo.
echo     pause
echo     exit /b 1
echo ^)
echo.
echo echo.
echo echo HypeStats has exited successfully.
echo echo.
echo pause
) > "release\HypeStats.bat"

REM Create Unix launcher
(
echo #!/bin/bash
echo.
echo # Set terminal colors
echo GREEN='\033[0;32m'
echo RED='\033[0;31m'
echo YELLOW='\033[1;33m'
echo NC='\033[0m' # No Color
echo.
echo # Show banner
echo echo -e "${GREEN}"
echo echo "HypeStats - Hypixel Bedwars Stats Companion"
echo echo -e "${NC}"
echo.
echo # Check if test mode flag is passed
echo TEST_MODE=false
echo if [ "$1" == "--test" ] ^|^| [ "$1" == "-t" ]; then
echo     TEST_MODE=true
echo     echo -e "${YELLOW}Starting HypeStats in TEST MODE...${NC}"
echo     echo "This mode simulates API calls and log file reading for testing purposes."
echo else
echo     echo "Starting HypeStats..."
echo fi
echo.
echo # Check for Java
echo if ! command -v java ^&^> /dev/null; then
echo     echo -e "${RED}ERROR: Java not found!${NC}"
echo     echo "Please install Java 17 or higher."
echo     echo "Visit: https://adoptium.net/"
echo     exit 1
echo fi
echo.
echo if [ "$TEST_MODE" = true ]; then
echo     java -jar hypestats.jar --test
echo else
echo     java -jar hypestats.jar
echo fi
echo.
echo if [ $? -ne 0 ]; then
echo     echo -e "${RED}"
echo     echo "Failed to run HypeStats."
echo     echo "See error messages above."
echo     echo -e "${NC}"
echo     exit 1
echo fi
echo.
echo echo
echo echo "HypeStats has exited successfully."
echo echo
echo read -p "Press [Enter] to exit..."
) > "release\run.sh"

echo.
echo Creating release ZIP file...
powershell -command "Compress-Archive -Path 'release\*' -DestinationPath 'HypeStats-Release.zip' -Force"

echo.
echo Release package created successfully.
echo Files are available in the 'release' directory and in 'HypeStats-Release.zip'
echo.
pause 