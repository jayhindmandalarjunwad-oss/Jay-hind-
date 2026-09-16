package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    private val marathiMonthMap = mapOf(
        "जानेवारी" to 0, "january" to 0, "jan" to 0, "01" to 0, "1" to 0,
        "फेब्रुवारी" to 1, "february" to 1, "feb" to 1, "02" to 1, "2" to 1,
        "मार्च" to 2, "march" to 2, "mar" to 2, "03" to 2, "3" to 2,
        "एप्रिल" to 3, "april" to 3, "apr" to 3, "04" to 3, "4" to 3,
        "मे" to 4, "may" to 4, "05" to 4, "5" to 4,
        "जून" to 5, "june" to 5, "jun" to 5, "06" to 5, "6" to 5,
        "जुलै" to 6, "july" to 6, "jul" to 6, "07" to 6, "7" to 6,
        "ऑगस्ट" to 7, "august" to 7, "aug" to 7, "08" to 7, "8" to 7,
        "सप्टेंबर" to 8, "september" to 8, "sept" to 8, "sep" to 8, "09" to 8, "9" to 8,
        "ऑक्टोबर" to 9, "october" to 9, "oct" to 9, "10" to 9,
        "नोव्हेंबर" to 10, "november" to 10, "nov" to 10, "11" to 10,
        "डिसेंबर" to 11, "december" to 11, "dec" to 11, "12" to 11
    )

    /**
     * Parses dates like "6 सप्टेंबर 2026", "10 सप्टेंबर 2026", "15 सप्टेंबर 2026",
     * "2026-09-06", "06/09/2026", "6/9/2026" into epoch millis timestamp
     * for accurate chronological ascending sorting (earliest upcoming date first).
     */
    fun parseEventDateToTimestamp(dateStr: String): Long {
        if (dateStr.isBlank()) return Long.MAX_VALUE
        val clean = dateStr.trim()

        try {
            // 1. Check for standard ISO format "yyyy-MM-dd"
            if (clean.matches(Regex("""\d{4}-\d{1,2}-\d{1,2}"""))) {
                val parts = clean.split("-")
                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, parts[0].toInt())
                    set(Calendar.MONTH, parts[1].toInt() - 1)
                    set(Calendar.DAY_OF_MONTH, parts[2].toInt())
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                return cal.timeInMillis
            }

            // 2. Check for slash or dash format "dd/MM/yyyy" or "dd-MM-yyyy"
            val slashMatch = Regex("""^(\d{1,2})[/\-](\d{1,2})[/\-](\d{4})$""").find(clean)
            if (slashMatch != null) {
                val day = slashMatch.groupValues[1].toInt()
                val month = slashMatch.groupValues[2].toInt() - 1
                val year = slashMatch.groupValues[3].toInt()
                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, day)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                return cal.timeInMillis
            }

            // 3. Check for natural Marathi / English text format "6 सप्टेंबर 2026" or "15 August 2026"
            val textMatch = Regex("""(\d{1,2})\s+([^\s\d]+)\s+(\d{4})""").find(clean)
            if (textMatch != null) {
                val day = textMatch.groupValues[1].toInt()
                val monthWord = textMatch.groupValues[2].lowercase(Locale.ROOT)
                val year = textMatch.groupValues[3].toInt()

                val monthIndex = marathiMonthMap[monthWord]
                    ?: marathiMonthMap.entries.firstOrNull { monthWord.contains(it.key) }?.value
                    ?: 0

                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, monthIndex)
                    set(Calendar.DAY_OF_MONTH, day)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                return cal.timeInMillis
            }
        } catch (_: Exception) {}

        return Long.MAX_VALUE
    }

    /**
     * Formats a timestamp into a WhatsApp-style date header string:
     * - "आज" (Today)
     * - "काल" (Yesterday)
     * - "१६ सप्टेंबर २०२६" or "16 Sep 2026" (Other days)
     */
    fun formatChatDateHeader(timestamp: Long): String {
        if (timestamp <= 0L) return ""
        val messageCal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val nowCal = Calendar.getInstance()

        // Same day -> "आज"
        val isToday = messageCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                messageCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)
        if (isToday) return "आज"

        // Yesterday
        val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val isYesterday = messageCal.get(Calendar.YEAR) == yesterdayCal.get(Calendar.YEAR) &&
                messageCal.get(Calendar.DAY_OF_YEAR) == yesterdayCal.get(Calendar.DAY_OF_YEAR)
        if (isYesterday) return "काल"

        // Check if same year
        val marathiMonths = arrayOf(
            "जानेवारी", "फेब्रुवारी", "मार्च", "एप्रिल", "मे", "जून",
            "जुलै", "ऑगस्ट", "सप्टेंबर", "ऑक्टोबर", "नोव्हेंबर", "डिसेंबर"
        )
        val day = messageCal.get(Calendar.DAY_OF_MONTH)
        val month = messageCal.get(Calendar.MONTH)
        val year = messageCal.get(Calendar.YEAR)
        val currentYear = nowCal.get(Calendar.YEAR)

        val monthName = if (month in marathiMonths.indices) marathiMonths[month] else ""

        return if (year == currentYear) {
            "$day $monthName"
        } else {
            "$day $monthName $year"
        }
    }

    /**
     * Returns a day key (e.g. "yyyy-DDD") to group or check date transitions
     */
    fun getDayKey(timestamp: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        return "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
    }

    /**
     * Returns smart time for chat summary lists (like WhatsApp):
     * - "10:30 AM" if today
     * - "काल" if yesterday
     * - "15/09/26" if older
     */
    fun formatChatListTime(timestamp: Long): String {
        if (timestamp <= 0L) return ""
        val messageCal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val nowCal = Calendar.getInstance()

        val isToday = messageCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                messageCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)
        if (isToday) {
            return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestamp))
        }

        val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val isYesterday = messageCal.get(Calendar.YEAR) == yesterdayCal.get(Calendar.YEAR) &&
                messageCal.get(Calendar.DAY_OF_YEAR) == yesterdayCal.get(Calendar.DAY_OF_YEAR)
        if (isYesterday) return "काल"

        return SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(timestamp))
    }
}
