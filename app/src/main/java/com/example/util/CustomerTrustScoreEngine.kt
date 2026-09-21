package com.example.util

import com.example.data.local.CommunityCreditRecordEntity
import com.example.data.local.MemoEntity
import com.example.data.local.SaleTransactionEntity
import java.security.MessageDigest

data class TrustScoreResult(
    val score: Int, // 0 to 100
    val ratingCategory: CreditRatingCategory,
    val titleBangla: String,
    val statusText: String,
    val isCommunityDefaulter: Boolean,
    val communityReportCount: Int,
    val communityWarningMessage: String?,
    val recommendedCreditLimit: Double,
    val positiveFactors: List<String>,
    val riskFactors: List<String>,
    val totalPaidHistorical: Double,
    val activeDueAmount: Double,
    val overdueCount: Int
)

enum class CreditRatingCategory {
    EXCELLENT, // 80 - 100 (Safe / Green)
    GOOD,      // 65 - 79 (Fair / Emerald)
    MODERATE,  // 45 - 64 (Warning / Amber)
    HIGH_RISK  // 0 - 44 (Defaulter / Red Alert)
}

object CustomerTrustScoreEngine {

    /**
     * Creates an anonymous SHA-256 hash from customer's phone number
     * to protect privacy in the decentralized community credit network.
     */
    fun hashPhoneNumber(phone: String): String {
        val cleanPhone = phone.replace(Regex("[^0-9]"), "")
        if (cleanPhone.isEmpty()) return ""
        val bytes = MessageDigest.getInstance("SHA-256").digest(cleanPhone.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }.take(16)
    }

