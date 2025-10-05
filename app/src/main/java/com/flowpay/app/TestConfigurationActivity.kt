package com.flowpay.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flowpay.app.ui.theme.FlowPayTheme
import com.flowpay.app.helpers.TestConfigurationHelper
import com.flowpay.app.managers.CallType
import com.flowpay.app.ui.dialogs.UssdProgressDialog
// import com.flowpay.app.ui.dialogs.Upi123ProgressDialog // Replaced with custom Compose dialog
import kotlinx.coroutines.delay
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

class TestConfigurationActivity : ComponentActivity() {
    private lateinit var testHelper: TestConfigurationHelper
    private val showUpi123CompletionDialog = mutableStateOf(false)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize test helper
        testHelper = TestConfigurationHelper(this, object : TestConfigurationHelper.UICallback {
            override fun showToast(message: String) {
                runOnUiThread { android.widget.Toast.makeText(this@TestConfigurationActivity, message, android.widget.Toast.LENGTH_LONG).show() }
            }
            
            override fun updateUssdTesting(isTesting: Boolean) {
                // State will be managed by the composable
            }
            
            override fun updateUssdDialog(show: Boolean) {
                // State will be managed by the composable
            }
            
            override fun updateUssdTestCompleted(completed: Boolean) {
                // State will be managed by the composable
            }
            
            override fun updateUpi123Testing(isTesting: Boolean) {
                // State will be managed by the composable
            }
            
            override fun updateUpi123TestCompleted(completed: Boolean) {
                // State will be managed by the composable
            }
            
            override fun updateUpi123Dialog(show: Boolean) {
                // State will be managed by the composable
            }
            
            override fun updateUpi123ConfigurationOptions(show: Boolean) {
                // State will be managed by the composable
            }
            
            override fun updateVoiceTesting(isTesting: Boolean) {
                // State will be managed by the composable
            }
            
            override fun updateVoiceDialog(show: Boolean) {
                // State will be managed by the composable
            }
            
            override fun updateVoiceTestCompleted(completed: Boolean) {
                // State will be managed by the composable
            }
            
            override fun updateCallCompleteButton(show: Boolean) {
                // State will be managed by the composable
            }
            
            override fun updateUssdProgressMessage(message: String) {
                // State will be managed by the composable
            }
            
            override fun updateUssdConfigurationOptions(show: Boolean) {
                // State will be managed by the composable
            }
            
            override fun showUpi123CompletionDialog() {
                runOnUiThread {
                    showUpi123CompletionDialog.value = true
                }
            }
            
            override fun navigateToMain() {
                val intent = Intent(this@TestConfigurationActivity, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        })
        
        // Initialize the helper
        testHelper.initialize()
        
        setContent {
            FlowPayTheme {
                TestConfigurationScreen(
                    testHelper = testHelper,
                    showUpi123CompletionDialog = showUpi123CompletionDialog.value,
                    onUpi123DialogDismiss = { confirmed ->
                        showUpi123CompletionDialog.value = false
                        testHelper.handleUpi123ConfigurationConfirmation(confirmed)
                    }
                )
            }
        }
    }
    
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        testHelper.handlePermissionResult(requestCode, permissions, grantResults)
    }
}

