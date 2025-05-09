package com.example.roznamcha.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.roznamcha.*
import com.example.roznamcha.data.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    application: Application,
    private val repository: TransactionRepository
) : AndroidViewModel(application) {

    // --- State for the Date Range Filter ---
    private val _dateRange = MutableStateFlow(DateRange.THIS_MONTH)

    // --- StateFlow for single values from SettingsManager ---
    private val _mainAsset = MutableStateFlow(0.0)
    val mainAsset: StateFlow<Double> = _mainAsset.asStateFlow()

    private val _currencySymbol = MutableStateFlow("AFN")
    val currencySymbol: StateFlow<String> = _currencySymbol.asStateFlow()

    // --- HELPER FUNCTIONS to create reactive, date-ranged Flows ---
    private fun createRangedTotalFlow(categoryName: String): Flow<Double> {
        return _dateRange.flatMapLatest { range ->
            val (start, end) = range.getTimestamps()
            if (range == DateRange.ALL_TIME) repository.getTotalAmountByCategory(categoryName)
            else repository.getTotalForCategoryInRange(categoryName, start, end)
        }.map { it ?: 0.0 }
    }

    private fun createRangedTotalByStatusFlow(categoryName: String, status: String): Flow<Double> {
        return _dateRange.flatMapLatest { range ->
            val (start, end) = range.getTimestamps()
            if (range == DateRange.ALL_TIME) repository.getTotalAmountByCategoryAndStatus(categoryName, status)
            else repository.getTotalAmountByCategoryAndStatusForRange(categoryName, status, start, end)
        }.map { it ?: 0.0 }
    }

    private fun createRangedUnsettledTotalFlow(categoryName: String): Flow<Double> {
        return _dateRange.flatMapLatest { range ->
            val (start, end) = range.getTimestamps()
            if (range == DateRange.ALL_TIME) repository.getUnsettledTotalForCategory(categoryName)
            else repository.getUnsettledTotalForCategoryForRange(categoryName, start, end)
        }.map { it ?: 0.0 }
    }

    // --- SOURCE FLOWS (All are now date-aware) ---
    val totalSales: StateFlow<Double> = createRangedTotalFlow(TransactionCategory.SALE.name)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val totalPurchases: StateFlow<Double> = createRangedTotalFlow(TransactionCategory.PURCHASE.name)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val totalRent: StateFlow<Double> = createRangedTotalFlow(TransactionCategory.RENT.name)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val totalOtherExpenses: StateFlow<Double> = createRangedTotalFlow(TransactionCategory.OTHER_EXPENSE.name)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val totalSalaries: StateFlow<Double> = createRangedTotalFlow(TransactionCategory.SALARY.name)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private val paidSales: StateFlow<Double> = createRangedTotalByStatusFlow(TransactionCategory.SALE.name, PaymentStatus.PAID.name)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    private val paidPurchases: StateFlow<Double> = createRangedTotalByStatusFlow(TransactionCategory.PURCHASE.name, PaymentStatus.PAID.name)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    private val unsettledSales: StateFlow<Double> = createRangedTotalByStatusFlow(TransactionCategory.SALE.name, PaymentStatus.DUE.name)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    private val unsettledPureDebts: StateFlow<Double> = createRangedUnsettledTotalFlow(TransactionCategory.DEBT.name)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    private val unsettledPureReceivables: StateFlow<Double> = createRangedUnsettledTotalFlow(TransactionCategory.RECEIVABLE.name)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // --- DERIVED FLOWS (Calculated from other flows) ---
    val totalOperationalExpenses: StateFlow<Double> = combine(totalRent, totalOtherExpenses, totalSalaries) { rent, other, salary ->
        rent + other + salary
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val netProfitOrLoss: StateFlow<Double> = combine(totalSales, totalPurchases, totalOperationalExpenses) { sales, purchases, expenses ->
        sales - purchases - expenses
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalDebtsDisplay: StateFlow<Double> =
        _dateRange.flatMapLatest { range ->
            val (start, end) = range.getTimestamps()
            // We need a new repository method that calls the new DAO query
            repository.getUnsettledDebtsTotalForRange(start, end)
        }.map { it ?: 0.0 }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalReceivablesDisplay: StateFlow<Double> =
        _dateRange.flatMapLatest { range ->
            val (start, end) = range.getTimestamps()
            // <<< ENSURE it's calling getTotalOutstandingReceivablesForRange >>>
            if (range == DateRange.ALL_TIME) {
                repository.getTotalOutstandingReceivables()
            } else {
                repository.getTotalOutstandingReceivablesForRange(start, end)
            }
        }.map { it ?: 0.0 }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // <<< THIS IS THE CORRECTED BLOCK >>>
    val cashOnHand: StateFlow<Double> = combine(
        mainAsset, paidSales, unsettledPureDebts, paidPurchases,
        unsettledPureReceivables, totalRent, totalOtherExpenses, totalSalaries
    ) { values ->
        // Access the elements of the 'values' array by their index
        val asset         = values[0]
        val pSales        = values[1]
        val unDebts       = values[2]
        val pPurchases    = values[3]
        val unReceivables = values[4]
        val rent          = values[5]
        val otherExp      = values[6]
        val salaries      = values[7]

        // Perform the calculation
        (asset + pSales + unDebts) - (pPurchases + unReceivables + rent + otherExp + salaries)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)


    // --- Initialization & Public Methods ---
    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        _mainAsset.value = SettingsManager.getMainAsset(getApplication())
        _currencySymbol.value = SettingsManager.getCurrency(getApplication()) ?: "AFN"
    }

    fun saveMainAsset(amount: Double) {
        viewModelScope.launch {
            SettingsManager.saveMainAsset(getApplication(), amount)
            _mainAsset.value = amount
        }
    }

    fun setDateRange(range: DateRange) {
        _dateRange.value = range
    }
}