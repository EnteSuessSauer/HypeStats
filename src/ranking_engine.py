"""
Ranking Engine for Hypixel Stats Companion.
Sorts players based on their statistics.
"""
from typing import Dict, Any, List, Optional, Callable
import copy

def rank_players(players: List[Dict]) -> List[Dict]:
    """
    Rank players based on their Bedwars stats, with emphasis on skill-based metrics.
    
    Args:
        players: List of player stat dictionaries
        
    Returns:
        List of ranked player stat dictionaries
    """
    if not players:
        return []
        
    # Create a copy to avoid modifying the original list
    players_copy = copy.deepcopy(players)
    
    # Define the sorting key function with emphasis on FKDR and WLR
    def sort_key(player):
        # Get numeric stats with fallbacks to 0 for non-numeric values
        try:
            stars = float(player.get('bedwars_stars', 0))
        except (ValueError, TypeError):
            stars = 0
            
        try:
            fkdr = float(player.get('fkdr', 0))
        except (ValueError, TypeError):
            fkdr = 0
            
        try:
            wlr = float(player.get('wlr', 0))
        except (ValueError, TypeError):
            wlr = 0
            
        # Calculate points for each stat (with higher weights for skill-based stats)
        stars_points = stars / 100.0  # Reduced weight for stars
        fkdr_points = fkdr * 5.0     # Increased weight for FKDR
        wlr_points = wlr * 3.0       # Increased weight for WLR
        
        # Cap stars points relative to FKDR points to prevent over-emphasis on stars
        stars_points = min(stars_points, fkdr_points * 0.3)
        
        # Total score (negative for descending sort)
        return -(stars_points + fkdr_points + wlr_points)
    
    # Sort by the key function
    sorted_players = sorted(players_copy, key=sort_key)
    
    return sorted_players

def get_top_players(processed_stats_list: List[Dict[str, Any]], 
                    count: int = 5, 
                    min_stars: Optional[int] = None) -> List[Dict[str, Any]]:
    """
    Get the top N players from the ranked list, optionally filtering by minimum stars.
    
    Args:
        processed_stats_list: List of processed player stat dictionaries.
        count: Number of top players to return.
        min_stars: Optional minimum Bedwars stars to include a player.
        
    Returns:
        List[Dict[str, Any]]: The top N players.
    """
    # First, rank all players
    ranked_players = rank_players(processed_stats_list)
    
    # Filter by minimum stars if specified
    if min_stars is not None:
        ranked_players = [
            player for player in ranked_players 
            if player.get('bedwars_stars', 0) >= min_stars
        ]
    
    # Return the top N players
    return ranked_players[:count]

def rank_by_criteria(processed_stats_list: List[Dict[str, Any]], 
                     criteria: str = 'bedwars_stars',
                     reverse: bool = True) -> List[Dict[str, Any]]:
    """
    Rank players by a specific criteria.
    
    Args:
        processed_stats_list: List of processed player stat dictionaries.
        criteria: The stat key to sort by.
        reverse: Whether to sort in descending order (True) or ascending (False).
        
    Returns:
        List[Dict[str, Any]]: The sorted list of player stats.
    """
    if not processed_stats_list:
        return []
    
    # Define a key function for sorting by the specified criteria
    def sort_key(player: Dict[str, Any]) -> Any:
        value = player.get(criteria, 0)
        
        # Handle non-numeric values for different types
        if criteria in ('bedwars_stars', 'achievement_points', 'karma'):
            try:
                return int(value) if value is not None else 0
            except (ValueError, TypeError):
                return 0
        elif criteria in ('fkdr', 'wlr', 'hypixel_level'):
            try:
                return float(value) if value is not None else 0.0
            except (ValueError, TypeError):
                return 0.0
        else:
            return value
    
    # Sort the list using the key function
    sorted_list = sorted(processed_stats_list, key=sort_key, reverse=reverse)
    
    # Add rank to each player
    for i, player in enumerate(sorted_list):
        player['rank'] = i + 1
    
    return sorted_list 

def get_sort_value(value):
    """
    Convert a table item value to a proper sorting value.
    Handles special cases like '?' and 'N/A'.
    
    Args:
        value: The value to convert for sorting
        
    Returns:
        float: The numeric value for sorting
    """
    # Handle non-numeric special cases
    if value == '?' or value == 'N/A' or value is None:
        return -1.0
    
    # Try to convert to float
    try:
        return float(value)
    except (ValueError, TypeError):
        # For string values, return minimum value
        if isinstance(value, str):
            return -1.0
        return 0.0 