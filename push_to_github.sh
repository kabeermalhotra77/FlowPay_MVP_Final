#!/bin/bash

# Script to push FlowPay to GitHub
echo "🚀 Pushing FlowPay to GitHub..."

# Navigate to project directory
cd "/Users/kabeermalhotra/Library/Mobile Documents/com~apple~CloudDocs/Desktop/FLOWPAYYYYY"

# Remove any existing .git directory
echo "🧹 Cleaning up existing git repository..."
rm -rf .git

# Initialize git repository
echo "📦 Initializing git repository..."
git init

# Add all files
echo "📁 Adding all files..."
git add .

# Create initial commit
echo "💾 Creating initial commit..."
git commit -m "Initial commit: FlowPay MVP Android app with USSD overlay and SMS parsing features

- Complete Android app with USSD overlay functionality
- SMS parsing and transaction detection
- QR code scanning capabilities
- Modern UI with glassmorphic design
- Comprehensive test suite and documentation
- 239 files with 30,416+ lines of code"

# Add remote origin (using HTTPS)
echo "🔗 Adding remote origin..."
git remote add origin https://github.com/kabeermalhotra77/FlowPay_MVP_Final.git

# Set main branch
echo "🌿 Setting main branch..."
git branch -M main

# Push to GitHub
echo "⬆️  Pushing to GitHub..."
git push -u origin main

echo "✅ Successfully pushed FlowPay to GitHub!"
echo "🔗 Repository: https://github.com/kabeermalhotra77/FlowPay_MVP_Final"
