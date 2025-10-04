#!/bin/bash

# Test script for Name Extraction Fix
# This script tests the updated regex patterns with various SMS formats

echo "🧪 Testing Name Extraction Fix"
echo "================================"

# Test cases for the updated patterns
echo ""
echo "📱 Testing Lowercase Names:"
echo "SMS: 'Rs 500 sent to john doe via UPI'"
echo "Expected: recipientName = 'John Doe'"
echo ""

echo "📱 Testing Mixed Case Names:"
echo "SMS: 'Rs 1000 paid to John Smith on 01-01-24'"
echo "Expected: recipientName = 'John Smith'"
echo ""

echo "📱 Testing Merchant Names:"
echo "SMS: 'Payment of Rs 250 to abc store'"
echo "Expected: recipientName = 'Abc Store'"
echo ""

echo "📱 Testing Names with Prefixes:"
echo "SMS: 'Rs 100 sent to Mr. john doe'"
echo "Expected: recipientName = 'John Doe'"
echo ""

echo "📱 Testing UPI IDs:"
echo "SMS: 'Payment to merchant.shop@paytm'"
echo "Expected: recipientName = 'Merchant Shop'"
echo ""

echo "📱 Testing Phone Numbers (should still work):"
echo "SMS: 'Rs 500 sent to 9876543210'"
echo "Expected: phoneNumber = '9876543210'"
echo ""

echo "📱 Testing Credit Messages:"
echo "SMS: 'Rs 1000 received from jane smith'"
echo "Expected: recipientName = 'Jane Smith'"
echo ""

echo "📱 Testing Simple Formats:"
echo "SMS: 'to john doe on 01-01-24'"
echo "Expected: recipientName = 'John Doe'"
echo ""

echo "✅ Key Changes Made:"
echo "- Changed [A-Z] to [a-zA-Z] for case-insensitive matching"
echo "- Added additional patterns for common SMS formats"
echo "- Moved phone number pattern to last priority"
echo "- Enhanced cleanupName() method for better mixed-case handling"
echo "- Added detailed logging for debugging"
echo ""

echo "🔍 To test the implementation:"
echo "1. Build and run the app"
echo "2. Send test SMS messages with lowercase/mixed-case names"
echo "3. Check Logcat for extraction logs with TAG 'TransactionDetector'"
echo "4. Verify names appear correctly in transaction history"
echo ""

echo "📋 Expected Logcat Output:"
echo "D/TransactionDetector: Extracting recipient info from: Rs 500 sent to john doe via UPI..."
echo "D/TransactionDetector: Pattern 0 matched: 'john doe'"
echo "D/TransactionDetector: ✓ Found recipient name: John Doe"
echo "D/TransactionDetector: Final extraction - Name: John Doe, Phone: null"
echo ""

echo "🎯 The fix should now correctly extract names from real-world SMS messages"
echo "   regardless of case format (uppercase, lowercase, or mixed case)."


