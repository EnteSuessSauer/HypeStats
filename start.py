#!/usr/bin/env python3
"""
Startup script for the Hypixel Stats Companion application.
This script:
1. Creates a virtual environment if it doesn't exist
2. Installs dependencies
3. Runs the application
"""

import os
import sys
import subprocess
import platform

def main():
    """Main function to build and start the application."""
    print("=== Hypixel Stats Companion Launcher ===")
    
    # Get the base directory of the project
    base_dir = os.path.dirname(os.path.abspath(__file__))
    os.chdir(base_dir)
    
    # Determine Python executable for virtual environment
    python_cmd = "python" if platform.system() == "Windows" else "python3"
    venv_dir = os.path.join(base_dir, "venv")
    
    # Path to virtual environment Python
    if platform.system() == "Windows":
        venv_python = os.path.join(venv_dir, "Scripts", "python.exe")
        venv_pip = os.path.join(venv_dir, "Scripts", "pip.exe")
    else:
        venv_python = os.path.join(venv_dir, "bin", "python")
        venv_pip = os.path.join(venv_dir, "bin", "pip")
    
    # Check if virtual environment exists
    if not os.path.exists(venv_dir):
        print("Creating virtual environment...")
        try:
            subprocess.run([python_cmd, "-m", "venv", venv_dir], check=True)
        except subprocess.CalledProcessError as e:
            print(f"Error creating virtual environment: {e}")
            sys.exit(1)
    
    # Install or update dependencies
    print("Installing dependencies...")
    requirements_file = os.path.join(base_dir, "requirements.txt")
    
    try:
        subprocess.run([venv_pip, "install", "-r", requirements_file], check=True)
    except subprocess.CalledProcessError as e:
        print(f"Error installing dependencies: {e}")
        sys.exit(1)
    
    # Run the application
    print("Starting Hypixel Stats Companion...")
    try:
        os.environ["PYTHONPATH"] = base_dir  # Set PYTHONPATH to include the project root
        main_script = os.path.join(base_dir, "src", "main.py")
        subprocess.run([venv_python, main_script])
    except subprocess.CalledProcessError as e:
        print(f"Error running the application: {e}")
        sys.exit(1)
    except KeyboardInterrupt:
        print("\nApplication terminated by user.")
    
    print("Application closed.")

if __name__ == "__main__":
    main() 