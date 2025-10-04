package com.flowpay.app.utils

import com.flowpay.app.data.Transaction
import com.flowpay.app.helpers.SimpleTransaction
import com.flowpay.app.repository.TransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

/**
 * Utility class for generating test transactions
 * Useful for testing the transaction history feature
 */
object TestTransactionGenerator {
    
    private val banks = listOf("HDFC Bank", "ICICI Bank", "SBI", "Axis Bank", "Kotak Mahindra Bank")
    private val statuses = listOf("SUCCESS", "FAILED", "PENDING")
    private val recipientNames = listOf(
        "John Doe", "Jane Smith", "Raj Kumar", "Priya Sharma", "Amit Patel",
        "Sneha Singh", "Vikram Reddy", "Anita Joshi", "Ravi Gupta", "Meera Iyer"
    )
    private val phoneNumbers = listOf(
        "9876543210", "8765432109", "7654321098", "6543210987", "5432109876",
        "4321098765", "3210987654", "2109876543", "1098765432", "0987654321"
    )
    
    /**
     * Generate a random test transaction
     */
    fun generateRandomTransaction(): SimpleTransaction {
        val random = Random()
        val amount = String.format("%.2f", random.nextDouble() * 10000 + 100) // ₹100 to ₹10,100
        val bank = banks.random()
        val status = statuses.random()
        val recipientName = recipientNames.random()
        val phoneNumber = phoneNumbers.random()
        val transactionId = generateTransactionId()
        
        return SimpleTransaction(
            transactionId = transactionId,
            amount = amount,
            status = status,
            bankName = bank,
            rawMessage = generateRawMessage(amount, bank, status, recipientName),
            timestamp = System.currentTimeMillis() - random.nextLong() % (30 * 24 * 60 * 60 * 1000), // Last 30 days
            upiId = "test@${bank.lowercase().replace(" ", "")}.com",
            transactionType = "DEBIT",
            recipientName = recipientName,
            phoneNumber = phoneNumber
        )
    }
    
    /**
     * Generate multiple test transactions
     */
    fun generateTestTransactions(count: Int): List<SimpleTransaction> {
        return (1..count).map { generateRandomTransaction() }
    }
    
    /**
     * Save test transactions to database
     */
    fun saveTestTransactions(
        context: android.content.Context,
        count: Int = 10,
        onComplete: (() -> Unit)? = null
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = TransactionRepository.getInstance(context)
                val testTransactions = generateTestTransactions(count)
                
                testTransactions.forEach { simpleTransaction ->
                    repository.saveTransaction(simpleTransaction)
                }
                
                onComplete?.invoke()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private fun generateTransactionId(): String {
        val random = Random()
        val timestamp = System.currentTimeMillis()
        val randomPart = random.nextInt(10000)
        return "TXN${timestamp}${randomPart}"
    }
    
    private fun generateRawMessage(
        amount: String,
        bank: String,
        status: String,
        recipientName: String
    ): String {
        return when (status) {
            "SUCCESS" -> {
                "Dear Customer, ₹$amount has been debited from your account for payment to $recipientName. " +
                "Txn ID: ${generateTransactionId()}. " +
                "Available balance: ₹${String.format("%.2f", Random().nextDouble() * 50000 + 10000)}. " +
                "Thank you for using $bank."
            }
            "FAILED" -> {
                "Dear Customer, your payment of ₹$amount to $recipientName has failed due to insufficient funds. " +
                "Txn ID: ${generateTransactionId()}. " +
                "Please try again with sufficient balance. " +
                "Thank you for using $bank."
            }
            "PENDING" -> {
                "Dear Customer, your payment of ₹$amount to $recipientName is being processed. " +
                "Txn ID: ${generateTransactionId()}. " +
                "You will receive confirmation shortly. " +
                "Thank you for using $bank."
            }
            else -> {
                "Dear Customer, transaction of ₹$amount to $recipientName is $status. " +
                "Txn ID: ${generateTransactionId()}. " +
                "Thank you for using $bank."
            }
        }
    }
}

