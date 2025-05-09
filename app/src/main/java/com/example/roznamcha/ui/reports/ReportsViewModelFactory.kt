package com.example.roznamcha.ui.reports // Or your actual reports package

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.roznamcha.data.TransactionRepository
import com.example.roznamcha.AppDatabase

/**
 * Factory for creating a ReportsViewModel with a constructor that takes a TransactionRepository.
 */
class ReportsViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReportsViewModel::class.java)) {
            val database = AppDatabase.getDatabase(application)
            // <<< CORRECTED CONSTRUCTOR CALL >>>
            val repository = TransactionRepository(
                context = application.applicationContext,
                transactionDao = database.transactionDao(),
                inventoryItemDao = database.inventoryItemDao(),
                customerDao = database.customerDao()
            )

            @Suppress("UNCHECKED_CAST")
            return ReportsViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}