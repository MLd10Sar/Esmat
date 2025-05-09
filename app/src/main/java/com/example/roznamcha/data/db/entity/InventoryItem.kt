package com.example.roznamcha.data.db.entity // <<< CORRECT PACKAGE

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inventory_items")
data class InventoryItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val unit: String?,
    var quantity: Double,
    val purchasePrice: Double? = null,
    val salePrice: Double? = null,
    val remarks: String? = null
)