@Composable
fun TestConfigurationScreen(
    testHelper: TestConfigurationHelper,
    showUpi123CompletionDialog: Boolean = false,
    onUpi123DialogDismiss: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    
    // Get test states from helper
    val testStates = testHelper.getTestStates()
    var ussdTestCompleted by remember { mutableStateOf(testStates.ussdTestCompleted) }
    var upi123TestCompleted by remember { mutableStateOf(testStates.upi123TestCompleted) }
    var ussdTesting by remember { mutableStateOf(testStates.ussdTesting) }
    var upi123Testing by remember { mutableStateOf(testStates.upi123Testing) }
    var showUssdDialog by remember { mutableStateOf(testStates.showUssdDialog) }
    var showUpi123Dialog by remember { mutableStateOf(testStates.showUpi123Dialog) }
    var showUssdConfigurationOptions by remember { mutableStateOf(testStates.showUssdConfigurationOptions) }
    var showUpi123ConfigurationOptions by remember { mutableStateOf(testStates.showUpi123ConfigurationOptions) }
    var ussdProgressMessage by remember { mutableStateOf(testStates.ussdProgressMessage) }
    var showCallCompleteButton by remember { mutableStateOf(testStates.showCallCompleteButton) }
    
    // Load existing test results
    LaunchedEffect(Unit) {
        val existingResults = testHelper.getTestResults()
        if (existingResults != null) {
            ussdTestCompleted = existingResults.ussdEnabled
            upi123TestCompleted = existingResults.upi123Enabled
        }
    }
    
    // Update states when helper states change
    LaunchedEffect(testStates) {
        ussdTestCompleted = testStates.ussdTestCompleted
        upi123TestCompleted = testStates.upi123TestCompleted
        ussdTesting = testStates.ussdTesting
        upi123Testing = testStates.upi123Testing
        showUssdDialog = testStates.showUssdDialog
        showUpi123Dialog = testStates.showUpi123Dialog
        showUssdConfigurationOptions = testStates.showUssdConfigurationOptions
        showUpi123ConfigurationOptions = testStates.showUpi123ConfigurationOptions
        ussdProgressMessage = testStates.ussdProgressMessage
        showCallCompleteButton = testStates.showCallCompleteButton
    }
    
    // Add a periodic state check to ensure UI updates
    LaunchedEffect(Unit) {
        while (true) {
            delay(100) // Check every 100ms
            val currentStates = testHelper.getTestStates()
            ussdTestCompleted = currentStates.ussdTestCompleted
            upi123TestCompleted = currentStates.upi123TestCompleted
            ussdTesting = currentStates.ussdTesting
            upi123Testing = currentStates.upi123Testing
            showUssdDialog = currentStates.showUssdDialog
            showUpi123Dialog = currentStates.showUpi123Dialog
            showUssdConfigurationOptions = currentStates.showUssdConfigurationOptions
            showUpi123ConfigurationOptions = currentStates.showUpi123ConfigurationOptions
            ussdProgressMessage = currentStates.ussdProgressMessage
            showCallCompleteButton = currentStates.showCallCompleteButton
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Test Header Card
            TestHeaderCard()
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Test Instructions
            TestInstructions()
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Test Buttons Container
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // USSD Test Button
                TestButton(
                    title = "Set up",
                    code = "*99#",
                    description = "Enable pay via scanning payments",
                    isCompleted = ussdTestCompleted,
                    isTesting = ussdTesting,
                    onClick = {
                        if (!ussdTestCompleted && !ussdTesting) {
                            testHelper.initiateCall(CallType.USSD)
                        }
                    }
                )
                
                // UPI123 Test Button
                TestButton(
                    title = "Set up",
                    code = "UPI123",
                    description = "Enable manual payment entries",
                    isCompleted = upi123TestCompleted,
                    isTesting = upi123Testing,
                    onClick = {
                        if (!upi123TestCompleted && !upi123Testing) {
                            testHelper.initiateUpi123Test()
                        }
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Skip Button
            Button(
                onClick = {
                    testHelper.skipTests()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF444444))
            ) {
                Text(
                    text = "Skip tests for now",
                    fontSize = 16.sp,
                    color = Color(0xFF888888),
                    modifier = Modifier.padding(18.dp)
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Continue Button
            val canContinue = testHelper.canContinue()
            val allTestsCompleted = testHelper.allTestsCompleted()
            
            Button(
                onClick = {
                    testHelper.continueToMain()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = canContinue,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (canContinue) Color(0xFF4A90E2) else Color(0xFF4A90E2).copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(25.dp)
            ) {
                Text(
                    text = when {
                        allTestsCompleted -> "All Tests Passed! Continue"
                        canContinue -> "Continue with partial setup"
                        else -> "Complete tests to continue"
                    },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.padding(22.dp)
                )
            }
        }
        
        // USSD Progress Dialog
        UssdProgressDialog(
            isVisible = showUssdDialog || showUssdConfigurationOptions,
            progressMessage = ussdProgressMessage,
            showConfigurationOptions = showUssdConfigurationOptions,
            onConfigured = { testHelper.handleUssdConfigurationConfirmation(true) },
            onNotConfigured = { testHelper.handleUssdConfigurationConfirmation(false) }
        )
        
        // UPI123 Completion Dialog - Custom styled dialog matching the UI
        if (showUpi123CompletionDialog) {
            Upi123CompletionDialog(
                onConfirmed = { onUpi123DialogDismiss(true) },
                onNotYet = { onUpi123DialogDismiss(false) }
            )
        }
    }
}

@Composable
fun Upi123CompletionDialog(
    onConfirmed: () -> Unit,
    onNotYet: () -> Unit
) {
    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1C1C1E)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF2C2C2E))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon with gradient background
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            color = Color(0xFF4A90E2).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(24.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = UpiIcon,
                        contentDescription = "UPI123",
                        tint = Color(0xFF4A90E2),
                        modifier = Modifier.size(44.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Title
                Text(
                    text = "UPI123 Setup",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    letterSpacing = (-0.5).sp
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Main question
                Text(
                    text = "Have you completed the UPI 123 setup?",
                    fontSize = 17.sp,
                    color = Color(0xFFE5E5EA),
                    textAlign = TextAlign.Center,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                
                // Additional info
                Text(
                    text = "Confirm if you've successfully completed the setup process.",
                    fontSize = 14.sp,
                    color = Color(0xFF8E8E93),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Buttons with better styling
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Yes, Completed Button
                    Button(
                        onClick = onConfirmed,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4A90E2)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 2.dp
                        )
                    ) {
                        Text(
                            text = "Yes, Completed",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            letterSpacing = 0.2.sp
                        )
                    }
                    
                    // Not Yet Button
                    Button(
                        onClick = onNotYet,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2C2C2E)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF3A3A3C))
                    ) {
                        Text(
                            text = "Not Yet",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFAAAAAA),
                            letterSpacing = 0.2.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TestHeaderCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8E8E8))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(35.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Test Icon
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(
                        color = Color(0xFF4A90E2),
                        shape = RoundedCornerShape(15.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = CheckCircleIcon,
                    contentDescription = "Test",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(25.dp))
            
            Text(
                text = "Test Configuration",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF000000),
                letterSpacing = (-1).sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Complete these tests to verify your\npayment methods are working correctly",
                fontSize = 16.sp,
                color = Color(0xFF666666),
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.3.sp,
                lineHeight = 24.sp,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Progress Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ProgressDot(isActive = false)
                ProgressDot(isActive = true)
                ProgressDot(isActive = false)
            }
        }
    }
}

