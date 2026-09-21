package com.example.util

import com.example.data.local.MemoItem
import com.example.data.local.ProductEntity
import java.util.Calendar

enum class VoiceTransactionType {
    DUE_SALE,   // বাকি বিক্রি
    CASH_SALE,  // নগদ বিক্রি
    PAYMENT_IN  // বকেয়া পরিশোধ / জমা
}

data class ParsedVoiceEntry(
    val rawSpokenText: String,
    val transactionType: VoiceTransactionType,
    val customerName: String,
    val customerPhone: String?,
    val items: List<MemoItem>,
    val totalAmount: Double,
    val paidAmount: Double,
    val dueAmount: Double,
    val promisedDueDateMillis: Long?,
    val notes: String
)

object VoiceToLedgerParser {

    /**
     * AI-based Bengali NLP Parser for voice transactions.
     */
    fun parseBengaliVoiceText(
        spokenText: String,
        existingProducts: List<ProductEntity>
    ): ParsedVoiceEntry {
        val text = spokenText.trim()
        val lower = text.lowercase()

        // 1. Detect Transaction Type
        val isPayment = lower.contains("জমা") || lower.contains("পরিশোধ") || lower.contains("দিলো") || lower.contains("দিলেন") || lower.contains("উসুল")
        val isDue = lower.contains("বাকি") || lower.contains("বাকিতে") || lower.contains("পরে দিবে") || lower.contains("দিব বলছে") || lower.contains("ধার")
        
        val type = when {
            isPayment && !lower.contains("বাকিতে") -> VoiceTransactionType.PAYMENT_IN
            isDue -> VoiceTransactionType.DUE_SALE
            else -> VoiceTransactionType.CASH_SALE
        }

        // 2. Extract Customer Name
        var customerName = extractCustomerName(text)
        if (customerName.isBlank()) {
            customerName = if (type == VoiceTransactionType.CASH_SALE) "নগদ ক্রেতা" else "সাধারণ গ্রাহক"
        }

        // 3. Extract Promised Due Date
        val promisedDate = extractDueDate(text)

        // 4. Extract Items, Quantities and Amounts
        val items = mutableListOf<MemoItem>()
        var totalAmount = 0.0

        // Parse explicit amounts from text (e.g. ৫০০ টাকা, ৫০ টাকার, 200)
        val amountRegex = Regex("([০-৯0-9]+)\\s*(?:টাকা|টাকার|টাকায়|tk)")
        val amountMatches = amountRegex.findAll(text).toList()

        // Product matching from catalog
        val matchedProducts = existingProducts.filter { prod ->
            text.contains(prod.name, ignoreCase = true)
        }

        if (matchedProducts.isNotEmpty()) {
            matchedProducts.forEachIndexed { idx, prod ->
                val price = prod.sellPrice
                val qty = extractQuantityForProduct(text, prod.name) ?: 1.0
                val itemTotal = price * qty
                items.add(
                    MemoItem(
                        sl = items.size + 1,
                        itemName = prod.name,
                        quantity = qty,
                        unit = prod.unit,
                        unitPrice = price,
                        total = itemTotal
                    )
                )
                totalAmount += itemTotal
            }
        } else {
            // General item parsing if not in catalog
            if (amountMatches.isNotEmpty()) {
                amountMatches.forEachIndexed { i, match ->
                    val amt = BengaliFormatters.fromBanglaNumber(match.groupValues[1]) ?: 0.0
                    if (amt > 0) {
                        val itemName = extractItemNameNearAmount(text, match.range.first) ?: "পণ্য ${i + 1}"
                        items.add(
                            MemoItem(
                                sl = items.size + 1,
                                itemName = itemName,
                                quantity = 1.0,
                                unit = "টি",
                                unitPrice = amt,
                                total = amt
                            )
                        )
                        totalAmount += amt
                    }
                }
            } else {
                // Fallback number search
                val numbers = Regex("[০-৯0-9]+").findAll(text).mapNotNull {
                    BengaliFormatters.fromBanglaNumber(it.value)
                }.filter { it > 0 }.toList()

                if (numbers.isNotEmpty()) {
                    val amt = numbers.first()
                    items.add(
                        MemoItem(
                            sl = items.size + 1,
                            itemName = "মালামাল / পণ্য",
                            quantity = 1.0,
                            unit = "টি",
                            unitPrice = amt,
                            total = amt
                        )
                    )
                    totalAmount = amt
                } else {
                    totalAmount = 100.0
                    items.add(
                        MemoItem(
                            sl = items.size + 1,
                            itemName = "মালামাল",
                            quantity = 1.0,
                            unit = "টি",
                            unitPrice = 100.0,
                            total = 100.0
                        )
                    )
                }
            }
        }

        val paidAmount = when (type) {
            VoiceTransactionType.CASH_SALE -> totalAmount
            VoiceTransactionType.PAYMENT_IN -> totalAmount
            VoiceTransactionType.DUE_SALE -> 0.0
        }

        val dueAmount = when (type) {
            VoiceTransactionType.DUE_SALE -> totalAmount
            else -> 0.0
        }

        return ParsedVoiceEntry(
            rawSpokenText = text,
            transactionType = type,
            customerName = customerName,
            customerPhone = null,
            items = items,
            totalAmount = totalAmount,
            paidAmount = paidAmount,
            dueAmount = dueAmount,
            promisedDueDateMillis = promisedDate,
            notes = "ভয়েস এন্ট্রি: $text"
        )
    }

