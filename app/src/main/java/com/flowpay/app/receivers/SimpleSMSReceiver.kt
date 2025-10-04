package com.flowpay.app.receivers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.flowpay.app.helpers.TransactionDetector
import com.flowpay.app.helpers.AudioStateManager
import com.flowpay.app.ui.activities.PaymentSuccessActivity
import com.flowpay.app.repository.TransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SimpleSMSReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "SimpleSMSReceiver"
    }
    
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        
        Log.d(TAG, "SMS Received - Processing...")
        
        try {
            val messages = extractMessages(intent)
            if (messages.isNullOrEmpty()) {
                Log.d(TAG, "No messages found in intent")
                return
            }
            
            for (smsMessage in messages) {
                processSingleSMS(context, smsMessage)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing SMS", e)
        }
    }
    
    private fun extractMessages(intent: Intent): Array<SmsMessage>? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                Telephony.Sms.Intents.getMessagesFromIntent(intent)
            } else {
                val pdus = intent.extras?.get("pdus") as? Array<*>
                pdus?.mapNotNull { pdu ->
                    try {
                        @Suppress("DEPRECATION")
                        SmsMessage.createFromPdu(pdu as ByteArray)
                    } catch (e: Exception) {
                        null
                    }
                }?.toTypedArray()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract messages", e)
            null
        }
    }
    
    private fun processSingleSMS(context: Context, smsMessage: SmsMessage) {
        val sender = smsMessage.originatingAddress ?: "Unknown"
        val body = smsMessage.messageBody ?: ""
        
        Log.d(TAG, "=== SMS Details ===")
        Log.d(TAG, "Sender: $sender")
        Log.d(TAG, "Body: ${body.take(100)}...")
        
        val detector = TransactionDetector.getInstance(context)
        
        // FIRST CHECK: Is there an active payment operation?
        if (!detector.shouldProcessSMS()) {
            Log.d(TAG, "No active payment operation, ignoring SMS")
            return  // Exit early - don't process or save anything
        }
        
        Log.d(TAG, "Active payment operation found, checking if bank SMS...")
        
        // SECOND CHECK: Process the SMS to see if it's a valid transaction
        val result = detector.processSMS(sender, body)
        
        if (result != null) {
            Log.d(TAG, "✅ Transaction Detected!")
            Log.d(TAG, "Amount: ${result.amount}")
            Log.d(TAG, "Status: ${result.status}")
            Log.d(TAG, "Transaction ID: ${result.transactionId}")
            
            // For UPI 123, mute IMMEDIATELY before any other processing
            val operationType = detector.getOperationType()
            if (operationType == "UPI_123") {
                Log.d(TAG, "UPI 123 transaction - MUTING CALL IMMEDIATELY")
                
                // Mute in main thread for immediate effect
                Handler(Looper.getMainLooper()).post {
                    val muted = AudioStateManager.muteCallAudio(context)
                    if (muted) {
                        Log.d(TAG, "✅ Call audio muted successfully")
                        
                        // Show toast to user
                        Toast.makeText(context, "Call muted - Payment successful", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.e(TAG, "❌ Failed to mute call audio")
                    }
                }
            }
            
            // Save to database
            saveTransactionToDatabase(context, result)
            
            // Send broadcast to dismiss USSD overlay
            Log.d(TAG, "Sending DISMISS_OVERLAY broadcast")
            val dismissIntent = Intent("DISMISS_OVERLAY")
            LocalBroadcastManager.getInstance(context).sendBroadcast(dismissIntent)
            
            // Also send the broadcast that QRScannerActivity expects
            Log.d(TAG, "Sending SMS_RECEIVED broadcast for QRScannerActivity")
            val smsIntent = Intent("com.flowpay.app.SMS_RECEIVED")
            LocalBroadcastManager.getInstance(context).sendBroadcast(smsIntent)
            
            launchSuccessScreen(context, result)
        } else {
            Log.d(TAG, "❌ Not a transaction SMS or doesn't match criteria")
        }
    }
    
    // NEW: Separate method for database saving with better error handling
    private fun saveTransactionToDatabase(context: Context, transaction: com.flowpay.app.helpers.SimpleTransaction) {
        Log.d(TAG, "Saving transaction with ID: ${transaction.transactionId}")
        Log.d(TAG, "Recipient: ${transaction.recipientName}, Amount: ${transaction.amount}")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = TransactionRepository.getInstance(context)
                
                // Log before saving
                Log.d(TAG, "About to save transaction ID: ${transaction.transactionId}")
                
                repository.saveTransaction(transaction)
                Log.d(TAG, "✅ Transaction saved to database with ID: ${transaction.transactionId}")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to save transaction to database", e)
            }
        }
    }
    
    private fun launchSuccessScreen(context: Context, transaction: com.flowpay.app.helpers.SimpleTransaction) {
        Log.d(TAG, "Launching success screen...")
        Log.d(TAG, "Transaction details - Recipient: ${transaction.recipientName}, Phone: ${transaction.phoneNumber}")
        
        val intent = Intent(context, PaymentSuccessActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("transaction_id", transaction.transactionId)
            putExtra("amount", transaction.amount)
            putExtra("status", transaction.status)
            putExtra("bank_name", transaction.bankName)
            putExtra("message", transaction.rawMessage)
            putExtra("timestamp", transaction.timestamp)
            putExtra("upi_id", transaction.upiId)
            putExtra("transaction_type", transaction.transactionType)
            putExtra("recipient_name", transaction.recipientName)  // NEW
            putExtra("phone_number", transaction.phoneNumber)      // NEW
        }
        
        context.startActivity(intent)
    }
    
}
