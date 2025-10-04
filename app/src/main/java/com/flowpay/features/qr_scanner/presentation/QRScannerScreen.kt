package com.flowpay.features.qr_scanner.presentation

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flowpay.features.qr_scanner.domain.CameraManager
import com.google.accompanist.permissions.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun QRScannerScreen(
    onNavigateBack: () -> Unit,
    viewModel: QRScannerViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.state.collectAsState()
    
    // Camera and Phone permissions
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.CALL_PHONE
        )
    )
    
    LaunchedEffect(Unit) {
        permissionsState.launchMultiplePermissionRequest()
        // Reset state when screen is launched
        viewModel.onScreenResumed()
    }
    
    // Cleanup when screen is disposed
    DisposableEffect(Unit) {
        onDispose {
            // Reset state when screen is disposed
            viewModel.resetToInitialState()
        }
    }
    
    if (permissionsState.allPermissionsGranted) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Camera Preview
            CameraPreview(
                onQrCodeScanned = { qrCode ->
                    viewModel.processQRCode(qrCode, context)
                },
                onCameraManagerReady = { cameraManager ->
                    viewModel.setCameraManager(cameraManager)
                }
            )
            
            // Scanning Overlay with animated elements
            AnimatedScanningOverlay()
            
            // Top Controls
            TopControls(
                onClose = {
                    // Reset state before closing
                    viewModel.resetToInitialState()
                    onNavigateBack()
                },
                onGalleryClick = {
                    // TODO: Implement gallery access
                }
            )
            
            // Bottom Instructions
            BottomInstructions()
            
            // Instructions Dialog (Upper 50% of screen)
            if (state.showInstructions) {
                PaymentInstructionsOverlay(
                    vpa = state.vpaAddress,
                    onTimeout = {
                        viewModel.onPaymentComplete()
                        onNavigateBack()
                    }
                )
                
                // Cancel Button (Lower 50% of screen)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 40.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Button(
                        onClick = {
                            viewModel.cancelPayment()
                            onNavigateBack()
                        },
                        modifier = Modifier
                            .width(200.dp)
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF4444)
                        ),
                        shape = RoundedCornerShape(25.dp)
                    ) {
                        Text(
                            "Cancel Payment",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            // Processing Screen
            if (state.showProcessing) {
                ProcessingScreen(
                    vpa = state.vpaAddress,
                    onClose = {
                        // Reset state before closing
                        viewModel.resetToInitialState()
                        onNavigateBack()
                    }
                )
            }
            
            // Success Dialog
            if (state.showSuccess) {
                SuccessDialog()
                LaunchedEffect(Unit) {
                    delay(2000) // Reduced to 2 seconds for faster flow
                    onNavigateBack()
                }
            }
            
            // Error handling
            state.error?.let { error ->
                LaunchedEffect(error) {
                    Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                    delay(2000)
                    viewModel.clearError()
                }
            }
        }
    } else {
        // Permission Denied Screen
        PermissionDeniedScreen(
            onRequestPermissions = { permissionsState.launchMultiplePermissionRequest() }
        )
    }
}

@Composable
fun CameraPreview(
    onQrCodeScanned: (String) -> Unit,
    onCameraManagerReady: (CameraManager) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraManager = remember { CameraManager() }
    
    DisposableEffect(Unit) {
        onDispose {
            cameraManager.stopCamera()
        }
    }
    
    // Notify parent about camera manager
    LaunchedEffect(cameraManager) {
        onCameraManagerReady(cameraManager)
    }
    
    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            
            cameraManager.initializeCamera(
                context = ctx,
                lifecycleOwner = lifecycleOwner,
                previewView = previewView,
                onQrCodeDetected = { qrCode ->
                    onQrCodeScanned(qrCode)
                }
            )
            
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}


