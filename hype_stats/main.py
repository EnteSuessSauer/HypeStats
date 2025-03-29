"""
Main entry point for the HypeStats application.
"""

import sys
import logging
import traceback

def setup_logging():
    """Set up logging configuration"""
    logging.basicConfig(
        level=logging.INFO,
        format='%(asctime)s - %(levelname)s - %(name)s - %(message)s',
        handlers=[
            logging.StreamHandler(sys.stdout),
            logging.FileHandler('hypestats.log', 'a')
        ]
    )
    
    # Reduce noise from third-party libraries
    logging.getLogger('urllib3').setLevel(logging.WARNING)
    logging.getLogger('pygame').setLevel(logging.WARNING)

def main():
    """Main entry point for the application"""
    # Set up logging
    setup_logging()
    logger = logging.getLogger(__name__)
    
    try:
        logger.info("Starting HypeStats application...")
        
        # Import here to avoid circular imports
        from hype_stats.ui.overlay import HypixelStatsOverlay
        
        # Create and start the overlay
        overlay = HypixelStatsOverlay()
        overlay.start()
        
        logger.info("HypeStats application closed.")
        return 0
        
    except Exception as e:
        logger.error(f"Unhandled exception: {e}")
        logger.error(traceback.format_exc())
        return 1

if __name__ == "__main__":
    sys.exit(main()) 