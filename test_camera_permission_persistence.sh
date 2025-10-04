#!/bin/bash

# Test script for camera permission persistence
# This script tests that camera permission dialog only appears once

echo "🧪 Testing Camera Permission Persistence"
echo "======================================"

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

echo ""
echo "🎯 Test Steps:"
echo "1. Complete the setup process (should not request camera permission)"
echo "2. Click 'Scan QR Code' - camera permission dialog should appear"
echo "3. Grant camera permission"
echo "4. Close QR scanner and go back to main screen"
echo "5. Click 'Scan QR Code' again - NO dialog should appear, camera should start directly"
echo "6. Repeat step 5 multiple times - dialog should never appear again"
echo ""
echo "📋 Expected Behavior:"
echo "✅ Camera permission dialog appears only ONCE"
echo "✅ After granting permission, QR scanner opens directly"
echo "✅ No permission dialogs on subsequent QR scanner opens"
echo ""
echo "🔍 Check logs for camera permission requests..."
adb logcat -d | grep -i "camera.*permission\|QRScanner.*permission" | tail -10

echo ""
echo "✅ Test completed! Follow the manual steps above to verify the fix."
