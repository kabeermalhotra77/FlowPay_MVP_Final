package com.flowpay.app.services

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.OvershootInterpolator
import android.view.animation.ScaleAnimation
import android.widget.LinearLayout
import android.widget.TextView
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.flowpay.app.R
import com.flowpay.app.managers.FeedbackManager

class USSDOverlayService : Service() {
    
    companion object {
        private const val TAG = "USSDOverlayService"
        private const val MESSAGE_DURATION = 4000L // 4 seconds
        private const val OVERLAY_HEIGHT_DP = 200
        private const val AUTO_DISMISS_TIMEOUT = 70000L // 70 seconds (as documented)
        
        /**
         * Shows the USSD overlay
         */
        fun showOverlay(context: Context) {
            try {
                Log.d(TAG, "=== STARTING USSD OVERLAY SERVICE ===")
                
                // Check if overlay permission is granted
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!Settings.canDrawOverlays(context)) {
                        Log.e(TAG, "Overlay permission not granted")
                        throw SecurityException("Overlay permission not granted")
                    }
                }
                
                val intent = Intent(context, USSDOverlayService::class.java)
                context.startService(intent)
                Log.d(TAG, "USSD overlay service started successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start USSD overlay service: ${e.message}", e)
                throw e // Re-throw to let caller handle the error
            }
        }
        
        /**
         * Hides the USSD overlay
         */
        fun hideOverlay(context: Context) {
            try {
                Log.d(TAG, "Hiding USSD overlay")
                val intent = Intent(context, USSDOverlayService::class.java)
                intent.action = "HIDE_OVERLAY"
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to hide USSD overlay: ${e.message}", e)
            }
        }
    }
    
    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private lateinit var messageContainer: View
    private lateinit var stepsContainer: LinearLayout
    private lateinit var messageText: TextView
    private lateinit var feedbackManager: FeedbackManager
    
    private val dismissReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "DISMISS_OVERLAY") {
                Log.d(TAG, "Dismiss overlay broadcast received")
                Handler(Looper.getMainLooper()).post {
                    dismissOverlay()
                }
            }
        }
    }
    
    private val appStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "APP_PAUSED", "APP_STOPPED", "APP_DESTROYED" -> {
                    Log.d(TAG, "App state changed to ${intent.action} - terminating USSD overlay")
                    Handler(Looper.getMainLooper()).post {
                        dismissOverlay()
                    }
                }
            }
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "USSDOverlayService created")
        try {
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            feedbackManager = FeedbackManager(this)
            Log.d(TAG, "Window manager obtained successfully")
            setupOverlay()
            registerReceivers()
            Log.d(TAG, "USSDOverlayService setup completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error in USSDOverlayService onCreate: ${e.message}", e)
            stopSelf()
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand called with action: ${intent?.action}")
        
        when (intent?.action) {
            "HIDE_OVERLAY" -> {
                Log.d(TAG, "Received HIDE_OVERLAY action")
                dismissOverlay()
                return START_NOT_STICKY
            }
        }
        
        return START_STICKY
    }
    
    private fun setupOverlay() {
        Log.d(TAG, "Setting up USSD overlay")
        try {
            // Show loading state
            showLoadingState()
            
            overlayView = LayoutInflater.from(this).inflate(R.layout.ussd_overlay_redesigned, null)
            Log.d(TAG, "Overlay layout inflated successfully")
            
            messageContainer = overlayView.findViewById(R.id.messageContainer)
            stepsContainer = overlayView.findViewById(R.id.stepsContainer)
            messageText = overlayView.findViewById(R.id.messageText)
            Log.d(TAG, "Overlay views found successfully")
            
            // Calculate status bar height to position overlay below it
            val statusBarHeight = getStatusBarHeight()
            Log.d(TAG, "Status bar height: ${statusBarHeight}px")
            
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                dpToPx(OVERLAY_HEIGHT_DP),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT  // Higher priority for older devices
                },
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,  // Keep screen on during USSD
                PixelFormat.TRANSLUCENT
            )
            
            params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            // Position overlay below status bar with additional padding
            params.y = statusBarHeight + dpToPx(8) // 8dp padding below status bar
            params.x = 0
            
            // Set higher z-order to show above USSD screen
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // For Android O+, ensure overlay appears above other windows
                params.windowAnimations = android.R.style.Animation_Dialog
            }
            
            Log.d(TAG, "Window manager params created - y position: ${params.y}px, type: ${params.type}")
            
            windowManager.addView(overlayView, params)
            Log.d(TAG, "Overlay view added to window manager successfully")
            
            // Verify overlay is actually visible
            overlayView.post {
                if (overlayView.visibility == View.VISIBLE) {
                    Log.d(TAG, "Overlay view confirmed visible on screen")
                } else {
                    Log.w(TAG, "Overlay view added but not visible, forcing visibility")
                    overlayView.visibility = View.VISIBLE
                }
                
                // Bring to front to ensure visibility above other views
                overlayView.bringToFront()
            }
            
            startContentSequence()
            animateEntry()
            hideLoadingState()
            Log.d(TAG, "Overlay setup completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup overlay: ${e.message}", e)
            Log.e(TAG, "Exception details: ${e.stackTraceToString()}")
            hideLoadingState()
            showErrorState("Failed to setup overlay")
            stopSelf()
        }
    }
    
    private fun showLoadingState() {
        Log.d(TAG, "Showing loading state")
        // This could be implemented with a loading indicator
        // For now, we'll just log the state
    }
    
    private fun hideLoadingState() {
        Log.d(TAG, "Hiding loading state")
        // Hide loading indicator if implemented
    }
    
    private fun showErrorState(message: String) {
        Log.e(TAG, "Showing error state: $message")
        // Could show error message to user
        // For now, we'll just log the error
    }
    
    private fun startContentSequence() {
        Log.d(TAG, "Starting content sequence - showing message first")
        
        // Show message for 4 seconds
        messageContainer.visibility = View.VISIBLE
        stepsContainer.visibility = View.GONE
        
        // Transition to steps after message duration
        Handler(Looper.getMainLooper()).postDelayed({
            transitionToSteps()
        }, MESSAGE_DURATION)
        
        // Auto-dismiss after configured timeout (70 seconds as documented)
        Handler(Looper.getMainLooper()).postDelayed({
            Log.d(TAG, "Auto-dismissing USSD overlay after timeout")
            dismissOverlay()
        }, AUTO_DISMISS_TIMEOUT)
    }
    
    private fun transitionToSteps() {
        Log.d(TAG, "Transitioning from message to steps")
        
        // Fade out message
        messageContainer.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                messageContainer.visibility = View.GONE
                stepsContainer.visibility = View.VISIBLE
                
                // Fade in steps
                stepsContainer.alpha = 0f
                stepsContainer.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start()
                
                Log.d(TAG, "Steps now visible")
            }
            .start()
    }
    
    private fun animateEntry() {
        Log.d(TAG, "Animating overlay entry")
        
        // Set initial state
        overlayView.alpha = 0f
        overlayView.scaleX = 0.85f
        overlayView.scaleY = 0.85f
        overlayView.translationY = -50f  // Start from above
        
        // Animate entry with overshoot
        overlayView.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .translationY(0f)
            .setDuration(600)
            .setInterpolator(OvershootInterpolator(1.2f))
            .withEndAction {
                Log.d(TAG, "Overlay entry animation completed successfully")
            }
            .start()
    }
    
    fun dismissOverlay() {
        Log.d(TAG, "Dismissing overlay")
        
        // Check if overlay view is still attached
        if (!::overlayView.isInitialized) {
            Log.w(TAG, "Overlay view not initialized, stopping service")
            stopSelf()
            return
        }
        
        try {
            // Animate exit with scale down
            overlayView.animate()
                .alpha(0f)
                .scaleX(0.9f)
                .scaleY(0.9f)
                .translationY(-50f)  // Move up while fading
                .setDuration(400)
                .withEndAction {
                    try {
                        windowManager.removeView(overlayView)
                        Log.d(TAG, "Overlay view removed from window manager")
                        stopSelf()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error removing overlay view", e)
                        // Force stop even if removal fails
                        stopSelf()
                    }
                }
                .start()
        } catch (e: Exception) {
            Log.e(TAG, "Error during overlay dismissal animation", e)
            // Force stop on any error
            try {
                windowManager.removeView(overlayView)
            } catch (removeError: Exception) {
                Log.e(TAG, "Error force-removing overlay", removeError)
            }
            stopSelf()
        }
    }
    
    private fun registerReceivers() {
        Log.d(TAG, "Registering broadcast receivers")
        val dismissFilter = IntentFilter("DISMISS_OVERLAY")
        LocalBroadcastManager.getInstance(this).registerReceiver(dismissReceiver, dismissFilter)
        
        val appStateFilter = IntentFilter().apply {
            addAction("APP_PAUSED")
            addAction("APP_STOPPED")
            addAction("APP_DESTROYED")
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(appStateReceiver, appStateFilter)
    }
    
    /**
     * Get the status bar height to position overlay properly
     */
    private fun getStatusBarHeight(): Int {
        var result = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId)
        }
        // Fallback to default if we can't get the actual height
        if (result == 0) {
            result = dpToPx(24) // Standard status bar height
        }
        return result
    }
    
    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "USSDOverlayService destroyed")
        
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(dismissReceiver)
            LocalBroadcastManager.getInstance(this).unregisterReceiver(appStateReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receivers", e)
        }
        
        if (::overlayView.isInitialized) {
            try {
                windowManager.removeView(overlayView)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing overlay in onDestroy", e)
            }
        }
    }
}