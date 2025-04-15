"""
Main window for the Hypixel Stats Companion App.
"""
import sys
import threading
import time
import os
from typing import List, Dict, Any, Optional

from PyQt6.QtCore import Qt, QObject, pyqtSignal, QThread
from PyQt6.QtWidgets import (
    QMainWindow, QWidget, QVBoxLayout, QHBoxLayout, 
    QLineEdit, QPushButton, QTableWidget, QTableWidgetItem,
    QLabel, QStatusBar, QHeaderView, QMessageBox, QDialog,
    QFormLayout, QDialogButtonBox, QFileDialog, QComboBox,
    QTabWidget, QGridLayout, QGroupBox, QTextBrowser, QSpinBox
)

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

class StatsWorker(QObject):
    """
    Worker thread for fetching player stats in the background.
    """
    finished = pyqtSignal(list)  # Signal emitted with processed player stats
    error = pyqtSignal(str)      # Signal emitted when an error occurs
    progress = pyqtSignal(int)   # Signal to update progress (0-100)
    
    def __init__(self, usernames: List[str], api_client: ApiClient) -> None:
        """
        Initialize the worker with usernames and API client.
        
        Args:
            usernames: List of usernames to fetch stats for.
            api_client: The API client to use.
        """
        super().__init__()
        self.usernames = usernames
        self.api_client = api_client
        self.is_running = True
    
    def process(self) -> None:
        """
        Process the usernames and fetch player stats.
        Emits the finished signal with the processed stats.
        """
        all_player_stats = []
        total_players = len(self.usernames)
        
        try:
            for i, username in enumerate(self.usernames):
                if not self.is_running:
                    break
                
                try:
                    # Update progress
                    progress_percent = int((i / total_players) * 100)
                    self.progress.emit(progress_percent)
                    
                    # Get UUID for the username
                    uuid = self.api_client.get_uuid(username)
                    
                    # Get player stats
                    player_data = self.api_client.get_player_stats(uuid)
                    
                    # Process the stats
                    processed_stats = stats_processor.extract_relevant_stats(player_data)
                    
                    # Add to the list
                    all_player_stats.append(processed_stats)
                
                except Exception as e:
                    # Skip this player but continue with others
                    print(f"Error processing player {username}: {str(e)}")
            
            # Rank the players
            if all_player_stats and self.is_running:
                ranked_stats = ranking_engine.rank_players(all_player_stats)
                
                # Calculate nick probabilities
                for player in ranked_stats:
                    nick_score = nick_detector.estimate_if_nicked(player)
                    player['nick_score'] = nick_score
                    player['nick_description'] = nick_detector.get_nick_probability_description(nick_score)
                
                # Emit the result
                self.finished.emit(ranked_stats)
            else:
                self.finished.emit([])
            
        except Exception as e:
            self.error.emit(f"Error fetching player stats: {str(e)}")
    
    def stop(self) -> None:
        """
        Stop the worker.
        """
        self.is_running = False

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
        
        # Check if API key is set, prompt if not
        if not self._check_api_key():
            # User canceled, exit the application
            sys.exit(0)
            
        # Check if log file path is set, prompt if not
        if not self._check_log_file():
            # User canceled, exit the application
            sys.exit(0)
        
        # Initialize API client and log monitor
        self.api_client = ApiClient()
        
        try:
            self.log_monitor = LogMonitor(self.handle_lobby_update)
        except ValueError as e:
            # If there's an error with the log file, show dialog to select a valid one
            QMessageBox.critical(self, "Log File Error", str(e))
            if not self._check_log_file(force_prompt=True):
                sys.exit(0)
            self.log_monitor = LogMonitor(self.handle_lobby_update)
            
        self.worker_thread = None
        
        # Set up UI
        self.setWindowTitle("Hypixel Stats Companion")
        self.setMinimumSize(800, 600)
        
        # Central widget
        central_widget = QWidget()
        self.setCentralWidget(central_widget)
        
        # Main layout
        main_layout = QVBoxLayout(central_widget)
        
        # Top controls layout
        top_controls = QHBoxLayout()
        
        # Search bar
        search_layout = QHBoxLayout()
        
        self.username_input = QLineEdit()
        self.username_input.setPlaceholderText("Enter Minecraft username")
        search_layout.addWidget(self.username_input)
        
        self.lookup_button = QPushButton("Lookup")
        self.lookup_button.clicked.connect(self.lookup_player)
        search_layout.addWidget(self.lookup_button)
        
        top_controls.addLayout(search_layout)
        
        # Add settings button
        self.settings_button = QPushButton("Settings")
        self.settings_button.clicked.connect(self.open_settings)
        top_controls.addWidget(self.settings_button)
        
        main_layout.addLayout(top_controls)
        
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
        
        main_layout.addLayout(lobby_layout)
        
        # Main table
        self.table = QTableWidget()
        self.table.setColumnCount(8)
        self.table.setHorizontalHeaderLabels([
            "Rank", "Username", "Stars", "FKDR", "WLR", 
            "Level", "Nick Est.", "AP"
        ])
        
        # Set table properties
        header = self.table.horizontalHeader()
        header.setSectionResizeMode(1, QHeaderView.ResizeMode.Stretch)  # Username column stretches
        for i in [0, 2, 3, 4, 5, 6, 7]:  # Fixed width for other columns
            header.setSectionResizeMode(i, QHeaderView.ResizeMode.ResizeToContents)
        
        self.table.setEditTriggers(QTableWidget.EditTrigger.NoEditTriggers)  # Read-only
        self.table.setSelectionBehavior(QTableWidget.SelectionBehavior.SelectRows)
        self.table.setAlternatingRowColors(True)
        
        # Double click on player row to show details
        self.table.itemDoubleClicked.connect(self._show_player_details_from_table)
        
        main_layout.addWidget(self.table)
        
        # Status bar
        self.status_bar = QStatusBar()
        self.setStatusBar(self.status_bar)
        self.status_bar.showMessage("Ready")
        
        # Start the log monitor
        self.start_monitoring()
    
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
                    self.log_monitor = LogMonitor(self.handle_lobby_update)
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
        username = self.username_input.text().strip()
        
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
                                return True
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
    
    def handle_lobby_update(self, usernames: List[str]) -> None:
        """
        Handle lobby player update from log monitor.
        
        Args:
            usernames: List of player usernames in the lobby.
        """
        # Update lobby status
        self.lobby_status.setText(f"{len(usernames)} players")
        
        # Clear the table
        self.table.setRowCount(0)
        
        # Update status
        self.status_bar.showMessage("Fetching lobby stats...")
        
        # Start the worker thread
        self.fetch_player_stats(usernames)
    
    def fetch_player_stats(self, usernames: List[str]) -> None:
        """
        Fetch player stats in a background thread.
        
        Args:
            usernames: List of player usernames to fetch stats for.
        """
        # If a thread is already running, stop it
        if self.worker_thread and self.worker_thread.isRunning():
            worker = self.worker_thread.findChild(StatsWorker)
            if worker:
                worker.stop()
            self.worker_thread.quit()
            self.worker_thread.wait()
        
        # Create a new thread and worker
        self.worker_thread = QThread()
        self.worker = StatsWorker(usernames, self.api_client)
        self.worker.moveToThread(self.worker_thread)
        
        # Connect signals
        self.worker_thread.started.connect(self.worker.process)
        self.worker.finished.connect(self._populate_table)
        self.worker.error.connect(self.show_error)
        self.worker.progress.connect(self._update_progress)
        self.worker.finished.connect(self.worker_thread.quit)
        
        # Start the thread
        self.worker_thread.start()
    
    def _update_progress(self, progress: int) -> None:
        """
        Update the status bar with progress.
        
        Args:
            progress: Progress percentage (0-100).
        """
        self.status_bar.showMessage(f"Fetching player stats: {progress}%")
    
    def _populate_table(self, player_stats: List[Dict[str, Any]]) -> None:
        """
        Populate the table with player stats.
        
        Args:
            player_stats: List of processed player stat dictionaries.
        """
        # Clear the table
        self.table.setRowCount(0)
        
        # If no players, show message and return
        if not player_stats:
            self.status_bar.showMessage("No players found or error fetching data")
            return
        
        # Add rows to the table
        self.table.setRowCount(len(player_stats))
        
        for row, player in enumerate(player_stats):
            # Rank
            rank_item = QTableWidgetItem(str(row + 1))
            self.table.setItem(row, 0, rank_item)
            
            # Username
            username_item = QTableWidgetItem(player.get('username', 'Unknown'))
            self.table.setItem(row, 1, username_item)
            
            # Stars - divide by 10 to show as decimal
            stars_value = player.get('bedwars_stars', 0)
            stars_item = QTableWidgetItem(f"{stars_value/10:.1f}")
            self.table.setItem(row, 2, stars_item)
            
            # FKDR
            fkdr_item = QTableWidgetItem(f"{player.get('fkdr', 0.0):.2f}")
            self.table.setItem(row, 3, fkdr_item)
            
            # WLR
            wlr_item = QTableWidgetItem(f"{player.get('wlr', 0.0):.2f}")
            self.table.setItem(row, 4, wlr_item)
            
            # Level
            level_item = QTableWidgetItem(f"{player.get('hypixel_level', 0.0):.1f}")
            self.table.setItem(row, 5, level_item)
            
            # Nick Est.
            nick_description = player.get('nick_description', 'Unknown')
            nick_item = QTableWidgetItem(nick_description)
            
            # Color code based on nick probability
            if nick_description == "Very Likely Nicked":
                nick_item.setBackground(Qt.GlobalColor.red)
            elif nick_description == "Probably Nicked":
                nick_item.setBackground(Qt.GlobalColor.yellow)
            
            self.table.setItem(row, 6, nick_item)
            
            # Achievement Points
            ap_item = QTableWidgetItem(str(player.get('achievement_points', 0)))
            self.table.setItem(row, 7, ap_item)
        
        # Update status
        self.status_bar.showMessage(f"Loaded stats for {len(player_stats)} players")
    
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
        
        # Stop any running worker thread
        if self.worker_thread and self.worker_thread.isRunning():
            worker = self.worker_thread.findChild(StatsWorker)
            if worker:
                worker.stop()
            self.worker_thread.quit()
            self.worker_thread.wait()
        
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