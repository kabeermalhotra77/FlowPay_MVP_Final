#!/bin/bash

# Test SMS Call Ending Fix
# This script tests if the SMS detection and call ending functionality works properly

echo "🧪 Testing SMS Call Ending Fix"
echo "================================"

# Clear previous logs
echo "📱 Clearing previous logs..."
adb logcat -c

echo ""
echo "🔍 Monitoring logs for SMS detection and call ending..."
echo "   - Look for 'TransactionDetector' logs"
echo "   - Look for 'SimpleSMSReceiver' logs"
echo "   - Look for 'CallManager' logs"
echo "   - Look for 'terminateCall' logs"
echo ""
echo "📋 Test Steps:"
echo "   1. Start a UPI 123 transfer in the app"
echo "   2. Wait for the call to be initiated"
echo "   3. Send a test SMS with transaction details"
echo "   4. Verify the call is automatically ended"
echo ""
echo "⏱️  Monitoring for 60 seconds..."
echo "   Press Ctrl+C to stop monitoring"
echo ""

# Monitor logs for SMS detection and call ending
adb logcat | grep -E "(TransactionDetector|SimpleSMSReceiver|CallManager|terminateCall|SMS.*Received|Transaction.*Detected|Call.*ended|startOperation|shouldProcessSMS)" --line-buffered

echo ""
echo "✅ Test monitoring completed"
echo "📊 Check the logs above to verify:"
echo "   - TransactionDetector.startOperation was called"
echo "   - shouldProcessSMS returned true"
echo "   - SMS was processed and call was terminated"
