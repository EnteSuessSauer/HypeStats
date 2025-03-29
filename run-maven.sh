#!/bin/bash

echo "Starting HypeStats with Maven JavaFX plugin..."

# Check if test mode flag is passed
TEST_MODE=false
if [ "$1" = "--test" ] || [ "$1" = "-t" ]; then
    TEST_MODE=true
    echo "Running in TEST MODE with JavaFX - API and log file reading will be simulated"
fi

# Check for Java
if ! command -v java &> /dev/null; then
    echo "ERROR: Java is not installed or not in the PATH."
    echo "Please install Java 17 or higher and try again."
    exit 1
fi

# Check for Maven
if ! command -v mvn &> /dev/null; then
    echo "ERROR: Maven is not installed or not in the PATH."
    echo ""
    echo "Please install Maven from: https://maven.apache.org/download.cgi"
    exit 1
fi

# Run the application with JavaFX modules using Maven
if [ "$TEST_MODE" = true ]; then
    mvn javafx:run -Djavafx.args="--test"
else
    mvn javafx:run
fi 