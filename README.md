## HypeStats - Hypixel Stats Companion

HypeStats is a companion application for Minecraft players that provides easy access to Hypixel statistics, lobby tracking, and game analysis.

### Features

- Player statistics lookup
- Real-time tracking of players in your lobby
- Win/loss predictions based on player statistics
- Automatic detection of game events through Minecraft log file analysis

### Requirements

- Java 17 or higher (OpenJDK or Oracle JDK)
- One of the following:
  - Maven 3.6.0 or higher (for auto-compilation and running)
  - JavaFX SDK 17+ (or let HypeStats download it for you)

### Installation

1. Download or clone the repository
2. Ensure Java 17+ is installed and available in your PATH
   - Verify with `java -version` in your terminal/command prompt
3. Optionally install Maven (required for development)
   - Windows: Download from [Maven's website](https://maven.apache.org/download.cgi) and add to PATH
   - Mac: `brew install maven`
   - Linux: Use your distribution's package manager (e.g., `apt install maven`)

### Quick Start

#### Windows
1. Double-click `HypeStats.bat`
2. The launcher will automatically detect Maven (even if not in PATH), check dependencies, and run the application

#### Mac/Linux
1. Make the launcher executable: `chmod +x HypeStats.sh`
2. Run `./HypeStats.sh`
3. The launcher will automatically detect and use the best available method to run the application

### Test Mode

To start the application in test mode (simulated API calls and log file reading):
- Windows: `HypeStats.bat --test` or `HypeStats.bat -t`
- Mac/Linux: `./HypeStats.sh --test` or `./HypeStats.sh -t`

### Troubleshooting

#### Missing JavaFX Runtime Components

If you see an error about missing JavaFX runtime components, HypeStats will offer to download JavaFX for you. You can also:

1. Download and install JavaFX SDK from [openjfx.io](https://openjfx.io/)
2. Set the JAVAFX_HOME environment variable to the JavaFX SDK location
   - Windows: `set JAVAFX_HOME=C:\path\to\javafx-sdk`
   - Mac/Linux: `export JAVAFX_HOME=/path/to/javafx-sdk`
3. Run the launcher again

#### Maven Issues

If Maven fails to build the project:
1. Make sure you have Java 17 or higher installed
   - Windows: Check your Java version with `java -version` 
   - Make sure JAVA_HOME is correctly set: `echo %JAVA_HOME%`
2. Check your internet connection for downloading dependencies
3. If you're behind a proxy, configure Maven's settings.xml
4. On Windows, the launcher will automatically search for Maven in common installation directories

### Development

To build and run HypeStats manually:

```bash
# Compile and run
mvn clean javafx:run

# Or compile and run in test mode
mvn clean javafx:run -Djavafx.args="--test"
```

### License

This project is licensed under the MIT License.

This means:
- You are free to use, modify, and distribute this software
- You can use the software for commercial purposes
- You can modify and create derivative works
- The only requirement is to include the original copyright notice and license text

The full license text is available in the LICENSE file included with this project.

## Project Structure

- `src/main/java/com/hypestats/` - Java source files
  - `controller/` - JavaFX controllers for UI interaction
  - `model/` - Data models
  - `util/` - Utility classes for API calls, settings management, etc.
- `src/main/resources/` - Application resources
  - `css/` - CSS styling
  - `fxml/` - FXML layout files
  - `images/` - Application images
- `logs/` - Development and error logs (created during test mode)
- `scripts/` - Build and release scripts
  - `create-release.bat` - Windows release script
  - `create-release.sh` - Mac/Linux release script

## Technologies Used

- JavaFX for the user interface
- Maven for dependency management and building
- OkHttp for API calls
- Gson for JSON parsing
- SLF4J with Logback for logging
- Hypixel & Mojang APIs

## API Rate Limiting

This application respects the Hypixel API rate limits of 120 requests per minute. The app implements client-side rate limiting to ensure you stay within these limits.

## Disclaimer

This project is not affiliated with or endorsed by Hypixel or Mojang. Use at your own risk and in accordance with the Hypixel and Mojang Terms of Service. 