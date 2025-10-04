package com.flowpay.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

/**
 * Room entity for storing transaction data
 * Maps to the existing SimpleTransaction structure
 */
@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey
    val transactionId: String,
    val amount: String,
    val status: String,
    val bankName: String,
    val rawMessage: String,
    val timestamp: Long = System.currentTimeMillis(),
    val upiId: String? = null,
    val transactionType: String = "DEBIT",
    val recipientName: String? = null,
    val phoneNumber: String? = null
) {
    /**
     * Convert to PaymentDetails for UI compatibility
     */
    fun toPaymentDetails(): PaymentDetails {
        return PaymentDetails(
            id = transactionId,
            recipientName = recipientName,
            phoneNumber = phoneNumber ?: "",
            amount = amount.toDoubleOrNull() ?: 0.0,
            timestamp = timestamp,
            status = when (status.uppercase()) {
                "SUCCESS", "SUCCESSFUL", "COMPLETED" -> PaymentStatus.COMPLETED
                "PENDING" -> PaymentStatus.PENDING
                "FAILED", "DECLINED", "CANCELLED" -> PaymentStatus.FAILED
                else -> PaymentStatus.PENDING
            }
        )
    }
    
    /**
     * Convert from SimpleTransaction
     */
    companion object {
        fun fromSimpleTransaction(simpleTransaction: com.flowpay.app.helpers.SimpleTransaction): Transaction {
            return Transaction(
                transactionId = simpleTransaction.transactionId,
                amount = simpleTransaction.amount,
                status = simpleTransaction.status,
                bankName = simpleTransaction.bankName,
                rawMessage = simpleTransaction.rawMessage,
                timestamp = simpleTransaction.timestamp,
                upiId = simpleTransaction.upiId,
                transactionType = simpleTransaction.transactionType,
                recipientName = simpleTransaction.recipientName,
                phoneNumber = simpleTransaction.phoneNumber
            )
        }
    }
}

