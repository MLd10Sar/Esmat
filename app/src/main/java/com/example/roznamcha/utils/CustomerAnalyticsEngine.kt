package com.example.roznamcha.utils

import com.example.roznamcha.TransactionCategory
import com.example.roznamcha.data.db.entity.Transaction
import kotlin.math.roundToLong

object CustomerAnalyticsEngine {

    /**
     * Analyzes a customer's transaction history to predict their payment behavior.
     * @param history A list of all transactions for a single customer.
     * @return A String containing a helpful insight, or null if not enough data.
     */
    fun predictRepaymentHabit(history: List<Transaction>): String? {
        // Find all original debts/receivables that have been fully paid (settled)
        val settledOriginalDebts = history.filter { transaction ->
            transaction.isSettled && // <<< CORRECT PROPERTY NAME
                    (transaction.category == TransactionCategory.RECEIVABLE.name || // <<< CORRECT PROPERTY NAME
                            (transaction.category == TransactionCategory.SALE.name && transaction.originalAmount != null)) // <<< CORRECT PROPERTY NAME
        }

        // We need at least 2 settled debts to make a meaningful prediction
        if (settledOriginalDebts.size < 2) {
            return null // Not enough historical data to analyze
        }

        val paymentDurationsInDays = settledOriginalDebts.mapNotNull { settledDebt ->
            // For each settled debt, find the LAST payment made against it.
            // The last payment is what marks the transaction as fully settled.
            val finalPayment = history
                .filter { it.parentTransactionId == settledDebt.id } // <<< CORRECT PROPERTY NAME
                .maxByOrNull { it.dateMillis } // Find the latest payment transaction

            if (finalPayment != null) {
                // Calculate the duration in milliseconds
                val durationMillis = finalPayment.dateMillis - settledDebt.dateMillis // <<< CORRECT PROPERTY NAME
                // Convert milliseconds to days
                durationMillis / 86_400_000.0 // (1000 * 60 * 60 * 24)
            } else {
                null
            }
        }

        if (paymentDurationsInDays.isEmpty()) {
            return null
        }

        // <<< FIX for Ambiguity: The list is of type Double, so average() is now clear >>>
        val averageDays = paymentDurationsInDays.average()
        val roundedAverage = averageDays.roundToLong()

        // Create a simple, human-readable insight based on the average
        return when {
            roundedAverage <= 1L -> "معمولاً در جریان ۱ روز پرداخت میکند."
            roundedAverage <= 7L -> "معمولاً در ظرف ۱ هفته پرداخت میکند."
            roundedAverage <= 25L -> {
                val start = (roundedAverage - 3).coerceAtLeast(1) // Ensure start is not negative
                val end = roundedAverage + 3
                "معمولاً بعد از $start الی $end روز پرداخت میکند."
            }
            roundedAverage <= 45L -> {
                val weeks = (roundedAverage / 7).toInt().coerceAtLeast(1)
                "معمولاً در حدود $weeks هفته بعد پرداخت میکند."
            }
            else -> "معمولاً بیشتر از یک ماه بعد پرداخت میکند."
        }
    }
}