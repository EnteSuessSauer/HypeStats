"""
Main window for the Hypixel Stats Companion App.
"""
import sys
import threading
import time
import os
from typing import List, Dict, Any, Optional

from PyQt6.QtCore import Qt, QObject, pyqtSignal, QThread, QTimer
from PyQt6.QtWidgets import (
    QMainWindow, QWidget, QVBoxLayout, QHBoxLayout, 
    QLineEdit, QPushButton, QTableWidget, QTableWidgetItem,
    QLabel, QStatusBar, QHeaderView, QMessageBox, QDialog,
    QFormLayout, QDialogButtonBox, QFileDialog, QComboBox,
    QTabWidget, QGridLayout, QGroupBox, QTextBrowser, QSpinBox,
    QProgressBar
)
from PyQt6.QtGui import QColor

from src.api_client import ApiClient
from src.log_monitor import LogMonitor
import src.stats_processor as stats_processor
import src.ranking_engine as ranking_engine
import src.nick_detector as nick_detector
from src.utils import config
import requests

class ApiKeyDialog(QDialog):
    """
    Dialog for entering the Hypixel API key.
    """
    
    def __init__(self, parent=None):
        """
        Initialize the API key dialog.
        """
        super().__init__(parent)
        
        self.setWindowTitle("Hypixel API Key Required")
        self.setFixedWidth(450)
        
        # Create layout
        layout = QVBoxLayout(self)
        
        # Add informational text
        info_label = QLabel(
            "A Hypixel API key is required to use this application.\n\n"
            "You can get your API key in two ways:\n\n"
            "1. Connect to Hypixel server (mc.hypixel.net) and run the command: /api new\n\n"
            "2. Visit the Hypixel Developer Dashboard: https://developer.hypixel.net/"
        )
        info_label.setWordWrap(True)
        layout.addWidget(info_label)
        
        # Create form layout for input
        form_layout = QFormLayout()
        layout.addLayout(form_layout)
        
        # Add API key input
        self.api_key_input = QLineEdit()
        self.api_key_input.setPlaceholderText("Enter your Hypixel API key here")
        form_layout.addRow("API Key:", self.api_key_input)
        
        # Add status label for validation feedback
        self.status_label = QLabel("")
        self.status_label.setStyleSheet("color: red;")
        layout.addWidget(self.status_label)
        
        # Add buttons
        button_box = QDialogButtonBox(QDialogButtonBox.StandardButton.Ok | QDialogButtonBox.StandardButton.Cancel)
        button_box.accepted.connect(self.validate_and_accept)
        button_box.rejected.connect(self.reject)
        layout.addWidget(button_box)
    
    def validate_and_accept(self):
        """
        Validate the API key before accepting the dialog.
        """
        api_key = self.api_key_input.text().strip()
        
        # Basic validation - Hypixel API keys are 36 characters (UUID format)
        if not api_key:
            self.status_label.setText("API key cannot be empty")
            return
        
        # Changed validation rule to be more flexible with key format
        # Some keys might not follow the exact UUID format
        if len(api_key) < 32:
            self.status_label.setText("API key is too short (should be at least 32 characters)")
            return
        
        # Key looks valid, accept the dialog
        self.accept()
    
    def get_api_key(self) -> str:
        """
        Get the entered API key.
        
        Returns:
            str: The API key.
        """
        return self.api_key_input.text().strip()

class LogFileDialog(QDialog):
    """
    Dialog for selecting the Minecraft log file path.
    """
    
    def __init__(self, parent=None):
        """
        Initialize the log file dialog.
        """
        super().__init__(parent)
        
        self.setWindowTitle("Minecraft Log File Selection")
        self.setFixedWidth(500)
        
        # Create layout
        layout = QVBoxLayout(self)
        
        # Add informational text
        info_label = QLabel(
            "Select the Minecraft log file to monitor. This is usually 'latest.log' in your Minecraft logs folder.\n\n"
            "You can choose from common log file locations for different launchers or browse to find your log file."
        )
        info_label.setWordWrap(True)
        layout.addWidget(info_label)
        
        # Create form layout for input
        form_layout = QFormLayout()
        layout.addLayout(form_layout)
        
        # Add launcher selection combobox
        self.launcher_combo = QComboBox()
        self.launcher_combo.addItem("Select a launcher...", "")
        
        # Get default log paths and add to combobox
        self.log_paths = config.get_default_log_paths()
        for launcher_name, log_path in self.log_paths.items():
            self.launcher_combo.addItem(f"{launcher_name} ({log_path})", log_path)
        
        self.launcher_combo.currentIndexChanged.connect(self._update_path_from_combo)
        form_layout.addRow("Launcher:", self.launcher_combo)
        
        # Add log path input with browse button
        path_layout = QHBoxLayout()
        self.log_path_input = QLineEdit()
        path_layout.addWidget(self.log_path_input)
        
        self.browse_button = QPushButton("Browse...")
        self.browse_button.clicked.connect(self._browse_for_log_file)
        path_layout.addWidget(self.browse_button)
        
        form_layout.addRow("Log File Path:", path_layout)
        
        # Add status label for validation feedback
        self.status_label = QLabel("")
        self.status_label.setStyleSheet("color: red;")
        layout.addWidget(self.status_label)
        
        # Add buttons
        button_box = QDialogButtonBox(QDialogButtonBox.StandardButton.Ok | QDialogButtonBox.StandardButton.Cancel)
        button_box.accepted.connect(self.validate_and_accept)
        button_box.rejected.connect(self.reject)
        layout.addWidget(button_box)
    
    def _update_path_from_combo(self, index: int) -> None:
        """
        Update the log path input when a launcher is selected.
        
        Args:
            index: The selected index in the combobox.
        """
        log_path = self.launcher_combo.currentData()
        if log_path:
            self.log_path_input.setText(log_path)
    
    def _browse_for_log_file(self) -> None:
        """
        Open a file dialog to browse for the log file.
        """
        file_path, _ = QFileDialog.getOpenFileName(
            self,
            "Select Minecraft Log File",
            os.path.expanduser("~"),
            "Log Files (*.log);;All Files (*.*)"
        )
        
        if file_path:
            self.log_path_input.setText(file_path)
    
    def validate_and_accept(self) -> None:
        """
        Validate the log file path before accepting the dialog.
        """
        log_path = self.log_path_input.text().strip()
        
        # Basic validation
        if not log_path:
            self.status_label.setText("Log file path cannot be empty")
            return
        
        # Check if the file exists
        if not os.path.exists(log_path):
            self.status_label.setText("The selected log file does not exist")
            return
        
        # Log path seems valid, accept the dialog
        self.accept()
    
    def get_log_file_path(self) -> str:
        """
        Get the entered log file path.
        
        Returns:
            str: The log file path.
        """
        return self.log_path_input.text().strip()

