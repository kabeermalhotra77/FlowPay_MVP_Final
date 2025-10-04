package com.flowpay.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Custom permission dialog that matches FlowPay's black theme and modern design
 */
@Composable
fun CustomPermissionDialog(
    title: String,
    message: String,
    positiveButtonText: String,
    negativeButtonText: String,
    onPositiveClick: () -> Unit,
    onNegativeClick: () -> Unit,
    onDismiss: () -> Unit,
    isCancelable: Boolean = true
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = isCancelable,
            dismissOnClickOutside = isCancelable
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A1A1A)
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 8.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with gradient background
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF7BA8F5), // Header blue - top
                                    Color(0xFF6A96EE)  // Header blue - bottom
                                )
                            )
                        )
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Message text
                Text(
                    text = message,
                    color = Color(0xFFE0E0E0),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Negative button
                    OutlinedButton(
                        onClick = onNegativeClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF888888)
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF404040),
                                    Color(0xFF404040)
                                )
                            )
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = negativeButtonText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    // Positive button
                    Button(
                        onClick = onPositiveClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF4A90E2),
                                            Color(0xFF3A7BD5)
                                        )
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = positiveButtonText,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Custom overlay permission dialog with enhanced styling
 */
@Composable
fun CustomOverlayPermissionDialog(
    onGrantPermission: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit
) {
    CustomPermissionDialog(
        title = "Overlay Permission Required",
        message = "FlowPay needs overlay permission to show payment protection dialogs during USSD calls.\n\n" +
                "This helps protect you from fraud by showing secure payment instructions.\n\n" +
                "Please grant overlay permission to continue.",
        positiveButtonText = "Grant Permission",
        negativeButtonText = "Cancel",
        onPositiveClick = onGrantPermission,
        onNegativeClick = onCancel,
        onDismiss = onDismiss,
        isCancelable = false
    )
}

/**
 * Custom basic permission dialog for camera, SMS, etc.
 */
@Composable
fun CustomBasicPermissionDialog(
    title: String,
    message: String,
    onGrantPermission: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit
) {
    CustomPermissionDialog(
        title = title,
        message = message,
        positiveButtonText = "Grant Permission",
        negativeButtonText = "Cancel",
        onPositiveClick = onGrantPermission,
        onNegativeClick = onCancel,
        onDismiss = onDismiss,
        isCancelable = true
    )
}
