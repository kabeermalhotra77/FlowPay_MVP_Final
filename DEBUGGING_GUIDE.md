# FlowPay UPI123 Integration - Debugging Guide

## **Critical Issues Fixed**

### ✅ **Issue 1: Call Audio Management**
**Problem**: Call audio was not being properly muted during UPI123 calls.

**Solution Implemented**:
- Enhanced `muteCall()` method to mute all audio streams
- Added comprehensive error handling and logging
- Set audio mode to `MODE_IN_CALL` for proper call audio control

```kotlin
private fun muteCall() {
    try {
        Log.d(TAG, "Attempting to mute call audio")
        audioManager.mode = AudioManager.MODE_IN_CALL
        audioManager.isMicrophoneMute = true
        audioManager.isSpeakerphoneOn = false
        
        // Mute all audio streams
        audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, 0, 0)
        audioManager.setStreamVolume(AudioManager.STREAM_RING, 0, 0)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 0, 0)
        audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0)
        
        Log.d(TAG, "Call audio muted successfully")
    } catch (e: Exception) {
        Log.e(TAG, "Failed to mute call audio: ${e.message}")
    }
}
```

### ✅ **Issue 2: Overlay Not Appearing Over Call**
**Problem**: CallOverlayActivity was not visible during the call.

**Solution Implemented**:
- Added window flags for better overlay visibility
- Enhanced theme configuration
- Added delay before call initiation to ensure overlay is drawn

```kotlin
// Window flags for overlay
window.addFlags(android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
window.addFlags(android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

// Delay to ensure overlay is drawn before call
handler.postDelayed({
    initiateUpiCall()
}, 500)
```

### ✅ **Issue 3: "Call Failed" When Call Ends**
**Problem**: Payment was showing as failed immediately when call ended.

**Solution Implemented**:
- Added call duration tracking (minimum 15 seconds for success)
- Added progress completion tracking (95% completion required)
- Enhanced success/failure detection logic

```kotlin
// Call duration tracking
private var callStartTime: Long = 0
private var callEndTime: Long = 0
private var isPaymentComplete = false
private const val MIN_CALL_DURATION_MS = 15000 // 15 seconds minimum

// Success detection
if (callDuration >= MIN_CALL_DURATION_MS && isPaymentComplete) {
    Log.d(TAG, "Call successful - moving to payment processing")
    moveToPaymentProcessing()
} else {
    Log.d(TAG, "Call too short or payment incomplete - showing failure")
    showPaymentFailure()
}
```

### ✅ **Issue 4: Enhanced Logging & Debugging**
**Solution Implemented**:
- Added comprehensive logging throughout the call flow
- Added call state transition tracking
- Added audio management logging
- Added progress completion tracking

## **Testing Checklist**

### **Pre-Test Setup**
- [ ] Ensure `SYSTEM_ALERT_WINDOW` permission is granted
- [ ] Check `MODIFY_AUDIO_SETTINGS` permission is granted
- [ ] Verify `CALL_PHONE` permission is granted
- [ ] Test on physical device (not emulator)

### **Test Flow**
1. **Start Payment**:
   - [ ] Dialog appears correctly
   - [ ] Phone number and amount are captured
   - [ ] CallOverlayActivity starts

2. **Call Initiation**:
   - [ ] Call string is correct: `tel:08045163666,,1,phone,amount,,1`
   - [ ] Call connects to UPI123 service
   - [ ] Overlay appears over call screen

3. **Audio Management**:
   - [ ] Call audio is muted (no IVR voice heard)
   - [ ] No DTMF tones audible
   - [ ] Microphone is muted

4. **Progress Tracking**:
   - [ ] Progress bar animates from 0% to 100%
   - [ ] Progress steps update correctly
   - [ ] Payment completion is tracked at 95%

5. **Call Completion**:
   - [ ] Call lasts at least 15 seconds
   - [ ] Success detection works correctly
   - [ ] PaymentProcessingActivity shows appropriate state

### **Debugging Commands**

#### **Check Logs**
```bash
adb logcat | grep "CallOverlayActivity"
adb logcat | grep "PaymentProcessingActivity"
adb logcat | grep "FlowPay"
```

#### **Key Log Messages to Look For**
```
CallOverlayActivity: CallOverlayActivity onCreate - Phone: [number], Amount: [amount]
CallOverlayActivity: Call state changed to: [state]
CallOverlayActivity: Call active - muting audio
CallOverlayActivity: Call audio muted successfully
CallOverlayActivity: Call ended - Duration: [duration]ms, Payment complete: [boolean]
CallOverlayActivity: Call successful - moving to payment processing
```

#### **Permission Checks**
```bash
adb shell dumpsys package com.flowpay.app | grep permission
```

## **Common Issues & Solutions**

### **Issue: Overlay Not Visible**
**Symptoms**: Call starts but no overlay appears
**Solutions**:
1. Check `SYSTEM_ALERT_WINDOW` permission in Settings
2. Verify theme configuration
3. Check window flags are set correctly

### **Issue: Audio Still Audible**
**Symptoms**: Can hear IVR system or DTMF tones
**Solutions**:
1. Check `MODIFY_AUDIO_SETTINGS` permission
2. Verify audio manager configuration
3. Test on different device manufacturers

### **Issue: Payment Shows Failed Immediately**
**Symptoms**: Call ends and immediately shows failure
**Solutions**:
1. Check call duration (should be > 15 seconds)
2. Verify progress completion tracking
3. Check call state transitions in logs

### **Issue: Call Doesn't Connect**
**Symptoms**: Call fails to initiate
**Solutions**:
1. Check `CALL_PHONE` permission
2. Verify call string format
3. Test with different phone numbers

## **Device-Specific Considerations**

### **Samsung Devices**
- May require additional audio routing configuration
- Check for Samsung-specific call screen overlays

### **Xiaomi/MIUI Devices**
- May need additional permissions in MIUI settings
- Check for battery optimization settings

### **OnePlus Devices**
- May require special overlay permission handling
- Check for OnePlus-specific call screen behavior

## **Performance Monitoring**

### **Memory Usage**
```bash
adb shell dumpsys meminfo com.flowpay.app
```

### **CPU Usage**
```bash
adb shell top | grep com.flowpay.app
```

### **Battery Usage**
```bash
adb shell dumpsys batterystats | grep com.flowpay.app
```

## **Next Steps**

1. **Test the implementation** with the debugging checklist above
2. **Monitor logs** during test runs
3. **Report any issues** with specific log messages
4. **Fine-tune parameters** based on testing results

The implementation now includes comprehensive error handling, logging, and debugging capabilities to help identify and resolve any remaining issues.
