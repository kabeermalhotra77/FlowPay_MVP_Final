// MainActivity.kt - UI Only
// All business logic has been moved to MainActivityHelper

package com.flowpay.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PermContactCalendar
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.runtime.CompositionLocalProvider
import com.flowpay.app.data.SettingsRepository
import com.flowpay.app.ui.theme.BlueAccentTheme
import com.flowpay.app.ui.theme.LocalFlowPayAccentTheme
import com.flowpay.app.ui.theme.RedAccentTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.flowpay.app.ui.theme.FlowPayTheme
import com.flowpay.app.helpers.MainActivityHelper
import com.flowpay.app.helpers.SMSPermissionHelper
import com.flowpay.app.helpers.TransactionDetector
import com.flowpay.app.receivers.SimpleSMSReceiver
import com.flowpay.app.constants.AppConstants
import com.flowpay.app.constants.PermissionConstants
import com.flowpay.app.data.PaymentDetails
import com.flowpay.app.data.PaymentStatus
import com.flowpay.app.viewmodel.TransactionViewModel
import com.flowpay.app.ui.activities.TransactionHistoryActivity
import com.flowpay.app.ui.activities.SettingsActivity
import com.flowpay.app.ui.dialogs.Contact
import com.flowpay.app.ui.dialogs.ContactPickerDialog
import com.flowpay.app.managers.PermissionManager
import com.flowpay.app.glasses.GlassesSessionManager
import com.flowpay.app.glasses.GlassesSessionState
import com.flowpay.app.glasses.userMessageForPermissionFailure
import com.flowpay.app.utils.TestTransactionGenerator
import com.flowpay.app.utils.findComponentActivity
import com.meta.wearable.dat.core.Wearables
import com.meta.wearable.dat.core.types.Permission
import com.meta.wearable.dat.core.types.PermissionStatus
import androidx.lifecycle.viewmodel.compose.viewModel
import android.content.IntentFilter
import android.provider.Telephony
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "FlowPay"
        const val QR_SCAN_REQUEST_CODE = 1001
        @Volatile
        var showCallDurationDialogCallback: (() -> Unit)? = null
        @Volatile
        var showCallSuccessDialogCallback: (() -> Unit)? = null
        @Volatile
        var dismissCallSuccessDialogCallback: (() -> Unit)? = null
        @Volatile
        var resetQRScanningStateCallback: (() -> Unit)? = null
        @Volatile
        var showOverlayPermissionDialogCallback: (() -> Unit)? = null
    }

    // Helper for all business logic
    private lateinit var helper: MainActivityHelper
    
    // SMS Receiver
    private var smsReceiver: SimpleSMSReceiver? = null
    
    // QR Scanner permission launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        helper.handleQRPermissionResult(permissions)
    }

    private val requestGlassesCameraLauncher = registerForActivityResult(
        Wearables.RequestPermissionContract()
    ) { result ->
        result.getOrNull()?.let { status ->
            if (status == PermissionStatus.Granted) {
                Log.d(TAG, "Glasses camera permission granted")
                Toast.makeText(this, "Glasses camera allowed. Say flow pay or tap the card to retry.", Toast.LENGTH_SHORT).show()
                com.flowpay.app.services.GlassesSessionService.instance?.startStreamAfterPermissionGranted(this)
            } else {
                Log.d(TAG, "Glasses camera permission denied or not granted")
                Toast.makeText(this, "Camera not allowed. You can grant it later in Meta AI (Glasses → App permissions).", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            val message = userMessageForPermissionFailure(result.exceptionOrNull())
            Log.w(TAG, "Glasses camera permission request failed: $message")
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    fun requestGlassesCameraPermission() {
        requestGlassesCameraLauncher.launch(Permission.CAMERA)
    }
    
    // UI Callback implementations
    private fun showPinEntryAlert() {
        // This will be handled in the Compose UI
        Log.d("MainActivity", "Pin entry alert requested")
    }
    
    private fun showOverlay() {
        helper.showOverlay()
    }
    
    private fun hideOverlay() {
        helper.hideOverlay()
    }
    
    private fun registerSMSReceiver() {
        try {
            smsReceiver = SimpleSMSReceiver()
            val filter = IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)
            filter.priority = IntentFilter.SYSTEM_HIGH_PRIORITY
            registerReceiver(smsReceiver, filter)
            Log.d("MainActivity", "SMS Receiver registered")
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to register SMS receiver", e)
        }
    }
    
    // Call this when starting UPI 123 transfer
    fun startUPI123Transfer(phoneNumber: String, amount: String) {
        Log.d("MainActivity", "Starting UPI 123 transfer to $phoneNumber for ₹$amount")
        
        // Start monitoring for SMS
        TransactionDetector.getInstance(this).startOperation(
            operationType = "UPI_123",
            expectedAmount = amount,
            phoneNumber = phoneNumber
        )
        
        // Your existing UPI 123 call logic here
        // Example: CallManager.initiateUPI123Call(phoneNumber, amount)
    }
    
    // Call this when starting QR scan payment
    fun startQRPayment(amount: String? = null, upiId: String? = null) {
        Log.d("MainActivity", "Starting QR payment")
        
        // Start monitoring for SMS
        TransactionDetector.getInstance(this).startOperation(
            operationType = "QR_SCAN",
            expectedAmount = amount
        )
        
        // Your existing QR payment logic here
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // IMMEDIATE theme transition from splash to main theme
        val app = application as? FlowPayApplication
        val settingsRepository = app?.settingsRepository ?: SettingsRepository(applicationContext)
        val accentTheme = settingsRepository.settingsFlow.value.accentTheme
        setTheme(
            if (accentTheme == "red") R.style.Theme_FlowPay_Red
            else R.style.Theme_FlowPay
        )
        Log.d(TAG, "Theme set to ${if (accentTheme == "red") "Theme_FlowPay_Red" else "Theme_FlowPay"}")
        
        // IMMEDIATE black setup - set before ANYTHING else
        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK
        Log.d(TAG, "Initial status bar color set to: ${window.statusBarColor}")
        window.setBackgroundDrawableResource(android.R.color.black)
        window.decorView.setBackgroundColor(android.graphics.Color.BLACK)
        
        // Enable system bars but keep them black
        window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS,
            android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
        )
        
        // Set system bars to black with white text/icons
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = android.graphics.Color.BLACK
            window.navigationBarColor = android.graphics.Color.BLACK
        }
        
        // Use modern WindowInsetsController for better status bar control
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.setSystemBarsAppearance(0, android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS)
                controller.setSystemBarsAppearance(0, android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS)
            }
        } else {
            // Fallback for older Android versions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.decorView.systemUiVisibility = 
                    android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                // Remove light status bar flag to show white text on black background
                window.decorView.systemUiVisibility = window.decorView.systemUiVisibility and
                    android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            }
        }
        
        // Force status bar to be black using a different approach
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = android.graphics.Color.parseColor("#000000")
            window.navigationBarColor = android.graphics.Color.parseColor("#000000")
        }
        
        // Force edge-to-edge black display
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = 
                android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        
        // Additional enforcement for black status bar
        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK
        
        // Delayed enforcement to override any system changes
        window.decorView.post {
            enforceBlackStatusBar()
        }
        
        // Initialize helper with UI callbacks
        helper = MainActivityHelper(this, object : MainActivityHelper.UICallback {
            override fun showToast(message: String) {
                runOnUiThread { Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show() }
            }
            
            override fun showPinEntryAlert() {
                this@MainActivity.showPinEntryAlert()
            }
            
            override fun showOverlay() {
                this@MainActivity.showOverlay()
            }
            
            override fun hideOverlay() {
                this@MainActivity.hideOverlay()
            }
            
            override fun updatePaymentState(paymentState: com.flowpay.app.states.PaymentState) {
                // This will be handled in the Compose UI
                Log.d("MainActivity", "Payment state updated: ${paymentState::class.simpleName}")
            }
            
            override fun navigateToSetup() {
                val intent = Intent(this@MainActivity, SetupActivity::class.java)
                startActivity(intent)
                finish()
            }
            
            override fun navigateToTestConfiguration() {
                val intent = Intent(this@MainActivity, TestConfigurationActivity::class.java)
                startActivity(intent)
                finish()
            }
            
            override fun navigateToPaymentSuccess(paymentData: com.flowpay.app.data.PaymentSuccessData) {
                // Dismiss success dialog if it's showing
                runOnUiThread {
                    Log.d("MainActivity", "💰 Payment success detected - dismissing success dialog")
                    dismissCallSuccessDialogCallback?.invoke()
                }
                // Handle payment success navigation if needed
                showToast("Payment successful! Transaction ID: ${paymentData.transactionId}")
            }
            
            override fun finishActivity() {
                finish()
            }
            
            override fun registerBroadcastReceiver(receiver: android.content.BroadcastReceiver, filter: android.content.IntentFilter) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
                } else {
                    @Suppress("DEPRECATION")
                    registerReceiver(receiver, filter)
                }
            }
            
            override fun unregisterBroadcastReceiver(receiver: android.content.BroadcastReceiver) {
                try {
                    unregisterReceiver(receiver)
        } catch (e: Exception) {
                    Log.w(TAG, "Error unregistering receiver: ${e.message}")
                }
            }
            
            override fun showCallDurationIssueDialog() {
                runOnUiThread {
                    Log.d("MainActivity", "🚨 Requesting call duration dialog to be shown")
                    showCallDurationDialogCallback?.invoke()
                }
            }
            
            override fun showCallSuccessDialog() {
                runOnUiThread {
                    Log.d("MainActivity", "✅ Requesting call success dialog to be shown")
                    showCallSuccessDialogCallback?.invoke()
                }
            }

            override fun showOverlayPermissionExplanation() {
                runOnUiThread {
                    Log.d("MainActivity", "Requesting overlay permission explanation dialog")
                    showOverlayPermissionDialogCallback?.invoke()
                }
            }
        })
        
        // Initialize helper
        helper.initialize()
        // Register SMS receiver
        registerSMSReceiver()
        
        // Check if setup/test is completed
        if (!helper.isSetupCompleted()) {
            helper.navigateToSetup()
            return
        }
        
        if (!helper.isTestCompleted()) {
            helper.navigateToTestConfiguration()
            return
        }
        
        setContent {
            val accent = if (accentTheme == "red") RedAccentTheme else BlueAccentTheme
            CompositionLocalProvider(LocalFlowPayAccentTheme provides accent) {
                FlowPayTheme {
                    MainScreen(
                        onInitiateTransfer = { phoneNumber, amount ->
                            helper.initiateTransfer(phoneNumber, amount)
                        },
                        onQRScanClick = {
                            helper.startQRScanning()
                        }
                    )
                }
            }
        }
    }

    
    
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        // Handle QR scan result
        if (requestCode == QR_SCAN_REQUEST_CODE) {
            // Reset scanning state when QR scanner finishes (regardless of result)
            Log.d("FlowPay", "QR scan finished with result code: $resultCode")
            resetQRScanningStateCallback?.invoke()
        }
        
        helper.handleActivityResult(requestCode, resultCode, data)
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        // Handle all permissions through PermissionManager
        val success = helper.handlePermissionResult(requestCode, permissions, grantResults)
        
        // Show appropriate feedback based on permission type
        when (requestCode) {
            PermissionConstants.GLASSES_PERMISSION_REQUEST_CODE -> {
                if (success) {
                    // Permissions granted — check local flag to decide next step
                    val prefs = getSharedPreferences(AppConstants.PREFS_NAME, android.content.Context.MODE_PRIVATE)
                    val setupStarted = prefs.getBoolean(AppConstants.KEY_GLASSES_SETUP_STARTED, false)
                    if (setupStarted) {
                        GlassesSessionManager.startSession(this)
                    } else {
                        GlassesSessionManager.startRegistration(this)
                    }
                }
                // No toast either way — silent for glasses
            }
            PermissionConstants.SMS_PERMISSION_REQUEST_CODE -> {
                if (success) {
                    Toast.makeText(this, "SMS permissions granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(
                        this,
                        "SMS permission is required to detect payment confirmations",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            PermissionConstants.PERMISSIONS_REQUEST_CODE -> {
                if (success) {
                    Toast.makeText(this, "All permissions granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Some permissions were denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    
    // Lifecycle methods - delegate to helper
    override fun onPause() {
        super.onPause()
        com.flowpay.app.services.GlassesSessionService.instance?.setCurrentActivity(null)
        helper.onPause()
    }
    
    override fun onResume() {
        super.onResume()
        com.flowpay.app.services.GlassesSessionService.instance?.setCurrentActivity(this)
        helper.onResume()
        
        // Ensure status bar stays black
        enforceBlackStatusBar()
    }
    
    private fun enforceBlackStatusBar() {
        Log.d(TAG, "Enforcing black status bar - API Level: ${Build.VERSION.SDK_INT}")
        
        // Try multiple approaches to ensure black status bar
        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK
        
        // Also try with parsed color
        window.statusBarColor = android.graphics.Color.parseColor("#000000")
        window.navigationBarColor = android.graphics.Color.parseColor("#000000")
        
        Log.d(TAG, "Status bar color set to: ${window.statusBarColor}")
        Log.d(TAG, "Navigation bar color set to: ${window.navigationBarColor}")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Log.d(TAG, "Using WindowInsetsController for API 30+")
            window.insetsController?.let { controller ->
                controller.setSystemBarsAppearance(0, android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS)
                controller.setSystemBarsAppearance(0, android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS)
                Log.d(TAG, "WindowInsetsController appearance set to dark")
            } ?: Log.w(TAG, "WindowInsetsController is null")
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Log.d(TAG, "Using systemUiVisibility for API 23+")
            window.decorView.systemUiVisibility = 
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility and
                android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            Log.d(TAG, "systemUiVisibility set to: ${window.decorView.systemUiVisibility}")
        }
        
        // Force refresh the window
        window.decorView.invalidate()
    }
    
    override fun onStop() {
        super.onStop()
        helper.onStop()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        helper.onDestroy()
        
        // Unregister SMS receiver
        try {
            smsReceiver?.let {
                unregisterReceiver(it)
                Log.d("MainActivity", "SMS Receiver unregistered")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to unregister SMS receiver", e)
        }
    }
}

// Custom QR Frame Icon - Bracket-style scan frame
@Composable
fun QRFrameIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color.White
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 4.dp.toPx()
        val cornerLength = size.width * 0.25f
        
        // Top-left corner
        drawLine(
            color = tint,
            start = Offset(0f, cornerLength),
            end = Offset(0f, 0f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(0f, 0f),
            end = Offset(cornerLength, 0f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        
        // Top-right corner
        drawLine(
            color = tint,
            start = Offset(size.width - cornerLength, 0f),
            end = Offset(size.width, 0f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(size.width, 0f),
            end = Offset(size.width, cornerLength),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        
        // Bottom-left corner
        drawLine(
            color = tint,
            start = Offset(0f, size.height - cornerLength),
            end = Offset(0f, size.height),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(0f, size.height),
            end = Offset(cornerLength, size.height),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        
        // Bottom-right corner
        drawLine(
            color = tint,
            start = Offset(size.width - cornerLength, size.height),
            end = Offset(size.width, size.height),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(size.width, size.height - cornerLength),
            end = Offset(size.width, size.height),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

// Payment Action Buttons - Two vertical pill buttons side by side
@Composable
fun PaymentActionButtons(
    onQRScanClick: () -> Unit,
    onPayContactClick: () -> Unit,
    isScanning: Boolean,
    modifier: Modifier = Modifier
) {
    // Button press states for animations
    var isQRPressed by remember { mutableStateOf(false) }
    var isPayPressed by remember { mutableStateOf(false) }
    
    // Enhanced animations with better timing
    val qrButtonScale by animateFloatAsState(
        targetValue = if (isQRPressed) 0.94f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "QR Button Scale"
    )
    val payButtonScale by animateFloatAsState(
        targetValue = if (isPayPressed) 0.94f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "Pay Button Scale"
    )
    
    // Vertical layout with centered buttons and OR divider
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Scan QR Button - Circular with text below
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Circular QR Button
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = CircleShape,
                        ambientColor = LocalFlowPayAccentTheme.current.headerGradientStart.copy(alpha = 0.3f),
                        spotColor = LocalFlowPayAccentTheme.current.headerGradientEnd.copy(alpha = 0.4f)
                    )
                    .scale(qrButtonScale)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                LocalFlowPayAccentTheme.current.headerGradientStart, // Header - top
                                LocalFlowPayAccentTheme.current.headerGradientEnd  // Header - bottom
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(1f, 1f)
                        ),
                        shape = CircleShape
                    )
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.15f),
                        shape = CircleShape
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                if (!isScanning) {
                                    isQRPressed = true
                                    tryAwaitRelease()
                                    isQRPressed = false
                                }
                            },
                            onTap = {
                                if (!isScanning) {
                                    onQRScanClick()
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isScanning) Icons.Default.QrCode else Icons.Default.QrCodeScanner,
                    contentDescription = "Scan QR Code",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            // QR Button Text
            Text(
                text = if (isScanning) "Opening..." else "Scan QR Code",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                textAlign = TextAlign.Center,
                letterSpacing = 0.3.sp,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.6f),
                        offset = Offset(0f, 1f),
                        blurRadius = 3f
                    )
                )
            )
        }
        
        // OR Divider
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .width(50.dp)
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.3f))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .background(
                        Color.White.copy(alpha = 0.1f),
                        shape = CircleShape
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "OR",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.8f),
                    letterSpacing = 1.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .width(50.dp)
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.3f))
            )
        }
        
        // Pay Contact Button - Rounded with text below
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Rounded Pay Contact Button
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(20.dp),
                        ambientColor = LocalFlowPayAccentTheme.current.headerGradientStart.copy(alpha = 0.3f),
                        spotColor = LocalFlowPayAccentTheme.current.headerGradientEnd.copy(alpha = 0.4f)
                    )
                    .scale(payButtonScale)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                LocalFlowPayAccentTheme.current.headerGradientStart, // Header - top
                                LocalFlowPayAccentTheme.current.headerGradientEnd  // Header - bottom
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(1f, 1f)
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isPayPressed = true
                                tryAwaitRelease()
                                isPayPressed = false
                            },
                            onTap = {
                                onPayContactClick()
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Pay Contact",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            // Pay Contact Button Text
            Text(
                text = "Pay Contact",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                textAlign = TextAlign.Center,
                letterSpacing = 0.3.sp,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.6f),
                        offset = Offset(0f, 1f),
                        blurRadius = 3f
                    )
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onInitiateTransfer: (String, String) -> Unit,
    onQRScanClick: () -> Unit
) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)

    // Re-read bank from prefs every time the screen resumes (e.g. returning from Settings)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var savedBank by remember { mutableStateOf(sharedPreferences.getString(AppConstants.KEY_SELECTED_BANK, "hdfc") ?: "hdfc") }

    // Glasses setup flag — read directly from SharedPrefs (instant, no SDK calls)
    // Set to true the moment user taps "Connect" in the setup dialog
    var isGlassesSetupStarted by remember {
        mutableStateOf(sharedPreferences.getBoolean(AppConstants.KEY_GLASSES_SETUP_STARTED, false))
    }

    LaunchedEffect(lifecycle) {
        snapshotFlow { lifecycle.currentState }
            .collect { state ->
                if (state == Lifecycle.State.RESUMED) {
                    savedBank = sharedPreferences.getString(AppConstants.KEY_SELECTED_BANK, "hdfc") ?: "hdfc"
                    isGlassesSetupStarted = sharedPreferences.getBoolean(AppConstants.KEY_GLASSES_SETUP_STARTED, false)
                }
            }
    }
    
    // Initialize ViewModel
    val transactionViewModel: TransactionViewModel = viewModel()
    
    // Collect transaction data from ViewModel
    val recentPayments by transactionViewModel.recentTransactions.collectAsState()
    val isLoading by transactionViewModel.isLoading.collectAsState()
    val error by transactionViewModel.error.collectAsState()
    
    var showPayContact by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(false) }
    var showGlassesSetupDialog by remember { mutableStateOf(false) }
    var showCallDurationDialog by remember { mutableStateOf(false) }
    var showCallSuccessDialog by remember { mutableStateOf(false) }
    var hasProactivelyRequestedCamera by remember { mutableStateOf(false) }

    // Permission dialog states
    var showSmsPermissionDialog by remember { mutableStateOf(false) }
    var pendingSmsAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showOverlayPermissionDialog by remember { mutableStateOf(false) }
    val hostActivity = remember(context) { context.findComponentActivity() }
    val permissionManager = remember(hostActivity) {
        hostActivity?.let { PermissionManager(it) }
    }

    val glassesState by GlassesSessionManager.state.collectAsState()
    var previousGlassesState by remember { mutableStateOf(glassesState) }
    LaunchedEffect(Unit) {
        MainActivity.showCallDurationDialogCallback = {
            showCallDurationDialog = true
            Log.d("MainActivity", "✅ Call duration dialog state set to true")
        }
    }
    
    // Set up callback for call success dialog
    LaunchedEffect(Unit) {
        MainActivity.showCallSuccessDialogCallback = {
            showCallSuccessDialog = true
            Log.d("MainActivity", "✅ Call success dialog state set to true")
        }
        MainActivity.dismissCallSuccessDialogCallback = {
            showCallSuccessDialog = false
            Log.d("MainActivity", "❌ Call success dialog state set to false")
        }
    }
    
    // Set up callback for resetting QR scanning state
    LaunchedEffect(Unit) {
        MainActivity.resetQRScanningStateCallback = {
            isScanning = false
            Log.d("MainActivity", "✅ QR scanning state reset to false via callback")
        }
    }

    // Set up callback for overlay permission explanation dialog
    LaunchedEffect(Unit) {
        MainActivity.showOverlayPermissionDialogCallback = {
            showOverlayPermissionDialog = true
        }
    }
    
    // Reset scanning state when app becomes visible (QR scanner finished)
    LaunchedEffect(lifecycle) {
        snapshotFlow { lifecycle.currentState }
            .collect { state ->
                if (state == Lifecycle.State.RESUMED) {
                    Log.d("FlowPay", "App resumed, current isScanning state: $isScanning")
                    // Always reset scanning state when app resumes (aggressive reset)
                    isScanning = false
                    Log.d("FlowPay", "Force reset isScanning to false - app resumed")
                    // Refresh transaction list when app resumes
                    transactionViewModel.refresh()
                    Log.d("FlowPay", "Refreshed transaction list - app resumed")
                }
            }
    }
    
    // Timeout mechanism to reset scanning state after configured timeout
    LaunchedEffect(isScanning) {
        if (isScanning) {
            kotlinx.coroutines.delay(AppConstants.USSD_SESSION_TIMEOUT) // Configured timeout
            if (isScanning) {
                isScanning = false
                Log.d("FlowPay", "Reset isScanning to false - timeout reached")
            }
        }
    }
    
    // Additional safety reset - reset scanning state on every recomposition if it's been true for too long
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(3000) // Check every 3 seconds
            if (isScanning) {
                // If scanning has been true for more than 5 seconds, reset it
                kotlinx.coroutines.delay(2000) // Wait 2 more seconds (total 5 seconds)
                if (isScanning) {
                    isScanning = false
                    Log.d("FlowPay", "Reset isScanning to false - safety timeout reached")
                }
            }
        }
    }
    
    // Force reset scanning state every 10 seconds as ultimate safety
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(10000) // Every 10 seconds
            isScanning = false
            Log.d("FlowPay", "Force reset isScanning to false - ultimate safety")
        }
    }

    LaunchedEffect(glassesState) {
        if (glassesState is GlassesSessionState.Connected && !GlassesSessionManager.hasCameraPermission() && !hasProactivelyRequestedCamera) {
            hasProactivelyRequestedCamera = true
            (context as? MainActivity)?.requestGlassesCameraPermission()
        }
        if (glassesState is GlassesSessionState.Idle) {
            hasProactivelyRequestedCamera = false
        }
    }
    LaunchedEffect(glassesState) {
        when {
            glassesState is GlassesSessionState.Connected && previousGlassesState is GlassesSessionState.Connecting ->
                Toast.makeText(context, "Glasses connected.", Toast.LENGTH_SHORT).show()
            glassesState is GlassesSessionState.Scanning && (previousGlassesState is GlassesSessionState.Listening || previousGlassesState is GlassesSessionState.Connected) ->
                Toast.makeText(context, "Scanning for QR...", Toast.LENGTH_SHORT).show()
            glassesState is GlassesSessionState.PaymentReady && previousGlassesState is GlassesSessionState.Scanning ->
                Toast.makeText(context, "QR scanned!", Toast.LENGTH_SHORT).show()
            glassesState is GlassesSessionState.Idle && previousGlassesState.isActive() ->
                Toast.makeText(context, "Glasses disconnected.", Toast.LENGTH_SHORT).show()
            glassesState is GlassesSessionState.Error ->
                Toast.makeText(context, "Couldn't reach glasses. Make sure they're on and connected via Meta AI.", Toast.LENGTH_LONG).show()
        }
        previousGlassesState = glassesState
    }
    
    val selectedBankName = when (savedBank) {
        "sbi" -> "State Bank of India"
        "hdfc" -> "HDFC Bank"
        "icici" -> "ICICI Bank"
        "axis" -> "Axis Bank"
        "kotak" -> "Kotak Mahindra Bank"
        "pnb" -> "Punjab National Bank"
        "bob" -> "Bank of Baroda"
        "yes" -> "Yes Bank"
        "idbi" -> "IDBI Bank"
        "canara" -> "Canara Bank"
        else -> "HDFC Bank"
    }
    
    // Debug logging for bank name
    Log.d("FlowPay", "Selected bank: $savedBank -> $selectedBankName")
    
    // Theme colors - Updated for the blue header design
    val backgroundColor = Color(0xFF0A0A0A) // Keep your dark background
    val accent = LocalFlowPayAccentTheme.current
    val primaryColor = accent.primary
    val primaryForegroundColor = Color.White
    val cardColor = Color(0xFF1F1F1F) // For other cards in dark theme
    val borderColor = Color(0xFF404040)
    val mutedForegroundColor = Color(0xFF888888)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black  // Force black background to block any white
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 0.dp)
                .background(Color.Black) // Ensure very top area is black
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 420.dp) // Slightly wider for better proportions
                    .align(Alignment.Center)
                    .background(Color.Black) // Pure black to match status bar/navigation bar
                    .statusBarsPadding() // Add padding for status bar
                    .navigationBarsPadding() // Add padding for navigation bar
                    .verticalScroll(rememberScrollState()) // Enable scrolling
            ) {
                // Add spacing from status bar
                Spacer(modifier = Modifier.height(24.dp))
                
                // Header Card - Enhanced gradient design with better elevation
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .height(220.dp) // Slightly taller for better proportions
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(20.dp), // More rounded corners
                            ambientColor = Color.Black.copy(alpha = 0.15f),
                            spotColor = Color.Black.copy(alpha = 0.15f)
                        ),
                    shape = RoundedCornerShape(20.dp), // More rounded corners to match screenshot
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent // Transparent to show gradient
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // Custom shadow instead
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        LocalFlowPayAccentTheme.current.headerGradientStart, // Lighter (top)
                                        LocalFlowPayAccentTheme.current.headerGradientEnd  // Lighter (bottom)
                                    )
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                    ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp) // Increased padding for better breathing room
                    ) {
                        // Add top margin as per spec
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // Top Section with Title and Settings
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Flowpay",
                                    fontSize = 24.sp, // Larger, more prominent
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = 0.5.sp,
                                    style = androidx.compose.ui.text.TextStyle(
                                        shadow = Shadow(
                                            color = Color.Black.copy(alpha = 0.15f),
                                            offset = androidx.compose.ui.geometry.Offset(0f, 2f),
                                            blurRadius = 6f
                                        )
                                    )
                                )
                                Spacer(modifier = Modifier.height(6.dp)) // More spacing
                                Text(
                                    text = "Offline UPI Payments",
                                    fontSize = 15.sp, // Slightly larger
                                    fontWeight = FontWeight.Normal,
                                    color = Color.White.copy(alpha = 0.95f), // More visible
                                    letterSpacing = 0.2.sp,
                                    style = androidx.compose.ui.text.TextStyle(
                                        shadow = Shadow(
                                            color = Color.Black.copy(alpha = 0.1f),
                                            offset = androidx.compose.ui.geometry.Offset(0f, 1f),
                                            blurRadius = 3f
                                        )
                                    )
                                )
                            }
                            
                            // Glasses Button - premium look when on (shadow, gradient), connection Toasts handled in LaunchedEffect
                            Box(
                                modifier = Modifier
                                    .padding(top = 8.dp, end = 4.dp)
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .then(
                                        if (glassesState.isActive())
                                            Modifier.shadow(
                                                elevation = 6.dp,
                                                shape = CircleShape,
                                                ambientColor = accent.primary.copy(alpha = 0.35f),
                                                spotColor = accent.primaryDark.copy(alpha = 0.4f)
                                            )
                                        else Modifier
                                    )
                                    .then(
                                        if (glassesState.isActive())
                                            Modifier.background(
                                                brush = Brush.verticalGradient(
                                                    colors = listOf(accent.headerGradientEnd, accent.primary)
                                                ),
                                                shape = CircleShape
                                            )
                                        else Modifier.background(Color.White.copy(alpha = 0.22f), CircleShape)
                                    )
                                    .then(
                                        if (glassesState.isActive())
                                            Modifier.border(1.5.dp, accent.accentLight, CircleShape)
                                        else Modifier
                                    )
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ) {
                                        if (glassesState.isActive()) {
                                            GlassesSessionManager.stopSession(context)
                                        } else if (!isGlassesSetupStarted) {
                                            // First time — show setup dialog
                                            showGlassesSetupDialog = true
                                        } else {
                                            // Already set up — check BT before starting
                                            val btManager = context.getSystemService(android.content.Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
                                            val glassesNames = listOf("RBM", "Ray-Ban", "Ray Ban", "Meta", "Oakley", "Stories")
                                            val glassyPaired = try {
                                                btManager?.adapter?.bondedDevices?.any { d ->
                                                    glassesNames.any { (d.name ?: "").contains(it, ignoreCase = true) }
                                                } == true
                                            } catch (e: SecurityException) { false }
                                            if (!glassyPaired) {
                                                android.widget.Toast.makeText(
                                                    context,
                                                    "Connect your Meta glasses via Bluetooth first",
                                                    android.widget.Toast.LENGTH_LONG
                                                ).show()
                                            } else {
                                                val activity = context as? android.app.Activity ?: return@clickable
                                                val pm = PermissionManager(activity)
                                                if (!pm.checkGlassesPermissions()) {
                                                    pm.requestGlassesPermissions()
                                                } else {
                                                    GlassesSessionManager.startSession(context)
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_glasses),
                                    contentDescription = "Smart Glasses",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            // Settings Button - Clean modern style with spec dimensions
                            Box(
                                modifier = Modifier
                                    .padding(top = 8.dp, end = 8.dp)
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Color.White.copy(alpha = 0.22f)
                                    )
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ) { 
                                        val intent = Intent(context, SettingsActivity::class.java)
                                        context.startActivity(intent)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Settings,
                                    contentDescription = "Settings",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp)) // Increased spacing

                        // Bank Info Section - Integrated card with proper layout
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp) // Taller for proper spacing
                                .clip(RoundedCornerShape(16.dp)) // More rounded to match header
                                .background(
                                    LocalFlowPayAccentTheme.current.headerGradientEnd // Lighter blue matching the gradient
                                )
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) { 
                                    Log.d("FlowPay", "Bank card clicked")
                                }
                                .padding(horizontal = 18.dp, vertical = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left column - Text content with proper sizing
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "Connected Bank",
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontWeight = FontWeight.Normal,
                                        letterSpacing = 0.3.sp,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = selectedBankName,
                                        fontSize = 18.sp, // Large enough to be prominent
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        letterSpacing = 0.3.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = androidx.compose.ui.text.TextStyle(
                                            shadow = Shadow(
                                                color = Color.Black.copy(alpha = 0.15f),
                                                offset = androidx.compose.ui.geometry.Offset(0f, 1f),
                                                blurRadius = 4f
                                            )
                                        )
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                // Right side - Wallet Icon with clean background
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Color.White.copy(alpha = 0.22f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalanceWallet,
                                        contentDescription = "Wallet",
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                        }
                    }
                    }
                }

                // Payment Action Buttons
                Spacer(modifier = Modifier.height(24.dp))
                
                PaymentActionButtons(
                    onQRScanClick = {
                        Log.d("FlowPay", "QR scan button clicked")
                        if (permissionManager?.checkSMSPermissions() != true) {
                            pendingSmsAction = {
                                isScanning = true
                                onQRScanClick()
                            }
                            showSmsPermissionDialog = true
                        } else {
                            isScanning = true
                            onQRScanClick()
                        }
                    },
                    onPayContactClick = {
                        showPayContact = true
                    },
                    isScanning = isScanning
                )

                // Add spacing between payment actions and transactions
                Spacer(modifier = Modifier.height(20.dp))

                // Recent Transactions - Clean layout matching main screen UI
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 0.dp), // Match action buttons spacing
                    shape = RoundedCornerShape(20.dp), // Match button corner radius
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF0A0A0A) // Very slightly lighter than pure black for subtle distinction
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp) // Reduced padding for more compact layout
                    ) {
                        // Header - Blue gradient icon matching header card
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Icon blending with background
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            Color(0xFF0A0A0A), // Same as card background
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = "History",
                                        modifier = Modifier.size(20.dp),
                                        tint = LocalFlowPayAccentTheme.current.headerGradientStart // Header color
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Recent Payments",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Your latest transactions",
                                        fontSize = 12.sp,
                                        color = Color(0xFF888888)
                                    )
                                }
                            }
                            
                            // View All button - always visible
                            TextButton(
                                onClick = { 
                                    val intent = Intent(context, TransactionHistoryActivity::class.java)
                                    context.startActivity(intent)
                                }
                            ) {
                                Text(
                                    text = "View All",
                                    fontSize = 12.sp,
                                    color = LocalFlowPayAccentTheme.current.accent // Accent color
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Transaction List, Loading State, or Empty State
                        when {
                            isLoading -> {
                                // Loading State - Clean centered design
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(32.dp),
                                        color = LocalFlowPayAccentTheme.current.headerGradientStart, // Match header
                                        strokeWidth = 3.dp
                                    )
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Text(
                                        text = "Loading transactions...",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF888888)
                                    )
                                }
                            }
                            
                            error != null -> {
                                // Error State - Clean centered design
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .background(
                                                Color(0xFF000000),
                                                CircleShape
                                            )
                                            .border(
                                                width = 0.8.dp,
                                                color = LocalFlowPayAccentTheme.current.headerGradientStart.copy(alpha = 0.25f),
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = "Error",
                                            modifier = Modifier.size(32.dp),
                                            tint = Color(0xFFFF6B6B)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Text(
                                        text = "Failed to load transactions",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = error ?: "Unknown error",
                                        fontSize = 13.sp,
                                        color = Color(0xFF888888),
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    TextButton(
                                        onClick = { transactionViewModel.refresh() }
                                    ) {
                                        Text(
                                            text = "Retry",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = LocalFlowPayAccentTheme.current.headerGradientStart
                                        )
                                    }
                                }
                            }
                            
                            recentPayments.isEmpty() -> {
                                // Empty State - Clean centered design matching screenshot
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .background(
                                                Color(0xFF2A2A2A), // Dark grey background matching screenshot
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = "No transactions",
                                            modifier = Modifier.size(32.dp),
                                            tint = Color(0xFF888888) // Light grey icon matching screenshot
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Text(
                                        text = "No transactions yet",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Your payment history will appear here",
                                        fontSize = 13.sp,
                                        color = Color(0xFF888888),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            
                            else -> {
                                // Transaction Items - Scrollable list showing latest 2-3 transactions
                                LazyColumn(
                                    modifier = Modifier.height(280.dp), // Increased height for better proportions
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(recentPayments) { payment ->
                                        TransactionItem(payment = payment)
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom spacing for better scrolling
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Dialogs
            if (showPayContact) {
                PayContactDialog(
                    onDismiss = { showPayContact = false },
                    onConfirm = { phone, amt ->
                        if (permissionManager?.checkSMSPermissions() != true) {
                            pendingSmsAction = {
                                showPayContact = false
                                onInitiateTransfer(phone, amt)
                            }
                            showSmsPermissionDialog = true
                        } else {
                            showPayContact = false
                            onInitiateTransfer(phone, amt)
                        }
                    }
                )
            }

            if (showSettings) {
                SettingsDialog(
                    onDismiss = { showSettings = false },
                    onReconfigure = {
                        showSettings = false
                        val sharedPrefs = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
                        sharedPrefs.edit().putBoolean(AppConstants.KEY_SETUP_COMPLETED, false).apply()
                        val intent = Intent(context, SetupActivity::class.java)
                        context.startActivity(intent)
                        (context as? ComponentActivity)?.finish()
                    }
                )
            }

            if (showCallDurationDialog) {
                CallDurationIssueDialog(
                    onDismiss = { showCallDurationDialog = false }
                )
            }

            if (showCallSuccessDialog) {
                CallSuccessDialog(
                    onDismiss = { showCallSuccessDialog = false }
                )
            }

            if (showGlassesSetupDialog) {
                GlassesSetupDialog(
                    onConnect = {
                        showGlassesSetupDialog = false
                        // Mark setup as started so dialog never appears again
                        sharedPreferences.edit()
                            .putBoolean(AppConstants.KEY_GLASSES_SETUP_STARTED, true)
                            .apply()
                        isGlassesSetupStarted = true
                        val activity = context as? android.app.Activity ?: return@GlassesSetupDialog
                        val pm = PermissionManager(activity)
                        if (!pm.checkGlassesPermissions()) {
                            pm.requestGlassesPermissions()
                            return@GlassesSetupDialog
                        }
                        GlassesSessionManager.startRegistration(activity)
                    },
                    onDismiss = { showGlassesSetupDialog = false }
                )
            }

            if (showOverlayPermissionDialog) {
                PermissionExplanationDialog(
                    title = "Overlay Permission",
                    message = "Flowpay needs overlay permission to protect your screen during USSD payment calls. This ensures your transaction details stay visible while the call is in progress.",
                    confirmButtonText = "Grant",
                    onConfirm = {
                        showOverlayPermissionDialog = false
                        permissionManager?.requestOverlayPermission()
                    },
                    onDismiss = {
                        showOverlayPermissionDialog = false
                    }
                )
            }

            if (showSmsPermissionDialog) {
                PermissionExplanationDialog(
                    title = "SMS Permission",
                    message = "This permission is used solely to confirm whether your transaction was completed or failed. Flowpay cannot access, read, or store any of your other messages.",
                    confirmButtonText = "Allow SMS access",
                    onConfirm = {
                        showSmsPermissionDialog = false
                        permissionManager?.requestSMSPermissions()
                    },
                    onDismiss = {
                        showSmsPermissionDialog = false
                        pendingSmsAction = null
                    }
                )
            }
        }
    }
}

@Composable
fun TransactionItem(payment: PaymentDetails) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp), // Increased height for better text spacing
        shape = RoundedCornerShape(16.dp), // Match button corner radius style
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0A0A0A) // Very slightly lighter than pure black for subtle distinction
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 14.dp), // Optimized padding
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp), // Increased padding to prevent overlap
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = payment.recipientName ?: payment.phoneNumber,
                    fontSize = 16.sp, // Larger for better visibility
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp)) // Increased spacing between name and date
                Text(
                    text = formatDate(payment.timestamp),
                    fontSize = 14.sp, // Larger font for better readability
                    color = Color(0xFFAAAAAA), // Lighter color for better contrast
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.padding(start = 8.dp) // Add padding to prevent overlap
            ) {
                Text(
                    text = formatAmount(payment.amount),
                    fontSize = 16.sp, // Larger for better visibility
                    fontWeight = FontWeight.Bold,
                    color = LocalFlowPayAccentTheme.current.headerGradientStart, // Header color
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.ArrowOutward,
                    contentDescription = "Outgoing",
                    modifier = Modifier.size(18.dp), // Slightly larger icon
                    tint = LocalFlowPayAccentTheme.current.headerGradientStart // Header color
                )
            }
        }
    }
}

