// In PaymentStatus.kt
package com.example.roznamcha

enum class PaymentStatus {
    PAID,       // Represents the status of being fully paid
    DUE, // Represents the status of being on credit (قرضه / باقی)
    PARTIAL
    // PARTIAL // Example for partially paid transactions
    // Add other relevant statuses here
}