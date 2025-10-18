#!/bin/bash

# Test script for UPI123 Unavailable Dialog
# This script tests the manual payment entry feature when UPI123 is not set up

echo "🧪 Testing UPI123 Unavailable Dialog Implementation"
echo "================================================="

# Build the app
echo "📱 Building the app..."
./gradlew assembleDebug

if [ $? -eq 0 ]; then
    echo "✅ Build successful"
else
    echo "❌ Build failed"
    exit 1
fi

echo ""
echo "🔍 Test Scenarios:"
echo "1. User has NOT completed UPI123 setup"
echo "   - Expected: Manual payment entry should show 'UPI123 Unavailable Dialog'"
echo "   - Dialog should explain UPI123 is not set up"
echo "   - Dialog should provide instructions on how to set it up"
echo ""
echo "2. User has completed UPI123 setup"
echo "   - Expected: Manual payment entry should work normally"
echo "   - Should proceed with UPI123 call flow if permissions are granted"
echo ""
echo "📋 Manual Testing Steps:"
echo "1. Install the app on a device/emulator"
echo "2. Complete the initial setup but skip UPI123 test configuration"
echo "3. Go to main screen and tap 'Pay Contact' button"
echo "4. Enter phone number and amount, then tap 'Pay'"
echo "5. Verify that the 'UPI123 Unavailable Dialog' appears"
echo "6. Check dialog content matches requirements:"
echo "   - Title: 'Manual Payment Not Available'"
echo "   - Message: 'UPI123 is not set up. Currently this feature is not available for you.'"
echo "   - Instructions on how to set up UPI123"
echo "   - Suggestion to use QR scanning instead (if *99# is set up)"
echo ""
echo "7. Complete UPI123 setup in Test Configuration"
echo "8. Go back to main screen and try manual payment entry again"
echo "9. Verify that UPI123 call flow proceeds (if permissions granted)"
echo ""
echo "🔄 Cross-Feature Testing:"
echo "10. Test with both *99# and UPI123 not set up:"
echo "    - QR scan should show QR unavailable dialog"
echo "    - Manual payment should show UPI123 unavailable dialog"
echo ""
echo "11. Test with only *99# set up:"
echo "    - QR scan should work"
echo "    - Manual payment should show UPI123 unavailable dialog"
echo ""
echo "12. Test with only UPI123 set up:"
echo "    - QR scan should show QR unavailable dialog"
echo "    - Manual payment should work"
echo ""
echo "13. Test with both set up:"
echo "    - Both features should work normally"
echo ""
echo "✅ Test completed - check the app behavior manually"
