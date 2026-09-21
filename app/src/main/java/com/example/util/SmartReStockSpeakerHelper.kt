package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.widget.Toast
import com.example.data.local.ProductEntity
import com.example.data.local.ShopProfileEntity
import java.net.URLEncoder
import java.util.Locale

object SmartReStockSpeakerHelper {

    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false

    fun initTts(context: Context, onReady: (() -> Unit)? = null) {
        if (tts == null) {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val result = tts?.setLanguage(Locale("bn", "BD"))
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        tts?.setLanguage(Locale.US)
                    }
                    isTtsInitialized = true
                    onReady?.invoke()
                }
            }
        }
    }

    /**
     * Estimates wholesale rate based on product name and selling price.
     */
    fun estimateWholesalePrice(product: ProductEntity): Double {
        if (product.buyPrice > 0) return product.buyPrice
        // Standard retail margin 12-18% deduction
        val est = product.sellPrice * 0.85
        return (Math.round(est * 10.0) / 10.0)
    }

    /**
     * Speaks the smart restocking alert loudly using TextToSpeech.
     */
    fun speakRestockAlert(
        context: Context,
        product: ProductEntity,
        shopProfile: ShopProfileEntity?
    ) {
        val owner = shopProfile?.ownerName?.ifBlank { "দোকানদার ভাই" } ?: "দোকানদার ভাই"
        val wholesaleRate = estimateWholesalePrice(product)
        val speechText = "${owner}, আপনার দোকানে ${product.name} শেষ হওয়ার পথে! বর্তমান স্টক মাত্র ${BengaliFormatters.toBanglaNumber(product.stock)} ${product.unit}। পাইকারি বাজারে আজকে প্রতি ${product.unit}-এর দাম আনুমানিক ${BengaliFormatters.toBanglaNumber(wholesaleRate)} টাকা। এখনই পাইকারি অর্ডার পাঠান।"

        if (tts == null || !isTtsInitialized) {
            initTts(context) {
                tts?.speak(speechText, TextToSpeech.QUEUE_FLUSH, null, "RESTOCK_ALERT")
            }
        } else {
            tts?.speak(speechText, TextToSpeech.QUEUE_FLUSH, null, "RESTOCK_ALERT")
        }

        Toast.makeText(context, "🔊 ভয়েস অ্যালার্ট: ${product.name} রি-স্টক ঘোষণা হচ্ছে", Toast.LENGTH_SHORT).show()
    }

    fun stopSpeaking() {
        tts?.stop()
    }

    /**
     * Generates a structured Purchase Order slip and shares it to Wholesaler/Mohajon via WhatsApp.
     */
    fun shareWholesaleOrderViaWhatsApp(
        context: Context,
        product: ProductEntity,
        suggestedOrderQty: Double,
        shopProfile: ShopProfileEntity?,
        mohajonPhone: String? = null
    ) {
        val shopName = shopProfile?.shopName?.ifBlank { "আমার দোকান" } ?: "আমার দোকান"
        val ownerName = shopProfile?.ownerName?.ifBlank { "প্রোপ্রাইটর" } ?: "প্রোপ্রাইটর"
        val phone = shopProfile?.phone?.ifBlank { "" } ?: ""
        val wholesaleEst = estimateWholesalePrice(product)
        val totalEst = wholesaleEst * suggestedOrderQty

        val msg = """
        *🛒 পাইকারি মালামাল অর্ডারের রিকুইজিশন স্লিপ*
        ━━━━━━━━━━━━━━━━━
        🏪 *দোকানের নাম:* $shopName
        👤 *অর্ডারকারী:* $ownerName
        📞 *ফোন নম্বর:* $phone
        📅 *তারিখ:* ${BengaliFormatters.formatDateBangla(System.currentTimeMillis())}
        ━━━━━━━━━━━━━━━━━
        📦 *প্রয়োজনীয় মালামাল:*
        • *পণ্য:* ${product.name} (${product.category})
        • *অর্ডারের পরিমাণ:* ${BengaliFormatters.toBanglaNumber(suggestedOrderQty)} ${product.unit}
        • *আনুমানিক দর:* ৳${BengaliFormatters.toBanglaCurrency(wholesaleEst)} / ${product.unit}
        • *আনুমানিক মোট বিল:* ৳${BengaliFormatters.toBanglaCurrency(totalEst)}
        ━━━━━━━━━━━━━━━━━
        ⚠️ *অনুরোধ:* মালামাল দ্রুত ডেলিভারি করার জন্য অনুরোধ করা হলো।
        
        _ডিজিটাল হিসাব খাতা ও স্মার্ট রি-স্টক সিস্টেম থেকে প্রেরিত_
        """.trimIndent()

        try {
            val encodedMsg = URLEncoder.encode(msg, "UTF-8")
            val cleanPhone = mohajonPhone?.replace(Regex("[^0-9]"), "") ?: ""
            val uri = if (cleanPhone.isNotBlank()) {
                val fullPhone = if (cleanPhone.startsWith("88")) cleanPhone else "88$cleanPhone"
                Uri.parse("https://api.whatsapp.com/send?phone=$fullPhone&text=$encodedMsg")
            } else {
                Uri.parse("https://api.whatsapp.com/send?text=$encodedMsg")
            }

            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, msg)
            }
            context.startActivity(Intent.createChooser(shareIntent, "মহাজনের কাছে অর্ডার পাঠান"))
        }
    }
}
