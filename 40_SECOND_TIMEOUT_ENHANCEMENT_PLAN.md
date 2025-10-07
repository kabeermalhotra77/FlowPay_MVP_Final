# 40-Second Timeout Enhancement Plan

## Problem Statement

### Issue 1: Both Dialogs Appearing
Currently, when a call exceeds 40 seconds:
- At 25 seconds: Success dialog appears ("Payment likely successful")
- At 40 seconds: Timeout dialog appears ("Transaction failed")
- **Problem**: User sees BOTH dialogs which is confusing

### Issue 2: No Overlay Feedback
The CallOverlay shows progress but doesn't handle the 40-second timeout itself. It should:
- Show a "Transaction Failed" message in the overlay when 40 seconds is reached
- Terminate the call
- Stay visible for a few seconds with the error message
- Then dismiss gracefully

---

## Current System Analysis

### Timer Flow (CallDurationMonitor.kt)
```
Transfer Initiated
    ↓
[25-second timer starts] ──→ If call active at 25s → Success callback → Cancel 25s timer only
    ↓
[40-second timer starts] ──→ If call active at 40s → Timeout callback → Stop all monitoring
```

**Current Issue**: The 25-second success callback is invoked even though the call will eventually timeout. The success dialog shows, then later the timeout dialog shows.

### Overlay Flow (CallOverlayService.kt)
```
Call Detected
    ↓
Show Overlay with progress animation (35 seconds animation)
    ↓
Overlay stays active until call ends
    ↓
Call ends → Hide overlay → Show dialog based on CallStateMonitor reason
```

**Current Issue**: Overlay has no awareness of the 40-second timeout. It just animates progress indefinitely.

---

## Solution Design

## Part 1: Fix Dialog Conflict

### Root Cause
The 25-second "success" callback fires and shows a dialog even though the call hasn't actually ended. This assumes the call will succeed, but if it goes past 40 seconds, we now know it failed.

### Solution Strategy
**Option A: Suppress 25-second dialog if timeout is imminent**
- At 25 seconds, set a flag instead of showing dialog immediately
- If call ends naturally after 25s but before 40s → Show success dialog
- If 40s timeout is reached → Cancel the pending success dialog, show timeout dialog only

**Option B: Cancel 25-second callback when 40s timeout occurs**
- At 25 seconds, show success dialog as normal
- At 40 seconds, dismiss any active dialogs, show timeout dialog

**Recommended: Option A** - Better UX, prevents dialog flashing

### Implementation Steps

#### Step 1.1: Add State Flag in CallDurationMonitor.kt
```kotlin
private var hasPendingSuccessDialog = false
```

#### Step 1.2: Modify 25-Second Timer Logic
Instead of immediately showing success dialog:
```kotlin
// At 25 seconds
if (isTimerActive && isCallActive) {
    Log.d(TAG, "✅ 25-SECOND TIMER COMPLETED")
    hasPendingSuccessDialog = true  // Mark as pending
    // DON'T invoke callback yet
    // DON'T cancel timers
}
```

#### Step 1.3: Modify Call End Logic (IDLE state)
When call ends naturally:
```kotlin
when (state) {
    TelephonyManager.CALL_STATE_IDLE -> {
        if (isCallActive) {
            isCallActive = false
            val elapsedSeconds = (System.currentTimeMillis() - timerStartTime) / 1000
            
            when {
                // Case 1: Call ended before 25 seconds
                elapsedSeconds < TIMER_DURATION_SECONDS -> {
                    onCallDurationIssue?.invoke()
                }
                // Case 2: Call ended between 25-40 seconds (success!)
                elapsedSeconds >= TIMER_DURATION_SECONDS && 
                elapsedSeconds < TIMER_MAXIMUM_DURATION_SECONDS -> {
                    if (hasPendingSuccessDialog) {
                        onCallSuccessful?.invoke()
                    }
                }
                // Case 3: Call ended after 40 seconds - already handled by timeout
            }
            stopMonitoring()
        }
    }
}
```

