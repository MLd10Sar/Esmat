package com.example.roznamcha.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    // You may want to add an index on customerId later for performance
    indices = [Index(value = ["category", "dateMillis"]), Index(value = ["customerId"])]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val originalAmount: Double?,
    var remainingAmount: Double?,
    val parentTransactionId: Long?,
    val category: String,
    val description: String,       // Item Description/Name or Person Name (if no customerId)
    val amount: Double?,
    val quantity: Double?,
    val unitPrice: Double?,
    val dateMillis: Long,
    val remarks: String?,
    val currency: String,
    val darak: String?,
    val billNumber: String?,
    val quantityUnit: String?,
    val paymentStatus: String?,     // <<< ADDED: e.g., "PAID", "DUE"
    val customerId: Long?, var isSettled: Boolean = false, //default to false, var so it can be updated.
    // this applies mainly to DEBT and RECEIVABLE categories
    val linkedInventoryItemId: Long? = null
)