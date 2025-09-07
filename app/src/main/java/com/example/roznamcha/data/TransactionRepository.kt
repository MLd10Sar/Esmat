package com.example.roznamcha.data

import android.content.Context
import com.example.roznamcha.SettingsManager
import com.example.roznamcha.data.db.dao.CustomerDao
import com.example.roznamcha.data.db.dao.InventoryItemDao
import com.example.roznamcha.data.db.dao.TransactionDao
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
    fun searchTransactionsByCategory(categoryName: String, query: String, includeSettlements: Boolean): Flow<List<Transaction>> {
        val queryPattern = "%${query}%"
        return transactionDao.searchTransactionsByCategory(categoryName, queryPattern, includeSettlements)
    }
    fun getUnsettledReceivablesList(query: String): Flow<List<Transaction>> {
        val queryPattern = "%${query}%"
        return transactionDao.getUnsettledReceivablesList(queryPattern)
    }

    fun getDirectPaidSalesTotalForRange(startDate: Int, endDate: Int): Flow<Double?> {
        return transactionDao.getDirectPaidSalesTotalForRange(startDate, endDate)
    }
    fun getDirectPaidSalesTotal(): Flow<Double?> {
        return transactionDao.getDirectPaidSalesTotal()
    }
    // --- NEW REPO METHODS for COGS ---
    fun getTotalCostOfGoodsSold(): Flow<Double?> {
        return transactionDao.getTotalCostOfGoodsSold()
    }

    fun getCostOfGoodsSoldForRange(startDate: Long, endDate: Long): Flow<Double?> {
        return transactionDao.getCostOfGoodsSoldForRange(startDate, endDate)
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

    // --- NEW METHODS for Smart Reminders ---
    suspend fun getOverdueReceivablesCount(overdueTimestamp: Long): Int {
        return transactionDao.getOverdueReceivablesCount(overdueTimestamp)
    }

    suspend fun getOverdueDebtsCount(overdueTimestamp: Long): Int {
        return transactionDao.getOverdueDebtsCount(overdueTimestamp)
    }
    // <<< ADD THIS MISSING FUNCTION >>>
    fun getTotalSalesForCustomer(customerId: Long): Flow<Double?> {
        // You'll need to add the corresponding query to your TransactionDao.kt
        return transactionDao.getTotalSalesForCustomer(customerId)
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

    fun getTransactionHistoryForCustomer(customerId: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionHistoryForCustomer(customerId)
    }

}