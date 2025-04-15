"""
Tests for the ranking engine.
"""
import pytest
from src.ranking_engine import rank_players, get_top_players, rank_by_criteria

class TestRankingEngine:
    """Tests for the ranking engine functions."""
    
    def test_rank_players_empty_list(self):
        """Test ranking an empty list of players."""
        result = rank_players([])
        assert result == []
    
    def test_rank_players_single_player(self):
        """Test ranking a list with a single player."""
        player = {
            'username': 'TestPlayer',
            'bedwars_stars': 100,
            'fkdr': 2.5
        }
        
        result = rank_players([player])
        
        assert len(result) == 1
        assert result[0]['username'] == 'TestPlayer'
        assert result[0]['rank'] == 1
    
    def test_rank_players_multiple_players(self):
        """Test ranking a list of multiple players."""
        players = [
            {
                'username': 'Player1',
                'bedwars_stars': 100,
                'fkdr': 2.0
            },
            {
                'username': 'Player2',
                'bedwars_stars': 200,
                'fkdr': 1.5
            },
            {
                'username': 'Player3',
                'bedwars_stars': 100,
                'fkdr': 3.0
            }
        ]
        
        result = rank_players(players)
        
        assert len(result) == 3
        
        # Player2 should be first (highest stars)
        assert result[0]['username'] == 'Player2'
        assert result[0]['rank'] == 1
        
        # Player3 should be second (same stars as Player1 but higher FKDR)
        assert result[1]['username'] == 'Player3'
        assert result[1]['rank'] == 2
        
        # Player1 should be third
        assert result[2]['username'] == 'Player1'
        assert result[2]['rank'] == 3
    
    def test_rank_players_missing_keys(self):
        """Test ranking players when some have missing keys."""
        players = [
            {
                'username': 'Player1',
                # Missing bedwars_stars
                'fkdr': 2.0
            },
            {
                'username': 'Player2',
                'bedwars_stars': 200,
                # Missing fkdr
            },
            {
                'username': 'Player3',
                'bedwars_stars': 100,
                'fkdr': 3.0
            }
        ]
        
        result = rank_players(players)
        
        assert len(result) == 3
        
        # Player2 should be first (highest stars, missing FKDR defaults to 0)
        assert result[0]['username'] == 'Player2'
        
        # Player3 should be second (100 stars, 3.0 FKDR)
        assert result[1]['username'] == 'Player3'
        
        # Player1 should be third (missing stars defaults to 0)
        assert result[2]['username'] == 'Player1'
    
    def test_rank_players_non_numeric_values(self):
        """Test ranking players with non-numeric values."""
        players = [
            {
                'username': 'Player1',
                'bedwars_stars': 'invalid',
                'fkdr': 2.0
            },
            {
                'username': 'Player2',
                'bedwars_stars': 200,
                'fkdr': 'invalid'
            },
            {
                'username': 'Player3',
                'bedwars_stars': 100,
                'fkdr': 3.0
            }
        ]
        
        result = rank_players(players)
        
        assert len(result) == 3
        
        # Player2 should be first (valid stars, invalid FKDR defaults to 0)
        assert result[0]['username'] == 'Player2'
        
        # Player3 should be second (100 stars, 3.0 FKDR)
        assert result[1]['username'] == 'Player3'
        
        # Player1 should be third (invalid stars defaults to 0)
        assert result[2]['username'] == 'Player1'
    
    def test_get_top_players(self):
        """Test getting the top players."""
        players = [
            {'username': 'Player1', 'bedwars_stars': 100, 'fkdr': 2.0},
            {'username': 'Player2', 'bedwars_stars': 200, 'fkdr': 1.5},
            {'username': 'Player3', 'bedwars_stars': 150, 'fkdr': 3.0},
            {'username': 'Player4', 'bedwars_stars': 50, 'fkdr': 4.0},
            {'username': 'Player5', 'bedwars_stars': 300, 'fkdr': 1.0}
        ]
        
        # Get top 3 players
        result = get_top_players(players, count=3)
        
        assert len(result) == 3
        assert result[0]['username'] == 'Player5'  # 300 stars
        assert result[1]['username'] == 'Player2'  # 200 stars
        assert result[2]['username'] == 'Player3'  # 150 stars
    
    def test_get_top_players_with_min_stars(self):
        """Test getting the top players with minimum stars filter."""
        players = [
            {'username': 'Player1', 'bedwars_stars': 100, 'fkdr': 2.0},
            {'username': 'Player2', 'bedwars_stars': 200, 'fkdr': 1.5},
            {'username': 'Player3', 'bedwars_stars': 150, 'fkdr': 3.0},
            {'username': 'Player4', 'bedwars_stars': 50, 'fkdr': 4.0},
            {'username': 'Player5', 'bedwars_stars': 300, 'fkdr': 1.0}
        ]
        
        # Get top 3 players with at least 150 stars
        result = get_top_players(players, count=3, min_stars=150)
        
        assert len(result) == 3
        assert result[0]['username'] == 'Player5'  # 300 stars
        assert result[1]['username'] == 'Player2'  # 200 stars
        assert result[2]['username'] == 'Player3'  # 150 stars
    
    def test_rank_by_criteria(self):
        """Test ranking players by a specific criteria."""
        players = [
            {'username': 'Player1', 'bedwars_stars': 100, 'fkdr': 2.0, 'wlr': 1.5},
            {'username': 'Player2', 'bedwars_stars': 200, 'fkdr': 1.5, 'wlr': 2.0},
            {'username': 'Player3', 'bedwars_stars': 150, 'fkdr': 3.0, 'wlr': 1.0}
        ]
        
        # Rank by FKDR (descending)
        result = rank_by_criteria(players, criteria='fkdr')
        
        assert len(result) == 3
        assert result[0]['username'] == 'Player3'  # FKDR 3.0
        assert result[1]['username'] == 'Player1'  # FKDR 2.0
        assert result[2]['username'] == 'Player2'  # FKDR 1.5
        
        # Rank by WLR (descending)
        result = rank_by_criteria(players, criteria='wlr')
        
        assert len(result) == 3
        assert result[0]['username'] == 'Player2'  # WLR 2.0
        assert result[1]['username'] == 'Player1'  # WLR 1.5
        assert result[2]['username'] == 'Player3'  # WLR 1.0
        
        # Rank by bedwars_stars (ascending)
        result = rank_by_criteria(players, criteria='bedwars_stars', reverse=False)
        
        assert len(result) == 3
        assert result[0]['username'] == 'Player1'  # 100 stars
        assert result[1]['username'] == 'Player3'  # 150 stars
        assert result[2]['username'] == 'Player2'  # 200 stars 