#### Step 1.4: Ensure 40-Second Timeout is Exclusive
At 40 seconds:
```kotlin
if (isTimerActive && isCallActive) {
    Log.w(TAG, "⏰ 40-SECOND TIMEOUT REACHED")
    hasPendingSuccessDialog = false  // Cancel any pending success dialog
    onCallTimeout?.invoke()
    stopMonitoring()
}
```

---

## Part 2: Add Overlay Timeout Handling

### Design
The overlay should have its own awareness of time and show a failure state at 40 seconds.

### Visual Flow
```
0-25s:   [Overlay showing progress: "Processing Payment..."]
25-40s:  [Overlay showing progress: "Processing Payment..."] 
40s:     [Overlay changes to: "❌ Transaction Failed - Please try later"]
40-43s:  [Overlay stays visible with error message]
43s:     [Overlay dismisses with animation]
         [Call is terminated]
         [Timeout dialog appears]
```

### Implementation Steps

#### Step 2.1: Add Overlay Timeout Timer in CallOverlayService.kt

Add new variables:
```kotlin
private var overlayTimeoutHandler: Handler? = null
private var overlayTimeoutRunnable: Runnable? = null
private var overlayStartTime = 0L
private val OVERLAY_TIMEOUT_DURATION = 40000L // 40 seconds
private val ERROR_MESSAGE_DISPLAY_DURATION = 3000L // 3 seconds
```

#### Step 2.2: Start Overlay Timeout When Overlay is Shown

In `createSystemOverlay()` after overlay is successfully added:
```kotlin
// After overlay is shown successfully
overlayStartTime = System.currentTimeMillis()
startOverlayTimeoutTimer()
```

#### Step 2.3: Create Timeout Timer Method

```kotlin
private fun startOverlayTimeoutTimer() {
    Log.d(TAG, "Starting overlay 40-second timeout monitoring")
    
    overlayTimeoutHandler = Handler(Looper.getMainLooper())
    overlayTimeoutRunnable = Runnable {
        val elapsedTime = System.currentTimeMillis() - overlayStartTime
        
        if (elapsedTime >= OVERLAY_TIMEOUT_DURATION && isOverlayActive) {
            Log.w(TAG, "⏰ Overlay has been active for 40 seconds - showing failure message")
            handleOverlayTimeout()
        }
    }
    
    overlayTimeoutHandler?.postDelayed(overlayTimeoutRunnable!!, OVERLAY_TIMEOUT_DURATION)
}
```

#### Step 2.4: Handle Overlay Timeout

```kotlin
private fun handleOverlayTimeout() {
    Log.w(TAG, "=== OVERLAY TIMEOUT TRIGGERED ===")
    
    try {
        // Update overlay UI to show error state
        updateOverlayToErrorState()
        
        // Terminate the call
        Log.d(TAG, "Terminating call due to timeout...")
        callManager?.restoreCallVolume()
        val terminated = callManager?.terminateCall() ?: false
        Log.d(TAG, "Call termination result: $terminated")
        
        // Keep overlay visible for a few seconds with error message
        Handler(Looper.getMainLooper()).postDelayed({
            Log.d(TAG, "Dismissing overlay after error message display")
            hideOverlayInternal()
            
            // Show timeout dialog
            dialogManager?.showTransactionTimeout()
            
            // Stop service
            Handler(Looper.getMainLooper()).postDelayed({
                stopSelf()
            }, 500)
            
        }, ERROR_MESSAGE_DISPLAY_DURATION)
        
    } catch (e: Exception) {
        Log.e(TAG, "Error handling overlay timeout: ${e.message}", e)
        // Fallback: just hide overlay and show dialog
        hideOverlayInternal()
        dialogManager?.showTransactionTimeout()
        stopSelf()
    }
}
```

#### Step 2.5: Update Overlay UI to Error State

