#!/bin/bash

# FlowPay Overlay Debugging Script
# This script helps debug why the overlay isn't working

echo "🔍 FlowPay Overlay Debugging Script"
echo "=================================="
echo ""

# Check if adb is available
if ! command -v adb &> /dev/null; then
    echo "❌ ADB not found. Please install Android SDK and add adb to PATH"
    exit 1
fi

# Check if device is connected
if ! adb devices | grep -q "device$"; then
    echo "❌ No Android device connected. Please connect your device and enable USB debugging"
    exit 1
fi

echo "✅ Android device connected"
echo ""

# Clear previous logs
echo "🧹 Clearing previous logs..."
adb logcat -c

echo "📱 Starting FlowPay app..."
adb shell am start -n com.flowpay.app/.MainActivity

echo "⏳ Waiting for app to start..."
sleep 3

echo ""
echo "🔍 Monitoring logs for overlay debugging..."
echo "Press Ctrl+C to stop monitoring"
echo ""

# Monitor logs for overlay-related messages
adb logcat | grep -E "OverlayDebug|CallOverlay|FlowPay|MainActivityHelper|CallOverlayService" --line-buffered
