package com.example.roznamcha.utils

import com.example.roznamcha.PaymentStatus
import com.example.roznamcha.TransactionCategory

/**
 * Checks if a given category name string matches the PURCHASE category.
 * @param categoryName The category name string from the database.
 * @return True if the category is PURCHASE, false otherwise.
 */
fun isPurchaseCategory(categoryName: String?): Boolean {
    return categoryName == TransactionCategory.PURCHASE.name
}

/**
 * Checks if a given payment status string matches the PAID status.
 * @param statusName The payment status string from the database.
 * @return True if the status is PAID, false otherwise.
 */
fun isPaidStatus(statusName: String?): Boolean {
    return statusName == PaymentStatus.PAID.name
}