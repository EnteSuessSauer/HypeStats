"""
Minecraft log reader for detecting lobby changes and extracting player names.
"""

import os
import sys
import time
import re
import logging

logger = logging.getLogger(__name__)

class MinecraftLogReader:
    """
    Reads and parses Minecraft log files to detect lobby changes and extract player names.
    """
    def __init__(self, log_path=None):
        """
        Initialize the log reader with the path to the Minecraft log file.
        
        Args:
            log_path (str, optional): Path to Minecraft log file. If None, auto-detection is attempted.
        """
        self.log_path = log_path
        self.last_position = 0
        self.last_check_time = 0
        self.lobby_players = []
        
        if self.log_path is None:
            # Try to find the logs directory
            self.detect_log_path()
    
    def detect_log_path(self):
        """Try to automatically detect the Minecraft logs path."""
        # Common locations for Minecraft logs
        possible_paths = []
        
        # Windows
        if os.name == 'nt':
            appdata = os.getenv('APPDATA')
            if appdata:
                possible_paths.append(os.path.join(appdata, '.minecraft', 'logs', 'latest.log'))
        
        # macOS
        elif sys.platform == 'darwin':
            home = os.path.expanduser('~')
            possible_paths.append(os.path.join(home, 'Library', 'Application Support', 'minecraft', 'logs', 'latest.log'))
        
        # Linux
        else:
            home = os.path.expanduser('~')
            possible_paths.append(os.path.join(home, '.minecraft', 'logs', 'latest.log'))
        
        # Check if any of the paths exist
        for path in possible_paths:
            if os.path.exists(path):
                self.log_path = path
                logger.info(f"Found Minecraft log file at: {path}")
                return
        
        logger.warning("Could not detect Minecraft log file automatically.")
    
    def check_logs_for_lobby_change(self):
        """
        Check if the player has joined a new lobby and extract player names.
        
        Returns:
            bool: True if a lobby change was detected and processed, False otherwise
        """
        if not self.log_path or not os.path.exists(self.log_path):
            return False
        
        # Only check logs every 2 seconds to avoid excessive file operations
        current_time = time.time()
        if current_time - self.last_check_time < 2:
            return False
        
        self.last_check_time = current_time
        
        try:
            # Get file size
            file_size = os.path.getsize(self.log_path)
            
            # If file size is smaller than last position, file has been rotated
            if file_size < self.last_position:
                self.last_position = 0
            
            # Only read new content
            if file_size > self.last_position:
                with open(self.log_path, 'r', encoding='utf-8', errors='ignore') as f:
                    f.seek(self.last_position)
                    new_content = f.read()
                    self.last_position = file_size
                
                # Look for lobby join messages
                if "ONLINE: " in new_content or "[CHAT] ONLINE:" in new_content:
                    # Extract player names from the message
                    self.parse_lobby_players(new_content)
                    return True
                
                # For Hypixel specifically, look for the lobby join message
                if "You are currently connected to server" in new_content:
                    # Wait a moment for the player list to appear
                    time.sleep(1)
                    return self.check_logs_for_lobby_change()
        
        except Exception as e:
            logger.error(f"Error reading log file: {e}")
        
        return False
    
    def parse_lobby_players(self, log_content):
        """
        Parse player names from lobby join log messages.
        
        Args:
            log_content (str): The log content to parse
        """
        # Different patterns to match player lists
        patterns = [
            r"ONLINE: (.*?)(?:\n|$)",  # Matches "ONLINE: player1, player2, ..."
            r"\[CHAT\] ONLINE:(.*?)(?:\n|$)",  # Matches "[CHAT] ONLINE: player1, player2, ..."
            r"There are \d+ players online:(.*?)(?:\n|$)"  # Matches "There are X players online: player1, player2, ..."
        ]
        
        for pattern in patterns:
            matches = re.findall(pattern, log_content)
            if matches:
                for match in matches:
                    # Split the player list by commas
                    players = [p.strip() for p in match.split(',')]
                    
                    # Additional cleanup for player names
                    clean_players = []
                    for player in players:
                        # Remove rank prefixes, colors, etc.
                        clean_name = re.sub(r'\[.*?\]|\s*\n.*', '', player).strip()
                        if clean_name:
                            clean_players.append(clean_name)
                    
                    if clean_players:
                        self.lobby_players = clean_players
                        logger.info(f"Found {len(clean_players)} players in lobby: {', '.join(clean_players[:5])}{'...' if len(clean_players) > 5 else ''}")
                        return
        
        # If no patterns matched but lobby change was detected
        # Try to find player names inline
        player_pattern = r"(?<=\s)[\w]{3,16}(?=\s|$)"
        players = re.findall(player_pattern, log_content)
        if players:
            self.lobby_players = list(set(players))
            logger.info(f"Extracted possible player names: {', '.join(self.lobby_players[:5])}{'...' if len(self.lobby_players) > 5 else ''}")

    def get_lobby_players(self):
        """
        Return the current list of players in the lobby.
        
        Returns:
            list: List of player names in the current lobby
        """
        return self.lobby_players 