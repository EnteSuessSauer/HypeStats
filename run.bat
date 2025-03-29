@echo off
echo HypeStats - Hypixel Bedwars Companion

REM Check if the JAR file exists
if not exist "target\hypestats-1.0-SNAPSHOT.jar" (
    echo JAR file not found. Building project with Maven...
    call mvn clean package
)

REM Run the application
java -jar target\hypestats-1.0-SNAPSHOT.jar 