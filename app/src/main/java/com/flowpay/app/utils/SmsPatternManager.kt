// =====================================
// 3. SmsPatternManager.kt
// =====================================
package com.flowpay.app.utils

import java.util.regex.Pattern

class SmsPatternManager {
    
    companion object {
        // Transaction detection patterns
        private val DEBIT_PATTERN = Pattern.compile(
            "(?i)(debited|sent|transferred|paid|withdrawn)",
            Pattern.CASE_INSENSITIVE
        )
        
        private val SUCCESS_PATTERN = Pattern.compile(
            "(?i)(success|successful|completed|done)",
            Pattern.CASE_INSENSITIVE
        )
        
        private val AMOUNT_PATTERN = Pattern.compile(
            "(?:Rs\\.?|INR|₹)\\s*([0-9,]+(?:\\.[0-9]{2})?)",
            Pattern.CASE_INSENSITIVE
        )
        
        private val UPI_ID_PATTERN = Pattern.compile(
            "([a-zA-Z0-9._-]+@[a-zA-Z0-9]+)",
            Pattern.CASE_INSENSITIVE
        )
        
        private val PHONE_PATTERN = Pattern.compile(
            "(?:to|from)?\\s*([0-9]{10}|[0-9]{4}\\s*[0-9]{3}\\s*[0-9]{3})",
            Pattern.CASE_INSENSITIVE
        )
        
        // Supported banks
        val SUPPORTED_BANKS = listOf(
            "HDFC", "ICICI", "SBI", "AXIS", "KOTAK", 
            "PNB", "BOB", "CANARA", "UNION", "IDBI", 
            "YES", "INDUS", "PAYTM", "PHONEPE", "GPAY"
        )
    }
    
    data class TransactionDetails(
        val isDebit: Boolean,
        val isSuccess: Boolean,
        val amount: String?,
        val upiId: String?,
        val phoneNumber: String?,
        val bank: String?
    )
    
    fun parseSms(message: String): TransactionDetails {
        val messageUpper = message.uppercase()
        
        return TransactionDetails(
            isDebit = DEBIT_PATTERN.matcher(message).find(),
            isSuccess = SUCCESS_PATTERN.matcher(message).find(),
            amount = extractAmount(message),
            upiId = extractUpiId(message),
            phoneNumber = extractPhoneNumber(message),
            bank = SUPPORTED_BANKS.firstOrNull { messageUpper.contains(it) }
        )
    }
    
    private fun extractAmount(message: String): String? {
        val matcher = AMOUNT_PATTERN.matcher(message)
        return if (matcher.find()) {
            matcher.group(1)?.replace(",", "")
        } else null
    }
    
    private fun extractUpiId(message: String): String? {
        val matcher = UPI_ID_PATTERN.matcher(message)
        return if (matcher.find()) matcher.group(1) else null
    }
    
    private fun extractPhoneNumber(message: String): String? {
        val matcher = PHONE_PATTERN.matcher(message)
        return if (matcher.find()) {
            matcher.group(1)?.replace("\\s".toRegex(), "")
        } else null
    }
    
    fun testPattern(pattern: String, testMessage: String): Boolean {
        return try {
            Pattern.compile(pattern).matcher(testMessage).find()
        } catch (e: Exception) {
            false
        }
    }
}