@Composable
fun AnimatedScanningOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "scanning")
    val scanningLineOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanningLine"
    )
    
    val cornerPulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cornerPulse"
    )
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val scanFrameSize = size.width * 0.7f
        val cornerLength = scanFrameSize * 0.15f
        val strokeWidth = 6f
        
        // Dark overlay with gradient
        drawRect(
            color = Color.Black.copy(alpha = 0.6f),
            size = size
        )
        
        // Clear scanning area with rounded corners
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(centerX - scanFrameSize / 2, centerY - scanFrameSize / 2),
            size = Size(scanFrameSize, scanFrameSize),
            cornerRadius = CornerRadius(24f, 24f),
            blendMode = BlendMode.Clear
        )
        
        // Animated scanning line
        val lineY = centerY - scanFrameSize / 2 + (scanFrameSize * scanningLineOffset)
        drawLine(
            color = Color(0xFF4CAF50).copy(alpha = 0.8f),
            start = Offset(centerX - scanFrameSize / 2 + 20f, lineY),
            end = Offset(centerX + scanFrameSize / 2 - 20f, lineY),
            strokeWidth = 3f
        )
        
        // Corner brackets with pulse animation
        val corners = listOf(
            // Top-left
            Offset(centerX - scanFrameSize / 2, centerY - scanFrameSize / 2),
            // Top-right
            Offset(centerX + scanFrameSize / 2, centerY - scanFrameSize / 2),
            // Bottom-left
            Offset(centerX - scanFrameSize / 2, centerY + scanFrameSize / 2),
            // Bottom-right
            Offset(centerX + scanFrameSize / 2, centerY + scanFrameSize / 2)
        )
        
        corners.forEachIndexed { index, corner ->
            val animatedCornerLength = cornerLength * cornerPulse
            val path = Path().apply {
                when (index) {
                    0 -> { // Top-left
                        moveTo(corner.x, corner.y + animatedCornerLength)
                        lineTo(corner.x, corner.y)
                        lineTo(corner.x + animatedCornerLength, corner.y)
                    }
                    1 -> { // Top-right
                        moveTo(corner.x - animatedCornerLength, corner.y)
                        lineTo(corner.x, corner.y)
                        lineTo(corner.x, corner.y + animatedCornerLength)
                    }
                    2 -> { // Bottom-left
                        moveTo(corner.x, corner.y - animatedCornerLength)
                        lineTo(corner.x, corner.y)
                        lineTo(corner.x + animatedCornerLength, corner.y)
                    }
                    3 -> { // Bottom-right
                        moveTo(corner.x - animatedCornerLength, corner.y)
                        lineTo(corner.x, corner.y)
                        lineTo(corner.x, corner.y - animatedCornerLength)
                    }
                }
            }
            
            drawPath(
                path = path,
                color = Color(0xFF4CAF50),
                style = Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
        }
        
        // Center dot
        drawCircle(
            color = Color(0xFF4CAF50).copy(alpha = 0.6f),
            radius = 8f,
            center = Offset(centerX, centerY)
        )
    }
}

@Composable
fun TopControls(
    onClose: () -> Unit,
    onGalleryClick: () -> Unit
) {
    var isFlashOn by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Close Button
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .size(48.dp)
                .background(
                    Color.Black.copy(alpha = 0.6f),
                    RoundedCornerShape(24.dp)
                )
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        
        // Gallery Button
        IconButton(
            onClick = onGalleryClick,
            modifier = Modifier
                .size(48.dp)
                .background(
                    Color.Black.copy(alpha = 0.6f),
                    RoundedCornerShape(24.dp)
                )
        ) {
            Icon(
                imageVector = Icons.Default.PhotoLibrary,
                contentDescription = "Gallery",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        
        // Flash Toggle Button
        IconButton(
            onClick = { isFlashOn = !isFlashOn },
            modifier = Modifier
                .size(48.dp)
                .background(
                    if (isFlashOn) Color(0xFFFFD700) else Color.Black.copy(alpha = 0.6f),
                    RoundedCornerShape(24.dp)
                )
        ) {
            Icon(
                imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                contentDescription = if (isFlashOn) "Flash On" else "Flash Off",
                tint = if (isFlashOn) Color.Black else Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun BottomInstructions() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.8f)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Position QR code within the frame",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Make sure the QR code is well-lit and clearly visible",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun PermissionDeniedScreen(
    onRequestPermissions: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Camera",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(64.dp)
                )
                
                Text(
                    text = "Camera Permission Required",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = "FlowPay needs camera access to scan QR codes for payments. Please grant the required permissions to continue.",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
                
                Button(
                    onClick = onRequestPermissions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    shape = RoundedCornerShape(25.dp)
                ) {
                    Text(
                        "Grant Permissions",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ProcessingScreen(
    vpa: String,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Processing Icon with animation
                ProcessingIcon()
                
                // Title
                Text(
                    text = "Processing Payment",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )
                
                // VPA Address
                Text(
                    text = "VPA Address:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray
                )
                
                // VPA with copy button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color(0xFFF5F5F5),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = vpa,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black,
                        modifier = Modifier.weight(1f)
                    )
                    
                    IconButton(
                        onClick = {
                            // VPA is already copied to clipboard
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "VPA Copied",
                            tint = Color(0xFF4CAF50)
                        )
                    }
                }
                
                // Instructions
                Text(
                    text = "Complete the payment in your UPI app and return to FlowPay",
                    fontSize = 16.sp,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )
                
                // Status message
                Text(
                    text = "VPA has been copied to clipboard",
                    fontSize = 14.sp,
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Medium
                )
                
                // Close button
                Button(
                    onClick = onClose,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    shape = RoundedCornerShape(25.dp)
                ) {
                    Text(
                        "Close",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ProcessingIcon() {
    val infiniteTransition = rememberInfiniteTransition(label = "processing")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    Box(
        modifier = Modifier.size(64.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Sync,
            contentDescription = "Processing",
            tint = Color(0xFF4CAF50),
            modifier = Modifier
                .size(64.dp)
                .graphicsLayer {
                    rotationZ = rotation
                }
        )
    }
}

