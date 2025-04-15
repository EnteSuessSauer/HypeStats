"""
Tests for the API client.
"""
import pytest
import requests
import json
from unittest.mock import MagicMock, patch

from src.api_client import ApiClient

@pytest.fixture
def mock_api_key():
    """Fixture to mock the API key."""
    with patch('src.utils.config.get_api_key') as mock_get_api_key:
        mock_get_api_key.return_value = 'test_api_key'
        yield mock_get_api_key

@pytest.fixture
def api_client(mock_api_key):
    """Fixture to create an API client."""
    return ApiClient()

class TestApiClient:
    """Tests for the ApiClient class."""
    
    def test_init(self, api_client, mock_api_key):
        """Test ApiClient initialization."""
        assert api_client.api_key == 'test_api_key'
        assert isinstance(api_client.session, requests.Session)
        mock_api_key.assert_called_once()
    
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
    
    def test_get_uuid_player_not_found(self, api_client, mocker):
        """Test get_uuid method when player is not found."""
        # Mock the _make_request method
        mocker.patch.object(api_client, '_make_request', return_value={})
        
        # Call the method and check for exception
        with pytest.raises(ValueError, match="Player 'TestPlayer' not found"):
            api_client.get_uuid('TestPlayer')
    
    def test_get_player_stats(self, api_client, mocker):
        """Test get_player_stats method."""
        # Mock the _make_request method
        mock_player_data = {'success': True, 'player': {'uuid': 'test_uuid', 'stats': {}}}
        mocker.patch.object(api_client, '_make_request', return_value=mock_player_data)
        
        # Call the method
        result = api_client.get_player_stats('test_uuid')
        
        # Verify the result
        assert result == mock_player_data['player']
        api_client._make_request.assert_called_once_with(
            'https://api.hypixel.net/player', 
            {'key': 'test_api_key', 'uuid': 'test_uuid'}
        )
    
    def test_get_player_stats_player_not_found(self, api_client, mocker):
        """Test get_player_stats method when player is not found."""
        # Mock the _make_request method
        mock_response = {'success': True, 'player': None}
        mocker.patch.object(api_client, '_make_request', return_value=mock_response)
        
        # Call the method and check for exception
        with pytest.raises(ValueError, match="Player with UUID 'test_uuid' not found"):
            api_client.get_player_stats('test_uuid')
    
    def test_get_player_status(self, api_client, mocker):
        """Test get_player_status method."""
        # Mock the _make_request method
        mock_status_data = {'success': True, 'session': {'online': True}}
        mocker.patch.object(api_client, '_make_request', return_value=mock_status_data)
        
        # Call the method
        result = api_client.get_player_status('test_uuid')
        
        # Verify the result
        assert result == mock_status_data['session']
        api_client._make_request.assert_called_once_with(
            'https://api.hypixel.net/status', 
            {'key': 'test_api_key', 'uuid': 'test_uuid'}
        )
    
    def test_get_player_status_no_session(self, api_client, mocker):
        """Test get_player_status method when session data is not available."""
        # Mock the _make_request method
        mock_response = {'success': True}  # No session key
        mocker.patch.object(api_client, '_make_request', return_value=mock_response)
        
        # Call the method and check for exception
        with pytest.raises(ValueError, match="Session data for player test_uuid not available"):
            api_client.get_player_status('test_uuid')
    
    def test_make_request(self, api_client, mocker):
        """Test _make_request method."""
        # Mock the requests.Session.get method
        mock_response = MagicMock()
        mock_response.status_code = 200
        mock_response.json.return_value = {'success': True, 'data': 'test_data'}
        mocker.patch.object(api_client.session, 'get', return_value=mock_response)
        
        # Mock time.time and time.sleep
        mocker.patch('time.time', side_effect=[0, 1])  # First call returns 0, second call returns 1
        mocker.patch('time.sleep')
        
        # Call the method
        result = api_client._make_request('https://test.url', {'param': 'value'})
        
        # Verify the result
        assert result == {'success': True, 'data': 'test_data'}
        api_client.session.get.assert_called_once_with('https://test.url', params={'param': 'value'})
        
    def test_make_request_error_status_code(self, api_client, mocker):
        """Test _make_request method with error status code."""
        # Mock the requests.Session.get method
        mock_response = MagicMock()
        mock_response.status_code = 404
        mock_response.text = 'Not Found'
        mocker.patch.object(api_client.session, 'get', return_value=mock_response)
        
        # Mock time.time and time.sleep
        mocker.patch('time.time', side_effect=[0, 1])
        mocker.patch('time.sleep')
        
        # Call the method and check for exception
        with pytest.raises(ValueError, match="API request failed with status code 404: Not Found"):
            api_client._make_request('https://test.url')
    
    def test_make_request_api_error(self, api_client, mocker):
        """Test _make_request method with API error."""
        # Mock the requests.Session.get method
        mock_response = MagicMock()
        mock_response.status_code = 200
        mock_response.json.return_value = {'success': False, 'cause': 'Invalid API key'}
        mocker.patch.object(api_client.session, 'get', return_value=mock_response)
        
        # Mock time.time and time.sleep
        mocker.patch('time.time', side_effect=[0, 1])
        mocker.patch('time.sleep')
        
        # Call the method and check for exception
        with pytest.raises(ValueError, match="API request failed: Invalid API key"):
            api_client._make_request('https://test.url')
    
    def test_make_request_request_exception(self, api_client, mocker):
        """Test _make_request method with request exception."""
        # Mock the requests.Session.get method to raise an exception
        mocker.patch.object(api_client.session, 'get', side_effect=requests.RequestException('Connection error'))
        
        # Mock time.time and time.sleep
        mocker.patch('time.time', side_effect=[0, 1])
        mocker.patch('time.sleep')
        
        # Call the method and check for exception
        with pytest.raises(requests.RequestException, match="Request to https://test.url failed: Connection error"):
            api_client._make_request('https://test.url') 