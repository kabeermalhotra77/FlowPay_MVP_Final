#!/bin/bash

echo "Testing Transaction History with Names"
echo "====================================="

echo "This script will help you test the name extraction functionality."
echo ""
echo "Steps to test:"
echo "1. Build and install the app with debug logging enabled"
echo "2. Use the test transaction generator to create transactions with names"
echo "3. Check the transaction history to see if names are displayed"
echo "4. Check the logs for extraction details"
echo ""

echo "To generate test transactions with names, you can use the TestTransactionGenerator"
echo "in the app, or manually create transactions through the UI."
echo ""

echo "To check the logs for name extraction:"
echo "adb logcat | grep -E '(TransactionDetector|TransactionHistory)'"
echo ""

echo "To check what's stored in the database:"
echo "adb shell 'run-as com.flowpay.app find /data/data/com.flowpay.app -name \"*.db\" -exec sqlite3 {} \"SELECT transactionId, recipientName, phoneNumber, amount FROM transactions LIMIT 5;\" \\;'"


