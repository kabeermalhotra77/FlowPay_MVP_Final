// SettingsActivity.kt
package com.flowpay.app.ui.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FlowPaySettingsTheme {
                SettingsScreen(
                    onBackPressed = { finish() }
                )
            }
        }
    }
}

// Theme
@Composable
fun FlowPaySettingsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF667EEA),
            secondary = Color(0xFF764BA2),
            background = Color(0xFF0A0A0A),
            surface = Color(0xFF1A1A1A),
            onBackground = Color.White,
            onSurface = Color.White
        )
    ) {
        content()
    }
}

// Data Classes
data class Bank(
    val id: String,
    val name: String,
    val upiNumber: String,
    val ussdCode: String
)

data class SettingsState(
    val selectedBank: Bank = banks.first(),
    val upiServiceNumber: String = "08045163666",
    val ussdCode: String = "*99*1*3#",
    val ussdTimeout: Int = 30,
    val smsDetectionEnabled: Boolean = true,
    val overlayEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val debugMode: Boolean = false,
    val setupCompleted: Boolean = true,
    val permissions: Map<String, Boolean> = emptyMap()
)

// Bank Data
val banks = listOf(
    Bank("hdfc", "HDFC Bank", "08045163666", "*99*1*3#"),
    Bank("sbi", "State Bank of India", "09223766666", "*99*2*3#"),
    Bank("icici", "ICICI Bank", "09222208888", "*99*3*3#"),
    Bank("axis", "Axis Bank", "09225892258", "*99*4*3#"),
    Bank("kotak", "Kotak Mahindra Bank", "09227663676", "*99*5*3#"),
    Bank("pnb", "Punjab National Bank", "09223011311", "*99*6*3#"),
    Bank("bob", "Bank of Baroda", "09223268686", "*99*7*3#"),
    Bank("yes", "Yes Bank", "09223920000", "*99*8*3#"),
    Bank("idbi", "IDBI Bank", "09212993399", "*99*9*3#"),
    Bank("canara", "Canara Bank", "09015483333", "*99*10*3#")
)

// ViewModel
class SettingsViewModel : androidx.lifecycle.ViewModel() {
    var state by mutableStateOf(SettingsState())
        private set

    fun updateBank(bank: Bank) {
        state = state.copy(
            selectedBank = bank,
            upiServiceNumber = bank.upiNumber,
            ussdCode = bank.ussdCode
        )
    }

    fun updateUpiNumber(number: String) {
        state = state.copy(upiServiceNumber = number)
    }

    fun updateUssdCode(code: String) {
        state = state.copy(ussdCode = code)
    }

    fun toggleSmsDetection() {
        state = state.copy(smsDetectionEnabled = !state.smsDetectionEnabled)
    }

    fun toggleOverlay() {
        state = state.copy(overlayEnabled = !state.overlayEnabled)
    }

    fun toggleNotifications() {
        state = state.copy(notificationsEnabled = !state.notificationsEnabled)
    }

    fun toggleDebugMode() {
        state = state.copy(debugMode = !state.debugMode)
    }

    fun updatePermissions(permissions: Map<String, Boolean>) {
        state = state.copy(permissions = permissions)
    }
}

