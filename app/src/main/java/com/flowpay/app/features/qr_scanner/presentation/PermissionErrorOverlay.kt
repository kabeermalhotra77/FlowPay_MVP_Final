package com.flowpay.app.features.qr_scanner.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flowpay.app.ui.theme.LocalFlowPayAccentTheme

@Composable
fun PermissionErrorOverlay(
    missingPermissions: List<String>,
    needsOverlayPermission: Boolean,
    onRequestPermissions: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Permissions Required",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Flowpay needs the following permissions to work properly:",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (missingPermissions.isNotEmpty()) {
                    Text(
                        text = "• Camera permission for QR scanning",
                        fontSize = 14.sp,
                        color = Color.Black
                    )
                }
                
                if (needsOverlayPermission) {
                    Text(
                        text = "• Overlay permission for payment protection",
                        fontSize = 14.sp,
                        color = Color.Black
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (missingPermissions.isNotEmpty()) {
                        Button(
                            onClick = onRequestPermissions,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LocalFlowPayAccentTheme.current.accent
                            )
                        ) {
                            Text(
                                text = "Grant Permissions",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }
                    
                    if (needsOverlayPermission) {
                        Button(
                            onClick = onRequestOverlayPermission,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LocalFlowPayAccentTheme.current.accent
                            )
                        ) {
                            Text(
                                text = "Grant Overlay",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Cancel",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
