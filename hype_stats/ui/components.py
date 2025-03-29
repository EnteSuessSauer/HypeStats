"""
Reusable UI components for the Pygame overlay.
"""

import pygame
import logging

logger = logging.getLogger(__name__)

class UIComponents:
    """
    Reusable UI components for the Pygame overlay.
    """
    def __init__(self, screen):
        """
        Initialize UI components.
        
        Args:
            screen (pygame.Surface): The pygame screen surface to draw on
        """
        self.screen = screen
        
        # Define colors
        self.bg_color = (0, 0, 0)
        self.text_color = (255, 255, 255)
        self.header_color = (255, 215, 0)  # Gold
        self.high_skill_color = (0, 255, 0)  # Green
        self.medium_skill_color = (255, 255, 0)  # Yellow
        self.low_skill_color = (255, 0, 0)  # Red
        self.button_color = (70, 70, 70)
        self.button_hover_color = (100, 100, 100)
        self.slider_color = (100, 100, 255)
        
        # Define fonts
        self.font_small = pygame.font.Font(None, 24)
        self.font_medium = pygame.font.Font(None, 28)
        self.font_large = pygame.font.Font(None, 36)
        
        # Track buttons and sliders
        self.buttons = []
        self.sliders = []
    
    def set_transparency(self, transparency):
        """
        Set the transparency level for the background color.
        
        Args:
            transparency (int): Transparency value (0-255)
        """
        self.bg_color = (0, 0, 0, transparency)
    
    def draw_text(self, text, font, color, x, y, align="left"):
        """
        Draw text on the screen.
        
        Args:
            text (str): Text to draw
            font (pygame.font.Font): Font to use
            color (tuple): RGB color tuple
            x (int): X position
            y (int): Y position
            align (str): Text alignment ("left", "center", "right")
            
        Returns:
            pygame.Rect: The text rectangle
        """
        text_surface = font.render(text, True, color)
        text_rect = text_surface.get_rect()
        
        if align == "left":
            text_rect.topleft = (x, y)
        elif align == "center":
            text_rect.midtop = (x, y)
        elif align == "right":
            text_rect.topright = (x, y)
        
        self.screen.blit(text_surface, text_rect)
        return text_rect
    
    def draw_rect(self, x, y, width, height, color, border_radius=0, border_width=0, border_color=None):
        """
        Draw a rectangle on the screen.
        
        Args:
            x (int): X position
            y (int): Y position
            width (int): Rectangle width
            height (int): Rectangle height
            color (tuple): RGB color tuple
            border_radius (int): Border radius
            border_width (int): Border width
            border_color (tuple): Border color
            
        Returns:
            pygame.Rect: The rectangle object
        """
        rect = pygame.Rect(x, y, width, height)
        pygame.draw.rect(self.screen, color, rect, 0, border_radius)
        
        if border_width > 0 and border_color:
            pygame.draw.rect(self.screen, border_color, rect, border_width, border_radius)
        
        return rect
    
    def draw_player_card(self, player_data, y_position):
        """
        Draw a player statistics card.
        
        Args:
            player_data (dict): Player statistics data
            y_position (int): Y position to draw the card
            
        Returns:
            pygame.Rect: The card rectangle
        """
        card_height = 100
        card_width = self.screen.get_width() - 20
        card_x = 10
        
        # Draw card background
        self.draw_rect(
            card_x, y_position, card_width, card_height, 
            (50, 50, 50, 200), 5, 2, (100, 100, 100)
        )
        
        # Draw username
        self.draw_text(
            player_data["username"], self.font_medium, 
            self.header_color, card_x + 10, y_position + 10
        )
        
        # Draw level
        self.draw_text(
            f"Level: {player_data['level']}", self.font_small, 
            self.text_color, card_x + 10, y_position + 40
        )
        
        # Draw KDR with color coding
        kdr = player_data["kdr"]
        if kdr != "N/A":
            if kdr > 3:
                kdr_color = self.high_skill_color
            elif kdr > 1:
                kdr_color = self.medium_skill_color
            else:
                kdr_color = self.low_skill_color
        else:
            kdr_color = self.text_color
        
        self.draw_text(
            f"KDR: {kdr}", self.font_small, 
            kdr_color, card_x + 120, y_position + 40
        )
        
        # Draw wins
        self.draw_text(
            f"Wins: {player_data['wins']}", self.font_small, 
            self.text_color, card_x + 10, y_position + 70
        )
        
        # Draw winstreak
        self.draw_text(
            f"Streak: {player_data['winstreak']}", self.font_small, 
            self.text_color, card_x + 120, y_position + 70
        )
        
        # Draw last game
        self.draw_text(
            f"Last: {player_data['last_game']}", self.font_small, 
            self.text_color, card_x + 220, y_position + 70
        )
        
        return pygame.Rect(card_x, y_position, card_width, card_height)
    
    def draw_button(self, x, y, width, height, text, action=None, params=None):
        """
        Draw a button and handle interaction.
        
        Args:
            x (int): X position
            y (int): Y position
            width (int): Button width
            height (int): Button height
            text (str): Button text
            action (function): Function to call when button is clicked
            params: Parameters to pass to the action function
            
        Returns:
            dict: Button data
        """
        mouse_pos = pygame.mouse.get_pos()
        click = pygame.mouse.get_pressed()[0]
        
        # Check if mouse is over button
        button_rect = pygame.Rect(x, y, width, height)
        is_hover = button_rect.collidepoint(mouse_pos)
        
        # Draw button with hover effect
        color = self.button_hover_color if is_hover else self.button_color
        self.draw_rect(x, y, width, height, color, 5, 2, (200, 200, 200))
        
        # Draw text
        self.draw_text(text, self.font_small, self.text_color, x + width/2, y + height/2, "center")
        
        # Handle click
        if is_hover and click and action:
            if params:
                action(params)
            else:
                action()
        
        # Add to buttons list if not already there
        button_data = {"rect": button_rect, "action": action, "params": params}
        if button_data not in self.buttons:
            self.buttons.append(button_data)
        
        return button_data
    
    def draw_slider(self, x, y, width, height, min_val, max_val, current_val, action=None):
        """
        Draw a slider and handle interaction.
        
        Args:
            x (int): X position
            y (int): Y position
            width (int): Slider width
            height (int): Slider height
            min_val (int): Minimum value
            max_val (int): Maximum value
            current_val (int): Current value
            action (function): Function to call when slider value changes
            
        Returns:
            dict: Slider data
        """
        mouse_pos = pygame.mouse.get_pos()
        click = pygame.mouse.get_pressed()[0]
        
        # Draw slider track
        self.draw_rect(x, y + height//2 - 2, width, 4, (70, 70, 70), 2)
        
        # Calculate handle position
        handle_pos = x + int((current_val - min_val) / (max_val - min_val) * width)
        
        # Create handle rect for hit testing
        handle_rect = pygame.Rect(handle_pos - 8, y, 16, height)
        
        # Check if mouse is dragging the handle
        if handle_rect.collidepoint(mouse_pos) and click:
            # Update value based on mouse position
            new_x = max(x, min(mouse_pos[0], x + width))
            new_val = min_val + (new_x - x) / width * (max_val - min_val)
            if action:
                action(int(new_val))
        
        # Draw handle
        pygame.draw.circle(self.screen, self.slider_color, (handle_pos, y + height//2), 8)
        
        # Add to sliders list if not already there
        slider_data = {
            "rect": pygame.Rect(x, y, width, height),
            "handle_rect": handle_rect,
            "min_val": min_val,
            "max_val": max_val,
            "current_val": current_val,
            "action": action
        }
        if slider_data not in self.sliders:
            self.sliders.append(slider_data)
        
        return slider_data
    
    def clear_ui_elements(self):
        """Clear all tracked UI elements."""
        self.buttons = []
        self.sliders = [] 