@Composable
fun ProgressDot(isActive: Boolean) {
    Box(
        modifier = Modifier
            .width(if (isActive) 24.dp else 8.dp)
            .height(8.dp)
            .background(
                color = if (isActive) Color(0xFF4A90E2) else Color(0xFFCCCCCC),
                shape = if (isActive) RoundedCornerShape(4.dp) else CircleShape
            )
    )
}

@Composable
fun TestInstructions() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Configure Payment Methods",
            fontSize = 24.sp,
            fontWeight = FontWeight.Light,
            color = Color.White,
            letterSpacing = 0.3.sp
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "We'll test both scanning and manual payment methods to ensure everything works smoothly",
            fontSize = 14.sp,
            color = Color(0xFF888888),
            lineHeight = 21.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
    }
}

@Composable
fun TestButton(
    title: String,
    code: String,
    description: String,
    isCompleted: Boolean,
    isTesting: Boolean,
    onClick: () -> Unit
) {
    // Debounce the click handler to prevent duplicate dials
    var lastClickTime by remember { mutableStateOf(0L) }
    val debouncedOnClick = {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime >= 1000) { // 1 second debounce
            lastClickTime = currentTime
            onClick()
        }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = !isTesting && !isCompleted,
                onClick = debouncedOnClick
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) Color(0xFF1A3A1A) else Color(0xFF1A1A1A)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, 
            if (isCompleted) Color(0xFF4CAF50) else Color(0xFF333333)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(25.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Test Button Icon
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(
                        color = Color(0x1AFFFFFF),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (code == "*99#") UssdIcon else UpiIcon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            // Test Button Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "$title ",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = code,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4A90E2),
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
                
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = Color(0xFF888888),
                    lineHeight = 20.sp
                )
            }
            
            // Test Button Status
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isTesting -> {
                        LoadingSpinner()
                    }
                    isCompleted -> {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(Color(0xFF4CAF50), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = CheckIcon,
                                contentDescription = "Completed",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .border(2.dp, Color(0xFF444444), CircleShape)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingSpinner() {
    val infiniteTransition = rememberInfiniteTransition(label = "spinner")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    Box(
        modifier = Modifier
            .size(20.dp)
            .background(Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .border(2.dp, Color.White, CircleShape)
                .border(2.dp, Color.Transparent, CircleShape)
                .rotate(rotation)
        )
    }
}

// Configuration Dialogs - Now integrated into progress dialogs

// Custom Icons
val CheckCircleIcon: ImageVector
    get() {
        return ImageVector.Builder(
            name = "check_circle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = androidx.compose.ui.graphics.SolidColor(Color.White),
                strokeLineWidth = 2.5f,
                strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round,
                strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round
            ) {
                moveTo(9f, 11f)
                lineTo(15f, 17f)
                lineTo(22f, 4f)
            }
            path(
                fill = null,
                stroke = androidx.compose.ui.graphics.SolidColor(Color.White),
                strokeLineWidth = 2.5f,
                strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round,
                strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round
            ) {
                moveTo(21f, 12f)
                lineTo(21f, 19f)
                arcTo(2f, 2f, 0f, true, true, 19f, 21f)
                lineTo(5f, 21f)
                arcTo(2f, 2f, 0f, true, true, 3f, 19f)
                lineTo(3f, 5f)
                arcTo(2f, 2f, 0f, true, true, 5f, 3f)
                lineTo(12f, 3f)
            }
        }.build()
    }

val UssdIcon: ImageVector
    get() {
        return ImageVector.Builder(
            name = "ussd",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = androidx.compose.ui.graphics.SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round,
                strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round
            ) {
                moveTo(4f, 4f)
                lineTo(20f, 4f)
                arcTo(2f, 2f, 0f, true, true, 22f, 6f)
                lineTo(22f, 18f)
                arcTo(2f, 2f, 0f, true, true, 20f, 20f)
                lineTo(4f, 20f)
                arcTo(2f, 2f, 0f, true, true, 2f, 18f)
                lineTo(2f, 6f)
                arcTo(2f, 2f, 0f, true, true, 4f, 4f)
                close()
            }
            path(
                fill = null,
                stroke = androidx.compose.ui.graphics.SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round,
                strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round
            ) {
                moveTo(9f, 9f)
                lineTo(15f, 9f)
            }
            path(
                fill = null,
                stroke = androidx.compose.ui.graphics.SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round,
                strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round
            ) {
                moveTo(9f, 12f)
                lineTo(15f, 12f)
            }
            path(
                fill = null,
                stroke = androidx.compose.ui.graphics.SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round,
                strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round
            ) {
                moveTo(9f, 15f)
                lineTo(15f, 15f)
            }
        }.build()
    }

