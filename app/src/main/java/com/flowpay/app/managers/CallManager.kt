package com.flowpay.app.managers

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.telecom.TelecomManager
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.compose.runtime.mutableStateOf
import com.flowpay.app.constants.PermissionConstants
import com.flowpay.app.constants.AppConstants

enum class CallType {
    USSD, VOICE, UPI123, MANUAL_TRANSFER
}

class CallManager(private val context: Context) {
    
    companion object {
        private const val TAG = "CallManager"
    }
    
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    
    private var originalCallVolume: Int = 0
    private var originalMediaVolume: Int = 0
    private var wasRingerNormal: Boolean = true
    private var previousAudioMode: Int = AudioManager.MODE_NORMAL
    private var isAudioMuted = false
    
    // Call state tracking - thread-safe with proper synchronization
    private var _isCallInProgress = mutableStateOf(false)
    @Volatile
    private var currentCallType: CallType? = null
    private var onCallEndedCallback: ((CallType) -> Unit)? = null
    private var onUssdSessionComplete: (() -> Unit)? = null
    private var phoneStateListener: PhoneStateListener? = null
    
    // Synchronization objects for thread safety
    private val audioLock = Any()
    private val callStateLock = Any()
    private val callbackLock = Any()
    private val listenerLock = Any()
    
    private val prefs = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * Gets the configured UPI service number or default
     */
    private fun getUpiServiceNumber(): String {
        return prefs.getString(AppConstants.KEY_UPI_SERVICE_NUMBER, AppConstants.DEFAULT_UPI_SERVICE_NUMBER) ?: AppConstants.DEFAULT_UPI_SERVICE_NUMBER
    }
    
    /**
     * Sets the UPI service number with validation
     */
    fun setUpiServiceNumber(serviceNumber: String) {
        val sanitizedNumber = sanitizePhoneNumber(serviceNumber)
        if (isValidPhoneNumber(sanitizedNumber)) {
            prefs.edit().putString(AppConstants.KEY_UPI_SERVICE_NUMBER, sanitizedNumber).apply()
            Log.d(TAG, "UPI service number updated to: $sanitizedNumber")
        } else {
            Log.e(TAG, "Invalid UPI service number: $serviceNumber")
            throw IllegalArgumentException("Invalid UPI service number format")
        }
    }
    
    /**
     * Gets the current UPI service number
     */
    fun getCurrentUpiServiceNumber(): String = getUpiServiceNumber()
    
    /**
     * Thread-safe callback management
     */
    private fun setCallEndedCallback(callback: ((CallType) -> Unit)?) {
        synchronized(callbackLock) {
            onCallEndedCallback = callback
        }
    }
    
    private fun setUssdSessionCompleteCallback(callback: (() -> Unit)?) {
        synchronized(callbackLock) {
            onUssdSessionComplete = callback
        }
    }
    
    private fun getCallEndedCallback(): ((CallType) -> Unit)? {
        synchronized(callbackLock) {
            return onCallEndedCallback
        }
    }
    
    private fun getUssdSessionCompleteCallback(): (() -> Unit)? {
        synchronized(callbackLock) {
            return onUssdSessionComplete
        }
    }
    
    private fun clearCallbacks() {
        synchronized(callbackLock) {
            onCallEndedCallback = null
            onUssdSessionComplete = null
        }
    }
    
    /**
     * Initiates a call - simplified without cycle tracking
     */
    fun initiateCall(
        context: Context,
        phoneNumber: String,
        callType: CallType,
        onCallEnded: (CallType) -> Unit,
        onUssdComplete: (() -> Unit)? = null
    ) {
        // Check permissions using centralized constants
        if (ContextCompat.checkSelfPermission(context, PermissionConstants.CRITICAL_PERMISSIONS[0]) != 
            android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "CALL_PHONE permission not granted")
            return
        }
        
        // Handle USSD calls using ACTION_CALL (simplified)
        if (callType == CallType.USSD) {
            handleUSSDCall(phoneNumber, onUssdComplete)
            return
        }
        
