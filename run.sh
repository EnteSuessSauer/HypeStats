#!/bin/bash

# Check if test mode flag is passed
TEST_MODE=false
if [ "$1" = "--test" ] || [ "$1" = "-t" ]; then
    TEST_MODE=true
    echo "Running in TEST MODE - API and log file reading will be simulated"
fi

# Check if the JAR file exists
if [ ! -f "target/hypestats-1.0-SNAPSHOT.jar" ]; then
    echo "JAR file not found. Building project with Maven..."
    mvn clean package
fi

# Run the application
if [ "$TEST_MODE" = true ]; then
    java -jar target/hypestats-1.0-SNAPSHOT.jar --test
else
    java -jar target/hypestats-1.0-SNAPSHOT.jar 