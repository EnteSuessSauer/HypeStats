"""
Main overlay window for displaying Hypixel player statistics.
"""

import pygame
import os
import logging
import tkinter as tk
from tkinter import simpledialog
import time

from hype_stats.ui.components import UIComponents
from hype_stats.utils.settings import Settings
from hype_stats.utils.log_reader import MinecraftLogReader
from hype_stats.api.hypixel_api import HypixelAPI
from hype_stats.api.mojang_api import MojangAPI

logger = logging.getLogger(__name__)

class HypixelStatsOverlay:
    def __init__(self):
        # Initialize settings
        self.settings = Settings()
        
        # Get API key
        self.api_key = self._get_api_key()
        if not self.api_key:
            logger.error("No API key provided. Exiting.")
            return
        
        # Initialize API clients
        self.hypixel_api = HypixelAPI(self.api_key)
        self.mojang_api = MojangAPI()
        
        # Initialize log reader
        self.log_reader = MinecraftLogReader()
        if not self.log_reader.log_path:
            self._prompt_for_log_path()
        
        # Initialize Pygame for the overlay
        self._init_pygame()
        
        # Player data
        self.players = {}
        self.last_lobby_check = 0
        
        # UI states
        self.visible = True
        self.show_settings = False
        self.waiting_for_key = False
        self.key_bind_target = None
        
        # Flag to track if we're checking API or not
        self.checking_api = False
        
        # Running flag
        self.running = True
    
    def _get_api_key(self):
        import os
        from dotenv import load_dotenv
        
        # Try to load from environment
        load_dotenv()
        api_key = os.getenv("HYPIXEL_API_KEY")
        
        # If no API key in .env, prompt user
        if not api_key:
            root = tk.Tk()
            root.withdraw()
            api_key = simpledialog.askstring("Hypixel API Key", "Enter your Hypixel API Key:")
            root.destroy()
            
            # Save API key to .env file if provided
            if api_key:
                with open(".env", "w") as f:
                    f.write(f"HYPIXEL_API_KEY={api_key}\n")
        
        return api_key
    
    def _prompt_for_log_path(self):
        root = tk.Tk()
        root.withdraw()
        log_path = simpledialog.askstring("Minecraft Log Path", 
                                        "Enter the path to your Minecraft latest.log file\n(usually in .minecraft/logs/latest.log):")
        root.destroy()
        
        if log_path and os.path.exists(log_path):
            self.log_reader.log_path = log_path
        else:
            logger.warning("No valid log path provided. Some features may not work.")
    
    def _init_pygame(self):
        pygame.init()
        
        # Set window dimensions
        self.width, self.height = 400, 600
        self.screen = pygame.display.set_mode((self.width, self.height), pygame.NOFRAME)
        pygame.display.set_caption("HypeStats - Hypixel Bedwars Overlay")
        
        # Set window icon (placeholder)
        try:
            icon = pygame.Surface((32, 32))
            icon.fill((100, 100, 255))
            pygame.display.set_icon(icon)
        except:
            pass
        
        # Make window transparent and always on top (platform-specific)
        if os.name == 'nt':  # Windows
            import win32gui
            import win32con
            self.hwnd = pygame.display.get_wm_info()["window"]
            win32gui.SetWindowLong(self.hwnd, win32con.GWL_EXSTYLE,
                                win32gui.GetWindowLong(self.hwnd, win32con.GWL_EXSTYLE) | 
                                win32con.WS_EX_LAYERED | 
                                win32con.WS_EX_TRANSPARENT | 
                                win32con.WS_EX_TOPMOST)
            win32gui.SetLayeredWindowAttributes(self.hwnd, win32con.RGB(0, 0, 0), 0, win32con.LWA_COLORKEY)
        
        # Initialize UI components
        self.ui = UIComponents(self.screen)
        self.ui.set_transparency(self.settings.transparency)
    
    def toggle_visibility(self):
        self.visible = not self.visible
        logger.info(f"Overlay visibility toggled to {self.visible}")
        
        if os.name == 'nt':  # Windows
            import win32gui
            import win32con
            if self.visible:
                win32gui.ShowWindow(self.hwnd, win32con.SW_SHOW)
            else:
                win32gui.ShowWindow(self.hwnd, win32con.SW_HIDE)
    
    def toggle_settings(self):
        self.show_settings = not self.show_settings
        self.ui.clear_ui_elements()
        logger.info(f"Settings panel toggled to {self.show_settings}")
    
    def update_transparency(self, value):
        self.settings.transparency = value
        self.ui.set_transparency(value)
        logger.info(f"Transparency updated to {value}")
    
    def start_key_binding(self, target):
        self.waiting_for_key = True
        self.key_bind_target = target
        logger.info(f"Waiting for key press to bind {target}")
    
    def apply_key_binding(self, key):
        if self.key_bind_target == "toggle":
            self.settings.toggle_key = key
        elif self.key_bind_target == "settings":
            self.settings.settings_key = key
        
        self.waiting_for_key = False
        self.key_bind_target = None
        self.settings.save_settings()
        logger.info(f"Applied new key binding: {pygame.key.name(key)}")
    
    def update_players_data(self):
        if self.checking_api:
            return
        
        self.checking_api = True
        lobby_players = self.log_reader.get_lobby_players()
        
        if not lobby_players:
            self.checking_api = False
            return
        
        updated_players = {}
        for i, username in enumerate(lobby_players[:16]):  # Limit to 16 players
            # Skip players we've already processed
            if username in self.players:
                updated_players[username] = self.players[username]
                continue
                
            # Get player UUID
            uuid = self.mojang_api.get_uuid(username)
            if uuid:
                # Get player data from Hypixel API
                stats_data = self.hypixel_api.get_player_stats(uuid)
                if stats_data:
                    player_stats = self.hypixel_api.parse_bedwars_stats(stats_data, username)
                    updated_players[username] = player_stats
                else:
                    # If API call failed, use default values
                    updated_players[username] = self._get_default_stats(username)
            else:
                # If UUID lookup failed, use default values
                updated_players[username] = self._get_default_stats(username)
                
            # Add delay between API calls to respect rate limits
            if i < len(lobby_players) - 1:
                time.sleep(0.3)
        
        self.players = updated_players
        self.checking_api = False
        logger.info(f"Updated data for {len(updated_players)} players")
    
    def _get_default_stats(self, username):
        return {
            "username": username,
            "level": "N/A",
            "wins": "N/A",
            "kdr": "N/A",
            "winstreak": "N/A",
            "last_game": "N/A"
        }
    
    def draw_settings_panel(self):
        panel_width = self.width - 40
        panel_height = 380
        panel_x = 20
        panel_y = 70
        
        # Draw panel background
        self.ui.draw_rect(panel_x, panel_y, panel_width, panel_height, (40, 40, 40, 220), 10, 2, (100, 100, 100))
        
        # Draw panel header
        self.ui.draw_text("Settings", self.ui.font_large, self.ui.header_color, panel_x + 20, panel_y + 20)
        
        # Draw keybind settings
        toggle_key_name = pygame.key.name(self.settings.toggle_key)
        self.ui.draw_text(f"Toggle Key: {toggle_key_name}", self.ui.font_small, self.ui.text_color, panel_x + 20, panel_y + 80)
        
        settings_key_name = pygame.key.name(self.settings.settings_key)
        self.ui.draw_text(f"Settings Key: {settings_key_name}", self.ui.font_small, self.ui.text_color, panel_x + 20, panel_y + 120)
        
        # Draw transparency setting
        self.ui.draw_text(f"Transparency: {self.settings.transparency}", self.ui.font_small, self.ui.text_color, panel_x + 20, panel_y + 160)
        
        # Draw slider for transparency
        self.ui.draw_slider(
            panel_x + 20, panel_y + 190, panel_width - 40, 20,
            0, 255, self.settings.transparency, self.update_transparency
        )
        
        # Draw buttons for changing keybinds
        self.ui.draw_button(
            panel_x + panel_width - 120, panel_y + 75, 100, 30,
            "Change Key", self.start_key_binding, "toggle"
        )
        
        self.ui.draw_button(
            panel_x + panel_width - 120, panel_y + 115, 100, 30,
            "Change Key", self.start_key_binding, "settings"
        )
        
        # Draw save button
        self.ui.draw_button(
            panel_x + panel_width - 120, panel_y + panel_height - 50, 100, 30,
            "Save", self.settings.save_settings
        )
        
        # Draw disclaimer
        disclaimer_text = [
            "DISCLAIMER:",
            "This overlay is not affiliated with or endorsed by Hypixel.",
            "It follows the Hypixel API policy by only updating player",
            "stats when you join a new lobby, not continuously."
        ]
        
        for i, line in enumerate(disclaimer_text):
            self.ui.draw_text(line, self.ui.font_small, (200, 200, 200), panel_x + 20, panel_y + 240 + i*25)
        
        # If waiting for key press, draw notification
        if self.waiting_for_key:
            self.ui.draw_rect(panel_x + 50, panel_y + panel_height - 100, panel_width - 100, 40, (50, 50, 50, 220), 5)
            self.ui.draw_text("Press any key...", self.ui.font_medium, (255, 255, 0), panel_x + 100, panel_y + panel_height - 90)
    
    def draw_header(self):
        header_height = 60
        
        # Draw header background
        self.ui.draw_rect(0, 0, self.width, header_height, (30, 30, 30, 220), 0, 2, (100, 100, 100))
        
        # Draw title
        self.ui.draw_text("HypeStats", self.ui.font_large, self.ui.header_color, 15, 10)
        
        # Draw subtitle
        self.ui.draw_text("Bedwars Player Statistics", self.ui.font_small, self.ui.text_color, 15, 35)
        
        # Draw player count
        player_count = len(self.players)
        self.ui.draw_text(f"Players: {player_count}", self.ui.font_small, self.ui.text_color, self.width - 15, 20, "right")
    
    def run(self):
        clock = pygame.time.Clock()
        drag = False
        drag_pos = (0, 0)
        
        while self.running:
            # Handle events
            for event in pygame.event.get():
                if event.type == pygame.QUIT:
                    self.running = False
                
                elif event.type == pygame.KEYDOWN:
                    # Handle key binds
                    if event.key == pygame.K_ESCAPE:
                        if self.waiting_for_key:
                            self.waiting_for_key = False
                        elif self.show_settings:
                            self.show_settings = False
                        else:
                            self.running = False
                    elif self.waiting_for_key:
                        # Apply new key binding
                        self.apply_key_binding(event.key)
                    elif event.key == self.settings.toggle_key:
                        self.toggle_visibility()
                    elif event.key == self.settings.settings_key:
                        self.toggle_settings()
                
                elif event.type == pygame.MOUSEBUTTONDOWN:
                    if event.button == 1:  # Left mouse button
                        # Check if clicked on a button
                        clicked_button = False
                        for button in self.ui.buttons:
                            if button["rect"].collidepoint(event.pos):
                                clicked_button = True
                                if button["action"]:
                                    if button["params"]:
                                        button["action"](button["params"])
                                    else:
                                        button["action"]()
                                break
                        
                        # Check if clicked on a slider
                        clicked_slider = False
                        for slider in self.ui.sliders:
                            if slider["handle_rect"].collidepoint(event.pos):
                                clicked_slider = True
                                break
                        
                        # If not clicked on UI element, start dragging
                        if not clicked_button and not clicked_slider:
                            drag = True
                            drag_pos = event.pos
                
                elif event.type == pygame.MOUSEBUTTONUP:
                    if event.button == 1:
                        drag = False
                
                elif event.type == pygame.MOUSEMOTION:
                    if drag and not self.waiting_for_key:
                        mouse_x, mouse_y = event.pos
                        rel_x, rel_y = mouse_x - drag_pos[0], mouse_y - drag_pos[1]
                        window_x, window_y = pygame.display.get_window_pos()
                        pygame.display.set_window_pos(window_x + rel_x, window_y + rel_y)
            
            # Check for lobby changes (only every few seconds)
            current_time = time.time()
            if current_time - self.last_lobby_check > 5:
                self.last_lobby_check = current_time
                if self.log_reader.check_logs_for_lobby_change():
                    # Update player data for new lobby
                    self.update_players_data()
            
            # Clear screen
            self.screen.fill((0, 0, 0))  # Fill with solid black for transparency
            
            if self.visible:
                # Draw header
                self.draw_header()
                
                if self.show_settings:
                    # Draw settings panel
                    self.draw_settings_panel()
                else:
                    # Draw player cards
                    y_pos = 70
                    for username, player_data in self.players.items():
                        self.ui.draw_player_card(player_data, y_pos)
                        y_pos += 110  # Card height + spacing
                        
                        # If we've run out of space, stop drawing cards
                        if y_pos > self.height - 50:
                            break
            
            # Update display
            pygame.display.flip()
            
            # Cap the frame rate
            clock.tick(30)
        
        # Clean up
        pygame.quit()
    
    def start(self):
        logger.info("Starting HypeStats overlay...")
        self.run() 