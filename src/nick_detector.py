"""
Nick Detector for Hypixel Stats Companion.
Implements heuristics to estimate if a player is using a nickname (nicked).
"""
from typing import Dict, Any, List, Optional, Union
import time

def estimate_if_nicked(processed_stats: Dict[str, Any]) -> float:
    """
    Estimate the likelihood that a player is using a nickname based on various heuristics.
    
    IMPORTANT NOTE: These heuristics are extremely basic and unreliable. They are provided
    as a simple example and should not be considered accurate for serious usage.
    
    Args:
        processed_stats: A dictionary of processed player statistics.
        
    Returns:
        float: A confidence score from 0.0 to 1.0, where 0.0 means likely not nicked,
              and 1.0 means highly suspect based on these simple checks.
    """
    if not processed_stats:
        return 0.0
    
    score = 0.0
    heuristics_count = 0
    
    # Heuristic 1: Low BedWars stars despite high Hypixel level
    # WARNING: Unreliable as some players might simply not play BedWars much
    hypixel_level = processed_stats.get('hypixel_level', 0)
    bedwars_stars = processed_stats.get('bedwars_stars', 0)
    
    if hypixel_level > 50 and bedwars_stars < 5:
        score += 0.3
        heuristics_count += 1
    
    # Heuristic 2: Very low achievement points
    # WARNING: Unreliable as new players will naturally have low achievement points
    achievement_points = processed_stats.get('achievement_points', 0)
    
    if achievement_points < 500:
        score += 0.2
        heuristics_count += 1
    
    # Heuristic 3: Stats profile seems unusually empty
    # WARNING: Unreliable as API might return incomplete data
    empty_stats_count = sum(1 for value in processed_stats.values() if value in (0, 0.0, '', None))
    
    if empty_stats_count > len(processed_stats) / 2:
        score += 0.25
        heuristics_count += 1
    
    # Heuristic 4: First login and last login are very close
    # WARNING: Unreliable as this could indicate a newer account
    first_login = processed_stats.get('first_login', 0)
    last_login = processed_stats.get('last_login', 0)
    
    if first_login > 0 and last_login > 0:
        time_diff_days = (last_login - first_login) / (1000 * 60 * 60 * 24)
        if time_diff_days < 7:  # Less than a week old account
            score += 0.25
            heuristics_count += 1
    
    # Calculate final score
    final_score = min(1.0, score)
    
    return final_score

def get_nick_probability_description(nick_score: float) -> str:
    """
    Get a textual description for a nick probability score.
    
    Args:
        nick_score: The nick probability score (0.0 to 1.0).
        
    Returns:
        str: A description of the nick probability.
    """
    if nick_score < 0.2:
        return "Likely Not Nicked"
    elif nick_score < 0.4:
        return "Possibly Nicked"
    elif nick_score < 0.7:
        return "Probably Nicked"
    else:
        return "Very Likely Nicked"

def analyze_lobby_for_nicks(processed_stats_list: List[Dict[str, Any]]) -> Dict[str, Any]:
    """
    Analyze a lobby for the presence of suspected nicked players.
    
    Args:
        processed_stats_list: A list of processed player statistics.
        
    Returns:
        Dict[str, Any]: Statistics about nicked players in the lobby.
    """
    if not processed_stats_list:
        return {
            'total_players': 0,
            'suspected_nicks': 0,
            'nick_percentage': 0.0,
            'suspected_players': []
        }
    
    # Calculate nick scores for all players
    for player in processed_stats_list:
        player['nick_score'] = estimate_if_nicked(player)
        player['nick_description'] = get_nick_probability_description(player['nick_score'])
    
    # Collect suspected nicked players (score >= 0.4)
    suspected_nicks = [
        {
            'username': player.get('username', 'Unknown'),
            'nick_score': player.get('nick_score', 0.0),
            'nick_description': player.get('nick_description', 'Unknown')
        }
        for player in processed_stats_list
        if player.get('nick_score', 0.0) >= 0.4
    ]
    
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