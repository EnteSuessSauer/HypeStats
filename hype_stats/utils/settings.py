"""
Settings management for the HypeStats application.
"""

import os
import pickle
import logging
import pygame

logger = logging.getLogger(__name__)

class Settings:
    """
    Handles loading, saving, and managing application settings.
    """
    def __init__(self):
        """Initialize with default settings."""
        self.toggle_key = pygame.K_F6  # Using pygame key constants
        self.settings_key = pygame.K_F7
        self.transparency = 128  # 0-255, where 0 is fully transparent
        self.settings_file = "hypixel_overlay_settings.pkl"
        
        self.load_settings()
    
    def load_settings(self):
        """Load settings from file if it exists."""
        try:
            if os.path.exists(self.settings_file):
                with open(self.settings_file, 'rb') as f:
                    data = pickle.load(f)
                    self.toggle_key = data.get('toggle_key', self.toggle_key)
                    self.settings_key = data.get('settings_key', self.settings_key)
                    self.transparency = data.get('transparency', self.transparency)
                logger.info(f"Loaded settings: toggle={self.toggle_key}, settings={self.settings_key}, transparency={self.transparency}")
            else:
                logger.info("No settings file found, using defaults")
        except Exception as e:
            logger.error(f"Error loading settings: {e}")
    
    def save_settings(self):
        """Save current settings to file."""
        try:
            data = {
                'toggle_key': self.toggle_key,
                'settings_key': self.settings_key,
                'transparency': self.transparency
            }
            with open(self.settings_file, 'wb') as f:
                pickle.dump(data, f)
            logger.info("Settings saved successfully")
        except Exception as e:
            logger.error(f"Error saving settings: {e}")
            
    def update_transparency(self, value):
        """Update transparency setting."""
        self.transparency = max(0, min(255, value))  # Ensure value is in valid range
        logger.debug(f"Updated transparency to {self.transparency}")
        return self.transparency 