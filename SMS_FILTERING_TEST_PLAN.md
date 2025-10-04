# SMS Filtering Test Plan

## Overview
This document outlines comprehensive testing procedures for the SMS filtering implementation in FlowPay app.

## Test Environment Setup

### Prerequisites
1. Android device with FlowPay app installed
2. SMS permissions granted
3. Overlay permissions granted
4. Phone call permissions granted
5. Test bank SMS numbers configured

### Test Data
- **Test Phone Numbers**: 9876543210, 9876543211
- **Test VPAs**: test@paytm, test@phonepe, test@gpay
- **Test Amounts**: 100, 500, 1000
- **Test Bank SMS Senders**: HDFC, ICICI, SBI, AXIS

## Test Cases

### 1. Manual Transfer Tests

#### Test Case 1.1: Valid Manual Transfer SMS
**Objective**: Verify SMS detection works for manual transfers
**Steps**:
1. Start manual transfer with phone: 9876543210, amount: 100
2. Complete UPI call flow
3. Send matching SMS: "Rs. 100 debited to 9876543210"
4. **Expected**: Success screen appears

#### Test Case 1.2: Non-Matching Manual Transfer SMS
**Objective**: Verify SMS filtering works for manual transfers
**Steps**:
1. Start manual transfer with phone: 9876543210, amount: 100
2. Complete UPI call flow
3. Send non-matching SMS: "Rs. 500 debited to 9876543211"
4. **Expected**: No success screen appears

#### Test Case 1.3: No Active Manual Transfer
**Objective**: Verify SMS is ignored when no manual transfer is active
**Steps**:
1. Do not start any manual transfer
2. Send SMS: "Rs. 100 debited to 9876543210"
3. **Expected**: No success screen appears

#### Test Case 1.4: Delayed Manual Transfer SMS
**Objective**: Verify SMS timeout works for manual transfers
**Steps**:
1. Start manual transfer with phone: 9876543210, amount: 100
2. Complete UPI call flow
3. Wait 11 minutes
4. Send matching SMS: "Rs. 100 debited to 9876543210"
5. **Expected**: No success screen appears (timeout exceeded)

### 2. QR Scanning Tests

#### Test Case 2.1: Valid QR Payment SMS
**Objective**: Verify SMS detection works for QR payments
**Steps**:
1. Scan QR code with VPA: test@paytm, amount: 500
2. Complete USSD flow
3. Send matching SMS: "Rs. 500 debited to test@paytm"
4. **Expected**: Success screen appears

#### Test Case 2.2: Non-Matching QR Payment SMS
**Objective**: Verify SMS filtering works for QR payments
**Steps**:
1. Scan QR code with VPA: test@paytm, amount: 500
2. Complete USSD flow
3. Send non-matching SMS: "Rs. 1000 debited to test@phonepe"
4. **Expected**: No success screen appears

#### Test Case 2.3: No Active QR Payment
**Objective**: Verify SMS is ignored when no QR payment is active
**Steps**:
1. Do not start any QR payment
2. Send SMS: "Rs. 500 debited to test@paytm"
3. **Expected**: No success screen appears

#### Test Case 2.4: Delayed QR Payment SMS
**Objective**: Verify SMS timeout works for QR payments
**Steps**:
1. Scan QR code with VPA: test@paytm, amount: 500
2. Complete USSD flow
3. Wait 11 minutes
4. Send matching SMS: "Rs. 500 debited to test@paytm"
5. **Expected**: No success screen appears (timeout exceeded)

### 3. Cross-Payment Tests

#### Test Case 3.1: Manual Transfer vs QR Payment SMS
**Objective**: Verify SMS filtering prevents cross-payment detection
**Steps**:
1. Start manual transfer with phone: 9876543210, amount: 100
2. Complete UPI call flow
3. Send QR payment SMS: "Rs. 100 debited to test@paytm"
4. **Expected**: No success screen appears

#### Test Case 3.2: QR Payment vs Manual Transfer SMS
**Objective**: Verify SMS filtering prevents cross-payment detection
**Steps**:
1. Scan QR code with VPA: test@paytm, amount: 500
2. Complete USSD flow
3. Send manual transfer SMS: "Rs. 500 debited to 9876543210"
4. **Expected**: No success screen appears

### 4. Edge Cases

#### Test Case 4.1: Multiple SMS Messages
**Objective**: Verify handling of multiple SMS messages
**Steps**:
1. Start manual transfer with phone: 9876543210, amount: 100
2. Complete UPI call flow
3. Send multiple SMS messages (matching and non-matching)
4. **Expected**: Only first matching SMS triggers success screen

#### Test Case 4.2: SMS with Different Amount Formats
**Objective**: Verify amount matching works with different formats
**Steps**:
1. Start manual transfer with phone: 9876543210, amount: 100
2. Complete UPI call flow
3. Send SMS with different formats:
   - "Rs. 100.00 debited to 9876543210"
   - "Rs. 100,00 debited to 9876543210"
   - "₹100 debited to 9876543210"
4. **Expected**: All formats should match and trigger success screen