        // Set up call tracking with thread safety
        synchronized(callStateLock) {
            currentCallType = callType
            setCallEndedCallback(onCallEnded)
            _isCallInProgress.value = true
        }
        
        // Create and register phone state listener for non-USSD calls
        synchronized(listenerLock) {
            phoneStateListener = createPhoneStateListener(onCallEnded)
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
        }
        
        // Initiate the call
        val intent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:$phoneNumber")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        
        try {
            context.startActivity(intent)
            Log.d(TAG, "Call initiated: $callType to $phoneNumber")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initiate call: ${e.message}")
            synchronized(callStateLock) {
                _isCallInProgress.value = false
                currentCallType = null
                clearCallbacks()
            }
        }
    }
    
    /**
     * Handle USSD calls using ACTION_CALL (simplified)
     */
    private fun handleUSSDCall(ussdCode: String, onUssdComplete: (() -> Unit)?) {
        // Set up USSD callback
        setUssdSessionCompleteCallback(onUssdComplete)
        _isCallInProgress.value = true
        currentCallType = CallType.USSD
        
        Log.d(TAG, "Starting USSD call: $ussdCode")
        
        
        // Create and register phone state listener for USSD calls
        synchronized(listenerLock) {
            phoneStateListener = createPhoneStateListener { callType ->
                if (callType == CallType.USSD) {
                    synchronized(callStateLock) {
                        _isCallInProgress.value = false
                        currentCallType = null
                        getUssdSessionCompleteCallback()?.invoke()
                        clearCallbacks()
                    }
                }
            }
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
        }
        
        // Initiate the USSD call using ACTION_CALL
        // URL encode the USSD code to preserve special characters like #
        val encodedUssdCode = Uri.encode(ussdCode)
        val intent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:$encodedUssdCode")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        
        try {
            context.startActivity(intent)
            Log.d(TAG, "USSD call initiated: $ussdCode")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initiate USSD call: ${e.message}")
            synchronized(callStateLock) {
                _isCallInProgress.value = false
                currentCallType = null
                getUssdSessionCompleteCallback()?.invoke()
                clearCallbacks()
            }
        }
    }
    
    /**
     * Creates phone state listener for non-USSD calls
     */
    private fun createPhoneStateListener(onCallEnded: (CallType) -> Unit): PhoneStateListener {
        return object : PhoneStateListener() {
            @Deprecated("Deprecated in Java")
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                val stateName = when (state) {
                    TelephonyManager.CALL_STATE_IDLE -> "IDLE"
                    TelephonyManager.CALL_STATE_RINGING -> "RINGING"
                    TelephonyManager.CALL_STATE_OFFHOOK -> "OFFHOOK"
                    else -> "UNKNOWN($state)"
                }
                Log.d(TAG, "Call State: $stateName")
                
                when (state) {
                    TelephonyManager.CALL_STATE_IDLE -> {
                        if (_isCallInProgress.value) {
                            // Call ended
                            synchronized(callStateLock) {
                                _isCallInProgress.value = false
                                currentCallType?.let { callType ->
                                    getCallEndedCallback()?.invoke(callType)
                                }
                                currentCallType = null
                                clearCallbacks()
                            }
                        }
                    }
                    TelephonyManager.CALL_STATE_OFFHOOK -> {
                        if (!_isCallInProgress.value) {
                            _isCallInProgress.value = true
                        }
                    }
                    TelephonyManager.CALL_STATE_RINGING -> {
                        // Call is ringing
                    }
                }
            }
        }
    }
    
    /**
     * Ends current call
     */
    fun endCall() {
        synchronized(callStateLock) {
            _isCallInProgress.value = false
            currentCallType = null
            clearCallbacks()
        }
        
        // Unregister phone state listener
        synchronized(listenerLock) {
            phoneStateListener?.let { listener ->
                telephonyManager.listen(listener, PhoneStateListener.LISTEN_NONE)
            }
            phoneStateListener = null
        }
    }
    
    /**
     * Checks if call is in progress
     */
    fun isCallInProgress(): Boolean = _isCallInProgress.value
    
    /**
     * Constructs the UPI123 call string in the format: tel:<serviceNumber>,,1,<phoneNumber>,,<amount>,,1
     * with proper input validation and sanitization
     */
    fun constructUPI123CallString(phoneNumber: String, amount: String): String {
        // Validate inputs before construction
        if (!isValidPhoneNumber(phoneNumber)) {
            throw IllegalArgumentException("Invalid phone number format: $phoneNumber")
        }
        if (!isValidAmount(amount)) {
            throw IllegalArgumentException("Invalid amount format: $amount")
        }
        
        val serviceNumber = getUpiServiceNumber()
        val sanitizedPhone = sanitizePhoneNumber(phoneNumber)
        val sanitizedAmount = sanitizeAmount(amount)
        
        // Additional security: escape any special characters that could be used for injection
        val escapedPhone = sanitizedPhone.replace(Regex("[^0-9]"), "")
        val escapedAmount = sanitizedAmount.replace(Regex("[^0-9.]"), "")
        
        return "tel:$serviceNumber,,1,$escapedPhone,,$escapedAmount,,1"
    }
    
    
    /**
     * Sanitizes phone number input (digits only)
     */
    private fun sanitizePhoneNumber(input: String): String {
        return input.replace(Regex("[^0-9]"), "")
    }
    
    /**
     * Sanitizes amount input (digits and single decimal point)
     */
    private fun sanitizeAmount(input: String): String {
        val digitsOnly = input.replace(Regex("[^0-9.]"), "")
        // Ensure only one decimal point
        val parts = digitsOnly.split(".")
        return if (parts.size > 2) {
            parts[0] + "." + parts.drop(1).joinToString("")
        } else {
            digitsOnly
        }
    }
    
    /**
     * Initiates a UPI123 call with the given phone number and amount
     */
    fun initiateUPI123Call(phoneNumber: String, amount: String): Boolean {
        Log.d("OverlayDebug", "=== CALLMANAGER INITIATE UPI123 CALL ===")
        Log.d("OverlayDebug", "Input phone: $phoneNumber, amount: $amount")
        
        return try {
            // Input validation
            if (phoneNumber.isNullOrBlank() || amount.isNullOrBlank()) {
                Log.e("OverlayDebug", "Phone number or amount is null or blank")
                Toast.makeText(context, "Please enter both phone number and amount", Toast.LENGTH_SHORT).show()
                return false
            }
            
            // Sanitize inputs with specific methods
            val sanitizedPhoneNumber = sanitizePhoneNumber(phoneNumber)
            val sanitizedAmount = sanitizeAmount(amount)
            
            // Validate sanitized inputs
            Log.d("OverlayDebug", "Sanitized phone: $sanitizedPhoneNumber, amount: $sanitizedAmount")
            
            if (!isValidPhoneNumber(sanitizedPhoneNumber)) {
                Log.e("OverlayDebug", "Invalid phone number format: $sanitizedPhoneNumber")
                Toast.makeText(context, "Please enter a valid 10-digit phone number", Toast.LENGTH_SHORT).show()
                return false
            }
            
            if (!isValidAmount(sanitizedAmount)) {
                Log.e("OverlayDebug", "Invalid amount format: $sanitizedAmount")
                Toast.makeText(context, "Please enter a valid amount (1-1000000)", Toast.LENGTH_SHORT).show()
                return false
            }
            
            val callString = constructUPI123CallString(sanitizedPhoneNumber, sanitizedAmount)
            Log.d("OverlayDebug", "Constructed call string: $callString")
            
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse(callString)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            
            val hasCallPermission = ContextCompat.checkSelfPermission(context, PermissionConstants.CRITICAL_PERMISSIONS[0]) == 
                PackageManager.PERMISSION_GRANTED
            Log.d("OverlayDebug", "CALL_PHONE permission granted: $hasCallPermission")
            
            if (hasCallPermission) {
                Log.d("OverlayDebug", "Starting call activity...")
                
                // Set up call state tracking for UPI123 calls
                synchronized(callStateLock) {
                    _isCallInProgress.value = true
                    currentCallType = CallType.MANUAL_TRANSFER
                    Log.d(TAG, "=== UPI123 CALL STATE TRACKING ENABLED ===")
                    Log.d(TAG, "_isCallInProgress.value: ${_isCallInProgress.value}")
                    Log.d(TAG, "currentCallType: $currentCallType")
                }
                
                // Note: Phone state monitoring for UPI123 calls is handled by CallStateMonitor
                // in CallOverlayService to avoid conflicts and ensure proper overlay dismissal
                Log.d(TAG, "=== UPI123 CALL STATE TRACKING ENABLED (CallStateMonitor will handle call end) ===")
                
                context.startActivity(intent)
                Log.d("OverlayDebug", "Call activity started successfully - returning true")
                true
            } else {
                Log.e("OverlayDebug", "CALL_PHONE permission not granted")
                Toast.makeText(context, "Phone call permission is required for manual payments. Please grant permission in Settings.", Toast.LENGTH_LONG).show()
                false
            }
        } catch (e: SecurityException) {
            Log.e("OverlayDebug", "Security exception while initiating call: ${e.message}")
            Toast.makeText(context, "Permission denied: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        } catch (e: IllegalArgumentException) {
            Log.e("OverlayDebug", "Invalid argument while initiating call: ${e.message}")
            Toast.makeText(context, "Invalid input: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        } catch (e: Exception) {
            Log.e("OverlayDebug", "Unexpected error while initiating UPI123 call: ${e.message}")
            Toast.makeText(context, "Failed to initiate call. Please try again.", Toast.LENGTH_SHORT).show()
            false
        }
    }
    
    /**
     * Mutes all audio streams for seamless call experience
     * @return true if successful, false otherwise
     */
    fun muteCallAudio(): Boolean {
        synchronized(audioLock) {
            return try {
            Log.d(TAG, "Attempting to mute call audio")
            
            // Store original audio settings with error checking
            originalCallVolume = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL)
            originalMediaVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            wasRingerNormal = audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL
            previousAudioMode = audioManager.mode
            
            // Set audio mode for call with validation
            audioManager.mode = AudioManager.MODE_IN_CALL
            if (audioManager.mode != AudioManager.MODE_IN_CALL) {
                Log.w(TAG, "Failed to set audio mode to MODE_IN_CALL")
                return false
            }
            
            // Mute microphone and speaker
            audioManager.isMicrophoneMute = true
            audioManager.isSpeakerphoneOn = false
            
            // Mute all audio streams with individual error handling
            val streams = listOf(
                AudioManager.STREAM_VOICE_CALL,
                AudioManager.STREAM_RING,
                AudioManager.STREAM_MUSIC,
                AudioManager.STREAM_ALARM,
                AudioManager.STREAM_NOTIFICATION
            )
            
            var allStreamsMuted = true
            streams.forEach { stream ->
                try {
                    audioManager.setStreamVolume(stream, 0, 0)
                    val currentVolume = audioManager.getStreamVolume(stream)
                    if (currentVolume != 0) {
                        Log.w(TAG, "Failed to mute stream $stream, current volume: $currentVolume")
                        allStreamsMuted = false
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to mute stream $stream: ${e.message}")
                    allStreamsMuted = false
                }
            }
            
            isAudioMuted = true
            Log.d(TAG, "Call audio muted successfully, all streams muted: $allStreamsMuted")
            true
            
            } catch (e: SecurityException) {
                Log.e(TAG, "Security exception while muting audio: ${e.message}")
                Toast.makeText(context, "Permission required to control audio", Toast.LENGTH_SHORT).show()
                false
            } catch (e: Exception) {
                Log.e(TAG, "Failed to mute call audio: ${e.message}")
                Toast.makeText(context, "Failed to mute audio: ${e.message}", Toast.LENGTH_SHORT).show()
                false
            }
        }
    }
    
    /**
     * Restores all audio streams to original settings
     * @return true if successful, false otherwise
     */
    fun unmuteCallAudio(): Boolean {
        synchronized(audioLock) {
            return try {
            Log.d(TAG, "Restoring call audio")
            
            // Restore audio mode with validation
            audioManager.mode = previousAudioMode
            if (audioManager.mode != previousAudioMode) {
                Log.w(TAG, "Failed to restore audio mode to $previousAudioMode")
            }
            
            // Unmute microphone
            audioManager.isMicrophoneMute = false
            
            // Restore audio volumes with individual error handling
            try {
                audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, originalCallVolume, 0)
                val currentCallVolume = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL)
                if (currentCallVolume != originalCallVolume) {
                    Log.w(TAG, "Failed to restore call volume, expected: $originalCallVolume, actual: $currentCallVolume")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restore call volume: ${e.message}")
            }
            
            try {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalMediaVolume, 0)
                val currentMediaVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                if (currentMediaVolume != originalMediaVolume) {
                    Log.w(TAG, "Failed to restore media volume, expected: $originalMediaVolume, actual: $currentMediaVolume")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restore media volume: ${e.message}")
            }
            
            // Restore ringer mode
            if (wasRingerNormal) {
                try {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                    if (audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL) {
                        Log.w(TAG, "Failed to restore ringer mode to NORMAL")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to restore ringer mode: ${e.message}")
                }
            }
            
            isAudioMuted = false
            Log.d(TAG, "Call audio restored successfully")
            true
            
            } catch (e: SecurityException) {
                Log.e(TAG, "Security exception while restoring audio: ${e.message}")
                Toast.makeText(context, "Permission required to control audio", Toast.LENGTH_SHORT).show()
                false
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restore call audio: ${e.message}")
                Toast.makeText(context, "Failed to restore audio: ${e.message}", Toast.LENGTH_SHORT).show()
                false
            }
        }
    }
    
    /**
     * Sets call volume to minimum (1) for IVR calls
     * @return true if successful, false otherwise
     */
    fun setCallVolumeToMinimum(): Boolean {
        synchronized(audioLock) {
            return try {
                Log.d(TAG, "Setting call volume to minimum (1)")
                
                // Store original call volume if not already stored
                if (originalCallVolume == 0) {
                    originalCallVolume = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL)
                    Log.d(TAG, "Stored original call volume: $originalCallVolume")
                }
                
                // Set audio mode for call
                audioManager.mode = AudioManager.MODE_IN_CALL
                
                // Set call volume to 1 (minimum audible level)
                audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, 1, AudioManager.FLAG_SHOW_UI)
                val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL)
                
                Log.d(TAG, "Call volume set to: $currentVolume (target was 1)")
                
                if (currentVolume <= 1) {
                    Log.d(TAG, "Call volume set to minimum successfully")
                    true
                } else {
                    Log.w(TAG, "Failed to set call volume to 1, current volume: $currentVolume")
                    false
                }
                
            } catch (e: SecurityException) {
                Log.e(TAG, "Security exception while setting call volume: ${e.message}")
                false
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set call volume to minimum: ${e.message}")
                false
            }
        }
    }
    
    /**
     * Restores call volume to original level
     * @return true if successful, false otherwise
     */
    fun restoreCallVolume(): Boolean {
        synchronized(audioLock) {
            return try {
                Log.d(TAG, "Restoring call volume to original level: $originalCallVolume")
                
                if (originalCallVolume > 0) {
                    // Restore audio mode
                    audioManager.mode = previousAudioMode
                    
                    // Restore call volume
                    audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, originalCallVolume, AudioManager.FLAG_SHOW_UI)
                    val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL)
                    
                    Log.d(TAG, "Call volume restored to: $currentVolume (expected: $originalCallVolume)")
                    
                    if (currentVolume == originalCallVolume) {
                        Log.d(TAG, "Call volume restored successfully")
                        true
                    } else {
                        Log.w(TAG, "Failed to restore call volume, expected: $originalCallVolume, actual: $currentVolume")
                        false
                    }
                } else {
                    Log.w(TAG, "No original call volume stored, cannot restore")
                    false
                }
                
            } catch (e: SecurityException) {
                Log.e(TAG, "Security exception while restoring call volume: ${e.message}")
                false
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restore call volume: ${e.message}")
                false
            }
        }
    }
    
    /**
     * Shows the call overlay for UPI123 protection using the new service
     */
    fun showCallOverlay(phoneNumber: String, amount: String) {
        try {
            // Use the new CallOverlayService instead of activity
            com.flowpay.app.services.CallOverlayService.showOverlay(context, phoneNumber, amount)
            Log.d(TAG, "Call overlay shown via service")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show call overlay: ${e.message}")
        }
    }
    
    /**
     * Hides the call overlay using the new service
     */
    fun hideCallOverlay() {
        try {
            // Use the new CallOverlayService instead of activity
            com.flowpay.app.services.CallOverlayService.hideOverlay(context)
            Log.d(TAG, "Call overlay hidden via service")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to hide call overlay: ${e.message}")
        }
    }
    
    /**
     * Terminates the active call using modern Android APIs
     * @return true if successful, false otherwise
     */
    fun terminateCall(): Boolean {
        synchronized(callStateLock) {
            return try {
                Log.d(TAG, "=== ATTEMPTING TO TERMINATE CALL ===")
                Log.d(TAG, "_isCallInProgress.value: ${_isCallInProgress.value}")
                Log.d(TAG, "currentCallType: $currentCallType")
                
                // Check if call is in progress
                if (!_isCallInProgress.value) {
                    Log.w(TAG, "No call in progress, cannot terminate")
                    return false
                }
                
                // Method 1: Use TelecomManager (Android 9+ - Primary method)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    try {
                        Log.d(TAG, "Trying TelecomManager.endCall() method...")
                        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as android.telecom.TelecomManager
                        val callTerminated = telecomManager.endCall()
                        Log.d(TAG, "Call terminated via TelecomManager: $callTerminated")
                        if (callTerminated) {
                            // Reset call state immediately
                            _isCallInProgress.value = false
                            currentCallType = null
                            return true
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to terminate call via TelecomManager: ${e.message}")
                    }
                }
                
                // Method 2: Use AccessibilityService approach (Android 6+)
                try {
                    Log.d(TAG, "Trying AccessibilityService approach...")
                    val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
                    if (accessibilityManager.isEnabled) {
                        // Simulate back button press to end call
                        val intent = Intent(Intent.ACTION_MEDIA_BUTTON)
                        intent.putExtra(Intent.EXTRA_KEY_EVENT, 
                            android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ENDCALL))
                        context.sendBroadcast(intent)
                        
                        // Also try to bring up the call screen and simulate hang up
                        val callIntent = Intent(Intent.ACTION_CALL)
                        callIntent.data = Uri.parse("tel:")
                        callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(callIntent)
                        
                        // Send ENDCALL key event after a short delay
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            val endCallIntent = Intent(Intent.ACTION_MEDIA_BUTTON)
                            endCallIntent.putExtra(Intent.EXTRA_KEY_EVENT, 
                                android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ENDCALL))
                            context.sendBroadcast(endCallIntent)
                        }, 100)
                        
                        Log.d(TAG, "Sent ENDCALL key event via AccessibilityService approach")
                        // Reset call state
                        _isCallInProgress.value = false
                        currentCallType = null
                        return true
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to terminate call via AccessibilityService: ${e.message}")
                }
                
                // Method 3: Use reflection to access ITelephony (fallback for older Android)
                try {
                    Log.d(TAG, "Trying ITelephony reflection method (fallback)...")
                    val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                    val telephonyClass = Class.forName(telephonyManager.javaClass.name)
                    val getITelephonyMethod = telephonyClass.getDeclaredMethod("getITelephony")
                    getITelephonyMethod.isAccessible = true
                    val telephony = getITelephonyMethod.invoke(telephonyManager)
                    val endCallMethod = telephony.javaClass.getDeclaredMethod("endCall")
                    val callTerminated = endCallMethod.invoke(telephony) as Boolean
                    Log.d(TAG, "Call terminated via ITelephony: $callTerminated")
                    if (callTerminated) {
                        _isCallInProgress.value = false
                        currentCallType = null
                        return true
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to terminate call via ITelephony: ${e.message}")
                }
                
                // Method 4: Force stop call by bringing up dialer and simulating hang up
                try {
                    Log.d(TAG, "Trying force stop call method...")
                    // Bring up the call screen
                    val callIntent = Intent(Intent.ACTION_CALL)
                    callIntent.data = Uri.parse("tel:")
                    callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(callIntent)
                    
                    // Wait a moment then simulate hang up
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        val endCallIntent = Intent(Intent.ACTION_MEDIA_BUTTON)
                        endCallIntent.putExtra(Intent.EXTRA_KEY_EVENT, 
                            android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ENDCALL))
                        context.sendBroadcast(endCallIntent)
                        // Reset call state after delay
                        _isCallInProgress.value = false
                        currentCallType = null
                    }, 200)
                    
                    Log.d(TAG, "Force stop call method executed")
                    return true
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to force stop call: ${e.message}")
                }
                
                Log.w(TAG, "All call termination methods failed")
                false
                
            } catch (e: SecurityException) {
                Log.e(TAG, "Security exception while terminating call: ${e.message}")
                false
            } catch (e: Exception) {
                Log.e(TAG, "Failed to terminate call: ${e.message}")
                false
            }
        }
    }
    
    
    /**
     * Cleanup method to release all resources and prevent memory leaks
     */
    fun cleanup() {
        synchronized(audioLock) {
            try {
                // Restore audio if it was muted
                if (isAudioMuted) {
                    unmuteCallAudio()
                } else {
                    Log.d(TAG, "Audio was not muted, no restoration needed")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during audio cleanup: ${e.message}")
            }
        }
        
        synchronized(callStateLock) {
            try {
                // Clear all callbacks and state
                clearCallbacks()
                currentCallType = null
                _isCallInProgress.value = false
            } catch (e: Exception) {
                Log.e(TAG, "Error during state cleanup: ${e.message}")
            }
        }
        
        // Unregister phone state listener
        synchronized(listenerLock) {
            try {
                phoneStateListener?.let { listener ->
                    telephonyManager.listen(listener, PhoneStateListener.LISTEN_NONE)
                }
                phoneStateListener = null
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering phone state listener: ${e.message}")
            }
        }
        
        Log.d(TAG, "CallManager cleanup completed")
    }
    
    /**
     * Checks if audio is currently muted
     */
    fun isAudioMuted(): Boolean = isAudioMuted
    
    /**
     * Gets the current call state
     */
    fun getCallState(): Int = telephonyManager.callState
    
    /**
     * Validates phone number format (10 digits)
     */
    fun isValidPhoneNumber(phoneNumber: String?): Boolean {
        if (phoneNumber.isNullOrBlank()) return false
        // More strict validation - exactly 10 digits, no leading zeros for first digit
        return phoneNumber.matches(Regex(AppConstants.PHONE_NUMBER_PATTERN))
    }
    
    /**
     * Validates amount format (positive number with reasonable limits)
     */
    fun isValidAmount(amount: String?): Boolean {
        if (amount.isNullOrBlank()) return false
        return try {
            val amountValue = amount.toDouble()
            // More reasonable limits: minimum 1 rupee, maximum 1 lakh
            amountValue >= AppConstants.MIN_AMOUNT_VALUE && amountValue <= AppConstants.MAX_AMOUNT_VALUE
        } catch (e: NumberFormatException) {
            false
        }
    }
    
    /**
     * Validates UPI service number format
     */
    fun isValidUpiServiceNumber(serviceNumber: String): Boolean {
        if (serviceNumber.isNullOrBlank()) return false
        return serviceNumber.matches(Regex("^[0-9]{10,12}$"))
    }
    
    /**
     * Sanitizes and validates phone number input
     */
    fun sanitizeAndValidatePhoneNumber(phoneNumber: String): String? {
        if (phoneNumber.isNullOrBlank()) return null
        val sanitized = phoneNumber.replace(Regex("[^0-9]"), "")
        return if (sanitized.length == 10) sanitized else null
    }
    
    /**
     * Sanitizes and validates amount input
     */
    fun sanitizeAndValidateAmount(amount: String): String? {
        if (amount.isNullOrBlank()) return null
        val sanitized = amount.replace(Regex("[^0-9.]"), "")
        return if (isValidAmount(sanitized)) sanitized else null
    }
}