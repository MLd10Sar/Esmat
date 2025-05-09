package com.example.roznamcha.ui.inventory

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.roznamcha.data.TransactionRepository
import com.example.roznamcha.data.db.entity.InventoryItem
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map // <<< ADD THIS IMPORT
import kotlinx.coroutines.launch

class InventoryListViewModel(
    application: Application,
    private val repository: TransactionRepository
) : AndroidViewModel(application) {

    // Exposes the main list of all inventory items
    val allInventoryItems: LiveData<List<InventoryItem>> = repository.getAllInventoryItems()
        .catch {
            // 'emit' is available here within the 'catch' operator's lambda.
            emit(emptyList())
        }
        .asLiveData()

    // --- LiveData for the new Quick Stats feature ---
    val totalItemCount: LiveData<Double> = repository.getTotalItemCount()
        .map { it ?: 0.0 } // 'it' refers to the value emitted by the Flow
        .asLiveData()

    val totalInventoryValue: LiveData<Double> = repository.getTotalInventoryValue()
        .map { it ?: 0.0 } // 'it' refers to the value emitted by the Flow
        .asLiveData()

    // --- ViewModel Methods ---

    // Note: Saving and editing items is handled by AddEditInventoryItemViewModel.
    // This ViewModel is only for the LIST screen.

    fun deleteItem(item: InventoryItem) {
        viewModelScope.launch {
            repository.deleteInventoryItem(item)
        }
    }
}