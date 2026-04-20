package com.flowpay.app.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.telephony.SmsMessage
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.flowpay.app.helpers.TransactionDetector
import com.flowpay.app.repository.TransactionRepository
import com.flowpay.app.ui.activities.PaymentSuccessActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SimpleSMSReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SimpleSMSReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent?.action != "android.provider.Telephony.SMS_RECEIVED") return

        val bundle: Bundle = intent.extras ?: return
        val pdus = bundle.get("pdus") as? Array<*> ?: return
        val format = bundle.getString("format") ?: "3gpp"

        // Reconstruct full message (multi-part SMS)
        val messages = pdus.mapNotNull { pdu ->
            try {
                SmsMessage.createFromPdu(pdu as ByteArray, format)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse PDU", e)
                null
            }
        }
        if (messages.isEmpty()) return

        val sender = messages[0].originatingAddress ?: return
        val body = messages.joinToString("") { it.messageBody }

        Log.d(TAG, "SMS received — Sender: $sender")

        val detector = TransactionDetector.getInstance(context)
        if (!detector.shouldProcessSMS()) {
            Log.d(TAG, "No active payment operation, ignoring SMS")
            return
        }

        val transaction = detector.processSMS(sender, body) ?: run {
            Log.d(TAG, "Not a transaction SMS")
            return
        }

        Log.d(TAG, "Transaction detected: amount=${transaction.amount}, status=${transaction.status}")

        // Save to database
        CoroutineScope(Dispatchers.IO).launch {
            try {
                TransactionRepository.getInstance(context).saveTransaction(transaction)
                Log.d(TAG, "Transaction saved: ${transaction.transactionId}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save transaction", e)
            }
        }

        // Broadcast for QRScannerActivity / UPI123 flow
        LocalBroadcastManager.getInstance(context)
            .sendBroadcast(Intent("com.flowpay.app.SMS_RECEIVED"))

        // Launch PaymentSuccessActivity
        val successIntent = Intent(context, PaymentSuccessActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("transaction_id",   transaction.transactionId)
            putExtra("amount",           transaction.amount)
            putExtra("status",           transaction.status)
            putExtra("bank_name",        transaction.bankName)
            putExtra("message",          transaction.rawMessage)
            putExtra("timestamp",        transaction.timestamp)
            putExtra("upi_id",           transaction.upiId)
            putExtra("transaction_type", transaction.transactionType)
            putExtra("recipient_name",   transaction.recipientName)
            putExtra("phone_number",     transaction.phoneNumber)
        }
        context.startActivity(successIntent)
    }
}