@Composable
fun PayContactDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    val context = LocalContext.current
    val hostActivity = remember(context) { context.findComponentActivity() }
    var phoneNumber by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedContactName by remember { mutableStateOf<String?>(null) }
    var showContactPicker by remember { mutableStateOf(false) }
    var showContactPermissionDialog by remember { mutableStateOf(false) }
    val permissionManager = remember(hostActivity) {
        hostActivity?.let { PermissionManager(it) }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1A),
        title = { 
            Text(
                text = "Pay Contact",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            ) 
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Show selected contact name if available
                selectedContactName?.let { name ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = LocalFlowPayAccentTheme.current.accent.copy(alpha = 0.15f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = LocalFlowPayAccentTheme.current.accent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Sending to: $name",
                                color = LocalFlowPayAccentTheme.current.accent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                // Phone number field with contact picker button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { 
                            if (it.all { char -> char.isDigit() } && it.length <= 10) {
                                phoneNumber = it
                                selectedContactName = null // Clear contact name if manually editing
                            }
                        },
                        label = { Text("Mobile Number", color = Color(0xFF8A8A8A)) },
                        placeholder = { Text("10 digits", color = Color(0xFF6A6A6A)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF4A4A4A),
                            unfocusedBorderColor = Color(0xFF3A3A3A),
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                    
                    // Contact picker button
                    IconButton(
                        onClick = {
                            val pm = permissionManager
                            if (pm == null) {
                                Toast.makeText(
                                    context.applicationContext,
                                    "Unable to open contacts from this screen.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@IconButton
                            }
                            if (pm.hasContactPermission()) {
                                showContactPicker = true
                            } else {
                                showContactPermissionDialog = true
                            }
                        },
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .size(48.dp)
                            .background(
                                color = LocalFlowPayAccentTheme.current.accent.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PermContactCalendar,
                            contentDescription = "Select Contact",
                            tint = LocalFlowPayAccentTheme.current.accent
                        )
                    }
                }
                
                OutlinedTextField(
                    value = amount,
                    onValueChange = { 
                        if (it.all { char -> char.isDigit() } && it.length <= 6) {
                            amount = it
                        }
                    },
                    label = { Text("Amount (₹)", color = Color(0xFF8A8A8A)) },
                    placeholder = { Text("Enter amount", color = Color(0xFF6A6A6A)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF4A4A4A),
                        unfocusedBorderColor = Color(0xFF3A3A3A),
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(phoneNumber, amount) },
                enabled = phoneNumber.length == 10 && amount.isNotEmpty() && amount != "0"
            ) {
                Text("Transfer", color = if (phoneNumber.length == 10 && amount.isNotEmpty()) LocalFlowPayAccentTheme.current.accent else Color(0xFF6A6A6A))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF8A8A8A))
            }
        }
    )
    
    // Show contact picker dialog
    if (showContactPicker) {
        ContactPickerDialog(
            onDismiss = { showContactPicker = false },
            onContactSelected = { contact ->
                phoneNumber = contact.phoneNumber
                selectedContactName = contact.name
                showContactPicker = false
            }
        )
    }

    // Show contact permission explanation dialog
    if (showContactPermissionDialog) {
        PermissionExplanationDialog(
            title = "Contacts Permission",
            message = "Flowpay needs access to your contacts so you can quickly select a recipient by name instead of typing their number manually.",
            confirmButtonText = "Grant",
            onConfirm = {
                showContactPermissionDialog = false
                permissionManager?.requestContactPermission()
            },
            onDismiss = {
                showContactPermissionDialog = false
            }
        )
    }
}

