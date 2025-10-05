# Overlay Call End Fix Implementation

## Problem Description

The call overlay system was not dismissing the overlay when UPI123 calls ended. The overlay would remain visible even after the call was completed, causing a poor user experience.

## Root Cause Analysis

The issue was caused by **multiple competing phone state listeners** that were not properly coordinated:

1. **CallManager** was setting up a phone state listener for UPI123 calls that only logged call end events but didn't notify the CallOverlayService
2. **CallStateMonitor** was setting up another phone state listener that should handle overlay dismissal
3. **MainActivityHelper** had its own call end handling logic
4. These systems were not coordinated, causing conflicts

Additionally, the **CallStateMonitor's UPI call detection logic** was failing because:
- UPI123 calls often have blank phone numbers in the call state events
- The `isUpiCall()` method was returning `false` for blank phone numbers
- This prevented the overlay from being dismissed when the call ended

## Solution Implemented

### 1. Removed Duplicate Phone State Listener from CallManager

**File:** `app/src/main/java/com/flowpay/app/managers/CallManager.kt`

**Change:** Removed the phone state listener setup in `initiateUPI123Call()` method to avoid conflicts with CallStateMonitor.

```kotlin
// Before: Conflicting phone state listener
phoneStateListener = createPhoneStateListener { callType ->
    Log.d(TAG, "=== UPI123 CALL ENDED ===")
    // ... only logging, no overlay dismissal
}

// After: Let CallStateMonitor handle everything
// Note: Phone state monitoring for UPI123 calls is handled by CallStateMonitor
// in CallOverlayService to avoid conflicts and ensure proper overlay dismissal
```

### 2. Enhanced UPI Call Detection for Blank Phone Numbers

**File:** `app/src/main/java/com/flowpay/app/managers/CallStateMonitor.kt`

**Change:** Modified `isUpiCall()` method to handle blank phone numbers by checking if the overlay service is active and expecting a UPI call.

```kotlin
private fun isUpiCall(phoneNumber: String?): Boolean {
    // If we have a valid UPI service number and phone number matches, it's definitely a UPI call
    if (!phoneNumber.isNullOrBlank() && !upiServiceNumber.isNullOrBlank()) {
        return PhoneNumberUtils.isPhoneNumberMatch(phoneNumber, upiServiceNumber)
    }
    
    // For UPI123 calls, phone number is often blank, so we need to check if overlay service
    // is active and expecting a UPI call (this indicates we're in a UPI123 call context)
    if (phoneNumber.isNullOrBlank() && overlayService != null) {
        Log.d(TAG, "Blank phone number detected - checking if this is a UPI123 call context")
        return overlayService.isActiveAndExpectingUpiCall()
    }
    
    return false
}
```

### 3. Added Overlay Service State Check Method

**File:** `app/src/main/java/com/flowpay/app/services/CallOverlayService.kt`

**Change:** Added method to check if overlay service is active and expecting a UPI call.

```kotlin
/**
 * Check if overlay service is active and expecting a UPI call
 * This helps CallStateMonitor determine if a blank phone number is a UPI call
 */
fun isActiveAndExpectingUpiCall(): Boolean {
    return isOverlayActive && isCallDetected
}
```

## How the Fix Works

1. **Single Source of Truth:** Only CallStateMonitor handles phone state changes for UPI123 calls
2. **Blank Phone Number Handling:** When phone number is blank (common for UPI123 calls), the system checks if the overlay service is active and expecting a UPI call
3. **Proper Call End Detection:** When call state changes to IDLE, CallStateMonitor properly detects it as a UPI call and triggers overlay dismissal
4. **Coordinated Cleanup:** CallOverlayService handles the overlay dismissal and shows appropriate dialogs

## Expected Behavior After Fix

1. User initiates UPI123 call
2. Overlay appears when call is detected
3. When call ends (user hangs up or call completes), overlay automatically disappears
4. Appropriate dialog is shown based on call end reason (completed, cancelled, failed, etc.)
5. Service stops cleanly

## Testing

Use the test script `test_overlay_call_end_fix.sh` to verify the fix:

```bash
./test_overlay_call_end_fix.sh
```

The script will monitor logs and show:
- ✅ CALL END DETECTED: When call state changes to IDLE
- ✅ OVERLAY DISMISSAL TRIGGERED: When overlay dismissal is triggered
- ✅ SERVICE STOPPED: When service stops cleanly
- ✅ BLANK PHONE NUMBER HANDLED: When blank phone numbers are handled correctly
- ✅ UPI CALL CONTEXT DETECTED: When UPI call context is detected

## Files Modified

1. `app/src/main/java/com/flowpay/app/managers/CallManager.kt`
2. `app/src/main/java/com/flowpay/app/managers/CallStateMonitor.kt`
3. `app/src/main/java/com/flowpay/app/services/CallOverlayService.kt`

## Impact

- ✅ Fixes overlay persistence issue
- ✅ Improves user experience
- ✅ Maintains existing functionality
- ✅ No breaking changes
- ✅ Better error handling for edge cases
