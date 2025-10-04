package com.flowpay.app.features.qr_scanner.presentation

import android.Manifest
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.flowpay.app.R
import com.flowpay.app.constants.PermissionConstants
import com.flowpay.app.data.UPIData
import com.flowpay.app.features.qr_scanner.domain.QRCodeAnalyzer
import com.flowpay.app.helpers.TransactionDetector
import com.flowpay.app.managers.PermissionManager
// DISABLED: USSDOverlay functionality temporarily disabled
// import com.flowpay.app.services.USSDOverlayService
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class QRScannerActivity : ComponentActivity() {
    
    companion object {
        const val RESULT_CANCELLED = 0
        const val RESULT_SUCCESS = 1
        const val RESULT_ERROR = 2
    }
    
    private lateinit var viewFinder: PreviewView
    private lateinit var scannerOverlay: View
    private lateinit var progressBar: View
    private lateinit var tvStatus: TextView
    private lateinit var btnBack: View
    private lateinit var btnTerminate: View
    private lateinit var topBar: View
    private lateinit var instructionsBox: View
    private lateinit var instructionsHeaderInitial: View
    private lateinit var instructionsExpanded: View
    private lateinit var cameraExecutor: ExecutorService
    private var imageAnalyzer: ImageAnalysis? = null
    private lateinit var permissionManager: PermissionManager
    private var isUSSDProcessActive = false
    private var messageHandler: android.os.Handler? = null
    private var smsTimeoutHandler: android.os.Handler? = null
    private var smsTimeoutRunnable: Runnable? = null
    
    // FIX: Add flag to prevent multiple QR code processing
    private var isProcessingQRCode = false
    
    // Permission launcher for runtime permissions
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        handlePermissionResult(permissions)
    }
    
    // Overlay permission launcher
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        handleOverlayPermissionResult()
    }
    
    // Broadcast receiver for USSD overlay dismissal
    private val overlayDismissReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "DISMISS_OVERLAY") {
                Log.d("QRScanner", "USSD overlay dismissed - continuing to wait for SMS")
                if (isUSSDProcessActive) {
                    // Don't close immediately, wait for SMS
                    updateBlackScreenStatus("USSD completed! Waiting for transaction confirmation...")
                }
            }
        }
    }
    
    // Broadcast receiver for SMS detection
    private val smsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.flowpay.app.SMS_RECEIVED") {
                Log.d("QRScanner", "SMS received - transaction successful!")
                if (isUSSDProcessActive) {
                    handleSMSReceived(intent)
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            Log.d("QRScanner", "=== QR SCANNER ACTIVITY CREATED ===")
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_qr_scanner)
            
            // Initialize views
            viewFinder = findViewById(R.id.viewFinder)
            scannerOverlay = findViewById(R.id.scannerOverlay)
            progressBar = findViewById(R.id.progressBar)
            tvStatus = findViewById(R.id.tvStatus)
            btnBack = findViewById(R.id.btnBack)
            btnTerminate = findViewById(R.id.btnTerminate)
            topBar = findViewById(R.id.topBar)
            instructionsBox = findViewById(R.id.instructionsBox)
            instructionsHeaderInitial = findViewById(R.id.instructionsHeaderInitial)
            instructionsExpanded = findViewById(R.id.instructionsExpanded)
            
            Log.d("QRScanner", "Views initialized successfully")
            
            // Initialize permission manager
            permissionManager = PermissionManager(this)
            Log.d("QRScanner", "Permission manager initialized")
            
            cameraExecutor = Executors.newSingleThreadExecutor()
            Log.d("QRScanner", "Camera executor created")
            
            btnBack.setOnClickListener {
                Log.d("QRScanner", "Back button clicked")
                setResult(RESULT_CANCELLED)
                finish()
            }
            
            btnTerminate.setOnClickListener {
                Log.d("QRScanner", "Terminate button clicked")
                terminateUSSDProcess()
            }
            
            // Register broadcast receivers
            val overlayFilter = IntentFilter("DISMISS_OVERLAY")
            LocalBroadcastManager.getInstance(this).registerReceiver(overlayDismissReceiver, overlayFilter)
            
            val smsFilter = IntentFilter("com.flowpay.app.SMS_RECEIVED")
            LocalBroadcastManager.getInstance(this).registerReceiver(smsReceiver, smsFilter)
            
            // Check permissions before starting camera
            Log.d("QRScanner", "Starting permission check...")
            checkPermissionsAndStartCamera()
            
        } catch (e: Exception) {
            Log.e("QRScanner", "Error in onCreate: ${e.message}", e)
            Toast.makeText(this, "Failed to initialize QR scanner: ${e.message}", Toast.LENGTH_LONG).show()
            setResult(RESULT_ERROR)
            finish()
        }
    }
    
    /**
     * Check all required permissions before starting camera
     */
    private fun checkPermissionsAndStartCamera() {
        Log.d("QRScanner", "Checking permissions before starting camera")
        
        // Check basic permissions first
        if (!permissionManager.checkAllPermissions()) {
            Log.d("QRScanner", "Basic permissions not granted, requesting...")
            showPermissionExplanationDialog()
            return
        }
        
        // Check overlay permission for USSD functionality
        if (!permissionManager.canDrawOverlays()) {
            Log.d("QRScanner", "Overlay permission not granted, requesting...")
            requestOverlayPermission()
            return
        }
        
        // All permissions granted, start camera
        Log.d("QRScanner", "All permissions granted, starting camera")
        startCamera()
    }
    
    /**
     * Show explanation dialog for required permissions
     */
    private fun showPermissionExplanationDialog() {
        val missingPermissions = permissionManager.getMissingPermissions()
        val permissionNames = missingPermissions.mapNotNull { permission ->
            PermissionConstants.PERMISSION_DESCRIPTIONS[permission]
        }
        
        val message = buildString {
            append("FlowPay needs the following permissions to scan QR codes and process payments:\n\n")
            permissionNames.forEach { description ->
                append("• $description\n")
            }
            append("\nThese permissions are essential for:\n")
            append("• Scanning QR codes for payment information\n")
            append("• Making USSD calls for payments\n")
            append("• Receiving payment confirmations\n")
            append("• Showing payment guidance overlays\n\n")
            append("Please grant all permissions to continue.")
        }
        
        AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage(message)
            .setPositiveButton("Grant Permissions") { _, _ ->
                requestBasicPermissions()
            }
            .setNegativeButton("Cancel") { _, _ ->
                setResult(RESULT_CANCELLED)
                finish()
            }
            .setCancelable(false)
            .show()
    }
    
    /**
     * Request basic permissions required for QR scanning and USSD
     */
    private fun requestBasicPermissions() {
        val missingPermissions = permissionManager.getMissingPermissions()
        if (missingPermissions.isNotEmpty()) {
            Log.d("QRScanner", "Requesting missing permissions: ${missingPermissions.joinToString()}")
            permissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }
    
    /**
     * Request overlay permission for USSD functionality
     */
    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Show explanation dialog first
            AlertDialog.Builder(this)
                .setTitle("Overlay Permission Required")
                .setMessage("FlowPay needs overlay permission to show payment guidance during USSD calls.\n\n" +
                    "This helps protect you from fraud by showing secure payment instructions.\n\n" +
                    "Please grant overlay permission to continue.")
                .setPositiveButton("Grant Overlay Permission") { _, _ ->
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    overlayPermissionLauncher.launch(intent)
                }
                .setNegativeButton("Cancel") { _, _ ->
                    setResult(RESULT_CANCELLED)
                    finish()
                }
                .setCancelable(false)
                .show()
        }
    }
    
    /**
     * Handle basic permission request results
     */
    private fun handlePermissionResult(permissions: Map<String, Boolean>) {
        val allGranted = permissions.values.all { it }
        
        if (allGranted) {
            Log.d("QRScanner", "All basic permissions granted")
            // Check overlay permission next
            if (!permissionManager.canDrawOverlays()) {
                requestOverlayPermission()
            } else {
                startCamera()
            }
        } else {
            val deniedPermissions = permissions.filter { !it.value }.keys
            Log.w("QRScanner", "Some permissions denied: $deniedPermissions")
            
            val deniedNames = deniedPermissions.mapNotNull { permission ->
                PermissionConstants.PERMISSION_DESCRIPTIONS[permission]
            }
            
            val message = buildString {
                append("The following permissions were denied:\n\n")
                deniedNames.forEach { description ->
                    append("• $description\n")
                }
                append("\nWithout these permissions, QR scanning and payment processing cannot work.\n\n")
                append("Please go to Settings > Apps > FlowPay > Permissions and grant all required permissions.")
            }
            
            showPermissionError(message)
        }
    }
    
    /**
     * Handle overlay permission result
     */
    private fun handleOverlayPermissionResult() {
        if (permissionManager.canDrawOverlays()) {
            Log.d("QRScanner", "Overlay permission granted")
            startCamera()
        } else {
            Log.w("QRScanner", "Overlay permission denied")
            showPermissionError("Overlay permission is required for USSD payment guidance.")
        }
    }
    
    /**
     * Show permission error and close activity
     */
    private fun showPermissionError(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage(message)
            .setPositiveButton("OK") { _, _ ->
                setResult(RESULT_ERROR)
                finish()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun startCamera() {
        try {
            Log.d("QRScanner", "Starting camera initialization...")
            val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
            
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    Log.d("QRScanner", "Camera provider obtained successfully")
                    
                    // Preview
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(viewFinder.surfaceProvider)
                    }
                    Log.d("QRScanner", "Preview configured")
                    
                    // QR Code Analysis
                    imageAnalyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also {
                            it.setAnalyzer(cameraExecutor, QRCodeAnalyzer { qrCode ->
                                processQRCode(qrCode)
                            })
                        }
                    Log.d("QRScanner", "Image analyzer configured")
                    
                    // Select back camera
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    
                    try {
                        // Unbind use cases before rebinding
                        cameraProvider.unbindAll()
                        Log.d("QRScanner", "Previous use cases unbound")
                        
                        // Bind use cases to camera
                        cameraProvider.bindToLifecycle(
                            this, cameraSelector, preview, imageAnalyzer!!
                        )
                        Log.d("QRScanner", "Camera use cases bound successfully")
                    } catch (exc: Exception) {
                        Log.e("QRScanner", "Use case binding failed", exc)
                        runOnUiThread {
                            Toast.makeText(this, "Camera initialization failed: ${exc.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                    
                } catch (e: Exception) {
                    Log.e("QRScanner", "Error in camera setup: ${e.message}", e)
                    runOnUiThread {
                        Toast.makeText(this, "Camera setup failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
                
            }, ContextCompat.getMainExecutor(this))
        } catch (e: Exception) {
            Log.e("QRScanner", "Failed to start camera: ${e.message}", e)
            Toast.makeText(this, "Failed to start camera: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun processQRCode(qrCode: String) {
        try {
            // FIX: Check if already processing a QR code to prevent multiple dials
            if (isProcessingQRCode) {
                Log.d("QRScanner", "Already processing QR code, ignoring duplicate detection")
                return
            }
            
            // Set processing flag
            isProcessingQRCode = true
            
            // Stop scanning
            imageAnalyzer?.clearAnalyzer()
            
            Log.d("QRScanner", "QR Code detected: ${qrCode.take(100)}...")
            
            // Show loading
            runOnUiThread {
                scannerOverlay.visibility = View.GONE
                progressBar.visibility = View.VISIBLE
                tvStatus.text = "Processing QR code..."
                tvStatus.visibility = View.VISIBLE
            }
            
            // Parse UPI data
            val upiData = parseUPIData(qrCode)
            if (upiData != null && upiData.vpa.isNotEmpty()) {
                Log.d("QRScanner", "Successfully parsed UPI data: VPA=${upiData.vpa}")
                // Process payment
                initiateUSSDPayment(upiData)
            } else {
                Log.w("QRScanner", "Failed to parse UPI data or empty VPA")
                showError("Invalid UPI QR code. Please scan a valid UPI payment QR code.")
                // Reset processing flag on error
                isProcessingQRCode = false
            }
        } catch (e: Exception) {
            Log.e("QRScanner", "Error processing QR code", e)
            showError("Error processing QR code. Please try again.")
            // Reset processing flag on error
            isProcessingQRCode = false
        }
    }
    
    private fun parseUPIData(upiString: String): UPIData? {
        return try {
            val upiData = com.flowpay.app.features.qr_scanner.domain.QRCodeParser.parseUPIQRCode(upiString)
            if (upiData.vpa.isNotEmpty()) {
                upiData
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("QRScanner", "Failed to parse UPI data", e)
            null
        }
    }
    
    private fun initiateUSSDPayment(upiData: UPIData) {
        // Always proceed with payment - skip amount dialog
        // VPA will be copied to clipboard and USSD will be dialed instantly
        proceedWithPayment(upiData)
    }
    
    
    private fun proceedWithPayment(upiData: UPIData) {
        try {
            Log.d("QRScanner", "=== STARTING SIMPLE QR PAYMENT PROCESS ===")
            Log.d("QRScanner", "VPA: ${upiData.vpa}")
            Log.d("QRScanner", "Amount: ${upiData.amount}")
            
            // Validate VPA
            if (upiData.vpa.isBlank()) {
                Log.e("QRScanner", "VPA is blank, cannot proceed")
                showError("Invalid VPA. Please scan a valid UPI QR code.")
                return
            }
            
            // Check permissions before proceeding
            if (!permissionManager.checkAllPermissions()) {
                Log.e("QRScanner", "Required permissions not granted")
                showError("Required permissions not granted. Please check app settings.")
                return
            }
            
            if (!permissionManager.canDrawOverlays()) {
                Log.e("QRScanner", "Overlay permission not granted")
                showError("Overlay permission required for payment guidance.")
                return
            }
            
            Log.d("QRScanner", "All permissions validated successfully")
            
            // Start SMS monitoring for QR payment
            try {
                TransactionDetector.getInstance(this).startOperation(
                    operationType = "QR_SCAN",
                    expectedAmount = upiData.amount
                )
                Log.d("QRScanner", "SMS monitoring started for QR payment")
            } catch (e: Exception) {
                Log.e("QRScanner", "Failed to start SMS monitoring: ${e.message}")
                showError("Failed to initialize payment system. Please try again.")
                return
            }
            
            // Show black screen with status
            showBlackScreenWithStatus("Processing payment...")
            
            // 1. Copy VPA to clipboard
            try {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("VPA", upiData.vpa)
                clipboard.setPrimaryClip(clip)
                Log.d("QRScanner", "VPA copied to clipboard successfully")
                
                // Update status
                updateBlackScreenStatus("VPA copied to clipboard")
            } catch (e: Exception) {
                Log.e("QRScanner", "Failed to copy VPA to clipboard: ${e.message}")
                // Continue anyway, this is not critical
            }
            
            // 2. Show USSD overlay - DISABLED
            // DISABLED: USSDOverlay functionality temporarily disabled
            // try {
            //     Log.d("QRScanner", "Starting USSD overlay service...")
            //     updateBlackScreenStatus("Starting payment guidance...")
            //     USSDOverlayService.showOverlay(this)
            //     Log.d("QRScanner", "USSD overlay service started successfully")
            //     updateBlackScreenStatus("Payment guidance ready")
            // } catch (e: Exception) {
            //     Log.e("QRScanner", "Failed to start USSD overlay: ${e.message}", e)
            //     showError("Failed to start payment guidance: ${e.message}")
            //     return
            // }
            
            // 3. Dial USSD code after a short delay to ensure overlay is ready
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    Log.d("QRScanner", "Dialing USSD code: *99*1*3#")
                    updateBlackScreenStatus("Initiating USSD call...")
                    dialUSSD()
                } catch (e: Exception) {
                    Log.e("QRScanner", "Failed to dial USSD: ${e.message}", e)
                    showError("Failed to initiate USSD call: ${e.message}")
                }
            }, 1000) // 1 second delay to ensure overlay is ready
            
        } catch (e: Exception) {
            Log.e("QRScanner", "Unexpected error in proceedWithPayment: ${e.message}", e)
            showError("An unexpected error occurred: ${e.message}")
        }
    }
    
    private fun dialUSSD() {
        val ussdCode = "*99*1*3#"
        val encodedHash = Uri.encode("#")
        val ussd = ussdCode.replace("#", encodedHash)
        
        Log.d("QRScanner", "Preparing to dial USSD: $ussdCode")
        
        // Check CALL_PHONE permission before dialing
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) 
            != PackageManager.PERMISSION_GRANTED) {
            Log.e("QRScanner", "CALL_PHONE permission not granted")
            showError("Phone call permission required to dial USSD. Please grant permission in settings.")
            return
        }
        
        val intent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:$ussd")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        
        try {
            Log.d("QRScanner", "Starting USSD call activity")
            updateBlackScreenStatus("USSD call initiated: $ussdCode")
            startActivity(intent)
            Log.d("QRScanner", "USSD call initiated successfully")
            
            // Start the extended message sequence for USSD process
            startUSSDProcessMessages()
            
        } catch (e: Exception) {
            Log.e("QRScanner", "Failed to dial USSD: ${e.message}", e)
            showError("Failed to initiate USSD call: ${e.message}")
        }
    }
    
    /**
     * Start extended message sequence during USSD process
     */
    private fun startUSSDProcessMessages() {
        Log.d("QRScanner", "Starting USSD process message sequence")
        isUSSDProcessActive = true
        
        val ussdMessages = listOf(
            "Connecting to USSD service...",
            "Almost there! Setting up payment...",
            "Great job! Processing transaction...",
            "Sending transaction details...",
            "Please wait while we process your payment...",
            "Transaction in progress...",
            "Almost done! Finalizing payment...",
            "Great work! Completing transaction...",
            "Payment processing... Please hold on...",
            "Final steps... Almost there!",
            "Transaction being processed...",
            "Great job! Payment almost complete...",
            "Finalizing your payment...",
            "USSD completed! Waiting for transaction confirmation...",
            "Please wait for SMS confirmation..."
        )
        
        val smsWaitingMessages = listOf(
            "Waiting for transaction confirmation...",
            "Please wait for SMS from your bank...",
            "Transaction being processed by bank...",
            "Almost there! Checking for confirmation...",
            "Great job! Payment is being verified...",
            "Please be patient, confirmation coming soon...",
            "Transaction in final stages...",
            "Bank is processing your payment...",
            "Confirmation SMS on the way...",
            "Almost done! Just a moment more...",
            "Great work! Payment being finalized...",
            "Bank is sending confirmation...",
            "Transaction almost complete...",
            "Please wait for SMS notification...",
            "Final verification in progress..."
        )
        
        var messageIndex = 0
        var isWaitingForSMS = false
        
        // Show messages every 2 seconds
        messageHandler = object : android.os.Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: android.os.Message) {
                if (isUSSDProcessActive) {
                    if (!isWaitingForSMS && messageIndex < ussdMessages.size) {
                        // Show USSD messages first
                        updateBlackScreenStatus(ussdMessages[messageIndex])
                        messageIndex++
                        
                        if (messageIndex >= ussdMessages.size) {
                            // Switch to SMS waiting messages
                            isWaitingForSMS = true
                            messageIndex = 0
                        }
                        sendEmptyMessageDelayed(0, 2000)
                    } else if (isWaitingForSMS) {
                        // Show SMS waiting messages
                        updateBlackScreenStatus(smsWaitingMessages[messageIndex % smsWaitingMessages.size])
                        messageIndex++
                        sendEmptyMessageDelayed(0, 3000) // Slower for SMS waiting
                    }
                }
            }
        }
        
        // Start the message sequence
        messageHandler?.sendEmptyMessageDelayed(0, 2000)
        
        // Start SMS timeout (150 seconds)
        startSMSTimeout()
    }
    
    /**
     * Start SMS timeout timer
     */
    private fun startSMSTimeout() {
        Log.d("QRScanner", "Starting SMS timeout timer (150 seconds)")
        
        smsTimeoutHandler = Handler(Looper.getMainLooper())
        smsTimeoutRunnable = Runnable {
            Log.d("QRScanner", "SMS timeout reached - returning to main screen")
            if (isUSSDProcessActive) {
                updateBlackScreenStatus("Transaction timeout. Returning to main screen...")
                Handler(Looper.getMainLooper()).postDelayed({
                    setResult(RESULT_CANCELLED)
                    finish()
                }, 2000)
            }
        }
        
        smsTimeoutHandler?.postDelayed(smsTimeoutRunnable!!, 150000) // 150 seconds
    }
    
    /**
     * Handle SMS received
     */
    private fun handleSMSReceived(intent: Intent) {
        Log.d("QRScanner", "SMS received - finishing QRScannerActivity immediately")
        
        // Cancel all timeouts and handlers
        smsTimeoutRunnable?.let {
            smsTimeoutHandler?.removeCallbacks(it)
        }
        messageHandler?.removeCallbacksAndMessages(null)
        
        // Dismiss USSD overlay
        // DISABLED: USSDOverlay functionality temporarily disabled
        /*
        try {
            USSDOverlayService.hideOverlay(this)
        } catch (e: Exception) {
            Log.e("QRScanner", "Failed to dismiss USSD overlay: ${e.message}")
        }
        */
        
        // Hide any black screen or status messages
        runOnUiThread {
            try {
                // Hide status text, instructions box and terminate button
                tvStatus.visibility = View.GONE
                instructionsBox.visibility = View.GONE
                btnTerminate.visibility = View.GONE
                topBar.visibility = View.VISIBLE  // Show top bar again
                
                // Reset instruction states for next time
                instructionsHeaderInitial.visibility = View.VISIBLE
                instructionsHeaderInitial.alpha = 1f
                instructionsExpanded.visibility = View.GONE
                instructionsExpanded.alpha = 1f
                
                // Reset background to normal
                findViewById<View>(android.R.id.content).setBackgroundColor(ContextCompat.getColor(this, R.color.background_secondary))
            } catch (e: Exception) {
                Log.e("QRScanner", "Error hiding black screen: ${e.message}")
            }
        }
        
        // Finish this activity immediately - SimpleSMSReceiver will handle showing success screen
        Log.d("QRScanner", "Finishing QRScannerActivity - SimpleSMSReceiver will show success screen")
        setResult(RESULT_SUCCESS)
        finish()
    }
    
    /**
     * Navigate to success screen
     */
    private fun navigateToSuccessScreen(transactionData: com.flowpay.app.models.TransactionData?) {
        try {
            Log.d("QRScanner", "Navigating to success screen")
            // Payment success handled via callback instead of separate activity
            Log.d("QRScanner", "Payment success - showing toast instead of separate activity")
            android.widget.Toast.makeText(this, "Payment successful!", android.widget.Toast.LENGTH_LONG).show()
            setResult(RESULT_SUCCESS)
            finish()
        } catch (e: Exception) {
            Log.e("QRScanner", "Failed to navigate to success screen: ${e.message}", e)
            // Fallback: just close the activity
            setResult(RESULT_ERROR)
            finish()
        }
    }
    
    /**
     * Terminate USSD process and return to main screen
     */
    private fun terminateUSSDProcess() {
        try {
            Log.d("QRScanner", "Terminating USSD process...")
            
            // Stop USSD process
            isUSSDProcessActive = false
            
            // Cancel all timeouts and handlers
            messageHandler?.removeCallbacksAndMessages(null)
            smsTimeoutRunnable?.let {
                smsTimeoutHandler?.removeCallbacks(it)
            }
            
            // Hide USSD overlay
            // DISABLED: USSDOverlay functionality temporarily disabled
            /*
            try {
                USSDOverlayService.hideOverlay(this)
                Log.d("QRScanner", "USSD overlay hidden")
            } catch (e: Exception) {
                Log.e("QRScanner", "Failed to hide USSD overlay: ${e.message}")
            }
            */
            
            // Stop SMS monitoring
            try {
                TransactionDetector.getInstance(this).stopOperation()
                Log.d("QRScanner", "SMS monitoring stopped")
            } catch (e: Exception) {
                Log.e("QRScanner", "Failed to stop SMS monitoring: ${e.message}")
            }
            
            // Show termination message briefly
            runOnUiThread {
                updateBlackScreenStatus("Process terminated. Returning to main screen...")
                btnTerminate.visibility = View.GONE
            }
            
            // Return to main screen after brief delay
            Handler(Looper.getMainLooper()).postDelayed({
                Log.d("QRScanner", "Returning to main screen after termination")
                setResult(RESULT_CANCELLED)
                finish()
            }, 1500)
            
        } catch (e: Exception) {
            Log.e("QRScanner", "Error terminating USSD process: ${e.message}", e)
            // Fallback: just close the activity
            setResult(RESULT_ERROR)
            finish()
        }
    }
    
    /**
     * Show black screen with status message
     */
    private fun showBlackScreenWithStatus(message: String) {
        runOnUiThread {
            Log.d("QRScanner", "Showing black screen with status: $message")
            
            // Hide camera and scanner elements
            viewFinder.visibility = View.GONE
            scannerOverlay.visibility = View.GONE
            progressBar.visibility = View.GONE
            topBar.visibility = View.GONE  // Hide top bar to prevent overlap
            
            // Show instructions box at the top with initial state
            instructionsBox.visibility = View.VISIBLE
            instructionsHeaderInitial.visibility = View.VISIBLE
            instructionsExpanded.visibility = View.GONE
            
            // Animate transition to expanded state after 4 seconds
            Handler(Looper.getMainLooper()).postDelayed({
                animateInstructionsTransition()
            }, 4000)
            
            // Show black screen with status
            tvStatus.text = message
            tvStatus.visibility = View.VISIBLE
            tvStatus.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            tvStatus.textSize = 18f
            
            // Show terminate button
            btnTerminate.visibility = View.VISIBLE
            
            // Set black background
            findViewById<View>(android.R.id.content).setBackgroundColor(ContextCompat.getColor(this, android.R.color.black))
        }
    }
    
    /**
     * Animate transition from initial header to expanded steps list
     */
    private fun animateInstructionsTransition() {
        runOnUiThread {
            Log.d("QRScanner", "Animating instructions transition to expanded state")
            
            // Fade out initial header
            instructionsHeaderInitial.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction {
                    instructionsHeaderInitial.visibility = View.GONE
                    
                    // Fade in expanded view
                    instructionsExpanded.alpha = 0f
                    instructionsExpanded.visibility = View.VISIBLE
                    instructionsExpanded.animate()
                        .alpha(1f)
                        .setDuration(400)
                        .start()
                }
                .start()
        }
    }
    
    /**
     * Update status message on black screen
     */
    private fun updateBlackScreenStatus(message: String) {
        runOnUiThread {
            Log.d("QRScanner", "Updating black screen status: $message")
            tvStatus.text = message
        }
    }
    
    private fun showError(message: String) {
        runOnUiThread {
            Log.e("QRScanner", "Showing error: $message")
            
            // Show error on black screen
            updateBlackScreenStatus("Error: $message")
            tvStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_light))
            
            // Show toast for additional feedback
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            
            // Resume scanning after 3 seconds with proper error recovery
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    Log.d("QRScanner", "Resuming QR scanning after error")
                    // Reset to camera view
                    viewFinder.visibility = View.VISIBLE
                    scannerOverlay.visibility = View.VISIBLE
                    topBar.visibility = View.VISIBLE  // Show top bar again
                    tvStatus.visibility = View.GONE
                    instructionsBox.visibility = View.GONE
                    btnTerminate.visibility = View.GONE
                    
                    // Reset instruction states
                    instructionsHeaderInitial.visibility = View.VISIBLE
                    instructionsHeaderInitial.alpha = 1f
                    instructionsExpanded.visibility = View.GONE
                    instructionsExpanded.alpha = 1f
                    
                    findViewById<View>(android.R.id.content).setBackgroundColor(ContextCompat.getColor(this, android.R.color.black))
                    
                    // FIX: Reset processing flag and clear analyzer before setting up new one
                    isProcessingQRCode = false
                    imageAnalyzer?.clearAnalyzer()
                    imageAnalyzer?.setAnalyzer(cameraExecutor, QRCodeAnalyzer { qrCode ->
                        processQRCode(qrCode)
                    })
                } catch (e: Exception) {
                    Log.e("QRScanner", "Failed to resume scanning: ${e.message}", e)
                    Toast.makeText(this, "Failed to resume scanning. Please restart the app.", Toast.LENGTH_LONG).show()
                }
            }, 3000)
        }
    }
    
    override fun onPause() {
        super.onPause()
        Log.d("QRScanner", "Activity paused - stopping camera and terminating USSD overlay")
        
        // Send app paused broadcast to USSD overlay service
        try {
            val intent = Intent("APP_PAUSED")
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
            Log.d("QRScanner", "App paused broadcast sent")
        } catch (e: Exception) {
            Log.e("QRScanner", "Error sending app paused broadcast: ${e.message}")
        }
        
        // Terminate USSD overlay when activity is paused
        // DISABLED: USSDOverlay functionality temporarily disabled
        /*
        try {
            USSDOverlayService.hideOverlay(this)
            Log.d("QRScanner", "USSD overlay terminated on pause")
        } catch (e: Exception) {
            Log.e("QRScanner", "Error terminating USSD overlay on pause: ${e.message}")
        }
        */
        
        try {
            imageAnalyzer?.clearAnalyzer()
            // FIX: Reset processing flag when paused
            isProcessingQRCode = false
        } catch (e: Exception) {
            Log.e("QRScanner", "Error stopping camera on pause: ${e.message}", e)
        }
    }
    
    override fun onStop() {
        super.onStop()
        Log.d("QRScanner", "Activity stopped - terminating USSD overlay")
        
        // Send app stopped broadcast to USSD overlay service
        try {
            val intent = Intent("APP_STOPPED")
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
            Log.d("QRScanner", "App stopped broadcast sent")
        } catch (e: Exception) {
            Log.e("QRScanner", "Error sending app stopped broadcast: ${e.message}")
        }
        
        // Terminate USSD overlay when activity is stopped
        // DISABLED: USSDOverlay functionality temporarily disabled
        /*
        try {
            USSDOverlayService.hideOverlay(this)
            Log.d("QRScanner", "USSD overlay terminated on stop")
        } catch (e: Exception) {
            Log.e("QRScanner", "Error terminating USSD overlay on stop: ${e.message}")
        }
        */
    }
    
    override fun onResume() {
        super.onResume()
        Log.d("QRScanner", "Activity resumed - restarting camera")
        try {
            if (imageAnalyzer != null && !isProcessingQRCode) {
                // FIX: Clear analyzer before setting up new one to prevent multiple analyzers
                imageAnalyzer?.clearAnalyzer()
                imageAnalyzer?.setAnalyzer(cameraExecutor, QRCodeAnalyzer { qrCode ->
                    processQRCode(qrCode)
                })
            }
        } catch (e: Exception) {
            Log.e("QRScanner", "Error restarting camera on resume: ${e.message}", e)
        }
    }
    
    override fun onDestroy() {
        try {
            Log.d("QRScanner", "Destroying QR scanner activity...")
            
            // Send app destroyed broadcast to USSD overlay service
            try {
                val intent = Intent("APP_DESTROYED")
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
                Log.d("QRScanner", "App destroyed broadcast sent")
            } catch (e: Exception) {
                Log.e("QRScanner", "Error sending app destroyed broadcast: ${e.message}")
            }
            
            // Terminate USSD overlay when activity is destroyed
            // DISABLED: USSDOverlay functionality temporarily disabled
            /*
            try {
                USSDOverlayService.hideOverlay(this)
                Log.d("QRScanner", "USSD overlay terminated on destroy")
            } catch (e: Exception) {
                Log.e("QRScanner", "Error terminating USSD overlay on destroy: ${e.message}")
            }
            */
            
            // Stop USSD process
            isUSSDProcessActive = false
            messageHandler?.removeCallbacksAndMessages(null)
            smsTimeoutRunnable?.let {
                smsTimeoutHandler?.removeCallbacks(it)
            }
            
            // FIX: Reset processing flag
            isProcessingQRCode = false
            
            // Unregister broadcast receivers
            try {
                LocalBroadcastManager.getInstance(this).unregisterReceiver(overlayDismissReceiver)
                LocalBroadcastManager.getInstance(this).unregisterReceiver(smsReceiver)
            } catch (e: Exception) {
                Log.e("QRScanner", "Error unregistering receivers: ${e.message}")
            }
            
            // Clear image analyzer first
            imageAnalyzer?.clearAnalyzer()
            imageAnalyzer = null
            
            // Shutdown camera executor safely
            if (::cameraExecutor.isInitialized && !cameraExecutor.isShutdown) {
                cameraExecutor.shutdown()
                Log.d("QRScanner", "Camera executor shutdown")
            }
            
            Log.d("QRScanner", "QR scanner activity destroyed successfully")
        } catch (e: Exception) {
            Log.e("QRScanner", "Error during activity destruction: ${e.message}", e)
        } finally {
            super.onDestroy()
        }
    }
}