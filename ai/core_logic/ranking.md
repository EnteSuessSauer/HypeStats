# Ranking Logic

This document explains the player ranking algorithms implemented in the `ranking_engine.py` module of the Hypixel Stats Companion App.

## Ranking Criteria

The primary ranking algorithm prioritizes players based on:

1. **Bedwars Stars** (Primary Sort Key): Higher stars indicate more experience in Bedwars
2. **FKDR (Final Kill/Death Ratio)** (Secondary Sort Key): Higher FKDR indicates better combat skill

This ranking approach emphasizes both long-term experience and skill, which are generally accepted indicators of player strength in the Bedwars community.

## Implementation Details

### Main Ranking Function

The main ranking function, `rank_players`, takes a list of player stat dictionaries and returns a sorted list:

```python
def rank_players(processed_stats_list: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    """
    Rank players based on their Bedwars stars and FKDR.
    """
    if not processed_stats_list:
        return []
    
    # Define a key function for sorting
    def sort_key(player: Dict[str, Any]) -> tuple:
        # Extract the sorting values with safe defaults
        stars = player.get('bedwars_stars', 0)
        fkdr = player.get('fkdr', 0.0)
        
        # Handle non-numeric values
        try:
            stars = int(stars) if stars is not None else 0
        except (ValueError, TypeError):
            stars = 0
            
        try:
            fkdr = float(fkdr) if fkdr is not None else 0.0
        except (ValueError, TypeError):
            fkdr = 0.0
        
        # Return a tuple for sorting (stars first, then FKDR)
        return (-stars, -fkdr)  # Negative to sort in descending order
    
    # Sort the list using the key function
    sorted_list = sorted(processed_stats_list, key=sort_key)
    
    # Add rank to each player
    for i, player in enumerate(sorted_list):
        player['rank'] = i + 1
    
    return sorted_list
```

### Sorting Implementation

The function uses several techniques to ensure robust sorting:

1. **Tuple-based Sorting**: Python's `sorted` function with a key function that returns a tuple allows multi-criteria sorting.
2. **Default Values**: Missing values are replaced with sensible defaults (0 for stars, 0.0 for FKDR).
3. **Error Handling**: Type conversion is wrapped in try-except blocks to handle non-numeric values.
4. **Descending Order**: Negative values are used in the sort key to sort in descending order (higher values first).
5. **Rank Assignment**: After sorting, each player is assigned a rank (1-based) to indicate their position.

### Alternative Ranking Functions

The module also provides additional ranking functions for flexibility:

#### Top Players Filter

```python
def get_top_players(processed_stats_list: List[Dict[str, Any]], 
                    count: int = 5, 
                    min_stars: Optional[int] = None) -> List[Dict[str, Any]]:
    """
    Get the top N players, optionally filtering by minimum stars.
    """
    # Rank all players
    ranked_players = rank_players(processed_stats_list)
    
    # Filter by minimum stars if specified
    if min_stars is not None:
        ranked_players = [
            player for player in ranked_players 
            if player.get('bedwars_stars', 0) >= min_stars
        ]
    
    # Return the top N players
    return ranked_players[:count]
```

#### Custom Criteria Ranking

```python
def rank_by_criteria(processed_stats_list: List[Dict[str, Any]], 
                     criteria: str = 'bedwars_stars',
                     reverse: bool = True) -> List[Dict[str, Any]]:
    """
    Rank players by a specific criteria.
    """
    # Sort the list using the specified criteria
    sorted_list = sorted(
        processed_stats_list, 
        key=lambda player: _get_safe_value(player, criteria),
        reverse=reverse
    )
    
    # Add rank to each player
    for i, player in enumerate(sorted_list):
        player['rank'] = i + 1
    
    return sorted_list
```

## Ranking Interpretation

- **Rank 1** is the highest-ranked player (most stars, then highest FKDR).
- Players with the same stars are sub-ranked by their FKDR.
- Players with missing data are ranked lower than players with data.

## Future Enhancements

1. **Weighted Ranking Algorithm**: Implement a scoring system that combines multiple stats with configurable weights
2. **Game Mode Specific Ranking**: Different ranking criteria for different Bedwars game modes (Solo, Doubles, etc.)
3. **Percentile Ranking**: Show players' percentile ranks relative to the overall player population
4. **Custom Sorting Criteria**: Allow users to choose their preferred sorting criteria
5. **Player Skill Estimation**: Develop a more sophisticated algorithm that combines multiple stats to estimate overall player skill 