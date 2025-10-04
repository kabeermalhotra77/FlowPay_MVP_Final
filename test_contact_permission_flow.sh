#!/bin/bash

# Test script for contact permission flow
# This script tests that contact permission is only requested when user clicks contact icon

echo "🧪 Testing Contact Permission Flow"
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

# Check if setup screen appears (should not request contact permission)
echo "🔍 Checking setup screen..."
adb shell dumpsys activity activities | grep -i "setup\|contact\|permission"

# Wait a bit more
sleep 2

# Check logs for contact permission requests during setup
echo "📋 Checking logs for contact permission requests during setup..."
adb logcat -d | grep -i "contact\|permission" | tail -10

echo ""
echo "🎯 Test Summary:"
echo "1. App should start with setup screen"
echo "2. No contact permission should be requested during setup"
echo "3. Contact permission should only be requested when user clicks contact icon in PayContactDialog"
echo ""
echo "📝 Manual Test Steps:"
echo "1. Complete the setup process"
echo "2. Click on 'Pay Contact' button"
echo "3. In the PayContactDialog, click on the contact icon (person icon)"
echo "4. Verify that contact permission dialog appears"
echo "5. Grant permission and verify contact picker opens"
echo ""
echo "✅ Test completed! Check the logs above for any unexpected contact permission requests during setup."
