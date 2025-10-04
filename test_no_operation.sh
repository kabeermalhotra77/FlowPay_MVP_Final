#!/bin/bash

# No Operation Test Script
# Tests that SMS is ignored when no active payment operation

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_status() {
    echo -e "${1}${2}${NC}"
}

print_status $BLUE "🧪 NO OPERATION TEST"
print_status $BLUE "===================="

print_status $YELLOW "\n📝 Testing SMS Ignored When No Active Operation"
print_status $BLUE "This test verifies that SMS is ignored when no payment operation is active"

# Clear previous logs
print_status $BLUE "Clearing previous logs..."
adb logcat -c

print_status $BLUE "\nSending SMS without active operation..."
print_status $YELLOW "Expected: SMS should be ignored with 'No active payment operation' message"

# Send SMS without starting any operation
adb shell am broadcast -a android.provider.Telephony.SMS_RECEIVED --es sender "HDFCBK" --es body "Rs 500 sent to John Doe via UPI. Txn successful."

sleep 3

print_status $BLUE "\nChecking logs for 'No active payment operation' message..."
NO_OPERATION_LOG=$(adb logcat -d | grep -E "No active payment operation" | tail -1)

if [ -n "$NO_OPERATION_LOG" ]; then
    print_status $GREEN "✅ SUCCESS: SMS correctly ignored - No active operation"
    print_status $BLUE "Log: $NO_OPERATION_LOG"
else
    print_status $RED "❌ FAILED: SMS was processed when it should have been ignored"
    print_status $YELLOW "Checking what happened instead..."
    adb logcat -d | grep -E "(SimpleSMSReceiver|TransactionDetector)" | tail -10
fi

print_status $BLUE "\nSending another SMS to verify consistency..."
adb shell am broadcast -a android.provider.Telephony.SMS_RECEIVED --es sender "ICICIB" --es body "Rs 1000 paid to Jane Smith via UPI. Txn successful."

sleep 3

print_status $BLUE "\nChecking logs again..."
NO_OPERATION_LOG2=$(adb logcat -d | grep -E "No active payment operation" | tail -1)

if [ -n "$NO_OPERATION_LOG2" ]; then
    print_status $GREEN "✅ SUCCESS: Second SMS also correctly ignored"
    print_status $BLUE "Log: $NO_OPERATION_LOG2"
else
    print_status $RED "❌ FAILED: Second SMS was processed when it should have been ignored"
fi

print_status $BLUE "\nChecking for any database saves (should be none)..."
DB_SAVE_LOG=$(adb logcat -d | grep -E "Transaction saved" | tail -1)

if [ -n "$DB_SAVE_LOG" ]; then
    print_status $RED "❌ FAILED: Transaction was saved to database when it should have been ignored"
    print_status $BLUE "Log: $DB_SAVE_LOG"
else
    print_status $GREEN "✅ SUCCESS: No transactions saved to database (correct behavior)"
fi

print_status $GREEN "\n✅ NO OPERATION TEST COMPLETED"
print_status $BLUE "============================="
print_status $YELLOW "Summary:"
print_status $YELLOW "1. SMS without active operation should be ignored"
print_status $YELLOW "2. Log should show 'No active payment operation, ignoring SMS'"
print_status $YELLOW "3. No transactions should be saved to database"
print_status $YELLOW "4. No success screen should be shown"

print_status $BLUE "\nTo monitor real-time logs:"
print_status $BLUE "adb logcat | grep -E '(SimpleSMSReceiver|No active payment operation)'"


