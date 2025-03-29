@echo off
echo Creating standalone HypeStats JAR...

REM Build with Maven
call mvn clean package

REM Check if the build was successful
if not exist "target\hypestats-1.0-SNAPSHOT.jar" (
    echo Maven build failed!
    exit /b 1
)

echo Build successful! You can run the application with:
echo run.bat
echo.
echo Or directly with:
echo java -jar target\hypestats-1.0-SNAPSHOT.jar 