@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    onReconfigure: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1A),
        title = { 
            Text(
                text = "Settings",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            ) 
        },
        text = { 
            Text(
                text = "Do you want to reconfigure your settings?",
                fontSize = 16.sp,
                color = Color(0xFFCCCCCC)
            ) 
        },
        confirmButton = {
            TextButton(
                onClick = onReconfigure,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = LocalFlowPayAccentTheme.current.accent
                )
            ) {
                Text("Reconfigure")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF8A8A8A))
            }
        }
    )
}

@Composable
fun GlassesSetupDialog(
    onConnect: () -> Unit,
    onDismiss: () -> Unit
) {
    val accent = LocalFlowPayAccentTheme.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1A),
        titleContentColor = Color.White,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(accent.accent.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(R.drawable.ic_glasses),
                        contentDescription = null,
                        tint = accent.accent,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = "Set Up Meta Glasses",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Use your Ray-Ban Meta glasses to scan UPI QR codes hands-free by saying \"Flowpay\".",
                    fontSize = 14.sp,
                    color = Color(0xFFCCCCCC),
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                val steps = listOf(
                    "Pair your Ray-Ban Meta glasses via Bluetooth in your phone's Settings.",
                    "Install the Meta AI app from the Play Store if you haven't already.",
                    "Open Meta AI and connect your glasses from within the app.",
                    "Tap Connect — Flowpay will register with your glasses via Meta.",
                    "Say \"Flowpay\" while wearing the glasses to scan QR codes hands-free."
                )
                steps.forEachIndexed { index, step ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(20.dp)
                                .background(accent.accent.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = accent.accent
                            )
                        }
                        Text(
                            text = step,
                            fontSize = 13.sp,
                            color = Color(0xFFCCCCCC),
                            lineHeight = 19.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConnect,
                colors = ButtonDefaults.textButtonColors(contentColor = accent.accent)
            ) {
                Text("Connect", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Not now", color = Color(0xFF8A8A8A))
            }
        }
    )
}

