# Testing Strategy

This document outlines the testing approach for the Hypixel Stats Companion App.

## Testing Framework

The project uses the following testing tools:

- **pytest**: The main testing framework
- **pytest-mock**: For mocking dependencies during tests

## Running Tests

To run the test suite, execute the following command from the project root:

```bash
pytest tests/
```

To run a specific test file:

```bash
pytest tests/test_api_client.py
```

To run with verbose output:

```bash
pytest -v tests/
```

## Test Structure

Tests are organized in the `tests/` directory, with test files named according to the module they test:

- `tests/test_api_client.py`: Tests for the Hypixel and Mojang API client
- `tests/test_ranking_engine.py`: Tests for the player ranking algorithms
- `tests/test_stats_processor.py`: Tests for stat calculation and processing
- `tests/test_nick_detector.py`: Tests for nick detection heuristics
- `tests/test_log_monitor.py`: Tests for Minecraft log file monitoring

## Testing Approach

### Unit Tests

Unit tests focus on testing individual functions and classes in isolation. Dependencies are mocked to ensure we're only testing the functionality of the unit itself.

Example of a unit test with mocking:

```python
def test_get_uuid(self, api_client, mocker):
    """Test get_uuid method."""
    # Mock the _make_request method
    mock_response = {'id': 'test_uuid', 'name': 'TestPlayer'}
    mocker.patch.object(api_client, '_make_request', return_value=mock_response)
    
    # Call the method
    result = api_client.get_uuid('TestPlayer')
    
    # Verify the result
    assert result == 'test_uuid'
    api_client._make_request.assert_called_once_with('https://api.mojang.com/users/profiles/minecraft/TestPlayer')
```

### Integration Tests

Integration tests verify that different components work together correctly. These tests still use mocks for external dependencies but allow internal components to interact.

### Mocking Strategy

External APIs and file system operations are always mocked to ensure:

1. Tests are fast and don't depend on external services
2. Tests are reliable and don't fail due to network issues
3. We can simulate different API responses and error conditions

Example of API mocking:

```python
@pytest.fixture
def mock_api_response(monkeypatch):
    """Mock requests.get for API responses."""
    def mock_get(*args, **kwargs):
        mock_response = MagicMock()
        mock_response.status_code = 200
        mock_response.json.return_value = {
            'success': True,
            'player': {
                'displayname': 'TestPlayer',
                'stats': {
                    'Bedwars': {
                        'wins_bedwars': 100,
                        'losses_bedwars': 50
                    }
                }
            }
        }
        return mock_response
    
    monkeypatch.setattr(requests, 'get', mock_get)
```

## Test Coverage Areas

### API Client Tests

- Test successful API calls
- Test error handling for various HTTP status codes
- Test parsing of API responses
- Test rate limiting behavior

### Stats Processor Tests

- Test calculation of derived metrics (FKDR, WLR, etc.)
- Test handling of missing or invalid data
- Test extraction of relevant stats from raw API data
- Test handling of edge cases (division by zero, etc.)

### Ranking Engine Tests

- Test sorting of players by different criteria
- Test handling of players with missing stats
- Test rank assignment
- Test filtering and top player selection

### Nick Detector Tests

- Test individual heuristics
- Test overall nick probability calculation
- Test textual description mapping
- Test lobby analysis

### Log Monitor Tests

- Test log file reading
- Test pattern matching for player names
- Test handling of file system events
- Test callback invocation

## Future Test Improvements

1. **UI Testing**: Add tests for the PyQt6 UI components using a specialized UI testing framework
2. **End-to-End Tests**: Add tests that simulate a full user interaction flow
3. **Property-Based Testing**: Use property-based testing for functions with complex logic
4. **Benchmarking Tests**: Add performance tests to ensure the application remains fast
5. **Test Fixtures**: Expand test fixtures to cover more test scenarios
6. **CI/CD Integration**: Set up automated testing in a CI/CD pipeline 