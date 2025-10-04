package com.flowpay.app.ui.dialogs

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun Upi123ProgressDialog(
    isVisible: Boolean,
    showConfigurationOptions: Boolean = false,
    onConfigured: () -> Unit = {},
    onNotConfigured: () -> Unit = {}
) {
    if (isVisible) {
        Dialog(
            onDismissRequest = { /* No dismiss */ },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            Upi123ProgressDialogContent(
                showConfigurationOptions = showConfigurationOptions,
                onConfigured = onConfigured,
                onNotConfigured = onNotConfigured
            )
        }
    }
}

@Composable
private fun Upi123ProgressDialogContent(
    showConfigurationOptions: Boolean = false,
    onConfigured: () -> Unit = {},
    onNotConfigured: () -> Unit = {}
) {
    // Pulsing animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // UPI Icon with Pulsing Animation
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        color = Color(0xFF4CAF50).copy(alpha = alpha * 0.2f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = UpiIcon,
                    contentDescription = "UPI123 Setup",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(48.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Title
            Text(
                text = if (showConfigurationOptions) "Setup Complete?" else "Setting Up UPI123",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Message
            Text(
                text = if (showConfigurationOptions) 
                    "Did you complete the UPI123 setup?\n\nThis enables manual payment entries."
                else 
                    "We're configuring UPI123 for manual payment entries.\nPlease wait while we set everything up...",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF888888),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            if (showConfigurationOptions) {
                // Configuration Options
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Not Configured Button
                    Button(
                        onClick = onNotConfigured,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(15.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF333333)
                        )
                    ) {
                        Text(
                            text = "Not Yet",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    // Configured Button
                    Button(
                        onClick = onConfigured,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(15.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Text(
                            text = "Yes",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Simple progress indicator
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = Color(0xFF4CAF50),
                    trackColor = Color(0xFF333333)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Progress Message with Pulsing Animation
                Text(
                    text = "Configuring UPI123...",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Light,
                    color = Color(0xFF666666).copy(alpha = alpha),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// Custom UPI Icon
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
                fill = androidx.compose.ui.graphics.SolidColor(Color(0xFF4CAF50)),
                fillAlpha = 1f,
                stroke = null,
                strokeAlpha = 1f,
                strokeLineWidth = 1f,
                strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round,
                strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round,
                strokeLineMiter = 1f,
                pathFillType = androidx.compose.ui.graphics.PathFillType.NonZero
            ) {
                // UPI "U" shape
                moveTo(6f, 8f)
                verticalLineTo(16f)
                curveTo(6f, 18.2f, 7.8f, 20f, 10f, 20f)
                horizontalLineTo(14f)
                curveTo(16.2f, 20f, 18f, 18.2f, 18f, 16f)
                verticalLineTo(8f)
                moveTo(8f, 8f)
                verticalLineTo(16f)
                curveTo(8f, 17.1f, 8.9f, 18f, 10f, 18f)
                horizontalLineTo(14f)
                curveTo(15.1f, 18f, 16f, 17.1f, 16f, 16f)
                verticalLineTo(8f)
                moveTo(10f, 12f)
                horizontalLineTo(14f)
                moveTo(10f, 14f)
                horizontalLineTo(14f)
            }
        }.build()
    }
