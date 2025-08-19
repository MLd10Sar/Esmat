package com.example.roznamcha.ui.customer

import android.app.Application
import androidx.lifecycle.*
import com.example.roznamcha.AppDatabase
import com.example.roznamcha.TransactionCategory
import com.example.roznamcha.data.TransactionRepository
import com.example.roznamcha.data.db.entity.Customer
import com.example.roznamcha.data.db.entity.Transaction
import com.example.roznamcha.ui.list.TransactionListItem // <<< IMPORT
import com.example.roznamcha.utils.CustomerAnalyticsEngine
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*

class CustomerDetailViewModel(
    application: Application,
    private val repository: TransactionRepository,
    private val customerId: Long
) : AndroidViewModel(application) {

    // --- Primary Data Sources ---
    val customer: LiveData<Customer?> = repository.getCustomerById(customerId).asLiveData()
    val currentBalance: LiveData<Double> = repository.getOutstandingBalanceForCustomer(customerId)
        .map { it ?: 0.0 }.asLiveData()
    val totalSalesToCustomer: LiveData<Double> = repository.getTotalSalesForCustomer(customerId)
        .map { it ?: 0.0 }.asLiveData()

    // Fetches the raw, sorted list of transactions for this customer
    private val rawTransactionHistory: LiveData<List<Transaction>> =
        repository.getTransactionsForCustomer(customerId).asLiveData()

    // --- LiveData for the UI ---

    // <<< FIX 1: This LiveData transforms the raw list into a list with headers for the adapter >>>
    val transactionHistoryWithHeaders: LiveData<List<TransactionListItem>> = rawTransactionHistory.map { transactions ->
        groupTransactionsByDate(transactions)
    }

    // <<< FIX 2: This LiveData correctly uses the RAW list for analysis >>>
    val repaymentInsight: LiveData<String?> = rawTransactionHistory.map { transactions ->
        // The analytics engine gets the simple List<Transaction> it expects
        CustomerAnalyticsEngine.predictRepaymentHabit(transactions)
    }


    // --- Helper Functions for Grouping (can be moved to a Util if shared) ---
    private fun groupTransactionsByDate(transactions: List<Transaction>): List<TransactionListItem> {
        if (transactions.isEmpty()) return emptyList()

        val groupedList = mutableListOf<TransactionListItem>()
        val farsiLocale = Locale("fa", "AF")
        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        var lastHeader = ""

        transactions.forEach { transaction ->
            val transactionCal = Calendar.getInstance().apply { timeInMillis = transaction.dateMillis }
            val dateString = when {
                isSameDay(transactionCal, today) -> "امروز"
                isSameDay(transactionCal, yesterday) -> "دیروز"
                else -> SimpleDateFormat("d MMMM yyyy", farsiLocale).format(Date(transaction.dateMillis))
            }
            if (dateString != lastHeader) {
                groupedList.add(TransactionListItem.DateHeader(dateString))
                lastHeader = dateString
            }
            groupedList.add(TransactionListItem.TransactionItem(transaction))
        }
        return groupedList
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}

// --- Factory to pass the customerId to the ViewModel ---
// This part is already correct and does not need to change.
class CustomerDetailViewModelFactory(
    private val application: Application,
    private val customerId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CustomerDetailViewModel::class.java)) {
            val database = AppDatabase.getDatabase(application)
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