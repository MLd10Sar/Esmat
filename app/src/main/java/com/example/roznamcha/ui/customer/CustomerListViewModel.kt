package com.example.roznamcha.ui.customer

import android.app.Application
import androidx.lifecycle.*
import com.example.roznamcha.AppDatabase
import com.example.roznamcha.data.TransactionRepository // <<< USE THE MAIN REPOSITORY
import com.example.roznamcha.data.db.entity.Customer
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class CustomerListViewModel(
    application: Application,
    // The ViewModel should depend on the single, main TransactionRepository
    private val repository: TransactionRepository
) : AndroidViewModel(application) {

    // Exposes the main list of all active customers
    val allCustomers: LiveData<List<Customer>> = repository.getAllActiveCustomers()
        .catch { emit(emptyList()) }
        .asLiveData()

    // --- LiveData for the new Quick Stats feature ---
    // These are now correctly placed INSIDE the class
    val customerCount: LiveData<Int> = repository.getActiveCustomerCount()
        .asLiveData()

    val totalOutstandingBalance: LiveData<Double> = repository.getTotalOutstandingReceivables()
        .map { it ?: 0.0 }
        .asLiveData()

    // --- ViewModel Methods ---
    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch {
            // It's better to deactivate than delete if they have transactions.
            // For now, let's just update to be inactive.
            val inactiveCustomer = customer.copy(isActive = false)
            repository.updateCustomer(inactiveCustomer) // <<< Add this method to Repo/DAO
        }
    }
}

// --- CORRECTED ViewModel Factory ---
class CustomerListViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CustomerListViewModel::class.java)) {
            val database = AppDatabase.getDatabase(application)
            // <<< CORRECTED CONSTRUCTOR CALL >>>
            val repository = TransactionRepository(
                context = application.applicationContext,
                transactionDao = database.transactionDao(),
                inventoryItemDao = database.inventoryItemDao(),
                customerDao = database.customerDao()
            )
            @Suppress("UNCHECKED_CAST")
            return CustomerListViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}