class StatsProcessor:
    """
    Process player stats synchronously.
    This is a replacement for the threaded version to simplify the application.
    """
    
    def __init__(self, usernames: List[str], api_client: ApiClient, existing_stats: List[Dict[str, Any]] = None) -> None:
        """
        Initialize the stats processor.
        
        Args:
            usernames: List of usernames to fetch stats for.
            api_client: The API client to use.
            existing_stats: List of existing player stats to preserve.
        """
        self.usernames = usernames
        self.api_client = api_client
        self.existing_stats = existing_stats if existing_stats else []
        
    def process(self, progress_callback=None) -> List[Dict[str, Any]]:
        """
        Process the usernames and fetch player stats.
        
        Args:
            progress_callback: Optional callback function to report progress (0-100)
            
        Returns:
            List[Dict[str, Any]]: The processed player stats
        """
        all_player_stats = []
        total_players = max(1, len(self.usernames))  # Ensure total_players is at least 1 to avoid division by zero
        processed_count = 0  # Track actual number of processed players
        
        # Start with existing player stats if available
        if self.existing_stats:
            all_player_stats = self.existing_stats.copy()
        
        try:
            for i, username in enumerate(self.usernames):
                # Skip players we already have stats for
                if self.existing_stats and any(player.get('username') == username for player in self.existing_stats):
                    # Count skipped players in progress calculation
                    processed_count += 1
                    if progress_callback:
                        progress_percent = min(99, int((processed_count / total_players) * 100))
                        progress_callback(progress_percent)
                    continue
                
                try:
                    # Update progress - make sure we don't reach 100% until completely done
                    processed_count += 1
                    if progress_callback:
                        progress_percent = min(99, int((processed_count / total_players) * 100))
                        progress_callback(progress_percent)
                    
                    # Get UUID for the username
                    try:
                        uuid = self.api_client.get_uuid(username)
                    except ValueError as e:
                        # If player not found in Mojang API, they are definitely nicked
                        if "Player '" in str(e) and "not found" in str(e):
                            print(f"Player {username} is definitely nicked (not found in Mojang API)")
                            placeholder_stats = {
                                'username': username,
                                'bedwars_stars': '?',
                                'fkdr': '?',
                                'wlr': '?',
                                'level': '?',
                                'achievement_points': '?',
                                'nick_probability': 1.0,  # 100% certain they're nicked
                                'nick_estimate': 'Confirmed Nick',
                                'is_placeholder': True  # Mark as placeholder for display purposes
                            }
                            all_player_stats.append(placeholder_stats)
                            continue
                        else:
                            # Re-raise other errors
                            raise
                    
                    # Get player stats
                    player_data = self.api_client.get_player_stats(uuid)
                    
                    # Process the stats
                    processed_stats = stats_processor.extract_relevant_stats(player_data)
                    
                    # Add to the list
                    all_player_stats.append(processed_stats)
                
                except Exception as e:
                    # Create placeholder stats for players that can't be found (likely nicked)
                    print(f"Error processing player {username}: {str(e)}")
                    
                    # Check if this is a Hypixel API error indicating player hasn't played Hypixel
                    is_confirmed_nick = False
                    error_msg = str(e).lower()
                    
                    if "player with uuid" in error_msg and "not found" in error_msg:
                        # This means they have a real Minecraft account but haven't played Hypixel
                        # Not necessarily a nick, so mark with lower probability
                        nick_probability = 0.6
                        nick_estimate = "Probable Nick"
                    elif "failed to get uuid" in error_msg:
                        # If we couldn't get UUID from Mojang, they're definitely nicked
                        nick_probability = 1.0
                        nick_estimate = "Confirmed Nick"
                        is_confirmed_nick = True
                    else:
                        # Other errors - use high probability but not certain
                        nick_probability = 0.9
                        nick_estimate = "Highly Likely Nick"
                    
                    # Create placeholder stats with appropriate nick probability
                    placeholder_stats = {
                        'username': username,
                        'bedwars_stars': '?',
                        'fkdr': '?',
                        'wlr': '?',
                        'level': '?',
                        'achievement_points': '?',
                        'nick_probability': nick_probability,
                        'nick_estimate': nick_estimate,
                        'is_placeholder': True,  # Mark as placeholder for display purposes
                        'is_confirmed_nick': is_confirmed_nick
                    }
                    all_player_stats.append(placeholder_stats)
            
            # Process the final stats
            final_stats = self._process_final_stats(all_player_stats)
            
            # Emit 100% progress if callback provided
            if progress_callback:
                progress_callback(100)
                
            return final_stats
            
        except Exception as e:
            # Make sure to emit 100% progress on error
            if progress_callback:
                progress_callback(100)
            print(f"Error fetching player stats: {str(e)}")
            # Return whatever we have so far
            return all_player_stats
    
    def _process_final_stats(self, player_stats: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """
        Process the final stats, including ranking and nick detection.
        
        Args:
            player_stats: List of player stats to process
            
        Returns:
            List[Dict[str, Any]]: Processed player stats
        """
        if not player_stats:
            return []
        
        try:
            # Only rank players with actual numeric stats
            valid_players = [p for p in player_stats if not p.get('is_placeholder', False)]
            placeholder_players = [p for p in player_stats if p.get('is_placeholder', False)]
            
            # Rank the valid players
            ranked_valid_stats = ranking_engine.rank_players(valid_players) if valid_players else []
            
            # Add placeholders at the end with incrementing ranks
            final_stats = []
            
            # Add valid ranked players
            for idx, player in enumerate(ranked_valid_stats):
                player['rank'] = idx + 1
                final_stats.append(player)
            
            # Add placeholder players with ranks after the valid players
            for idx, player in enumerate(placeholder_players):
                player['rank'] = len(ranked_valid_stats) + idx + 1
                final_stats.append(player)
            
            # Calculate nick probabilities for players with real stats
            for player in final_stats:
                if not player.get('is_placeholder', False) and not player.get('nick_probability'):
                    try:
                        nick_score = nick_detector.estimate_if_nicked(player)
                        player['nick_probability'] = nick_score
                        player['nick_estimate'] = nick_detector.get_nick_probability_description(nick_score)
                    except Exception as e:
                        # If nick detection fails, use a default
                        print(f"Error calculating nick probability for {player.get('username')}: {str(e)}")
                        player['nick_probability'] = 0.0
                        player['nick_estimate'] = "Unknown"
            
            return final_stats
        except Exception as e:
            print(f"Error in _process_final_stats: {str(e)}")
            # Return the original stats if something goes wrong
            return player_stats

class SettingsDialog(QDialog):
    """
    Dialog for changing application settings.
    """
    
    def __init__(self, parent=None):
        """
        Initialize the settings dialog.
        """
        super().__init__(parent)
        
        self.setWindowTitle("Settings")
        self.setFixedWidth(600)
        self.setMinimumHeight(400)
        
        # Create layout
        layout = QVBoxLayout(self)
        
        # Create tabs
        tab_widget = QTabWidget()
        layout.addWidget(tab_widget)
        
        # API Key tab
        api_key_tab = QWidget()
        tab_widget.addTab(api_key_tab, "API Key")
        
        api_key_layout = QVBoxLayout(api_key_tab)
        
        # Add API key info
        api_key_info = QLabel(
            "Set your Hypixel API key. You can get this by:\n\n"
            "1. Connect to Hypixel server (mc.hypixel.net) and run the command: /api new\n"
            "2. Visit the Hypixel Developer Dashboard: https://developer.hypixel.net/"
        )
        api_key_info.setWordWrap(True)
        api_key_layout.addWidget(api_key_info)
        
        # API key input
        api_key_form = QFormLayout()
        self.api_key_input = QLineEdit()
        
        # Get current API key if it exists
        try:
            current_api_key = config.get_api_key()
            self.api_key_input.setText(current_api_key)
        except:
            pass
            
        api_key_form.addRow("API Key:", self.api_key_input)
        api_key_layout.addLayout(api_key_form)
        
        # Test API key button
        self.test_api_key_button = QPushButton("Test API Key")
        self.test_api_key_button.clicked.connect(self._test_api_key)
        api_key_layout.addWidget(self.test_api_key_button)
        
        # API key status label
        self.api_key_status = QLabel("")
        api_key_layout.addWidget(self.api_key_status)
        
        api_key_layout.addStretch()
        
        # Log File tab
        log_file_tab = QWidget()
        tab_widget.addTab(log_file_tab, "Log File")
        
        log_file_layout = QVBoxLayout(log_file_tab)
        
        # Add log file info
        log_file_info = QLabel(
            "Set the path to your Minecraft log file. This is the file that the app "
            "will monitor for lobby information when you use the /who command in Hypixel."
        )
        log_file_info.setWordWrap(True)
        log_file_layout.addWidget(log_file_info)
        
        # Log file path form
        log_file_form = QFormLayout()
        
        # Add launcher selection combobox
        self.launcher_combo = QComboBox()
        self.launcher_combo.addItem("Select a launcher...", "")
        
        # Get default log paths and add to combobox
        self.log_paths = config.get_default_log_paths()
        for launcher_name, log_path in self.log_paths.items():
            self.launcher_combo.addItem(f"{launcher_name} ({log_path})", log_path)
        
        self.launcher_combo.currentIndexChanged.connect(self._update_path_from_combo)
        log_file_form.addRow("Launcher:", self.launcher_combo)
        
        # Log file path input with browse button
        path_layout = QHBoxLayout()
        self.log_path_input = QLineEdit()
        
        # Get current log path if it exists
        try:
            current_log_path = config.get_log_file_path()
            self.log_path_input.setText(current_log_path)
        except:
            pass
            
        path_layout.addWidget(self.log_path_input)
        
        self.browse_button = QPushButton("Browse...")
        self.browse_button.clicked.connect(self._browse_for_log_file)
        path_layout.addWidget(self.browse_button)
        
        log_file_form.addRow("Log File Path:", path_layout)
        log_file_layout.addLayout(log_file_form)
        
        # Add polling interval settings
        polling_form = QFormLayout()
        
        # Create spin box for poll interval (1-10 seconds)
        self.poll_interval_spin = QSpinBox()
        self.poll_interval_spin.setMinimum(1)
        self.poll_interval_spin.setMaximum(10)
        self.poll_interval_spin.setSuffix(" seconds")
        
        # Get current polling interval
        try:
            current_interval = config.get_polling_interval()
            self.poll_interval_spin.setValue(current_interval)
        except:
            self.poll_interval_spin.setValue(2)  # Default to 2 seconds
            
        polling_form.addRow("Log polling interval:", self.poll_interval_spin)
        
        # Add explanation of polling
        polling_explanation = QLabel(
            "The polling interval determines how frequently the app checks the log file for changes. "
            "A shorter interval (1-2 seconds) will detect players faster but use more resources. "
            "A longer interval (5-10 seconds) will use fewer resources but may be slower to detect players."
        )
        polling_explanation.setWordWrap(True)
        polling_explanation.setStyleSheet("color: gray; font-size: 9pt;")
        
        log_file_layout.addLayout(polling_form)
        log_file_layout.addWidget(polling_explanation)
        
        # Test log file button
        self.test_log_file_button = QPushButton("Test Log File")
        self.test_log_file_button.clicked.connect(self._test_log_file)
        log_file_layout.addWidget(self.test_log_file_button)
        
        # Log file status label
        self.log_file_status = QLabel("")
        log_file_layout.addWidget(self.log_file_status)
        
        log_file_layout.addStretch()
        
        # Add buttons
        button_box = QDialogButtonBox(QDialogButtonBox.StandardButton.Save | QDialogButtonBox.StandardButton.Cancel)
        button_box.accepted.connect(self.save_settings)
        button_box.rejected.connect(self.reject)
        layout.addWidget(button_box)
    
    def _update_path_from_combo(self, index: int) -> None:
        """
        Update the log path input when a launcher is selected.
        
        Args:
            index: The selected index in the combobox.
        """
        log_path = self.launcher_combo.currentData()
        if log_path:
            self.log_path_input.setText(log_path)
    
    def _browse_for_log_file(self) -> None:
        """
        Open a file dialog to browse for the log file.
        """
        file_path, _ = QFileDialog.getOpenFileName(
            self,
            "Select Minecraft Log File",
            os.path.expanduser("~"),
            "Log Files (*.log);;All Files (*.*)"
        )
        
        if file_path:
            self.log_path_input.setText(file_path)
    
    def _test_api_key(self) -> None:
        """
        Test if the entered API key is valid.
        """
        api_key = self.api_key_input.text().strip()
        
        if not api_key:
            self.api_key_status.setStyleSheet("color: red;")
            self.api_key_status.setText("API key cannot be empty")
            return
        
        self.api_key_status.setStyleSheet("color: orange;")
        self.api_key_status.setText("Testing API key...")
        
        try:
            # Create a temporary API client to test the key
            temp_client = ApiClient(api_key)
            if temp_client.verify_api_key():
                self.api_key_status.setStyleSheet("color: green;")
                self.api_key_status.setText("API key is valid!")
            else:
                self.api_key_status.setStyleSheet("color: red;")
                self.api_key_status.setText("API key validation failed. The key may be invalid or expired.")
        except Exception as e:
            self.api_key_status.setStyleSheet("color: red;")
            self.api_key_status.setText(f"Error testing API key: {str(e)}")
    
    def _test_log_file(self) -> None:
        """
        Test if the entered log file path is valid.
        """
        log_path = self.log_path_input.text().strip()
        
        if not log_path:
            self.log_file_status.setStyleSheet("color: red;")
            self.log_file_status.setText("Log file path cannot be empty")
            return
        
        if not os.path.exists(log_path):
            self.log_file_status.setStyleSheet("color: red;")
            self.log_file_status.setText("Log file does not exist at the specified path")
            return
        
        # Check if it's a readable file
        if not os.path.isfile(log_path):
            self.log_file_status.setStyleSheet("color: red;")
            self.log_file_status.setText("The specified path is not a file")
            return
        
        try:
            # Try to read the file
            with open(log_path, 'r', encoding='utf-8', errors='replace') as f:
                f.read(100)  # Read a small portion to test
            
            self.log_file_status.setStyleSheet("color: green;")
            self.log_file_status.setText("Log file is valid and readable!")
        except Exception as e:
            self.log_file_status.setStyleSheet("color: red;")
            self.log_file_status.setText(f"Error accessing log file: {str(e)}")
    
    def save_settings(self) -> None:
        """
        Save the settings and close the dialog.
        """
        # Save API key if it's not empty
        api_key = self.api_key_input.text().strip()
        if api_key:
            config.save_config("Hypixel", "API_KEY", api_key)
        
        # Save log file path if it's not empty
        log_path = self.log_path_input.text().strip()
        if log_path:
            config.save_config("Minecraft", "LOG_FILE_PATH", log_path)
            
        # Save polling interval
        poll_interval = self.poll_interval_spin.value()
        config.set_polling_interval(poll_interval)
        
        # Show a message that settings have been saved
        QMessageBox.information(self, "Settings Saved", "Your settings have been saved. Some changes may require restarting the application.")
        
        self.accept()

class PlayerDetailsDialog(QDialog):
    """
    Dialog for displaying detailed player statistics.
    """
    
    def __init__(self, player_data: Dict[str, Any], parent=None):
        """
        Initialize the player details dialog.
        
        Args:
            player_data: Dictionary containing processed player statistics.
            parent: Parent widget.
        """
        super().__init__(parent)
        
        self.player_data = player_data
        self.username = player_data.get('username', 'Unknown Player')
        
        self.setWindowTitle(f"Player Details: {self.username}")
        self.setMinimumSize(700, 500)
        
        # Create layout
        layout = QVBoxLayout(self)
        
        # Header with player name and level
        header_layout = QHBoxLayout()
        
        self.player_name_label = QLabel(f"<h1>{self.username}</h1>")
        header_layout.addWidget(self.player_name_label)
        
        hypixel_level = player_data.get('hypixel_level', 0)
        level_label = QLabel(f"<h2>Level: {hypixel_level:.1f}</h2>")
        header_layout.addWidget(level_label, alignment=Qt.AlignmentFlag.AlignRight)
        
        layout.addLayout(header_layout)
        
        # Nick detection warning if applicable
        nick_description = player_data.get('nick_description', '')
        if nick_description in ["Probably Nicked", "Very Likely Nicked"]:
            nick_warning = QLabel(f"⚠️ Nick Detection: {nick_description}")
            nick_warning.setStyleSheet("background-color: #fff3cd; color: #856404; padding: 5px; border-radius: 5px;")
            layout.addWidget(nick_warning)
        
        # Main content area
        content_layout = QHBoxLayout()
        
        # Left side: General stats
        general_stats_group = QGroupBox("General Statistics")
        general_stats_layout = QFormLayout(general_stats_group)
        
        general_stats = [
            ("Achievements Points", str(player_data.get('achievement_points', 0))),
            ("First Login", self._format_timestamp(player_data.get('first_login', 0))),
            ("Last Login", self._format_timestamp(player_data.get('last_login', 0))),
            ("Karma", str(player_data.get('karma', 0)))
        ]
        
        for label, value in general_stats:
            general_stats_layout.addRow(QLabel(f"<b>{label}:</b>"), QLabel(value))
        
        content_layout.addWidget(general_stats_group)
        
        # Right side: Bedwars stats
        bedwars_stats_group = QGroupBox("Bedwars Statistics")
        bedwars_stats_layout = QGridLayout(bedwars_stats_group)
        
        # Stars with special styling
        stars = player_data.get('bedwars_stars', 0)
        stars_label = QLabel(f"<h3>⭐ {stars}</h3>")
        bedwars_stats_layout.addWidget(stars_label, 0, 0, 1, 2, alignment=Qt.AlignmentFlag.AlignCenter)
        
        # Main stats in a grid
        bedwars_stats = [
            ("Wins", str(player_data.get('wins', 0)), "Losses", str(player_data.get('losses', 0))),
            ("Final Kills", str(player_data.get('final_kills', 0)), "Final Deaths", str(player_data.get('final_deaths', 0))),
            ("Beds Broken", str(player_data.get('beds_broken', 0)), "Beds Lost", str(player_data.get('beds_lost', 0))),
            ("WLR", f"{player_data.get('wlr', 0):.2f}", "FKDR", f"{player_data.get('fkdr', 0):.2f}"),
            ("BBLR", f"{player_data.get('bblr', 0):.2f}", "Winstreak", str(player_data.get('winstreak', 0)))
        ]
        
        row = 1
        for stat1_label, stat1_value, stat2_label, stat2_value in bedwars_stats:
            bedwars_stats_layout.addWidget(QLabel(f"<b>{stat1_label}:</b>"), row, 0)
            bedwars_stats_layout.addWidget(QLabel(stat1_value), row, 1)
            bedwars_stats_layout.addWidget(QLabel(f"<b>{stat2_label}:</b>"), row, 2)
            bedwars_stats_layout.addWidget(QLabel(stat2_value), row, 3)
            row += 1
        
        content_layout.addWidget(bedwars_stats_group)
        layout.addLayout(content_layout)
        
        # Add raw stats in a collapsible section
        raw_stats_group = QGroupBox("Raw Data (Advanced)")
        raw_stats_group.setCheckable(True)
        raw_stats_group.setChecked(False)
        raw_stats_layout = QVBoxLayout(raw_stats_group)
        
        raw_stats_browser = QTextBrowser()
        raw_stats_browser.setPlainText(str(player_data))
        raw_stats_layout.addWidget(raw_stats_browser)
        
        layout.addWidget(raw_stats_group)
        
        # Add close button
        button_box = QDialogButtonBox(QDialogButtonBox.StandardButton.Close)
        button_box.rejected.connect(self.reject)
        layout.addWidget(button_box)
    
    def _format_timestamp(self, timestamp: int) -> str:
        """
        Format a Unix timestamp to a human-readable date.
        
        Args:
            timestamp: Unix timestamp in milliseconds.
            
        Returns:
            str: Formatted date string.
        """
        if not timestamp:
            return "Never"
        
        try:
            # Convert from milliseconds to seconds
            timestamp_seconds = timestamp / 1000
            # Format the date
            return time.strftime("%Y-%m-%d %H:%M:%S", time.localtime(timestamp_seconds))
        except:
            return "Unknown"

class MainWindow(QMainWindow):
    """
    Main window for the Hypixel Stats Companion App.
    """
    
    def __init__(self) -> None:
        """
        Initialize the main window.
        """
        super().__init__()
        
        # Load the API key
        try:
            api_key = config.get_api_key()
            self.api_client = ApiClient(api_key)
        except Exception as e:
            api_key = None
            self.api_client = None
            print(f"Error loading API key: {str(e)}")
        
        # Setup UI
        self.setWindowTitle("Hypixel Stats Companion")
        self.setMinimumSize(1000, 600)
        
        # Check if API key is set, if not, show dialog
        if not api_key:
            self._prompt_for_api_key()
            
        # Check if log file path is set, if not, show dialog
        try:
            log_file_path = config.get_log_file_path()
            if not log_file_path or not os.path.exists(log_file_path):
                self._prompt_for_log_file()
        except:
            self._prompt_for_log_file()
        
        # Create central widget and layout
        central_widget = QWidget()
        self.setCentralWidget(central_widget)
        main_layout = QVBoxLayout(central_widget)
        
        # Create tabs
        self.tabs = QTabWidget()
        main_layout.addWidget(self.tabs)
        
        # Create main tab
        self.main_tab = QWidget()
        self.tabs.addTab(self.main_tab, "Player Stats")
        
        # Set up main tab layout
        main_tab_layout = QVBoxLayout(self.main_tab)
        
        # Top controls for main tab
        top_controls = QHBoxLayout()
        
        # Player lookup
        search_layout = QHBoxLayout()
        search_layout.addWidget(QLabel("Lookup Player:"))
        
        self.player_lookup = QLineEdit()
        self.player_lookup.setPlaceholderText("Enter a player name")
        self.player_lookup.returnPressed.connect(self.lookup_player)
        search_layout.addWidget(self.player_lookup)
        
        self.lookup_button = QPushButton("Lookup")
        self.lookup_button.clicked.connect(self.lookup_player)
        search_layout.addWidget(self.lookup_button)
        
        top_controls.addLayout(search_layout)
        
        # Add settings button
        self.settings_button = QPushButton("Settings")
        self.settings_button.clicked.connect(self.open_settings)
        top_controls.addWidget(self.settings_button)
        
        main_tab_layout.addLayout(top_controls)
        
        # Lobby status
        lobby_layout = QHBoxLayout()
        lobby_layout.addWidget(QLabel("Lobby Players:"))
        
        self.lobby_status = QLabel("0 players")
        lobby_layout.addWidget(self.lobby_status)
        
        lobby_layout.addStretch()
        
        # Add refresh button to manually check log file
        self.refresh_button = QPushButton("Refresh")
        self.refresh_button.setToolTip("Manually check the log file for player updates")
        self.refresh_button.clicked.connect(self.refresh_log)
        lobby_layout.addWidget(self.refresh_button)
        
        self.monitor_toggle_button = QPushButton("Start Monitoring")
        self.monitor_toggle_button.clicked.connect(self.toggle_monitoring)
        lobby_layout.addWidget(self.monitor_toggle_button)
        
        main_tab_layout.addLayout(lobby_layout)
        
        # Main table
        self.table = QTableWidget()
        self.table.setColumnCount(9)  # Added Team column
        self.table.setHorizontalHeaderLabels([
            "Rank", "Username", "Team", "Stars", "FKDR", "WLR", 
            "Level", "Nick Est.", "AP"
        ])
        
        # Set table properties
        header = self.table.horizontalHeader()
        
        # Set minimum widths for columns to ensure header text remains visible with sort indicator
        self.table.setColumnWidth(0, 50)  # Rank
        self.table.setColumnWidth(1, 120)  # Username
        self.table.setColumnWidth(2, 80)   # Team
        self.table.setColumnWidth(3, 60)   # Stars
        self.table.setColumnWidth(4, 60)   # FKDR
        self.table.setColumnWidth(5, 60)   # WLR
        self.table.setColumnWidth(6, 60)   # Level
        self.table.setColumnWidth(7, 100)  # Nick Est.
        self.table.setColumnWidth(8, 60)   # AP
        
        # Username column stretches
        header.setSectionResizeMode(1, QHeaderView.ResizeMode.Stretch)
        # Other columns resize to content but with constraints
        for i in [0, 2, 3, 4, 5, 6, 7, 8]:
            header.setSectionResizeMode(i, QHeaderView.ResizeMode.Interactive)
            
        # Set header text alignment to center
        for i in range(9):
            self.table.horizontalHeaderItem(i).setTextAlignment(Qt.AlignmentFlag.AlignCenter)
            
        self.table.setEditTriggers(QTableWidget.EditTrigger.NoEditTriggers)
        self.table.setSelectionBehavior(QTableWidget.SelectionBehavior.SelectRows)
        self.table.setAlternatingRowColors(True)
        
        # Enable sorting
        self.table.setSortingEnabled(True)
        
        # Connect header click to sorting function
        self.table.horizontalHeader().sectionClicked.connect(self._sort_table)
        
        # Double click on player row to show details
        self.table.itemDoubleClicked.connect(self._show_player_details_from_table)
        
        main_tab_layout.addWidget(self.table)
        
        # Add progress bar for stats fetching
        self.fetch_progress = QProgressBar()
        self.fetch_progress.setMaximum(100)
        self.fetch_progress.setTextVisible(True)
        self.fetch_progress.setAlignment(Qt.AlignmentFlag.AlignCenter)
        self.fetch_progress.hide()  # Hide by default
        main_tab_layout.addWidget(self.fetch_progress)
        
        # Status bar
        self.status_bar = QStatusBar()
        self.setStatusBar(self.status_bar)
        self.status_bar.showMessage("Ready")
        
        # Start the log monitor
        self.log_monitor = LogMonitor(self.handle_lobby_update, self.update_player_team)
        
        # Store current player stats for team color updates
        self.current_player_stats = []
        
        # Start the log monitor
        self.start_monitoring()
        
        # Set up timers
        self._setup_timers()
    
    def _setup_timers(self) -> None:
        """
        Set up timers for periodic tasks.
        """
        # Create a timer for log file checking
        self.log_check_timer = QTimer()
        self.log_check_timer.setParent(self)  # Explicitly set parent
        log_poll_interval = config.get_polling_interval() * 1000  # Convert seconds to milliseconds
        self.log_check_timer.setInterval(log_poll_interval)
        self.log_check_timer.timeout.connect(self._check_log_file)
        self.log_check_timer.start()
        
        # Create a timer for periodic refresh
        self.refresh_timer = QTimer()
        self.refresh_timer.setParent(self)  # Explicitly set parent
        self.refresh_timer.setInterval(30000)  # 30 seconds in milliseconds
        self.refresh_timer.timeout.connect(self.refresh_log)
        self.refresh_timer.start()
        
        print(f"Set up timers - log check every {log_poll_interval/1000} seconds, refresh every 30 seconds")
    
    def _check_log_file(self) -> None:
        """
        Periodically check the log file for changes.
        Called by the log_check_timer.
        """
        if hasattr(self, 'log_monitor') and self.log_monitor and self.log_monitor.running:
            self.log_monitor.check_log_file()
    
    def open_settings(self) -> None:
        """
        Open the settings dialog.
        """
        dialog = SettingsDialog(self)
        result = dialog.exec()
        
        if result == QDialog.DialogCode.Accepted:
            # Reload API client if API key changed
            try:
                new_api_key = config.get_api_key()
                if new_api_key != self.api_client.api_key:
                    self.api_client = ApiClient(new_api_key)
            except:
                pass
            
            # Restart log monitor if log file path or polling interval changed
            try:
                restart_needed = False
                
                # Check if log file path changed
                new_log_path = config.get_log_file_path()
                if self.log_monitor and hasattr(self.log_monitor, 'log_file_path') and new_log_path != self.log_monitor.log_file_path:
                    restart_needed = True
                
                # Check if polling interval changed
                new_poll_interval = config.get_polling_interval()
                if self.log_monitor and hasattr(self.log_monitor, 'poll_interval') and new_poll_interval != self.log_monitor.poll_interval:
                    restart_needed = True
                
                # Restart if needed
                if restart_needed and self.log_monitor.running:
                    self.status_bar.showMessage("Restarting log monitor with new settings...")
                    self.stop_monitoring()
                    self.log_monitor = LogMonitor(self.handle_lobby_update, self.update_player_team)
                    self.start_monitoring()
            except:
                pass
    
    def _show_player_details_from_table(self, item) -> None:
        """
        Show detailed player stats when a row in the table is double-clicked.
        
        Args:
            item: The table item that was clicked.
        """
        row = item.row()
        username_item = self.table.item(row, 1)
        if username_item:
            username = username_item.text()
            self.lookup_player_details(username)
    
    def lookup_player_details(self, username: str) -> None:
        """
        Look up a player's detailed stats and show them in a dialog.
        
        Args:
            username: The player's Minecraft username.
        """
        self.status_bar.showMessage(f"Looking up details for player: {username}")
        
        try:
            # Get UUID for the username
            uuid = self.api_client.get_uuid(username)
            
            # Get player stats
            player_data = self.api_client.get_player_stats(uuid)
            
            # Process the stats
            processed_stats = stats_processor.extract_relevant_stats(player_data)
            
            # Calculate nick probability
            nick_score = nick_detector.estimate_if_nicked(processed_stats)
            processed_stats['nick_score'] = nick_score
            processed_stats['nick_description'] = nick_detector.get_nick_probability_description(nick_score)
            
            # Show the details dialog
            dialog = PlayerDetailsDialog(processed_stats, self)
            dialog.exec()
            
            self.status_bar.showMessage("Ready")
            
        except Exception as e:
            self.show_error(f"Error looking up player: {str(e)}")
    
    def lookup_player(self) -> None:
        """
        Look up a player by username.
        """
        username = self.player_lookup.text().strip()
        
        if not username:
            self.show_error("Please enter a username")
            return
        
        # When looking up a single player, show detailed stats instead of adding to table
        self.lookup_player_details(username)
    
    def start_monitoring(self) -> None:
        """
        Start monitoring the Minecraft log file.
        """
        try:
            self.log_monitor.start()
            self.monitor_toggle_button.setText("Stop Monitoring")
            self.status_bar.showMessage("Monitoring Minecraft log file")
        except Exception as e:
            self.show_error(f"Error starting log monitor: {str(e)}")
    
    def stop_monitoring(self) -> None:
        """
        Stop monitoring the Minecraft log file.
        """
        try:
            self.log_monitor.stop()
            self.monitor_toggle_button.setText("Start Monitoring")
            self.status_bar.showMessage("Monitoring stopped")
        except Exception as e:
            self.show_error(f"Error stopping log monitor: {str(e)}")
    
    def toggle_monitoring(self) -> None:
        """
        Toggle the log file monitoring on/off.
        """
        if self.log_monitor.running:
            self.stop_monitoring()
        else:
            self.start_monitoring()
    
    def _check_api_key(self) -> bool:
        """
        Check if the API key is set in the config file.
        If not, prompt the user to enter it.
        
        Returns:
            bool: True if API key is set or was successfully entered, False if the user canceled.
        """
        try:
            # Try to get the API key, this will raise an exception if not set
            api_key = config.get_api_key()
            
            # Verify the API key works by making a test request
            temp_client = ApiClient(api_key)
            if not temp_client.verify_api_key():
                raise ValueError("The API key is invalid or has expired")
                
            return True
            
        except (ValueError, KeyError, FileNotFoundError) as e:
            # Determine the error message based on the exception
            if isinstance(e, ValueError) and "Please set your Hypixel API key" in str(e):
                message = "The config file exists but contains a placeholder API key."
            elif isinstance(e, ValueError) and "invalid or has expired" in str(e):
                message = "Your API key is invalid or has expired. Please get a new one."
            elif isinstance(e, KeyError):
                message = "The API key setting is missing from the config file."
            elif isinstance(e, FileNotFoundError):
                message = "The config file was not found."
            else:
                message = str(e)
                
            # Show a more informative message before the dialog
            QMessageBox.information(
                self,
                "API Key Required",
                f"You need to enter your Hypixel API key to use this application.\n\n"
                f"Reason: {message}\n\n"
                f"You can get your API key by:\n"
                f"1. Connecting to the Hypixel server (mc.hypixel.net) and running: /api new\n"
                f"2. Visiting the Hypixel Developer Dashboard: https://developer.hypixel.net/"
            )
            
            # API key not set, show dialog
            dialog = ApiKeyDialog(self)
            if dialog.exec():
                # User clicked OK
                api_key = dialog.get_api_key()
                if api_key:
                    # Validate the new API key with a network request
                    try:
                        temp_client = ApiClient(api_key)
                        if not temp_client.verify_api_key():
                            QMessageBox.critical(
                                self, 
                                "Invalid API Key", 
                                "The API key validation failed. This could be because:\n\n"
                                "1. The key is invalid or has expired\n"
                                "2. The Hypixel API is experiencing issues\n"
                                "3. Your internet connection is having problems\n\n"
                                "You can try again or continue with this key (some features may not work)."
                            )
                            
                            # Ask if the user wants to try again or use this key anyway
                            result = QMessageBox.question(
                                self,
                                "Continue?",
                                "Would you like to use this key anyway and try to start the application?",
                                QMessageBox.StandardButton.Yes | QMessageBox.StandardButton.No,
                                QMessageBox.StandardButton.No
                            )
                            
                            if result == QMessageBox.StandardButton.Yes:
                                # Save the key and continue, even though validation failed
                                config.save_config("Hypixel", "API_KEY", api_key)
                                self.api_client = ApiClient(api_key)
                            else:
                                return self._check_api_key()  # Try again
                        
                        # Key is valid, save it
                        config.save_config("Hypixel", "API_KEY", api_key)
                        return True
                        
                    except requests.RequestException as e:
                        # Connection error during validation
                        QMessageBox.warning(
                            self,
                            "Connection Error",
                            f"Could not verify the API key due to a connection error:\n{str(e)}\n\n"
                            f"The key will be saved, but may not work if entered incorrectly."
                        )
                        # Save anyway and continue
                        config.save_config("Hypixel", "API_KEY", api_key)
                        self.api_client = ApiClient(api_key)
                        return True
                        
                else:
                    QMessageBox.critical(self, "Error", "API key cannot be empty.")
                    return self._check_api_key()  # Recursively prompt until valid
            else:
                # User clicked Cancel
                return False
    
    def _check_log_file(self, force_prompt: bool = False) -> bool:
        """
        Check if the Minecraft log file path is set in the config file.
        If not, or if force_prompt is True, prompt the user to select it.
        
        Args:
            force_prompt: Whether to force showing the log file dialog, even if a path is set.
            
        Returns:
            bool: True if log file path is set or was successfully selected, False if the user canceled.
        """
        try:
            if not force_prompt:
                # Try to get the log file path, this will auto-detect if not set
                config.get_log_file_path()
                return True
                
            # If we get here, either force_prompt is True or we need to prompt for the log file
            dialog = LogFileDialog(self)
            if dialog.exec():
                # User clicked OK
                log_file_path = dialog.get_log_file_path()
                if log_file_path:
                    # Save the log file path to config
                    config.save_config("Minecraft", "LOG_FILE_PATH", log_file_path)
                    return True
                else:
                    QMessageBox.critical(self, "Error", "Log file path cannot be empty.")
                    return self._check_log_file(force_prompt=True)  # Try again
            else:
                # User clicked Cancel
                return False
                
        except ValueError as e:
            # Log file path could not be auto-detected or is invalid
            QMessageBox.critical(
                self,
                "Log File Selection Required",
                f"We need to know where your Minecraft log file is located:\n\n{str(e)}"
            )
            
            # Show log file selection dialog
            dialog = LogFileDialog(self)
            if dialog.exec():
                log_file_path = dialog.get_log_file_path()
                if log_file_path:
                    config.save_config("Minecraft", "LOG_FILE_PATH", log_file_path)
                    return True
                else:
                    QMessageBox.critical(self, "Error", "Log file path cannot be empty.")
                    return self._check_log_file(force_prompt=True)  # Try again
            else:
                return False
    
    def handle_lobby_update(self, usernames):
        """
        Update the lobby status including player stats.
        Called when the log monitor detects a /who command output.
        
        Args:
            usernames: List of player usernames
        """
        if not usernames:
            return

        self.update_status("Updating lobby stats...")
        self.last_update_time = time.time()
        
        # Store the complete list of usernames for reference
        self.all_lobby_usernames = usernames.copy()
        
        # Show loading animation
        self.fetch_progress.setValue(0)
        self.fetch_progress.setMaximum(len(usernames))
        self.fetch_progress.show()
        
        # Create a stats processor and process the usernames
        processor = StatsProcessor(usernames, self.api_client)
        
        try:
            # Process the stats synchronously, with a progress callback
            player_stats = processor.process(progress_callback=self.update_progress)
            
            # Process the results
            self.process_player_stats(player_stats)
            
        except Exception as e:
            print(f"Error processing player stats: {str(e)}")
            self.handle_error(f"Error fetching player stats: {str(e)}")
            
            # Hide the progress bar
            self.fetch_progress.hide()
    
    def process_player_stats(self, player_stats):
        """
        Process the final player stats received from the worker.
        
        Args:
            player_stats: List of player stats dictionaries
        """
        try:
            # Find players that are in the lobby but didn't get stats (likely nicks)
            found_usernames = {p.get('username', '').lower() for p in player_stats}
            missing_players = []
            
            for username in self.all_lobby_usernames:
                if username.lower() not in found_usernames:
                    # Add placeholder stats for missing players (likely nicks)
                    missing_players.append({
                        'username': username,
                        'bedwars_stars': '?',
                        'fkdr': '?',
                        'wlr': '?',
                        'level': '?',
                        'achievement_points': '?',
                        'nick_probability': 0.9,  # 90% likely to be a nick
                        'nick_estimate': 'Highly Likely Nick',
                        'rank': len(player_stats) + len(missing_players) + 1  # Append at the end
                    })
            
            # Add the missing players to the stats list
            all_players = player_stats + missing_players
            
            # Update the current player stats
            self.current_player_stats = all_players
            
            # Update the table with the player stats
            self._populate_table()
            
            # Update status bar with player count
            num_suspected_nicks = sum(1 for p in self.current_player_stats if p.get('nick_probability', 0) > 0.5)
            self.update_status(f"Found {len(self.current_player_stats)} players ({num_suspected_nicks} suspected nicks)")
            
            # Hide progress bar
            self.fetch_progress.hide()
            
        except Exception as e:
            print(f"Error in process_player_stats: {str(e)}")
            self.handle_error(f"Error processing player stats: {str(e)}")
    
    def _populate_table(self) -> None:
        """Populate the table with player stats."""
        try:
            # Disable sorting while updating
            self.table.setSortingEnabled(False)
            self.table.clearContents()
            self.table.setRowCount(len(self.current_player_stats))
            
            for row, player in enumerate(self.current_player_stats):
                # Rank column
                rank_item = QTableWidgetItem()
                rank = player.get('rank', row + 1)  # Default to row+1 if rank not present
                try:
                    rank_value = int(rank)
                    # Force numeric sorting by using the UserRole
                    rank_item.setData(Qt.ItemDataRole.DisplayRole, rank_value)
                    rank_item.setData(Qt.ItemDataRole.UserRole, rank_value)
                except (ValueError, TypeError):
                    rank_value = row + 1
                rank_item.setData(Qt.ItemDataRole.DisplayRole, rank_value)
                rank_item.setData(Qt.ItemDataRole.UserRole, rank_value)
                rank_item.setTextAlignment(Qt.AlignmentFlag.AlignCenter)
                self.table.setItem(row, 0, rank_item)
                
                # Username column
                username_item = QTableWidgetItem(player.get('username', ''))
                username_item.setTextAlignment(Qt.AlignmentFlag.AlignCenter)
                self.table.setItem(row, 1, username_item)
                
                # Team column
                team_item = QTableWidgetItem()
                team_color = "Unknown"
                if hasattr(self, 'log_monitor') and player.get('username') in self.log_monitor.player_teams:
                    team_color = self.log_monitor.player_teams[player.get('username')]
                    
                    # Set background color based on team color
                    if team_color == "RED":
                        team_item.setBackground(QColor(255, 100, 100))
                        team_item.setForeground(QColor(0, 0, 0))
                        team_item.setText("RED")
                    elif team_color == "BLUE":
                        team_item.setBackground(QColor(100, 100, 255))
                        team_item.setForeground(QColor(255, 255, 255))
                        team_item.setText("BLUE")
                    elif team_color == "GREEN":
                        team_item.setBackground(QColor(100, 255, 100))
                        team_item.setForeground(QColor(0, 0, 0))
                        team_item.setText("GREEN")
                    elif team_color == "YELLOW":
                        team_item.setBackground(QColor(255, 255, 100))
                        team_item.setForeground(QColor(0, 0, 0))
                        team_item.setText("YELLOW")
                    elif team_color == "AQUA":
                        team_item.setBackground(QColor(100, 255, 255))
                        team_item.setForeground(QColor(0, 0, 0))
                        team_item.setText("AQUA")
                    elif team_color == "WHITE":
                        team_item.setBackground(QColor(255, 255, 255))
                        team_item.setForeground(QColor(0, 0, 0))
                        team_item.setText("WHITE")
                    elif team_color == "PINK":
                        team_item.setBackground(QColor(255, 100, 255))
                        team_item.setForeground(QColor(0, 0, 0))
                        team_item.setText("PINK")
                    elif team_color == "GRAY":
                        team_item.setBackground(QColor(150, 150, 150))
                        team_item.setForeground(QColor(255, 255, 255))
                        team_item.setText("GRAY")
                    elif team_color == "YOUR_TEAM":
                        team_item.setBackground(QColor(100, 255, 100))
                        team_item.setForeground(QColor(0, 0, 0))
                        team_item.setText("YOUR TEAM")
                    else:
                        team_item.setText(team_color)
                else:
                    team_item.setText("")
                
                team_item.setTextAlignment(Qt.AlignmentFlag.AlignCenter)
                self.table.setItem(row, 2, team_item)
                
                # Stars column
                stars_item = QTableWidgetItem()
                stars = player.get('bedwars_stars', '?')
                if stars != '?':
                    try:
                        stars_value = int(stars)
                        stars_item.setData(Qt.ItemDataRole.DisplayRole, str(stars_value))
                        stars_item.setData(Qt.ItemDataRole.UserRole, stars_value)  # Store as integer for sorting
                    except (ValueError, TypeError):
                        stars_item.setText(str(stars))
                        stars_item.setData(Qt.ItemDataRole.UserRole, -999)  # Default for sorting
                else:
                    stars_item.setText('?')
                    stars_item.setData(Qt.ItemDataRole.UserRole, -1000)  # Place ? at the bottom when sorting
                stars_item.setTextAlignment(Qt.AlignmentFlag.AlignCenter)
                self.table.setItem(row, 3, stars_item)
                
                # FKDR column
                fkdr_item = QTableWidgetItem()
                fkdr = player.get('fkdr', '?')
                if fkdr != '?':
                    try:
                        fkdr_value = float(fkdr)
                        fkdr_item.setData(Qt.ItemDataRole.DisplayRole, f"{fkdr_value:.2f}")
                        fkdr_item.setData(Qt.ItemDataRole.UserRole, fkdr_value)  # Store as float for sorting
                    except (ValueError, TypeError):
                        fkdr_item.setText(str(fkdr))
                        fkdr_item.setData(Qt.ItemDataRole.UserRole, -999.0)  # Default for sorting
                else:
                    fkdr_item.setText('?')
                    fkdr_item.setData(Qt.ItemDataRole.UserRole, -1000.0)  # Place ? at the bottom when sorting
                fkdr_item.setTextAlignment(Qt.AlignmentFlag.AlignCenter)
                self.table.setItem(row, 4, fkdr_item)
                
                # WLR column
                wlr_item = QTableWidgetItem()
                wlr = player.get('wlr', '?')
                if wlr != '?':
                    try:
                        wlr_value = float(wlr)
                        wlr_item.setData(Qt.ItemDataRole.DisplayRole, f"{wlr_value:.2f}")
                        wlr_item.setData(Qt.ItemDataRole.UserRole, wlr_value)  # Store as float for sorting
                    except (ValueError, TypeError):
                        wlr_item.setText(str(wlr))
                        wlr_item.setData(Qt.ItemDataRole.UserRole, -999.0)  # Default for sorting
                else:
                    wlr_item.setText('?')
                    wlr_item.setData(Qt.ItemDataRole.UserRole, -1000.0)  # Place ? at the bottom when sorting
                wlr_item.setTextAlignment(Qt.AlignmentFlag.AlignCenter)
                self.table.setItem(row, 5, wlr_item)
                
                # Nick Est. column
                nick_est_item = QTableWidgetItem(player.get('nick_estimate', ''))
                # Highlight suspected nicks
                nick_probability = player.get('nick_probability', 0.0)
                is_confirmed_nick = player.get('is_confirmed_nick', False)
                
                if is_confirmed_nick or nick_probability >= 1.0:  # Confirmed nicks
                    nick_est_item.setBackground(QColor(200, 0, 0))  # Darker red for confirmed nicks
                    nick_est_item.setForeground(QColor(255, 255, 255))  # White text for readability
                elif nick_probability > 0.7:  # Highly likely nicks
                    nick_est_item.setBackground(QColor(255, 100, 100))  # Red background
                elif nick_probability > 0.4:  # Possible nicks
                    nick_est_item.setBackground(QColor(255, 200, 100))  # Orange background
                nick_est_item.setTextAlignment(Qt.AlignmentFlag.AlignCenter)
                # Store the nick probability for sorting
                nick_est_item.setData(Qt.ItemDataRole.UserRole, nick_probability)
                self.table.setItem(row, 6, nick_est_item)
                
                # AP column
                ap_item = QTableWidgetItem()
                ap = player.get('achievement_points', '?')
                if ap != '?':
                    try:
                        ap_value = int(ap)
                        ap_item.setData(Qt.ItemDataRole.DisplayRole, str(ap_value))
                        ap_item.setData(Qt.ItemDataRole.UserRole, ap_value)  # Store as integer for sorting
                    except (ValueError, TypeError):
                        ap_item.setText(str(ap))
                        ap_item.setData(Qt.ItemDataRole.UserRole, -999)  # Default for sorting
                else:
                    ap_item.setText('?')
                    ap_item.setData(Qt.ItemDataRole.UserRole, -1000)  # Place ? at the bottom when sorting
                ap_item.setTextAlignment(Qt.AlignmentFlag.AlignCenter)
                self.table.setItem(row, 7, ap_item)
                
            # Apply initial sort to rank column
            header = self.table.horizontalHeader()
            current_sort_column = header.sortIndicatorSection()
            current_sort_order = header.sortIndicatorOrder()
            
            # Re-enable sorting
            self.table.setSortingEnabled(True)
            
            # If we have a sort column, apply it
            if current_sort_column >= 0:
                self.table.sortByColumn(current_sort_column, current_sort_order)
            else:
                # Default sort by rank
                self.table.sortByColumn(0, Qt.SortOrder.AscendingOrder)
            
            # Update lobby status
            self.lobby_status.setText(f"{len(self.current_player_stats)} players")
            
        except Exception as e:
            print(f"Error in _populate_table: {str(e)}")
            self.handle_error(f"Error populating table: {str(e)}")
    
    def update_player_team(self, player_name: str, team_color: str) -> None:
        """
        Update team color for a player and refresh the table if needed.
        
        Args:
            player_name: The player's username.
            team_color: The team color.
        """
        if not self.current_player_stats:
            return
            
        # Check if player is in the current table
        player_found = False
        for player in self.current_player_stats:
            if player.get('username') == player_name:
                player_found = True
                break
                
        if player_found:
            # Remember current sort state
            header = self.table.horizontalHeader()
            sort_column = header.sortIndicatorSection()
            sort_order = header.sortIndicatorOrder()
            
            # Find the row for this player
            for row in range(self.table.rowCount()):
                username_item = self.table.item(row, 1)
                if username_item and username_item.text() == player_name:
                    # Update the team cell
                    team_item = QTableWidgetItem()
                    team_item.setTextAlignment(Qt.AlignmentFlag.AlignCenter)
                    
                    # Set team color background
                    if team_color == "RED":
                        team_item.setBackground(QColor(255, 100, 100))
                        team_item.setForeground(QColor(0, 0, 0))
                        team_item.setText("RED")
                    elif team_color == "BLUE":
                        team_item.setBackground(QColor(100, 100, 255))
                        team_item.setForeground(QColor(255, 255, 255))
                        team_item.setText("BLUE")
                    elif team_color == "GREEN":
                        team_item.setBackground(QColor(100, 255, 100))
                        team_item.setForeground(QColor(0, 0, 0))
                        team_item.setText("GREEN")
                    elif team_color == "YELLOW":
                        team_item.setBackground(QColor(255, 255, 100))
                        team_item.setForeground(QColor(0, 0, 0))
                        team_item.setText("YELLOW")
                    elif team_color == "AQUA":
                        team_item.setBackground(QColor(100, 255, 255))
                        team_item.setForeground(QColor(0, 0, 0))
                        team_item.setText("AQUA")
                    elif team_color == "WHITE":
                        team_item.setBackground(QColor(255, 255, 255))
                        team_item.setForeground(QColor(0, 0, 0))
                        team_item.setText("WHITE")
                    elif team_color == "PINK":
                        team_item.setBackground(QColor(255, 100, 255))
                        team_item.setForeground(QColor(0, 0, 0))
                        team_item.setText("PINK")
                    elif team_color == "GRAY":
                        team_item.setBackground(QColor(150, 150, 150))
                        team_item.setForeground(QColor(255, 255, 255))
                        team_item.setText("GRAY")
                    elif team_color == "YOUR_TEAM":
                        team_item.setBackground(QColor(100, 255, 100))
                        team_item.setForeground(QColor(0, 0, 0))
                        team_item.setText("YOUR TEAM")
                    else:
                        team_item.setText(team_color)
                    
                    # Disable sorting temporarily
                    self.table.setSortingEnabled(False)
                    
                    # Update the item
                    self.table.setItem(row, 2, team_item)
                    
                    # Restore sorting
                    self.table.setSortingEnabled(True)
                    
                    # Restore sort column
                    if sort_column >= 0:
                        self.table.sortByColumn(sort_column, sort_order)
                    
                    break
    
    def show_error(self, message: str) -> None:
        """
        Show an error message.
        
        Args:
            message: The error message to display.
        """
        self.status_bar.showMessage(f"Error: {message}")
        QMessageBox.critical(self, "Error", message)
    
    def closeEvent(self, event) -> None:
        """
        Handle window close event to clean up resources.
        """
        # Stop the log monitor
        self.stop_monitoring()
        
        # Stop all timers
        if hasattr(self, 'log_check_timer'):
            self.log_check_timer.stop()
        if hasattr(self, 'refresh_timer'):
            self.refresh_timer.stop()
        
        # Accept the event
        event.accept()

    def refresh_log(self) -> None:
        """
        Manually force the log monitor to check the log file.
        """
        if self.log_monitor and self.log_monitor.running:
            self.status_bar.showMessage("Manually refreshing lobby data...")
            self.log_monitor.force_check()
        else:
            self.status_bar.showMessage("Log monitoring is not active. Start monitoring first.")
            QMessageBox.information(
                self,
                "Monitoring Inactive",
                "Log monitoring is not active. Please start monitoring first."
            )

    def _prompt_for_api_key(self) -> None:
        """
        Prompt the user for the API key.
        """
        dialog = ApiKeyDialog(self)
        if dialog.exec():
            api_key = dialog.get_api_key()
            if api_key:
                # Validate the new API key with a network request
                try:
                    temp_client = ApiClient(api_key)
                    if not temp_client.verify_api_key():
                        QMessageBox.critical(
                            self, 
                            "Invalid API Key", 
                            "The API key validation failed. This could be because:\n\n"
                            "1. The key is invalid or has expired\n"
                            "2. The Hypixel API is experiencing issues\n"
                            "3. Your internet connection is having problems\n\n"
                            "You can try again or continue with this key (some features may not work)."
                        )
                        
                        # Ask if the user wants to try again or use this key anyway
                        result = QMessageBox.question(
                            self,
                            "Continue?",
                            "Would you like to use this key anyway and try to start the application?",
                            QMessageBox.StandardButton.Yes | QMessageBox.StandardButton.No,
                            QMessageBox.StandardButton.No
                        )
                        
                        if result == QMessageBox.StandardButton.Yes:
                            # Save the key and continue, even though validation failed
                            config.save_config("Hypixel", "API_KEY", api_key)
                            self.api_client = ApiClient(api_key)
                        else:
                            self._prompt_for_api_key()  # Try again
                except requests.RequestException as e:
                    # Connection error during validation
                    QMessageBox.warning(
                        self,
                        "Connection Error",
                        f"Could not verify the API key due to a connection error:\n{str(e)}\n\n"
                        f"The key will be saved, but may not work if entered incorrectly."
                    )
                    # Save anyway and continue
                    config.save_config("Hypixel", "API_KEY", api_key)
                    self.api_client = ApiClient(api_key)
            else:
                QMessageBox.critical(self, "Error", "API key cannot be empty.")
                self._prompt_for_api_key()  # Recursively prompt until valid
    
    def _prompt_for_log_file(self) -> None:
        """
        Prompt the user for the log file path.
        """
        dialog = LogFileDialog(self)
        if dialog.exec():
            log_file_path = dialog.get_log_file_path()
            if log_file_path:
                config.save_config("Minecraft", "LOG_FILE_PATH", log_file_path)
            else:
                QMessageBox.critical(self, "Error", "Log file path cannot be empty.")
                self._prompt_for_log_file()  # Try again
        else:
            # User clicked Cancel
            sys.exit(0)

    def _sort_table(self, column_index):
        """
        Sort the table by the clicked column.
        Handles numeric columns appropriately.
        
        Args:
            column_index: The index of the column to sort by
        """
        # Get the current sort indicator
        header = self.table.horizontalHeader()
        
        # If this column is already being sorted, toggle the order
        if header.sortIndicatorSection() == column_index:
            # Toggle order (if ascending, make descending and vice versa)
            if header.sortIndicatorOrder() == Qt.SortOrder.AscendingOrder:
                new_order = Qt.SortOrder.DescendingOrder
            else:
                new_order = Qt.SortOrder.AscendingOrder
        else:
            # Default orders based on column type
            if column_index == 0:  # Rank
                new_order = Qt.SortOrder.AscendingOrder
            elif column_index in [3, 4, 5, 7]:  # Stars, FKDR, WLR, AP
                new_order = Qt.SortOrder.DescendingOrder
            else:
                new_order = Qt.SortOrder.AscendingOrder
        
        # Apply sort indicator directly without using sortItems
        # This ensures Qt handles the sort event internally
        self.table.horizontalHeader().setSortIndicator(column_index, new_order)

    def update_status(self, message: str) -> None:
        """
        Update the status bar with a message.
        
        Args:
            message: The message to display in the status bar.
        """
        if hasattr(self, 'status_bar'):
            self.status_bar.showMessage(message)

    def update_progress(self, value: int) -> None:
        """
        Update the progress bar with a new value.
        
        Args:
            value: The progress value to display (0-100).
        """
        if hasattr(self, 'fetch_progress'):
            self.fetch_progress.setValue(value)

    def handle_error(self, message: str) -> None:
        """
        Handle an error message from the worker thread.
        
        Args:
            message: The error message to display.
        """
        self.show_error(message)
        
        # Hide the progress bar
        if hasattr(self, 'fetch_progress'):
            self.fetch_progress.hide() 