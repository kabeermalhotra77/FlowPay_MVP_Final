# UPI123 Payment System Integration

This document describes the comprehensive UPI123 payment system integration for the FlowPay Android application.

## Overview

The UPI123 payment system allows users to make payments through a phone call-based verification process. The system integrates seamlessly with the existing FlowPay app and provides a secure, offline payment solution.

## Architecture

### Core Components

1. **ManualEntryDialog** - User interface for entering payment details
2. **CallOverlayActivity** - Handles the UPI123 call initiation and progress tracking
3. **PaymentProcessingActivity** - Manages the waiting period for bank verification call
4. **PaymentSuccessActivity** - Displays successful payment confirmation
5. **CallManager** - Utility class for call management and audio control
6. **PermissionManager** - Handles all required permissions
7. **NetworkUtils** - Network connectivity utilities
8. **UPI123Repository** - Data persistence and state management

### Payment Flow

```
User Input → Permission Check → Call Initiation → Progress Tracking → Bank Verification → Success/Failure
```

## Features

### 1. Manual Payment Entry
- **Phone Number Validation**: 10-digit mobile number validation
- **Amount Validation**: Decimal amount validation with proper formatting
- **Real-time Error Handling**: Immediate feedback for invalid inputs
- **Modern UI**: Material Design 3 components with custom styling

### 2. UPI123 Call Integration
- **Automated Call Initiation**: Constructs proper UPI123 call string
- **Progress Tracking**: Real-time progress indicators with step descriptions
- **Audio Management**: Automatic call muting for seamless user experience
- **Call State Monitoring**: Tracks call states and transitions

### 3. Bank Verification Process
- **Incoming Call Detection**: Monitors for bank verification calls
- **Timeout Handling**: 5-second timeout with retry mechanism
- **Visual Feedback**: Animated loading states and progress indicators
- **Error Recovery**: Graceful handling of failed transactions

### 4. Success Confirmation
- **Transaction Details**: Complete transaction summary
- **Animated UI**: Smooth animations for better user experience
- **Transaction History**: Automatic saving of transaction data
- **Navigation**: Seamless return to main activity

## Technical Implementation

### Permissions Required

```xml
<uses-permission android:name="android.permission.CALL_PHONE" />
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
```

### Key Classes

#### CallManager
- Manages UPI123 call string construction
- Handles call audio muting/unmuting
- Provides call state monitoring
- Implements call termination functionality

#### PermissionManager
- Centralized permission management
- Handles runtime permission requests
- Manages overlay permission for transparent activities
- Provides permission status checking

#### PaymentState
- Sealed class hierarchy for payment states
- Type-safe state management
- Clear state transitions
- Comprehensive error handling

### UI Components

#### ManualEntryDialog
- Material Design 3 OutlinedTextField components
- Real-time input validation
- Error state management
- Responsive layout design

#### CallOverlayScreen
- Transparent overlay design
- Circular progress indicators
- Step-by-step progress tracking
- Cancel functionality

#### PaymentProcessingScreen
- Animated phone icon
- Countdown timer display
- Retry mechanism
- Failure state handling

#### PaymentSuccessScreen
- Animated success icon
- Transaction detail cards
- Smooth spring animations
- Clean navigation flow

## Usage

### Basic Integration

1. **Add Dependencies**: Include required dependencies in `build.gradle`
2. **Update Manifest**: Add permissions and activities to `AndroidManifest.xml`
3. **Create Themes**: Add transparent theme for overlay activities
4. **Initialize System**: Set up permission manager and call manager

### Example Usage

```kotlin
// Initialize payment system
val permissionManager = PermissionManager(this)
val callManager = CallManager(this)

// Check permissions before initiating payment
if (permissionManager.checkAllPermissions()) {
    startCallOverlay(phoneNumber, amount)
} else {
    permissionManager.requestRequiredPermissions()
}
```

### Call String Format

The UPI123 call string follows this format:
```
tel:08045163666,,1,<phoneNumber>,<amount>,,1
```

Where:
- `08045163666` is the UPI123 service number
- `phoneNumber` is the recipient's 10-digit mobile number
- `amount` is the payment amount in rupees

## Security Considerations

1. **Permission Management**: All sensitive permissions are properly managed
2. **Call Monitoring**: Secure call state monitoring without data interception
3. **Audio Control**: Safe audio muting without affecting system security
4. **Data Validation**: Comprehensive input validation and sanitization
5. **State Management**: Secure state transitions and error handling

## Error Handling

### Common Error Scenarios

1. **Permission Denied**: Graceful fallback with user guidance
2. **Call Failed**: Automatic retry mechanism
3. **Network Issues**: Offline-capable design
4. **Invalid Input**: Real-time validation and feedback
5. **Timeout**: Configurable timeout with retry options

### Recovery Mechanisms

- **Automatic Retry**: Built-in retry for failed calls
- **User Guidance**: Clear error messages and next steps
- **State Persistence**: Maintains state across activity transitions
- **Graceful Degradation**: Fallback options for all scenarios

## Testing

### Unit Tests
- Permission manager functionality
- Call manager utilities
- Input validation logic
- State management

### Integration Tests
- End-to-end payment flow
- Permission handling
- Call state monitoring
- UI state transitions

### Manual Testing
- Various device configurations
- Different network conditions
- Permission scenarios
- Error conditions

## Future Enhancements

1. **QR Code Integration**: Add QR code scanning capability
2. **Transaction History**: Enhanced transaction management
3. **Multiple Payment Methods**: Support for additional payment options
4. **Analytics**: Payment analytics and reporting
5. **Offline Support**: Enhanced offline capabilities

## Troubleshooting

### Common Issues

1. **Permissions Not Granted**: Check permission manager implementation
2. **Call Not Initiating**: Verify call string format and permissions
3. **Audio Issues**: Check audio manager configuration
4. **UI Not Updating**: Verify state management and coroutines

### Debug Information

Enable debug logging for detailed troubleshooting:
```kotlin
Log.d("CallManager", "Call audio muted successfully")
Log.d("PermissionManager", "All permissions granted")
Log.d("PaymentState", "State updated: $newState")
```

## Conclusion

The UPI123 payment system provides a robust, secure, and user-friendly payment solution for the FlowPay application. The modular architecture ensures maintainability and extensibility while the comprehensive error handling provides a smooth user experience.

For additional support or questions, refer to the individual class documentation or contact the development team.
