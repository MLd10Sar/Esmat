package com.example.roznamcha.ui.list

import com.example.roznamcha.data.db.entity.Transaction

/**
 * A sealed class to represent the different types of items in our transaction list.
 * The list can contain either an actual transaction or a date header.
 */
sealed class TransactionListItem {
    data class TransactionItem(val transaction: Transaction) : TransactionListItem()
    data class DateHeader(val date: String) : TransactionListItem()
}