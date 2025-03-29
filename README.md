# HypeStats

A Python-based overlay that displays Hypixel Bedwars player statistics in real-time. HypeStats monitors your Minecraft logs to detect when you join a new lobby and automatically fetches player statistics from the Hypixel API.

## Features

- Real-time overlay that displays Bedwars stats of players in your current lobby
- Transparent, draggable window that can be toggled on/off with a hotkey
- Color-coded stats for quick assessment of player skill levels
- Configurable hotkeys and transparency settings
- Automatically detects when you join a new lobby

## Requirements

- Python 3.7+
- Minecraft with Hypixel access
- Hypixel API key (obtain from https://api.hypixel.net/)

## Installation

1. Clone this repository:
```
git clone https://github.com/yourusername/HypeStats.git
cd HypeStats
```

2. Install the required dependencies:
```
pip install -r requirements.txt
```

3. Create a `.env` file in the project root directory and add your Hypixel API key:
```
HYPIXEL_API_KEY=your_api_key_here
```

## Usage

1. Start the overlay:
```
python -m hype_stats.main
```

2. The program will attempt to automatically locate your Minecraft log file. If it cannot, you will be prompted to enter the path manually.

3. Use the following default hotkeys:
   - **F6**: Toggle overlay visibility
   - **F7**: Open settings panel
   - **ESC**: Close settings or exit application

## Configuration

The settings panel allows you to:
- Change hotkeys
- Adjust overlay transparency
- Customize display options

Settings are automatically saved to `hypixel_overlay_settings.pkl`.

## Notes

- This overlay is NOT affiliated with or endorsed by Hypixel
- HypeStats complies with Hypixel's API Terms of Service by only updating player stats when you join a new lobby (not continuously)
- Use responsibly and respect Hypixel's API rate limits

## License

MIT License - See LICENSE file for details.

## Acknowledgements

- Hypixel API for providing the data
- Pygame library for the overlay functionality
