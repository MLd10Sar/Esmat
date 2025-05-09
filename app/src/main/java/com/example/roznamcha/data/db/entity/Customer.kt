package com.example.roznamcha.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "customers",
    indices = [Index(value = ["name"], unique = true)] // Index on name for searching/sorting
)
data class Customer(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,      // نام کامل
    val code: String?,     // کد (Optional)
    val type: String?,     // نوعیت (e.g., "مشتری", "فروشنده" - Customer/Supplier) - Optional
    val contactInfo: String?, // معلومات تماس (e.g., phone number) - Optional
    val isActive: Boolean = true // Flag to deactivate instead of delete (optional)
)