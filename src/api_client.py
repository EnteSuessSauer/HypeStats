"""
API Client for the Hypixel Stats Companion App.
Handles communication with Hypixel and Mojang APIs.
"""
import time
import requests
from typing import Dict, Any, Optional, List
import threading
from collections import deque

from src.utils import config

class RateLimiter:
    """
    Implements a sliding window rate limiter for API calls.
    
    The Hypixel API has a rate limit of 300 requests per 5 minutes per API key.
    This class ensures we don't exceed that limit by tracking when requests were made
    and waiting if necessary.
    
    Thread safety is ensured through proper use of locks with context managers,
    and by minimizing lock hold times to prevent blocking other threads unnecessarily.
    """
    
    def __init__(self, max_requests, time_window):
        """
        Initialize the rate limiter.
        
        Args:
            max_requests (int): Maximum number of requests allowed in the time window.
            time_window (int): Time window in seconds.
        """
        self.max_requests = max_requests  # 300 requests
        self.time_window = time_window    # 5 minutes (300 seconds)
        self.request_timestamps = deque()
        self._lock = threading.Lock()
    
    def wait_if_needed(self):
        """
        Wait if the rate limit would be exceeded by making a request now.
        Uses a context manager for the lock to ensure proper release even when exceptions occur.
        """
        current_time = time.time()
        wait_time = 0
        
        # Use a context manager to ensure the lock is always released
        with self._lock:
            # Remove timestamps outside the window
            while self.request_timestamps and self.request_timestamps[0] < current_time - self.time_window:
                self.request_timestamps.popleft()
            
            if len(self.request_timestamps) >= self.max_requests:
                # Calculate how long to wait for the oldest request to expire
                wait_time = self.request_timestamps[0] + self.time_window - current_time
                wait_time = max(wait_time, 0)  # Ensure non-negative
        
        # Sleep outside the lock to not block other threads unnecessarily
        if wait_time > 0:
            time.sleep(wait_time)
        
        # Record this request - separate lock acquisition to minimize lock hold time
        with self._lock:
            self.request_timestamps.append(time.time())

