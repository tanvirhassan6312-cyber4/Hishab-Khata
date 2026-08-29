package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sale_transactions")
data class SaleTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: Long,
    val productName: String,
    val previousStock: Int,
    val quantitySold: Int,
    val currentStock: Int,
    val unitPrice: Double,
    val totalAmount: Double,
    val paidAmount: Double,
    val dueAmount: Double,
    val isDue: Boolean = false,
    val isPaid: Boolean = false, // When due is cleared later
    val customerName: String? = null,
    val customerPhone: String? = null,
    val shopkeeperName: String? = null,
    val dueDate: Long? = null, // timestamp of payment promise
    val paidAt: Long? = null, // timestamp when marked as paid
    val note: String? = null,
    val qrCodeUsed: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
