package com.example.roznamcha.ui.reports

import android.app.Application
import androidx.lifecycle.*
import com.example.roznamcha.DateRange
import com.example.roznamcha.TransactionCategory
import com.example.roznamcha.data.TransactionRepository
import com.example.roznamcha.data.db.entity.CategoryTotal
import com.example.roznamcha.data.db.entity.CustomerSaleTotal // <<< CORRECTED IMPORT
import com.example.roznamcha.data.db.entity.ItemSaleTotal
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map


@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModel(
    application: Application,
    private val repository: TransactionRepository
) : AndroidViewModel(application) {

    private val _dateRange = MutableStateFlow(DateRange.THIS_MONTH)

    private val expenseCategoryNames = listOf(
        TransactionCategory.PURCHASE.name,
        TransactionCategory.RENT.name,
        TransactionCategory.OTHER_EXPENSE.name,
        TransactionCategory.SALARY.name // <<< CORRECTED from Transaction.SALARY
    )

    // --- Source Flows for Health Score Calculation ---
    private val totalSalesFlow = _dateRange.flatMapLatest { range ->
        val (start, end) = range.getTimestamps()
        repository.getTotalForCategoryInRange(TransactionCategory.SALE.name, start, end)
    }.map { it ?: 0.0 }

    private val totalExpensesFlow = _dateRange.flatMapLatest { range ->
        val (start, end) = range.getTimestamps()
        repository.getSumOfExpensesForRange(expenseCategoryNames, start, end)
    }.map { it ?: 0.0 }

    private val totalReceivablesFlow = _dateRange.flatMapLatest { range ->
        val (start, end) = range.getTimestamps()
        repository.getTotalOutstandingReceivablesForRange(start, end)
    }.map { it ?: 0.0 }


    // --- LiveData for UI ("Top Lists") ---
    val topExpenses: LiveData<List<CategoryTotal>> = _dateRange.flatMapLatest { range ->
        val (start, end) = range.getTimestamps()
        repository.getExpenseTotalsGroupedByCategory(expenseCategoryNames, start, end)
    }.asLiveData()

    val topSellingItems: LiveData<List<ItemSaleTotal>> = _dateRange.flatMapLatest { range ->
        val (start, end) = range.getTimestamps()
        repository.getTopSellingItemsByQuantity(start, end)
    }.asLiveData()

    val topCustomers: LiveData<List<CustomerSaleTotal>> = _dateRange.flatMapLatest { range ->
        val (start, end) = range.getTimestamps()
        repository.getTopCustomersBySale(start, end)
    }.asLiveData()

    // LiveData for Financial Health Score
    val financialHealthScore: LiveData<Pair<Int, String>> =
        combine(totalSalesFlow, totalExpensesFlow, totalReceivablesFlow) { sales, expenses, receivables ->
            var score = 0
            if (sales > 0) {
                val netProfit = sales - expenses
                if (netProfit > 0) score += 50
                if ((expenses / sales) < 0.8) score += 25
                if ((receivables / sales) < 0.3) score += 25
            }
            val rating = when {
                score >= 75 -> "عالی"
                score >= 50 -> "خوب"
                else -> "نیاز به توجه"
            }
            Pair(score, rating)
        }.asLiveData()

    fun setDateRange(range: DateRange) {
        _dateRange.value = range
    }
}