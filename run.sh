#!/bin/bash

# Check if the JAR file exists
if [ ! -f "target/hypestats-1.0-SNAPSHOT.jar" ]; then
    echo "JAR file not found. Building project with Maven..."
    mvn clean package
fi

# Run the application
java -jar target/hypestats-1.0-SNAPSHOT.jar 