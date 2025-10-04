#!/bin/bash

# Test script to manually trigger the dialog for testing
# This will help you test the dialog implementation

echo "🧪 FlowPay Dialog Test Trigger"
echo "=============================="
echo ""

echo "📱 Starting FlowPay app..."
adb shell am start -n com.flowpay.app/.MainActivity

echo ""
echo "⏳ Waiting 3 seconds for app to start..."
sleep 3

echo ""
echo "🔧 Triggering test dialog via ADB..."
# This will call the testShowDialog() method we added
adb shell am broadcast -a com.flowpay.app.TEST_DIALOG

echo ""
echo "✅ Test dialog should now appear on your phone!"
echo ""
echo "📊 Check the log output for dialog-related messages:"
echo "   - Look for 🚨 showCallDurationIssueDialog called!"
echo "   - Look for 🔔 Attempting to show dialog..."
echo "   - Look for ✅ Dialog created, showing now..."
echo "   - Look for ✅✅✅ DIALOG SHOULD BE VISIBLE NOW!"
echo ""
echo "If dialog fails, you should see fallback messages:"
echo "   - Look for ❌ Failed to show dialog"
echo "   - Look for 📢 Showing notification as fallback..."
echo ""
echo "Press Ctrl+C to stop log monitoring when done testing."



