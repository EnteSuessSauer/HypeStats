"""
Configuration utility for Hypixel Stats Companion.
Handles loading and accessing configuration from config.ini.
"""
import os
import sys
import configparser
from typing import Optional, List, Dict

# Define the config file path relative to the project root
CONFIG_FILE = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))), 'config.ini')

def load_config() -> configparser.ConfigParser:
    """
    Load the configuration from config.ini.
    
    Returns:
        ConfigParser: The loaded configuration.
        
    Raises:
        FileNotFoundError: If config.ini doesn't exist.
    """
    if not os.path.exists(CONFIG_FILE):
        # Create default config file if it doesn't exist
        create_default_config()
    
    config = configparser.ConfigParser()
    config.read(CONFIG_FILE)
    
    return config

def create_default_config() -> None:
    """
    Create a default configuration file with placeholder values.
    """
    config = configparser.ConfigParser()
    
    config['Hypixel'] = {
        'API_KEY': 'YOUR_HYPIXEL_API_KEY_HERE'
    }
    
    config['Minecraft'] = {
        'LOG_FILE_PATH': 'auto',
        'POLLING_INTERVAL': '2'  # Default to 2 seconds
    }
    
    with open(CONFIG_FILE, 'w') as config_file:
        config.write(config_file)

def get_api_key() -> str:
    """
    Get the Hypixel API key from configuration.
    
    Returns:
        str: The API key.
        
    Raises:
        KeyError: If the API key is not set in config.ini.
        FileNotFoundError: If config.ini doesn't exist.
    """
    config = load_config()
    
    if 'Hypixel' not in config or 'API_KEY' not in config['Hypixel']:
        raise KeyError("Hypixel API key not found in config.ini")
    
    api_key = config['Hypixel']['API_KEY']
    
    if not api_key or api_key == "YOUR_HYPIXEL_API_KEY_HERE":
        raise ValueError("Please set your Hypixel API key in config.ini")
    
    return api_key

def get_default_log_paths() -> Dict[str, str]:
    """
    Get a dictionary of default log paths for different Minecraft launchers.
    
    Returns:
        Dict[str, str]: Dictionary of launcher names to log paths.
    """
    # Get user's home directory
    home_dir = os.path.expanduser('~')
    appdata_roaming = os.path.join(os.environ.get('APPDATA', home_dir))
    
    # Define default paths for various launchers
    return {
        "Vanilla Minecraft": os.path.join(appdata_roaming, ".minecraft", "logs", "latest.log"),
        "Badlion Client": os.path.join(appdata_roaming, ".minecraft", "logs", "blclient", "minecraft", "latest.log"),
        "Lunar Client": os.path.join(home_dir, ".lunarclient", "offline", "multiver", "logs", "latest.log"),
        "Feather Client": os.path.join(appdata_roaming, ".feather", "client", "logs", "latest.log"),
        "Prism Launcher": os.path.join(appdata_roaming, "PrismLauncher", "instances", "Vanilla", ".minecraft", "logs", "latest.log")
    }

def find_existing_log_file() -> Optional[str]:
    """
    Try to find an existing Minecraft log file from the default paths.
    
    Returns:
        Optional[str]: Path to the first log file found, or None if no log files exist.
    """
    for path in get_default_log_paths().values():
        if os.path.exists(path):
            print(f"Found log file at: {path}")
            return path
    
    return None

def get_log_file_path() -> str:
    """
    Get the Minecraft log file path from configuration.
    If set to 'auto', try to auto-detect the log file.
    
    Returns:
        str: The log file path.
        
    Raises:
        ValueError: If the log file path cannot be determined.
    """
    config = load_config()
    
    if 'Minecraft' not in config or 'LOG_FILE_PATH' not in config['Minecraft']:
        # If config section doesn't exist, create it
        config['Minecraft'] = {'LOG_FILE_PATH': 'auto'}
        save_config_object(config)
    
    log_path = config['Minecraft']['LOG_FILE_PATH']
    
    # If path is "auto" or empty, try to detect it automatically
    if not log_path or log_path == "auto" or log_path == "PATH_TO_YOUR_MINECRAFT/logs/latest.log":
        # Try to find an existing log file
        auto_detected_path = find_existing_log_file()
        
        if auto_detected_path:
            # Save the detected path for future use
            save_config("Minecraft", "LOG_FILE_PATH", auto_detected_path)
            return auto_detected_path
        else:
            raise ValueError("Could not auto-detect Minecraft log file path. Please set it manually in config.ini")
    
    # Check if the configured path exists
    if not os.path.exists(log_path):
        raise ValueError(f"Configured log file path does not exist: {log_path}")
    
    return log_path

def save_config(section: str, key: str, value: str) -> None:
    """
    Save a configuration value to config.ini.
    
    Args:
        section: The configuration section.
        key: The configuration key.
        value: The value to save.
    """
    config = load_config()
    
    if section not in config:
        config[section] = {}
    
    config[section][key] = value
    save_config_object(config)

def save_config_object(config: configparser.ConfigParser) -> None:
    """
    Save the entire configuration object to config.ini.
    
    Args:
        config: The configuration object to save.
    """
    with open(CONFIG_FILE, 'w') as config_file:
        config.write(config_file)

def get_polling_interval() -> int:
    """
    Get the log file polling interval from configuration.
    
    Returns:
        int: The polling interval in seconds (default: 2).
    """
    config = load_config()
    
    if 'Minecraft' not in config or 'POLLING_INTERVAL' not in config['Minecraft']:
        # If not set, use default and save it
        interval = 2
        save_config("Minecraft", "POLLING_INTERVAL", str(interval))
        return interval
    
    try:
        interval = int(config['Minecraft']['POLLING_INTERVAL'])
        # Ensure reasonable bounds (1-10 seconds)
        interval = max(1, min(10, interval))
        return interval
    except ValueError:
        # If not a valid number, reset to default
        save_config("Minecraft", "POLLING_INTERVAL", "2")
        return 2

def set_polling_interval(seconds: int) -> None:
    """
    Set the log file polling interval.
    
    Args:
        seconds: The polling interval in seconds (will be clamped to 1-10).
    """
    # Ensure reasonable bounds (1-10 seconds)
    interval = max(1, min(10, seconds))
    save_config("Minecraft", "POLLING_INTERVAL", str(interval)) 