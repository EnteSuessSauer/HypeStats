#!/bin/bash

# Set terminal colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Show banner
echo -e "${GREEN}Creating HypeStats release package...${NC}"
echo

# Check for Java
if ! command -v java &> /dev/null; then
    echo -e "${RED}ERROR: Java is not installed or not in the PATH.${NC}"
    echo "Please install Java 17 or higher and try again."
    exit 1
fi

# Check for Maven
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}ERROR: Maven is not installed or not in the PATH.${NC}"
    echo "Please install Maven first."
    echo "You can download Maven from: https://maven.apache.org/download.cgi"
    exit 1
fi

echo "Building application with Maven..."
mvn clean package
if [ $? -ne 0 ]; then
    echo
    echo -e "${RED}Maven build failed.${NC}"
    echo "Check the output above for errors."
    exit 1
fi

echo
echo "Creating release directory..."
rm -rf release
mkdir -p release/logs

echo
echo "Copying files to release directory..."
cp target/hypestats-1.0-SNAPSHOT.jar release/hypestats.jar
cp README.md release/
cp LICENSE release/ 2>/dev/null

echo
echo "Creating launcher scripts for release..."

# Create Windows launcher
cat > release/HypeStats.bat << 'EOF'
@echo off
title HypeStats - Hypixel Bedwars Stats Companion
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

REM Check for Java
java -version >nul 2>&1
if errorlevel 1 (
    color 0c
    echo ERROR: Java not found!
    echo Please install Java 17 or higher from https://adoptium.net/
    pause
    exit /b 1
)

if "%TEST_MODE%"=="true" (
    java -jar hypestats.jar --test
) else (
    java -jar hypestats.jar
)

if errorlevel 1 (
    color 0c
    echo.
    echo Failed to run HypeStats.
    echo See error messages above.
    echo.
    pause
    exit /b 1
)

echo.
echo HypeStats has exited successfully.
echo.
pause
EOF

# Create Unix launcher
cat > release/run.sh << 'EOF'
#!/bin/bash

# Set terminal colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Show banner
echo -e "${GREEN}"
echo "HypeStats - Hypixel Bedwars Stats Companion"
echo -e "${NC}"

# Check if test mode flag is passed
TEST_MODE=false
if [ "$1" == "--test" ] || [ "$1" == "-t" ]; then
    TEST_MODE=true
    echo -e "${YELLOW}Starting HypeStats in TEST MODE...${NC}"
    echo "This mode simulates API calls and log file reading for testing purposes."
else
    echo "Starting HypeStats..."
fi

# Check for Java
if ! command -v java &> /dev/null; then
    echo -e "${RED}ERROR: Java not found!${NC}"
    echo "Please install Java 17 or higher."
    echo "Visit: https://adoptium.net/"
    exit 1
fi

if [ "$TEST_MODE" = true ]; then
    java -jar hypestats.jar --test
else
    java -jar hypestats.jar
fi

if [ $? -ne 0 ]; then
    echo -e "${RED}"
    echo "Failed to run HypeStats."
    echo "See error messages above."
    echo -e "${NC}"
    exit 1
fi

echo
echo "HypeStats has exited successfully."
echo
read -p "Press [Enter] to exit..."
EOF

# Make the shell script executable
chmod +x release/run.sh

echo
echo "Creating release ZIP file..."
zip -r HypeStats-Release.zip release/* > /dev/null

echo
echo -e "${GREEN}Release package created successfully.${NC}"
echo "Files are available in the 'release' directory and in 'HypeStats-Release.zip'"
echo 