"""
Log Monitor for Hypixel Stats Companion.
Monitors Minecraft log file for Hypixel /who command output.
"""
import os
import re
import time
import threading
from typing import Callable, List, Optional, Set, Dict

from watchdog.observers import Observer
from watchdog.events import FileSystemEventHandler, FileModifiedEvent

from src.utils import config

class LogEventHandler(FileSystemEventHandler):
    """
    File system event handler for watching log file changes.
    """
    
    def __init__(self, callback: Callable[[], None]) -> None:
        """
        Initialize the event handler with a callback.
        
        Args:
            callback: Function to call when the log file is modified.
        """
        self.callback = callback
        
    def on_modified(self, event: FileModifiedEvent) -> None:
        """
        Called when a file is modified.
        
        Args:
            event: The file modification event.
        """
        # Call the callback function when the log file is modified
        self.callback()

class LogMonitor:
    """
    Monitor the Minecraft log file for Hypixel /who command output.
    Uses a hybrid approach with file system events and periodic polling.
    """
    
    def __init__(self, callback: Callable[[List[str]], None], team_callback: Optional[Callable[[str, str], None]] = None) -> None:
        """
        Initialize the log monitor.
        
        Args:
            callback: Function to call when player names are extracted from the log.
            team_callback: Function to call when team color information is found for a player.
        """
        self.log_file_path = config.get_log_file_path()
        self.callback = callback
        self.team_callback = team_callback
        self.last_position = 0
        self.observer = None
        self.poll_thread = None
        self.running = False
        self.poll_interval = config.get_polling_interval()  # Get interval from config
        self.last_successful_read = 0  # Timestamp of last successful read
        
        # Store all players seen in the current session
        self.all_players: Set[str] = set()
        self.last_lobby_change_time = 0  # Timestamp of last lobby change
        
        # Regular expressions for parsing /who command output
        # Example: "ONLINE: Player1, Player2, Player3"
        self.who_pattern = re.compile(r"ONLINE:\s*(.*?)(?:\s*\(\d+\))?$")
        
        # Regular expression for team color detection in Bedwars
        # Example: "[TEAM] Player1: message" or "Player1 has joined the RED team!"
        self.team_color_pattern = re.compile(r"(?:\[TEAM\])|(?:has joined the (\w+) team)")
        self.team_chat_pattern = re.compile(r"\[TEAM\] (\w+)")
        self.team_join_pattern = re.compile(r"(\w+) has joined the (\w+) team")
        
        # Regex to detect lobby changes
        self.lobby_join_pattern = re.compile(r"You joined the lobby!")
        self.game_start_pattern = re.compile(r"The game has started!")
        
        # Store player team colors
        self.player_teams: Dict[str, str] = {}
        
        # Initialize the last position to the current file size if it exists
        if os.path.exists(self.log_file_path):
            try:
                self.last_position = os.path.getsize(self.log_file_path)
            except OSError as e:
                print(f"Error getting file size: {str(e)}")
                self.last_position = 0
                
        print(f"Log monitor initialized with polling interval: {self.poll_interval} seconds")
    
    def get_player_team(self, player_name: str) -> Optional[str]:
        """
        Get the team color for a player.
        
        Args:
            player_name: The player's username.
            
        Returns:
            Optional[str]: The team color if known, None otherwise.
        """
        return self.player_teams.get(player_name)
    
    def get_all_player_teams(self) -> Dict[str, str]:
        """
        Get all known player team colors.
        
        Returns:
            Dict[str, str]: Dictionary mapping player names to team colors.
        """
        return self.player_teams.copy()
    
    def clear_player_teams(self) -> None:
        """
        Clear all stored player team information.
        This should be called when joining a new game.
        """
        self.player_teams.clear()
    
    def reset_lobby(self) -> None:
        """
        Reset the player list for a new lobby or game.
        """
        self.all_players.clear()
        self.player_teams.clear()
        self.last_lobby_change_time = time.time()
        print("Lobby reset - cleared player list and team information")
        
    def start(self) -> None:
        """
        Start monitoring the log file using both file system events and polling.
        """
        if self.running:
            return
        
        self.running = True
        
        # Create a watchdog observer to monitor the log file's directory
        log_dir = os.path.dirname(self.log_file_path)
        self.observer = Observer()
        event_handler = LogEventHandler(self._process_new_lines)
        
        # Schedule the observer to watch the log file's directory
        self.observer.schedule(event_handler, log_dir, recursive=False)
        self.observer.start()
        
        # Start a polling thread as a backup mechanism for buffered writes
        self.poll_thread = threading.Thread(target=self._poll_log_file, daemon=True)
        self.poll_thread.start()
        
        # Reset the player list when starting monitoring
        self.reset_lobby()
        
        print(f"Started monitoring log file: {self.log_file_path}")
    
    def _poll_log_file(self) -> None:
        """
        Periodically poll the log file for changes, regardless of file system events.
        This helps catch buffered writes that might not trigger a file system event.
        """
        while self.running:
            try:
                # Force a read of the log file
                self._process_new_lines(force_read=True)
            except Exception as e:
                print(f"Error polling log file: {str(e)}")
            
            # Wait before polling again
            time.sleep(self.poll_interval)
    
    def stop(self) -> None:
        """
        Stop monitoring the log file.
        """
        if not self.running:
            return
        
        self.running = False
        
        # Stop the observer
        if self.observer:
            self.observer.stop()
            self.observer.join()
            self.observer = None
        
        # The poll_thread will stop when self.running becomes False
        # We don't need to join it as it's a daemon thread
        
        print("Stopped monitoring log file")
    
    def force_check(self) -> None:
        """
        Manually force a check of the log file.
        Can be called when the user wants to refresh the player list.
        """
        if self.running:
            self._process_new_lines(force_read=True)
    
    def _process_new_lines(self, force_read: bool = False) -> None:
        """
        Process new lines in the log file.
        Extracts player names from /who command output and calls the callback.
        Also looks for team color information in chat messages.
        
        Args:
            force_read: Whether to force reading the file even if it doesn't appear to have changed.
        """
        try:
            # Check if the file exists
            if not os.path.exists(self.log_file_path):
                print(f"Log file not found: {self.log_file_path}")
                return
            
            # Get the current file size
            current_size = os.path.getsize(self.log_file_path)
            
            # If the file has been truncated, reset position
            if current_size < self.last_position:
                self.last_position = 0
            
            # If there's no new content and we're not forcing a read, do nothing
            if current_size <= self.last_position and not force_read:
                return
            
            # If we're forcing a read but the file hasn't changed, try to reopen it
            # This can help with buffering issues
            if force_read and current_size == self.last_position:
                # Only force-reread if we haven't successfully read in the last 10 seconds
                current_time = time.time()
                if current_time - self.last_successful_read < 10:
                    return
                    
                # Try to reopen the file and read from our last position
                pass  # Continue to file reading code
            
            # Read new content - use a small buffer size to reduce OS buffering
            with open(self.log_file_path, 'r', encoding='utf-8', errors='replace', buffering=1) as file:
                file.seek(self.last_position)
                new_content = file.read()
                
                # Only update position if we actually read something or if we're at the end
                if new_content or current_size == self.last_position:
                    self.last_position = current_size
                    self.last_successful_read = time.time()
            
            # Process each line
            if new_content:
                player_names_detected = False
                team_color_changed = False
                lobby_changed = False
                
                for line in new_content.splitlines():
                    # Check for lobby/game state changes
                    if self.lobby_join_pattern.search(line) or self.game_start_pattern.search(line):
                        self.reset_lobby()
                        lobby_changed = True
                        continue
                        
                    # Check for player list
                    player_names = self._parse_who_output(line)
                    if player_names:
                        # Update all_players set with new players
                        self.all_players.update(player_names)
                        player_names_detected = True
                    
                    # Check for team color information
                    if self._parse_team_color_info(line):
                        team_color_changed = True
                
                # After processing all lines, send the complete player list if we detected new players
                if player_names_detected or team_color_changed or lobby_changed:
                    # Convert set back to list for callback
                    if self.all_players:
                        self.callback(list(self.all_players))
                
                # If no player names were detected but team information changed
                # we might want to notify the UI to update with the latest team information
                elif team_color_changed and not player_names_detected:
                    player_names = list(self.player_teams.keys())
                    if player_names:
                        self.callback(player_names)
        
        except Exception as e:
            print(f"Error processing log file: {str(e)}")
    
    def _strip_minecraft_formatting(self, text: str) -> str:
        """
        Strip Minecraft formatting codes from text.
        
        Args:
            text: Text that may contain Minecraft formatting codes.
            
        Returns:
            str: Text with formatting codes removed.
        """
        # First handle standard Minecraft color codes (§ followed by a color code)
        clean_text = re.sub(r'§[0-9a-fklmnor]', '', text)
        
        # Handle corrupted encoding sequences that might appear in log files
        # This includes characters that might be malformed in UTF-8
        clean_text = re.sub(r'[\x00-\x1F\x7F-\xFF]', '', clean_text)
        
        # As a final cleanup, remove any non-alphanumeric characters from the end
        # This catches any remaining weird sequences at the end of names
        clean_text = re.sub(r'[^a-zA-Z0-9_]+$', '', clean_text)
        
        return clean_text
    
    def _parse_who_output(self, line: str) -> Optional[List[str]]:
        """
        Parse a log line to check if it matches the Hypixel /who command output.
        
        Args:
            line: A single line from the log file.
            
        Returns:
            List[str]: List of player names if the line matches, None otherwise.
        """
        # Check if the line matches the /who command output pattern
        match = self.who_pattern.search(line)
        if not match:
            return None
        
        # Extract the player names part
        players_text = match.group(1).strip()
        
        # If there are no players, return an empty list
        if not players_text or players_text == "None":
            return []
        
        # Split by comma and clean up each name
        player_names = [name.strip() for name in players_text.split(',')]
        
        # Filter out any empty names and strip formatting codes
        player_names = [self._strip_minecraft_formatting(name) for name in player_names if name]
        
        # Print debug info about detected players
        print(f"Detected {len(player_names)} players in /who command")
        
        # Don't clear existing players - only add new ones
        # We want to preserve the player list between /who commands
        for name in player_names:
            if name not in self.all_players:
                print(f"Adding new player: {name}")
                self.all_players.add(name)
        
        # Return the complete current known player list
        return list(self.all_players)
    
    def _parse_team_color_info(self, line: str) -> bool:
        """
        Parse a log line to extract team color information.
        
        Args:
            line: A single line from the log file.
            
        Returns:
            bool: True if team color information was found, False otherwise.
        """
        found_team_info = False
        
        # Check for team chat messages
        team_chat_match = self.team_chat_pattern.search(line)
        if team_chat_match:
            # Someone is chatting in team chat, they're on the same team as the player
            player_name = team_chat_match.group(1)
            if player_name:
                # Clean the player name
                player_name = self._strip_minecraft_formatting(player_name)
                
                # Use 'YOUR_TEAM' as a placeholder for the player's team
                if player_name and player_name not in self.player_teams:
                    self.player_teams[player_name] = 'YOUR_TEAM'
                    if self.team_callback:
                        self.team_callback(player_name, 'YOUR_TEAM')
                    print(f"Detected player {player_name} in your team")
                    found_team_info = True
            return found_team_info
        
        # Check for team join messages
        team_join_match = self.team_join_pattern.search(line)
        if team_join_match:
            player_name = team_join_match.group(1)
            team_color = team_join_match.group(2)
            if player_name and team_color:
                # Clean the player name
                player_name = self._strip_minecraft_formatting(player_name)
                
                self.player_teams[player_name] = team_color.upper()
                # Also add to all_players set in case this is a new player
                self.all_players.add(player_name)
                if self.team_callback:
                    self.team_callback(player_name, team_color.upper())
                print(f"Detected player {player_name} joined {team_color.upper()} team")
                found_team_info = True
            return found_team_info
            
        return found_team_info 