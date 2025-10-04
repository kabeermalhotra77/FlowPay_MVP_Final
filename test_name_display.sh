#!/bin/bash

echo "Testing Name Display in Transaction History"
echo "=========================================="

echo "This script will help you test and verify that names are being displayed"
echo "instead of phone numbers in the transaction history."
echo ""

echo "Steps to test:"
echo "1. Build and install the updated app"
echo "2. Make a test payment or use the test transaction generator"
echo "3. Check the transaction history screen"
echo "4. Verify that names are displayed instead of phone numbers"
echo ""

echo "To check the logs for name extraction:"
echo "adb logcat | grep -E '(TransactionDetector|TransactionHistory)'"
echo ""

echo "To check what's stored in the database:"
echo "adb shell 'run-as com.flowpay.app find /data/data/com.flowpay.app -name \"*.db\" -exec sqlite3 {} \"SELECT transactionId, recipientName, phoneNumber, amount FROM transactions ORDER BY timestamp DESC LIMIT 5;\" \\;'"
echo ""

echo "Expected behavior:"
echo "- If recipientName is available, it should be displayed"
echo "- If recipientName is null but phoneNumber is available, phoneNumber should be displayed"
echo "- If both are null, 'Unknown' should be displayed"
echo ""

echo "Common SMS formats that should extract names:"
echo "- 'Debited Rs.100.00 to JOHN DOE via UPI'"
echo "- 'Amount Rs.500.00 paid to ABC STORE via UPI'"
echo "- 'UPI payment to MERCHANT NAME via UPI'"
echo "- 'Money transferred to RECIPIENT NAME via UPI'"
echo ""

echo "If names are still not showing:"
echo "1. Check the logs for extraction details"
echo "2. Verify the SMS format matches our patterns"
echo "3. Check if the database has the correct schema"
echo "4. Ensure the app is using the updated code"


