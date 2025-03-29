# HypeStats - Hypixel Bedwars Stats Companion

HypeStats is a standalone Java application for Minecraft Bedwars players, allowing you to check stats for yourself and players in your current lobby.

## Features

- **Player Lookup**: Search for any Minecraft player and view their detailed Bedwars statistics
- **Lobby Tracking**: Automatically detects players in your current Bedwars lobby by monitoring your Minecraft log file
- **Live Stats**: View win/loss ratios, K/D ratios, beds broken, and other important stats
- **Desktop Application**: JavaFX-based UI that works alongside your Minecraft game

## Requirements

- Java 17 or higher
- Minecraft with Hypixel Bedwars
- Hypixel API key
- Maven (for building or running with the Maven JavaFX plugin)

## Quick Start

### Windows
For the simplest experience, use the Maven-based launcher:
```
HypeStats-Maven.bat
```

For running with test mode:
```
HypeStats-Maven.bat --test
```

### Unix/Linux/macOS
```bash
# Make the script executable (first time only)
chmod +x run-maven.sh

# Run the application
./run-maven.sh

# Or run in test mode
./run-maven.sh --test
```

## Running with a Standalone JAR
If you prefer not to use Maven at runtime, you can create a standalone release package. This requires installing JavaFX separately.

1. Create a release package:
```
create-release.bat   # On Windows
./create-release.sh  # On Unix/Linux/macOS
```

2. Install JavaFX SDK from [openjfx.io](https://openjfx.io/)

3. Set the JAVAFX_HOME environment variable:
```
set JAVAFX_HOME=C:\path\to\javafx-sdk-17.0.2   # Windows
export JAVAFX_HOME=/path/to/javafx-sdk-17.0.2  # Unix/Linux/macOS
```

4. Run the standalone JAR:
```
java --module-path "%JAVAFX_HOME%\lib" --add-modules javafx.controls,javafx.fxml,javafx.web -jar hypestats.jar
```

## Test Mode

For testing purposes without requiring access to the Hypixel API or actual Minecraft log files:

### Windows
```
HypeStats-Maven.bat --test
```

### Unix/Linux/macOS
```bash
./run-maven.sh --test
```

In test mode:
- The application will show [TEST MODE] in the window title
- The API service will generate random mock player data
- The log file reader will simulate player joins and game events
- All errors and events are logged to the `./logs` directory for debugging

## Manual Installation

1. Clone this repository
2. Build with Maven:
   ```
   mvn clean package
   ```
3. Run with the JavaFX Maven plugin:
   ```
   mvn javafx:run
   ```

## Troubleshooting

### JavaFX Runtime Components Missing
If you see an error like "JavaFX runtime components are missing", use one of these solutions:

1. Use the Maven-based launcher: `HypeStats-Maven.bat` or `run-maven.sh`

2. Install JavaFX SDK and set JAVAFX_HOME environment variable:
   ```
   set JAVAFX_HOME=C:\path\to\javafx-sdk-17.0.2
   java --module-path "%JAVAFX_HOME%\lib" --add-modules javafx.controls,javafx.fxml,javafx.web -jar target\hypestats-1.0-SNAPSHOT.jar
   ```

### Maven Not Found
If you see "Maven is not installed or not in the PATH":
1. Download Maven from [maven.apache.org](https://maven.apache.org/download.cgi)
2. Install it and add it to your PATH

## Usage

### Setting Your API Key

1. Get a Hypixel API key by joining the Hypixel Minecraft server (mc.hypixel.net) and typing `/api new` in chat
2. Enter this API key when prompted in the app

### Player Lookup

Use the search box on the Player Lookup tab to look up any player's Bedwars statistics.

### Lobby Tracking

1. Go to the Lobby Tracker tab
2. Confirm the path to your Minecraft log file (default paths are automatically detected)
3. Click "Start Monitoring" to begin detecting players in your lobby

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

## Technologies Used

- JavaFX for the user interface
- Maven for dependency management and building
- OkHttp for API calls
- Gson for JSON parsing
- SLF4J with Logback for logging
- Hypixel & Mojang APIs

## API Rate Limiting

This application respects the Hypixel API rate limits of 120 requests per minute. The app implements client-side rate limiting to ensure you stay within these limits.

## License

MIT

## Disclaimer

This project is not affiliated with or endorsed by Hypixel or Mojang. Use at your own risk and in accordance with the Hypixel Terms of Service. 