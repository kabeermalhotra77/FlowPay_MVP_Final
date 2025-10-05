#!/bin/bash

# Test script for dialog visibility fix
# This script tests that dialogs appear on top when follow-up calls cover the screen

echo "=== Testing Dialog Visibility Fix ==="
echo "This test will:"
echo "1. Start a UPI123 call"
echo "2. Monitor logs for dialog visibility improvements"
echo "3. Verify app comes to foreground when dialog appears"
echo ""

# Clear previous logs
adb logcat -c

echo "Starting log monitoring for dialog visibility fix..."
echo "Please initiate a UPI123 call manually and let it end naturally"
echo ""

# Monitor logs for the specific issue
adb logcat -s TransactionDialogManager CallOverlayService CallStateMonitor OverlayDebug | while read line; do
    echo "$line"
    
    # Check for app foregrounding
    if echo "$line" | grep -q "Bringing app to foreground for dialog visibility"; then
        echo "✅ APP FOREGROUNDING TRIGGERED: $line"
    fi
    
    # Check for dialog creation with window flags
    if echo "$line" | grep -q "Set window flags to ensure dialog appears on top"; then
        echo "✅ DIALOG WINDOW FLAGS SET: $line"
    fi
    
    # Check for dialog showing
    if echo "$line" | grep -q "Showing transaction completed dialog"; then
        echo "✅ TRANSACTION COMPLETED DIALOG SHOWN: $line"
    fi
    
    # Check for dialog showing
    if echo "$line" | grep -q "Showing transaction failed dialog"; then
        echo "✅ TRANSACTION FAILED DIALOG SHOWN: $line"
    fi
    
    # Check for dialog showing
    if echo "$line" | grep -q "Showing transaction cancelled dialog"; then
        echo "✅ TRANSACTION CANCELLED DIALOG SHOWN: $line"
    fi
    
    # Check for app brought to foreground
    if echo "$line" | grep -q "App brought to foreground successfully"; then
        echo "✅ APP SUCCESSFULLY BROUGHT TO FOREGROUND: $line"
    fi
    
    # Check for call end detection
    if echo "$line" | grep -q "Call ended with reason"; then
        echo "✅ CALL END DETECTED: $line"
    fi
    
    # Check for overlay dismissal
    if echo "$line" | grep -q "Hide overlay first"; then
        echo "✅ OVERLAY DISMISSAL TRIGGERED: $line"
    fi
done
