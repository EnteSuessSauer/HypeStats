import requests
import logging
import time
import os
from dotenv import load_dotenv

logger = logging.getLogger(__name__)

class HypixelAPI:
    def __init__(self, api_key=None):
        if api_key is None:
            load_dotenv()
            api_key = os.getenv('HYPIXEL_API_KEY')
        self.api_key = api_key
        self.stats_cache = {}
        self.rate_limit_remaining = 120
        self.rate_limit_reset_time = time.time() + 60

    def get_player_stats(self, uuid):
        if uuid in self.stats_cache:
            return self.stats_cache[uuid]
        
        current_time = time.time()
        if current_time > self.rate_limit_reset_time:
            self.rate_limit_remaining = 120
            self.rate_limit_reset_time = current_time + 60
        
        if self.rate_limit_remaining <= 0:
            return None
        
        if not self.api_key:
            return None
        
        try:
            response = requests.get(f'https://api.hypixel.net/player?uuid={uuid}&key={self.api_key}')
            self.rate_limit_remaining -= 1
            
            if response.status_code == 200:
                data = response.json()
                self.stats_cache[uuid] = data
                return data
            return None
        except Exception as e:
            return None
    
    def parse_bedwars_stats(self, data, username):
        if not data or 'player' not in data or not data['player']:
            return {
                'username': username,
                'level': 'N/A',
                'wins': 'N/A', 
                'kdr': 'N/A',
                'winstreak': 'N/A',
                'last_game': 'N/A'
            }
        
        player = data['player']
        stats = player.get('stats', {})
        
        network_exp = player.get('networkExp', 0)
        level = self.calculate_network_level(network_exp)
        
        bedwars = stats.get('Bedwars', {})
        wins = bedwars.get('wins_bedwars', 0)
        losses = bedwars.get('losses_bedwars', 0)
        kills = bedwars.get('kills_bedwars', 0)
        deaths = bedwars.get('deaths_bedwars', 0)
        winstreak = bedwars.get('winstreak', 0)
        
        kdr = round(kills / max(deaths, 1), 2)
        
        last_game = player.get('mostRecentGameType', 'Unknown')
        
        return {
            'username': username,
            'level': level,
            'wins': wins,
            'kdr': kdr,
            'winstreak': winstreak,
            'last_game': last_game
        }
    
    @staticmethod
    def calculate_network_level(exp):
        return round(1 + (-8750.0 + (8750 ** 2 + 5000 * exp) ** 0.5) / 2500, 2)
