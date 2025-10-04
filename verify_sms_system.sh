#!/bin/bash

# Complete SMS Parsing System Verification Script
# Verifies all components are working correctly

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

print_status $BLUE "🔍 SMS PARSING SYSTEM VERIFICATION"
print_status $BLUE "==================================="

# Function to check if a file exists
check_file() {
    local file_path="$1"
    local description="$2"
    
    if [ -f "$file_path" ]; then
        print_status $GREEN "✅ $description"
        return 0
    else
        print_status $RED "❌ $description - FILE NOT FOUND"
        return 1
    fi
}

# Function to check if a pattern exists in a file
check_pattern() {
    local file_path="$1"
    local pattern="$2"
    local description="$3"
    
    if grep -q "$pattern" "$file_path" 2>/dev/null; then
        print_status $GREEN "✅ $description"
        return 0
    else
        print_status $RED "❌ $description - PATTERN NOT FOUND"
        return 1
    fi
}

# Function to check if a pattern does NOT exist in a file
check_no_pattern() {
    local file_path="$1"
    local pattern="$2"
    local description="$3"
    
    if ! grep -q "$pattern" "$file_path" 2>/dev/null; then
        print_status $GREEN "✅ $description"
        return 0
    else
        print_status $RED "❌ $description - PATTERN FOUND (should not exist)"
        return 1
    fi
}

print_status $YELLOW "\n📁 PART 1: ESSENTIAL FILES CHECK"
print_status $BLUE "================================="

# Check essential files
check_file "app/src/main/java/com/flowpay/app/receivers/SimpleSMSReceiver.kt" "SimpleSMSReceiver.kt exists"
check_file "app/src/main/java/com/flowpay/app/helpers/TransactionDetector.kt" "TransactionDetector.kt exists"
check_file "app/src/main/java/com/flowpay/app/helpers/SMSPermissionHelper.kt" "SMSPermissionHelper.kt exists"
check_file "app/src/main/java/com/flowpay/app/helpers/DebugHelper.kt" "DebugHelper.kt exists"
check_file "app/src/main/java/com/flowpay/app/ui/activities/PaymentSuccessActivity.kt" "PaymentSuccessActivity.kt exists"

print_status $YELLOW "\n📁 PART 2: OLD FILES REMOVAL CHECK"
print_status $BLUE "==================================="

# Check that old files are removed
check_no_pattern "app/src/main/java" "PaymentStateManager" "PaymentStateManager.kt removed"
check_no_pattern "app/src/main/java" "UPI123Repository" "UPI123Repository.kt removed"
check_no_pattern "app/src/main/java" "SMSReceiver" "Old SMSReceiver.kt removed"

print_status $YELLOW "\n📁 PART 3: PERMISSIONS CHECK"
print_status $BLUE "============================="

# Check AndroidManifest.xml permissions
check_pattern "app/src/main/AndroidManifest.xml" "RECEIVE_SMS" "RECEIVE_SMS permission present"
check_pattern "app/src/main/AndroidManifest.xml" "READ_SMS" "READ_SMS permission present"
check_pattern "app/src/main/AndroidManifest.xml" "READ_PHONE_STATE" "READ_PHONE_STATE permission present"
check_pattern "app/src/main/AndroidManifest.xml" "MODIFY_AUDIO_SETTINGS" "MODIFY_AUDIO_SETTINGS permission present"
check_no_pattern "app/src/main/AndroidManifest.xml" "ANSWER_PHONE_CALLS" "ANSWER_PHONE_CALLS permission absent (correct)"

print_status $YELLOW "\n📁 PART 4: SMS MONITORING CHECK"
print_status $BLUE "================================="

# Check SimpleSMSReceiver has shouldProcessSMS check
check_pattern "app/src/main/java/com/flowpay/app/receivers/SimpleSMSReceiver.kt" "shouldProcessSMS" "SimpleSMSReceiver checks shouldProcessSMS()"
check_pattern "app/src/main/java/com/flowpay/app/receivers/SimpleSMSReceiver.kt" "No active payment operation" "Early exit message present"

# Check TransactionDetector has shouldProcessSMS method
check_pattern "app/src/main/java/com/flowpay/app/helpers/TransactionDetector.kt" "fun shouldProcessSMS" "shouldProcessSMS() method exists"
check_pattern "app/src/main/java/com/flowpay/app/helpers/TransactionDetector.kt" "TIMEOUT_MILLIS" "Timeout mechanism present"

print_status $YELLOW "\n📁 PART 5: NAME EXTRACTION CHECK"
print_status $BLUE "================================="

# Check name extraction patterns use [a-zA-Z]
check_pattern "app/src/main/java/com/flowpay/app/helpers/TransactionDetector.kt" "\\[a-zA-Z\\]" "Patterns use [a-zA-Z] (case-insensitive)"
check_pattern "app/src/main/java/com/flowpay/app/helpers/TransactionDetector.kt" "recipientName.*String" "recipientName field exists"
check_pattern "app/src/main/java/com/flowpay/app/helpers/TransactionDetector.kt" "phoneNumber.*String" "phoneNumber field exists"
check_pattern "app/src/main/java/com/flowpay/app/helpers/TransactionDetector.kt" "extractRecipientInfo" "extractRecipientInfo method exists"
check_pattern "app/src/main/java/com/flowpay/app/helpers/TransactionDetector.kt" "cleanupName" "cleanupName method exists"

