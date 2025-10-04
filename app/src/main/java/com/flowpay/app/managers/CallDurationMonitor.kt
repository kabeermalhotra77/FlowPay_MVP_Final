package com.flowpay.app.managers

import android.content.Context
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import kotlinx.coroutines.*

/**
 * Monitors call duration for manual transfers to detect if call ends before 25 seconds
 * which indicates payment limit exceeded or configuration issues
 */
class CallDurationMonitor(private val context: Context) {
    
    companion object {
        private const val TAG = "CallDurationMonitor"
        private const val TIMER_DURATION_SECONDS = 25L // 25 seconds timer from transfer button press
    }
    
    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    private var phoneStateListener: PhoneStateListener? = null
    private var timerStartTime = 0L
    private var isTimerActive = false
    private var isCallActive = false
    private var onCallDurationIssue: (() -> Unit)? = null
    private var onCallSuccessful: (() -> Unit)? = null
    private var timerJob: Job? = null
    private var currentCallType: String? = null
    
    /**
     * Start 25-second timer from transfer button press
     */
    fun startTimer(
        callType: String = "MANUAL_TRANSFER",
        onCallDurationIssue: () -> Unit,
        onCallSuccessful: () -> Unit
    ) {
        this.onCallDurationIssue = onCallDurationIssue
        this.onCallSuccessful = onCallSuccessful
        this.currentCallType = callType
        
        Log.d(TAG, "Starting 25-second timer from transfer button press for call type: $callType")
        
        timerStartTime = System.currentTimeMillis()
        isTimerActive = true
        
        // Start the 25-second timer
        timerJob = CoroutineScope(Dispatchers.Main).launch {
            delay(TIMER_DURATION_SECONDS * 1000) // Wait for 25 seconds
            
            // If timer completes and call is still active, payment is likely successful
            if (isTimerActive && isCallActive) {
                Log.d(TAG, "✅ 25-SECOND TIMER COMPLETED WITH CALL STILL ACTIVE")
                Log.d(TAG, "📞 Call type: $currentCallType")
                Log.d(TAG, "💰 Payment likely successful - no dialog needed")
                try {
                    onCallSuccessful?.invoke()
                    Log.d(TAG, "✅ Success callback executed")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error in call successful callback: ${e.message}")
                }
                stopTimer()
            } else if (isTimerActive) {
                Log.d(TAG, "⏰ 25-second timer completed but call not active - monitoring stopped")
                stopTimer()
            }
        }
        
        // Start monitoring call state
        startCallStateMonitoring()
    }
    
    /**
     * Start monitoring call state changes
     */
    private fun startCallStateMonitoring() {
        phoneStateListener = object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                val stateName = when (state) {
                    TelephonyManager.CALL_STATE_IDLE -> "IDLE"
                    TelephonyManager.CALL_STATE_RINGING -> "RINGING"
                    TelephonyManager.CALL_STATE_OFFHOOK -> "OFFHOOK"
                    else -> "UNKNOWN($state)"
                }
                Log.d(TAG, "Call state changed: $stateName, phoneNumber: $phoneNumber")
                
                when (state) {
                    TelephonyManager.CALL_STATE_OFFHOOK -> {
                        if (!isCallActive && currentCallType == "MANUAL_TRANSFER") {
                            isCallActive = true
                            Log.d(TAG, "Manual transfer call started - monitoring for early termination")
                        } else if (!isCallActive) {
                            Log.d(TAG, "Call started but not a manual transfer (type: $currentCallType) - ignoring")
                        }
                    }
                    TelephonyManager.CALL_STATE_IDLE -> {
                        if (isCallActive) {
                            isCallActive = false
                            val elapsedTime = System.currentTimeMillis() - timerStartTime
                            val elapsedSeconds = elapsedTime / 1000
                            Log.d(TAG, "Call ended after ${elapsedSeconds} seconds from timer start")
                            
                            // Check if call ended before 25-second timer
                            if (isTimerActive && elapsedSeconds < TIMER_DURATION_SECONDS) {
                                Log.w(TAG, "🚨 MANUAL TRANSFER CALL ENDED BEFORE 25 SECONDS!")
                                Log.w(TAG, "⏱️ Elapsed time: ${elapsedSeconds}s (required: ${TIMER_DURATION_SECONDS}s)")
                                Log.w(TAG, "📞 Call type: $currentCallType")
                                Log.w(TAG, "🔔 Triggering dialog callback...")
                                try {
                                    onCallDurationIssue?.invoke()
                                    Log.d(TAG, "✅ Dialog callback executed successfully")
                                } catch (e: Exception) {
                                    Log.e(TAG, "❌ Error in call duration issue callback: ${e.message}")
                                }
                                stopTimer()
                            } else if (isTimerActive) {
                                Log.d(TAG, "✅ Call ended after 25+ seconds - normal completion")
                            }
                        }
                    }
                    TelephonyManager.CALL_STATE_RINGING -> {
                        // Call is ringing
                    }
                }
            }
        }
        
        try {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
            Log.d(TAG, "Phone state listener registered for call monitoring")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start call state monitoring: ${e.message}")
        }
    }
    
    /**
     * Stop the timer and monitoring
     */
    private fun stopTimer() {
        isTimerActive = false
        timerJob?.cancel()
        timerJob = null
    }
    
    /**
     * Stop monitoring call duration
     */
    fun stopMonitoring() {
        Log.d(TAG, "Stopping call duration monitoring")
        
        stopTimer()
        
        try {
            phoneStateListener?.let {
                telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop call duration monitoring: ${e.message}")
        }
        
        phoneStateListener = null
        isCallActive = false
        isTimerActive = false
        onCallDurationIssue = null
        onCallSuccessful = null
        currentCallType = null
    }
    
    /**
     * Check if timer is currently active
     */
    fun isTimerActive(): Boolean = isTimerActive
    
    /**
     * Check if call is currently active
     */
    fun isCallActive(): Boolean = isCallActive
    
    /**
     * Get elapsed time since timer started in seconds
     */
    fun getElapsedTime(): Long {
        return if (isTimerActive) {
            (System.currentTimeMillis() - timerStartTime) / 1000
        } else {
            0L
        }
    }
    
    /**
     * Cleanup method for proper resource management
     */
    fun cleanup() {
        Log.d(TAG, "Cleaning up CallDurationMonitor")
        stopMonitoring()
    }
}
