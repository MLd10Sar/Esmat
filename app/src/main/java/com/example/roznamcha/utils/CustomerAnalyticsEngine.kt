package com.example.roznamcha.utils

import com.example.roznamcha.TransactionCategory
import com.example.roznamcha.data.db.entity.Transaction
import java.util.concurrent.TimeUnit
import kotlin.math.roundToLong

object CustomerAnalyticsEngine {

    fun predictRepaymentHabit(history: List<Transaction>): String? {
        // This function is fine as is
        val settledPayments = history.filter { it.parentTransactionId != null && it.isSettled }
        if (settledPayments.size < 2) return null
        val paymentDurationsInDays = settledPayments.mapNotNull { payment ->
            val originalDebt = history.find { it.id == payment.parentTransactionId }
            originalDebt?.let {
                val durationMillis = payment.dateMillis - it.dateMillis
                if (durationMillis >= 0) durationMillis / 86_400_000.0 else null
            }
        }
        if (paymentDurationsInDays.isEmpty()) return null
        val averageDays = paymentDurationsInDays.average()
        val roundedAverage = averageDays.roundToLong()
        return when {
            roundedAverage <= 1L -> "معمولاً در جریان ۱ روز پرداخت میکند."
            roundedAverage <= 7L -> "معمولاً در ظرف ۱ هفته پرداخت میکند."
            else -> "معمولاً بعد از ${roundedAverage} روز پرداخت میکند."
        }
    }

    fun predictNextPurchase(history: List<Transaction>): String? {
        // This function is fine as is
        val sales = history.filter { it.category == TransactionCategory.SALE.name && it.description.isNotBlank() }
        if (sales.size < 3) return null
        val mostFrequentItem = sales.groupBy { it.description.lowercase() }.maxByOrNull { it.value.size }?.key
        return mostFrequentItem?.let { "معمولاً «${it.replaceFirstChar { char -> char.uppercase() }}» خریداری میکند." }
    }

    fun assignCustomerValueTag(history: List<Transaction>): String? {
        val sales = history.filter { it.category == TransactionCategory.SALE.name }
        if (sales.isEmpty()) return "مشتری جدید"

        // Fix 1: Handle nullable amounts before summing
        val totalSalesAmount = sales.map { it.amount ?: 0.0 }.sum()

        val daysSinceLastSale = sales.maxOfOrNull { it.dateMillis }?.let {
            TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - it)
        } ?: Long.MAX_VALUE

        val HIGH_VALUE_THRESHOLD = 50000.0
        // Fix 2: Explicitly use Longs for comparison
        val RECENT_DAYS_THRESHOLD = 30L
        val REGULAR_CUSTOMER_DAYS_THRESHOLD = 60L

        return when {
            totalSalesAmount > HIGH_VALUE_THRESHOLD && daysSinceLastSale <= RECENT_DAYS_THRESHOLD -> "مشتری ارزشمند"
            sales.size >= 5 && daysSinceLastSale <= REGULAR_CUSTOMER_DAYS_THRESHOLD -> "مشتری عادی"
            else -> "مشتری جدید"
        }
    }
}