package com.example.roznamcha.data.db.dao

import androidx.room.*
// <<< ADD/VERIFY THESE IMPORTS >>>
import com.example.roznamcha.data.db.entity.CategoryTotal
import com.example.roznamcha.data.db.entity.CustomerSaleTotal
import com.example.roznamcha.data.db.entity.ItemSaleTotal
import com.example.roznamcha.data.db.entity.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    // --- Basic CRUD ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: Transaction)

    @Update
    suspend fun update(transaction: Transaction)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    // --- Single Item Retrieval ---
    @Query("SELECT * FROM transactions WHERE id = :id")
    fun getTransactionById(id: Long): Flow<Transaction?>
    //our new update for AI
    @Query("SELECT SUM(amount) FROM transactions WHERE customerId = :customerId AND category = 'SALE'")
    fun getTotalSalesForCustomer(customerId: Long): Flow<Double?>

    // --- List Screen Queries ---
    //@Query("SELECT * FROM transactions WHERE category = :categoryName ORDER BY dateMillis DESC")
    //fun getTransactionsByCategory(categoryName: String): Flow<List<Transaction>>

    // <<< REPLACE the old searchTransactionsByCategory with this more powerful version >>>
    @Query("""
        SELECT * FROM transactions
        WHERE category = :categoryName
        AND description LIKE :queryPattern
        AND (:includeSettlements = 1 OR parentTransactionId IS NULL)
        ORDER BY dateMillis DESC
    """)
    fun searchTransactionsByCategory(categoryName: String, queryPattern: String, includeSettlements: Boolean): Flow<List<Transaction>>

    // <<< CORRECTED & FINAL QUERY for Dashboard Debt Total >>>
    @Query("""
        SELECT SUM(remainingAmount) FROM transactions
        WHERE isSettled = 0 AND (category = 'DEBT' OR (category = 'PURCHASE' AND paymentStatus IN ('DUE', 'PARTIAL')))
    """)
    fun getUnsettledDebtsTotal(): Flow<Double?>


    // <<< ADD THIS QUERY for total value of all unsettled receivables >>>
    // <<< CORRECTED & FINAL QUERY for Dashboard Receivable Total >>>
    @Query("""
        SELECT SUM(remainingAmount) FROM transactions
        WHERE isSettled = 0 AND (category = 'SALE' OR (category = 'RECEIVABLE' AND isSettled = 0))
    """)
    fun getUnsettledReceivablesTotal(): Flow<Double?>

    @Query("""
    SELECT SUM(remainingAmount) FROM transactions
    WHERE isSettled = 0 AND (category = 'DEBT' OR (category = 'PURCHASE' AND paymentStatus IN ('DUE', 'PARTIAL')))
    AND dateMillis BETWEEN :startDate AND :endDate
""")
    fun getUnsettledDebtsTotalForRange(startDate: Long, endDate: Long): Flow<Double?>

    @Query("""
        SELECT * FROM transactions WHERE
        ( (category = 'SALE' AND paymentStatus IN ('DUE', 'PARTIAL')) OR (category = 'RECEIVABLE' AND isSettled = 0) )
        AND description LIKE :queryPattern
        ORDER BY isSettled ASC, dateMillis DESC
    """)
    fun getUnsettledReceivablesList(queryPattern: String): Flow<List<Transaction>>

    @Query("""
        SELECT * FROM transactions WHERE
        ( (category = 'PURCHASE' AND paymentStatus IN ('DUE', 'PARTIAL')) OR (category = 'DEBT' AND isSettled = 0) )
        AND description LIKE :queryPattern
        ORDER BY isSettled ASC, dateMillis DESC
    """)
    fun getUnsettledDebtsList(queryPattern: String): Flow<List<Transaction>>

    // --- Customer-Specific Queries ---
    @Query("SELECT * FROM transactions WHERE customerId = :customerId ORDER BY dateMillis DESC")
    fun getTransactionsForCustomer(customerId: Long): Flow<List<Transaction>>

    @Query("SELECT dateMillis FROM transactions WHERE customerId = :customerId ORDER BY dateMillis DESC LIMIT 1")
    fun getLastTransactionDateForCustomer(customerId: Long): Flow<Long?>

    @Query("SELECT SUM(remainingAmount) FROM transactions WHERE customerId = :customerId AND isSettled = 0")
    fun getOutstandingBalanceForCustomer(customerId: Long): Flow<Double?>

    // --- DASHBOARD TOTALS (ALL TIME) ---
    @Query("SELECT SUM(amount) FROM transactions WHERE category = :categoryName")
    fun getTotalAmountByCategory(categoryName: String): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE category = :categoryName AND paymentStatus = :paymentStatus")
    fun getTotalAmountByCategoryAndStatus(categoryName: String, paymentStatus: String): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE category = :categoryName AND isSettled = 0")
    fun getUnsettledTotalForCategory(categoryName: String): Flow<Double?>

    @Query("""
        SELECT SUM(remainingAmount) FROM transactions
        WHERE isSettled = 0 AND (category = 'SALE' OR category = 'RECEIVABLE')
    """)
    fun getTotalOutstandingReceivables(): Flow<Double?>

    // --- DATE-RANGED QUERIES ---
    @Query("SELECT SUM(amount) FROM transactions WHERE category = :categoryName AND dateMillis BETWEEN :startDate AND :endDate")
    fun getTotalAmountByCategoryForRange(categoryName: String, startDate: Long, endDate: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE category = :categoryName AND paymentStatus = :paymentStatus AND dateMillis BETWEEN :startDate AND :endDate")
    fun getTotalAmountByCategoryAndStatusForRange(categoryName: String, paymentStatus: String, startDate: Long, endDate: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE category = :categoryName AND isSettled = 0 AND dateMillis BETWEEN :startDate AND :endDate")
    fun getUnsettledTotalForCategoryForRange(categoryName: String, startDate: Long, endDate: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE category IN ('RENT', 'OTHER_EXPENSE', 'SALARY') AND dateMillis BETWEEN :startDate AND :endDate")
    fun getOperationalExpensesInRange(startDate: Long, endDate: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE category IN (:expenseCategories) AND dateMillis BETWEEN :start AND :end")
    fun getSumOfExpensesForRange(expenseCategories: List<String>, start: Long, end: Long): Flow<Double?>

    @Query("""
        SELECT SUM(remainingAmount) FROM transactions
        WHERE isSettled = 0 AND (category = 'SALE' OR category = 'RECEIVABLE')
        AND dateMillis BETWEEN :startDate AND :endDate
    """)
    fun getTotalOutstandingReceivablesForRange(startDate: Long, endDate: Long): Flow<Double?>
    

    // --- REPORTS QUERIES ---
    @Query("""
        SELECT category, SUM(amount) as totalAmount FROM transactions
        WHERE category IN (:expenseCategories) AND dateMillis BETWEEN :start AND :end
        GROUP BY category
    """)
    fun getExpenseTotalsGroupedByCategory(expenseCategories: List<String>, start: Long, end: Long): Flow<List<CategoryTotal>>

    @Query("""
        SELECT description, SUM(quantity) as totalQuantity FROM transactions
        WHERE category = 'SALE' AND dateMillis BETWEEN :start AND :end
        GROUP BY description
        ORDER BY totalQuantity DESC
        LIMIT :limit
    """)
    fun getTopSellingItemsByQuantity(start: Long, end: Long, limit: Int = 5): Flow<List<ItemSaleTotal>>

    @Query("""
        SELECT T.customerId, C.name as customerName, SUM(T.amount) as totalAmount
        FROM transactions AS T
        LEFT JOIN customers AS C ON T.customerId = C.id
        WHERE T.category = 'SALE' AND T.dateMillis BETWEEN :start AND :end
        AND T.customerId IS NOT NULL
        GROUP BY T.customerId
        ORDER BY totalAmount DESC
        LIMIT :limit
    """)
    fun getTopCustomersBySale(start: Long, end: Long, limit: Int = 5): Flow<List<CustomerSaleTotal>>

    @Query("SELECT * FROM transactions WHERE customerId = :customerId ORDER BY dateMillis DESC")
    fun getTransactionHistoryForCustomer(customerId: Long): Flow<List<Transaction>>

    // --- NEW QUERIES FOR SMART REMINDERS ---

    /**
     * Counts the number of unsettled receivables (credit sales or pure receivables)
     * that are older than a given timestamp.
     */
    @Query("""
        SELECT COUNT(id) FROM transactions
        WHERE isSettled = 0
        AND (category = 'SALE' OR category = 'RECEIVABLE')
        AND dateMillis < :overdueTimestamp
    """)
    suspend fun getOverdueReceivablesCount(overdueTimestamp: Long): Int

    /**
     * Counts the number of unsettled debts (credit purchases or pure debts)
     * that are older than a given timestamp.
     */
    @Query("""
        SELECT COUNT(id) FROM transactions
        WHERE isSettled = 0
        AND (category = 'PURCHASE' OR category = 'DEBT')
        AND dateMillis < :overdueTimestamp
    """)
    suspend fun getOverdueDebtsCount(overdueTimestamp: Long): Int
}