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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.flowpay.app.data.Transaction
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TransactionDetailDialog(
    transaction: Transaction,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
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
                TransactionDetailRow(
                    label = "Transaction ID",
                    value = transaction.transactionId,
                    onCopy = {
                        clipboardManager.setText(AnnotatedString(transaction.transactionId))
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Amount
                TransactionDetailRow(
                    label = "Amount",
                    value = formatAmount(transaction.amount.toDoubleOrNull() ?: 0.0),
                    onCopy = {
                        clipboardManager.setText(AnnotatedString(transaction.amount))
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Status
                TransactionDetailRow(
                    label = "Status",
                    value = transaction.status.replaceFirstChar { it.uppercase() },
                    onCopy = {
                        clipboardManager.setText(AnnotatedString(transaction.status))
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Bank
                TransactionDetailRow(
                    label = "Bank",
                    value = transaction.bankName,
                    onCopy = {
                        clipboardManager.setText(AnnotatedString(transaction.bankName))
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Recipient
                if (!transaction.recipientName.isNullOrEmpty()) {
                    TransactionDetailRow(
                        label = "Recipient",
                        value = transaction.recipientName,
                        onCopy = {
                            clipboardManager.setText(AnnotatedString(transaction.recipientName))
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Phone Number
                if (!transaction.phoneNumber.isNullOrEmpty()) {
                    TransactionDetailRow(
                        label = "Phone Number",
                        value = transaction.phoneNumber,
                        onCopy = {
                            clipboardManager.setText(AnnotatedString(transaction.phoneNumber))
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // UPI ID
                if (!transaction.upiId.isNullOrEmpty()) {
                    TransactionDetailRow(
                        label = "UPI ID",
                        value = transaction.upiId,
                        onCopy = {
                            clipboardManager.setText(AnnotatedString(transaction.upiId))
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Date & Time
                TransactionDetailRow(
                    label = "Date & Time",
                    value = formatFullDate(transaction.timestamp),
                    onCopy = {
                        clipboardManager.setText(AnnotatedString(formatFullDate(transaction.timestamp)))
                    }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Raw Message (if needed)
                if (transaction.rawMessage.isNotEmpty()) {
                    Text(
                        text = "Raw Message",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = transaction.rawMessage,
                            modifier = Modifier.padding(12.dp),
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            lineHeight = 16.sp
                        )
                    }
                }
                
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
private fun TransactionDetailRow(
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

private fun formatFullDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale("en", "IN"))
    return formatter.format(Date(timestamp))
}

