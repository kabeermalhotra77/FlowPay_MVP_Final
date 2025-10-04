# Settings Integration Guide

This guide shows how to integrate the new Settings system into your FlowPay app.

## 🎯 Overview

The Settings system includes:
- **SettingsActivity**: Modern UI for app configuration
- **SettingsRepository**: Persistent storage for settings
- **PermissionManager**: Centralized permission handling
- **SmsPatternManager**: SMS parsing and pattern matching

## 📁 Files Created

### Core Settings Files
- `SettingsActivity.kt` - Main settings UI
- `SettingsRepository.kt` - Settings persistence
- `PermissionManager.kt` - Permission management
- `SmsPatternManager.kt` - SMS pattern parsing

### Integration Updates
- `MainActivity.kt` - Added settings launch
- `AndroidManifest.xml` - Added SettingsActivity
- `build.gradle` - Added preferences dependency

## 🚀 Usage Examples

### 1. Basic Settings Usage

```kotlin
class PaymentActivity : ComponentActivity() {
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var permissionManager: PermissionManager
    private lateinit var smsPatternManager: SmsPatternManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize managers
        settingsRepository = SettingsRepository(this)
        permissionManager = PermissionManager(this)
        smsPatternManager = SmsPatternManager()
        
        // Observe settings changes
        lifecycleScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                updateUpiServiceNumber(settings.upiServiceNumber)
                toggleSmsDetection(settings.smsDetectionEnabled)
                setDebugMode(settings.debugMode)
            }
        }
    }
    
    private fun makeUpiCall() {
        val settings = settingsRepository.settingsFlow.value
        val phoneNumber = settings.upiServiceNumber
        
        val intent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:$phoneNumber")
        }
        startActivity(intent)
    }
}
```

### 2. Permission Management

```kotlin
class MainActivity : ComponentActivity() {
    private lateinit var permissionManager: PermissionManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        permissionManager = PermissionManager(this)
        
        // Check permissions
        val missingPermissions = permissionManager.getMissingPermissions()
        if (missingPermissions.isNotEmpty()) {
            permissionManager.requestPermissions(this, missingPermissions.toTypedArray())
        }
        
        // Check specific permission
        if (!permissionManager.checkPermission(Manifest.permission.CALL_PHONE)) {
            // Handle missing permission
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == PermissionManager.PERMISSION_REQUEST_CODE) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (allGranted) {
                // All permissions granted
            } else {
                // Some permissions denied
                permissionManager.openAppSettings()
            }
        }
    }
}
```

### 3. SMS Pattern Detection

```kotlin
class SmsReceiver : BroadcastReceiver() {
    private val smsPatternManager = SmsPatternManager()
    
    override fun onReceive(context: Context, intent: Intent) {
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        
        for (message in messages) {
            val details = smsPatternManager.parseSms(message.messageBody)
            
            if (details.isDebit && details.isSuccess) {
                // Valid payment SMS detected
                handlePaymentSms(details)
            }
        }
    }
    
    private fun handlePaymentSms(details: SmsPatternManager.TransactionDetails) {
        Log.d("SMS", "Amount: ${details.amount}")
        Log.d("SMS", "UPI ID: ${details.upiId}")
        Log.d("SMS", "Bank: ${details.bank}")
    }
}
```

### 4. Settings Integration in Compose

```kotlin
@Composable
fun PaymentScreen() {
    val context = LocalContext.current
    val settingsRepository = remember { SettingsRepository(context) }
    val settings by settingsRepository.settingsFlow.collectAsState()
    
    // Use settings in your UI
    LaunchedEffect(settings) {
        // React to settings changes
        if (settings.debugMode) {
            Log.d("Debug", "Debug mode enabled")
        }
    }
    
    // Your UI components here
    Button(
        onClick = {
            // Use settings for UPI call
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:${settings.upiServiceNumber}")
            }
            context.startActivity(intent)
        }
    ) {
        Text("Make UPI Call")
    }
}
```

## 🔧 Configuration

### Bank Configuration
The settings support 10 major Indian banks with pre-configured UPI numbers and USSD codes:

