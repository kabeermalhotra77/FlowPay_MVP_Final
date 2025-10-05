package com.flowpay.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.flowpay.app.data.PaymentDetails
import com.flowpay.app.data.PaymentStatus
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PaymentDetailsDetailDialog(
    payment: PaymentDetails,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transaction Details",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Transaction ID
                PaymentDetailRow(
                    label = "Transaction ID",
                    value = payment.id,
                    onCopy = {
                        clipboardManager.setText(AnnotatedString(payment.id))
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Amount
                PaymentDetailRow(
                    label = "Amount",
                    value = formatAmount(payment.amount),
                    onCopy = {
                        clipboardManager.setText(AnnotatedString(payment.amount.toString()))
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Status
                PaymentDetailRow(
                    label = "Status",
                    value = formatStatus(payment.status),
                    onCopy = {
                        clipboardManager.setText(AnnotatedString(payment.status.name))
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Recipient Name (if available)
                if (!payment.recipientName.isNullOrEmpty()) {
                    PaymentDetailRow(
                        label = "Recipient",
                        value = payment.recipientName,
                        onCopy = {
                            clipboardManager.setText(AnnotatedString(payment.recipientName))
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Phone Number
                if (payment.phoneNumber.isNotEmpty()) {
                    PaymentDetailRow(
                        label = "Phone Number",
                        value = payment.phoneNumber,
                        onCopy = {
                            clipboardManager.setText(AnnotatedString(payment.phoneNumber))
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Date and Time
                PaymentDetailRow(
                    label = "Date & Time",
                    value = formatFullDate(payment.timestamp),
                    onCopy = {
                        clipboardManager.setText(AnnotatedString(formatFullDate(payment.timestamp)))
                    }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Close Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A90E2))
                ) {
                    Text(
                        text = "Close",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentDetailRow(
    label: String,
    value: String,
    onCopy: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 14.sp,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
        }
        
        IconButton(
            onClick = onCopy,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CopyAll,
                contentDescription = "Copy",
                modifier = Modifier.size(16.dp),
                tint = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

private fun formatAmount(amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    return formatter.format(amount)
}

private fun formatStatus(status: PaymentStatus): String {
    return when (status) {
        PaymentStatus.COMPLETED -> "Completed"
        PaymentStatus.PENDING -> "Pending"
        PaymentStatus.FAILED -> "Failed"
        PaymentStatus.CANCELLED -> "Cancelled"
    }
}

private fun formatFullDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale("en", "IN"))
    return formatter.format(Date(timestamp))
}
