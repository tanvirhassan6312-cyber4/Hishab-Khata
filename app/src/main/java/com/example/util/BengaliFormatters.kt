package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object BengaliFormatters {

    private val banglaDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')

    fun toBanglaDigits(str: String): String {
        val sb = StringBuilder()
        for (char in str) {
            if (char in '0'..'9') {
                sb.append(banglaDigits[char - '0'])
            } else {
                sb.append(char)
            }
        }
        return sb.toString()
    }

    fun toBanglaNumber(number: Number): String {
        val str = if (number is Double || number is Float) {
            if (number.toDouble() % 1.0 == 0.0) {
                number.toLong().toString()
            } else {
                String.format(Locale.US, "%.2f", number.toDouble())
            }
        } else {
            number.toString()
        }
        return toBanglaDigits(str)
    }

    fun toBanglaCurrency(amount: Double): String {
        return "৳ ${toBanglaNumber(amount)}"
    }

    fun formatDateBangla(timestamp: Long): String {
        val date = Date(timestamp)
        val dayFormat = SimpleDateFormat("dd", Locale.US)
        val monthFormat = SimpleDateFormat("MM", Locale.US)
        val yearFormat = SimpleDateFormat("yyyy", Locale.US)

        val day = toBanglaNumber(dayFormat.format(date).toInt())
        val monthNum = monthFormat.format(date).toInt()
        val year = toBanglaNumber(yearFormat.format(date).toInt())

        val months = arrayOf(
            "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
            "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
        )
        val monthName = months.getOrElse(monthNum - 1) { "" }
        return "$day $monthName $year"
    }

    fun formatTimeBangla(timestamp: Long): String {
        val date = Date(timestamp)
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)
        val rawTime = timeFormat.format(date)
        val parts = rawTime.split(" ")
        val timeDigits = toBanglaDigits(parts[0])
        val period = if (parts.size > 1 && parts[1].equals("PM", ignoreCase = true)) "বিকাল/রাত" else "সকাল"
        return "$period $timeDigits"
    }

    fun formatDateTimeBangla(timestamp: Long): String {
        return "${formatDateBangla(timestamp)} (${formatTimeBangla(timestamp)})"
    }

    fun isToday(timestamp: Long): Boolean {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply { timeInMillis = timestamp }
        return now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
    }

    fun isOverdue(timestamp: Long): Boolean {
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return timestamp < todayStart
    }
}
