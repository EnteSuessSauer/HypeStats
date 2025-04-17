# Architecture Overview

The Hypixel Stats Companion App is structured as a desktop application built with Python and PyQt6. This document outlines the high-level architecture, component interactions, and data flow.

## Architecture Diagram

```
┌─────────────────┐       ┌──────────────┐       ┌───────────────┐
│                 │       │              │       │               │
│  Main Window    │←──────│ Log Monitor  │←──────│ Minecraft     │
│  (UI)           │       │              │       │ Log File      │
│                 │       └──────────────┘       └───────────────┘
│                 │              │
│                 │              │
│                 │              ▼
│                 │       ┌──────────────┐       ┌───────────────┐
│                 │       │              │       │               │
│                 │←──────│  API Client  │←──────│  Hypixel &    │
│                 │       │              │       │  Mojang APIs  │
│                 │       └──────────────┘       └───────────────┘
│                 │              │
│                 │              │
│                 │              ▼
│                 │       ┌──────────────┐
│                 │       │  Stats       │
│                 │←──────│  Processor   │
│                 │       │              │
│                 │       └──────────────┘
│                 │              │
│                 │              │
│                 │              ▼
│                 │       ┌──────────────┐
│                 │       │  Ranking     │
│                 │←──────│  Engine      │
│                 │       │              │
│                 │       └──────────────┘
│                 │              │
│                 │              │
│                 │              ▼
│                 │       ┌──────────────┐
│                 │       │  Nick        │
│                 │←──────│  Detector    │
│                 │       │              │
└─────────────────┘       └──────────────┘
```

## Component Interactions

### User Interface (MainWindow)

The `MainWindow` class is the central UI component that coordinates the interaction between the user and the backend services. It:

1. Initializes the API client, log monitor, and other components
2. Displays player statistics in a table format
3. Provides manual player lookup functionality
4. Shows lobby status and nick detection warnings
5. Uses QTimer to periodically check the log file

### Log Monitor

The `LogMonitor` class:

1. Uses the `watchdog` library to observe the Minecraft log file for changes
2. Parses log entries to identify `/who` command output
3. Extracts player usernames from the log output
4. Triggers updates to the UI via a callback mechanism when new players are detected

### API Client

The `ApiClient` class:

1. Handles communication with Hypixel and Mojang APIs
2. Manages API authentication using the configured API key
3. Implements basic rate limiting to avoid hitting API limits
4. Provides error handling for network and API-specific errors
5. Exposes methods to fetch player UUIDs, stats, and online status

### Stats Processor

The stats processor module:

1. Takes raw JSON data from the Hypixel API
2. Extracts relevant statistics (e.g., bedwars stats)
3. Calculates derived metrics like FKDR (Final Kill/Death Ratio) and WLR (Win/Loss Ratio)
4. Produces a simplified player stats dictionary that's easier to work with

### Ranking Engine

The ranking engine module:

1. Takes a list of processed player statistics
2. Sorts players based on configurable criteria (primarily bedwars stars, then FKDR)
3. Provides utilities to get the top N players or filter by minimum criteria
4. Assigns rank numbers to each player based on the sorting

### Nick Detector

The nick detector module:

1. Analyzes player statistics for anomalies that might indicate a nicked player
2. Implements basic heuristics like comparing hypixel level to bedwars stars
3. Assigns a confidence score for whether a player is likely using a nickname
4. Provides textual descriptions of the likelihood (e.g., "Likely Not Nicked", "Probably Nicked")

## Data Flow

1. The log monitor detects player names from the Minecraft log file through:
   a. File system events (using watchdog's Observer daemon thread with proper cleanup)
   b. Periodic checks via QTimer from the main thread
   
2. The main window receives these names via a callback and:
   a. Uses the `StatsProcessor` to synchronously fetch and process player stats in batches
   b. Updates the UI with progress information during processing
   c. Processes Qt events between batches to maintain UI responsiveness
   d. Displays the results in the table

3. The processing flow for each player:
   a. Request UUID for username from the Mojang API
   b. Fetch player statistics from the Hypixel API
   c. Process these statistics using the stats processor
   d. Rank the players using the ranking engine
   e. Estimate nick probability using the nick detector

4. The UI is updated with player information in a tabular format, with options to sort by various stats.

## Application Design

The application follows a predominantly single-threaded design with event-driven updates:

1. UI responsiveness is maintained through Qt's event loop architecture and strategic QCoreApplication.processEvents() calls
2. Instead of background worker threads, the application processes data in batches with intermediate UI updates
3. Long operations are broken down into smaller chunks with progress updates to keep the user informed
4. The `watchdog` library's Observer runs as a daemon thread with proper timeout handling
5. Thread locks are implemented with context managers to ensure proper cleanup even during exceptions
6. The application is designed to gracefully shut down by properly cleaning up resources and terminating threads

This design reduces the complexity and potential issues of multithreaded programming, while still providing a responsive user experience.

## Configuration

The application uses a configuration file (`config.ini`) to store:

1. The Hypixel API key
2. The path to the Minecraft log file
3. Log polling interval settings

This is managed by the utilities in `src/utils/config.py`. 