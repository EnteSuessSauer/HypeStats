# AI Documentation for Hypixel Stats Companion

This directory contains documentation to help AI assistants understand the codebase of the Hypixel Stats Companion application. Each file focuses on different aspects of the application design, implementation, and architecture.

## Key Files

- [Architecture Overview](architecture.md): High-level architecture, component interactions, and data flow
- [Developer Notes](developer_notes.md): Notes and considerations for developers, including tech stack and improvement areas
- [Data Models](data_models.md): Description of the key data structures used throughout the application
- [Testing Strategy](testing_strategy.md): Overview of the testing approach for the application

## Application Overview

The Hypixel Stats Companion is a desktop application that helps Minecraft players analyze their opponents in Hypixel Bedwars games. It:

1. Monitors the Minecraft log file for `/who` command output to detect players in the current lobby
2. Fetches player statistics from the Hypixel API
3. Ranks players based on their skill level (stars, FKDR, WLR)
4. Attempts to detect "nicked" players (players using alternate accounts to hide their true skill level)
5. Displays all this information in a sortable table

## Technical Highlights

- Written in **Python** with **PyQt6** for the user interface
- Uses a **single-threaded event-driven design** with timers for periodic tasks
- Leverages the **watchdog** library for file system monitoring
- Communicates with both the **Mojang API** (UUID lookups) and **Hypixel API** (player stats)
- Implements custom statistics processing, ranking algorithms, and nick detection heuristics

## Directory Structure

```
/ai
├── README.md               # This file
├── architecture.md         # Architecture overview
├── data_models.md          # Data structure documentation
├── developer_notes.md      # Notes for developers
├── testing_strategy.md     # Testing approach
├── apis/                   # API documentation
├── core_logic/             # Documentation for core logic components
└── prompt_addons/          # Additional prompts for AI assistance
```

## Key Concepts

- **Log Monitor**: Watches the Minecraft log file for player information
- **API Client**: Handles communication with external APIs
- **Stats Processor**: Transforms raw API data into usable statistics
- **Ranking Engine**: Sorts and ranks players based on skill metrics
- **Nick Detector**: Uses heuristics to identify players using nicknames

## Implementation Notes

- The application uses a **single-threaded design** with Qt's event loop and timers
- Long operations provide progress feedback to maintain UI responsiveness
- File system monitoring via **watchdog** runs in a separate thread but interacts with the main thread via callbacks
- The UI is designed to be sortable and filterable for easy analysis of player stats 