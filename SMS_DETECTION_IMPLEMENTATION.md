# SMS Detection System Implementation

## Overview

This document describes the complete SMS detection system implemented for FlowPay app. The system automatically detects payment confirmation SMS from banks and triggers appropriate actions.

## Architecture

### Components

1. **SMSReceiver** - Broadcast receiver that listens for incoming SMS
2. **CallOverlayService** - Service that handles SMS detection and triggers success screens
3. **TransactionData** - Data class for storing parsed transaction information
4. **TestSMSUtil** - Utility for testing SMS functionality
5. **SMSTestActivity** - UI for testing different SMS scenarios

### Flow

```
SMS Received → SMSReceiver → Parse Transaction → Local Broadcast → CallOverlayService → Success Screen
```

## Implementation Details

### 1. SMSReceiver.kt

**Location**: `app/src/main/java/com/flowpay/app/receivers/SMSReceiver.kt`

**Features**:
- Listens for `android.provider.Telephony.SMS_RECEIVED` broadcasts
- Filters SMS from known bank senders
- Detects transaction-related SMS using keyword matching
- Parses transaction details using regex patterns
- Sends local broadcast with parsed transaction data

**Bank Senders Supported**:
- HDFC, ICICI, SBI, Axis, Kotak, PNB, BOB, Canara, Union, IDBI, Yes Bank
- Paytm, PhonePe, Google Pay, BharatPe

**Transaction Keywords**:
- debited, credited, payment, transferred, sent, received
- txn, transaction, upi, imps, neft, rtgs
- successfully, successful, completed, failed, declined, insufficient

### 2. CallOverlayService.kt

**Location**: `app/src/main/java/com/flowpay/app/services/CallOverlayService.kt`

**SMS Integration**:
- Registers local broadcast receiver for SMS events
- Handles `TRANSACTION_DETECTED` action
- Provides haptic feedback (vibration)
- Launches PaymentSuccessActivity with transaction data
- Automatically stops service after success screen launch

### 3. TransactionData.kt

**Location**: `app/src/main/java/com/flowpay/app/data/UPIData.kt`

**Fields**:
- `amount`: Transaction amount
- `upiId`: UPI reference/transaction ID
- `balance`: Account balance after transaction
- `recipient`: Recipient name or VPA
- `status`: Transaction status (SUCCESS/FAILED/PENDING)
- `rawMessage`: Original SMS text
- `timestamp`: Parsing timestamp

### 4. AndroidManifest.xml

**SMS Receiver Registration**:
```xml
<receiver
    android:name=".receivers.SMSReceiver"
    android:enabled="true"
    android:exported="true"
    android:permission="android.permission.BROADCAST_SMS">
    <intent-filter android:priority="999">
        <action android:name="android.provider.Telephony.SMS_RECEIVED" />
    </intent-filter>
</receiver>
```

**Permissions**:
- `android.permission.RECEIVE_SMS`
- `android.permission.READ_SMS`
- `android.permission.VIBRATE`

### 5. MainActivity.kt

**SMS Permission Handling**:
- Checks for SMS permissions on app start
- Shows dialog explaining why SMS permission is needed
- Requests permissions with specific request code
- Handles permission results with user feedback

## Testing

### TestSMSUtil.kt

**Location**: `app/src/main/java/com/flowpay/app/utils/TestSMSUtil.kt`

**Test Methods**:
- `sendTestSMS()` - Basic transaction SMS
- `sendTestSMSWithBankFormat(bankName)` - Bank-specific SMS formats
- `sendTestFailedSMS()` - Failed transaction SMS
- `sendTestPendingSMS()` - Pending transaction SMS
- `testAllSMSFormats()` - Sequential test of all formats

### SMSTestActivity.kt

**Location**: `app/src/main/java/com/flowpay/app/ui/activities/SMSTestActivity.kt`

**UI Features**:
- Buttons for testing different SMS scenarios
- Real-time feedback via Toast messages
- Comprehensive test suite

## Usage

### 1. Permission Setup

