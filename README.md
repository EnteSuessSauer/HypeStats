## HypeStats - Hypixel Bedwars Stats Companion

HypeStats is a companion application for Minecraft Bedwars players that provides easy access to player statistics, lobby tracking, and game analysis.

### Features

- Player statistics lookup
- Real-time tracking of players in your lobby
- Win/loss predictions based on player statistics
- Automatic detection of game events through Minecraft log file analysis

### Requirements

- Java 17 or higher
- One of the following:
  - Maven (for auto-compilation and running)
  - JavaFX SDK (or let HypeStats download it for you)

### Quick Start

#### Windows
1. Download or clone the repository
2. Run `HypeStats.bat`
3. The launcher will automatically detect and use the best available method to run the application

#### Mac/Linux
1. Download or clone the repository
2. Make the launcher executable: `chmod +x run.sh`
3. Run `./run.sh`
4. The launcher will automatically detect and use the best available method to run the application

### Test Mode

To start the application in test mode (simulated API calls and log file reading):
- Windows: `HypeStats.bat --test` or `HypeStats.bat -t`
- Mac/Linux: `./run.sh --test` or `./run.sh -t`

### Troubleshooting

#### Missing JavaFX Runtime Components

If you see an error about missing JavaFX runtime components, HypeStats will offer to download JavaFX for you. You can also:

1. Download and install JavaFX SDK from [openjfx.io](https://openjfx.io/)
2. Set the JAVAFX_HOME environment variable to the JavaFX SDK location
3. Run the launcher again

#### Maven Issues

If Maven fails to build the project:
1. Make sure you have Java 17 or higher installed
2. Check your internet connection for downloading dependencies
3. If you're behind a proxy, configure Maven's settings.xml

### Development

To build and run HypeStats manually:

```bash
# Compile and run
mvn clean javafx:run

# Or compile and run in test mode
mvn clean javafx:run -Djavafx.args="--test"
```

### License

This project is licensed under the MIT License - see the LICENSE file for details.

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

## Disclaimer

This project is not affiliated with or endorsed by Hypixel or Mojang. Use at your own risk and in accordance with the Hypixel Terms of Service. 