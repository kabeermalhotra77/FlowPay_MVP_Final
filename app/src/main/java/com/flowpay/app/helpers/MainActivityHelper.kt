package com.flowpay.app.helpers

import android.Manifest
import android.content.BroadcastReceiver
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import com.flowpay.app.constants.PermissionConstants
import android.widget.Toast
import com.flowpay.app.managers.CallManager
import com.flowpay.app.managers.PermissionManager
import com.flowpay.app.managers.CallDurationMonitor
import com.flowpay.app.services.CallOverlayService
// DISABLED: USSDOverlay functionality temporarily disabled
// import com.flowpay.app.services.USSDOverlayService
import com.flowpay.app.utils.PhoneNumberUtils
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.flowpay.app.states.PaymentState
import com.flowpay.app.SetupActivity
import com.flowpay.app.TestConfigurationActivity
import com.flowpay.app.constants.AppConstants

/**
 * Helper class containing all business logic for MainActivity
 * This separates the UI concerns from the business logic
 */
class MainActivityHelper(
    private val context: Context,
    private val uiCallback: UICallback
) {
    companion object {
        private const val TAG = "MainActivityHelper"
    }

    // Managers
    private var callManager: CallManager? = null
    private var permissionManager: PermissionManager? = null
    private var callDurationMonitor: CallDurationMonitor? = null
    
    // Permission request tracking
    private var isRequestingPermissions = false
    
    // Legacy services for compatibility
    private lateinit var telephonyManager: TelephonyManager
    
    // UPI call tracking - using simplified state management
    private val upiCallState = com.flowpay.app.managers.UpiCallStateManager()
    
    
    
    // Phone state listener
    private var phoneStateListener: PhoneStateListener? = null
    @Volatile
    private var isPhoneStateListenerRegistered = false
    private val phoneStateListenerLock = Any()
    
    
    // Broadcast receiver for audio restoration and call termination
    private lateinit var audioRestoreReceiver: BroadcastReceiver
    
    
    // State synchronization
    private val stateLock = Any()
    

    /**
     * Interface for UI callbacks
     */
    interface UICallback {
        fun showToast(message: String)
        fun showPinEntryAlert()
        fun showOverlay()
        fun hideOverlay()
        fun updatePaymentState(paymentState: PaymentState)
        fun navigateToSetup()
        fun navigateToTestConfiguration()
        fun navigateToPaymentSuccess(paymentData: com.flowpay.app.data.PaymentSuccessData)
        fun finishActivity()
        fun registerBroadcastReceiver(receiver: BroadcastReceiver, filter: IntentFilter)
        fun unregisterBroadcastReceiver(receiver: BroadcastReceiver)
        fun showCallDurationIssueDialog()
        fun showCallSuccessDialog()
    }
    
    
    
    /**
     * Check SMS permissions specifically
     */
    fun checkSMSPermissions(): Boolean {
        return permissionManager?.checkSMSPermissions() ?: false
    }
    
    /**
     * Request SMS permissions specifically
     */
    fun requestSMSPermissions() {
        permissionManager?.requestSMSPermissions()
    }
    
    
    /**
     * Open QR scanner
     */
    private fun openQRScanner() {
        val intent = Intent(context, com.flowpay.app.features.qr_scanner.presentation.QRScannerActivity::class.java)
        // Remove FLAG_ACTIVITY_NEW_TASK to allow result handling
        if (context is Activity) {
            context.startActivityForResult(intent, com.flowpay.app.MainActivity.QR_SCAN_REQUEST_CODE)
        } else {
            // Fallback for non-Activity context
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
    
    /**
     * Start QR scanning - public method for UI access
     * @param isFromPermissionGrant true if called after permission grant, false if initiated by user
     */
    fun startQRScanning(isFromPermissionGrant: Boolean = false) {
        Log.d(TAG, "=== STARTING QR SCANNING ===")
        
        // If this is called after permission grant, don't open QR scanner
        // The form state restoration will handle showing the PayContactDialog
        if (isFromPermissionGrant) {
            Log.d(TAG, "Called after permission grant, not opening QR scanner")
            return
        }
        
        // Check camera permission first (most important for QR scanning)
        if (permissionManager?.isPermissionGranted(Manifest.permission.CAMERA) != true) {
            Log.d(TAG, "Camera permission not granted, opening QR scanner to request permission")
            uiCallback.showToast("Opening QR scanner...")
            openQRScanner()
            return
        }
        
        // Check other basic permissions
        if (permissionManager?.checkAllPermissions() != true) {
            Log.d(TAG, "Other permissions not granted, requesting...")
            uiCallback.showToast("Requesting required permissions...")
            isRequestingPermissions = true
            permissionManager?.requestRequiredPermissions()
            return
        }
        
        // Check overlay permission for USSD functionality
        if (permissionManager?.canDrawOverlays() != true) {
            Log.d(TAG, "Overlay permission not granted, requesting...")
            uiCallback.showToast("Requesting overlay permission for payment protection...")
            isRequestingPermissions = true
            permissionManager?.requestOverlayPermission()
            // Don't return here - let the permission result handler open QR scanner
            // The handleActivityResult() method will call startQRScanning() again after permission is granted
            return
        }
        
        Log.d(TAG, "All permissions granted, opening QR scanner")
        uiCallback.showToast("Opening QR scanner...")
        openQRScanner()
    }
    
    /**
     * Handle activity result
     */
    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d(TAG, "handleActivityResult: requestCode=$requestCode, resultCode=$resultCode")
        
        when (requestCode) {
            PermissionConstants.OVERLAY_PERMISSION_REQ_CODE -> {
                Log.d(TAG, "Overlay permission result received")
                isRequestingPermissions = false
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M && 
                    android.provider.Settings.canDrawOverlays(context)) {
                    Log.d(TAG, "Overlay permission granted, not opening QR scanner - form state will be restored")
                    // Don't open QR scanner, let form state restoration handle it
                    startQRScanning(isFromPermissionGrant = true)
                } else {
                    Log.w(TAG, "Overlay permission not granted, QR scanning cannot proceed")
                    uiCallback.showToast("Overlay permission is required for QR scanning. Please enable it in Settings.")
                }
            }
            PermissionConstants.PERMISSIONS_REQUEST_CODE -> {
                Log.d(TAG, "Basic permissions result received")
                isRequestingPermissions = false
                // Handle basic permission results if needed
                if (permissionManager?.checkAllPermissions() == true) {
                    Log.d(TAG, "Basic permissions granted, checking overlay permission")
                    startQRScanning()
                } else {
                    Log.w(TAG, "Basic permissions not granted")
                    uiCallback.showToast("Required permissions not granted. Please try again.")
                }
            }
        }
    }
    
    /**
     * Show overlay
     */
    fun showOverlay() {
        try {
            // Overlay functionality - simplified for new SMS system
            Log.d(TAG, "Show overlay requested")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing overlay: ${e.message}")
        }
    }
    
    /**
     * Hide overlay
     */
    fun hideOverlay() {
        try {
            com.flowpay.app.services.CallOverlayService.hideOverlay(context)
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding overlay: ${e.message}")
        }
    }
    
    

    
    
    

    /**
     * Initialize all managers and services
     */
    fun initialize() {
        try {
            // Initialize managers
            callManager = CallManager(context)
            permissionManager = PermissionManager(context as Activity)
            callDurationMonitor = CallDurationMonitor(context)
            
            // Initialize call duration monitor for manual transfers
            Log.d("MainActivityHelper", "🟢 Initializing CallDurationMonitor...")
            Log.d("MainActivityHelper", "✅ CallDurationMonitor initialized - callbacks will be set during transfer")
            
            // Initialize legacy services
            telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            
            // State management handled by new SMS system
            
            // Register phone state listener if permission is granted
            if (permissionManager?.isPermissionGranted(Manifest.permission.READ_PHONE_STATE) == true) {
                registerPhoneStateListener()
                Log.d(TAG, "Phone state listener registered successfully")
            } else {
                Log.w(TAG, "READ_PHONE_STATE permission not granted, phone state listener not registered")
            }
            
            // Initialize broadcast receiver for audio restoration and call termination
            audioRestoreReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    when (intent?.action) {
                        AppConstants.ACTION_RESTORE_AUDIO -> {
                            Log.d(TAG, "Received audio restoration broadcast from overlay service")
                            restoreCallAudio()
                            resetUpiCallTracking()
                        }
                        AppConstants.ACTION_CALL_TERMINATED -> {
                            Log.d(TAG, "Received call termination broadcast from overlay service")
                            // Reset UPI call tracking when call is terminated via overlay button
                            resetUpiCallTracking()
                            restoreCallAudio()
                            
                            // For early termination (< 30 seconds), log and reset
                            Handler(Looper.getMainLooper()).postDelayed({
                                Log.d(TAG, "Early call termination - reset to main screen")
                            }, 1000) // Small delay to ensure state is properly updated
                        }
                    }
                }
            }
            
            // Register broadcast receiver for audio restoration and call termination
            registerBroadcastReceiver()
            
            // Check permissions
            if (permissionManager?.checkAllPermissions() != true) {
                permissionManager?.requestRequiredPermissions()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during initialization: ${e.message}")
        }
    }
    
    /**
     * Register broadcast receiver for audio restoration and call termination
     */
    private fun registerBroadcastReceiver() {
        try {
            val filter = IntentFilter().apply {
                addAction(AppConstants.ACTION_RESTORE_AUDIO)
                addAction(AppConstants.ACTION_CALL_TERMINATED)
            }
            uiCallback.registerBroadcastReceiver(audioRestoreReceiver, filter)
            Log.d(TAG, "Audio restoration and call termination broadcast receiver registered")
        } catch (e: Exception) {
            Log.e(TAG, "Error registering broadcast receiver: ${e.message}")
        }
    }

    /**
     * Check if setup is completed
     */
    fun isSetupCompleted(): Boolean {
        val sharedPreferences = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean(AppConstants.KEY_SETUP_COMPLETED, false)
    }

    /**
     * Check if test configuration is completed
     */
    fun isTestCompleted(): Boolean {
        val sharedPreferences = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean(AppConstants.KEY_TEST_COMPLETED, false)
    }

    

    /**
     * Initiate transfer with validation and business logic
     */
    fun initiateTransfer(phoneNumber: String, amount: String) {
        Log.d(TAG, "Initiating transfer - Phone: $phoneNumber, Amount: $amount")
        
        // Validate input
        if (phoneNumber.isNullOrBlank() || amount.isNullOrBlank()) {
            uiCallback.showToast("Please enter both phone number and amount")
            return
        }
        
        // Validate phone number
        if (callManager?.isValidPhoneNumber(phoneNumber) != true) {
            uiCallback.showToast("Please enter valid 10-digit number")
            return
        }
        
        // Validate amount
        if (callManager?.isValidAmount(amount) != true) {
            uiCallback.showToast("Please enter valid amount")
            return
        }
        
        // Check permissions
        if (permissionManager?.checkAllPermissions() != true) {
            uiCallback.showToast("Required permissions not granted")
            permissionManager?.requestRequiredPermissions()
            return
        }
        
        if (permissionManager?.checkOverlayPermission() != true) {
            uiCallback.showToast("Overlay permission required")
            permissionManager?.requestOverlayPermission()
            return
        }
        
        // Additional overlay permission validation
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            uiCallback.showToast("Overlay permission required for call protection")
            permissionManager?.requestOverlayPermission()
            return
        }
        
        // Start SMS monitoring for UPI 123 transfer
        com.flowpay.app.helpers.TransactionDetector.getInstance(context).startOperation(
            operationType = "UPI_123",
            expectedAmount = amount
        )
        
        // Set UPI call state with service number for proper detection
        val upiServiceNumber = callManager?.getCurrentUpiServiceNumber() ?: AppConstants.DEFAULT_UPI_SERVICE_NUMBER
        upiCallState.startCall(upiServiceNumber, amount)
        
        
        // Stop any existing monitoring and start fresh call duration monitoring
        Log.d("MainActivityHelper", "⏱️ Starting call duration monitoring...")
        callDurationMonitor?.stopMonitoring() // Ensure clean state
        callDurationMonitor?.startTimer(
            callType = "MANUAL_TRANSFER",
            onCallDurationIssue = {
                Log.d("MainActivityHelper", "🚨 Call ended before 25 seconds - showing dialog")
                uiCallback.showCallDurationIssueDialog()
            },
            onCallSuccessful = {
                Log.d("MainActivityHelper", "✅ Call completed successfully after 25 seconds - showing success dialog")
                uiCallback.showCallSuccessDialog()
            }
        )
        Log.d("MainActivityHelper", "✅ Call monitoring started - 25 second timer active")
        
        // Use CallManager to initiate call
        val success = callManager?.initiateUPI123Call(phoneNumber, amount) ?: false
        handleCallInitiationResult(success, phoneNumber, amount)
    }

    /**
     * Handle call initiation result
     */
    private fun handleCallInitiationResult(success: Boolean, phoneNumber: String, amount: String) {
        if (success) {
            Log.d("OverlayDebug", "UPI123 call initiated successfully - tracking enabled")
            
            // Check overlay permission before showing using PermissionManager
            if (permissionManager?.canDrawOverlays() != true) {
                Log.e("OverlayDebug", "Overlay permission not granted - requesting permission")
                uiCallback.showToast("Overlay permission required - please grant permission")
                
                // Request overlay permission using PermissionManager
                permissionManager?.requestOverlayPermission()
                return
            }
            
            // Start CallOverlayService with timeout - overlay will only show when call is detected
            Log.d("OverlayDebug", "Call initiated - starting CallOverlayService with 40s timeout")
            CallOverlayService.showOverlay(context, phoneNumber, amount)
            Log.d("OverlayDebug", "CallOverlayService started - waiting for call detection")
            
        } else {
            Log.e("OverlayDebug", "Failed to initiate UPI123 call")
            resetUpiCallTracking()
        }
    }

    /**
     * Handle QR permission results
     */
    fun handleQRPermissionResult(permissions: Map<String, Boolean>) {
        Log.d(TAG, "QR permission results received: $permissions")
        
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            Log.d(TAG, "All QR permissions granted, starting QR scanning")
            startQRScanning()
        } else {
            Log.w(TAG, "Some QR permissions were denied")
            uiCallback.showToast("Camera permission is required for QR scanning")
        }
    }

    /**
     * Handle permission results
     */
    fun handlePermissionResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray): Boolean {
        val success = permissionManager?.handlePermissionResult(requestCode, permissions, grantResults) ?: false
        
        if (success) {
            // Re-enable phone state listener if READ_PHONE_STATE permission was granted
            if (permissions.contains(Manifest.permission.READ_PHONE_STATE) &&
                grantResults[permissions.indexOf(Manifest.permission.READ_PHONE_STATE)] == 
                PackageManager.PERMISSION_GRANTED) {
                registerPhoneStateListener()
                Log.d(TAG, "Phone state listener registered after permission grant")
            }
        } else {
            uiCallback.showToast("Some permissions were denied. App may not work properly.")
        }
        
        return success
    }

    /**
     * Handle overlay permission result
     */
    fun handleOverlayPermissionResult() {
        if (permissionManager?.canDrawOverlays() == true) {
            Log.d(TAG, "Overlay permission granted")
            
            // Check if there's a pending overlay request
            val sharedPrefs = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
            val hasPendingOverlay = sharedPrefs.getBoolean(AppConstants.KEY_PENDING_OVERLAY, false)
            
            if (hasPendingOverlay) {
                val phoneNumber = sharedPrefs.getString(AppConstants.KEY_PENDING_PHONE, "") ?: ""
                val amount = sharedPrefs.getString(AppConstants.KEY_PENDING_AMOUNT, "") ?: ""
                
                Log.d(TAG, "Processing pending overlay request - Phone: $phoneNumber, Amount: $amount")
                
                // Clear pending request
                sharedPrefs.edit()
                    .remove(AppConstants.KEY_PENDING_OVERLAY)
                    .remove(AppConstants.KEY_PENDING_PHONE)
                    .remove(AppConstants.KEY_PENDING_AMOUNT)
                    .apply()
                
                // Show overlay immediately
                CallOverlayService.showOverlay(context, phoneNumber, amount)
            }
        } else {
            Log.w(TAG, "Overlay permission denied")
            uiCallback.showToast("Overlay permission is required for call protection")
            
            // Check for special device permissions (Xiaomi/MIUI)
            checkSpecialPermissions()
        }
    }
    
    /**
     * Check for special permissions required by custom ROMs like MIUI
     */
    private fun checkSpecialPermissions() {
        try {
            // For Xiaomi/MIUI devices
            val intent = Intent("miui.intent.action.APP_PERM_EDITOR")
            intent.setClassName("com.miui.securitycenter",
                "com.miui.permcenter.permissions.PermissionsEditorActivity")
            intent.putExtra("extra_pkgname", context.packageName)
            context.startActivity(intent)
            Log.d(TAG, "Opened MIUI permission settings")
        } catch (e: Exception) {
            // Try alternative MIUI permission path
            try {
                val intent = Intent("miui.intent.action.APP_PERM_EDITOR")
                intent.setClassName("com.miui.securitycenter",
                    "com.miui.permcenter.permissions.AppPermissionsEditorActivity")
                intent.putExtra("extra_pkgname", context.packageName)
                context.startActivity(intent)
                Log.d(TAG, "Opened MIUI permission settings (alternative)")
            } catch (e2: Exception) {
                // Try Huawei EMUI
                try {
                    val intent = Intent("com.huawei.systemmanager.startupmamager.StartupManagerActivity")
                    intent.putExtra("packageName", context.packageName)
                    context.startActivity(intent)
                    Log.d(TAG, "Opened EMUI permission settings")
                } catch (e3: Exception) {
                    // Try Oppo ColorOS
                    try {
                        val intent = Intent("com.coloros.safecenter.permission.PermissionManagerActivity")
                        intent.putExtra("packageName", context.packageName)
                        context.startActivity(intent)
                        Log.d(TAG, "Opened ColorOS permission settings")
                    } catch (e4: Exception) {
                        // Try Vivo FuntouchOS
                        try {
                            val intent = Intent("com.vivo.permissionmanager.activity.BaikeActivity")
                            intent.putExtra("packageName", context.packageName)
                            context.startActivity(intent)
                            Log.d(TAG, "Opened FuntouchOS permission settings")
                        } catch (e5: Exception) {
                            Log.d(TAG, "Not a custom ROM device or permission settings not available")
                        }
                    }
                }
            }
        }
    }

    /**
     * Create phone state listener
     */
    private fun createPhoneStateListener() = object : PhoneStateListener() {
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            super.onCallStateChanged(state, phoneNumber)
            Log.d(TAG, "Phone state changed to: ${getStateName(state)}")
            
            try {
                when (state) {
                    TelephonyManager.CALL_STATE_OFFHOOK -> {
                        handleCallConnected(phoneNumber)
                    }
                    TelephonyManager.CALL_STATE_IDLE -> {
                        handleCallEnded()
                    }
                    TelephonyManager.CALL_STATE_RINGING -> {
                        Log.d(TAG, "Call ringing")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in phone state listener", e)
            }
        }
    }
    
    private fun getStateName(state: Int): String = when(state) {
        TelephonyManager.CALL_STATE_IDLE -> "IDLE"
        TelephonyManager.CALL_STATE_OFFHOOK -> "OFFHOOK"
        TelephonyManager.CALL_STATE_RINGING -> "RINGING"
        else -> "UNKNOWN"
    }
    
    /**
     * Handle call connected state (OFFHOOK)
     */
    private fun handleCallConnected(phoneNumber: String?) {
        // Check for UPI calls
        if (upiCallState.isActive()) {
            Log.d("OverlayDebug", "=== shouldActivateForUpiCall DEBUG ===")
            Log.d("OverlayDebug", "Input phoneNumber: $phoneNumber")
            Log.d("OverlayDebug", "upiCallState: ${upiCallState.getDebugInfo()}")
            
            // Early exit if no active UPI call
            if (!upiCallState.isActive()) {
                Log.d("OverlayDebug", "No active UPI call - returning false")
                return
            }
            
            // Check timeout
            if (!upiCallState.isWithinTimeout()) {
                Log.d("OverlayDebug", "UPI call timeout exceeded - returning false")
                return
            }
            
            // Handle empty phone number (common Android behavior)
            if (phoneNumber.isNullOrBlank()) {
                Log.d("OverlayDebug", "Empty phone number - assuming UPI call within timeout")
                Log.d(TAG, "UPI call connected - showing overlay")
                showUpiOverlay(phoneNumber)
                return
            }
            
            // Check if incoming call matches UPI service number
            val upiServiceNumber = callManager?.getCurrentUpiServiceNumber() ?: AppConstants.DEFAULT_UPI_SERVICE_NUMBER
            val isUpiServiceNumber = PhoneNumberUtils.isPhoneNumberMatch(phoneNumber, upiServiceNumber)
            
            Log.d("OverlayDebug", "Call analysis - phoneNumber: $phoneNumber, upiServiceNumber: $upiServiceNumber")
            Log.d("OverlayDebug", "isUpiServiceNumber: $isUpiServiceNumber")
            Log.d("OverlayDebug", "shouldActivateForUpiCall result: $isUpiServiceNumber")
            Log.d("OverlayDebug", "=== END shouldActivateForUpiCall DEBUG ===")
            
            if (isUpiServiceNumber) {
                Log.d(TAG, "UPI call connected - showing overlay")
                showUpiOverlay(phoneNumber)
            }
        }
    }
    
    /**
     * Handle call ended state (IDLE)
     */
    private fun handleCallEnded() {
        if (upiCallState.isActive()) {
            Log.d(TAG, "UPI call ended - cleaning up")
            hideUpiOverlay()
            resetUpiCallTracking()
        }
    }
    
    /**
     * Show UPI overlay when call is detected
     */
    private fun showUpiOverlay(phoneNumber: String?) {
        CallOverlayService.waitForServiceReady { isReady ->
            if (isReady) {
                Log.d("OverlayDebug", "Service ready - notifying call detection")
                CallOverlayService.getInstance()?.onCallDetected()
                
                
                // Mute audio
                try {
                    callManager?.muteCallAudio()
                    Log.d(TAG, "Audio muted using CallManager")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to mute audio: ${e.message}")
                }
                Log.d("OverlayDebug", "Call detected - overlay should now be visible")
            } else {
                Log.e("OverlayDebug", "Service not ready for call detection - overlay may not show")
                // Fallback: try to show overlay directly
                try {
                    CallOverlayService.showOverlay(context, "", "")
                } catch (e: Exception) {
                    Log.e("OverlayDebug", "Fallback overlay show failed: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Hide UPI overlay when call ends
     */
    /**
     * Restore call audio by unmuting the call
     */
    private fun restoreCallAudio() {
        try {
            Log.d(TAG, "Restoring call audio...")
            callManager?.unmuteCallAudio()
            Log.d(TAG, "Audio restored using CallManager")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore audio: ${e.message}")
        }
    }

    private fun hideUpiOverlay() {
        try {
            // Restore audio
            Log.d("OverlayDebug", "Restoring audio...")
            try {
                callManager?.unmuteCallAudio()
                Log.d(TAG, "Audio restored using CallManager")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restore audio: ${e.message}")
            }
            
            // Hide overlay
            Log.d("OverlayDebug", "Hiding overlay...")
            CallOverlayService.hideOverlay(context)
            
            // Show pin entry alert after delay
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    Log.d("OverlayDebug", "Showing pin entry alert...")
                    uiCallback.showPinEntryAlert()
                } catch (e: Exception) {
                    Log.e(TAG, "Error showing pin entry alert", e)
                }
            }, AppConstants.PIN_ENTRY_ALERT_DELAY)
        } catch (e: Exception) {
            Log.e(TAG, "Error during call ended handling", e)
        }
    }
    
    /**
     * Register phone state listener
     */
    private fun registerPhoneStateListener() {
        synchronized(phoneStateListenerLock) {
            try {
                // Double-check locking pattern
                if (isPhoneStateListenerRegistered) {
                    Log.d(TAG, "Phone state listener already registered")
                    return
                }
                
                // Unregister any existing listener first
                try {
                    phoneStateListener?.let {
                        telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error unregistering old listener", e)
                }
                
                phoneStateListener = createPhoneStateListener()
                telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
                isPhoneStateListenerRegistered = true
                
                Log.d(TAG, "Phone state listener registered successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register phone state listener", e)
                isPhoneStateListenerRegistered = false
            }
        }
    }

    /**
     * Unregister phone state listener
     */
    private fun unregisterPhoneStateListener() {
        synchronized(phoneStateListenerLock) {
            if (!isPhoneStateListenerRegistered) return
            
            try {
                phoneStateListener?.let {
                    telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE)
                }
                isPhoneStateListenerRegistered = false
                phoneStateListener = null
                Log.d(TAG, "Phone state listener unregistered")
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering phone state listener", e)
            }
        }
    }




    /**
     * Reset UPI call tracking
     */
    private fun resetUpiCallTracking() {
        synchronized(stateLock) {
            upiCallState.endCall()
            Log.d(TAG, "UPI call tracking reset - ready for regular calls")
        }
    }

    /**
     * Handle app lifecycle events
     */
    fun onPause() {
        // Send app paused broadcast to USSD overlay service
        try {
            val intent = Intent("APP_PAUSED")
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
            Log.d(TAG, "App paused broadcast sent")
        } catch (e: Exception) {
            Log.w(TAG, "Error sending app paused broadcast: ${e.message}")
        }
        
        // Terminate USSD overlay when app goes to background
        // DISABLED: USSDOverlay functionality temporarily disabled
        /*
        try {
            USSDOverlayService.hideOverlay(context)
            Log.d(TAG, "App paused - USSD overlay terminated")
        } catch (e: Exception) {
            Log.w(TAG, "Error terminating USSD overlay on pause: ${e.message}")
        }
        */
        
        // Only unregister phone state listener if NOT in UPI call
        // During UPI calls, we need the listener to detect call state changes
        if (!upiCallState.isActive()) {
            unregisterPhoneStateListener()
            Log.d(TAG, "App paused - unregistered phone state listener (no UPI call active)")
        } else {
            Log.d(TAG, "App paused during UPI call - keeping phone state listener active for overlay detection")
        }
    }

    fun onStop() {
        // Send app stopped broadcast to USSD overlay service
        try {
            val intent = Intent("APP_STOPPED")
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
            Log.d(TAG, "App stopped broadcast sent")
        } catch (e: Exception) {
            Log.w(TAG, "Error sending app stopped broadcast: ${e.message}")
        }
        
        // Terminate USSD overlay when app is stopped
        // DISABLED: USSDOverlay functionality temporarily disabled
        /*
        try {
            USSDOverlayService.hideOverlay(context)
            Log.d(TAG, "App stopped - USSD overlay terminated")
        } catch (e: Exception) {
            Log.w(TAG, "Error terminating USSD overlay on stop: ${e.message}")
        }
        */
        
        // Only unregister phone state listener if NOT in UPI call
        // During UPI calls, we need the listener to detect call state changes
        if (!upiCallState.isActive()) {
            unregisterPhoneStateListener()
            Log.d(TAG, "App stopped - unregistered phone state listener (no UPI call active)")
        } else {
            Log.d(TAG, "App stopped during UPI call - keeping phone state listener active for overlay detection")
        }
    }

    fun onResume() {
        // Re-register phone state listener if permission is granted
        if (::telephonyManager.isInitialized && 
            permissionManager?.isPermissionGranted(Manifest.permission.READ_PHONE_STATE) == true) {
            registerPhoneStateListener()
            if (upiCallState.isActive()) {
                Log.d(TAG, "App resumed during UPI call - phone state listener re-registered for overlay detection")
            }
        }
    }

    fun onDestroy() {
        try {
            // Send app destroyed broadcast to USSD overlay service
            try {
                val intent = Intent("APP_DESTROYED")
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
                Log.d(TAG, "App destroyed broadcast sent")
            } catch (e: Exception) {
                Log.w(TAG, "Error sending app destroyed broadcast: ${e.message}")
            }
            
            // Terminate USSD overlay when app is destroyed
            // DISABLED: USSDOverlay functionality temporarily disabled
            /*
            try {
                USSDOverlayService.hideOverlay(context)
                Log.d(TAG, "App destroyed - USSD overlay terminated")
            } catch (e: Exception) {
                Log.w(TAG, "Error terminating USSD overlay on destroy: ${e.message}")
            }
            */
            
            // Unregister broadcast receiver
            try {
                uiCallback.unregisterBroadcastReceiver(audioRestoreReceiver)
                Log.d(TAG, "Audio restoration broadcast receiver unregistered")
            } catch (e: Exception) {
                Log.w(TAG, "Error unregistering audio restoration receiver: ${e.message}")
            }
            
            // Always unregister phone state listener to prevent memory leaks
            unregisterPhoneStateListener()
            
            // Clean up CallManager resources
            try {
                callManager?.cleanup()
            } catch (e: Exception) {
                Log.w(TAG, "Error cleaning up CallManager: ${e.message}")
            }
            
            // Clean up audio state
            try {
                // Hide overlay if visible
                CallOverlayService.hideOverlay(context)
            } catch (e: Exception) {
                Log.w(TAG, "Error hiding overlay during cleanup: ${e.message}")
            }
            
            // Reset UPI call tracking
            resetUpiCallTracking()
            
            // State management handled by new SMS system
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup: ${e.message}")
        }
    }
    
    /**
     * Navigate to setup screen
     */
    fun navigateToSetup() {
        val intent = Intent(context, SetupActivity::class.java)
        context.startActivity(intent)
    }
    
    /**
     * Navigate to test configuration screen
     */
    fun navigateToTestConfiguration() {
        val intent = Intent(context, TestConfigurationActivity::class.java)
        context.startActivity(intent)
    }
    
    /**
     * Cleanup resources and stop monitoring
     */
    fun cleanup() {
        Log.d("MainActivityHelper", "Cleaning up resources")
        callDurationMonitor?.cleanup()
        callDurationMonitor = null
    }
    
    
    
    
    
    
    
    
    
}


