package com.example.roznamcha.workers

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.roznamcha.MainActivity
import com.example.roznamcha.R
import com.example.roznamcha.TransactionCategory
import com.example.roznamcha.data.TransactionRepository
import com.example.roznamcha.AppDatabase // <<< Make sure this import is correct
import kotlinx.coroutines.flow.firstOrNull
import java.text.NumberFormat
import java.text.SimpleDateFormat // Add this if missing
import java.util.Date // Add this if missing
import java.util.*

class SummaryWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    // Unique ID for the notification channel
    private val CHANNEL_ID = "DAILY_SUMMARY_CHANNEL"
    private val TAG = "SummaryWorker-DEBUG" // Create a specific tag for easy filtering

    override suspend fun doWork(): Result {
        Log.d("SummaryWorker", "Worker starting...")
        try {
            // --- MOVED ALL LOGIC INSIDE THE SINGLE TRY BLOCK ---

            // 1. Get database and repository instances
            val database = AppDatabase.getDatabase(context)
            val repository = TransactionRepository(
                transactionDao = database.transactionDao(),
                inventoryItemDao = database.inventoryItemDao(),
                customerDao = database.customerDao(),
                context = context
            )


            // --- 1. Calculate and LOG the Date Range ---
            val calendar = Calendar.getInstance()
            val readableFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

            calendar.set(Calendar.HOUR_OF_DAY, 23); calendar.set(Calendar.MINUTE, 59); calendar.set(Calendar.SECOND, 59)
            val endOfToday = calendar.timeInMillis

            calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0)
            val startOfToday = calendar.timeInMillis

            Log.d(TAG, "--- Date Range Check ---")
            Log.d(TAG, "Start Time: ${readableFormat.format(Date(startOfToday))} ($startOfToday)")
            Log.d(TAG, "End Time:   ${readableFormat.format(Date(endOfToday))} ($endOfToday)")

            // --- 2. Fetch and LOG Data from Database ---
            Log.d(TAG, "--- Database Fetch ---")
            val salesToday = repository.getTotalForCategoryInRange(
                TransactionCategory.SALE.name, startOfToday, endOfToday
            ).firstOrNull() ?: 0.0
            Log.d(TAG, "Total Sales for Today = $salesToday")

            val purchasesToday = repository.getTotalForCategoryInRange(
                TransactionCategory.PURCHASE.name, startOfToday, endOfToday
            ).firstOrNull() ?: 0.0
            Log.d(TAG, "Total Purchases for Today = $purchasesToday")

            val expensesToday = repository.getOperationalExpensesInRange(
                startOfToday, endOfToday
            ).firstOrNull() ?: 0.0
            Log.d(TAG, "Total Operational Expenses (Rent, Other) for Today = $expensesToday")

            // --- 3. Check for activity to report ---
            if (salesToday == 0.0 && purchasesToday == 0.0 && expensesToday == 0.0) {
                Log.d(TAG, "CONCLUSION: No significant activity found. Skipping notification.")
                return Result.success()
            }

            // --- 4. Perform and LOG the Final Calculation ---
            val profitToday = salesToday - (purchasesToday + expensesToday)
            Log.d(TAG, "--- Final Calculation ---")
            Log.d(TAG, "Profit = (Sales) - (Purchases + Expenses)")
            Log.d(TAG, "Profit = ($salesToday) - ($purchasesToday + $expensesToday) = $profitToday")

            // --- 5. Build and Show Notification ---
            val nf = NumberFormat.getNumberInstance(Locale.US)
            val salesText = "فروشات: ${nf.format(salesToday)}"
            val profitText = "مفاد: ${nf.format(profitToday)}"
            val contentText = "$salesText | $profitText"

            Log.d(TAG, "CONCLUSION: Activity found! Showing notification with content: '$contentText'")

            createNotificationChannel()
            showNotification("روزنامچه: خلاصه امروز", contentText)

            Log.d(TAG, "---------- Worker finished (SUCCESS) ----------")
            return Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "---------- Worker failed with an exception ----------", e)
            return Result.failure()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Daily Summaries"
            val descriptionText = "Shows a summary of daily business activity"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(title: String, content: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_icon)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w("SummaryWorker", "Notification permission not granted. Cannot show notification.")
            return
        }

        NotificationManagerCompat.from(context).notify(1, builder.build())
    }
}