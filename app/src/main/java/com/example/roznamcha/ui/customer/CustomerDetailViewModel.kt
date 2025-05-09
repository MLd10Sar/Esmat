package com.example.roznamcha.ui.customer

import android.app.Application
import androidx.lifecycle.*
import com.example.roznamcha.data.TransactionRepository
import com.example.roznamcha.AppDatabase
import com.example.roznamcha.data.db.entity.Customer
import com.example.roznamcha.data.db.entity.Transaction

class CustomerDetailViewModel(
    application: Application,
    private val repository: TransactionRepository,
    private val customerId: Long
) : AndroidViewModel(application) {

    // LiveData for the customer's details (name, contact, etc.)
    val customerDetails: LiveData<Customer?> = repository.getCustomerById(customerId).asLiveData()

    // LiveData for the customer's full transaction history
    val transactionHistory: LiveData<List<Transaction>> = repository.getTransactionsForCustomer(customerId).asLiveData()

    // LiveData for the calculated current balance
    val currentBalance: LiveData<Double> = transactionHistory.map { transactions ->
        var balance = 0.0
        transactions.forEach { txn ->
            // Add to balance for DUE sales/receivables
            if ((txn.category == "SALE" || txn.category == "RECEIVABLE") && !txn.isSettled) {
                balance += (txn.remainingAmount ?: txn.amount)!!
            }
            // Subtract from balance for DUE purchases/debts
            else if ((txn.category == "PURCHASE" || txn.category == "DEBT") && !txn.isSettled) {
                balance -= (txn.remainingAmount ?: txn.amount)!!
            }
        }
        balance
    }
}

// Factory to pass the customerId to the ViewModel
class CustomerDetailViewModelFactory(
    private val application: Application,
    private val customerId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CustomerDetailViewModel::class.java)) {
            val database = AppDatabase.getDatabase(application)
            // <<< CORRECTED CONSTRUCTOR CALL >>>
            val repository = TransactionRepository(
                context = application.applicationContext,
                transactionDao = database.transactionDao(),
                inventoryItemDao = database.inventoryItemDao(),
                customerDao = database.customerDao()
            )
            @Suppress("UNCHECKED_CAST")
            return CustomerDetailViewModel(application, repository, customerId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}