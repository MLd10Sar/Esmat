package com.example.roznamcha.data.db.entity

/**
 * A simple data class (POJO) to hold the results of a GROUP BY query for expenses.
 * This is NOT an @Entity, as it doesn't represent its own table.
 */
data class CategoryTotal(
    val category: String,
    val totalAmount: Double
)

/**
 * A simple data class (POJO) to hold the results of a query for top selling items.
 */
data class ItemSaleTotal(
    val description: String,
    val totalQuantity: Double
)

/**
 * A simple data class (POJO) to hold the results of a query for top customers.
 */
data class CustomerSaleTotal(
    val customerId: Long?,
    val customerName: String?,
    val totalAmount: Double
)