// Main Settings Screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showSaveIndicator by remember { mutableStateOf(false) }
    var expandedSection by remember { mutableStateOf<String?>(null) }

    // Permission checker
    LaunchedEffect(Unit) {
        val permissions = mapOf(
            Manifest.permission.CALL_PHONE to (ContextCompat.checkSelfPermission(
                context, Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED),
            Manifest.permission.READ_PHONE_STATE to (ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED),
            Manifest.permission.RECEIVE_SMS to (ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECEIVE_SMS
            ) == PackageManager.PERMISSION_GRANTED),
            Manifest.permission.READ_SMS to (ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_SMS
            ) == PackageManager.PERMISSION_GRANTED),
            Manifest.permission.CAMERA to (ContextCompat.checkSelfPermission(
                context, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED)
        )
        viewModel.updatePermissions(permissions)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                showSaveIndicator = true
                                delay(2000)
                                showSaveIndicator = false
                            }
                        }
                    ) {
                        Text(
                            "Save",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0A0A0A)
                )
            )

            // Settings Content
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Profile Card
                item {
                    ProfileCard()
                }

                // Bank Configuration Section
                item {
                    SettingsSection(
                        title = "BANK CONFIGURATION",
                        expanded = expandedSection == "bank",
                        onToggleExpand = {
                            expandedSection = if (expandedSection == "bank") null else "bank"
                        }
                    ) {
                        BankConfigurationContent(viewModel)
                    }
                }

                // USSD Configuration Section
                item {
                    SettingsSection(
                        title = "USSD CONFIGURATION",
                        expanded = expandedSection == "ussd",
                        onToggleExpand = {
                            expandedSection = if (expandedSection == "ussd") null else "ussd"
                        }
                    ) {
                        UssdConfigurationContent(viewModel)
                    }
                }

                // Permissions Management Section
                item {
                    SettingsSection(
                        title = "PERMISSIONS",
                        expanded = expandedSection == "permissions",
                        onToggleExpand = {
                            expandedSection = if (expandedSection == "permissions") null else "permissions"
                        }
                    ) {
                        PermissionsContent(viewModel)
                    }
                }

                // SMS Detection Section
                item {
                    SettingsSection(
                        title = "SMS DETECTION",
                        expanded = expandedSection == "sms",
                        onToggleExpand = {
                            expandedSection = if (expandedSection == "sms") null else "sms"
                        }
                    ) {
                        SmsDetectionContent(viewModel)
                    }
                }

                // Notifications Section
                item {
                    SettingsSection(
                        title = "NOTIFICATIONS",
                        expanded = expandedSection == "notifications",
                        onToggleExpand = {
                            expandedSection = if (expandedSection == "notifications") null else "notifications"
                        }
                    ) {
                        NotificationsContent(viewModel)
                    }
                }

                // Advanced Settings Section
                item {
                    SettingsSection(
                        title = "ADVANCED",
                        expanded = expandedSection == "advanced",
                        onToggleExpand = {
                            expandedSection = if (expandedSection == "advanced") null else "advanced"
                        }
                    ) {
                        AdvancedSettingsContent(viewModel)
                    }
                }

                // App Info Section
                item {
                    SettingsSection(
                        title = "APP INFORMATION",
                        expanded = expandedSection == "info",
                        onToggleExpand = {
                            expandedSection = if (expandedSection == "info") null else "info"
                        }
                    ) {
                        AppInfoContent()
                    }
                }

                // Danger Zone
                item {
                    DangerZone()
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }

        // Save Indicator
        AnimatedVisibility(
            visible = showSaveIndicator,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF667EEA)
                ),
                shape = RoundedCornerShape(50.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Settings Saved",
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// Profile Card Component
@Composable
fun ProfileCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A1A).copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFF2A2A2A))
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Background Gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF667EEA).copy(alpha = 0.1f),
                                Color(0xFF764BA2).copy(alpha = 0.1f)
                            )
                        )
                    )
            )

            // Content
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF667EEA), Color(0xFF764BA2))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "FP",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(20.dp))

                Column {
                    Text(
                        "FlowPay User",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "UPI ID: user@hdfc",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

// Settings Section Component
@Composable
fun SettingsSection(
    title: String,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            title,
            fontSize = 12.sp,
            color = Color.Gray,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A1A1A).copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF2A2A2A))
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleExpand() }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Tap to ${if (expanded) "collapse" else "expand"}",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                }
                
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        content()
                    }
                }
            }
        }
    }
}

// Bank Configuration Content
@Composable
fun BankConfigurationContent(viewModel: SettingsViewModel) {
    val state = viewModel.state
    var showBankDropdown by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Bank Selection
        SettingItem(
            icon = "🏦",
            title = "Bank Selection",
            subtitle = "Choose your primary bank",
            trailing = {
                Box {
                    TextButton(onClick = { showBankDropdown = true }) {
                        Text(state.selectedBank.name, color = Color.White)
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = Color.Gray
                        )
                    }
                    DropdownMenu(
                        expanded = showBankDropdown,
                        onDismissRequest = { showBankDropdown = false },
                        modifier = Modifier.background(Color(0xFF2A2A2A))
                    ) {
                        banks.forEach { bank ->
                            DropdownMenuItem(
                                text = { Text(bank.name, color = Color.White) },
                                onClick = {
                                    viewModel.updateBank(bank)
                                    showBankDropdown = false
                                }
                            )
                        }
                    }
                }
            }
        )

        // UPI Service Number
        SettingItem(
            icon = "📞",
            title = "UPI Service Number",
            subtitle = state.upiServiceNumber,
            trailing = {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
        )

        // Test Connection
        Button(
            onClick = { /* Test connection */ },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF667EEA).copy(alpha = 0.2f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Test Connection", color = Color(0xFF667EEA))
        }
    }
}

// USSD Configuration Content
@Composable
fun UssdConfigurationContent(viewModel: SettingsViewModel) {
    val state = viewModel.state

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SettingItem(
            icon = "📱",
            title = "USSD Code",
            subtitle = state.ussdCode
        )
        
        SettingItem(
            icon = "⏱️",
            title = "USSD Timeout",
            subtitle = "${state.ussdTimeout} seconds"
        )
        
        SettingItem(
            icon = "📊",
            title = "Step Delay",
            subtitle = "5 seconds"
        )
    }
}

// Permissions Content
@Composable
fun PermissionsContent(viewModel: SettingsViewModel) {
    val context = LocalContext.current
    val state = viewModel.state

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PermissionItem(
            title = "Phone Calls",
            description = "Required for UPI 123 payments",
            granted = state.permissions[Manifest.permission.CALL_PHONE] ?: false,
            onRequest = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }
        )
        
        PermissionItem(
            title = "SMS",
            description = "For payment confirmations",
            granted = state.permissions[Manifest.permission.RECEIVE_SMS] ?: false,
            onRequest = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }
        )
        
        PermissionItem(
            title = "Camera",
            description = "For QR code scanning",
            granted = state.permissions[Manifest.permission.CAMERA] ?: false,
            onRequest = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }
        )
    }
}

