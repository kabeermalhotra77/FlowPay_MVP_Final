#!/bin/bash

# Test script for camera permission flow
# This script tests that camera permission is only requested when user tries to scan QR codes

echo "🧪 Testing Camera Permission Flow"
echo "================================="

# Build the app
echo "📱 Building the app..."
./gradlew assembleDebug

if [ $? -eq 0 ]; then
    echo "✅ App built successfully"
else
    echo "❌ App build failed"
    exit 1
fi

# Install the app
echo "📲 Installing the app..."
adb install -r app/build/outputs/apk/debug/app-debug.apk

if [ $? -eq 0 ]; then
    echo "✅ App installed successfully"
else
    echo "❌ App installation failed"
    exit 1
fi

# Clear app data to start fresh
echo "🧹 Clearing app data..."
adb shell pm clear com.flowpay.app

# Start the app
echo "🚀 Starting the app..."
adb shell am start -n com.flowpay.app/.MainActivity

# Wait for app to start
sleep 3

# Check if setup screen appears (should not request camera permission)
echo "🔍 Checking setup screen..."
adb shell dumpsys activity activities | grep -i "setup\|camera\|permission"

# Wait a bit more
sleep 2

# Check logs for camera permission requests during setup
echo "📋 Checking logs for camera permission requests during setup..."
adb logcat -d | grep -i "camera\|permission" | tail -10

echo ""
echo "🎯 Test Summary:"
echo "1. App should start with setup screen"
echo "2. No camera permission should be requested during setup"
echo "3. Camera permission should only be requested when user clicks 'Scan QR Code'"
echo ""
echo "📝 Manual Test Steps:"
echo "1. Complete the setup process"
echo "2. Click on 'Scan QR Code' button"
echo "3. Verify that camera permission dialog appears"
echo "4. Grant permission and verify camera starts"
echo ""
echo "✅ Test completed! Check the logs above for any unexpected camera permission requests during setup."
