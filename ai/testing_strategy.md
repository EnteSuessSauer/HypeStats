# Testing Strategy

This document outlines the testing approach for the Hypixel Stats Companion application.

## Testing Goals

1. Ensure application reliability across different systems
2. Detect regressions early in the development process
3. Maintain code quality and application stability
4. Validate error handling and edge cases
5. Ensure proper API integration

## Testing Levels

### Unit Testing

Unit tests focus on testing individual components in isolation, with dependencies mocked or stubbed.

**Key Components to Test:**

- **ApiClient**: Test UUID lookups, stat retrieval, rate limiting, and error handling
- **LogMonitor**: Test log parsing logic, player name extraction, and team color detection
- **StatsProcessor**: Test calculation of derived statistics (FKDR, WLR, etc.)
- **RankingEngine**: Test sorting algorithms and ranking criteria
- **NickDetector**: Test nick probability calculations and heuristics

**Unit Testing Tools:**

- **pytest**: Primary testing framework
- **pytest-mock**: For mocking dependencies
- **unittest.mock**: For patching and mocking standard library components

**Example Unit Test:**

```python
def test_extract_player_names_from_who_command():
    log_monitor = LogMonitor(mock.MagicMock(), None)
    log_line = "ONLINE: Player1, Player2, Player3 (3)"
    
    result = log_monitor._parse_who_output(log_line)
    
    assert result == ["Player1", "Player2", "Player3"]
```

### Integration Testing

Integration tests verify that different components work correctly when combined.

**Key Integration Points:**

- **LogMonitor + UI**: Test that detected players are properly passed to the UI
- **ApiClient + StatsProcessor**: Test end-to-end stat retrieval and processing
- **StatsProcessor + RankingEngine + NickDetector**: Test the complete player processing pipeline

**Integration Testing Approach:**

- Use real component implementations where possible
- Mock external dependencies (APIs, file system)
- Test complete workflows from user perspective

**Example Integration Test:**

```python
def test_api_client_with_stats_processor(mocker):
    # Mock API responses
    mocker.patch('requests.get', return_value=mock_response)
    
    # Create real implementations
    api_client = ApiClient("test_key")
    
    # Test the integration
    uuid = api_client.get_uuid("TestPlayer")
    player_data = api_client.get_player_stats(uuid)
    processed_stats = stats_processor.extract_relevant_stats(player_data)
    
    assert processed_stats["username"] == "TestPlayer"
    assert "bedwars_stars" in processed_stats
    assert "fkdr" in processed_stats
```

### UI Testing

UI tests focus on the behavior of the application interface.

**UI Testing Strategy:**

- **Manual Testing**: For visual layout verification and subjective user experience
- **Automated Tests**: For functional aspects of the UI (table sorting, button actions, etc.)

**UI Testing Challenges:**

- PyQt6 UI components require special handling in automated tests
- Event-driven nature of the UI necessitates careful test design

**Automated UI Test Example:**

```python
def test_table_sorting(qtbot):
    # Create the main window
    window = MainWindow()
    qtbot.addWidget(window)
    
    # Populate with test data
    window.current_player_stats = [
        {"username": "Player1", "bedwars_stars": 100, "rank": 2},
        {"username": "Player2", "bedwars_stars": 500, "rank": 1}
    ]
    window._populate_table()
    
    # Click on the stars column header to sort
    qtbot.mouseClick(window.table.horizontalHeader(), Qt.LeftButton, pos=QPoint(200, 5))
    
    # Verify the sorting worked
    assert window.table.item(0, 1).text() == "Player2"
    assert window.table.item(1, 1).text() == "Player1"
```

### End-to-End Testing

End-to-end tests verify the complete application workflow in a realistic environment.

**E2E Testing Approaches:**

- **Manual Testing**: Following user workflows from start to finish
- **Recorded User Sessions**: For regression testing of common scenarios

**Key E2E Scenarios:**

1. Application startup and configuration
2. Log file monitoring and player detection
3. Player stats retrieval and display
4. Table sorting and filtering
5. Team color detection and display

## Testing Challenges

### API Testing Considerations

- **Rate Limiting**: Hypixel API has rate limits that must be respected during testing
- **Mocking**: API responses should be mocked to provide consistent test behavior
- **Error Handling**: Various API error conditions need to be tested

### File System Testing

- **Log File Variations**: Minecraft log files can vary across different Minecraft versions and launchers
- **File System Events**: Testing watchdog events requires careful test design
- **Cross-Platform Issues**: File paths and line endings can differ across operating systems

### UI Event Handling

- **Event Timing**: UI events and updates need to be synchronized properly in tests
- **Widget Testing**: QWidget testing requires proper handling of the Qt event loop
- **Visual Verification**: Some UI aspects are hard to verify programmatically

## Mock Data Strategy

To facilitate testing without hitting real APIs, we maintain mock data for:

1. **Mojang API**: Mock player UUID responses
2. **Hypixel API**: Mock player statistics responses
3. **Log Files**: Sample log file snippets for different scenarios

**Example Mock Data Location:**
- `tests/mock_data/api_responses/`: Mock API responses
- `tests/mock_data/log_files/`: Sample Minecraft log files

## Test Coverage Goals

- Core logic components: 90%+ coverage
- API clients and data processors: 80%+ coverage
- UI components: 60%+ coverage (with focus on functional aspects)

## Error Injection Testing

To ensure robustness, we deliberately inject errors during testing:

- Network failures during API calls
- Malformed API responses
- Corrupted log files
- Resource constraints (memory limitations)

## Performance Testing

Key performance metrics to monitor:

- UI responsiveness during API calls
- Memory usage with large player datasets
- Response time for player data processing
- Log file monitoring overhead

## QA Process

1. **Pre-Commit Testing**: Developers run unit tests before committing
2. **Continuous Integration**: Automated testing on each push/PR
3. **Manual QA**: Regular manual testing sessions for UI and usability
4. **Beta Testing**: Release candidates tested by users before final release

## Test Automation

Tests are automated through:

1. **pytest** for running all test types
2. **GitHub Actions** for CI/CD integration
3. **Pre-commit hooks** for local test running

## Special Testing Considerations

### Platform-Specific Testing

- Windows testing is prioritized as the primary platform
- macOS and Linux testing for cross-platform support

### Internationalization

- Testing with non-ASCII player names
- Testing with different locale settings

### Accessibility

- Basic keyboard navigation testing
- Color contrast verification for team colors 