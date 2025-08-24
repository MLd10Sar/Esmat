package com.example.roznamcha.utils

import android.content.Context
import android.util.Log
import com.example.roznamcha.SettingsManager
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

object DateUtils {

    private val afghanShamsiMonths = arrayOf(
        "حمل", "ثور", "جوزا", "سرطان", "اسد", "سنبله",
        "میزان", "عقرب", "قوس", "جدی", "دلو", "حوت"
    )

    /**
     * The main function the app will call.
     * It checks the user's preference and formats the timestamp accordingly.
     */
    fun formatMillis(context: Context, millis: Long): String {
        return try {
            val preferredFormat = SettingsManager.getDateFormat(context)
            if (preferredFormat == "SHAMSI") {
                formatMillisToAfghanShamsi(millis)
            } else {
                formatMillisToGregorian(millis)
            }
        } catch (e: Exception) {
            Log.e("DateUtils", "Error formatting date, falling back to Gregorian.", e)
            formatMillisToGregorian(millis) // Safe fallback
        }
    }

    private fun formatMillisToGregorian(millis: Long): String {
        val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.US)
        return dateFormat.format(Date(millis))
    }

    private fun formatMillisToAfghanShamsi(millis: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = millis }
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1 // Month is 0-11
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // Use the robust conversion logic
        val jalaliResult = gregorianToJalali(year, month, day)
        val jYear = jalaliResult["year"]
        val jMonth = jalaliResult["month"]
        val jDay = jalaliResult["day"]

        if (jYear == null || jMonth == null || jDay == null || jMonth < 1 || jMonth > 12) {
            return formatMillisToGregorian(millis) // Fallback on error
        }

        // Use our custom Afghan month names
        val monthName = afghanShamsiMonths[jMonth - 1]

        return "$jDay $monthName $jYear"
    }

    // --- ROBUST GREGORIAN TO JALALI CONVERSION ALGORITHM ---
    // This version is more mathematically sound and less prone to edge-case errors.
    private fun gregorianToJalali(gYear: Int, gMonth: Int, gDay: Int): HashMap<String, Int> {
        val gDaysInMonth = intArrayOf(0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        val jDaysInMonth = intArrayOf(0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)

        var jYear: Int
        val jMonth: Int
        val jDay: Int

        val gy = gYear - 1600
        val gm = gMonth - 1
        val gd = gDay - 1

        var gDayNo = 365 * gy + Math.floor((gy + 3) / 4.0).toInt() - Math.floor((gy + 99) / 100.0).toInt() + Math.floor((gy + 399) / 400.0).toInt()
        for (i in 0 until gm) {
            gDayNo += gDaysInMonth[i+1]
        }
        if (gm > 1 && ((gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0))) {
            gDayNo++
        }
        gDayNo += gd

        var jDayNo = gDayNo - 79

        val jNp = Math.floor(jDayNo / 12053.0).toInt()
        jDayNo %= 12053

        jYear = 979 + 33 * jNp + 4 * (jDayNo / 1461)
        jDayNo %= 1461

        if (jDayNo >= 366) {
            jYear += Math.floor((jDayNo - 1) / 365.0).toInt()
            jDayNo = (jDayNo - 1) % 365
        }

        var i = 0
        while (i < 11 && jDayNo >= jDaysInMonth[i+1]) {
            jDayNo -= jDaysInMonth[i+1]
            i++
        }
        jMonth = i + 1
        jDay = jDayNo + 1

        val result = HashMap<String, Int>()
        result["year"] = jYear
        result["month"] = jMonth
        result["day"] = jDay
        return result
    }
}