The app automatically requests SMS permissions on first launch:

```kotlin
private fun checkSMSPermissions() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val smsPermissions = arrayOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS
        )
        // Request permissions with user-friendly dialog
    }
}
```

### 2. Testing SMS Detection

Launch the SMS test activity:

```kotlin
val intent = Intent(context, SMSTestActivity::class.java)
context.startActivity(intent)
```

### 3. Manual Testing

Use TestSMSUtil to simulate SMS:

```kotlin
// Test basic SMS
TestSMSUtil.sendTestSMS(context)

// Test specific bank format
TestSMSUtil.sendTestSMSWithBankFormat(context, "HDFC")

// Test all formats
TestSMSUtil.testAllSMSFormats(context)
```

## SMS Format Examples

### HDFC Bank
```
Dear Customer, Rs.250.00 has been debited from your A/c **5678 on 15-Jan-24. 
UPI Ref: 987654321098. Bal: Rs.12,500.00. -HDFC Bank
```

### SBI Bank
```
SBI: Rs.500.00 debited from A/c **9999 on 15-Jan-24. 
UPI Ref: 123456789012. Available Bal: Rs.8,750.00
```

### Paytm
```
Paytm: Rs.75.00 debited from your Paytm Payments Bank A/c **2222 on 15-Jan-24. 
UPI Ref: 777777777777. Available Balance: Rs.3,250.00
```

## Parsing Patterns

### Amount Extraction
- `(?:rs\.?\s*)([0-9,]+(?:\.[0-9]{2})?)`
- `(?:inr\s*)([0-9,]+(?:\.[0-9]{2})?)`
- `₹\s*([0-9,]+(?:\.[0-9]{2})?)`

### UPI Reference
- `(?:upi\s*(?:ref|id)?[:\s]*)([0-9]{9,12})`
- `(?:ref\.?\s*no\.?[:\s]*)([0-9]{9,12})`
- `(?:transaction\s*id[:\s]*)([A-Z0-9]{9,12})`

### Balance Extraction
- `(?:bal|balance|available balance)[:\s]*(?:rs\.?\s*)?([0-9,]+(?:\.[0-9]{2})?)`
- `(?:available|avl|avail)\.?\s*(?:bal|balance)?[:\s]*(?:rs\.?\s*)?([0-9,]+(?:\.[0-9]{2})?)`

## Troubleshooting

### Common Issues

1. **SMS not detected**
   - Check if default SMS app is blocking broadcasts
   - Verify SMS permissions are granted
   - Check receiver priority in manifest

2. **Permission denied**
   - Ensure permissions are requested at runtime
   - Check Settings > Apps > Your App > Permissions

3. **Receiver not triggered**
   - Verify receiver is registered in manifest
   - Check if receiver is exported="true"
   - Ensure proper intent filter priority

4. **Can't parse SMS**
   - Log the exact SMS format from your bank
   - Adjust regex patterns in SMSReceiver.kt
   - Test with TestSMSUtil

### Debug Logging

Enable debug logs to track SMS flow:

```kotlin
Log.d("SMSReceiver", "=== SMS BROADCAST RECEIVED ===")
Log.d("SMSReceiver", "SMS from: $sender")
Log.d("SMSReceiver", "SMS body: $body")
Log.d("SMSReceiver", "Transaction detected: $transactionData")
```

## Security Considerations

1. **SMS Content**: Only transaction-related SMS are processed
2. **Bank Filtering**: Only known bank senders are accepted
3. **Data Privacy**: Transaction data is only used for app functionality
4. **Permissions**: Minimal required permissions are requested

## Future Enhancements

1. **Machine Learning**: Use ML for better SMS parsing
2. **More Banks**: Add support for additional banks
3. **Offline Support**: Cache transaction data locally
4. **Analytics**: Track SMS detection success rates
5. **Custom Patterns**: Allow users to add custom SMS patterns

## Conclusion

The SMS detection system provides robust, reliable payment confirmation detection with comprehensive testing capabilities. The modular design allows for easy maintenance and future enhancements.


