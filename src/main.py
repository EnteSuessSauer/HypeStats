"""
Main entry point for the Hypixel Stats Companion App.
"""
import sys
from PyQt6.QtWidgets import QApplication

from src.ui.main_window import MainWindow

def main():
    """
    Main application entry point.
    """
    app = QApplication(sys.argv)
    
    # Set application name and organization
    app.setApplicationName("Hypixel Stats Companion")
    app.setOrganizationName("HypixelCompanion")
    
    # Create and show the main window
    main_window = MainWindow()
    main_window.show()
    
    # Start the application event loop
    sys.exit(app.exec())

if __name__ == "__main__":
    main() 