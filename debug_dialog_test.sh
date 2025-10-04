#!/bin/bash

# Debug Dialog Test Script for FlowPay
# This script helps test the call duration dialog functionality

echo "🔍 FlowPay Dialog Debug Test Script"
echo "=================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if adb is available
if ! command -v adb &> /dev/null; then
    print_error "ADB not found! Please install Android SDK and add to PATH"
    exit 1
fi

# Check if device is connected
if ! adb devices | grep -q "device$"; then
    print_error "No Android device connected! Please connect your device and enable USB debugging"
    exit 1
fi

print_success "Android device detected"

# Build the app
print_status "Building debug APK..."
if ./gradlew clean assembleDebug; then
    print_success "Build completed successfully"
else
    print_error "Build failed!"
    exit 1
fi

# Install the app
print_status "Installing APK..."
if adb install -r app/build/outputs/apk/debug/app-debug.apk; then
    print_success "APK installed successfully"
else
    print_error "APK installation failed!"
    exit 1
fi

# Clear previous logs
print_status "Clearing previous logs..."
adb logcat -c

print_status "Starting log monitoring..."
print_warning "Now perform the following test:"
echo ""
echo "1. Open the FlowPay app"
echo "2. Go to Manual Transfer"
echo "3. Enter a phone number (e.g., 9876543210)"
echo "4. Enter an amount (e.g., 100)"
echo "5. Press Transfer"
echo "6. END THE CALL WITHIN 10-15 SECONDS"
echo ""
print_warning "Watch for these key log messages:"
echo "🟢 START MONITORING - Timer begins NOW"
echo "📞 CALL STATE CHANGED: OFFHOOK"
echo "📞 CALL STATE CHANGED: IDLE"
echo "🚨 TRIGGER DIALOG - Call ended BEFORE 25 seconds!"
echo "🚨 CALLBACK: Call ended before 25 seconds!"
echo "🚨 showCallDurationIssueDialog called!"
echo "✅✅✅ DIALOG IS NOW VISIBLE!"
echo ""
print_status "Starting logcat with filter..."

# Start logcat with filter for our debug messages
adb logcat | grep -E "CallDurationMonitor|MainActivityHelper|MainActivity.*showCallDurationIssueDialog|MainActivity.*UICallback"


