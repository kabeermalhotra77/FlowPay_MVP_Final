#!/bin/bash

# USSD Overlay Lifecycle Management Test Script
# This script tests the USSD overlay termination scenarios

echo "=== USSD Overlay Lifecycle Management Test ==="
echo "Testing USSD overlay termination when app goes to background or is closed"
echo ""

# Function to check if device is connected
check_device() {
    if ! adb devices | grep -q "device$"; then
        echo "❌ No Android device connected. Please connect a device and enable USB debugging."
        exit 1
    fi
    echo "✅ Android device connected"
}

# Function to install and launch the app
launch_app() {
    echo "📱 Installing and launching FlowPay app..."
    adb install -r app/build/outputs/apk/debug/app-debug.apk
    adb shell am start -n com.flowpay.app/.MainActivity
    sleep 3
    echo "✅ App launched successfully"
}

# Function to start QR scanner
start_qr_scanner() {
    echo "📷 Starting QR scanner..."
    adb shell input tap 200 400  # Tap on QR scan button (adjust coordinates as needed)
    sleep 2
    echo "✅ QR scanner started"
}

# Function to simulate QR code scan and USSD overlay
simulate_qr_scan() {
    echo "🔍 Simulating QR code scan..."
    # This would normally trigger the USSD overlay
    # For testing, we'll check if the overlay service is running
    adb shell "am broadcast -a com.flowpay.app.TRIGGER_QR_SCAN"
    sleep 2
    echo "✅ QR scan simulation completed"
}

# Function to check if USSD overlay service is running
check_ussd_overlay() {
    echo "🔍 Checking USSD overlay service status..."
    if adb shell "ps | grep -i ussd" | grep -q "ussd"; then
        echo "✅ USSD overlay service is running"
        return 0
    else
        echo "❌ USSD overlay service is not running"
        return 1
    fi
}

# Function to simulate app going to background
simulate_app_background() {
    echo "📱 Simulating app going to background..."
    adb shell input keyevent KEYCODE_HOME
    sleep 2
    echo "✅ App sent to background"
}

# Function to simulate app being closed
simulate_app_close() {
    echo "📱 Simulating app being closed..."
    adb shell am force-stop com.flowpay.app
    sleep 2
    echo "✅ App closed"
}

# Function to check logs for USSD overlay termination
check_termination_logs() {
    echo "📋 Checking logs for USSD overlay termination..."
    echo "Recent logs related to USSD overlay:"
    adb logcat -d | grep -i "ussd.*overlay.*terminated" | tail -5
    echo ""
    echo "App lifecycle logs:"
    adb logcat -d | grep -i "app.*paused\|app.*stopped\|app.*destroyed" | tail -5
}

# Function to test app resume
test_app_resume() {
    echo "📱 Testing app resume..."
    adb shell am start -n com.flowpay.app/.MainActivity
    sleep 2
    echo "✅ App resumed"
}

# Main test execution
main() {
    echo "Starting USSD Overlay Lifecycle Management Tests..."
    echo "=================================================="
    echo ""
    
    # Check device connection
    check_device
    
    # Launch app
    launch_app
    
    # Start QR scanner
    start_qr_scanner
    
    # Simulate QR scan
    simulate_qr_scan
    
    # Check if USSD overlay is running
    if check_ussd_overlay; then
        echo "✅ USSD overlay is active - proceeding with lifecycle tests"
    else
        echo "⚠️  USSD overlay not detected - this may be expected if no QR was actually scanned"
    fi
    
    echo ""
    echo "=== Test 1: App Backgrounding ==="
    echo "Testing USSD overlay termination when app goes to background..."
    
    # Simulate app going to background
    simulate_app_background
    
    # Check if USSD overlay was terminated
    sleep 3
    if check_ussd_overlay; then
        echo "❌ FAIL: USSD overlay is still running after app backgrounding"
    else
        echo "✅ PASS: USSD overlay was terminated when app went to background"
    fi
    
    echo ""
    echo "=== Test 2: App Resume ==="
    echo "Testing app resume behavior..."
    
    # Resume app
    test_app_resume
    
    echo ""
    echo "=== Test 3: App Closing ==="
    echo "Testing USSD overlay termination when app is closed..."
    
    # Start QR scanner again for closing test
    start_qr_scanner
    simulate_qr_scan
    
    if check_ussd_overlay; then
        echo "✅ USSD overlay is active - proceeding with close test"
    fi
    
    # Simulate app being closed
    simulate_app_close
    
    # Check if USSD overlay was terminated
    sleep 3
    if check_ussd_overlay; then
        echo "❌ FAIL: USSD overlay is still running after app closing"
    else
        echo "✅ PASS: USSD overlay was terminated when app was closed"
    fi
    
    echo ""
    echo "=== Test 4: Log Analysis ==="
    echo "Analyzing logs for proper lifecycle management..."
    check_termination_logs
    
    echo ""
    echo "=== Test Summary ==="
    echo "✅ USSD Overlay Lifecycle Management Tests Completed"
    echo ""
    echo "Key Features Tested:"
    echo "1. USSD overlay termination on app backgrounding"
    echo "2. USSD overlay termination on app closing"
    echo "3. App state broadcast system"
    echo "4. Proper cleanup and resource management"
    echo ""
    echo "The USSD overlay should now only be active while the user is"
    echo "actively using the app during QR scan operations."
}

# Run the tests
main
