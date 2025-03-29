#!/bin/bash

# Check if test mode flag is passed
TEST_MODE=false
if [ "$1" = "--test" ] || [ "$1" = "-t" ]; then
    TEST_MODE=true
    echo "Running in TEST MODE - API and log file reading will be simulated"
fi

# Check for Java
if ! command -v java &> /dev/null; then
    echo "ERROR: Java is not installed or not in the PATH."
    echo "Please install Java 17 or higher and try again."
    exit 1
fi

# Create the target directory if it doesn't exist
mkdir -p target

# Check if the JAR file exists
if [ ! -f "target/hypestats-1.0-SNAPSHOT.jar" ]; then
    echo "JAR file not found. Building project with Maven..."
    
    # Check for Maven
    if ! command -v mvn &> /dev/null; then
        echo "ERROR: Maven is not installed or not in the PATH."
        echo ""
        echo "Please install Maven or use a pre-built version of HypeStats."
        echo "You can download Maven from: https://maven.apache.org/download.cgi"
        exit 1
    fi
    
    mvn clean package
    
    if [ $? -ne 0 ]; then
        echo ""
        echo "Maven build failed. Here are possible reasons:"
        echo "1. Java version incompatibility - Make sure you're using Java 17+"
        echo "2. Dependency issues - Check if you're connected to the internet"
        echo "3. Compiler errors - Check the output above for specific errors"
        echo ""
        echo "To see more detailed error messages, try running:"
        echo "mvn clean package -X"
        echo ""
        exit 1
    fi
fi

# Run the application
if [ "$TEST_MODE" = true ]; then
    java -jar target/hypestats-1.0-SNAPSHOT.jar --test
else
    java -jar target/hypestats-1.0-SNAPSHOT.jar 