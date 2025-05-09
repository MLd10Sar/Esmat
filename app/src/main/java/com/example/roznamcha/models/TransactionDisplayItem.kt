package com.example.roznamcha.models

import com.example.roznamcha.data.db.entity.Transaction

/**
 * A wrapper data class to hold a Transaction and any pre-fetched, related data
 * needed for display, like the customer's name. This simplifies the adapter's logic.
 */
data class TransactionDisplayItem(
    val transaction: Transaction,
    val customerName: String? // The pre-fetched name of the customer
)