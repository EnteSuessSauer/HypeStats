# Hypixel API Documentation

## Overview

The Hypixel API provides programmatic access to player statistics, status, and other data from the Hypixel Minecraft server. The Hypixel Stats Companion App uses these endpoints to retrieve player information for analysis and display.

## Authentication

All requests to the Hypixel API require an API key, which can be obtained in two ways:

### Method 1: In-Game Command (Traditional)
1. Connect to the Hypixel server (`mc.hypixel.net`)
2. Run the `/api new` command in-game
3. Copy the generated API key

### Method 2: Developer Dashboard (New)
1. Visit the Hypixel Developer Dashboard at https://developer.hypixel.net/
2. Create an account or log in
3. Generate a new API key from the dashboard

The API key is stored in the `config.ini` file and loaded through the `src/utils/config.py` module.

## Rate Limiting

The Hypixel API has the following rate limits:

- 300 requests per 5 minutes (1 request per second on average)
- The limit resets every 5 minutes

The application implements a basic rate limiting mechanism in the `_make_request` method of the `ApiClient` class:

```python
# Simple rate limiting to avoid hitting API limits
if time_since_last_request < 0.5:  # Wait at least 0.5 seconds between requests
    time.sleep(0.5 - time_since_last_request)
```

**Improvement Area:** For a more robust implementation, a queue-based approach with token bucket rate limiting would be more appropriate, especially when dealing with many players simultaneously.

## Endpoints Used

### 1. Player Endpoint

**URL:** `https://api.hypixel.net/player`

**Parameters:**
- `key`: Your API key
- `uuid`: The player's UUID (obtained from Mojang API)

**Response Structure (Simplified):**
```json
{
  "success": true,
  "player": {
    "uuid": "player-uuid",
    "displayname": "Player1",
    "networkExp": 123456,
    "achievementPoints": 1000,
    "karma": 5000,
    "firstLogin": 1577836800000,
    "lastLogin": 1609459200000,
    "achievements": {
      "bedwars_level": 120
    },
    "stats": {
      "Bedwars": {
        "Experience": 12345,
        "wins_bedwars": 500,
        "losses_bedwars": 250,
        "final_kills_bedwars": 1000,
        "final_deaths_bedwars": 400,
        "coins": 50000,
        "winstreak": 7
      }
    }
  }
}
```

**Key Fields Extracted:**

The `StatsProcessor` extracts and processes the following fields:

- `displayname`: The player's username
- `networkExp`: Used to calculate the player's Hypixel network level
- `achievementPoints`: Achievement points earned
- `karma`: Karma points earned
- `firstLogin`: Timestamp of first login (used for nick detection)
- `lastLogin`: Timestamp of last login
- `achievements.bedwars_level`: Directly gives Bedwars star level (primary source)
- `stats.Bedwars.Experience`: Used as fallback to calculate Bedwars star level if not available in achievements
- `stats.Bedwars.wins_bedwars`: Bedwars wins
- `stats.Bedwars.losses_bedwars`: Bedwars losses
- `stats.Bedwars.final_kills_bedwars`: Bedwars final kills
- `stats.Bedwars.final_deaths_bedwars`: Bedwars final deaths
- `stats.Bedwars.coins`: Bedwars coins
- `stats.Bedwars.winstreak`: Current Bedwars win streak

### 2. Status Endpoint

**URL:** `https://api.hypixel.net/status`

**Parameters:**
- `key`: Your API key
- `uuid`: The player's UUID (obtained from Mojang API)

**Response Structure:**
```json
{
  "success": true,
  "session": {
    "online": true,
    "gameType": "BEDWARS",
    "mode": "FOUR_FOUR",
    "map": "Lighthouse"
  }
}
```

**Key Fields Used:**
- `online`: Whether the player is currently online
- `gameType`: The game type the player is currently playing
- `mode`: The specific game mode being played

## Error Handling

The API client handles several types of errors:

1. **Network Errors**: Connection issues, timeouts
2. **Authentication Errors**: Invalid API key
3. **Rate Limiting Errors**: Too many requests
4. **Missing Player Data**: Player not found or no data available

Error handling is implemented in the `_make_request` method with specific handling for API-specific error responses:

```python
if response.status_code != 200:
    raise ValueError(f"API request failed with status code {response.status_code}: {response.text}")

response_json = response.json()

# Handle Hypixel API specific error responses
if 'success' in response_json and not response_json['success']:
    error_msg = response_json.get('cause', 'Unknown API error')
    raise ValueError(f"API request failed: {error_msg}")
```

## Future Improvements

1. **Enhanced Rate Limiting**: Implement a more sophisticated rate limiting mechanism, such as a token bucket algorithm
2. **Caching**: Add caching for frequently accessed player data to reduce API calls
3. **Batch Requests**: Investigate if the API supports batch requests to fetch multiple players at once
4. **Retries**: Add a retry mechanism for transient errors
5. **Webhook Support**: Consider supporting Hypixel API webhooks for real-time updates if they become available 