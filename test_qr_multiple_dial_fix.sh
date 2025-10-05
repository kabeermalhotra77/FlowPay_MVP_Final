#!/bin/bash

# Test script to verify QR code multiple dial fix
# This script tests that scanning a QR code only dials *99*1*3# once

echo "=== Testing QR Code Multiple Dial Fix ==="
echo "This test verifies that scanning a QR code only dials *99*1*3# once"
echo ""

# Check if device is connected
if ! adb devices | grep -q "device$"; then
    echo "❌ No device connected. Please connect your device and enable USB debugging."
    exit 1
fi

echo "✅ Device connected"

# Clear logs
echo "Clearing previous logs..."
adb logcat -c

echo ""
echo "=== Test Instructions ==="
echo "1. The app will be launched"
echo "2. Navigate to QR scanner"
echo "3. Scan a QR code"
echo "4. Check logs for multiple *99*1*3# dials"
echo "5. The fix should prevent multiple dials"
echo ""

# Launch the app
echo "Launching FlowPay app..."
adb shell am start -n com.flowpay.app/.MainActivity

# Wait for app to start
sleep 3

echo "App launched. Please navigate to QR scanner and scan a QR code."
echo "Monitoring logs for USSD dialing..."

# Monitor logs for USSD dialing
echo ""
echo "=== Monitoring Logs ==="
echo "Looking for 'Dialing USSD code: *99*1*3#' messages..."
echo "Press Ctrl+C to stop monitoring"
echo ""

# Count USSD dial attempts
ussd_count=0
lifecycle_fix_shown=0
while true; do
    # Check for USSD dial logs
    ussd_logs=$(adb logcat -d | grep "Preparing to dial USSD: \*99\*1\*3#" | wc -l)
    
    if [ "$ussd_logs" -gt "$ussd_count" ]; then
        ussd_count=$ussd_logs
        echo "🔍 USSD dial detected (count: $ussd_count)"
        
        if [ "$ussd_count" -gt 1 ]; then
            echo "❌ MULTIPLE USSD DIALS DETECTED! Fix may not be working properly."
            echo "Expected: 1 dial, Found: $ussd_count dials"
        else
            echo "✅ Single USSD dial detected - fix appears to be working"
        fi
    fi
    
    # Check for processing flag logs
    processing_logs=$(adb logcat -d | grep "Already processing QR code, ignoring duplicate detection" | wc -l)
    if [ "$processing_logs" -gt 0 ]; then
        echo "✅ Duplicate detection prevention working (blocked $processing_logs duplicate attempts)"
    fi
    
    # Check for lifecycle fix logs
    if [ "$lifecycle_fix_shown" -eq 0 ]; then
        lifecycle_logs=$(adb logcat -d | grep "USSD process active - keeping processing flag to prevent re-dial" | wc -l)
        if [ "$lifecycle_logs" -gt 0 ]; then
            echo "✅ Lifecycle fix working - processing flag preserved during USSD process"
            lifecycle_fix_shown=1
        fi
        
        no_restart_logs=$(adb logcat -d | grep "Not restarting camera.*isUSSDProcessActive: true" | wc -l)
        if [ "$no_restart_logs" -gt 0 ]; then
            echo "✅ Camera analyzer correctly not restarting during USSD process"
        fi
    fi
    
    sleep 1
done
