"""
Client for the Mojang API to handle username to UUID conversion.
"""

import requests
import logging
import time

logger = logging.getLogger(__name__)

class MojangAPI:
    """
    Client for the Mojang API to handle username to UUID conversion.
    """
    def __init__(self):
        """Initialize the Mojang API client."""
        self.uuid_cache = {}  # Cache UUID lookups to reduce API calls
        self.rate_limit_remaining = 600  # Mojang API rate limit (600 requests per 10 minutes)
        self.rate_limit_reset_time = time.time() + 600  # Time when rate limit resets
    
    def get_uuid(self, username):
        """
        Convert Minecraft username to UUID using Mojang API.
        
        Args:
            username (str): Minecraft username
            
        Returns:
            str: UUID of the player or None if not found
        """
        # Check cache first
        if username in self.uuid_cache:
            logger.debug(f"UUID cache hit for {username}")
            return self.uuid_cache[username]
        
        # Check rate limit
        current_time = time.time()
        if current_time > self.rate_limit_reset_time:
            self.rate_limit_remaining = 600
            self.rate_limit_reset_time = current_time + 600
        
        if self.rate_limit_remaining <= 0:
            logger.warning("Mojang API rate limit reached. Waiting for reset.")
            return None
        
        # Call Mojang API
        try:
            response = requests.get(f"https://api.mojang.com/users/profiles/minecraft/{username}")
            self.rate_limit_remaining -= 1
            
            if response.status_code == 200:
                uuid = response.json().get("id")
                if uuid:
                    # Cache the result
                    self.uuid_cache[username] = uuid
                    return uuid
            elif response.status_code == 429:
                logger.warning("Mojang API rate limit reached (429 response)")
                self.rate_limit_remaining = 0
                return None
            else:
                logger.warning(f"Failed to get UUID for {username}: HTTP {response.status_code}")
                return None
        except Exception as e:
            logger.error(f"Error getting UUID for {username}: {e}")
            return None 