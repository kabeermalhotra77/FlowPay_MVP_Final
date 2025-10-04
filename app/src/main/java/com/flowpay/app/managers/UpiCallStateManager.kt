package com.flowpay.app.managers

import android.util.Log
import com.flowpay.app.constants.AppConstants

/**
 * Simplified UPI call state manager that replaces the complex UpiCallState class
 * while maintaining exact same interface for overlay compatibility
 */
class UpiCallStateManager {
    companion object {
        private const val TAG = "UpiCallStateManager"
    }
    
    // Simplified state tracking - keeping only essential variables
    @Volatile private var _isInProgress = false
    @Volatile private var _startTime = 0L
    @Volatile private var _serviceNumber = ""
    @Volatile private var _amount = ""
    
    /**
     * Start UPI call tracking
     */
    fun startCall(serviceNumber: String, amount: String) {
        synchronized(this) {
            _isInProgress = true
            _startTime = System.currentTimeMillis()
            _serviceNumber = serviceNumber
            _amount = amount
            Log.d(TAG, "UPI call started - Service: $serviceNumber, Amount: $amount")
        }
    }
    
    /**
     * End UPI call tracking
     */
    fun endCall() {
        synchronized(this) {
            _isInProgress = false
            _startTime = 0L
            _serviceNumber = ""
            _amount = ""
            Log.d(TAG, "UPI call ended - state reset")
        }
    }
    
    /**
     * Check if UPI call is active
     */
    fun isActive(): Boolean = synchronized(this) { 
        _isInProgress && _serviceNumber.isNotEmpty() 
    }
    
    /**
     * Check if call is within timeout window
     */
    fun isWithinTimeout(): Boolean = synchronized(this) {
        _isInProgress && (System.currentTimeMillis() - _startTime) < AppConstants.UPI_CALL_TIMEOUT
    }
    
    // Getters for overlay compatibility - maintaining exact same interface
    fun getServiceNumber(): String = synchronized(this) { _serviceNumber }
    fun getAmount(): String = synchronized(this) { _amount }
    fun getStartTime(): Long = synchronized(this) { _startTime }
    
    // Legacy compatibility properties
    val phoneNumber: String get() = getServiceNumber()
    val isInProgress: Boolean get() = _isInProgress
    
    /**
     * Get debug information for logging
     */
    fun getDebugInfo(): String {
        return synchronized(this) {
            "UpiCallStateManager(isInProgress=$_isInProgress, startTime=$_startTime, " +
            "serviceNumber='$_serviceNumber', amount='$_amount', isActive=${isActive()}, " +
            "isWithinTimeout=${isWithinTimeout()})"
        }
    }
}
