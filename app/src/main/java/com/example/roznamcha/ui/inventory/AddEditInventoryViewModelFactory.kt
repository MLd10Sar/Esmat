package com.example.roznamcha.ui.inventory

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.roznamcha.data.TransactionRepository
import com.example.roznamcha.AppDatabase

class AddEditInventoryViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddEditInventoryItemViewModel::class.java)) {
            val database = AppDatabase.getDatabase(application)
            // <<< CORRECTED CONSTRUCTOR CALL >>>
            val repository = TransactionRepository(
                context = application.applicationContext,
                transactionDao = database.transactionDao(),
                inventoryItemDao = database.inventoryItemDao(),
                customerDao = database.customerDao()
            )
            @Suppress("UNCHECKED_CAST")
            return AddEditInventoryItemViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}