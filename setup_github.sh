#!/bin/bash

# Setup script to push FlowPay to GitHub
echo "Setting up FlowPay repository for GitHub..."

# Navigate to project directory
cd "/Users/kabeermalhotra/Library/Mobile Documents/com~apple~CloudDocs/Desktop/FLOWPAYYYYY"

# Remove any existing .git directory
rm -rf .git

# Initialize git repository
echo "Initializing git repository..."
git init

# Add all files
echo "Adding all files..."
git add .

# Create initial commit
echo "Creating initial commit..."
git commit -m "Initial commit: FlowPay MVP Android app with USSD overlay and SMS parsing features"

# Add remote origin
echo "Adding remote origin..."
git remote add origin https://github.com/kabeermalhotra77/FlowPay_MVP_Final.git

# Set main branch
echo "Setting main branch..."
git branch -M main

# Push to GitHub
echo "Pushing to GitHub..."
git push -u origin main

echo "Setup complete!"
