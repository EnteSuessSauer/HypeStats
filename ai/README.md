# Hypixel Stats Companion App

This directory contains context documentation for AI assistance with the Hypixel Stats Companion App.

## Project Overview

The Hypixel Stats Companion App is a desktop application that provides real-time statistics and analysis for Hypixel players. It monitors Minecraft log files to detect players in the current game lobby and displays their statistics, allowing users to gauge the skill level of opponents and teammates.

## Core Modules

- **ApiClient**: Handles communication with Hypixel and Mojang APIs, including authentication, rate limiting, and error handling.
- **LogMonitor**: Monitors the Minecraft log file for the Hypixel `/who` command output to detect players in the current lobby.
- **StatsProcessor**: Processes raw player stats from the Hypixel API into usable data, including calculating key metrics like FKDR and WLR.
- **RankingEngine**: Sorts players based on their statistics, allowing for identification of the most skilled players.
- **NickDetector**: Implements basic heuristics to estimate if a player is using a nickname ("nicked").
- **MainWindow**: The PyQt6-based user interface that displays player statistics and handles user interactions.

## Directory Structure

```
hypixel-stats-companion/
├── src/                    # Main source code
│   ├── ui/                 # User interface components
│   ├── utils/              # Utility functions and helpers
│   ├── api_client.py       # API client for Hypixel and Mojang
│   ├── log_monitor.py      # Minecraft log file monitor
│   ├── stats_processor.py  # Player statistics processor
│   ├── ranking_engine.py   # Player ranking algorithms
│   ├── nick_detector.py    # Nickname detection heuristics
│   └── main.py             # Application entry point
├── tests/                  # Test cases
├── ai/                     # AI context documentation (this directory)
│   ├── apis/               # API documentation
│   ├── core_logic/         # Core logic documentation
│   └── prompt_addons/      # AI prompt templates
└── .gitignore, requirements.txt, etc.
```

## Purpose of the /ai Directory

This directory serves as context for AI assistants to understand the project structure, architecture, and implementation details. It contains documentation on:

1. The architecture and design of the application
2. API endpoints and data models used
3. Core algorithms and heuristics
4. Testing strategies
5. Developer notes on known issues and improvement areas

This documentation helps AI assistants provide more accurate and contextually relevant assistance when working with the codebase. 