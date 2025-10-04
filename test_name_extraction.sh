#!/bin/bash

# Name Extraction Test Script
# Tests the name extraction patterns in TransactionDetector

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

print_status $BLUE "🧪 NAME EXTRACTION TEST"
print_status $BLUE "======================="

# Function to test name extraction
test_name_extraction() {
    local test_name="$1"
    local sms_body="$2"
    local expected_pattern="$3"
    
    print_status $YELLOW "\nTesting: $test_name"
    print_status $BLUE "SMS: $sms_body"
    
    # Start an operation first
    print_status $BLUE "Starting UPI 123 operation..."
    # This would normally be done through the app UI
    
    # Send the test SMS
    adb shell am broadcast -a android.provider.Telephony.SMS_RECEIVED --es sender "HDFCBK" --es body "$sms_body"
    
    sleep 3
    
    # Check logs for extraction
    print_status $BLUE "Checking extraction logs..."
    adb logcat -d | grep -E "(Extracted recipient|Found recipient name|Found phone number)" | tail -5
}

# Test cases for name extraction
print_status $YELLOW "\n📝 Test Cases for Name Extraction"

# Test 1: Lowercase names
test_name_extraction "Lowercase: john doe" "Rs 500 sent to john doe via UPI. Txn successful." "john doe"

# Test 2: Uppercase names  
test_name_extraction "Uppercase: JOHN DOE" "Rs 1000 paid to JOHN DOE via UPI. Txn successful." "JOHN DOE"

# Test 3: Mixed case names
test_name_extraction "Mixed case: John Doe" "Rs 750 transferred to John Doe via UPI. Txn successful." "John Doe"

# Test 4: Names with special characters
test_name_extraction "Special chars: M/s ABC Store" "Rs 200 paid to M/s ABC Store via UPI. Txn successful." "ABC Store"

# Test 5: Phone number fallback
test_name_extraction "Phone number: 9876543210" "Rs 300 sent to 9876543210 via UPI. Txn successful." "9876543210"

# Test 6: UPI ID extraction
test_name_extraction "UPI ID: merchant.shop@paytm" "Rs 150 to merchant.shop@paytm via UPI. Txn successful." "Merchant Shop"

# Test 7: Multiple words
test_name_extraction "Multiple words: John Smith Jr" "Rs 400 paid to John Smith Jr via UPI. Txn successful." "John Smith Jr"

# Test 8: Names with dots
test_name_extraction "Dots: Dr. Jane Smith" "Rs 600 sent to Dr. Jane Smith via UPI. Txn successful." "Jane Smith"

print_status $GREEN "\n✅ NAME EXTRACTION TEST COMPLETED"
print_status $BLUE "================================"
print_status $YELLOW "Check the logs above to verify:"
print_status $YELLOW "1. Names are extracted correctly regardless of case"
print_status $YELLOW "2. Phone numbers are extracted as fallback"
print_status $YELLOW "3. UPI IDs are processed and converted to readable names"
print_status $YELLOW "4. Special characters and prefixes are handled properly"
print_status $YELLOW "5. Multiple word names are preserved"

print_status $BLUE "\nTo monitor real-time extraction logs:"
print_status $BLUE "adb logcat | grep -E '(TransactionDetector|Extracted recipient|Found recipient)'"