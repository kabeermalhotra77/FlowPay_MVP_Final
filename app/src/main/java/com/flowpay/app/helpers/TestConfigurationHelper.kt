package com.flowpay.app.helpers

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.flowpay.app.MainActivity
import com.flowpay.app.data.TestResults
import com.flowpay.app.data.TestResultsManager
import com.flowpay.app.managers.CallManager
import com.flowpay.app.managers.CallType
import com.flowpay.app.managers.PermissionManager

/**
 * Helper class containing all business logic for TestConfigurationActivity
 * This separates the UI concerns from the business logic
 */
class TestConfigurationHelper(
    private val context: Context,
    private val uiCallback: UICallback
) {
    companion object {
        private const val TAG = "TestConfigurationHelper"
    }

    // Managers
    private lateinit var callManager: CallManager
    private lateinit var testResultsManager: TestResultsManager
    private lateinit var permissionManager: PermissionManager
    
    // Test state variables
    private var currentTestingType: CallType? = null
    private var ussdTesting = false
    private var showUssdDialog = false
    private var ussdTestCompleted = false
    private var showCallCompleteButton = false
    private var upi123TestCompleted = false
    private var voiceTesting = false
    private var showVoiceDialog = false
    private var voiceTestCompleted = false
    private var upi123Testing = false
    private var showUpi123Dialog = false
    private var showUpi123ConfigurationOptions = false
    private var ussdProgressMessage = "Initializing..."
    private var showUssdConfigurationOptions = false

    /**
     * Interface for UI callbacks
     */
    interface UICallback {
        fun showToast(message: String)
        fun updateUssdTesting(isTesting: Boolean)
        fun updateUssdDialog(show: Boolean)
        fun updateUssdTestCompleted(completed: Boolean)
        fun updateUpi123Testing(isTesting: Boolean)
        fun updateUpi123TestCompleted(completed: Boolean)
        fun updateUpi123Dialog(show: Boolean)
        fun updateUpi123ConfigurationOptions(show: Boolean)
        fun updateVoiceTesting(isTesting: Boolean)
        fun updateVoiceDialog(show: Boolean)
        fun updateVoiceTestCompleted(completed: Boolean)
        fun updateCallCompleteButton(show: Boolean)
        fun updateUssdProgressMessage(message: String)
        fun updateUssdConfigurationOptions(show: Boolean)
        fun navigateToMain()
    }
    

    /**
     * Initialize all managers
     */
    fun initialize() {
        callManager = CallManager(context)
        testResultsManager = TestResultsManager(context)
        permissionManager = PermissionManager(context as Activity)
        
        // Check and request permissions
        if (!permissionManager.checkAllPermissions()) {
            permissionManager.requestRequiredPermissions()
        }
    }

    /**
     * Handle permission results
     */
    fun handlePermissionResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray): Boolean {
        val success = permissionManager.handlePermissionResult(requestCode, permissions, grantResults)
        if (success) {
            Log.d(TAG, "All permissions granted")
            uiCallback.showToast("Permissions granted! You can now test USSD")
        } else {
            Log.w(TAG, "Some permissions denied")
            uiCallback.showToast("Some permissions were denied. App may not work properly.")
        }
        return success
    }

    /**
     * Initiate a test call
     */
    fun initiateCall(callType: CallType) {
        // Check permissions before initiating call
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.CALL_PHONE) != 
            PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "CALL_PHONE permission not granted")
            uiCallback.showToast("Phone call permission is required to test USSD")
            permissionManager.requestRequiredPermissions()
            return
        }
        
        val number = when (callType) {
            CallType.USSD -> "*99#"
            CallType.VOICE -> "1234567890"
            CallType.UPI123 -> "1234567890" // This should not be used anymore
            CallType.MANUAL_TRANSFER -> "1234567890"
        }
        
        // Update UI state
        when (callType) {
            CallType.USSD -> {
                Log.d(TAG, "Initiating USSD setup for *99# - will show dialog after 25 seconds")
                ussdTesting = true
                showUssdDialog = true
                ussdProgressMessage = "Dialing *99#..."
                uiCallback.updateUssdTesting(true)
                uiCallback.updateUssdDialog(true)
                uiCallback.updateUssdProgressMessage(ussdProgressMessage)
                Log.d(TAG, "USSD state set - ussdTesting: $ussdTesting, showUssdDialog: $showUssdDialog")
            }
            CallType.VOICE -> {
                voiceTesting = true
                showVoiceDialog = true
                uiCallback.updateVoiceTesting(true)
                uiCallback.updateVoiceDialog(true)
            }
            CallType.MANUAL_TRANSFER -> {
                // Manual transfer testing - similar to voice call
                voiceTesting = true
                showVoiceDialog = true
                uiCallback.updateVoiceTesting(true)
                uiCallback.updateVoiceDialog(true)
            }
            CallType.UPI123 -> {
                // UPI123 should use initiateUpi123Test() method instead
                Log.w(TAG, "UPI123 should use initiateUpi123Test() method instead of initiateCall()")
                return
            }
        }
        
        currentTestingType = callType
        
        // For USSD, start the 25-second timeout
        if (callType == CallType.USSD) {
            startUssdTimeout() // Start 25-second timeout
        }
        
        callManager.initiateCall(
            context = context,
            phoneNumber = number,
            callType = callType,
            onCallEnded = { type ->
                when (type) {
                    CallType.VOICE -> {
                        voiceTesting = false
                        showVoiceDialog = false
                        voiceTestCompleted = true
                        showCallCompleteButton = true
                        uiCallback.updateVoiceTesting(false)
                        uiCallback.updateVoiceDialog(false)
                        uiCallback.updateVoiceTestCompleted(true)
                        uiCallback.updateCallCompleteButton(true)
                    }
                    CallType.MANUAL_TRANSFER -> {
                        voiceTesting = false
                        showVoiceDialog = false
                        voiceTestCompleted = true
                        showCallCompleteButton = true
                        uiCallback.updateVoiceTesting(false)
                        uiCallback.updateVoiceDialog(false)
                        uiCallback.updateVoiceTestCompleted(true)
                        uiCallback.updateCallCompleteButton(true)
                    }
                    CallType.UPI123 -> {
                        // UPI123 is handled by initiateUpi123Test() method
                        Log.w(TAG, "UPI123 call ended in main initiateCall - this should not happen")
                    }
                    CallType.USSD -> {
                        // USSD call ended - just update button state
                        showCallCompleteButton = true
                        uiCallback.updateCallCompleteButton(true)
                    }
                }
                currentTestingType = null
            }
        )
    }

    // Simplified dialog logic - using simple 25-second timeout for USSD and immediate dialog for UPI123
    
    /**
     * Start 25-second timeout for USSD setup
     */
    private fun startUssdTimeout() {
        Log.d(TAG, "Starting 25-second USSD timeout timer")
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        
        // After 25 seconds, show configuration dialog
        handler.postDelayed({
            Log.d(TAG, "USSD 25-second timeout reached - showing configuration dialog")
            if (ussdTesting && !ussdTestCompleted) {
                // Hide progress dialog and show configuration dialog
                showUssdDialog = false
                showUssdConfigurationOptions = true
                uiCallback.updateUssdDialog(false)
                uiCallback.updateUssdConfigurationOptions(true)
                Log.d(TAG, "Configuration dialog shown after 25 seconds - showUssdDialog: $showUssdDialog, showUssdConfigurationOptions: $showUssdConfigurationOptions")
            } else {
                Log.d(TAG, "USSD timeout reached but conditions not met - ussdTesting: $ussdTesting, ussdTestCompleted: $ussdTestCompleted")
            }
        }, 25000) // 25 seconds
    }
    
    /**
     * Handle USSD configuration confirmation
     */
    fun handleUssdConfigurationConfirmation(configured: Boolean) {
        Log.d(TAG, "USSD configuration confirmation: $configured")
        
        showUssdConfigurationOptions = false
        showUssdDialog = false
        ussdTesting = false
        
        if (configured) {
            ussdTestCompleted = true
            showCallCompleteButton = true
            uiCallback.updateUssdTestCompleted(true)
            uiCallback.updateCallCompleteButton(true)
            saveTestResults(ussdTestCompleted, upi123TestCompleted)
            uiCallback.showToast("USSD setup marked as completed")
        } else {
            uiCallback.showToast("USSD setup not completed - you can try again")
        }
        
        uiCallback.updateUssdConfigurationOptions(false)
        uiCallback.updateUssdDialog(false)
        uiCallback.updateUssdTesting(false)
    }
    
    /**
     * Handle UPI123 configuration confirmation
     */
    fun handleUpi123ConfigurationConfirmation(configured: Boolean) {
        Log.d(TAG, "UPI123 configuration confirmation: $configured")
        
        showUpi123ConfigurationOptions = false
        showUpi123Dialog = false
        upi123Testing = false
        
        if (configured) {
            upi123TestCompleted = true
            showCallCompleteButton = true
            uiCallback.updateUpi123TestCompleted(true)
            uiCallback.updateCallCompleteButton(true)
            saveTestResults(ussdTestCompleted, upi123TestCompleted)
            uiCallback.showToast("UPI123 setup completed successfully")
        } else {
            uiCallback.showToast("UPI123 setup not completed. You can try again later.")
        }
        
        uiCallback.updateUpi123ConfigurationOptions(false)
        uiCallback.updateUpi123Dialog(false)
        uiCallback.updateUpi123Testing(false)
    }
    
    
    /**
     * Initiate UPI123 test - simplified flow - show dialog when call ends
     */
    fun initiateUpi123Test() {
        Log.d(TAG, "initiateUpi123Test called - upi123TestCompleted: $upi123TestCompleted, upi123Testing: $upi123Testing")
        if (!upi123TestCompleted && !upi123Testing) {
            Log.d(TAG, "Starting UPI123 test")
            upi123Testing = true
            showUpi123Dialog = true
            uiCallback.updateUpi123Testing(true)
            uiCallback.updateUpi123Dialog(true)
            
            // Initiate UPI123 call - simple flow
            callManager.initiateCall(
                context = context,
                phoneNumber = "08045163666", // Use the specific number for UPI123
                callType = CallType.UPI123,
                onCallEnded = { type ->
                    if (type == CallType.UPI123 && upi123Testing) {
                        Log.d(TAG, "UPI123 call ended - showing configuration dialog after 2 second delay")
                        
                        // Stop testing state
                        upi123Testing = false
                        uiCallback.updateUpi123Testing(false)
                        
                        // Add 2-second delay before showing configuration dialog
                        val handler = android.os.Handler(android.os.Looper.getMainLooper())
                        handler.postDelayed({
                            if (!upi123TestCompleted) {
                                showUpi123Dialog = false
                                showUpi123ConfigurationOptions = true
                                uiCallback.updateUpi123Dialog(false)
                                uiCallback.updateUpi123ConfigurationOptions(true)
                                Log.d(TAG, "UPI123 configuration dialog shown after 2 second delay")
                            }
                        }, 2000) // 2 second delay
                    }
                }
            )
        } else {
            Log.d(TAG, "UPI123 test not started - already completed or testing")
        }
    }

    /**
     * Save test results
     */
    private fun saveTestResults(ussdCompleted: Boolean, upi123Completed: Boolean) {
        val results = TestResults(
            ussdEnabled = ussdCompleted,
            upi123Enabled = upi123Completed
        )
        testResultsManager.saveTestResults(results)
    }

    /**
     * Get existing test results
     */
    fun getTestResults(): TestResults? {
        return testResultsManager.getTestResults()
    }

    /**
     * Skip tests and continue to main
     */
    fun skipTests() {
        // Save current test results before continuing
        saveTestResults(ussdTestCompleted, upi123TestCompleted)
        
        // Mark test configuration as completed even when skipped
        val sharedPreferences = context.getSharedPreferences("FlowPayPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .putBoolean("test_configuration_completed", true)
            .apply()
        
        uiCallback.navigateToMain()
    }

    /**
     * Continue to main after completing tests
     */
    fun continueToMain() {
        // Save test results before continuing
        saveTestResults(ussdTestCompleted, upi123TestCompleted)
        
        // Mark test configuration as completed
        val sharedPreferences = context.getSharedPreferences("FlowPayPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .putBoolean("test_configuration_completed", true)
            .apply()
        
        uiCallback.navigateToMain()
    }

    /**
     * Check if tests can continue
     */
    fun canContinue(): Boolean {
        return ussdTestCompleted || upi123TestCompleted
    }

    /**
     * Check if all tests are completed
     */
    fun allTestsCompleted(): Boolean {
        return ussdTestCompleted && upi123TestCompleted
    }
    
    /**
     * Reset all test configurations
     */
    fun resetAllTests() {
        Log.d(TAG, "Resetting all test configurations")
        
        // Reset USSD test
        ussdTesting = false
        showUssdDialog = false
        ussdTestCompleted = false
        
        // Reset UPI123 test
        upi123Testing = false
        upi123TestCompleted = false
        
        // Reset voice test
        voiceTesting = false
        showVoiceDialog = false
        voiceTestCompleted = false
        
        // Reset call complete button
        showCallCompleteButton = false
        
        // Update UI callbacks
        uiCallback.updateUssdTesting(false)
        uiCallback.updateUssdDialog(false)
        uiCallback.updateUssdTestCompleted(false)
        uiCallback.updateUpi123Testing(false)
        uiCallback.updateUpi123TestCompleted(false)
        uiCallback.updateVoiceTesting(false)
        uiCallback.updateVoiceDialog(false)
        uiCallback.updateVoiceTestCompleted(false)
        uiCallback.updateCallCompleteButton(false)
        
        // Clear saved test results
        testResultsManager.clearTestResults()
        
        uiCallback.showToast("All test configurations have been reset")
        Log.d(TAG, "All test configurations reset successfully")
    }

    /**
     * Get current test states
     */
    fun getTestStates(): TestStates {
        return TestStates(
            ussdTesting = ussdTesting,
            showUssdDialog = showUssdDialog,
            ussdTestCompleted = ussdTestCompleted,
            upi123Testing = upi123Testing,
            upi123TestCompleted = upi123TestCompleted,
            voiceTesting = voiceTesting,
            showVoiceDialog = showVoiceDialog,
            voiceTestCompleted = voiceTestCompleted,
            showCallCompleteButton = showCallCompleteButton,
            ussdProgressMessage = ussdProgressMessage,
            showUssdConfigurationOptions = showUssdConfigurationOptions,
            showUpi123Dialog = showUpi123Dialog,
            showUpi123ConfigurationOptions = showUpi123ConfigurationOptions
        )
    }
    
    /**
     * Get current USSD progress message
     */
    fun getUssdProgressMessage(): String {
        return ussdProgressMessage
    }
    

    /**
     * Data class for test states
     */
    data class TestStates(
        val ussdTesting: Boolean,
        val showUssdDialog: Boolean,
        val ussdTestCompleted: Boolean,
        val upi123Testing: Boolean,
        val upi123TestCompleted: Boolean,
        val voiceTesting: Boolean,
        val showVoiceDialog: Boolean,
        val voiceTestCompleted: Boolean,
        val showCallCompleteButton: Boolean,
        val ussdProgressMessage: String,
        val showUssdConfigurationOptions: Boolean,
        val showUpi123Dialog: Boolean,
        val showUpi123ConfigurationOptions: Boolean
    )
}
