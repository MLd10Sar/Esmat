package com.example.roznamcha.ui.inventory

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.roznamcha.data.TransactionRepository
import com.example.roznamcha.AppDatabase

class AddEditInventoryItemViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddEditInventoryItemViewModel::class.java)) {
            // Get database instance
            val database = AppDatabase.getDatabase(application)
            // Create the repository with all its dependencies
            val repository = TransactionRepository(
                context = application.applicationContext,
                transactionDao = database.transactionDao(),
                inventoryItemDao = database.inventoryItemDao(),
                customerDao = database.customerDao()
            )
            // Create and return the ViewModel
            @Suppress("UNCHECKED_CAST")
            return AddEditInventoryItemViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}