    private fun extractCustomerName(text: String): String {
        val honorifics = listOf("ভাইকে", "ভাই", "চাচাকে", "চাচা", "কাকাকে", "কাকা", "মামা", "মামাকে", "সাহেব", "সাহেবকে", "দা", "দাকে")
        val words = text.split(" ", "।", ",", "\n")
        
        for (i in words.indices) {
            val word = words[i]
            for (h in honorifics) {
                if (word.endsWith(h)) {
                    val namePart = word.removeSuffix(h)
                    return if (namePart.isNotBlank()) "$namePart ${h.removeSuffix("কে")}" else word
                }
                if (word == h && i > 0) {
                    return "${words[i - 1]} $h"
                }
            }
        }

        // Check common first word as name if followed by amount
        if (words.isNotEmpty() && words[0].length in 3..12 && !words[0].contains("টাকা") && !words[0].contains("বাকি")) {
            return words[0]
        }

        return ""
    }

    private fun extractDueDate(text: String): Long? {
        val cal = Calendar.getInstance()
        val lower = text.lowercase()

        return when {
            lower.contains("কালকে") || lower.contains("আগামীকাল") -> {
                cal.add(Calendar.DAY_OF_YEAR, 1)
                cal.timeInMillis
            }
            lower.contains("দুইদিন") || lower.contains("২ দিন") -> {
                cal.add(Calendar.DAY_OF_YEAR, 2)
                cal.timeInMillis
            }
            lower.contains("তিনদিন") || lower.contains("৩ দিন") -> {
                cal.add(Calendar.DAY_OF_YEAR, 3)
                cal.timeInMillis
            }
            lower.contains("সপ্তাহ") || lower.contains("৭ দিন") -> {
                cal.add(Calendar.DAY_OF_YEAR, 7)
                cal.timeInMillis
            }
            lower.contains("মাস") || lower.contains("৩০ দিন") -> {
                cal.add(Calendar.DAY_OF_YEAR, 30)
                cal.timeInMillis
            }
            lower.contains("শুক্রবার") -> {
                cal.add(Calendar.DAY_OF_YEAR, 5)
                cal.timeInMillis
            }
            else -> null
        }
    }

    private fun extractQuantityForProduct(text: String, productName: String): Double? {
        val regex = Regex("([০-৯0-9]+)\\s*(?:কেজি|লিটার|প্যাকেট|পিস|বক্স|ডজন)")
        val match = regex.find(text)
        if (match != null) {
            return BengaliFormatters.fromBanglaNumber(match.groupValues[1])
        }
        return null
    }

    private fun extractItemNameNearAmount(text: String, amountIndex: Int): String? {
        val before = text.substring(0, amountIndex).trim()
        val words = before.split(" ")
        val lastWords = words.takeLast(2).filter { it.length > 1 }
        val commonItems = listOf("চাল", "ডাল", "তেল", "চিনি", "আটা", "লবণ", "বিস্কুট", "সাবান", "চা পাতা", "মসলা", "ডিম", "দুধ")
        for (item in commonItems) {
            if (before.contains(item)) return item
        }
        return lastWords.lastOrNull()
    }
}
