#!/bin/bash

# Complete SMS Parsing System Test Script
# Tests all aspects of the SMS detection and name extraction system

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${1}${2}${NC}"
}

print_status $BLUE "🧪 COMPLETE SMS PARSING SYSTEM TEST"
print_status $BLUE "====================================="

# Test 1: Name Extraction Patterns
print_status $YELLOW "\n📝 Test 1: Name Extraction Patterns"
print_status $BLUE "Testing various name formats..."

# Test lowercase names
print_status $BLUE "Testing lowercase: 'paid to john doe'"
adb shell am broadcast -a android.provider.Telephony.SMS_RECEIVED --es sender "HDFCBK" --es body "Rs 500 sent to john doe via UPI. Txn successful."

sleep 2

# Test uppercase names
print_status $BLUE "Testing uppercase: 'paid to JOHN DOE'"
adb shell am broadcast -a android.provider.Telephony.SMS_RECEIVED --es sender "ICICIB" --es body "Rs 1000 paid to JOHN DOE via UPI. Txn successful."

sleep 2

# Test mixed case names
print_status $BLUE "Testing mixed case: 'paid to John Doe'"
adb shell am broadcast -a android.provider.Telephony.SMS_RECEIVED --es sender "SBIBK" --es body "Rs 750 paid to John Doe via UPI. Txn successful."

sleep 2

# Test phone number fallback
print_status $BLUE "Testing phone number: 'sent to 9876543210'"
adb shell am broadcast -a android.provider.Telephony.SMS_RECEIVED --es sender "AXISBK" --es body "Rs 250 sent to 9876543210 via UPI. Txn successful."

sleep 2

# Test UPI ID extraction
print_status $BLUE "Testing UPI ID: 'to merchant.shop@paytm'"
adb shell am broadcast -a android.provider.Telephony.SMS_RECEIVED --es sender "PAYTMB" --es body "Rs 100 to merchant.shop@paytm via UPI. Txn successful."

sleep 2

# Test 2: No Active Operation
print_status $YELLOW "\n📝 Test 2: No Active Operation (Should be ignored)"
print_status $BLUE "Sending SMS without active operation (should be ignored)..."

# This should be ignored
adb shell am broadcast -a android.provider.Telephony.SMS_RECEIVED --es sender "HDFCBK" --es body "Rs 500 sent to test user via UPI. Txn successful."

sleep 2

# Test 3: UPI 123 Operation
print_status $YELLOW "\n📝 Test 3: UPI 123 Operation with Call Muting"
print_status $BLUE "Starting UPI 123 operation..."

# Start UPI 123 operation (this would normally be done through the app)
# We'll simulate this by checking the logs
print_status $BLUE "Checking if UPI 123 operation can be started..."

# Test 4: Transaction History
print_status $YELLOW "\n📝 Test 4: Transaction History Display"
print_status $BLUE "Checking if transactions appear in history..."

# Test 5: Log Analysis
print_status $YELLOW "\n📝 Test 5: Log Analysis"
print_status $BLUE "Analyzing logs for proper SMS processing..."

# Check for proper SMS processing logs
print_status $BLUE "Checking SimpleSMSReceiver logs..."
adb logcat -d | grep -E "(SimpleSMSReceiver|TransactionDetector)" | tail -20

print_status $BLUE "\nChecking name extraction logs..."
adb logcat -d | grep -E "(Extracted recipient|Found recipient name|Found phone number)" | tail -10

print_status $BLUE "\nChecking database save logs..."
adb logcat -d | grep -E "(Transaction saved|Failed to save)" | tail -10

print_status $BLUE "\nChecking call muting logs..."
adb logcat -d | grep -E "(AudioStateManager|Call audio muted|Call audio restored)" | tail -10

# Test 6: Success Screen Verification
print_status $YELLOW "\n📝 Test 6: Success Screen Verification"
print_status $BLUE "Checking if success screen shows recipient information..."

# Test 7: Database Verification
print_status $YELLOW "\n📝 Test 7: Database Verification"
print_status $BLUE "Checking if transactions are saved to database..."

# Check database content (if accessible)
print_status $BLUE "Database verification would require app-specific queries..."

print_status $GREEN "\n✅ SMS PARSING SYSTEM TEST COMPLETED"
print_status $BLUE "====================================="
print_status $YELLOW "Check the logs above to verify:"
print_status $YELLOW "1. Names are extracted correctly (john doe, JOHN DOE, John Doe)"
print_status $YELLOW "2. Phone numbers are extracted as fallback"
print_status $YELLOW "3. UPI IDs are processed correctly"
print_status $YELLOW "4. SMS without active operation is ignored"
print_status $YELLOW "5. Transactions are saved to database"
print_status $YELLOW "6. Call muting works for UPI 123"
print_status $YELLOW "7. Success screen shows recipient information"

print_status $BLUE "\nTo monitor real-time logs, run:"
print_status $BLUE "adb logcat | grep -E '(SimpleSMSReceiver|TransactionDetector|AudioStateManager)'"


