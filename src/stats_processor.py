"""
Stats Processor for Hypixel Stats Companion.
Processes raw player stats from the Hypixel API into usable data.
"""
from typing import Dict, Any, List, Optional, Union

def get_nested_value(data: Dict[str, Any], keys: List[str], default: Any = None) -> Any:
    """
    Safely get a value from a nested dictionary.
    
    Args:
        data: The dictionary to extract from.
        keys: A list of keys to navigate the nested structure.
        default: The default value to return if any key is missing.
        
    Returns:
        The value at the specified path, or the default if any key is missing.
    """
    current = data
    for key in keys:
        if not isinstance(current, dict) or key not in current:
            return default
        current = current[key]
    return current

def calculate_fkdr(player_stats: Dict[str, Any]) -> float:
    """
    Calculate Bedwars Final Kill/Death Ratio.
    
    Args:
        player_stats: The raw player stats dictionary from the Hypixel API.
        
    Returns:
        float: The calculated FKDR, or 0.0 if it can't be calculated.
    """
    final_kills = get_nested_value(player_stats, ['stats', 'Bedwars', 'final_kills_bedwars'], 0)
    final_deaths = get_nested_value(player_stats, ['stats', 'Bedwars', 'final_deaths_bedwars'], 0)
    
    # Convert to integers in case they're strings
    try:
        final_kills = int(final_kills)
        final_deaths = int(final_deaths)
    except (ValueError, TypeError):
        return 0.0
    
    # Avoid division by zero
    if final_deaths == 0:
        return float(final_kills) if final_kills > 0 else 0.0
    
    return round(final_kills / final_deaths, 2)

def calculate_wlr(player_stats: Dict[str, Any]) -> float:
    """
    Calculate Bedwars Win/Loss Ratio.
    
    Args:
        player_stats: The raw player stats dictionary from the Hypixel API.
        
    Returns:
        float: The calculated WLR, or 0.0 if it can't be calculated.
    """
    wins = get_nested_value(player_stats, ['stats', 'Bedwars', 'wins_bedwars'], 0)
    losses = get_nested_value(player_stats, ['stats', 'Bedwars', 'losses_bedwars'], 0)
    
    # Convert to integers in case they're strings
    try:
        wins = int(wins)
        losses = int(losses)
    except (ValueError, TypeError):
        return 0.0
    
    # Avoid division by zero
    if losses == 0:
        return float(wins) if wins > 0 else 0.0
    
    return round(wins / losses, 2)

def get_bedwars_stars(player_stats: Dict[str, Any]) -> int:
    """
    Calculate Bedwars star level from the experience value.
    
    Args:
        player_stats: The raw player stats dictionary from the Hypixel API.
        
    Returns:
        int: The calculated Bedwars star level.
    """
    experience = get_nested_value(player_stats, ['stats', 'Bedwars', 'Experience'], 0)
    
    # Convert to integer in case it's a string
    try:
        experience = int(experience)
    except (ValueError, TypeError):
        return 0
    
    # Prestige levels
    prestiges = [
        (0, 0), (500, 100), (1500, 200), (3500, 300), (7000, 400),
        (12000, 500), (20000, 600), (30000, 700), (45000, 800), (65000, 900)
    ]
    
    # Find the highest prestige level
    prestige_level = 0
    for exp_threshold, level in prestiges:
        if experience >= exp_threshold:
            prestige_level = level
    
    # Calculate stars based on prestige and remaining XP
    next_prestige_index = next((i for i, (exp, _) in enumerate(prestiges) if exp > experience), len(prestiges))
    prev_prestige_index = next_prestige_index - 1
    
    if prev_prestige_index < 0:
        return 0
    
    prev_exp, prev_level = prestiges[prev_prestige_index]
    
    # Calculate the level within the current prestige (100 per prestige)
    level_within_prestige = (experience - prev_exp) // 5000
    stars = prev_level + level_within_prestige
    
    return stars