```kotlin
val banks = listOf(
    Bank("hdfc", "HDFC Bank", "08045163666", "*99*1*3#"),
    Bank("sbi", "State Bank of India", "09223766666", "*99*2*3#"),
    Bank("icici", "ICICI Bank", "09222208888", "*99*3*3#"),
    // ... more banks
)
```

### SMS Pattern Configuration
The SMS pattern manager supports detection of:
- Debit keywords: "debited", "sent", "transferred", "paid", "withdrawn"
- Success keywords: "success", "successful", "completed", "done"
- Amount patterns: "Rs. 100", "INR 100", "₹100"
- UPI ID patterns: "user@bank"
- Phone number patterns: 10-digit numbers

## 📱 Settings UI Features

### Collapsible Sections
- Bank Configuration
- USSD Configuration
- Permissions Management
- SMS Detection
- Notifications
- Advanced Settings
- App Information

### Interactive Elements
- Bank selection dropdown
- Toggle switches for features
- Permission status indicators
- Save confirmation feedback

### Dark Theme
- Consistent with FlowPay design
- Purple/blue gradient accents
- High contrast for readability

## 🔐 Permissions Required

The app requires these permissions (already declared in AndroidManifest.xml):
- `CALL_PHONE` - Make UPI 123 payment calls
- `READ_PHONE_STATE` - Monitor call states
- `RECEIVE_SMS` - Receive payment confirmations
- `READ_SMS` - Read payment-related messages
- `CAMERA` - Scan QR codes
- `VIBRATE` - Haptic feedback

## 🎨 Customization

### Adding New Banks
```kotlin
val newBank = Bank("newbank", "New Bank", "1234567890", "*99*11*3#")
val updatedBanks = banks + newBank
```

### Custom SMS Patterns
```kotlin
class CustomSmsPatternManager : SmsPatternManager() {
    private val customPattern = Pattern.compile("your custom pattern")
    
    override fun parseSms(message: String): TransactionDetails {
        val baseDetails = super.parseSms(message)
        // Add custom logic
        return baseDetails
    }
}
```

### Settings Persistence
```kotlin
// Save settings
val newSettings = settingsRepository.settingsFlow.value.copy(
    debugMode = true,
    smsDetectionEnabled = false
)
settingsRepository.saveSettings(newSettings)

// Clear all data
settingsRepository.clearAllData()

// Reset to defaults
settingsRepository.resetToDefaults()
```

## 🚀 Launch Settings

The settings can be launched from anywhere in your app:

```kotlin
// From Activity
val intent = Intent(this, SettingsActivity::class.java)
startActivity(intent)

// From Compose
val context = LocalContext.current
IconButton(
    onClick = {
        val intent = Intent(context, SettingsActivity::class.java)
        context.startActivity(intent)
    }
) {
    Icon(Icons.Default.Settings, contentDescription = "Settings")
}
```

## ✅ Testing

### Test Settings Persistence
```kotlin
@Test
fun testSettingsPersistence() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val repository = SettingsRepository(context)
    
    val testSettings = SettingsRepository.SavedSettings(
        bankId = "test",
        debugMode = true
    )
    
    repository.saveSettings(testSettings)
    val loadedSettings = repository.settingsFlow.value
    
    assertEquals("test", loadedSettings.bankId)
    assertTrue(loadedSettings.debugMode)
}
```

### Test SMS Patterns
```kotlin
@Test
fun testSmsPatterns() {
    val manager = SmsPatternManager()
    
    val testMessage = "Rs. 100 debited from your account. UPI ID: user@hdfc"
    val details = manager.parseSms(testMessage)
    
    assertTrue(details.isDebit)
    assertEquals("100", details.amount)
    assertEquals("user@hdfc", details.upiId)
}
```

## 🎯 Next Steps

1. **Test the Settings UI** - Launch the app and tap the settings icon
2. **Configure your bank** - Select your bank and verify UPI number
3. **Test permissions** - Grant/deny permissions and see the UI update
4. **Customize patterns** - Modify SMS patterns for your specific use case
5. **Integrate with existing code** - Use SettingsRepository in your payment flows

The Settings system is now fully integrated and ready to use! 🚀
