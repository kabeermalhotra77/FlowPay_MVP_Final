#!/bin/bash

echo "=== QR Scanner Button Debug Script ==="
echo "This script will help debug the QR scanner button issue."
echo ""

# Install the app
echo "1. Installing the updated app..."
adb install -r app/build/outputs/apk/debug/app-debug.apk
if [ $? -eq 0 ]; then
    echo "✅ App installed successfully"
else
    echo "❌ App installation failed"
    exit 1
fi

echo ""
echo "2. Starting the app..."
adb shell am start -n com.flowpay.app/.MainActivity

echo ""
echo "3. Monitoring logs for QR scanner button clicks..."
echo "   - Click the QR scanner button in the app"
echo "   - Watch for debug logs below"
echo "   - Press Ctrl+C to stop monitoring"
echo ""

# Monitor logs for QR scanner related events
adb logcat -s "FlowPay" "QRScanner" "AndroidRuntime" | grep -E "(QR|scan|button|click|startQRScanning|QRScannerActivity)"
