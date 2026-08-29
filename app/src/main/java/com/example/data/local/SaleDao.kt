package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {

    @Query("SELECT * FROM sale_transactions ORDER BY createdAt DESC")
    fun getAllTransactions(): Flow<List<SaleTransactionEntity>>

    @Query("SELECT * FROM sale_transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): SaleTransactionEntity?

    @Query("SELECT * FROM sale_transactions WHERE productId = :productId ORDER BY createdAt DESC")
    fun getTransactionsByProduct(productId: Long): Flow<List<SaleTransactionEntity>>

    @Query("SELECT * FROM sale_transactions WHERE productName LIKE '%' || :query || '%' OR customerName LIKE '%' || :query || '%' OR customerPhone LIKE '%' || :query || '%' OR qrCodeUsed LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchTransactions(query: String): Flow<List<SaleTransactionEntity>>

    @Query("SELECT * FROM sale_transactions WHERE createdAt >= :startOfDay AND createdAt <= :endOfDay ORDER BY createdAt DESC")
    fun getTransactionsForDateRange(startOfDay: Long, endOfDay: Long): Flow<List<SaleTransactionEntity>>

    @Query("SELECT * FROM sale_transactions WHERE isDue = 1 ORDER BY isPaid ASC, dueDate ASC")
    fun getAllDueTransactions(): Flow<List<SaleTransactionEntity>>

    @Query("SELECT * FROM sale_transactions WHERE isDue = 1 AND isPaid = 0 AND dueDate IS NOT NULL AND dueDate <= :endOfDay ORDER BY dueDate ASC")
    fun getPendingDueReminders(endOfDay: Long): Flow<List<SaleTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: SaleTransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: SaleTransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: SaleTransactionEntity)

    @Query("UPDATE sale_transactions SET isPaid = 1, paidAt = :paidAt WHERE id = :id")
    suspend fun markAsPaid(id: Long, paidAt: Long = System.currentTimeMillis())

    @Query("UPDATE sale_transactions SET isPaid = 0, paidAt = NULL WHERE id = :id")
    suspend fun markAsUnpaid(id: Long)

    @Query("SELECT SUM(totalAmount) FROM sale_transactions WHERE createdAt >= :startOfDay AND createdAt <= :endOfDay")
    fun getTodayTotalSales(startOfDay: Long, endOfDay: Long): Flow<Double?>

    @Query("SELECT SUM(dueAmount) FROM sale_transactions WHERE isDue = 1 AND isPaid = 0 AND createdAt >= :startOfDay AND createdAt <= :endOfDay")
    fun getTodayTotalDue(startOfDay: Long, endOfDay: Long): Flow<Double?>

    @Query("SELECT SUM(dueAmount) FROM sale_transactions WHERE isDue = 1 AND isPaid = 0")
    fun getAllPendingDueTotal(): Flow<Double?>

    @Query("DELETE FROM sale_transactions")
    suspend fun deleteAllTransactions()
}
