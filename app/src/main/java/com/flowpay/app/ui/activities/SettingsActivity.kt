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
import androidx.compose.ui.draw.shadow
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

// Theme - Updated to match main screen
@Composable
fun FlowPaySettingsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF7BA8F5), // Main screen blue
            secondary = Color(0xFF6A96EE), // Main screen blue variant
            background = Color(0xFF000000), // Pure black like main screen
            surface = Color(0xFF0A0A0A), // Card background matching main screen
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

// Main Settings Screen - Redesigned to match main screen UI
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showSaveIndicator by remember { mutableStateOf(false) }

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

    // Main screen colors and theme
    val backgroundColor = Color(0xFF000000) // Pure black like main screen
    val primaryColor = Color(0xFF7BA8F5) // Main blue color from main screen
    val cardColor = Color(0xFF0A0A0A) // Card background matching main screen
    val borderColor = Color(0xFF333333) // Subtle borders

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = backgroundColor
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 420.dp) // Match main screen width constraint
                    .align(Alignment.Center)
                    .background(backgroundColor)
            ) {
                // Add spacing from status bar
                Spacer(modifier = Modifier.height(24.dp))
                
                // Header Card - Matching main screen gradient design
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .height(180.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(20.dp),
                            ambientColor = Color.Black.copy(alpha = 0.15f),
                            spotColor = Color.Black.copy(alpha = 0.15f)
                        ),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF7BA8F5), // Lighter, softer blue (top)
                                        Color(0xFF6A96EE)  // Lighter blue with slight depth (bottom)
                                    )
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            // Top Section with Title and Back Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = onBackPressed,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            Color.White.copy(alpha = 0.2f),
                                            CircleShape
                                        )
                                ) {
                                    Icon(
                                        Icons.Default.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                
                                TextButton(
                                    onClick = {
                                        scope.launch {
                                            showSaveIndicator = true
                                            delay(2000)
                                            showSaveIndicator = false
                                        }
                                    },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text(
                                        "Save",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Title and Subtitle
                            Text(
                                "Settings",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Configure your FlowPay experience",
                                fontSize = 16.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }

                // Settings Content - Modern card-based layout
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Profile Card
                    item {
                        ModernProfileCard()
                    }

                    // Bank Configuration Card
                    item {
                        ModernSettingsCard(
                            title = "Bank Configuration",
                            icon = "🏦",
                            content = { BankConfigurationContent(viewModel) }
                        )
                    }

                    // USSD Configuration Card
                    item {
                        ModernSettingsCard(
                            title = "USSD Configuration",
                            icon = "📱",
                            content = { UssdConfigurationContent(viewModel) }
                        )
                    }

                    // Permissions Card
                    item {
                        ModernSettingsCard(
                            title = "Permissions",
                            icon = "🔐",
                            content = { PermissionsContent(viewModel) }
                        )
                    }

                    // SMS Detection Card
                    item {
                        ModernSettingsCard(
                            title = "SMS Detection",
                            icon = "📨",
                            content = { SmsDetectionContent(viewModel) }
                        )
                    }

                    // Notifications Card
                    item {
                        ModernSettingsCard(
                            title = "Notifications",
                            icon = "🔔",
                            content = { NotificationsContent(viewModel) }
                        )
                    }

                    // Advanced Settings Card
                    item {
                        ModernSettingsCard(
                            title = "Advanced",
                            icon = "⚙️",
                            content = { AdvancedSettingsContent(viewModel) }
                        )
                    }

                    // App Info Card
                    item {
                        ModernSettingsCard(
                            title = "App Information",
                            icon = "ℹ️",
                            content = { AppInfoContent() }
                        )
                    }

                    // Danger Zone
                    item {
                        ModernDangerZone()
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }

            // Save Indicator - Enhanced design
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
                        containerColor = primaryColor
                    ),
                    shape = RoundedCornerShape(25.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Settings Saved Successfully",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

// Modern Profile Card - Matching main screen design
@Composable
fun ModernProfileCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color.Black.copy(alpha = 0.15f),
                spotColor = Color.Black.copy(alpha = 0.15f)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0A0A0A) // Match main screen card color
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Avatar with gradient background
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF7BA8F5), // Main screen blue
                                Color(0xFF6A96EE)  // Main screen blue variant
                            )
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
                    color = Color(0xFF888888) // Consistent with main screen
                )
            }
        }
    }
}

// Modern Settings Card - Clean, non-expandable design
@Composable
fun ModernSettingsCard(
    title: String,
    icon: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color.Black.copy(alpha = 0.15f),
                spotColor = Color.Black.copy(alpha = 0.15f)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0A0A0A) // Match main screen card color
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Card Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF7BA8F5).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(icon, fontSize = 20.sp)
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Text(
                    title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Card Content
            content()
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

// Modern Setting Item Component - Enhanced design
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
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF7BA8F5).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 20.sp)
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                subtitle,
                fontSize = 14.sp,
                color = Color(0xFF888888),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        trailing?.invoke()
    }
}

// Modern Permission Item Component
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
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                description,
                fontSize = 14.sp,
                color = Color(0xFF888888)
            )
        }
        
        if (granted) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF4CAF50).copy(alpha = 0.2f),
                border = BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Granted", 
                        color = Color(0xFF4CAF50), 
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        } else {
            Button(
                onClick = onRequest,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF7BA8F5).copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    "Grant", 
                    color = Color(0xFF7BA8F5), 
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// Modern Danger Zone Component
@Composable
fun ModernDangerZone() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color.Black.copy(alpha = 0.15f),
                spotColor = Color.Black.copy(alpha = 0.15f)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0A0A0A) // Match main screen card color
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header with warning icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFF5722).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("⚠️", fontSize = 20.sp)
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Text(
                    "Danger Zone",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "These actions cannot be undone. Proceed with caution.",
                fontSize = 14.sp,
                color = Color(0xFF888888),
                lineHeight = 20.sp
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { /* Clear data */ },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFFF5722)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFFF5722).copy(alpha = 0.5f))
                ) {
                    Text(
                        "Clear Data", 
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                OutlinedButton(
                    onClick = { /* Reset settings */ },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFFF5722)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFFF5722).copy(alpha = 0.5f))
                ) {
                    Text(
                        "Reset All", 
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
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

