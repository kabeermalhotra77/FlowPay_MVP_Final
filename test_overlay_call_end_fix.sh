#!/bin/bash

# Test script for overlay call end fix
# This script tests the UPI123 call overlay dismissal fix

echo "=== Testing Overlay Call End Fix ==="
echo "This test will:"
echo "1. Start a UPI123 call"
echo "2. Monitor logs for call state changes"
echo "3. Verify overlay is dismissed when call ends"
echo ""

# Clear previous logs
adb logcat -c

echo "Starting log monitoring for overlay call end fix..."
echo "Please initiate a UPI123 call manually and let it end naturally"
echo ""

# Monitor logs for the specific issue
adb logcat -s CallStateMonitor CallOverlayService OverlayDebug CallManager | while read line; do
    echo "$line"
    
    # Check for call end detection
    if echo "$line" | grep -q "Call state changed: IDLE"; then
        echo "✅ CALL END DETECTED: $line"
    fi
    
    # Check for overlay dismissal
    if echo "$line" | grep -q "Call ended with reason"; then
        echo "✅ OVERLAY DISMISSAL TRIGGERED: $line"
    fi
    
    # Check for service stop
    if echo "$line" | grep -q "Stopping service"; then
        echo "✅ SERVICE STOPPED: $line"
    fi
    
    # Check for blank phone number handling
    if echo "$line" | grep -q "Blank phone number detected"; then
        echo "✅ BLANK PHONE NUMBER HANDLED: $line"
    fi
    
    # Check for UPI call context detection
    if echo "$line" | grep -q "checking if this is a UPI123 call context"; then
        echo "✅ UPI CALL CONTEXT DETECTED: $line"
    fi
done
