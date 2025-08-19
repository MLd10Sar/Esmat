package com.example.roznamcha.workers

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.os.bundleOf
import androidx.navigation.NavDeepLinkBuilder
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.roznamcha.R
import com.example.roznamcha.TransactionCategory
import com.example.roznamcha.data.TransactionRepository
import com.example.roznamcha.AppDatabase
import com.example.roznamcha.data.db.dao.CustomerDao
import com.example.roznamcha.data.db.dao.InventoryItemDao
import com.example.roznamcha.data.db.dao.TransactionDao
import java.util.Calendar

class ReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val CHANNEL_ID = "REMINDER_CHANNEL"
    private val NOTIFICATION_ID = 2 // Use a different ID from the summary worker

    override suspend fun doWork(): Result {
        Log.d("ReminderWorker", "Worker starting...")
        try {
            val database = AppDatabase.getDatabase(context)
            val repository = TransactionRepository(
                transactionDao = database.transactionDao(),
                inventoryItemDao = database.inventoryItemDao(),
                customerDao = database.customerDao(),
                context = context
            )

            // --- 1. Define "Overdue" ---
            // Let's consider anything older than 7 days as overdue
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -7)
            val sevenDaysAgoTimestamp = calendar.timeInMillis

            // --- 2. Fetch Overdue Counts from the Database ---
            val overdueReceivables = repository.getOverdueReceivablesCount(sevenDaysAgoTimestamp)
            val overdueDebts = repository.getOverdueDebtsCount(sevenDaysAgoTimestamp)

            Log.d("ReminderWorker", "Found $overdueReceivables overdue receivables and $overdueDebts overdue debts.")

            // --- 3. Build the Notification Message ---
            val notificationMessages = mutableListOf<String>()
            if (overdueReceivables > 0) {
                notificationMessages.add("شما $overdueReceivables طلب پرداخت نشده دارید")
            }
            if (overdueDebts > 0) {
                notificationMessages.add("شما $overdueDebts بدهی پرداخت نشده دارید")
            }

            // --- 4. Show Notification ONLY if there is something to report ---
            if (notificationMessages.isNotEmpty()) {
                val title = "یادآوری حسابات روزنامچه"
                val content = notificationMessages.joinToString(" و ") + " که بیشتر از ۷ روز از آنها گذشته است."

                // Decide which screen to open when the notification is tapped
                val destinationFragmentId = if (overdueReceivables > 0) {
                    R.id.transactionListFragment // Prioritize showing receivables
                } else {
                    R.id.transactionListFragment // Also fine for debts
                }
                val categoryArg = if (overdueReceivables > 0) {
                    TransactionCategory.RECEIVABLE.name
                } else {
                    TransactionCategory.DEBT.name
                }
                val pendingIntent = createDeepLink(destinationFragmentId, categoryArg)

                createNotificationChannel()
                showNotification(title, content, pendingIntent)
            } else {
                Log.d("ReminderWorker", "No overdue items found. Skipping notification.")
            }

            Log.d("ReminderWorker", "Worker finished successfully.")
            return Result.success()

        } catch (e: Exception) {
            Log.e("ReminderWorker", "Worker failed", e)
            return Result.failure()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Account Reminders"
            val descriptionText = "Reminds you about overdue debts and receivables"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // Helper to create a deep link that navigates to a specific fragment with arguments
    private fun createDeepLink(destination: Int, categoryArg: String): PendingIntent {
        val bundle = bundleOf("category" to categoryArg, "filterType" to "OVERDUE_ONLY")
        return NavDeepLinkBuilder(context)
            .setGraph(R.navigation.nav_graph)
            .setDestination(destination)
            .setArguments(bundle)
            .createPendingIntent()
    }

    private fun showNotification(title: String, content: String, pendingIntent: PendingIntent) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_icon)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content)) // Allows for longer text
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent) // This makes the notification clickable
            .setAutoCancel(true)

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Log.w("ReminderWorker", "Notification permission not granted.")
            return
        }
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
    }
}