package com.example.roznamcha.ui.customer // Adjust package

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.example.roznamcha.AppDatabase
import com.example.roznamcha.data.CustomerRepository
import com.example.roznamcha.data.db.entity.Customer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
// <<< IMPORT Transaction if needed by other parts, but not strictly needed for THIS fix >>>
// import com.example.ketabat.data.db.entity.Transaction

class AddEditCustomerViewModel(
    application: Application,
    private val repository: CustomerRepository
) : AndroidViewModel(application) {

    private val TAG = "AddEditCustomerVM"

    private val _loadedCustomer = MutableLiveData<Customer?>()
    val loadedCustomer: LiveData<Customer?> = _loadedCustomer

    private var currentCustomerId: Long = -1L

    fun loadCustomer(id: Long) {
        Log.d(TAG, "loadCustomer called with ID: $id")
        if (id == -1L || id == 0L) {
            currentCustomerId = -1L
            _loadedCustomer.value = null
            Log.d(TAG, "loadCustomer: Resetting for new customer.")
            return
        }
        currentCustomerId = id
        viewModelScope.launch(Dispatchers.IO) {
            Log.d(TAG, "loadCustomer: Fetching customer $id from repository...")
            try {
                val customer = repository.getCustomerById(id).firstOrNull()
                Log.d(TAG, "loadCustomer: Fetched customer: ${customer?.name ?: "null"}")
                _loadedCustomer.postValue(customer)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading customer $id", e)
                _loadedCustomer.postValue(null)
            }
        }
    }

    fun saveCustomer(
        name: String,
        code: String?,
        type: String?,
        contactInfo: String?,
        isActive: Boolean
    ) {
        val customerIdToSave = if (currentCustomerId == -1L) 0 else currentCustomerId
        Log.d(TAG, "saveCustomer called. ID to save: $customerIdToSave, Name: $name")

        val customerToSave = Customer(
            id = customerIdToSave,
            name = name,
            code = code?.ifEmpty { null },
            type = type?.ifEmpty { null },
            contactInfo = contactInfo?.ifEmpty { null },
            isActive = isActive
        )

        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (customerIdToSave == 0L) {
                    repository.insert(customerToSave)
                    Log.i(TAG, "Inserted new customer: $name")
                } else {
                    repository.update(customerToSave)
                    Log.i(TAG, "Updated customer: $name (ID: $customerIdToSave)")
                }
                // TODO: Emit success event
            } catch (e: Exception) {
                Log.e(TAG, "Error saving customer $name", e)
                // TODO: Emit error event
            }
        }
    }

    fun deleteCustomer() {
        Log.d(TAG, "deleteCustomer called for ID: $currentCustomerId")
        if (currentCustomerId != -1L) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val customerToDeactivate = loadedCustomer.value?.copy(isActive = false)
                    if(customerToDeactivate != null) {
                        repository.update(customerToDeactivate) // Mark inactive
                        Log.i(TAG, "Marked customer inactive: ID $currentCustomerId")

                        // Reset state only after successful operation
                        withContext(Dispatchers.Main) {
                            // <<< FIX: Only reset customer-related state >>>
                            _loadedCustomer.value = null // Reset loaded customer LiveData
                            currentCustomerId = -1L     // Reset current ID tracking variable
                        }
                    } else {
                        Log.w(TAG, "Attempted to delete/deactivate but loadedCustomer was null")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error deleting/deactivating customer ID $currentCustomerId", e)
                    // TODO: Emit error event
                }
            }
        } else {
            Log.w(TAG, "Delete called but currentCustomerId is -1L")
        }
    }

    // <<< FIX: Removed the unrelated _loadedTransaction declaration >>>
    // private val _loadedTransaction = MutableLiveData<Transaction?>()

} // End of ViewModel Class

// --- ViewModel Factory ---
class AddEditCustomerViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddEditCustomerViewModel::class.java)) {
            Log.d("AddEditCustFactory", "Creating AddEditCustomerViewModel")
            val db = AppDatabase.getDatabase(application)
            val repo = CustomerRepository(db.customerDao())
            return AddEditCustomerViewModel(application, repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}