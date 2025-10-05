#!/bin/bash

# Direct test for audio muting functionality
# This script tests the audio muting implementation directly

echo "🧪 Testing Audio Muting Implementation Directly"
echo "=============================================="

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

# Function to start the app
start_app() {
    print_status "INFO" "Starting FlowPay app..."
    adb shell am start -n com.flowpay.app/.MainActivity
    sleep 2
    print_status "SUCCESS" "App started"
}

# Function to test audio muting directly
test_audio_muting() {
    print_status "INFO" "Testing audio muting functionality..."
    
    # Start a call to test muting
    print_status "INFO" "Starting test call..."
    adb shell am start -a android.intent.action.CALL -d tel:"*123#"
    sleep 3
    
    # Check if call is active
    print_status "INFO" "Call should be active now. Testing audio muting..."
    
    # Test the audio muting by triggering the SMS receiver manually
    print_status "INFO" "Testing SMS muting logic..."
    
    # Use a different approach - send intent to our app
    adb shell am broadcast -a "com.flowpay.app.TEST_SMS_MUTING" --es sender "BANK" --es body "UPI123: Rs.100 debited to MERCHANT. UPI Ref: 123456789"
    
    sleep 2
    
    # End the call
    print_status "INFO" "Ending test call..."
    adb shell input keyevent KEYCODE_ENDCALL
    sleep 2
    
    print_status "SUCCESS" "Audio muting test completed"
}

# Function to monitor logs
monitor_logs() {
    print_status "INFO" "Starting log monitoring for audio muting tests..."
    echo ""
    echo "🔍 Monitoring logs for audio muting behavior..."
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

# Main execution
main() {
    echo "🚀 Starting Direct Audio Muting Test"
    echo ""
    
    check_device
    start_app
    
    echo ""
    print_status "INFO" "Choose test mode:"
    echo "1. Monitor logs only (recommended for manual testing)"
    echo "2. Test audio muting directly"
    echo "3. Run both tests"
    echo ""
    read -p "Enter your choice (1-3): " choice
    
    case $choice in
        1)
            monitor_logs
            ;;
        2)
            test_audio_muting
            ;;
        3)
            test_audio_muting
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

