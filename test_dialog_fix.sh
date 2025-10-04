#!/bin/bash

# Test script for Dialog Display Solution
# This script helps test the robust dialog implementation

echo "🔧 FlowPay Dialog Fix Test Script"
echo "================================="
echo ""

# Build the app
echo "📦 Building the app..."
./gradlew clean assembleDebug

if [ $? -eq 0 ]; then
    echo "✅ Build successful!"
    echo ""
    
    # Install the app
    echo "📱 Installing the app..."
    adb install -r app/build/outputs/apk/debug/app-debug.apk
    
    if [ $? -eq 0 ]; then
        echo "✅ Installation successful!"
        echo ""
        
        # Clear logs
        echo "🧹 Clearing previous logs..."
        adb logcat -c
        
        # Start monitoring logs
        echo "📊 Starting log monitoring..."
        echo "   - Look for dialog-related logs with 'MainActivity' or 'CallDuration'"
        echo "   - Test by making a call that ends in < 25 seconds"
        echo "   - You should see multiple fallback mechanisms in action"
        echo ""
        echo "🔍 Monitoring logs (press Ctrl+C to stop)..."
        echo "================================================"
        
        adb logcat | grep -E "MainActivity|CallDuration|Dialog|Payment.*Issue"
        
    else
        echo "❌ Installation failed!"
        exit 1
    fi
else
    echo "❌ Build failed!"
    exit 1
fi



