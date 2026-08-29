package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shop_profile")
data class ShopProfileEntity(
    @PrimaryKey
    val id: Int = 1,
    val shopName: String = "আমার ব্যবসা",
    val ownerName: String = "তানভির আহমেদ",
    val phone: String = "01700000000",
    val address: String = "ঢাকা, বাংলাদেশ",
    val currencySymbol: String = "৳",
    val smsTemplate: String = "সম্মানিত গ্রাহক {customer_name}, {shop_name}-এ আপনার {product_name}-এর বকেয়া {due_amount} টাকা পরিশোধের তারিখ আজ। অনুগ্রহ করে টাকা পরিশোধ করুন। ধন্যবাদ।",
    val dueReminderDaysBefore: Int = 0,
    val isNotificationEnabled: Boolean = true
)
