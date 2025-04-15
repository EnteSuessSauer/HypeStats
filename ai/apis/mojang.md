# Mojang API Documentation

## Overview

The Mojang API is used to convert Minecraft usernames to UUIDs, which are required for most calls to the Hypixel API. The Hypixel Stats Companion App uses the profile endpoint to retrieve player UUIDs from their Minecraft usernames.

## Authentication

Unlike the Hypixel API, the Mojang API does not require authentication for the endpoints used in this application. However, it does have stricter rate limits.

## Rate Limiting

The Mojang API has the following rate limits:

- 600 requests per 10 minutes (1 request per second on average)
- If exceeded, your IP may be temporarily banned from accessing the API

The application uses the same basic rate limiting mechanism as for the Hypixel API, with a minimum delay between requests:

```python
# Simple rate limiting to avoid hitting API limits
if time_since_last_request < 0.5:  # Wait at least 0.5 seconds between requests
    time.sleep(0.5 - time_since_last_request)
```

## Endpoints Used

### Profile Endpoint

**URL:** `https://api.mojang.com/users/profiles/minecraft/{username}`

**Parameters:**
- `username`: The Minecraft username to convert to UUID

**Response Structure:**
```json
{
  "id": "38ba542eb5924c81a5fe8692eb4da596",
  "name": "PlayerName"
}
```

**Key Fields Used:**
- `id`: The player's UUID (without hyphens)
- `name`: The player's current username (may differ in capitalization from the requested username)

## UUID Format

The Mojang API returns UUIDs without hyphens, which is the format expected by the Hypixel API. Example:

- Mojang API UUID (used in our app): `38ba542eb5924c81a5fe8692eb4da596`
- Dashed UUID format (not used): `38ba542e-b592-4c81-a5fe-8692eb4da596`

## Error Handling

The API client handles several types of errors from the Mojang API:

1. **Not Found (404)**: Returned when the username does not exist
2. **Rate Limiting (429)**: Returned when too many requests are made
3. **Server Errors (500+)**: Returned when there are issues with the Mojang API

Error handling follows the same pattern as for Hypixel API calls:

```python
if response.status_code != 200:
    raise ValueError(f"API request failed with status code {response.status_code}: {response.text}")
```

Special handling for missing players:

```python
if not response or 'id' not in response:
    raise ValueError(f"Player '{username}' not found")
```

## Caching Considerations

Since Minecraft usernames can be changed but UUIDs remain constant, it's generally good practice to cache the UUID for a username once it's been fetched. However, this caching must consider:

1. Username changes (the cache should be refreshed periodically)
2. Case-sensitivity (Mojang usernames are case-insensitive for lookup but preserve case in storage)

The current implementation does not include UUID caching, which could be an area for future optimization.

## Future Improvements

1. **Implement UUID Caching**: Cache UUID lookups to reduce API calls for frequently searched players
2. **Batch Username Lookup**: If player volume grows, consider implementing batch username to UUID conversion using Mojang's batch API endpoint
3. **Fallback Mechanisms**: Implement fallbacks to other UUID lookup services in case Mojang API has downtime 