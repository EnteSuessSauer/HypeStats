# Data Models

This document describes the key data structures used in the Hypixel Stats Companion App, focusing on the processed player stats model that's used throughout the application.

## Processed Player Stats Dictionary

The `extract_relevant_stats` function in `src/stats_processor.py` transforms the raw API response into a standardized dictionary with the following structure:

```python
{
    'username': 'PlayerName',           # String: Player's display name
    'uuid': '38ba542eb5924c81a5fe8692eb4da596',  # String: Player's UUID (without hyphens)
    
    # Calculated metrics
    'hypixel_level': 120.5,             # Float: Player's Hypixel network level
    'bedwars_stars': 450,               # Integer: Bedwars star level (prestige)
    'fkdr': 3.75,                       # Float: Final Kill/Death Ratio
    'wlr': 2.1,                         # Float: Win/Loss Ratio
    
    # Raw stats
    'achievement_points': 5000,         # Integer: Achievement points
    'karma': 12500,                     # Integer: Karma points
    'first_login': 1577836800000,       # Long: Timestamp of first login (ms since epoch)
    'last_login': 1609459200000,        # Long: Timestamp of last login (ms since epoch)
    'bedwars_coins': 250000,            # Integer: Bedwars coins
    'bedwars_games_played': 1500,       # Integer: Total Bedwars games played
    'bedwars_winstreak': 7,             # Integer: Current Bedwars win streak
    
    # Added by ranking engine
    'rank': 1,                          # Integer: Player's rank in the sorted list
    
    # Added by nick detector
    'nick_score': 0.15,                 # Float: Likelihood that the player is nicked (0.0-1.0)
    'nick_description': 'Likely Not Nicked'  # String: Textual description of nick probability
}
```

### Key Fields Explained

#### Identity Fields

- `username`: The player's display name as shown in Minecraft.
- `uuid`: The player's unique identifier used across Hypixel and Mojang APIs.

#### Calculated Metrics

- `hypixel_level`: Player's network-wide level, calculated from network experience using a specific formula implemented in `get_hypixel_level()`.
- `bedwars_stars`: Bedwars prestige/star level, calculated from Bedwars experience using prestige thresholds in `get_bedwars_stars()`.
- `fkdr`: Final Kill/Death Ratio, a key competitive metric in Bedwars, calculated as `final_kills / final_deaths`.
- `wlr`: Win/Loss Ratio, calculated as `wins / losses`.

#### Raw Stats

- `achievement_points`: Points earned from completing achievements.
- `karma`: Points earned from positive community interactions.
- `first_login`: Timestamp of the player's first login to Hypixel.
- `last_login`: Timestamp of the player's most recent login.
- `bedwars_coins`: In-game currency specific to Bedwars.
- `bedwars_games_played`: Total number of Bedwars games played.
- `bedwars_winstreak`: Current consecutive wins in Bedwars.

#### Analysis Fields

- `rank`: Added by the ranking engine to indicate the player's position in the sorted list.
- `nick_score`: Added by the nick detector to quantify the likelihood that a player is using a nickname.
- `nick_description`: Textual representation of the nick score.

## Example Transformation Flow

1. Raw Hypixel API response ➡️ `api_client.get_player_stats(uuid)`
2. Processed stats dictionary ➡️ `stats_processor.extract_relevant_stats(player_data)`
3. Ranked list of player dictionaries ➡️ `ranking_engine.rank_players(processed_stats_list)`
4. Nick analysis added ➡️ `nick_detector.estimate_if_nicked(player)` for each player
5. Display in UI table ➡️ `MainWindow._populate_table(player_stats)`

## Data Type Handling

The application handles various data type issues:

- **Missing Values**: The `get_nested_value` function safely extracts values from nested dictionaries with default fallbacks.
- **Type Conversion**: Numerical values are converted to the appropriate type (int, float) with error handling.
- **Division by Zero**: Ratio calculations (FKDR, WLR) include checks to prevent division by zero.
- **Timestamps**: Unix timestamps are in milliseconds since epoch.

## Future Enhancements

1. **Structured Classes**: Convert the dictionary model to proper Python classes with validation.
2. **Data Validation**: Add more robust validation for fields.
3. **Additional Game Modes**: Extend the model to include statistics from other Hypixel game modes.
4. **Historical Tracking**: Add support for tracking changes in player stats over time. 