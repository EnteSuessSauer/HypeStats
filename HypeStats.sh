#!/bin/bash

# Set terminal colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Show banner
echo -e "${GREEN}"
echo "HypeStats - Hypixel Stats Companion"
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

# Check for Maven
MVN_FOUND=false

# First check if mvn is in PATH
if command -v mvn &> /dev/null; then
    MVN_FOUND=true
    MVN_PATH="mvn"
else
    # Try common Maven installation locations
    COMMON_PATHS=(
        "/usr/bin/mvn"
        "/usr/local/bin/mvn"
        "$HOME/apache-maven/bin/mvn"
        "$HOME/maven/bin/mvn"
    )
    
    for path in "${COMMON_PATHS[@]}"; do
        if [ -x "$path" ]; then
            MVN_FOUND=true
            MVN_PATH="$path"
            break
        fi
    done
fi

if [ "$MVN_FOUND" = false ]; then
    echo -e "${RED}ERROR: Maven not found!${NC}"
    echo "Please install Maven from: https://maven.apache.org/download.cgi"
    echo "Make sure it's added to your PATH."
    exit 1
fi

# Create target directory if it doesn't exist
mkdir -p target

echo "Building and running the application..."
echo

if [ "$TEST_MODE" = true ]; then
    "$MVN_PATH" clean javafx:run -Djavafx.args="--test"
else
    "$MVN_PATH" clean javafx:run
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