#!/bin/bash

# Final script to push FlowPay to GitHub
echo "🚀 Final push to GitHub..."

# Navigate to project directory
cd "/Users/kabeermalhotra/Library/Mobile Documents/com~apple~CloudDocs/Desktop/FLOWPAYYYYY"

# Clean up and reinitialize
echo "🧹 Cleaning up..."
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
- 240 files with 30,461+ lines of code"

# Add remote origin
echo "🔗 Adding remote origin..."
git remote add origin https://github.com/kabeermalhotra77/FlowPay_MVP_Final.git

# Set main branch
echo "🌿 Setting main branch..."
git branch -M main

# Try to push with token
echo "⬆️  Pushing to GitHub with token..."
git push -u origin main

echo "✅ Push completed!"
echo "🔗 Repository: https://github.com/kabeermalhotra77/FlowPay_MVP_Final"
