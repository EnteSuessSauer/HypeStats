# Nick Detection Logic

This document details the heuristic approaches used for nick detection in the Hypixel Stats Companion App and their limitations.

## What is "Nicking"?

In Hypixel, "nicking" refers to the practice of playing under a temporary alternative username. This feature is available to certain ranks and allows players to hide their identity and stats from opponents. Detecting nicked players is important because:

1. Their true skill level is hidden from regular API lookups
2. They may be significantly more skilled than their temporary profile suggests
3. Players want to know if they're facing nicked opponents for strategic purposes

## Detection Limitations

It's crucial to understand the **significant limitations** of nick detection:

1. **No Guaranteed Method**: There is no foolproof way to determine if a player is nicked
2. **False Positives**: Some legitimate new players or alt accounts may be flagged as nicked
3. **False Negatives**: Some nicked players may have convincing temporary profiles
4. **Rapidly Changing**: Hypixel may change how nicking works, breaking detection methods

**Note**: The implementation in this app is a basic demonstration and should not be relied upon for competitive analysis.

## Implemented Heuristics

The `estimate_if_nicked` function in `src/nick_detector.py` assigns a confidence score (0.0 to 1.0) based on multiple heuristics:

### Heuristic 1: Low Bedwars Stars Despite High Hypixel Level

```python
# Heuristic 1: Low BedWars stars despite high Hypixel level
# WARNING: Unreliable as some players might simply not play BedWars much
hypixel_level = processed_stats.get('hypixel_level', 0)
bedwars_stars = processed_stats.get('bedwars_stars', 0)

if hypixel_level > 50 and bedwars_stars < 5:
    score += 0.3
    heuristics_count += 1
```

**Rationale**: If a player has a high network level but very low Bedwars stars, it might indicate a nicked player as the temporary profile often has minimal Bedwars stats.
**Limitation**: Some players genuinely focus on other game modes and rarely play Bedwars.

### Heuristic 2: Very Low Achievement Points

```python
# Heuristic 2: Very low achievement points
# WARNING: Unreliable as new players will naturally have low achievement points
achievement_points = processed_stats.get('achievement_points', 0)

if achievement_points < 500:
    score += 0.2
    heuristics_count += 1
```

**Rationale**: Nicked profiles typically have few achievements.
**Limitation**: Legitimate new players also have few achievements.

### Heuristic 3: Unusually Empty Stats Profile

```python
# Heuristic 3: Stats profile seems unusually empty
# WARNING: Unreliable as API might return incomplete data
empty_stats_count = sum(1 for value in processed_stats.values() if value in (0, 0.0, '', None))

if empty_stats_count > len(processed_stats) / 2:
    score += 0.25
    heuristics_count += 1
```

**Rationale**: Nicked profiles often have minimal stats recorded.
**Limitation**: API limitations or errors may cause incomplete data for legitimate players.

### Heuristic 4: Recent Account Creation

```python
# Heuristic 4: First login and last login are very close
# WARNING: Unreliable as this could indicate a newer account
first_login = processed_stats.get('first_login', 0)
last_login = processed_stats.get('last_login', 0)

if first_login > 0 and last_login > 0:
    time_diff_days = (last_login - first_login) / (1000 * 60 * 60 * 24)
    if time_diff_days < 7:  # Less than a week old account
        score += 0.25
        heuristics_count += 1
```

**Rationale**: Nicked profiles often appear newly created.
**Limitation**: Many legitimate new players join Hypixel every day.

## Confidence Score Calculation

The final confidence score is the sum of individual heuristic scores, capped at 1.0:

```python
final_score = min(1.0, score)
```

## Textual Interpretation

The confidence score is converted to a human-readable description:

```python
def get_nick_probability_description(nick_score: float) -> str:
    """
    Get a textual description for a nick probability score.
    """
    if nick_score < 0.2:
        return "Likely Not Nicked"
    elif nick_score < 0.4:
        return "Possibly Nicked"
    elif nick_score < 0.7:
        return "Probably Nicked"
    else:
        return "Very Likely Nicked"
```

## UI Integration

In the UI, these descriptions are displayed with color coding:
- "Very Likely Nicked" shows with a red background
- "Probably Nicked" shows with a yellow background

## Lobby Analysis

The module also provides a function to analyze an entire lobby:

```python
def analyze_lobby_for_nicks(processed_stats_list: List[Dict[str, Any]]) -> Dict[str, Any]:
    """
    Analyze a lobby for the presence of suspected nicked players.
    """
    # Calculate nick scores for all players
    for player in processed_stats_list:
        player['nick_score'] = estimate_if_nicked(player)
        player['nick_description'] = get_nick_probability_description(player['nick_score'])
    
    # Collect suspected nicked players (score >= 0.4)
    suspected_nicks = [player for player in processed_stats_list if player.get('nick_score', 0.0) >= 0.4]
    
    # Calculate statistics
    total_players = len(processed_stats_list)
    suspected_nick_count = len(suspected_nicks)
    nick_percentage = (suspected_nick_count / total_players) * 100 if total_players > 0 else 0
    
    return {
        'total_players': total_players,
        'suspected_nicks': suspected_nick_count,
        'nick_percentage': round(nick_percentage, 1),
        'suspected_players': suspected_nicks
    }
```

## Future Improvements

1. **Machine Learning Approach**: Collect data on known nicked vs. non-nicked players and train a classification model
2. **Behavioral Analysis**: Analyze gameplay patterns that might indicate skill levels inconsistent with stats
3. **Statistical Anomalies**: Implement more sophisticated statistical analysis to identify outliers
4. **API Updates**: Monitor for any new Hypixel API features that could help with nick detection
5. **Community Input**: Create a system for users to report confirmed nicked players to improve detection heuristics
6. **Configuration**: Allow users to adjust the sensitivity of detection (threshold values) 