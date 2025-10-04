#!/bin/bash

echo "🧪 Testing Vertical Button Layout Redesign"
echo "=========================================="

# Navigate to project directory
cd "/Users/kabeermalhotra/Library/Mobile Documents/com~apple~CloudDocs/Desktop/FLOWPAYYYYY"

echo "📱 Building and testing the app with new vertical button layout..."

# Clean and build the project
echo "🔨 Cleaning project..."
./gradlew clean

echo "🔨 Building project..."
./gradlew assembleDebug

if [ $? -eq 0 ]; then
    echo "✅ Build successful!"
    echo ""
    echo "🎨 New Button Layout Features:"
    echo "   • QR Scan button positioned at the top"
    echo "   • Pay Contact button positioned below"
    echo "   • Thin 'OR' divider line between buttons"
    echo "   • Enhanced button design with:"
    echo "     - Larger size (100dp instead of 80dp)"
    echo "     - Better shadows (8dp elevation)"
    echo "     - Rounded corners (20dp radius)"
    echo "     - Different colors (blue for QR, green for contact)"
    echo "     - Larger icons (28dp instead of 24dp)"
    echo "     - Better typography (16sp, SemiBold)"
    echo ""
    echo "🚀 Install and test the app to see the new layout!"
    echo "   adb install app/build/outputs/apk/debug/app-debug.apk"
else
    echo "❌ Build failed! Please check for errors."
    exit 1
fi

echo ""
echo "✨ Vertical button layout redesign complete!"
