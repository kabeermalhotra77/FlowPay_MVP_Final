#!/bin/bash

# USSD Overlay Debugging Script
echo "=== USSD Overlay Debugging ==="
echo "This script will monitor the USSD overlay system"
echo ""

# Clear logcat
adb logcat -c
echo "✓ Logcat cleared"

echo ""
echo "Monitoring logs... Press Ctrl+C to stop"
echo "Now scan a QR code to trigger the USSD overlay"
echo ""

# Monitor specific logs
adb logcat | grep -E "USSDOverlay|QRScanner|WindowManager|Permission" --color=always