def get_hypixel_level(player_stats: Dict[str, Any]) -> float:
    """
    Get Hypixel network level from player stats.
    
    Args:
        player_stats: The raw player stats dictionary from the Hypixel API.
        
    Returns:
        float: The calculated Hypixel network level.
    """
    exp = get_nested_value(player_stats, ['networkExp'], 0)
    
    # Fallback to networkExp if it exists
    if exp == 0:
        exp = get_nested_value(player_stats, ['networkExp'], 0)
    
    # Convert to float in case it's a string
    try:
        exp = float(exp)
    except (ValueError, TypeError):
        return 0.0
    
    # Formula based on Hypixel network level calculation
    # Level = (√(exp + 15312.5) - 125/sqrt(2)) / (25√2)
    if exp == 0:
        return 1.0
    
    import math
    level = (math.sqrt(exp + 15312.5) - 125/math.sqrt(2)) / (25 * math.sqrt(2))
    
    return round(max(1.0, level), 2)

def extract_relevant_stats(player_stats: Dict[str, Any]) -> Dict[str, Any]:
    """
    Extract relevant statistics from raw player stats.
    
    Args:
        player_stats: The raw player stats dictionary from the Hypixel API.
        
    Returns:
        Dict[str, Any]: A simplified dictionary with relevant stats.
    """
    if not player_stats:
        return {}
    
    # Get the display name or fall back to uuid
    display_name = player_stats.get('displayname', player_stats.get('uuid', 'Unknown'))
    
    # Extract bedwars specific stats for detailed view
    final_kills = get_nested_value(player_stats, ['stats', 'Bedwars', 'final_kills_bedwars'], 0)
    final_deaths = get_nested_value(player_stats, ['stats', 'Bedwars', 'final_deaths_bedwars'], 0)
    wins = get_nested_value(player_stats, ['stats', 'Bedwars', 'wins_bedwars'], 0)
    losses = get_nested_value(player_stats, ['stats', 'Bedwars', 'losses_bedwars'], 0)
    beds_broken = get_nested_value(player_stats, ['stats', 'Bedwars', 'beds_broken_bedwars'], 0)
    beds_lost = get_nested_value(player_stats, ['stats', 'Bedwars', 'beds_lost_bedwars'], 0)
    
    # Calculate beds broken to lost ratio
    bblr = 0.0
    if beds_lost > 0:
        bblr = round(beds_broken / beds_lost, 2)
    elif beds_broken > 0:
        bblr = float(beds_broken)
    
    # Extract stats
    stats = {
        'username': display_name,
        'uuid': player_stats.get('uuid', ''),
        'hypixel_level': get_hypixel_level(player_stats),
        'bedwars_stars': get_bedwars_stars(player_stats),
        'fkdr': calculate_fkdr(player_stats),
        'wlr': calculate_wlr(player_stats),
        'bblr': bblr,
        'achievement_points': get_nested_value(player_stats, ['achievementPoints'], 0),
        'karma': get_nested_value(player_stats, ['karma'], 0),
        'first_login': get_nested_value(player_stats, ['firstLogin'], 0),
        'last_login': get_nested_value(player_stats, ['lastLogin'], 0),
        'bedwars_coins': get_nested_value(player_stats, ['stats', 'Bedwars', 'coins'], 0),
        'bedwars_games_played': wins + losses,
        'bedwars_winstreak': get_nested_value(player_stats, ['stats', 'Bedwars', 'winstreak'], 0),
        
        # Detailed bedwars stats for player details view
        'final_kills': final_kills,
        'final_deaths': final_deaths,
        'wins': wins,
        'losses': losses,
        'beds_broken': beds_broken,
        'beds_lost': beds_lost,
        'winstreak': get_nested_value(player_stats, ['stats', 'Bedwars', 'winstreak'], 0)
    }
    
    return stats 