#### Test Case 4.3: SMS with Different Phone Formats
**Objective**: Verify phone matching works with different formats
**Steps**:
1. Start manual transfer with phone: 9876543210, amount: 100
2. Complete UPI call flow
3. Send SMS with different phone formats:
   - "Rs. 100 debited to 9876543210"
   - "Rs. 100 debited to +91-9876543210"
   - "Rs. 100 debited to 9876543210@upi"
4. **Expected**: All formats should match and trigger success screen

#### Test Case 4.4: SMS with Different VPA Formats
**Objective**: Verify VPA matching works with different formats
**Steps**:
1. Scan QR code with VPA: test@paytm, amount: 500
2. Complete USSD flow
3. Send SMS with different VPA formats:
   - "Rs. 500 debited to test@paytm"
   - "Rs. 500 debited to test@paytm@upi"
   - "Rs. 500 debited to test"
4. **Expected**: All formats should match and trigger success screen

## Logging and Debugging

### Debug Logs to Monitor
1. **SMSReceiver**: Look for "SMS FILTERING DEBUG" logs
2. **PaymentStateManager**: Look for payment type and matching logs
3. **CallOverlayService**: Look for manual transfer validation logs
4. **USSDOverlayService**: Look for QR payment validation logs

### Key Log Messages
```
=== SMS FILTERING DEBUG ===
SMS Amount: [amount]
SMS Recipient: [recipient]
Active Session: [true/false]
Payment Type: [MANUAL_TRANSFER/QR_SCANNING]
Time Diff: [time_in_ms]
Matches Transaction: [true/false]
=== END SMS FILTERING DEBUG ===
```

### Manual Transfer Matching Logs
```
Manual transfer matching - Amount: [true/false] ([sms_amount] vs [expected_amount]), Phone: [true/false] ([sms_recipient] vs [expected_phone])
```

### QR Payment Matching Logs
```
QR payment matching - Amount: [true/false] ([sms_amount] vs [expected_amount]), VPA: [true/false] ([sms_recipient] vs [expected_vpa])
```

## Performance Testing

### Test Case P1: SMS Processing Performance
**Objective**: Verify SMS processing doesn't impact app performance
**Steps**:
1. Start multiple payment sessions
2. Send multiple SMS messages rapidly
3. Monitor app performance and memory usage
4. **Expected**: No performance degradation

### Test Case P2: Memory Leak Testing
**Objective**: Verify no memory leaks in SMS processing
**Steps**:
1. Run app for extended period
2. Process multiple SMS messages
3. Monitor memory usage over time
4. **Expected**: No memory leaks detected

## Error Handling Tests

### Test Case E1: Invalid SMS Format
**Objective**: Verify graceful handling of invalid SMS
**Steps**:
1. Start manual transfer
2. Send malformed SMS
3. **Expected**: App continues normally, no crashes

### Test Case E2: Missing Payment State
**Objective**: Verify graceful handling of missing payment state
**Steps**:
1. Clear app data
2. Send SMS without active payment
3. **Expected**: SMS is ignored, no crashes

## Regression Testing

### Test Case R1: Existing Functionality
**Objective**: Verify existing functionality still works
**Steps**:
1. Test all existing payment flows
2. Verify overlay functionality
3. Verify USSD functionality
4. **Expected**: All existing features work as before

### Test Case R2: Backward Compatibility
**Objective**: Verify backward compatibility with existing data
**Steps**:
1. Test with existing payment history
2. Test with existing user preferences
3. **Expected**: No data loss or corruption

## Test Execution Checklist

- [ ] All manual transfer tests pass
- [ ] All QR scanning tests pass
- [ ] All cross-payment tests pass
- [ ] All edge cases pass
- [ ] Performance tests pass
- [ ] Error handling tests pass
- [ ] Regression tests pass
- [ ] Debug logs are comprehensive
- [ ] No memory leaks detected
- [ ] No crashes or ANRs

## Success Criteria

1. **Accuracy**: 100% of matching SMS messages trigger success screens
2. **Filtering**: 100% of non-matching SMS messages are ignored
3. **Performance**: SMS processing adds < 50ms latency
4. **Reliability**: No crashes or memory leaks
5. **Compatibility**: All existing functionality preserved

## Test Results Template

| Test Case | Status | Notes |
|-----------|--------|-------|
| 1.1 | ✅/❌ | |
| 1.2 | ✅/❌ | |
| 1.3 | ✅/❌ | |
| 1.4 | ✅/❌ | |
| 2.1 | ✅/❌ | |
| 2.2 | ✅/❌ | |
| 2.3 | ✅/❌ | |
| 2.4 | ✅/❌ | |
| 3.1 | ✅/❌ | |
| 3.2 | ✅/❌ | |
| 4.1 | ✅/❌ | |
| 4.2 | ✅/❌ | |
| 4.3 | ✅/❌ | |
| 4.4 | ✅/❌ | |
| P1 | ✅/❌ | |
| P2 | ✅/❌ | |
| E1 | ✅/❌ | |
| E2 | ✅/❌ | |
| R1 | ✅/❌ | |
| R2 | ✅/❌ | |

## Notes

- Test with real bank SMS messages when possible
- Use different Android versions for compatibility testing
- Test on different device manufacturers (Samsung, Xiaomi, OnePlus, etc.)
- Monitor battery usage during SMS processing
- Test with poor network conditions
