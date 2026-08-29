package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memos")
data class MemoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val memoNumber: String,
    val memoType: String = "বিক্রয় মেমো", // বিক্রয় মেমো, চালান / ডেলিভারি, ম্যানেজার / ক্যাশ ভাউচার, ক্রয় মেমো, বকেয়া মেমো
    val date: Long = System.currentTimeMillis(),
    val shopName: String,
    val preparedBy: String = "",
    val shopPhone: String = "",
    val shopAddress: String = "",
    val customerName: String,
    val customerPhone: String? = null,
    val customerAddress: String? = null,
    val itemsJson: String, // Serialized list of MemoItem
    val subtotal: Double = 0.0,
    val discountAmount: Double = 0.0,
    val vatPercent: Double = 0.0,
    val vatAmount: Double = 0.0,
    val grandTotal: Double = 0.0,
    val paidAmount: Double = 0.0,
    val dueAmount: Double = 0.0,
    val paymentMethod: String = "নগদ (Cash)",
    val notes: String? = null,
    val terms: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class MemoItem(
    val sl: Int,
    val itemName: String,
    val quantity: Double,
    val unit: String = "পিস",
    val unitPrice: Double,
    val total: Double
)
