#!/bin/bash

echo "=== FlowPay Overlay Log Monitor ==="
echo "Monitoring CallOverlay and USSD Overlay services..."
echo "Press Ctrl+C to stop monitoring"
echo ""

# Clear existing logs
adb logcat -c

# Monitor logs with filtering for overlay-related events
adb logcat | grep -E "(CallOverlayService|USSDOverlayService|OverlayDebug|FlowPay|CallOverlay|USSD|Overlay|initiateTransfer|showOverlay|onCallDetected)" --line-buffered | while read line; do
    timestamp=$(date '+%H:%M:%S')
    echo "[$timestamp] $line"
done
