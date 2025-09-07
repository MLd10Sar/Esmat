package com.example.roznamcha.utils

import android.content.Context
import com.example.roznamcha.SettingsManager
import saman.zamani.persiandate.PersianDate
import saman.zamani.persiandate.PersianDateFormat
import java.text.SimpleDateFormat
import java.util.*

object DateUtils {

    private val afghanShamsiMonths = arrayOf(
        "حمل", "ثور", "جوزا", "سرطان", "اسد", "سنبله",
        "میزان", "عقرب", "قوس", "جدی", "دلو", "حوت"
    )

    /**
     * The main function the app will call.
     * It checks the user's preference and formats the timestamp accordingly.
     */
    fun formatMillis(context: Context, millis: Long, format: String = "Y F d"): String {
        val preferredFormat = SettingsManager.getDateFormat(context)

        return if (preferredFormat == "SHAMSI") {
            try {
                // Use the new, reliable library for conversion
                val persianDate = PersianDate(millis)

                // "Y F d" -> "1403 حمل 5"
                // The library uses 'F' for the full month name.
                val formatter = PersianDateFormat(format, PersianDateFormat.PersianDateNumberCharacter.FARSI)

                // Manually replace the Iranian month name with the Afghan one
                formatter.format(persianDate)
                    .replace("فروردین", "حمل")
                    .replace("اردیبهشت", "ثور")
                    .replace("خرداد", "جوزا")
                    .replace("تیر", "سرطان")
                    .replace("مرداد", "اسد")
                    .replace("شهریور", "سنبله")
                    .replace("مهر", "میزان")
                    .replace("آبان", "عقرب")
                    .replace("آذر", "قوس")
                    .replace("دی", "جدی")
                    .replace("بهمن", "دلو")
                    .replace("اسفند", "حوت")

            } catch (e: Exception) {
                // Fallback to Gregorian if anything goes wrong
                SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date(millis))
            }
            convertToShamsi(millis)
        } else {
            // Gregorian
            SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date(millis))
            val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.US)
            dateFormat.format(Date(millis))
        }
    }
    private fun convertToShamsi(millis: Long): String {
        // This is a placeholder for your actual Shamsi conversion library/logic.
        // Let's create a simple one for demonstration.
        // You would replace this with your real library call.
        val calendar = Calendar.getInstance().apply { timeInMillis = millis }
        val year = calendar.get(Calendar.YEAR) - 621 // Approximate
        val month = calendar.get(Calendar.MONTH) + 1 // Not accurate, just for show
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        return "شمسی: $year/$month/$day" // Example output
    }
}