@Composable
fun PermissionExplanationDialog(
    title: String,
    message: String,
    confirmButtonText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1A),
        title = {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        },
        text = {
            Text(
                text = message,
                fontSize = 14.sp,
                color = Color(0xFFCCCCCC),
                lineHeight = 20.sp
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = LocalFlowPayAccentTheme.current.accent
                )
            ) {
                Text(confirmButtonText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Not now", color = Color(0xFF8A8A8A))
            }
        }
    )
}

// Utility functions
fun formatAmount(amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    return formatter.format(amount)
}

fun formatDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("dd MMM, HH:mm", Locale("en", "IN"))
    return formatter.format(Date(timestamp))
}

fun getStatusColor(status: PaymentStatus): Color {
    return when (status) {
        PaymentStatus.COMPLETED -> Color(0xFF1F1F1F)
        PaymentStatus.PENDING -> Color(0xFF1F1F1F)
        PaymentStatus.FAILED -> Color(0xFF3D3D3D)
        PaymentStatus.CANCELLED -> Color(0xFF3D3D3D)
    }
}

@Composable
fun CallDurationIssueDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1A),
        title = { 
            Text(
                text = "Payment Issue",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            ) 
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Transaction failed - call ended in less than 25 seconds.",
                    color = Color(0xFFCCCCCC),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "This usually means:",
                    color = Color(0xFFCCCCCC),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Text(
                    text = "• Daily payment limit reached\n• UPI not configured",
                    color = Color(0xFFAAAAAA),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Text(
                    text = "Try again tomorrow.",
                    color = LocalFlowPayAccentTheme.current.accentLight,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = LocalFlowPayAccentTheme.current.accentLight
                )
            ) {
                Text(
                    text = "OK",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    )
}

@Composable
fun CallSuccessDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1A),
        title = { 
            Text(
                text = "Great!",
                color = LocalFlowPayAccentTheme.current.accentLight,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            ) 
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Your request has been sent to your respective bank account.",
                    color = Color(0xFFCCCCCC),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "You shall shortly receive a call from them to confirm the transaction requested.",
                    color = Color(0xFFCCCCCC),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Text(
                    text = "When the call comes:",
                    color = Color(0xFFCCCCCC),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "1. Open your dialer\n2. Press 1 to confirm the request\n3. Enter your PIN",
                    color = Color(0xFFAAAAAA),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = LocalFlowPayAccentTheme.current.accentLight
                )
            ) {
                Text(
                    text = "Got it",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    )
}
