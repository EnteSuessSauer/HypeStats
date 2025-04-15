"""
Ranking Engine for Hypixel Stats Companion.
Sorts players based on their statistics.
"""
from typing import Dict, Any, List, Optional, Callable

def rank_players(processed_stats_list: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    """
    Rank players based on their Bedwars stars, FKDR, and WLR.
    
    Args:
        processed_stats_list: List of processed player stat dictionaries.
        
    Returns:
        List[Dict[str, Any]]: The sorted list of player stats.
    """
    if not processed_stats_list:
        return []
    
    # Define a key function for sorting
    def sort_key(player: Dict[str, Any]) -> tuple:
        # Extract the sorting values with safe defaults
        stars = player.get('bedwars_stars', 0)
        fkdr = player.get('fkdr', 0.0)
        wlr = player.get('wlr', 0.0)
        
        # Handle non-numeric values
        try:
            stars = int(stars) if stars is not None else 0
        except (ValueError, TypeError):
            stars = 0
            
        try:
            fkdr = float(fkdr) if fkdr is not None else 0.0
        except (ValueError, TypeError):
            fkdr = 0.0
            
        try:
            wlr = float(wlr) if wlr is not None else 0.0
        except (ValueError, TypeError):
            wlr = 0.0
        
        # Calculate a weighted score with increased emphasis on FKDR and WLR
        # FKDR gets 3x weight, WLR gets 2x weight, and stars get 0.5x weight
        score = (fkdr * 3.0) + (wlr * 2.0) + (stars / 20.0)
        
        # Return the negative score for descending sort
        return (-score,)
    
    # Sort the list using the key function
    sorted_list = sorted(processed_stats_list, key=sort_key)
    
    # Add rank to each player
    for i, player in enumerate(sorted_list):
        player['rank'] = i + 1
    
    return sorted_list

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