    /**
     * AI-driven Credit Trust Score Calculation Engine.
     * Evaluates store payment behaviors, past memos, overdue delays, and anonymous community flags.
     */
    fun calculateScore(
        customerName: String,
        customerPhone: String?,
        localSales: List<SaleTransactionEntity>,
        localMemos: List<MemoEntity>,
        communityRecord: CommunityCreditRecordEntity? = null
    ): TrustScoreResult {
        var baseScore = 70.0
        val positives = mutableListOf<String>()
        val risks = mutableListOf<String>()

        val cleanPhone = customerPhone?.replace(Regex("[^0-9]"), "") ?: ""

        // Filter local transactions for this customer
        val matchedSales = localSales.filter { sale ->
            val matchByName = !sale.customerName.isNullOrBlank() && sale.customerName.trim().equals(customerName.trim(), ignoreCase = true)
            val matchByPhone = cleanPhone.isNotBlank() && sale.customerPhone?.replace(Regex("[^0-9]"), "") == cleanPhone
            matchByName || matchByPhone
        }

        val matchedMemos = localMemos.filter { memo ->
            val matchByName = memo.customerName.isNotBlank() && memo.customerName.trim().equals(customerName.trim(), ignoreCase = true)
            val matchByPhone = cleanPhone.isNotBlank() && memo.customerPhone?.replace(Regex("[^0-9]"), "") == cleanPhone
            matchByName || matchByPhone
        }

        var totalPaid = 0.0
        var activeDue = 0.0
        var overdueCount = 0
        val now = System.currentTimeMillis()

        // Analyze local sales
        matchedSales.forEach { sale ->
            totalPaid += sale.paidAmount
            if (sale.isDue && !sale.isPaid) {
                activeDue += sale.dueAmount
                if (sale.dueDate != null && sale.dueDate < now) {
                    overdueCount++
                }
            }
        }

        // Analyze local memos
        matchedMemos.forEach { memo ->
            totalPaid += memo.paidAmount
            if (memo.dueAmount > 0) {
                // If not tracked separately in sales
                if (matchedSales.none { it.customerPhone == memo.customerPhone && it.dueAmount == memo.dueAmount }) {
                    activeDue += memo.dueAmount
                }
            }
        }

        // 1. Positive Scoring: Historical Repayment Track Record
        if (totalPaid > 10000) {
            baseScore += 18.0
            positives.add("বড় অংকের লেনদেন ও সফলভাবে টাকা পরিশোধ করেছেন (+১৮)")
        } else if (totalPaid > 3000) {
            baseScore += 12.0
            positives.add("নিয়মিত পরিশোধের ভালো রেকর্ড রয়েছে (+১২)")
        } else if (totalPaid > 500) {
            baseScore += 6.0
            positives.add("সফল লেনদেনের অভিজ্ঞতা রয়েছে (+৬)")
        }

        // 2. Active Due Penalty
        if (activeDue > 5000) {
            baseScore -= 22.0
            risks.add("বর্তমানে বড় অংকের (৳${BengaliFormatters.toBanglaCurrency(activeDue)}) বাকি জমে আছে (-২২)")
        } else if (activeDue > 1500) {
            baseScore -= 12.0
            risks.add("বর্তমানে ৳${BengaliFormatters.toBanglaCurrency(activeDue)} টাকা বকেয়া রয়েছে (-১২)")
        } else if (activeDue == 0.0 && totalPaid > 0) {
            baseScore += 8.0
            positives.add("কোনো বকেয়া নেই, সব পরিশোধিত (+৮)")
        }

        // 3. Overdue Delay Penalty
        if (overdueCount > 2) {
            baseScore -= 28.0
            risks.add("একাধিকবার প্রতিশ্রুত তারিখে বাকি পরিশোধ করেননি (-২৮)")
        } else if (overdueCount == 1) {
            baseScore -= 14.0
            risks.add("প্রতিশ্রুত তারিখের মেয়াদ শেষ হয়ে গেছে (-১৪)")
        }

        // 4. Community Risk Alert (Decentralized & Anonymous)
        var isCommunityDefaulter = false
        var communityReportCount = 0
        var communityWarning: String? = null

        if (communityRecord != null && communityRecord.isDefaulter) {
            isCommunityDefaulter = true
            communityReportCount = communityRecord.defaultReportsCount
            val penalty = 35.0 + (communityReportCount * 8.0).coerceAtMost(30.0)
            baseScore -= penalty
            communityWarning = "সতর্কতা! এই কাস্টমার অন্য ${BengaliFormatters.toBanglaNumber(communityReportCount)}টি দোকানে বকেয়া বাকি পরিশোধ করেননি। (${communityRecord.anonymousNote})"
            risks.add("কমিউনিটি সতর্কতা: অন্য দোকানে বকেয়া ডিফল্টের রেকর্ড রয়েছে (-${penalty.toInt()})")
        }

        // Clamp final score 0 to 100
        val finalScore = baseScore.coerceIn(5.0, 99.0).toInt()

        val category = when {
            finalScore >= 80 -> CreditRatingCategory.EXCELLENT
            finalScore >= 65 -> CreditRatingCategory.GOOD
            finalScore >= 45 -> CreditRatingCategory.MODERATE
            else -> CreditRatingCategory.HIGH_RISK
        }

        val (titleBangla, statusText) = when (category) {
            CreditRatingCategory.EXCELLENT -> Pair("চমৎকার ট্রাস্ট স্কোর", "খুব নিরাপদ (Safe Customer)")
            CreditRatingCategory.GOOD -> Pair("ভালো ট্রাস্ট স্কোর", "নিরাপদ বাকি দেওয়া যায়")
            CreditRatingCategory.MODERATE -> Pair("মাঝারি ক্রেডিট স্কোর", "সতর্ক থাকুন / লিমিট কম রাখুন")
            CreditRatingCategory.HIGH_RISK -> Pair(
                if (isCommunityDefaulter) "🔴 কমিউনিটি রিস্ক অ্যালার্ট (ডিফল্টার)" else "উচ্চ ঝুঁকি (High Risk)",
                "ঝুঁকিপূর্ণ / বাকি দেওয়া নিষেধ"
            )
        }

        // Recommended Credit Limit calculation
        val creditLimit = when (category) {
            CreditRatingCategory.EXCELLENT -> (totalPaid * 0.4).coerceIn(3000.0, 20000.0)
            CreditRatingCategory.GOOD -> (totalPaid * 0.25).coerceIn(1500.0, 8000.0)
            CreditRatingCategory.MODERATE -> 1000.0
            CreditRatingCategory.HIGH_RISK -> 0.0
        }

        return TrustScoreResult(
            score = finalScore,
            ratingCategory = category,
            titleBangla = titleBangla,
            statusText = statusText,
            isCommunityDefaulter = isCommunityDefaulter,
            communityReportCount = communityReportCount,
            communityWarningMessage = communityWarning,
            recommendedCreditLimit = creditLimit,
            positiveFactors = positives,
            riskFactors = risks,
            totalPaidHistorical = totalPaid,
            activeDueAmount = activeDue,
            overdueCount = overdueCount
        )
    }
}
