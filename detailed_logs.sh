#!/bin/bash

echo "=== FlowPay Detailed Log Monitor ==="
echo "Capturing all log levels for comprehensive debugging..."
echo "Logs will be saved to flowpay_logs.txt"
echo "Press Ctrl+C to stop and view logs"
echo ""

# Clear existing logs
adb logcat -c

# Start detailed logging and save to file
adb logcat -v time | grep -E "(FlowPay|com.flowpay.app|MainActivity|PaymentStateManager|CallManager|PermissionManager|QRScanner|USSD|UPI|Payment|Overlay|SMS|Call|Error|Exception|FATAL|WARN|INFO|DEBUG|VERBOSE)" --line-buffered | tee flowpay_logs.txt
