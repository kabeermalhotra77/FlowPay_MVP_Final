package com.flowpay.app.helpers

import android.content.Context
import android.util.Log

object DebugHelper {
    private const val TAG = "SMS_DEBUG"
    
    fun logSMSReceived(sender: String, body: String) {
        Log.d(TAG, "=====================================")
        Log.d(TAG, "SMS RECEIVED")
        Log.d(TAG, "=====================================")
        Log.d(TAG, "Sender: $sender")
        Log.d(TAG, "Body Length: ${body.length}")
        Log.d(TAG, "Body: $body")
        Log.d(TAG, "=====================================")
    }
    
    fun testDetection(context: Context) {
        val testMessages = listOf(
            Pair("HDFCBK", "Your A/c XX1234 is debited for Rs.500.00 on 27-09-25. Info: UPI/123456789. Txn successful."),
            Pair("ICICIB", "Rs 1,000.00 debited from A/c XX5678. Payment successful."),
            Pair("123456", "Payment of Rs.100 successful. Transaction ID: ABC123"),
            Pair("AX-AXISBK", "INR 750.00 transferred successfully. Ref No: 2024092712345")
        )
        
        val detector = TransactionDetector.getInstance(context)
        
        for ((sender, body) in testMessages) {
            Log.d(TAG, "Testing: $sender")
            detector.startOperation("TEST", "500")
            val result = detector.processSMS(sender, body)
            Log.d(TAG, "Result: $result")
        }
    }
    
    // Test methods for SMSTestActivity
    fun sendTestSMS(context: Context) {
        Log.d(TAG, "Sending basic test SMS")
        testDetection(context)
    }
    
    fun sendTestSMSWithBankFormat(context: Context, bank: String) {
        Log.d(TAG, "Sending test SMS for bank: $bank")
        val testMessage = when (bank) {
            "HDFC" -> "Your A/c XX1234 is debited for Rs.500.00 on 27-09-25. Txn successful."
            "SBI" -> "Your SBI account is debited by Rs.250.50. Transaction completed successfully."
            "ICICI" -> "Rs 1,000.00 debited from A/c XX5678. Payment successful."
            "PAYTM" -> "Payment of Rs.100 successful via Paytm. Transaction ID: ABC123"
            else -> "Payment of Rs.100 successful. Transaction ID: ABC123"
        }
        
        val detector = TransactionDetector.getInstance(context)
        detector.startOperation("TEST", "500")
        val result = detector.processSMS("${bank}BK", testMessage)
        Log.d(TAG, "Test result for $bank: $result")
    }
    
    fun sendTestFailedSMS(context: Context) {
        Log.d(TAG, "Sending failed transaction test SMS")
        val detector = TransactionDetector.getInstance(context)
        detector.startOperation("TEST", "500")
        val result = detector.processSMS("TESTBK", "Transaction failed. Please try again.")
        Log.d(TAG, "Failed test result: $result")
    }
    
    fun sendTestPendingSMS(context: Context) {
        Log.d(TAG, "Sending pending transaction test SMS")
        val detector = TransactionDetector.getInstance(context)
        detector.startOperation("TEST", "500")
        val result = detector.processSMS("TESTBK", "Transaction pending. Please wait.")
        Log.d(TAG, "Pending test result: $result")
    }
    
    fun testAllSMSFormats(context: Context) {
        Log.d(TAG, "Testing all SMS formats")
        testDetection(context)
    }
}