val UpiIcon: ImageVector
    get() {
        return ImageVector.Builder(
            name = "upi",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = androidx.compose.ui.graphics.SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round,
                strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round
            ) {
                moveTo(2f, 7f)
                lineTo(22f, 7f)
                arcTo(2f, 2f, 0f, true, true, 22f, 9f)
                lineTo(22f, 19f)
                arcTo(2f, 2f, 0f, true, true, 20f, 21f)
                lineTo(4f, 21f)
                arcTo(2f, 2f, 0f, true, true, 2f, 19f)
                lineTo(2f, 9f)
                arcTo(2f, 2f, 0f, true, true, 2f, 7f)
                close()
            }
            path(
                fill = null,
                stroke = androidx.compose.ui.graphics.SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round,
                strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round
            ) {
                moveTo(7f, 12f)
                lineTo(9f, 12f)
            }
            path(
                fill = null,
                stroke = androidx.compose.ui.graphics.SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round,
                strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round
            ) {
                moveTo(7f, 16f)
                lineTo(13f, 16f)
            }
            path(
                fill = null,
                stroke = androidx.compose.ui.graphics.SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round,
                strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round
            ) {
                moveTo(16f, 12f)
                lineTo(18f, 12f)
            }
            path(
                fill = null,
                stroke = androidx.compose.ui.graphics.SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round,
                strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round
            ) {
                moveTo(16f, 16f)
                lineTo(18f, 16f)
            }
        }.build()
    }

val CheckIcon: ImageVector
    get() {
        return ImageVector.Builder(
            name = "check",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = androidx.compose.ui.graphics.SolidColor(Color.White),
                strokeLineWidth = 3f,
                strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round,
                strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round
            ) {
                moveTo(5f, 13f)
                lineTo(9f, 17f)
                lineTo(19f, 7f)
            }
        }.build()
    }
