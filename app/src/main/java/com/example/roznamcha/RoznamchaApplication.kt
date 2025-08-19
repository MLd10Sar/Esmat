package com.example.roznamcha

import android.app.Application
import androidx.work.*
import com.example.roznamcha.workers.SummaryWorker
import java.util.concurrent.TimeUnit
import com.example.roznamcha.workers.ReminderWorker

class RoznamchaApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Schedule the daily summary worker
        setupDailySummaryWorker()
        setupDailyReminderWorker() // <<< CALL THE NEW FUNCTION
    }

    private fun setupDailySummaryWorker() {
        // Define constraints (e.g., only run when network is available, optional)
        val constraints = Constraints.Builder()
            .build()

        // Create a periodic request to run once a day
        val dailySummaryRequest = PeriodicWorkRequestBuilder<SummaryWorker>(1, TimeUnit.DAYS)
            .setConstraints(constraints)
            .build()


        // Enqueue the work. Use UNIQUE work to prevent scheduling duplicates.
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "DailySummaryWork", // A unique name for this job
            ExistingPeriodicWorkPolicy.KEEP, // Keep the existing job if one is already scheduled
            dailySummaryRequest
        )
    }
    private fun setupDailyReminderWorker() {
        // Create a periodic request to run once a day
        val dailyReminderRequest = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            // You can add constraints, e.g., to run only when the device is charging
            // .setConstraints(Constraints.Builder().setRequiresCharging(true).build())
            .build()

        // Enqueue the unique work
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "DailyReminderWork", // A unique name for THIS job
            ExistingPeriodicWorkPolicy.KEEP, // Keep the existing job if one is already scheduled
            dailyReminderRequest
        )
    }
}