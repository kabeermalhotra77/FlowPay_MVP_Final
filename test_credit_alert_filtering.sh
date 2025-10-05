#!/bin/bash

# Test script for Credit Alert Filtering Implementation
# This script helps verify that credit alerts and informational messages are properly filtered

echo "======================================"
echo "Credit Alert Filtering Test"
echo "======================================"
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Get package name
PACKAGE="com.flowpay.app"

echo "📱 Testing Credit Alert Filtering..."
echo ""

# Clear previous logs
adb logcat -c

echo "Test Setup:"
echo "1. The app must be running"
echo "2. SMS permissions must be granted"
echo "3. You need to manually:"
echo "   - Start a payment operation (QR or UPI123)"
echo "   - Send test SMS messages to verify filtering"
echo ""

echo "─────────────────────────────────────"
echo "Test Messages to Send:"
echo "─────────────────────────────────────"
echo ""

echo "${RED}❌ SHOULD BE FILTERED (Informational):${NC}"
echo ""
echo "1. Credit Alert:"
echo "   'HDFC-BANK: Credit alert: Rs 5000 credited to your account'"
echo ""
echo "2. Balance Alert:"
echo "   'ICICI: Balance alert: Your account balance is Rs 10000'"
echo ""
echo "3. Promotional:"
echo "   'SBI: Promotional offer: Get 20% cashback on next transaction'"
echo ""
echo "4. Cashback:"
echo "   'AXIS: Cashback alert: Rs 100 reward credited'"
echo ""
echo "5. Account Credited:"
echo "   'HDFC: Your account has been credited with Rs 25000'"
echo ""

echo "${GREEN}✅ SHOULD BE PROCESSED (Transactions):${NC}"
echo ""
echo "6. Debit Transaction:"
echo "   'HDFC-BANK: Rs 100 debited from your account to John Smith. Ref: ABC123'"
echo ""
echo "7. UPI Payment:"
echo "   'ICICI: Rs 500 sent to merchant@paytm via UPI. Txn ID: XYZ789'"
echo ""
echo "8. Successful Payment:"
echo "   'SBI: Payment of Rs 250 to 9876543210 successful'"
echo ""

echo "─────────────────────────────────────"
echo "Monitoring Logs..."
echo "─────────────────────────────────────"
echo ""

# Monitor relevant logs
adb logcat -v time | grep -E "TransactionDetector|SimpleSMSReceiver" | while read line; do
    # Highlight key filtering messages
    if echo "$line" | grep -q "Informational message detected"; then
        echo -e "${YELLOW}🔍 FILTER DETECTED:${NC} $line"
    elif echo "$line" | grep -q "Ignoring informational message"; then
        echo -e "${RED}❌ MESSAGE FILTERED:${NC} $line"
    elif echo "$line" | grep -q "Not an informational message"; then
        echo -e "${GREEN}✓ Passed filter check:${NC} $line"
    elif echo "$line" | grep -q "Transaction Detected"; then
        echo -e "${GREEN}✅ TRANSACTION PROCESSED:${NC} $line"
    elif echo "$line" | grep -q "Detected bank:"; then
        echo -e "${GREEN}🏦 Bank detected:${NC} $line"
    elif echo "$line" | grep -q "Not a transaction message"; then
        echo -e "${RED}⚠️  Not transaction:${NC} $line"
    else
        echo "$line"
    fi
done

# If the script is interrupted
trap "echo ''; echo 'Test monitoring stopped.'; exit 0" INT TERM