```kotlin
private fun updateOverlayToErrorState() {
    Log.d(TAG, "Updating overlay to show error state")
    
    try {
        overlayView?.let { view ->
            // Stop any ongoing animations
            progressRunnable?.let {
                Handler(Looper.getMainLooper()).removeCallbacks(it)
            }
            
            // Update status text to show error
            val statusText = view.findViewById<TextView>(R.id.statusText)
            statusText?.apply {
                text = "Transaction Failed"
                setTextColor(Color.parseColor("#FF6B6B")) // Red color
                textSize = 18f
                animate()
                    .scaleX(1.1f)
                    .scaleY(1.1f)
                    .setDuration(200)
                    .withEndAction {
                        animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(200)
                            .start()
                    }
                    .start()
            }
            
            // Update step text
            val stepText = view.findViewById<TextView>(R.id.stepText)
            stepText?.apply {
                text = "Please try again later"
                setTextColor(Color.parseColor("#FFCCCCCC"))
            }
            
            // Stop progress bar animation
            val progressBar = view.findViewById<android.widget.ProgressBar>(R.id.progressBar)
            progressBar?.apply {
                clearAnimation()
                // Optionally set progress to 0 or keep current
            }
            
            // Update progress percent
            val progressPercent = view.findViewById<TextView>(R.id.progressPercent)
            progressPercent?.apply {
                text = "Failed"
                setTextColor(Color.parseColor("#FF6B6B"))
            }
            
            // Add visual feedback - shake animation
            view.animate()
                .translationX(-10f)
                .setDuration(50)
                .withEndAction {
                    view.animate()
                        .translationX(10f)
                        .setDuration(50)
                        .withEndAction {
                            view.animate()
                                .translationX(-10f)
                                .setDuration(50)
                                .withEndAction {
                                    view.animate()
                                        .translationX(0f)
                                        .setDuration(50)
                                        .start()
                                }
                                .start()
                        }
                        .start()
                }
                .start()
        }
        
    } catch (e: Exception) {
        Log.e(TAG, "Error updating overlay to error state: ${e.message}")
    }
}
```

#### Step 2.6: Cancel Timeout Timer When Overlay is Dismissed Normally

In `hideOverlayInternal()`:
```kotlin
private fun hideOverlayInternal() {
    // ... existing code ...
    
    // Cancel overlay timeout timer
    overlayTimeoutRunnable?.let {
        overlayTimeoutHandler?.removeCallbacks(it)
    }
    overlayTimeoutHandler = null
    overlayTimeoutRunnable = null
    
    // ... rest of existing code ...
}
```

#### Step 2.7: Cancel Timeout Timer When Call Ends Naturally

In `handleCallEnded()`:
```kotlin
private fun handleCallEnded(reason: CallStateMonitor.CallEndReason) {
    // Cancel overlay timeout timer since call ended naturally
    overlayTimeoutRunnable?.let {
        overlayTimeoutHandler?.removeCallbacks(it)
    }
    
    // ... rest of existing code ...
}
```

---

## Coordination Between Systems

### CallDurationMonitor + CallOverlayService

Both systems will have 40-second timers, but they serve different purposes:

1. **CallDurationMonitor (MainActivityHelper)**:
   - Monitors call duration from transfer button press
   - Shows timeout **dialog** if call exceeds 40 seconds
   - Terminates call
   - Dismisses overlay via CallManager

2. **CallOverlayService**:
   - Monitors overlay display time from when overlay appears
   - Updates **overlay UI** to show error message at 40 seconds
   - Terminates call
   - Self-dismisses after showing error

### Race Condition Handling

**Scenario**: Both timers reach 40 seconds at the same time

**Solution**: Whichever fires first will:
1. Terminate the call
2. When call terminates, both systems will detect CALL_STATE_IDLE
3. Overlay will dismiss normally
4. Only one timeout dialog will show (guarded by dialog manager)