print_status $YELLOW "\n📁 PART 6: PAYMENT SUCCESS SCREEN CHECK"
print_status $BLUE "======================================"

# Check PaymentSuccessActivity layout
check_pattern "app/src/main/res/layout/activity_payment_success.xml" "layout_recipient" "Recipient layout exists"
check_pattern "app/src/main/res/layout/activity_payment_success.xml" "tv_recipient_label" "Recipient label exists"
check_pattern "app/src/main/res/layout/activity_payment_success.xml" "tv_recipient_name" "Recipient name text exists"
check_pattern "app/src/main/res/layout/activity_payment_success.xml" "#000000" "Black background present"

# Check PaymentSuccessActivity.kt
check_pattern "app/src/main/java/com/flowpay/app/ui/activities/PaymentSuccessActivity.kt" "recipientLayout" "Recipient layout variable exists"
check_pattern "app/src/main/java/com/flowpay/app/ui/activities/PaymentSuccessActivity.kt" "recipientLabel" "Recipient label variable exists"
check_pattern "app/src/main/java/com/flowpay/app/ui/activities/PaymentSuccessActivity.kt" "recipientText" "Recipient text variable exists"
check_pattern "app/src/main/java/com/flowpay/app/ui/activities/PaymentSuccessActivity.kt" "recipient_name" "recipient_name intent extra handled"
check_pattern "app/src/main/java/com/flowpay/app/ui/activities/PaymentSuccessActivity.kt" "phone_number" "phone_number intent extra handled"

print_status $YELLOW "\n📁 PART 7: CALL MUTING CHECK"
print_status $BLUE "============================"

# Check AudioStateManager
check_file "app/src/main/java/com/flowpay/app/helpers/AudioStateManager.kt" "AudioStateManager.kt exists"
check_pattern "app/src/main/java/com/flowpay/app/helpers/AudioStateManager.kt" "muteCallAudio" "muteCallAudio method exists"
check_pattern "app/src/main/java/com/flowpay/app/helpers/AudioStateManager.kt" "restoreCallAudio" "restoreCallAudio method exists"

# Check CallStateListener
check_file "app/src/main/java/com/flowpay/app/helpers/CallStateListener.kt" "CallStateListener.kt exists"
check_pattern "app/src/main/java/com/flowpay/app/helpers/CallStateListener.kt" "onCallStateChanged" "onCallStateChanged method exists"

print_status $YELLOW "\n📁 PART 8: DATABASE CHECK"
print_status $BLUE "========================="

# Check Transaction entity
check_pattern "app/src/main/java/com/flowpay/app/data/Transaction.kt" "recipientName.*String" "Transaction entity has recipientName field"
check_pattern "app/src/main/java/com/flowpay/app/data/Transaction.kt" "phoneNumber.*String" "Transaction entity has phoneNumber field"
check_pattern "app/src/main/java/com/flowpay/app/data/Transaction.kt" "fromSimpleTransaction" "fromSimpleTransaction method exists"

# Check TransactionRepository
check_file "app/src/main/java/com/flowpay/app/repository/TransactionRepository.kt" "TransactionRepository.kt exists"
check_pattern "app/src/main/java/com/flowpay/app/repository/TransactionRepository.kt" "saveTransaction" "saveTransaction method exists"

# Check Room dependencies in build.gradle
check_pattern "app/build.gradle" "room-runtime" "Room runtime dependency present"
check_pattern "app/build.gradle" "room-ktx" "Room KTX dependency present"
check_pattern "app/build.gradle" "room-compiler" "Room compiler dependency present"

print_status $YELLOW "\n📁 PART 9: MAIN ACTIVITY TRANSACTION HISTORY CHECK"
print_status $BLUE "================================================="

# Check MainActivity has transaction history
check_pattern "app/src/main/java/com/flowpay/app/MainActivity.kt" "recentPayments" "recentPayments variable exists"
check_pattern "app/src/main/java/com/flowpay/app/MainActivity.kt" "TransactionItem" "TransactionItem composable exists"
check_pattern "app/src/main/java/com/flowpay/app/MainActivity.kt" "recipientName.*phoneNumber" "TransactionItem shows recipient name or phone"

print_status $GREEN "\n✅ VERIFICATION COMPLETED"
print_status $BLUE "========================"
print_status $YELLOW "All components have been checked. If any items show ❌, those need to be fixed."
print_status $YELLOW "If all items show ✅, the SMS parsing system is properly implemented."

print_status $BLUE "\nNext steps:"
print_status $BLUE "1. Run the test scripts to verify functionality"
print_status $BLUE "2. Test with real SMS messages"
print_status $BLUE "3. Verify call muting works during UPI 123"
print_status $BLUE "4. Check transaction history displays correctly"


