#!/bin/bash

echo "Creating HypeStats release package..."
echo ""

# Check for Java
if ! command -v java &> /dev/null; then
    echo "ERROR: Java is not installed or not in the PATH."
    echo "Please install Java 17 or higher and try again."
    exit 1
fi

# Check for Maven
if ! command -v mvn &> /dev/null; then
    echo "ERROR: Maven is not installed or not in the PATH."
    echo "Please install Maven first."
    echo "You can download Maven from: https://maven.apache.org/download.cgi"
    exit 1
fi

echo "Building application with Maven..."
mvn clean package
if [ $? -ne 0 ]; then
    echo ""
    echo "Maven build failed."
    echo "Check the output above for errors."
    exit 1
fi

echo ""
echo "Creating release directory..."
rm -rf release
mkdir -p release/logs

echo ""
echo "Copying files to release directory..."
cp target/hypestats-1.0-SNAPSHOT.jar release/hypestats.jar
cp README.md release/
cp LICENSE release/ 2>/dev/null || true
cp run.bat release/
cp run.sh release/
cp HypeStats.bat release/
cp HypeStats-Test.bat release/

# Make script executable
chmod +x release/run.sh

echo ""
echo "Creating no-compile launcher for release..."
cat > release/HypeStats-Run.sh << 'EOT'
#!/bin/bash

echo "Starting HypeStats..."

# Check for Java
if ! command -v java &> /dev/null; then
    echo "ERROR: Java is not installed or not in the PATH."
    echo "Please install Java 17 or higher and try again."
    exit 1
fi

# Run the application
java -jar hypestats.jar
EOT

cat > release/HypeStats-Test-Run.sh << 'EOT'
#!/bin/bash

echo "Starting HypeStats in TEST MODE..."
echo ""
echo "This mode simulates API calls and log file reading for testing purposes."
echo "All errors and events are logged to the ./logs directory."
echo ""

# Check for Java
if ! command -v java &> /dev/null; then
    echo "ERROR: Java is not installed or not in the PATH."
    echo "Please install Java 17 or higher and try again."
    exit 1
fi

# Run the application in test mode
java -jar hypestats.jar --test
EOT

# Make the scripts executable
chmod +x release/HypeStats-Run.sh
chmod +x release/HypeStats-Test-Run.sh

echo ""
echo "Creating release tarball..."
tar -czvf HypeStats-Release.tar.gz -C release .

echo ""
echo "Release package created successfully."
echo "Files are available in the 'release' directory and in 'HypeStats-Release.tar.gz'"
echo "" 