**Additional Safety**: In `MainActivityHelper` timeout callback:
```kotlin
onCallTimeout = {
    // Only proceed if overlay is still active
    if (CallOverlayService.isOverlayActive()) {
        callManager?.hideCallOverlay()
        callManager?.terminateCall()
        uiCallback.showCallTimeoutDialog()
    }
}
```

---

## Testing Scenarios

### Scenario 1: Call ends at 20 seconds (before 25s)
- ❌ 25s timer not reached
- ❌ 40s timer not reached
- **Result**: Shows "Call Duration Issue" dialog ✅

### Scenario 2: Call ends at 30 seconds (between 25s and 40s)
- ✅ 25s timer completed, `hasPendingSuccessDialog = true`
- ❌ 40s timer not reached
- Call ends naturally
- **Result**: Shows "Success" dialog ✅

### Scenario 3: Call ends at 45 seconds (after 40s timeout)
- ✅ 25s timer completed, `hasPendingSuccessDialog = true`
- ✅ 40s timer triggers
  - Sets `hasPendingSuccessDialog = false`
  - Invokes timeout callback
  - OR overlay timeout triggers first, shows error in overlay
- **Result**: Shows "Transaction Timeout" dialog ONLY ✅
- **Overlay**: Shows error message, then dismisses ✅

### Scenario 4: User terminates call at 35 seconds (via terminate button)
- ✅ 25s timer completed
- ❌ 40s timer not reached
- User presses terminate button
- **Result**: Shows "Transaction Cancelled by User" dialog ✅

---

## Files to Modify

### Part 1: Fix Dialog Conflict
1. ✅ `CallDurationMonitor.kt` - Add state management to prevent both dialogs

### Part 2: Overlay Timeout UI
2. ✅ `CallOverlayService.kt` - Add overlay timeout timer and UI updates

### Part 3: Coordination (Optional Enhancement)
3. ⚠️ `MainActivityHelper.kt` - Guard against race conditions (optional)

---

## Implementation Order

1. **First**: Fix Part 1 (Dialog Conflict)
   - Modify `CallDurationMonitor.kt` to use pending success flag
   - Test: Ensure only one dialog appears when call exceeds 40s

2. **Second**: Implement Part 2 (Overlay Timeout)
   - Add timeout timer to `CallOverlayService.kt`
   - Add `updateOverlayToErrorState()` method
   - Test: Verify overlay shows error message and dismisses

3. **Third**: Integration Testing
   - Test all scenarios listed above
   - Verify no race conditions
   - Verify smooth transitions

---

## UI/UX Considerations

### Overlay Error State Design
- **Status**: "Transaction Failed" (Red, size 18sp)
- **Step**: "Please try again later" (Light gray)
- **Progress**: "Failed" (Red)
- **Animation**: Gentle shake (4 cycles, 10px amplitude)
- **Duration**: 3 seconds before dismissal
- **Dismissal**: Fade out animation (same as normal)

### Dialog Priority
Only ONE dialog should appear:
1. If call < 25s → "Call Duration Issue"
2. If 25s ≤ call < 40s → "Success"
3. If call ≥ 40s → "Transaction Timeout" ONLY

---

## Rollback Plan

If issues occur:
1. Part 1 can be reverted by restoring original `CallDurationMonitor.kt`
2. Part 2 can be disabled by commenting out `startOverlayTimeoutTimer()` call
3. Both systems are independent and can be rolled back separately

---

## Success Criteria

✅ When call exceeds 40 seconds:
- Only ONE dialog appears (timeout dialog)
- No success dialog appears
- Overlay shows error message before dismissing
- Call is terminated gracefully
- No crashes or exceptions

✅ When call ends between 25-40 seconds:
- Success dialog appears
- No timeout dialog appears
- Overlay dismisses normally

✅ When call ends before 25 seconds:
- Call duration issue dialog appears
- No other dialogs appear


