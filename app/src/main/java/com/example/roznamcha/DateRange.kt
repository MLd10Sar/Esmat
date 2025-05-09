package com.example.roznamcha

import java.util.Calendar

/**
 * An enum to represent the different date range filters on the dashboard.
 * Includes a helper function to get the start and end timestamps for each range.
 */
enum class DateRange {
    TODAY,
    THIS_WEEK,
    THIS_MONTH,
    ALL_TIME;

    fun getTimestamps(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        return when (this) {
            TODAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis

                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val end = calendar.timeInMillis
                Pair(start, end)
            }
            THIS_WEEK -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis

                calendar.add(Calendar.WEEK_OF_YEAR, 1)
                calendar.add(Calendar.MILLISECOND, -1)
                val end = calendar.timeInMillis
                Pair(start, end)
            }
            THIS_MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis

                calendar.add(Calendar.MONTH, 1)
                calendar.add(Calendar.MILLISECOND, -1)
                val end = calendar.timeInMillis
                Pair(start, end)
            }
            ALL_TIME -> {
                Pair(0L, Long.MAX_VALUE) // A range that covers all possible dates
            }
        }
    }
}