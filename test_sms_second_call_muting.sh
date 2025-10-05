#!/bin/bash

# Test script for SMS-triggered muting during second call
# This script tests the enhanced SMS muting logic that properly handles both first and second call scenarios

echo "🧪 Testing SMS-Triggered Muting During Second Call"
echo "=================================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    local status=$1
    local message=$2
    case $status in
        "INFO")
            echo -e "${BLUE}ℹ️  $message${NC}"
            ;;
        "SUCCESS")
            echo -e "${GREEN}✅ $message${NC}"
            ;;
        "WARNING")
            echo -e "${YELLOW}⚠️  $message${NC}"
            ;;
        "ERROR")
            echo -e "${RED}❌ $message${NC}"
            ;;
    esac
}

# Function to check if device is connected
check_device() {
    if ! adb devices | grep -q "device$"; then
        print_status "ERROR" "No Android device connected. Please connect your device and enable USB debugging."
        exit 1
    fi
    print_status "SUCCESS" "Android device connected"
}

# Function to build and install the app
build_and_install() {
    print_status "INFO" "Building and installing the app..."
    
    if ./gradlew assembleDebug; then
        print_status "SUCCESS" "App built successfully"
    else
        print_status "ERROR" "Failed to build app"
        exit 1
    fi
    
    if adb install -r app/build/outputs/apk/debug/app-debug.apk; then
        print_status "SUCCESS" "App installed successfully"
    else
        print_status "ERROR" "Failed to install app"
        exit 1
    fi
}

# Function to start the app
start_app() {
    print_status "INFO" "Starting FlowPay app..."
    adb shell am start -n com.flowpay.app/.MainActivity
    sleep 2
    print_status "SUCCESS" "App started"
}

# Function to grant permissions
grant_permissions() {
    print_status "INFO" "Granting necessary permissions..."
    
    # Grant overlay permission
    adb shell appops set com.flowpay.app SYSTEM_ALERT_WINDOW allow
    
    # Grant SMS permission
    adb shell pm grant com.flowpay.app android.permission.RECEIVE_SMS
    adb shell pm grant com.flowpay.app android.permission.READ_SMS
    
    # Grant phone permission
    adb shell pm grant com.flowpay.app android.permission.READ_PHONE_STATE
    adb shell pm grant com.flowpay.app android.permission.CALL_PHONE
    
    # Grant audio permission
    adb shell pm grant com.flowpay.app android.permission.MODIFY_AUDIO_SETTINGS
    
    print_status "SUCCESS" "Permissions granted"
}

# Function to monitor logs
monitor_logs() {
    print_status "INFO" "Starting log monitoring for SMS muting tests..."
    echo ""
    echo "🔍 Monitoring logs for SMS muting behavior..."
    echo "Look for these key log messages:"
    echo "  - 'SMS RECEIVED FOR UPI_123'"
    echo "  - 'Current audio state - Muted: true/false'"
    echo "  - 'First call detected' or 'Second call detected'"
    echo "  - 'Second call audio muted successfully after SMS'"
    echo "  - 'Audio was muted, restoring now'"
    echo "  - 'Audio restored successfully'"
    echo ""
    echo "Press Ctrl+C to stop monitoring"
    echo ""
    
    adb logcat -c  # Clear existing logs
    adb logcat | grep -E "(SimpleSMSReceiver|AudioStateManager|CallStateListener|SMS.*UPI_123|audio.*muted|audio.*restored)"
}

# Function to simulate UPI 123 call flow
simulate_upi123_flow() {
    print_status "INFO" "Simulating UPI 123 call flow..."
    
    # Start UPI 123 transfer (this should mute the first call)
    print_status "INFO" "Step 1: Starting UPI 123 transfer (first call should be muted)"
    adb shell am start -a android.intent.action.CALL -d tel:"*123#"
    sleep 3
    
    # Simulate SMS during first call
    print_status "INFO" "Step 2: Simulating SMS during first call (should stay muted)"
    adb shell am broadcast -a android.provider.Telephony.SMS_RECEIVED --es sender "BANK" --es body "UPI123: Rs.100 debited to MERCHANT. UPI Ref: 123456789"
    sleep 2
    
    # End first call
    print_status "INFO" "Step 3: Ending first call (audio should be restored)"
    adb shell input keyevent KEYCODE_ENDCALL
    sleep 2
    
    # Start second call (this should have normal audio)
    print_status "INFO" "Step 4: Starting second call from bank (should have normal audio)"
    adb shell am start -a android.intent.action.CALL -d tel:"+919876543210"
    sleep 3
    
    # Simulate SMS during second call (this should NOW mute the call)
    print_status "INFO" "Step 5: Simulating SMS during second call (should NOW mute the call)"
    adb shell am broadcast -a android.provider.Telephony.SMS_RECEIVED --es sender "BANK" --es body "UPI123: Rs.100 debited to MERCHANT. UPI Ref: 123456789"
    sleep 2
    
    # End second call
    print_status "INFO" "Step 6: Ending second call (audio should be restored)"
    adb shell input keyevent KEYCODE_ENDCALL
    sleep 2
    
    print_status "SUCCESS" "UPI 123 flow simulation completed"
}

# Function to test edge cases
test_edge_cases() {
    print_status "INFO" "Testing edge cases..."
    
    # Test 1: SMS without active call
    print_status "INFO" "Edge Case 1: SMS without active call"
    adb shell am broadcast -a android.provider.Telephony.SMS_RECEIVED --es sender "BANK" --es body "UPI123: Rs.100 debited to MERCHANT. UPI Ref: 123456789"
    sleep 1
    
    # Test 2: Multiple SMS messages
    print_status "INFO" "Edge Case 2: Multiple SMS messages"
    adb shell am start -a android.intent.action.CALL -d tel:"*123#"
    sleep 2
    adb shell am broadcast -a android.provider.Telephony.SMS_RECEIVED --es sender "BANK" --es body "UPI123: Rs.100 debited to MERCHANT. UPI Ref: 123456789"
    sleep 1
    adb shell am broadcast -a android.provider.Telephony.SMS_RECEIVED --es sender "BANK" --es body "UPI123: Rs.100 debited to MERCHANT. UPI Ref: 123456789"
    sleep 1
    adb shell input keyevent KEYCODE_ENDCALL
    sleep 1
    
    print_status "SUCCESS" "Edge cases tested"
}

# Main execution
main() {
    echo "🚀 Starting SMS Second Call Muting Test"
    echo ""
    
    check_device
    grant_permissions
    build_and_install
    start_app
    
    echo ""
    print_status "INFO" "Choose test mode:"
    echo "1. Monitor logs only (recommended for manual testing)"
    echo "2. Run automated UPI 123 flow simulation"
    echo "3. Test edge cases"
    echo "4. Run all tests"
    echo ""
    read -p "Enter your choice (1-4): " choice
    
    case $choice in
        1)
            monitor_logs
            ;;
        2)
            simulate_upi123_flow
            ;;
        3)
            test_edge_cases
            ;;
        4)
            simulate_upi123_flow
            test_edge_cases
            monitor_logs
            ;;
        *)
            print_status "ERROR" "Invalid choice"
            exit 1
            ;;
    esac
}

# Run main function
main

