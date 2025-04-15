# Developer Notes

This document contains notes and considerations for developers working on the Hypixel Stats Companion App.

## Technology Stack

The application is built using:

- **Python 3.8+**: Core programming language
- **PyQt6**: GUI framework
- **requests**: HTTP client for API communication
- **watchdog**: File system monitoring for Minecraft logs
- **configparser**: Configuration file management
- **pytest/pytest-mock**: Testing framework

## Getting Started

1. Clone the repository
2. Create a virtual environment: `python -m venv .venv`
3. Activate the environment:
   - Windows: `.venv\Scripts\activate`
   - Unix/macOS: `source .venv/bin/activate`
4. Install dependencies: `pip install -r requirements.txt`
5. Edit `config.ini` to set your Hypixel API key and Minecraft log path
6. Run the application: `python -m src.main`

## Code Structure

The codebase follows a modular structure:

- **API Client**: Handles communication with external APIs
- **Log Monitor**: Watches and parses Minecraft log files
- **Stats Processor**: Transforms raw API data into usable statistics
- **Ranking Engine**: Sorts players based on their statistics
- **Nick Detector**: Attempts to identify players using nicknames
- **UI Components**: PyQt6-based user interface

## Key Areas for Improvement

### 1. Rate Limiting

The current implementation uses a simple time-delay approach to rate limiting:

```python
if time_since_last_request < 0.5:  # Wait at least 0.5 seconds between requests
    time.sleep(0.5 - time_since_last_request)
```

This is adequate for basic usage but has several limitations:

- Not suitable for concurrent requests
- Doesn't properly distribute requests over time
- Doesn't account for varying rate limits between APIs

**Recommendation**: Implement a token bucket algorithm or use a rate limiting library like `ratelimit`.

### 2. UI Responsiveness

While the application uses threading to avoid freezing the UI during API calls, there are still areas where the UI could be more responsive:

- Better progress indication during data fetching
- More incremental updates as data becomes available
- More graceful handling of API timeouts

**Recommendation**: Explore using `asyncio` with PyQt, or implement a more sophisticated worker thread pool.

### 3. Nick Detection Accuracy

The current nick detection uses basic heuristics that are prone to false positives and negatives. This is a challenging problem with no perfect solution, but several improvements are possible:

- Collect data on confirmed nicked players to improve heuristics
- Consider machine learning approaches with more features
- Add user feedback mechanisms to improve the system over time

**Recommendation**: Start by adding more heuristics and fine-tuning the existing ones based on user feedback.

### 4. Caching Strategy

The application currently doesn't cache any data, resulting in repeated API calls for the same information. Implementing intelligent caching would:

- Reduce API calls and improve performance
- Reduce the risk of hitting rate limits
- Enable offline functionality for previously retrieved data

**Recommendation**: Implement a tiered caching system with memory cache for active sessions and disk cache for persistent data.

### 5. Error Handling

While the application includes basic error handling, it could be more robust:

- More specific error types for different failure scenarios
- Better recovery mechanisms for transient failures
- More informative error messages for users

**Recommendation**: Implement a custom exception hierarchy and centralized error handling system.

## Configuration Management

The application uses a simple `config.ini` file for storing:

- Hypixel API key
- Minecraft log file path

Consider expanding this to include:

- UI preferences
- Rate limiting settings
- Caching behavior
- Nick detection sensitivity

## Packaging for Distribution

To create distributable packages:

1. **Windows**: Use PyInstaller or cx_Freeze to create a Windows executable
   ```
   pyinstaller --onefile --windowed src/main.py
   ```

2. **macOS**: Create a macOS application bundle
   ```
   pyinstaller --onefile --windowed --name "Hypixel Stats Companion" src/main.py
   ```

3. **Linux**: Package as an AppImage or use distribution-specific packaging

## Known Issues

1. **Log Detection**: The log file parsing is sensitive to Hypixel's formatting changes for the `/who` command
2. **API Limitations**: The Hypixel API sometimes returns incomplete data for certain players
3. **Nick Detection**: False positives in nick detection, especially for newer players
4. **UI Scaling**: Some UI elements may not scale properly on high DPI displays

## Future Feature Ideas

1. **Historical Tracking**: Track player stats over time to show improvement
2. **Party Integration**: Detect and analyze your party members automatically
3. **Game Mode Filtering**: Specialized stats for different Bedwars modes (Solo, Doubles, etc.)
4. **Notification System**: Alert when highly skilled players join the lobby
5. **Integration with Discord**: Share lobby analysis with teammates
6. **Custom Ranking**: Allow users to define their own ranking criteria 