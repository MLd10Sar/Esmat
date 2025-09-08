package com.example.roznamcha.utils

import android.content.Context
import com.example.roznamcha.SettingsManager
import saman.zamani.persiandate.PersianDate
import saman.zamani.persiandate.PersianDateFormat
import java.text.SimpleDateFormat
import java.util.*

object DateUtils {

    // A reusable formatter for the standard Gregorian date (e.g., 2025/09/07)
    private val gregorianFormatter = SimpleDateFormat("yyyy/MM/dd", Locale.US)

    // --- We will define the Afghan month names ourselves ---
    private val AFGHAN_MONTH_NAMES = arrayOf(
        "حمل", "ثور", "جوزا", "سرطان", "اسد", "سنبله",
        "میزان", "عقرب", "قوس", "جدی", "دلو", "حوت"
    )

    /**
     * The single, authoritative function to format a date timestamp throughout the app.
     * It checks the user's preference in SettingsManager and returns the correctly formatted date string.
     *
     * @param context The application context.
     * @param millis The timestamp in UTC milliseconds to format.
     * @return A formatted date string (e.g., "۱۶ سنبله ۱۴۰۴" or "2025/09/07").
     */
    fun formatMillis(context: Context, millis: Long): String {
        // 1. Check the user's saved preference
        val preferredFormat = SettingsManager.getDateFormat(context)

        return if (preferredFormat == "SHAMSI") {
            try {
                // 2. Create a PersianDate object from the timestamp
                val persianDate = PersianDate(millis)

                // 3. Manually build the date string using our custom month names
                val day = persianDate.shDay
                val monthName = AFGHAN_MONTH_NAMES[persianDate.shMonth - 1] // -1 because array is 0-indexed
                val year = persianDate.shYear

                // Use Persian numbers by converting them
                "$day".toFarsiNumber() + " " + monthName + " " + "$year".toFarsiNumber()

            } catch (e: Exception) {
                // If anything goes wrong, fall back to Gregorian to prevent a crash.
                gregorianFormatter.format(Date(millis))
            }
        } else {
            // 4. If Gregorian, just use the standard Gregorian formatter.
            gregorianFormatter.format(Date(millis))
        }
    }

    /**
     * An extension function to convert English digit strings to Persian digits.
     */
    private fun String.toFarsiNumber(): String {
        return this
            .replace("0", "۰")
            .replace("1", "۱")
            .replace("2", "۲")
            .replace("3", "۳")
            .replace("4", "۴")
            .replace("5", "۵")
            .replace("6", "۶")
            .replace("7", "۷")
            .replace("8", "۸")
            .replace("9", "۹")
    }
}