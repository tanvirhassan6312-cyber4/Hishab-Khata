package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "products",
    indices = [Index(value = ["qrCode"], unique = true)]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val buyPrice: Double = 0.0,
    val sellPrice: Double = 0.0,
    val stock: Int = 0,
    val unit: String = "পিস", // কেজি, পিস, লিটার, প্যাকেট, ডজন, বস্তা
    val category: String = "সাধারণ",
    val qrCode: String? = null, // Unique QR Code e.g. "CHAL-001"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