// SMS Detection Content
@Composable
fun SmsDetectionContent(viewModel: SettingsViewModel) {
    val state = viewModel.state

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SettingItem(
            icon = "📨",
            title = "SMS Detection",
            subtitle = "Auto-detect transaction SMS",
            trailing = {
                Switch(
                    checked = state.smsDetectionEnabled,
                    onCheckedChange = { viewModel.toggleSmsDetection() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF667EEA)
                    )
                )
            }
        )
        
        SettingItem(
            icon = "🔍",
            title = "Detection Keywords",
            subtitle = "debited, sent, transferred, success"
        )
        
        SettingItem(
            icon = "🏦",
            title = "Supported Banks",
            subtitle = "HDFC, ICICI, SBI, AXIS, and more..."
        )
    }
}

// Notifications Content
@Composable
fun NotificationsContent(viewModel: SettingsViewModel) {
    val state = viewModel.state

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SettingItem(
            icon = "🔔",
            title = "Notifications",
            subtitle = "Payment alerts and updates",
            trailing = {
                Switch(
                    checked = state.notificationsEnabled,
                    onCheckedChange = { viewModel.toggleNotifications() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF667EEA)
                    )
                )
            }
        )
        
        SettingItem(
            icon = "📳",
            title = "Vibration",
            subtitle = "Haptic feedback enabled"
        )
        
        SettingItem(
            icon = "🔊",
            title = "Sound",
            subtitle = "Payment success sound"
        )
    }
}

// Advanced Settings Content
@Composable
fun AdvancedSettingsContent(viewModel: SettingsViewModel) {
    val state = viewModel.state

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SettingItem(
            icon = "🎯",
            title = "Call Overlay",
            subtitle = "Show overlay during UPI calls",
            trailing = {
                Switch(
                    checked = state.overlayEnabled,
                    onCheckedChange = { viewModel.toggleOverlay() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF667EEA)
                    )
                )
            }
        )
        
        SettingItem(
            icon = "🔧",
            title = "Debug Mode",
            subtitle = "Enable developer options",
            trailing = {
                Switch(
                    checked = state.debugMode,
                    onCheckedChange = { viewModel.toggleDebugMode() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF667EEA)
                    )
                )
            }
        )
        
        SettingItem(
            icon = "📝",
            title = "Log Level",
            subtitle = if (state.debugMode) "DEBUG" else "ERROR"
        )
    }
}

// App Info Content
@Composable
fun AppInfoContent() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SettingItem(
            icon = "ℹ️",
            title = "Version",
            subtitle = "1.0.0 (Build 100)"
        )
        
        SettingItem(
            icon = "📱",
            title = "Android Version",
            subtitle = android.os.Build.VERSION.RELEASE
        )
        
        SettingItem(
            icon = "💾",
            title = "Storage Used",
            subtitle = "12.5 MB"
        )
    }
}

// Setting Item Component
@Composable
fun SettingItem(
    icon: String,
    title: String,
    subtitle: String,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF2A2A2A)),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 20.sp)
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            Text(
                subtitle,
                fontSize = 13.sp,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        trailing?.invoke()
    }
}

// Permission Item Component
@Composable
fun PermissionItem(
    title: String,
    description: String,
    granted: Boolean,
    onRequest: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            Text(
                description,
                fontSize = 13.sp,
                color = Color.Gray
            )
        }
        
        if (granted) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF43E97B).copy(alpha = 0.2f),
                border = BorderStroke(1.dp, Color(0xFF43E97B).copy(alpha = 0.5f))
            ) {
                Text(
                    text = "Granted", 
                    color = Color(0xFF43E97B), 
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        } else {
            Button(
                onClick = onRequest,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF5576C).copy(alpha = 0.2f)
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Grant", color = Color(0xFFF5576C), fontSize = 12.sp)
            }
        }
    }
}

// Danger Zone Component
@Composable
fun DangerZone() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5576C).copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFF5576C).copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("⚠️", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Danger Zone",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFF5576C)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { /* Clear data */ },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFF5576C)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFF5576C).copy(alpha = 0.3f))
                ) {
                    Text("Clear Data", fontSize = 14.sp)
                }
                
                OutlinedButton(
                    onClick = { /* Reset settings */ },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFF5576C)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFF5576C).copy(alpha = 0.3f))
                ) {
                    Text("Reset", fontSize = 14.sp)
                }
            }
        }
    }
}

// Add to MainActivity.kt to launch Settings
/*
// In your MainActivity or main screen, add this to launch settings:

private fun launchSettings() {
    val intent = Intent(this, SettingsActivity::class.java)
    startActivity(intent)
}

// Or in Compose:
@Composable
fun MainScreen() {
    // Your existing UI...
    IconButton(onClick = { 
        val context = LocalContext.current
        context.startActivity(Intent(context, SettingsActivity::class.java))
    }) {
        Icon(Icons.Default.Settings, contentDescription = "Settings")
    }
}
*/

