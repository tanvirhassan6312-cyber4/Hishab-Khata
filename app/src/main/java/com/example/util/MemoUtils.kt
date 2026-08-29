package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.data.local.MemoEntity
import com.example.data.local.MemoItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random

object MemoUtils {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val listType = Types.newParameterizedType(List::class.java, MemoItem::class.java)
    private val adapter = moshi.adapter<List<MemoItem>>(listType)

    fun serializeItems(items: List<MemoItem>): String {
        return try {
            adapter.toJson(items) ?: "[]"
        } catch (e: Exception) {
            "[]"
        }
    }

    fun deserializeItems(json: String): List<MemoItem> {
        return try {
            if (json.isBlank()) emptyList() else adapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun generateMemoNumber(): String {
        val dateFormat = SimpleDateFormat("yyMMdd", Locale.US)
        val dateStr = dateFormat.format(Date())
        val randomSuffix = (1000..9999).random()
        return "M-$dateStr-$randomSuffix"
    }

    fun numberToBanglaWords(amount: Double): String {
        val num = amount.toLong()
        if (num <= 0) return "শূন্য টাকা মাত্র"

        val units = arrayOf(
            "", "এক", "দুই", "তিন", "চার", "পাঁচ", "ছয়", "সাত", "আট", "নয়",
            "দশ", "এগারো", "বারো", "তেরো", "চৌদ্দ", "পনেরো", "ষোল", "সতেরো", "আঠারো", "উনিশ"
        )
        val tens = arrayOf(
            "", "", "বিশ", "ত্রিশ", "চল্লিশ", "পঞ্চাশ", "ষাট", "সত্তর", "আশি", "নব্বই"
        )

        fun convertUnder100(n: Int): String {
            return when {
                n < 20 -> units[n]
                n % 10 == 0 -> tens[n / 10]
                else -> "${tens[n / 10]} ${units[n % 10]}"
            }
        }

        fun convertUnder1000(n: Int): String {
            val hundred = n / 100
            val rest = n % 100
            val hStr = if (hundred > 0) "${units[hundred]} শত" else ""
            val rStr = if (rest > 0) convertUnder100(rest) else ""
            return listOf(hStr, rStr).filter { it.isNotBlank() }.joinToString(" ")
        }

        var remaining = num
        val crore = (remaining / 10000000).toInt()
        remaining %= 10000000
        val lakh = (remaining / 100000).toInt()
        remaining %= 100000
        val thousand = (remaining / 1000).toInt()
        remaining %= 1000
        val hundredAndRest = remaining.toInt()

        val parts = mutableListOf<String>()
        if (crore > 0) parts.add("${convertUnder100(crore)} কোটি")
        if (lakh > 0) parts.add("${convertUnder100(lakh)} লক্ষ")
        if (thousand > 0) parts.add("${convertUnder100(thousand)} হাজার")
        if (hundredAndRest > 0) parts.add(convertUnder1000(hundredAndRest))

        val result = parts.joinToString(" ").trim()
        return if (result.isBlank()) "শূন্য টাকা মাত্র" else "$result টাকা মাত্র"
    }

    fun buildBengaliMemoMessage(memo: MemoEntity, items: List<MemoItem>): String {
        val sb = StringBuilder()
        sb.append("🧾 *${memo.shopName}*\n")
        if (memo.shopAddress.isNotBlank()) {
            sb.append("📍 ঠিকানা: ${memo.shopAddress}\n")
        }
        if (memo.shopPhone.isNotBlank()) {
            sb.append("📞 মোবাইল: ${memo.shopPhone}\n")
        }
        sb.append("─────────────────────\n")
        sb.append("📋 *${memo.memoType.uppercase()}*\n")
        sb.append("🔢 মেমো নং: *${memo.memoNumber}*\n")
        sb.append("📅 তারিখ: ${BengaliFormatters.formatDateTimeBangla(memo.date)}\n")
        if (memo.preparedBy.isNotBlank()) {
            sb.append("👤 প্রস্তুতকারক: ${memo.preparedBy}\n")
        }
        sb.append("─────────────────────\n")
        sb.append("👤 *গ্রাহক / প্রাপকের তথ্য:*\n")
        sb.append("নাম: *${memo.customerName}*\n")
        if (!memo.customerPhone.isNullOrBlank()) {
            sb.append("ফোন: ${memo.customerPhone}\n")
        }
        if (!memo.customerAddress.isNullOrBlank()) {
            sb.append("ঠিকানা: ${memo.customerAddress}\n")
        }
        sb.append("─────────────────────\n")
        sb.append("📦 *পণ্য ও সেবার বিবরণ:*\n")

        items.forEachIndexed { index, item ->
            val banglaSl = BengaliFormatters.toBanglaNumber(index + 1)
            val banglaQty = BengaliFormatters.toBanglaNumber(item.quantity)
            val banglaRate = BengaliFormatters.toBanglaCurrency(item.unitPrice)
            val banglaTotal = BengaliFormatters.toBanglaCurrency(item.total)
            sb.append("$banglaSl. *${item.itemName}*\n")
            sb.append("   $banglaQty ${item.unit} × $banglaRate = *$banglaTotal*\n")
        }

        sb.append("─────────────────────\n")
        sb.append("💰 উপমোট (Subtotal): ${BengaliFormatters.toBanglaCurrency(memo.subtotal)}\n")

        if (memo.discountAmount > 0) {
            sb.append("🏷️ ছাড় / ডিসকাউন্ট: -${BengaliFormatters.toBanglaCurrency(memo.discountAmount)}\n")
        }
        if (memo.vatAmount > 0) {
            sb.append("🏛️ ভ্যাট/ট্যাক্স (${BengaliFormatters.toBanglaNumber(memo.vatPercent)}%): +${BengaliFormatters.toBanglaCurrency(memo.vatAmount)}\n")
        }

        sb.append("💵 *সর্বমোট বিল: ${BengaliFormatters.toBanglaCurrency(memo.grandTotal)}*\n")
        sb.append("💳 পরিশোধিত: *${BengaliFormatters.toBanglaCurrency(memo.paidAmount)}* (${memo.paymentMethod})\n")

        if (memo.dueAmount > 0) {
            sb.append("⚠️ *অবশিষ্ট বাকি: ${BengaliFormatters.toBanglaCurrency(memo.dueAmount)}*\n")
        } else {
            sb.append("✅ *পরিশোধের অবস্থা: সম্পূর্ণ পরিশোধিত*\n")
        }

        sb.append("🗣️ কথায়: ${numberToBanglaWords(memo.grandTotal)}\n")

        if (!memo.notes.isNullOrBlank()) {
            sb.append("📝 নোট: ${memo.notes}\n")
        }
        if (!memo.terms.isNullOrBlank()) {
            sb.append("📌 শর্তাবলী: ${memo.terms}\n")
        } else {
            sb.append("📌 শর্তাবলী: বিক্রিত মাল ফেরত নেওয়া হয় না।\n")
        }

        sb.append("─────────────────────\n")
        sb.append("✨ আমাদের সাথে কেনাকাটা করার জন্য ধন্যবাদ!")
        return sb.toString()
    }

    fun shareViaWhatsApp(context: Context, memo: MemoEntity, items: List<MemoItem>) {
        try {
            val message = buildBengaliMemoMessage(memo, items)
            val encodedMessage = URLEncoder.encode(message, "UTF-8")
            val phone = memo.customerPhone?.filter { it.isDigit() } ?: ""
            val formattedPhone = when {
                phone.startsWith("880") -> phone
                phone.startsWith("01") -> "88$phone"
                phone.length == 10 && phone.startsWith("1") -> "880$phone"
                else -> phone
            }

            val uri = if (formattedPhone.isNotBlank()) {
                Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone&text=$encodedMessage")
            } else {
                Uri.parse("https://api.whatsapp.com/send?text=$encodedMessage")
            }

            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to standard share
            shareAsText(context, memo, items)
        }
    }

    fun shareAsText(context: Context, memo: MemoEntity, items: List<MemoItem>) {
        try {
            val message = buildBengaliMemoMessage(memo, items)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "${memo.shopName} - মেমো নং: ${memo.memoNumber}")
                putExtra(Intent.EXTRA_TEXT, message)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(Intent.createChooser(intent, "মেমো শেয়ার করুন"))
        } catch (e: Exception) {
            Toast.makeText(context, "মেমো শেয়ার করা সম্ভব হয়নি", Toast.LENGTH_SHORT).show()
        }
    }
}
