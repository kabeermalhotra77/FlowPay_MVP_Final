package com.flowpay.app.managers

import android.content.Context
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import com.flowpay.app.services.CallOverlayService
import com.flowpay.app.utils.PhoneNumberUtils

/**
 * Monitors call state changes and determines the reason for call termination
 * to provide appropriate feedback to the user
 */
class CallStateMonitor(private val context: Context) {
    
    companion object {
        private const val TAG = "CallStateMonitor"
        private const val MIN_CALL_DURATION_FOR_SUCCESS = 30000L // 30 seconds
        private const val MIN_CALL_DURATION_FOR_CANCELLATION = 5000L // 5 seconds
    }
    
    enum class CallEndReason {
        USER_CANCELLED,    // User pressed red button (very short call)
        CALL_COMPLETED,    // Call lasted 30+ seconds
        CALL_FAILED,       // Call ended before 30 seconds but after 5 seconds
        SYSTEM_TERMINATED  // System ended the call
    }
    
    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    private var phoneStateListener: PhoneStateListener? = null
    private var callStartTime = 0L
    private var isCallActive = false
    private var onCallEnded: ((CallEndReason) -> Unit)? = null
    private var overlayService: CallOverlayService? = null
    private var upiServiceNumber: String? = null
    
    /**
     * Set the overlay service for integrated call detection
     */
    fun setOverlayService(service: CallOverlayService?) {
        this.overlayService = service
    }
    
    /**
     * Set the UPI service number for call detection
     */
    fun setUpiServiceNumber(serviceNumber: String?) {
        this.upiServiceNumber = serviceNumber
    }
    
    /**
     * Start monitoring call state changes
     */
    fun startMonitoring(onCallEnded: (CallEndReason) -> Unit) {
        this.onCallEnded = onCallEnded
        Log.d(TAG, "Starting call state monitoring")
        
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
                        if (!isCallActive) {
                            isCallActive = true
                            callStartTime = System.currentTimeMillis()
                            Log.d(TAG, "Call started at: $callStartTime")
                            
                            // Check if this is a UPI call and notify overlay service
                            if (isUpiCall(phoneNumber)) {
                                Log.d(TAG, "UPI call detected - notifying overlay service")
                                overlayService?.onCallDetected()
                            }
                        }
                    }
                    TelephonyManager.CALL_STATE_IDLE -> {
                        if (isCallActive) {
                            isCallActive = false
                            val callDuration = System.currentTimeMillis() - callStartTime
                            val reason = determineCallEndReason(callDuration)
                            Log.d(TAG, "Call ended after ${callDuration}ms, reason: $reason")
                            onCallEnded?.invoke(reason)
                        }
                    }
                    TelephonyManager.CALL_STATE_RINGING -> {
                        // Call is ringing, do nothing
                    }
                }
            }
        }
        
        try {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start call state monitoring: ${e.message}")
        }
    }
    
    /**
     * Determine the reason for call termination based on duration
     */
    private fun determineCallEndReason(duration: Long): CallEndReason {
        return when {
            duration < MIN_CALL_DURATION_FOR_CANCELLATION -> {
                Log.d(TAG, "Very short call duration ($duration ms) - likely user cancelled")
                CallEndReason.USER_CANCELLED
            }
            duration >= MIN_CALL_DURATION_FOR_SUCCESS -> {
                Log.d(TAG, "Call completed successfully ($duration ms)")
                CallEndReason.CALL_COMPLETED
            }
            else -> {
                Log.d(TAG, "Call failed or ended unexpectedly ($duration ms)")
                CallEndReason.CALL_FAILED
            }
        }
    }
    
    /**
     * Stop monitoring call state changes
     */
    fun stopMonitoring() {
        Log.d(TAG, "Stopping call state monitoring")
        try {
            phoneStateListener?.let {
                telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop call state monitoring: ${e.message}")
        }
        phoneStateListener = null
        isCallActive = false
        onCallEnded = null
    }
    
    /**
     * Check if a call is currently active
     */
    fun isCallActive(): Boolean = isCallActive
    
    /**
     * Get the current call duration if call is active
     */
    fun getCurrentCallDuration(): Long {
        return if (isCallActive) {
            System.currentTimeMillis() - callStartTime
        } else {
            0L
        }
    }
    
    /**
     * Check if the incoming call is from UPI service
     * For UPI123 calls, phone number is often blank, so we check if we're in a UPI call context
     */
    private fun isUpiCall(phoneNumber: String?): Boolean {
        // If we have a valid UPI service number and phone number matches, it's definitely a UPI call
        if (!phoneNumber.isNullOrBlank() && !upiServiceNumber.isNullOrBlank()) {
            return PhoneNumberUtils.isPhoneNumberMatch(phoneNumber, upiServiceNumber)
        }
        
        // For UPI123 calls, phone number is often blank, so we need to check if overlay service
        // is active and expecting a UPI call (this indicates we're in a UPI123 call context)
        if (phoneNumber.isNullOrBlank() && overlayService != null) {
            Log.d(TAG, "Blank phone number detected - checking if this is a UPI123 call context")
            // If overlay service is active and expecting a UPI call, assume this is a UPI call
            val service = overlayService
            return service?.isActiveAndExpectingUpiCall() ?: false
        }
        
        return false
    }
}
