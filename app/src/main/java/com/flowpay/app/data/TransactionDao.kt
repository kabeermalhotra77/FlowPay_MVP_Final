package com.flowpay.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Transaction operations
 */
@Dao
interface TransactionDao {
    
    /**
     * Get all transactions ordered by timestamp (newest first)
     */
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>
    
    /**
     * Get recent transactions (last 10)
     */
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int = 10): Flow<List<Transaction>>
    
    /**
     * Get transactions by status
     */
    @Query("SELECT * FROM transactions WHERE status = :status ORDER BY timestamp DESC")
    fun getTransactionsByStatus(status: String): Flow<List<Transaction>>
    
    /**
     * Get transactions by bank name
     */
    @Query("SELECT * FROM transactions WHERE bankName = :bankName ORDER BY timestamp DESC")
    fun getTransactionsByBank(bankName: String): Flow<List<Transaction>>
    
    /**
     * Search transactions by recipient name or phone number
     */
    @Query("SELECT * FROM transactions WHERE recipientName LIKE :query OR phoneNumber LIKE :query ORDER BY timestamp DESC")
    fun searchTransactions(query: String): Flow<List<Transaction>>
    
    /**
     * Get transaction by ID
     */
    @Query("SELECT * FROM transactions WHERE transactionId = :transactionId")
    suspend fun getTransactionById(transactionId: String): Transaction?
    
    /**
     * Insert a new transaction
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)
    
    /**
     * Insert multiple transactions
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<Transaction>)
    
    /**
     * Update an existing transaction
     */
    @Update
    suspend fun updateTransaction(transaction: Transaction)
    
    /**
     * Delete a transaction
     */
    @Delete
    suspend fun deleteTransaction(transaction: Transaction)
    
    /**
     * Delete transaction by ID
     */
    @Query("DELETE FROM transactions WHERE transactionId = :transactionId")
    suspend fun deleteTransactionById(transactionId: String)
    
    /**
     * Delete all transactions
     */
    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()
    
    /**
     * Get transaction count
     */
    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun getTransactionCount(): Int
    
    /**
     * Get total amount of completed transactions
     */
    @Query("SELECT SUM(CAST(amount AS REAL)) FROM transactions WHERE status IN ('SUCCESS', 'SUCCESSFUL', 'COMPLETED')")
    suspend fun getTotalAmount(): Double?
    
    /**
     * Get transactions within date range
     */
    @Query("SELECT * FROM transactions WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getTransactionsByDateRange(startTime: Long, endTime: Long): Flow<List<Transaction>>
}

