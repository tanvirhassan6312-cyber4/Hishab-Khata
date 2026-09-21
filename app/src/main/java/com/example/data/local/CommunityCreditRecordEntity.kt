package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "community_credit_records",
    indices = [
        Index(value = ["customerPhone"], unique = true),
        Index(value = ["phoneHash"])
    ]
)
data class CommunityCreditRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val customerPhone: String,
    val phoneHash: String,
    val customerName: String = "",
    val defaultReportsCount: Int = 1,
    val totalOverdueReported: Double = 0.0,
    val isDefaulter: Boolean = true,
    val riskLevel: String = "HIGH", // HIGH, MEDIUM, SAFE
    val anonymousNote: String = "অন্য ১টি দোকানে অপরিশোধিত বাকি রয়েছে।",
    val reportedTimestamp: Long = System.currentTimeMillis()
)
