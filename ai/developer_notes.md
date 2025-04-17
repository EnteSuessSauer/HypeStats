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

- **API Client**: Handles communication with external APIs with thread-safe rate limiting
- **Log Monitor**: Watches and parses Minecraft log files using daemon threads for stability
- **Stats Processor**: Transforms raw API data into usable statistics with batched processing
- **Ranking Engine**: Sorts players based on their statistics
- **Nick Detector**: Attempts to identify players using nicknames
- **UI Components**: PyQt6-based user interface with event-based programming

## Key Areas for Improvement

### 1. Rate Limiting

The current implementation uses a simple time-delay approach to rate limiting:

```python
if time_since_last_request < 0.5:  # Wait at least 0.5 seconds between requests
    time.sleep(0.5 - time_since_last_request)
```

This is adequate for basic usage but has several limitations:

- Doesn't properly distribute requests over time
- Doesn't account for varying rate limits between APIs

**Recommendation**: Implement a token bucket algorithm or use a rate limiting library like `ratelimit`.

### 2. UI Responsiveness

The application uses a predominantly single-threaded design with Qt's event loop for UI responsiveness:

- Batch processing of data with intermediate UI updates
- Strategic calls to QCoreApplication.processEvents() to keep UI responsive
- Progress indication is provided during data fetching
- UI operations are kept minimal during processing

This approach simplifies the code and avoids thread synchronization issues while still maintaining responsiveness:

- Processing is performed in small batches to allow UI updates
- Long-running operations are broken into chunks with regular event processing
- Daemon threads are used for file monitoring with proper cleanup

**Recommendation**: Continue to optimize the batch processing approach by fine-tuning batch sizes and update frequencies based on different hardware capabilities.

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
5. **UI Responsiveness**: Large data fetching operations can cause brief UI pauses

## Thread Safety Considerations

When working with this codebase, keep in mind these threading-related guidelines:

1. **Minimal Threading**: The application is designed to minimize thread usage; avoid introducing new threads unless absolutely necessary
2. **Thread Cleanup**: Always use proper cleanup mechanisms (timeouts, try-finally, context managers) when working with threads
3. **UI Updates**: Never update the UI directly from a non-main thread; always use signals/slots or callbacks
4. **Batch Processing**: For long operations, follow the batch processing pattern with QCoreApplication.processEvents() to maintain UI responsiveness
5. **Lock Usage**: When using locks, always use them with context managers to ensure proper release
6. **Daemon Threads**: Background threads should be marked as daemon threads to prevent them from blocking application shutdown
7. **Graceful Shutdown**: Always handle exceptions during application shutdown to ensure resources are properly released

## Future Feature Ideas

1. **Historical Tracking**: Track player stats over time to show improvement
2. **Party Integration**: Detect and analyze your party members automatically
3. **Game Mode Filtering**: Specialized stats for different Bedwars modes (Solo, Doubles, etc.)
4. **Notification System**: Alert when highly skilled players join the lobby
5. **Integration with Discord**: Share lobby analysis with teammates
6. **Custom Ranking**: Allow users to define their own ranking criteria 
7. **Asynchronous API Processing**: Implement truly non-blocking API calls for improved responsiveness 