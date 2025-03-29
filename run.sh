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

# Try different launch methods in order of preference
LAUNCH_SUCCESS=false

echo "Checking for available launch methods..."

# Try Method 1: Maven JavaFX Plugin (preferred)
if command -v mvn &> /dev/null; then
    echo "Launch Method: Maven JavaFX Plugin"
    
    if [ "$TEST_MODE" = true ]; then
        mvn javafx:run -Djavafx.args="--test"
    else
        mvn javafx:run
    fi
    
    if [ $? -eq 0 ]; then
        LAUNCH_SUCCESS=true
        exit 0
    else
        echo "Maven launch failed, trying alternative methods..."
    fi
fi

# Try Method 2: Check for JavaFX SDK
JAVAFX_FOUND=false

if [ -n "$JAVAFX_HOME" ] && [ -d "$JAVAFX_HOME/lib" ]; then
    JAVAFX_FOUND=true
else
    # Try to find JavaFX in common locations
    for p in \
        "/usr/lib/jvm/javafx-sdk"* \
        "/opt/javafx-sdk"* \
        "$HOME/javafx-sdk"* \
        "./javafx-sdk"*
    do
        if [ -f "$p/lib/javafx.base.jar" ]; then
            JAVAFX_HOME="$p"
            JAVAFX_FOUND=true
            echo "Found JavaFX SDK at: $JAVAFX_HOME"
            break
        fi
    done
fi

if [ "$JAVAFX_FOUND" = true ]; then
    echo "Launch Method: Java with JavaFX modules"
    
    if [ "$TEST_MODE" = true ]; then
        java --module-path "$JAVAFX_HOME/lib" --add-modules javafx.controls,javafx.fxml,javafx.web -jar target/hypestats-1.0-SNAPSHOT.jar --test
    else
        java --module-path "$JAVAFX_HOME/lib" --add-modules javafx.controls,javafx.fxml,javafx.web -jar target/hypestats-1.0-SNAPSHOT.jar
    fi
    
    if [ $? -eq 0 ]; then
        LAUNCH_SUCCESS=true
        exit 0
    else
        echo "JavaFX launch failed, trying next method..."
    fi
fi

# Try Method 3: Download JavaFX SDK automatically
if [ ! -d "javafx-sdk" ]; then
    echo -e "${YELLOW}JavaFX not found. Would you like to download it automatically? (Y/N)${NC}"
    read -r DOWNLOAD_CHOICE
    if [ "$DOWNLOAD_CHOICE" = "Y" ] || [ "$DOWNLOAD_CHOICE" = "y" ]; then
        echo "Downloading JavaFX SDK..."
        
        # Detect OS
        if [[ "$OSTYPE" == "darwin"* ]]; then
            # macOS
            curl -L https://download2.gluonhq.com/openjfx/17.0.2/openjfx-17.0.2_osx-x64_bin-sdk.zip -o javafx.zip
        else
            # Linux
            curl -L https://download2.gluonhq.com/openjfx/17.0.2/openjfx-17.0.2_linux-x64_bin-sdk.zip -o javafx.zip
        fi
        
        echo "Extracting JavaFX SDK..."
        unzip -q javafx.zip
        mv javafx-sdk-17.0.2 javafx-sdk
        rm javafx.zip
        JAVAFX_HOME="$(pwd)/javafx-sdk"
        
        echo "Launch Method: Java with downloaded JavaFX modules"
        
        if [ "$TEST_MODE" = true ]; then
            java --module-path "$JAVAFX_HOME/lib" --add-modules javafx.controls,javafx.fxml,javafx.web -jar target/hypestats-1.0-SNAPSHOT.jar --test
        else
            java --module-path "$JAVAFX_HOME/lib" --add-modules javafx.controls,javafx.fxml,javafx.web -jar target/hypestats-1.0-SNAPSHOT.jar
        fi
        
        if [ $? -eq 0 ]; then
            LAUNCH_SUCCESS=true
            exit 0
        fi
    fi
fi

if [ "$LAUNCH_SUCCESS" = false ]; then
    echo -e "${RED}"
    echo "============================================================"
    echo "Launch failed. Please try one of the following:"
    echo ""
    echo "1. Install Maven: https://maven.apache.org/download.cgi"
    echo "2. Install JavaFX SDK: https://openjfx.io/"
    echo "   and set JAVAFX_HOME environment variable"
    echo "3. Manually run: mvn javafx:run"
    echo "============================================================"
    echo -e "${NC}"
fi

echo ""
read -p "Press [Enter] to exit..." 