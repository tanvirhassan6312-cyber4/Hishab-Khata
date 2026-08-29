package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object ReminderUtils {

    fun dialPhoneNumber(context: Context, phoneNumber: String) {
        val cleanPhone = phoneNumber.replace(Regex("[^0-9+]"), "")
        if (cleanPhone.isBlank()) {
            Toast.makeText(context, "ফোন নম্বর পাওয়া যায়নি", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$cleanPhone")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "ডায়ালার খুলতে সমস্যা হয়েছে", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Send completely free automated reminder message via WhatsApp
     */
    fun sendWhatsAppReminder(
        context: Context,
        phoneNumber: String,
        customerName: String?,
        shopName: String,
        shopkeeperName: String,
        productName: String,
        dueAmount: Double
    ) {
        var cleanPhone = phoneNumber.replace(Regex("[^0-9]"), "")
        if (cleanPhone.isBlank()) {
            Toast.makeText(context, "মোবাইল নম্বর পাওয়া যায়নি", Toast.LENGTH_SHORT).show()
            return
        }
        if (cleanPhone.startsWith("0")) {
            cleanPhone = "88$cleanPhone" // Bangladesh international prefix
        } else if (!cleanPhone.startsWith("88") && cleanPhone.length == 10) {
            cleanPhone = "880$cleanPhone"
        }

        val name = customerName?.takeIf { it.isNotBlank() } ?: "সম্মানিত গ্রাহক"
        val shop = shopName.ifBlank { "আমার দোকান" }
        val owner = shopkeeperName.takeIf { it.isNotBlank() }?.let { " ($it)" } ?: ""

        val message = """
            *আসসালামু আলাইকুম $name,*
            
            🏪 দোকান: *$shop$owner*
            📦 পণ্যের বিবরণ: *$productName*
            💰 *বকেয়া টাকার পরিমাণ:* *৳${BengaliFormatters.toBanglaNumber(dueAmount)}*
            
            আপনার হিসাবের অবশিষ্ট বকেয়া পরিশোধ করার জন্য বিনীতভাবে অনুরোধ করা হলো। 
            
            _ধন্যবাদান্তে,_
            *$shop*
        """.trimIndent()

        val encodedMsg = Uri.encode(message)
        val whatsappUri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMsg")

        try {
            val intent = Intent(Intent.ACTION_VIEW, whatsappUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val fallbackUri = Uri.parse("https://wa.me/$cleanPhone?text=$encodedMsg")
                val fallbackIntent = Intent(Intent.ACTION_VIEW, fallbackUri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
            } catch (e2: Exception) {
                Toast.makeText(context, "হোয়াটসঅ্যাপ (WhatsApp) অ্যাপ ওপেন করা যায়নি", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Send instant sale invoice / receipt via WhatsApp (Zero Cost)
     */
    fun sendWhatsAppSaleReceipt(
        context: Context,
        phoneNumber: String,
        customerName: String?,
        shopName: String,
        productName: String,
        quantity: Int,
        totalAmount: Double,
        paidAmount: Double,
        dueAmount: Double
    ) {
        var cleanPhone = phoneNumber.replace(Regex("[^0-9]"), "")
        if (cleanPhone.isBlank()) return
        if (cleanPhone.startsWith("0")) {
            cleanPhone = "88$cleanPhone"
        }

        val name = customerName?.takeIf { it.isNotBlank() } ?: "সম্মানিত গ্রাহক"
        val shop = shopName.ifBlank { "আমার দোকান" }

        val message = """
            🧾 *বিক্রয় রসিদ - $shop*
            👤 ক্রেতা: *$name*
            📦 পণ্য: *$productName*
            🔢 পরিমাণ: *${BengaliFormatters.toBanglaNumber(quantity)} টি*
            💵 মোট মূল্য: *৳${BengaliFormatters.toBanglaNumber(totalAmount)}*
            ✅ নগদ পরিশোধ: *৳${BengaliFormatters.toBanglaNumber(paidAmount)}*
            ${if (dueAmount > 0) "⚠️ *অবশিষ্ট বকেয়া:* *৳${BengaliFormatters.toBanglaNumber(dueAmount)}*" else "🎉 *পরিশোধ:* সম্পূর্ণ পরিশোধিত"}
            
            _আমাদের সাথে কেনাকাটা করার জন্য ধন্যবাদ!_
        """.trimIndent()

        val encodedMsg = Uri.encode(message)
        val whatsappUri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMsg")
        try {
            val intent = Intent(Intent.ACTION_VIEW, whatsappUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "হোয়াটসঅ্যাপ ওপেন করা যায়নি", Toast.LENGTH_SHORT).show()
        }
    }

    fun sendSmsReminder(
        context: Context,
        phoneNumber: String,
        customerName: String?,
        shopName: String,
        shopkeeperName: String,
        productName: String,
        dueAmount: Double
    ) {
        val cleanPhone = phoneNumber.replace(Regex("[^0-9+]"), "")
        if (cleanPhone.isBlank()) {
            Toast.makeText(context, "মোবাইল নম্বর পাওয়া যায়নি", Toast.LENGTH_SHORT).show()
            return
        }

        val name = customerName?.takeIf { it.isNotBlank() } ?: "গ্রাহক"
        val message = "আসসালামু আলাইকুম $name ভাই, আপনি $shopkeeperName ($shopName)-এর কাছ থেকে বাকিতে $productName নিয়েছিলেন। আপনার বকেয়া ৳${BengaliFormatters.toBanglaNumber(dueAmount)} আজ পরিশোধের কথা ছিল। অনুগ্রহ করে বকেয়া পরিশোধ করার জন্য অনুরোধ করা হলো। ধন্যবাদ।"

        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$cleanPhone")
                putExtra("sms_body", message)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val sendIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("sms:$cleanPhone")
                    putExtra("sms_body", message)
                }
                context.startActivity(sendIntent)
            } catch (e2: Exception) {
                Toast.makeText(context, "এসএমএস অ্যাপ চালু করা যায়নি", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun shareText(context: Context, title: String, content: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, content)
        }
        context.startActivity(Intent.createChooser(intent, "শেয়ার করুন"))
    }
}
