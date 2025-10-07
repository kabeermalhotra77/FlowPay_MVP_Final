#!/bin/bash

# Test TransactionDetector Fix
# This script tests if the TransactionDetector properly starts and detects operations

echo "🧪 Testing TransactionDetector Fix"
echo "=================================="

# Clear previous logs
echo "📱 Clearing previous logs..."
adb logcat -c

echo ""
echo "🔍 Monitoring logs for TransactionDetector operations..."
echo "   - Look for 'startOperation' logs"
echo "   - Look for 'shouldProcessSMS' logs"
echo "   - Look for 'Active operation' logs"
echo ""
echo "📋 Test Steps:"
echo "   1. Start a UPI 123 transfer in the app"
echo "   2. Verify TransactionDetector.startOperation is called"
echo "   3. Verify shouldProcessSMS returns true"
echo "   4. Send a test SMS and verify it's processed"
echo ""
echo "⏱️  Monitoring for 30 seconds..."
echo "   Press Ctrl+C to stop monitoring"
echo ""

# Monitor logs for TransactionDetector
adb logcat | grep -E "(TransactionDetector|startOperation|shouldProcessSMS|Active operation|No active operation)" --line-buffered

echo ""
echo "✅ Test monitoring completed"
echo "📊 Check the logs above to verify:"
echo "   - 'Starting operation: UPI_123' appears"
echo "   - 'Active operation: UPI_123' appears"
echo "   - 'shouldProcessSMS' returns true"
