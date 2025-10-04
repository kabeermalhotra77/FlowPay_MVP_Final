package com.flowpay.app

import android.content.Context
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import kotlinx.coroutines.*

class CallDurationMonitor(private val context: Context) {
    private var telephonyManager: TelephonyManager? = null
    private var phoneStateListener: PhoneStateListener? = null
    private var monitoringJob: Job? = null
    private var isMonitoring = false
    private var timerStartTime: Long = 0
    private var callHasStarted = false
    private var callHasEnded = false
    private var hasTriggeredCallback = false // Add this flag
    
    companion object {
        private const val TAG = "CallDurationMonitor"
        private const val TIMER_DURATION_MS = 25000L // 25 seconds
    }
    
    interface CallDurationCallback {
        fun onCallEndedBeforeTimer()
        fun onTimerCompleted()
    }
    
    private var callback: CallDurationCallback? = null
    
    fun setCallback(callback: CallDurationCallback) {
        Log.d(TAG, "✅ Callback set successfully")
        this.callback = callback
    }
    
    fun startMonitoring() {
        Log.d(TAG, "🟢 START MONITORING - Timer begins NOW at ${System.currentTimeMillis()}")
        
        // Reset all states
        callHasStarted = false
        callHasEnded = false
        isMonitoring = true
        timerStartTime = System.currentTimeMillis()
        
        Log.d(TAG, "📱 States reset - callHasStarted: false, callHasEnded: false, isMonitoring: true")
        
        // Initialize telephony manager
        telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        
        if (telephonyManager == null) {
            Log.e(TAG, "❌ ERROR: TelephonyManager is null!")
            return
        } else {
            Log.d(TAG, "✅ TelephonyManager initialized successfully")
        }
        
        // Setup phone state listener
        phoneStateListener = object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                super.onCallStateChanged(state, phoneNumber)
                
                Log.d(TAG, "📞 CALL STATE CHANGED: ${getStateName(state)} | Phone: $phoneNumber")
                
                when (state) {
                    TelephonyManager.CALL_STATE_OFFHOOK -> {
                        Log.d(TAG, "📞 OFFHOOK - Call active/dialing")
                        
                        if (!callHasStarted && isMonitoring) {
                            callHasStarted = true
                            val currentTime = System.currentTimeMillis()
                            Log.d(TAG, "✅ Call STARTED - Monitoring active")
                            Log.d(TAG, "⏱️ Time since timer start: ${currentTime - timerStartTime}ms")
                        } else {
                            Log.d(TAG, "ℹ️ OFFHOOK but already started or not monitoring")
                        }
                    }
                    
                    TelephonyManager.CALL_STATE_IDLE -> {
                        Log.d(TAG, "📞 IDLE - Call ended or no call")
                        
                        if (callHasStarted && !callHasEnded && isMonitoring) {
                            callHasEnded = true
                            val currentTime = System.currentTimeMillis()
                            val elapsedTime = currentTime - timerStartTime
                            
                            Log.d(TAG, "🔴 CALL ENDED!")
                            Log.d(TAG, "⏱️ Total elapsed time: ${elapsedTime}ms (${elapsedTime/1000} seconds)")
                            Log.d(TAG, "⏱️ Timer threshold: ${TIMER_DURATION_MS}ms (25 seconds)")
                            
                            if (elapsedTime < TIMER_DURATION_MS) {
                                Log.d(TAG, "🚨 TRIGGER DIALOG - Call ended BEFORE 25 seconds!")
                                Log.d(TAG, "🚨 Calling onCallEndedBeforeTimer callback...")
                                
                                handleCallEndedBeforeTimer()
                            } else {
                                Log.d(TAG, "✅ Call lasted MORE than 25 seconds - Normal completion")
                            }
                            
                            stopMonitoring()
                        } else {
                            Log.d(TAG, "ℹ️ IDLE but conditions not met:")
                            Log.d(TAG, "   - callHasStarted: $callHasStarted")
                            Log.d(TAG, "   - callHasEnded: $callHasEnded")
                            Log.d(TAG, "   - isMonitoring: $isMonitoring")
                        }
                    }
                    
                    TelephonyManager.CALL_STATE_RINGING -> {
                        Log.d(TAG, "📞 RINGING - Incoming call (ignoring)")
                    }
                }
            }
        }
        
        // Register the listener
        try {
            telephonyManager?.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
            Log.d(TAG, "✅ PhoneStateListener registered successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ ERROR registering PhoneStateListener", e)
        }
        
        // Start the 25-second timer coroutine
        Log.d(TAG, "⏰ Starting 25-second timer coroutine...")
        
        monitoringJob = GlobalScope.launch {
            try {
                Log.d(TAG, "⏰ Timer coroutine started, waiting 25 seconds...")
                delay(TIMER_DURATION_MS)
                
                if (isMonitoring && !callHasEnded) {
                    Log.d(TAG, "⏰ 25-SECOND TIMER COMPLETED - Call still ongoing or completed successfully")
                    
                    withContext(Dispatchers.Main) {
                        try {
                            Log.d(TAG, "📢 Calling onTimerCompleted callback...")
                            callback?.onTimerCompleted()
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ ERROR in timer completed callback", e)
                        }
                    }
                    stopMonitoring()
                } else {
                    Log.d(TAG, "⏰ Timer completed but monitoring stopped or call already ended")
                }
            } catch (e: CancellationException) {
                Log.d(TAG, "⏰ Timer cancelled")
            } catch (e: Exception) {
                Log.e(TAG, "❌ ERROR in timer coroutine", e)
            }
        }
    }
    
    private fun handleCallEndedBeforeTimer() {
        Log.d(TAG, "🔔 handleCallEndedBeforeTimer called")
        
        // Prevent multiple callbacks
        if (hasTriggeredCallback) {
            Log.d(TAG, "ℹ️ Callback already triggered, skipping")
            return
        }
        
        if (callback == null) {
            Log.e(TAG, "❌ ERROR: Callback is NULL! Cannot show dialog!")
            return
        }
        
        hasTriggeredCallback = true // Set flag before triggering
        
        GlobalScope.launch(Dispatchers.Main) {
            try {
                Log.d(TAG, "🔔 Executing callback ONCE on Main thread...")
                callback?.onCallEndedBeforeTimer()
                Log.d(TAG, "✅ Callback executed successfully!")
            } catch (e: Exception) {
                Log.e(TAG, "❌ ERROR calling callback", e)
            }
        }
    }
    
    private fun getStateName(state: Int): String {
        return when (state) {
            TelephonyManager.CALL_STATE_IDLE -> "IDLE"
            TelephonyManager.CALL_STATE_RINGING -> "RINGING"
            TelephonyManager.CALL_STATE_OFFHOOK -> "OFFHOOK"
            else -> "UNKNOWN"
        }
    }
    
    fun stopMonitoring() {
        Log.d(TAG, "🛑 STOP MONITORING called")
        
        isMonitoring = false
        
        // Cancel the timer
        monitoringJob?.cancel()
        monitoringJob = null
        Log.d(TAG, "✅ Timer cancelled")
        
        // Unregister phone state listener
        phoneStateListener?.let {
            telephonyManager?.listen(it, PhoneStateListener.LISTEN_NONE)
            Log.d(TAG, "✅ PhoneStateListener unregistered")
        }
        
        phoneStateListener = null
        telephonyManager = null
        
        // Reset states
        callHasStarted = false
        callHasEnded = false
        
        Log.d(TAG, "✅ Monitoring stopped and cleaned up")
    }
    
    fun cleanup() {
        Log.d(TAG, "🧹 CLEANUP called")
        stopMonitoring()
        callback = null
    }
}