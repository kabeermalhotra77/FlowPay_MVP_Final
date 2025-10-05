#!/bin/bash

echo "📱 Testing UPI123 Dialog Fix on Android Device"
echo "=============================================="

# Check if device is connected
echo "🔍 Checking device connection..."
if adb devices | grep -q "device$"; then
    echo "✅ Android device connected"
    DEVICE_ID=$(adb devices | grep "device$" | head -1 | cut -f1)
    echo "📱 Device ID: $DEVICE_ID"
else
    echo "❌ No Android device connected"
    echo "Please connect your Android device and enable USB debugging"
    exit 1
fi

echo ""
echo "🚀 Starting FlowPay app..."
adb shell am start -n com.flowpay.app/.MainActivity

echo ""
echo "📋 Test Instructions:"
echo "1. Navigate to the Test Configuration screen"
echo "2. Press the 'UPI123' test button"
echo "3. Wait for the call to dial and end"
echo "4. Verify that the dialog appears without crashing"
echo "5. Check that the app doesn't reset/restart"

echo ""
echo "🔍 Monitoring app logs for UPI123 dialog..."
echo "Press Ctrl+C to stop monitoring"

# Monitor logs for UPI123 dialog related events
adb logcat -s "TestConfigurationHelper" "TestConfigurationActivity" "CallManager" | grep -E "(UPI123|dialog|crash|error|exception)"
