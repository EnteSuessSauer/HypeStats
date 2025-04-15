"""
Log Monitor for Hypixel Stats Companion.
Monitors Minecraft log file for Hypixel /who command output.
"""
import os
import re
import time
import threading
from typing import Callable, List, Optional, Set

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
    
    def __init__(self, callback: Callable[[List[str]], None]) -> None:
        """
        Initialize the log monitor.
        
        Args:
            callback: Function to call when player names are extracted from the log.
        """
        self.log_file_path = config.get_log_file_path()
        self.callback = callback
        self.last_position = 0
        self.observer = None
        self.poll_thread = None
        self.running = False
        self.poll_interval = config.get_polling_interval()  # Get interval from config
        self.last_successful_read = 0  # Timestamp of last successful read
        
        # Regular expressions for parsing /who command output
        # Example: "ONLINE: Player1, Player2, Player3"
        self.who_pattern = re.compile(r"ONLINE:\s*(.*?)(?:\s*\(\d+\))?$")
        
        # Initialize the last position to the current file size if it exists
        if os.path.exists(self.log_file_path):
            try:
                self.last_position = os.path.getsize(self.log_file_path)
            except OSError as e:
                print(f"Error getting file size: {str(e)}")
                self.last_position = 0
                
        print(f"Log monitor initialized with polling interval: {self.poll_interval} seconds")
    
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
                for line in new_content.splitlines():
                    player_names = self._parse_who_output(line)
                    if player_names:
                        self.callback(player_names)
        
        except Exception as e:
            print(f"Error processing log file: {str(e)}")
    
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
        
        # Filter out any empty names
        player_names = [name for name in player_names if name]
        
        # Print debug info about detected players
        print(f"Detected players: {player_names}")
        
        # Remove duplicates while preserving order
        seen: Set[str] = set()
        unique_names = [name for name in player_names if not (name in seen or seen.add(name))]
        
        return unique_names 