class ApiClient:
    """
    Client for interacting with Hypixel and Mojang APIs.
    Handles authentication, requests, and error handling.
    """
    
    def __init__(self, api_key=None) -> None:
        """
        Initialize the API client with the given API key.
        
        Args:
            api_key (str, optional): The Hypixel API key. If not provided,
                                     attempts to get it from configuration.
        """
        self.api_key = api_key or config.get_api_key()
        self.session = requests.Session()
        
        # Initialize rate limiter (300 requests per 5 minutes)
        self.rate_limiter = RateLimiter(300, 300)
        
        # Mojang: 600 requests per 10 minutes
        self.mojang_limiter = RateLimiter(600, 600)  # 600 seconds = 10 minutes
        
    def _make_request(self, url: str, params: Optional[Dict[str, str]] = None, is_hypixel: bool = True) -> Dict[str, Any]:
        """
        Make an HTTP GET request to the specified URL with parameters.
        
        Args:
            url: The URL to make the request to.
            params: Optional dictionary of query parameters.
            is_hypixel: Whether this is a Hypixel API request (True) or a Mojang API request (False).
            
        Returns:
            Dict[str, Any]: The JSON response from the API.
            
        Raises:
            requests.RequestException: For any request-related errors.
            ValueError: For API errors (non-200 status codes).
        """
        # Apply rate limiting based on which API we're calling
        if is_hypixel:
            self.rate_limiter.wait_if_needed()
        else:
            self.mojang_limiter.wait_if_needed()
        
        try:
            response = self.session.get(url, params=params)
            
            if response.status_code != 200:
                raise ValueError(f"API request failed with status code {response.status_code}: {response.text}")
            
            response_json = response.json()
            
            # Handle Hypixel API specific error responses
            if 'success' in response_json and not response_json['success']:
                error_msg = response_json.get('cause', 'Unknown API error')
                raise ValueError(f"API request failed: {error_msg}")
            
            return response_json
        except requests.RequestException as e:
            raise requests.RequestException(f"Request to {url} failed: {str(e)}")
    
    def get_uuid(self, username: str) -> str:
        """
        Get a player's UUID from their Minecraft username using the Mojang API.
        
        Args:
            username: The Minecraft username.
            
        Returns:
            str: The player's UUID.
            
        Raises:
            ValueError: If the player doesn't exist or API request fails.
            requests.RequestException: For any request-related errors.
        """
        url = f"https://api.mojang.com/users/profiles/minecraft/{username}"
        
        try:
            response = self._make_request(url, is_hypixel=False)
            
            if not response or 'id' not in response:
                raise ValueError(f"Player '{username}' not found")
            
            return response['id']
        except requests.RequestException as e:
            raise requests.RequestException(f"Failed to get UUID for {username}: {str(e)}")
        except ValueError as e:
            raise ValueError(f"Failed to get UUID for {username}: {str(e)}")
    
    def get_player_stats(self, uuid: str) -> Dict[str, Any]:
        """
        Get a player's statistics from the Hypixel API.
        
        Args:
            uuid: The player's UUID.
            
        Returns:
            Dict[str, Any]: The player's statistics.
            
        Raises:
            ValueError: If the player doesn't exist or API request fails.
            requests.RequestException: For any request-related errors.
        """
        url = "https://api.hypixel.net/player"
        params = {
            "key": self.api_key,
            "uuid": uuid
        }
        
        try:
            response = self._make_request(url, params, is_hypixel=True)
            
            if not response.get('player'):
                raise ValueError(f"Player with UUID '{uuid}' not found")
            
            return response['player']
        except requests.RequestException as e:
            raise requests.RequestException(f"Failed to get stats for player {uuid}: {str(e)}")
        except ValueError as e:
            raise ValueError(f"Failed to get stats for player {uuid}: {str(e)}")
    
    def get_player_status(self, uuid: str) -> Dict[str, Any]:
        """
        Get a player's online status from the Hypixel API.
        
        Args:
            uuid: The player's UUID.
            
        Returns:
            Dict[str, Any]: The player's online status.
            
        Raises:
            ValueError: If the API request fails.
            requests.RequestException: For any request-related errors.
        """
        url = "https://api.hypixel.net/status"
        params = {
            "key": self.api_key,
            "uuid": uuid
        }
        
        try:
            response = self._make_request(url, params, is_hypixel=True)
            
            return response['session']
        except requests.RequestException as e:
            raise requests.RequestException(f"Failed to get status for player {uuid}: {str(e)}")
        except ValueError as e:
            raise ValueError(f"Failed to get status for player {uuid}: {str(e)}")
        except KeyError:
            # This is a special case where the API might return success: true but no session
            raise ValueError(f"Session data for player {uuid} not available")
    
    def verify_api_key(self) -> bool:
        """
        Verify that the API key is valid by making a test request.
        
        Returns:
            bool: True if the API key is valid, False otherwise.
            
        Raises:
            requests.RequestException: For any request-related errors.
        """
        # First try the key endpoint, which is specifically for key validation
        url = "https://api.hypixel.net/key"
        params = {"key": self.api_key}
        
        try:
            response = self._make_request(url, params, is_hypixel=True)
            
            # Check if the API key is valid
            if response.get('success', False):
                # Additional debug information about the key
                key_info = response.get('record', {})
                key_owner = key_info.get('owner', 'Unknown')
                print(f"API Key validated successfully. Owner: {key_owner}")
                return True
            else:
                # Log the error for debugging
                error = response.get('cause', 'Unknown error')
                print(f"API Key validation failed: {error}")
                
                # If the key endpoint fails, it might be deprecated, so try an alternative endpoint
                return self._try_alternative_validation()
                
        except (ValueError, requests.RequestException) as e:
            # More detailed error logging
            print(f"API Key validation error with /key endpoint: {str(e)}")
            
            # Try alternative method if the first one fails
            return self._try_alternative_validation()
    
    def _try_alternative_validation(self) -> bool:
        """
        Alternative method to validate API key if the primary method fails.
        Uses the player endpoint with a known existing player UUID (Hypixel's own account).
        
        Returns:
            bool: True if the API key is valid, False otherwise.
        """
        # Hypixel's account UUID - should always exist as a fallback check
        hypixel_uuid = "f7c77d999f154a66a87dc4a51ef30d19"
        url = "https://api.hypixel.net/player"
        params = {
            "key": self.api_key,
            "uuid": hypixel_uuid
        }
        
        try:
            response = self._make_request(url, params, is_hypixel=True)
            valid = response.get('success', False)
            
            if valid:
                print("API Key validated successfully using alternative method")
            else:
                error = response.get('cause', 'Unknown error')
                print(f"Alternative API Key validation failed: {error}")
                
            return valid
        except (ValueError, requests.RequestException) as e:
            print(f"Alternative API Key validation error: {str(e)}")
            return False 