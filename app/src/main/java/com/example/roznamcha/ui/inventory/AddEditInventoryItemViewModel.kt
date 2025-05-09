package com.example.roznamcha.ui.inventory

import android.app.Application
import androidx.lifecycle.*
import com.example.roznamcha.data.TransactionRepository
import com.example.roznamcha.data.db.entity.InventoryItem
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class AddEditInventoryItemViewModel(
    application: Application,
    private val repository: TransactionRepository
) : AndroidViewModel(application) {

    private val _loadedItem = MutableLiveData<InventoryItem?>()
    val loadedItem: LiveData<InventoryItem?> = _loadedItem

    /**
     * Loads a single item from the database for editing.
     */
    fun loadItem(id: Long) {
        if (id == -1L) return // This is a new item, no need to load.

        viewModelScope.launch {
            // Get the item from the repository.
            // This requires getInventoryItemById to exist in the repo/DAO.
            val item = repository.getInventoryItemById(id).firstOrNull()
            _loadedItem.postValue(item)
        }
    }

    /**
     * Saves an item (either inserts a new one or updates an existing one).
     */
    fun saveItem(item: InventoryItem) {
        viewModelScope.launch {
            if (item.id == 0L) {
                repository.insertInventoryItem(item)
            } else {
                repository.updateInventoryItem(item)
            }
        }
    }
}