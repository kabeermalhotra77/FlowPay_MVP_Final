package com.flowpay.app.services

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.provider.Telephony
import android.util.Log
import android.graphics.Rect
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.flowpay.app.R
import com.flowpay.app.models.TransactionData
import com.flowpay.app.managers.CallStateMonitor
import com.flowpay.app.managers.TransactionDialogManager
import com.flowpay.app.states.PaymentType
import com.flowpay.app.utils.PhoneNumberUtils
import com.flowpay.app.utils.OverlayLogger

class CallOverlayService : Service() {
    
    companion object {
        private const val TAG = "CallOverlayService"
        private const val ACTION_START_OVERLAY = "START_OVERLAY"
        private const val ACTION_STOP_OVERLAY = "STOP_OVERLAY"
        private const val TIMEOUT_DURATION = 40000L // 40 seconds timeout
        
        private var serviceInstance: CallOverlayService? = null
        private val serviceReadyLock = Any()
        private var isServiceReady = false
        
        /**
         * Shows the call overlay with payment details
         */
        fun showOverlay(context: Context, phoneNumber: String, amount: String) {
            try {
                Log.d(TAG, "=== Starting call overlay for $phoneNumber, amount: $amount ===")
                
                // Stop any existing service first
                if (serviceInstance != null) {
                    Log.d(TAG, "Stopping existing service before starting new one")
                    context.stopService(Intent(context, CallOverlayService::class.java))
                    Handler(Looper.getMainLooper()).postDelayed({
                        startNewService(context, phoneNumber, amount)
                    }, 500)
                } else {
                    startNewService(context, phoneNumber, amount)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start call overlay service: ${e.message}")
            }
        }
        
        private fun startNewService(context: Context, phoneNumber: String, amount: String) {
            try {
                val intent = Intent(context, CallOverlayService::class.java).apply {
                    putExtra("action", ACTION_START_OVERLAY)
                    putExtra("phone_number", phoneNumber)
                    putExtra("amount", amount)
                }
                context.startService(intent)
                
                // Wait for service to be ready before proceeding
                Handler(Looper.getMainLooper()).postDelayed({
                    if (serviceInstance == null) {
                        Log.w(TAG, "Service not ready, retrying...")
                        context.startService(intent)
                    }
                }, 100)
                
                Log.d(TAG, "Call overlay service started for $phoneNumber, amount: $amount")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start new service: ${e.message}")
            }
        }
        
        /**
         * Hides the call overlay
         */
        fun hideOverlay(context: Context) {
            try {
                val intent = Intent(context, CallOverlayService::class.java).apply {
                    putExtra("action", ACTION_STOP_OVERLAY)
                }
                context.startService(intent)
                Log.d(TAG, "Call overlay hide requested")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to hide call overlay: ${e.message}")
            }
        }
        
        /**
         * Checks if overlay is currently active
         */
        fun isOverlayActive(): Boolean = serviceInstance?.isOverlayActive ?: false
        
        /**
         * Gets the current service instance
         */
        fun getInstance(): CallOverlayService? = serviceInstance
        
        /**
         * FIX: Add method to check if service is ready
         */
        fun isServiceReady(): Boolean = serviceInstance != null
        
        /**
         * Wait for service to be ready with timeout
         * @param maxWaitTime Maximum time to wait in milliseconds (default 5000ms)
         * @param callback Callback with boolean indicating if service is ready
         */
        fun waitForServiceReady(maxWaitTime: Long = 5000L, callback: (Boolean) -> Unit) {
            val startTime = System.currentTimeMillis()
            val checkRunnable = object : Runnable {
                override fun run() {
                    synchronized(serviceReadyLock) {
                        if (isServiceReady && serviceInstance != null) {
                            Log.d(TAG, "Service is ready")
                            callback(true)
                        } else if (System.currentTimeMillis() - startTime > maxWaitTime) {
                            Log.e(TAG, "Service readiness timeout after ${maxWaitTime}ms")
                            callback(false)
                        } else {
                            Handler(Looper.getMainLooper()).postDelayed(this, 50)
                        }
                    }
                }
            }
            Handler(Looper.getMainLooper()).post(checkRunnable)
        }
        
        /**
         * FIX: Add service status monitoring
         */
        fun getServiceStatus(): String {
            return "Instance: ${serviceInstance != null}, " +
                   "Overlay Active: ${serviceInstance?.isOverlayActive ?: false}, " +
                   "Call Detected: ${serviceInstance?.isCallDetected ?: false}, " +
                   "Pending Phone: ${serviceInstance?.pendingPhoneNumber ?: "null"}, " +
                   "Pending Amount: ${serviceInstance?.pendingAmount ?: "null"}"
        }
    }
    
    private val binder = CallOverlayBinder()
    private var isOverlayActive = false
    private var overlayView: View? = null
    private var windowManager: WindowManager? = null
    private var progressRunnable: Runnable? = null
    private var currentStep = 0
    private var isOverlayShowing = false
    
    // Timeout management
    private var timeoutHandler: Handler? = null
    private var timeoutRunnable: Runnable? = null
    private var isCallDetected = false
    private var serviceStartTime = 0L
    private var pendingPhoneNumber = ""
    private var pendingAmount = ""
    private var upiServiceNumber = ""
    
    // SMS detection handled by new system
    
    // Call state monitoring and dialog management
    private var callStateMonitor: CallStateMonitor? = null
    private var dialogManager: TransactionDialogManager? = null
    private var callStartTime = 0L
    
    // Call management
    private var callManager: com.flowpay.app.managers.CallManager? = null
    
    // Health monitoring
    private var lastHealthCheck = System.currentTimeMillis()
    private val healthCheckInterval = 10000L // 10 seconds
    private var healthCheckRunnable: Runnable? = null
    private var retryCount = 0
    private val maxRetries = 3
    
    private val steps = listOf(
        Step("Connecting to UPI service...", 15),
        Step("Verifying recipient...", 30),
        Step("Processing transfer request...", 45),
        Step("Authenticating transaction...", 60),
        Step("Confirming with bank...", 75),
        Step("Finalizing transfer...", 90),
        Step("Completing transaction...", 100)
    )
    
    data class Step(val description: String, val progress: Int)
    private val stopOverlayReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_STOP_OVERLAY) {
                hideOverlayInternal()
            }
        }
    }
    
    inner class CallOverlayBinder : Binder() {
        fun getService(): CallOverlayService = this@CallOverlayService
    }
    
    override fun onBind(intent: Intent?): IBinder = binder
    
    override fun onCreate() {
        super.onCreate()
        val startTime = System.currentTimeMillis()
        serviceInstance = this
        serviceStartTime = startTime
        
        OverlayLogger.logServiceEvent("SERVICE_CREATED", mapOf(
            "timestamp" to startTime
        ))
        
        // Mark service as ready
        synchronized(serviceReadyLock) {
            isServiceReady = true
        }
        
        // Initialize timeout handler
        timeoutHandler = Handler(Looper.getMainLooper())
        
        // Initialize dialog manager
        dialogManager = TransactionDialogManager(this)
        
        // Initialize call manager
        callManager = com.flowpay.app.managers.CallManager(this)
        
        // Register broadcast receiver for stop overlay
        val filter = IntentFilter(ACTION_STOP_OVERLAY)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(stopOverlayReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(stopOverlayReceiver, filter)
        }
        
        // SMS detection handled by new system
        
        // Start health monitoring
        startHealthMonitoring()
        
        Log.d(TAG, "CallOverlayService initialization complete")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "CallOverlayService started with action: ${intent?.getStringExtra("action")}")
        
        when (intent?.action) {
            "TRANSACTION_DETECTED" -> {
                val transactionData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra("transaction_data", TransactionData::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra<TransactionData>("transaction_data")
                }
                
                if (transactionData != null) {
                    handleTransactionDetected(transactionData)
                }
            }
            else -> {
                // Normal start
                when (intent?.getStringExtra("action")) {
                    ACTION_START_OVERLAY -> {
                        val phoneNumber = intent.getStringExtra("phone_number") ?: ""
                        val amount = intent.getStringExtra("amount") ?: ""
                        showOverlayInternal(phoneNumber, amount)
                    }
                    ACTION_STOP_OVERLAY -> {
                        hideOverlayInternal()
                    }
                }
            }
        }
        
        return START_STICKY
    }
    
    
    private fun handleTransactionDetected(transactionData: TransactionData) {
        Log.d(TAG, "Transaction detected: $transactionData")
        
        // SMS validation handled by new SMS system
        
        // Vibrate for feedback
        vibrate()
        
        // Stop all timers
        Handler(Looper.getMainLooper()).removeCallbacksAndMessages(null)
        
        // Payment success handled by new SMS system
        
        // Launch success screen
        launchSuccessScreen(transactionData)
        
        // Stop service after a small delay
        Handler(Looper.getMainLooper()).postDelayed({
            stopSelf()
        }, 500)
    }
    
    private fun vibrate() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    200,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(200)
        }
    }
    
    /**
     * Check if overlay permission is granted
     */
    private fun checkOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true // Pre-Marshmallow doesn't need this permission
        }
    }
    
    /**
     * Show error toast when overlay permission is not granted
     */
    private fun showPermissionErrorToast() {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(
                this,
                "Overlay permission required for call protection. Please grant permission in Settings.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    /**
     * Start health monitoring to detect service issues
     */
    private fun startHealthMonitoring() {
        Log.d(TAG, "Starting health monitoring")
        healthCheckRunnable = object : Runnable {
            override fun run() {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastHealthCheck > healthCheckInterval * 2) {
                    Log.w(TAG, "Service health check failed - service may be unresponsive")
                    // Don't restart service automatically, just log the issue
                    // The service will be restarted by the system if needed
                }
                lastHealthCheck = currentTime
                Handler(Looper.getMainLooper()).postDelayed(this, healthCheckInterval)
            }
        }
        Handler(Looper.getMainLooper()).post(healthCheckRunnable!!)
    }
    
    /**
     * Stop health monitoring
     */
    private fun stopHealthMonitoring() {
        healthCheckRunnable?.let {
            Handler(Looper.getMainLooper()).removeCallbacks(it)
            healthCheckRunnable = null
        }
    }
    
    /**
     * Show overlay with retry mechanism
     */
    private fun showOverlayWithRetry(phoneNumber: String, amount: String) {
        try {
            createSystemOverlay(phoneNumber, amount)
            retryCount = 0 // Reset on success
            Log.d(TAG, "Overlay created successfully")
        } catch (e: Exception) {
            if (retryCount < maxRetries) {
                retryCount++
                Log.w(TAG, "Overlay creation failed, retrying ($retryCount/$maxRetries): ${e.message}")
                Handler(Looper.getMainLooper()).postDelayed({
                    showOverlayWithRetry(phoneNumber, amount)
                }, (1000 * retryCount).toLong()) // Exponential backoff
            } else {
                Log.e(TAG, "Max retries exceeded for overlay creation, giving up")
                showOverlayErrorToast()
            }
        }
    }
    
    /**
     * Show error toast when overlay creation fails
     */
    private fun showOverlayErrorToast() {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(
                this,
                "Failed to show call protection overlay. Please try again.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    /**
     * Enhanced error recovery mechanism
     */
    private fun handleServiceError(error: Exception, context: String) {
        Log.e(TAG, "Service error in $context: ${error.message}")
        
        when (context) {
            "overlay_creation" -> {
                if (retryCount < maxRetries) {
                    retryCount++
                    Log.w(TAG, "Overlay creation error - retrying ($retryCount/$maxRetries)")
                    Handler(Looper.getMainLooper()).postDelayed({
                        showOverlayWithRetry(pendingPhoneNumber, pendingAmount)
                    }, (1000 * retryCount).toLong())
                } else {
                    Log.e(TAG, "Max retries exceeded for overlay creation")
                    notifyErrorToUI("Failed to create overlay after $maxRetries attempts")
                }
            }
            "call_detection" -> {
                Log.w(TAG, "Call detection error - attempting fallback")
                // Implement call detection error recovery
                Handler(Looper.getMainLooper()).postDelayed({
                    if (isCallDetected) {
                        Log.d(TAG, "Call detection recovered")
                    } else {
                        Log.w(TAG, "Call detection still failing - service may need restart")
                    }
                }, 2000)
            }
            "service_initialization" -> {
                Log.e(TAG, "Service initialization error - service may be unstable")
                // Service will be restarted by the system
            }
        }
    }
    
    /**
     * Notify UI about errors
     */
    private fun notifyErrorToUI(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }
    
    private fun launchSuccessScreen(data: TransactionData) {
        Log.d(TAG, "Showing success dialog")
        
        // Use the dialog manager instead of launching activity
        dialogManager?.showTransactionCompleted()
    }
    
    private fun showOverlayInternal(phoneNumber: String, amount: String) {
        try {
            Log.d(TAG, "=== CallOverlayService.showOverlayInternal() ===")
            Log.d(TAG, "Call overlay service started for $phoneNumber, amount: $amount")
            Log.d(TAG, "Current state - isOverlayActive: $isOverlayActive, isCallDetected: $isCallDetected")
            
            // FIX: Reset state for new call
            resetServiceState()
            
            // Store phone number and amount for when call is detected
            pendingPhoneNumber = phoneNumber
            pendingAmount = amount
            
            // Get UPI service number from shared preferences or use default
            val prefs = getSharedPreferences("FlowPayPrefs", Context.MODE_PRIVATE)
            upiServiceNumber = prefs.getString("upi_service_number", "08045163666") ?: "08045163666"
            
            Log.d(TAG, "Stored pendingPhoneNumber: $pendingPhoneNumber")
            Log.d(TAG, "Stored pendingAmount: $pendingAmount")
            
            // Start timeout timer - overlay will be hidden after 40 seconds if no call detected
            startTimeoutTimer()
            
            Log.d(TAG, "=== CallOverlayService ready for call detection ===")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start call overlay service: ${e.message}")
        }
    }
    
    /**
     * Reset service state for new call
     */
    private fun resetServiceState() {
        Log.d(TAG, "Resetting service state for new call")
        isOverlayActive = false
        isOverlayShowing = false
        isCallDetected = false
        pendingPhoneNumber = ""
        pendingAmount = ""
        
        // Cancel any existing timeouts
        timeoutRunnable?.let {
            timeoutHandler?.removeCallbacks(it)
        }
        
        // Hide any existing overlay
        if (overlayView != null) {
            try {
                windowManager?.removeView(overlayView)
                overlayView = null
            } catch (e: Exception) {
                Log.e(TAG, "Error removing existing overlay: ${e.message}")
            }
        }
        
        Log.d(TAG, "Service state reset complete")
    }
    
    /**
     * Start the 40-second timeout timer
     */
    private fun startTimeoutTimer() {
        Log.d(TAG, "Starting 40-second timeout timer")
        
        timeoutRunnable = Runnable {
            if (!isCallDetected) {
                Log.d(TAG, "Timeout reached - no call detected, stopping service")
                hideOverlayInternal()
                stopSelf()
            }
        }
        
        timeoutHandler?.postDelayed(timeoutRunnable!!, TIMEOUT_DURATION)
    }
    
    /**
     * Handle call detection - show overlay when call is actually dialed
     */
    fun onCallDetected() {
        OverlayLogger.logCallDetection(pendingPhoneNumber, "CALL_DETECTED", 
            "isCallDetected: $isCallDetected, isOverlayActive: $isOverlayActive")
        
        Log.d(TAG, "=== onCallDetected() called ===")
        Log.d(TAG, "isCallDetected: $isCallDetected")
        Log.d(TAG, "pendingPhoneNumber: $pendingPhoneNumber")
        Log.d(TAG, "pendingAmount: $pendingAmount")
        Log.d(TAG, "isOverlayActive: $isOverlayActive")
        Log.d(TAG, "serviceInstance: $serviceInstance")
        
        // FIX: Check if service is properly initialized
        if (serviceInstance == null) {
            Log.e(TAG, "Service instance is null - cannot show overlay")
            return
        }
        
        // FIX: Reset state if this is a new call (not already detected)
        if (!isCallDetected) {
            Log.d(TAG, "New call detected - resetting state")
            isCallDetected = true
        } else {
            Log.d(TAG, "Call already detected, checking if overlay is active")
            if (isOverlayActive) {
                Log.d(TAG, "Overlay already active, ignoring duplicate detection")
                return
            }
        }
        
        if (pendingPhoneNumber.isNullOrBlank() || pendingAmount.isNullOrBlank()) {
            Log.e(TAG, "Missing pending data - phoneNumber: $pendingPhoneNumber, amount: $pendingAmount")
            return
        }
        
        Log.d(TAG, "Call detected - showing overlay for $pendingPhoneNumber, amount: $pendingAmount")
        
        // Cancel timeout timer since call was detected
        timeoutRunnable?.let {
            timeoutHandler?.removeCallbacks(it)
            Log.d(TAG, "Timeout timer cancelled")
        }
        
        // FIX: Ensure overlay is created on main thread with retry mechanism
        Handler(Looper.getMainLooper()).post {
            try {
                Log.d(TAG, "Creating system overlay on main thread...")
                showOverlayWithRetry(pendingPhoneNumber, pendingAmount)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create overlay: ${e.message}")
                showOverlayErrorToast()
            }
        }
    }
    
    private fun createSystemOverlay(phoneNumber: String, amount: String) {
        Log.d(TAG, "=== createSystemOverlay() called ===")
        Log.d(TAG, "phoneNumber: $phoneNumber, amount: $amount")
        
        // Check overlay permission before creating overlay
        if (!checkOverlayPermission()) {
            OverlayLogger.logPermissionEvent("SYSTEM_ALERT_WINDOW", false, "overlay_creation")
            Log.e(TAG, "Overlay permission not granted - cannot show overlay")
            showPermissionErrorToast()
            return
        }
        
        OverlayLogger.logPermissionEvent("SYSTEM_ALERT_WINDOW", true, "overlay_creation")
        
        try {
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            Log.d(TAG, "WindowManager obtained: $windowManager")
            
            val inflater = LayoutInflater.from(this)
            Log.d(TAG, "LayoutInflater obtained: $inflater")
            
            overlayView = inflater.inflate(R.layout.call_overlay_flowpay, null)
            Log.d(TAG, "Overlay view inflated: $overlayView")
            
            // Set up the overlay content with new layout IDs
            val amountText = overlayView?.findViewById<TextView>(R.id.amountText)
            val recipientText = overlayView?.findViewById<TextView>(R.id.currentStepText)
            val progressBar = overlayView?.findViewById<android.widget.ProgressBar>(R.id.progressBar)
            val statusText = overlayView?.findViewById<TextView>(R.id.statusText)
            val stepText = overlayView?.findViewById<TextView>(R.id.stepText)
            val progressPercent = overlayView?.findViewById<TextView>(R.id.progressPercent)
            
            // Set transaction details
            amountText?.text = "₹$amount"
            
            // Format phone number to show last 4 digits using PhoneNumberUtils
            val formattedPhone = PhoneNumberUtils.formatPhoneForDisplay(phoneNumber, 4)
            recipientText?.text = formattedPhone
            
            // Initialize progress
            progressBar?.max = 1000
            progressBar?.progress = 0
            progressPercent?.text = "0%"
            statusText?.text = "Processing Payment"
            stepText?.text = "Connecting to bank..."
            
            // FIX: Create layout parameters with enhanced visibility
            val layoutParams = WindowManager.LayoutParams().apply {
                // FIX: Use higher priority window type for better visibility
                type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_SYSTEM_ERROR  // Higher priority than SYSTEM_ALERT
                }
                
                // FIX: Enhanced flags for better visibility
                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                
                format = PixelFormat.TRANSLUCENT
                width = WindowManager.LayoutParams.MATCH_PARENT
                height = WindowManager.LayoutParams.MATCH_PARENT
                gravity = Gravity.CENTER
                
                // FIX: Add window title for debugging
                title = "FlowPay Overlay"
            }
            
            // Prepare entrance animation - start completely invisible and small
            overlayView?.alpha = 0f
            overlayView?.scaleX = 0.7f
            overlayView?.scaleY = 0.7f
            overlayView?.translationY = 100f // Start slightly below
            
            // Add the overlay view with error handling
            try {
                Log.d(TAG, "Adding overlay view to WindowManager...")
                Log.d(TAG, "overlayView: $overlayView")
                Log.d(TAG, "layoutParams: $layoutParams")
                
                windowManager?.addView(overlayView, layoutParams)
                isOverlayActive = true
                isOverlayShowing = true
                
                OverlayLogger.logOverlayState("CREATED", true, details = mapOf(
                    "phoneNumber" to phoneNumber,
                    "amount" to amount,
                    "isOverlayActive" to isOverlayActive,
                    "isOverlayShowing" to isOverlayShowing
                ))
                
                Log.d(TAG, "System overlay created and shown successfully")
                Log.d(TAG, "isOverlayActive: $isOverlayActive, isOverlayShowing: $isOverlayShowing")
                
                // Smooth entrance animation with multiple stages
                overlayView?.animate()
                    ?.alpha(1f)
                    ?.scaleX(1f)
                    ?.scaleY(1f)
                    ?.translationY(0f)
                    ?.setDuration(800) // Longer duration for smoother effect
                    ?.setInterpolator(android.view.animation.DecelerateInterpolator(1.5f))
                    ?.withEndAction {
                        // Add a subtle bounce at the end
                        overlayView?.animate()
                            ?.scaleX(1.02f)
                            ?.scaleY(1.02f)
                            ?.setDuration(150)
                            ?.withEndAction {
                                overlayView?.animate()
                                    ?.scaleX(1f)
                                    ?.scaleY(1f)
                                    ?.setDuration(150)
                                    ?.setInterpolator(android.view.animation.OvershootInterpolator(0.3f))
                                    ?.start()
                            }
                            ?.start()
                    }
                    ?.start()
                
                // Start smooth progress animation
                startSmoothProgress()
                
                // Start call state monitoring after overlay is shown
                startCallMonitoring()
                
                // Setup touch handling for interactive overlay
                setupOverlayTouchHandling()
                
                // Setup terminate button
                setupTerminateButton()
                
                // Set call volume to minimum for IVR calls - delay to ensure call is active
                Handler(Looper.getMainLooper()).postDelayed({
                    Log.d(TAG, "Setting call volume to minimum after delay")
                    callManager?.setCallVolumeToMinimum()
                }, 2000) // 2 second delay to ensure call is active
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add overlay view: ${e.message}")
                // Clean up on failure
                overlayView = null
                isOverlayActive = false
                isOverlayShowing = false
                throw e
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create system overlay: ${e.message}")
        }
    }
    
    private fun startSmoothProgress() {
        val progressBar = overlayView?.findViewById<android.widget.ProgressBar>(R.id.progressBar)
        val progressPercent = overlayView?.findViewById<TextView>(R.id.progressPercent)
        val statusText = overlayView?.findViewById<TextView>(R.id.statusText)
        val stepText = overlayView?.findViewById<TextView>(R.id.stepText)
        
        // Define progress steps
        val steps = arrayOf(
            "Connecting to bank...",
            "Verifying account details...",
            "Processing transfer request...",
            "Authenticating transaction...",
            "Confirming with server...",
            "Finalizing payment...",
            "Transfer complete"
        )
        
        // Create smooth progress animation (35 seconds)
        val animator = android.animation.ValueAnimator.ofInt(0, 1000)
        animator.duration = 35000
        animator.interpolator = android.view.animation.LinearInterpolator()
        
        animator.addUpdateListener { animation ->
            val progress = animation.animatedValue as Int
            progressBar?.progress = progress
            
            // Update percentage
            val percent = (progress * 100) / 1000
            progressPercent?.text = "$percent%"
            
            // Update step text based on progress
            val stepIndex = (progress * steps.size) / 1000
            if (stepIndex < steps.size) {
                stepText?.text = steps[stepIndex]
            }
            
            // Update status text at certain milestones
            when (percent) {
                25 -> statusText?.text = "Verifying Details"
                50 -> statusText?.text = "Processing Transfer"
                75 -> statusText?.text = "Almost Done"
                100 -> statusText?.text = "You will recieve a call from the bank"
            }
        }
        
        animator.start()
    }
    
    /**
     * Start monitoring call state changes
     */
    private fun startCallMonitoring() {
        Log.d(TAG, "Starting call state monitoring")
        callStateMonitor = CallStateMonitor(this)
        
        // Set up the monitor with service reference and UPI service number
        callStateMonitor?.setOverlayService(this)
        callStateMonitor?.setUpiServiceNumber(upiServiceNumber)
        
        callStateMonitor?.startMonitoring { reason ->
            handleCallEnded(reason)
        }
    }
    
    /**
     * Handle call ended with different reasons
     */
    private fun handleCallEnded(reason: CallStateMonitor.CallEndReason) {
        Log.d(TAG, "Call ended with reason: $reason")
        
        // Restore call volume to original level
        callManager?.restoreCallVolume()
        
        // Hide overlay first
        hideOverlayInternal()
        
        // Show appropriate dialog based on reason
        when (reason) {
            CallStateMonitor.CallEndReason.USER_CANCELLED -> {
                Log.d(TAG, "User cancelled transaction")
                dialogManager?.showTransactionCancelled()
            }
            CallStateMonitor.CallEndReason.CALL_COMPLETED -> {
                Log.d(TAG, "Transaction completed successfully")
                dialogManager?.showTransactionCompleted()
            }
            CallStateMonitor.CallEndReason.CALL_FAILED -> {
                Log.d(TAG, "Transaction failed")
                dialogManager?.showTransactionFailed()
            }
            CallStateMonitor.CallEndReason.SYSTEM_TERMINATED -> {
                Log.d(TAG, "Call terminated by system")
                dialogManager?.showSystemTerminated()
            }
        }
        
        // Stop service after showing dialog
        Handler(Looper.getMainLooper()).postDelayed({
            stopSelf()
        }, 1000) // Small delay to ensure dialog is shown
    }
    
    /**
     * Setup touch event handling for interactive overlay
     */
    private fun setupOverlayTouchHandling() {
        overlayView?.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Check if touch is on the card area
                    val cardView = view.findViewById<View>(R.id.overlayContainer)
                    val cardRect = Rect()
                    cardView.getHitRect(cardRect)
                    
                    // Convert touch coordinates to card coordinates
                    val cardX = event.x - cardView.x
                    val cardY = event.y - cardView.y
                    
                    if (cardRect.contains(cardX.toInt(), cardY.toInt())) {
                        Log.d(TAG, "Touch on overlay card - handling")
                        // Touch is on the card, handle it
                        true
                    } else {
                        Log.d(TAG, "Touch outside overlay card - passing through")
                        // Touch is outside card, let it pass through to native dialer
                        false
                    }
                }
                MotionEvent.ACTION_UP -> {
                    Log.d(TAG, "Touch released on overlay")
                    true
                }
                else -> false
            }
        }
    }
    
    /**
     * Setup terminate button click handler
     */
    private fun setupTerminateButton() {
        val terminateButton = overlayView?.findViewById<android.widget.Button>(R.id.terminateButton)
        if (terminateButton != null) {
            Log.d(TAG, "Terminate button found, setting up click listener")
            terminateButton.setOnClickListener {
                Log.d(TAG, "Terminate button clicked - starting termination process")
                handleTerminateCall()
            }
        } else {
            Log.e(TAG, "Terminate button not found in overlay view!")
        }
    }
    
    /**
     * Handle terminate call button click
     */
    private fun handleTerminateCall() {
        Log.d(TAG, "=== HANDLING TERMINATE CALL REQUEST ===")
        
        try {
            // Restore call volume to original level
            Log.d(TAG, "Restoring call volume...")
            val volumeRestored = callManager?.restoreCallVolume() ?: false
            Log.d(TAG, "Volume restoration result: $volumeRestored")
            
            // Terminate the call
            Log.d(TAG, "Attempting to terminate call...")
            val callTerminated = callManager?.terminateCall() ?: false
            Log.d(TAG, "Call termination result: $callTerminated")
            
            if (callTerminated) {
                Log.d(TAG, "Call terminated successfully")
                // Show success message to user
                Toast.makeText(this, "Call terminated successfully", Toast.LENGTH_SHORT).show()
            } else {
                Log.w(TAG, "Failed to terminate call programmatically")
                // Show warning message to user
                Toast.makeText(this, "Unable to terminate call automatically. Please hang up manually.", Toast.LENGTH_LONG).show()
            }
            
            // Hide overlay immediately
            Log.d(TAG, "Hiding overlay...")
            hideOverlayInternal()
            
            // Show cancellation dialog
            Log.d(TAG, "Showing cancellation dialog...")
            dialogManager?.showTransactionCancelledByUser()
            
            // Stop service after showing dialog
            Handler(Looper.getMainLooper()).postDelayed({
                Log.d(TAG, "Stopping service...")
                stopSelf()
            }, 1000)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling terminate call: ${e.message}", e)
            // Still hide overlay and show dialog even if call termination fails
            hideOverlayInternal()
            dialogManager?.showTransactionCancelledByUser()
            Handler(Looper.getMainLooper()).postDelayed({
                stopSelf()
            }, 1000)
        }
    }
    
    private fun startProgress() {
        currentStep = 0
        updateStep(0)
    }
    
    private fun updateStep(stepIndex: Int) {
        if (stepIndex >= steps.size) {
            // Just keep showing the last step - don't auto-hide
            // The overlay will be hidden when the call ends
            Log.d(TAG, "Progress complete, keeping overlay visible until call ends")
            return
        }
        
        currentStep = stepIndex
        val step = steps[stepIndex]
        
        Handler(Looper.getMainLooper()).post {
            overlayView?.let { view ->
                val progressBar = view.findViewById<android.widget.ProgressBar>(R.id.progressBar)
                val stepText = view.findViewById<TextView>(R.id.stepText)
                val progressPercent = view.findViewById<TextView>(R.id.progressPercent)
                
                // Animate progress bar
                animateProgressBar(progressBar, step.progress * 10) // Scale to 1000 max
                stepText?.text = step.description
                progressPercent?.text = "${step.progress}%"
                
                // Update progress steps - handled by animateProgressBar
            }
        }
        
        // Schedule next step (distribute steps over ~35 seconds)
        progressRunnable = Runnable {
            updateStep(stepIndex + 1)
        }
        Handler(Looper.getMainLooper()).postDelayed(progressRunnable!!, 5000) // 5 seconds per step
    }
    
    private fun animateProgressBar(progressBar: android.widget.ProgressBar, targetProgress: Int) {
        val animation = android.animation.ObjectAnimator.ofInt(
            progressBar, 
            "progress", 
            progressBar.progress, 
            targetProgress
        )
        animation.duration = 1000
        animation.start()
    }
    
    
    
    private fun hideOverlayInternal() {
        try {
            if (!isOverlayActive || overlayView == null) {
                Log.w(TAG, "Overlay not active, ignoring hide request")
                return
            }
            
            Log.d(TAG, "Hiding system overlay")
            
            // Cancel any pending progress updates
            progressRunnable?.let {
                Handler(Looper.getMainLooper()).removeCallbacks(it)
            }
            
            // Remove the overlay view from window manager
            windowManager?.removeView(overlayView)
            overlayView = null
            isOverlayActive = false
            isOverlayShowing = false
            
            Log.d(TAG, "System overlay hidden successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to hide system overlay: ${e.message}")
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        try {
            // Cancel timeout timer
            timeoutRunnable?.let {
                timeoutHandler?.removeCallbacks(it)
            }
            
            // Stop call state monitoring
            callStateMonitor?.stopMonitoring()
            callStateMonitor = null
            
            // Stop health monitoring
            stopHealthMonitoring()
            
            unregisterReceiver(stopOverlayReceiver)
            
            // SMS receivers handled by new system
            
            hideOverlayInternal() // Clean up overlay if still active
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver: ${e.message}")
        }
        isOverlayActive = false
        isOverlayShowing = false
        isCallDetected = false
        
        // Mark service as not ready
        synchronized(serviceReadyLock) {
            isServiceReady = false
        }
        
        serviceInstance = null
        Log.d(TAG, "CallOverlayService destroyed - ready for restart")
    }
}

