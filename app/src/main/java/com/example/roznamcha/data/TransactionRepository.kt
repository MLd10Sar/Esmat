package com.example.roznamcha.data

import android.content.Context
import com.example.roznamcha.SettingsManager
import com.example.roznamcha.data.db.dao.CustomerDao
import com.example.roznamcha.data.db.dao.InventoryItemDao
import com.example.roznamcha.data.db.dao.TransactionDao
// <<< CORRECTED IMPORTS: Point to the 'entity' package for data classes >>>
import com.example.roznamcha.data.db.entity.*
import kotlinx.coroutines.flow.Flow

//class TrialLimitReachedException : Exception("The trial transaction limit has been reached.")

class TransactionRepository(
    private val context: Context,
    private val transactionDao: TransactionDao,
    private val inventoryItemDao: InventoryItemDao,
    private val customerDao: CustomerDao
) {

    // --- Transaction Methods ---
    suspend fun insert(transaction: Transaction) {
        if (!SettingsManager.isAccessGranted(context) &&
            SettingsManager.getTransactionCount(context) >= SettingsManager.TRIAL_LIMIT) {
            throw TrialLimitReachedException()
        }
        transactionDao.insert(transaction)
        if (!SettingsManager.isAccessGranted(context)) {
            SettingsManager.incrementTransactionCount(context)
        }
    }

    suspend fun update(transaction: Transaction) = transactionDao.update(transaction)
    suspend fun deleteById(id: Long) = transactionDao.deleteById(id)
    suspend fun updateCustomer(customer: Customer) = customerDao.update(customer)
    suspend fun updateInventoryItem(item: InventoryItem) = inventoryItemDao.update(item)

    // --- Transaction Retrieval & Search ---
    fun getTransactionById(id: Long): Flow<Transaction?> = transactionDao.getTransactionById(id)
    fun getTransactionsByCategory(categoryName: String): Flow<List<Transaction>> = transactionDao.getTransactionsByCategory(categoryName)
    fun searchTransactionsByCategoryAndDescription(categoryName: String, query: String): Flow<List<Transaction>> {
        val queryPattern = "%${query}%"
        return transactionDao.searchTransactionsByCategoryAndDescription(categoryName, queryPattern)
    }
    fun getUnsettledReceivablesList(query: String): Flow<List<Transaction>> {
        val queryPattern = "%${query}%"
        return transactionDao.getUnsettledReceivablesList(queryPattern)
    }
    fun getUnsettledDebtsList(query: String): Flow<List<Transaction>> {
        val queryPattern = "%${query}%"
        return transactionDao.getUnsettledDebtsList(queryPattern)
    }
    fun getTransactionsForCustomer(customerId: Long): Flow<List<Transaction>> = transactionDao.getTransactionsForCustomer(customerId)

    // --- Calculation & Total Methods ---
    fun getTotalAmountByCategory(categoryName: String): Flow<Double?> = transactionDao.getTotalAmountByCategory(categoryName)
    fun getTotalAmountByCategoryAndStatus(categoryName: String, paymentStatus: String): Flow<Double?> = transactionDao.getTotalAmountByCategoryAndStatus(categoryName, paymentStatus)
    fun getUnsettledTotalForCategory(categoryName: String): Flow<Double?> = transactionDao.getUnsettledTotalForCategory(categoryName)
    fun getTotalOutstandingReceivables(): Flow<Double?> = transactionDao.getTotalOutstandingReceivables()

    fun getUnsettledDebtsTotal(): Flow<Double?> {
        return transactionDao.getUnsettledDebtsTotal()
    }

    fun getUnsettledReceivablesTotal(): Flow<Double?> {
        return transactionDao.getUnsettledReceivablesTotal()
    }

    fun getUnsettledDebtsTotalForRange(startDate: Long, endDate: Long): Flow<Double?> {
        return transactionDao.getUnsettledDebtsTotalForRange(startDate, endDate)
    }
    // --- Date Range Methods ---
    fun getTotalForCategoryInRange(categoryName: String, startDate: Long, endDate: Long): Flow<Double?> = transactionDao.getTotalAmountByCategoryForRange(categoryName, startDate, endDate)
    fun getTotalAmountByCategoryAndStatusForRange(categoryName: String, paymentStatus: String, startDate: Long, endDate: Long): Flow<Double?> = transactionDao.getTotalAmountByCategoryAndStatusForRange(categoryName, paymentStatus, startDate, endDate)
    fun getUnsettledTotalForCategoryForRange(categoryName: String, startDate: Long, endDate: Long): Flow<Double?> = transactionDao.getUnsettledTotalForCategoryForRange(categoryName, startDate, endDate)
    fun getOperationalExpensesInRange(startDate: Long, endDate: Long): Flow<Double?> = transactionDao.getOperationalExpensesInRange(startDate, endDate)
    fun getSumOfExpensesForRange(expenseCategories: List<String>, start: Long, end: Long): Flow<Double?> = transactionDao.getSumOfExpensesForRange(expenseCategories, start, end)
    fun getTotalOutstandingReceivablesForRange(startDate: Long, endDate: Long): Flow<Double?> = transactionDao.getTotalOutstandingReceivablesForRange(startDate, endDate)

    // --- Reports Methods ---
    fun getExpenseTotalsGroupedByCategory(expenseCategories: List<String>, start: Long, end: Long): Flow<List<CategoryTotal>> = transactionDao.getExpenseTotalsGroupedByCategory(expenseCategories, start, end)
    fun getTopSellingItemsByQuantity(start: Long, end: Long): Flow<List<ItemSaleTotal>> = transactionDao.getTopSellingItemsByQuantity(start, end)
    fun getTopCustomersBySale(start: Long, end: Long): Flow<List<CustomerSaleTotal>> = transactionDao.getTopCustomersBySale(start, end)

    // --- Inventory Item Methods ---
    fun getAllInventoryItems(): Flow<List<InventoryItem>> = inventoryItemDao.getAllItems()
    suspend fun insertInventoryItem(item: InventoryItem) = inventoryItemDao.insert(item)
    suspend fun deleteInventoryItem(item: InventoryItem) = inventoryItemDao.delete(item)
    suspend fun processStockUpdate(itemId: Long, quantityChange: Double) = inventoryItemDao.updateStock(itemId, quantityChange)
    fun getInventoryItemById(id: Long): Flow<InventoryItem?> = inventoryItemDao.getItemById(id)
    fun getTotalItemCount(): Flow<Double?> = inventoryItemDao.getTotalItemCount()
    fun getTotalInventoryValue(): Flow<Double?> = inventoryItemDao.getTotalInventoryValue()

    // --- Customer Methods ---
    fun getAllActiveCustomers(): Flow<List<Customer>> = customerDao.getAllActiveCustomers()
    fun getActiveCustomerCount(): Flow<Int> = customerDao.getActiveCustomerCount()
    fun getCustomerById(customerId: Long): Flow<Customer?> = customerDao.getCustomerById(customerId)

    // --- Customer Snapshot Methods ---
    fun getLastTransactionDateForCustomer(customerId: Long): Flow<Long?> = transactionDao.getLastTransactionDateForCustomer(customerId)
    fun getOutstandingBalanceForCustomer(customerId: Long): Flow<Double?> = transactionDao.getOutstandingBalanceForCustomer(customerId)
}