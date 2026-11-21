package com.afraz.campusos.feature_toolkit

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afraz.campusos.core.util.ExpenseMath
import com.afraz.campusos.data.AppDatabase
import com.afraz.campusos.data.ExpenseGroupEntity
import com.afraz.campusos.data.ExpenseSplitEntity
import com.afraz.campusos.data.PersonalExpenseEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject

data class FinanceState(
    val monthStartMillis: Long = monthStart(System.currentTimeMillis()),
    val monthlyBudgetPaise: Long = 500_000L,
    val expenses: List<PersonalExpenseEntity> = emptyList(),
    val groups: List<ExpenseGroupEntity> = emptyList(),
    val splits: List<ExpenseSplitEntity> = emptyList()
) {
    companion object {
        fun monthStart(timeMillis: Long): Long = Calendar.getInstance().apply {
            timeInMillis = timeMillis
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}

@HiltViewModel
class FinanceViewModel @Inject constructor(
    private val database: AppDatabase,
    private val settings: DataStore<Preferences>
) : ViewModel() {
    private val budgetKey = longPreferencesKey("monthly_budget_paise")
    private val selectedMonth = MutableStateFlow(FinanceState.monthStart(System.currentTimeMillis()))
    private val budget = settings.data.map { it[budgetKey] ?: 500_000L }
    private val financeData = combine(
        database.personalExpenseDao().observeAll(),
        database.expenseGroupDao().observeAll(),
        database.expenseSplitDao().observeAll(),
        budget
    ) { expenses, groups, splits, budgetPaise ->
        FinanceState(monthlyBudgetPaise = budgetPaise, expenses = expenses, groups = groups, splits = splits)
    }
    val state: StateFlow<FinanceState> = combine(financeData, selectedMonth) { data, month ->
        data.copy(monthStartMillis = month)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FinanceState())

    fun changeMonth(offset: Int) {
        selectedMonth.value = Calendar.getInstance().apply {
            timeInMillis = selectedMonth.value
            add(Calendar.MONTH, offset)
            set(Calendar.DAY_OF_MONTH, 1)
        }.timeInMillis.let { FinanceState.monthStart(it) }
    }

    fun addExpense(note: String, category: String, amountRupees: String, timestamp: Long) = viewModelScope.launch(Dispatchers.IO) {
        val amountPaise = runCatching { ExpenseMath.rupeesToPaise(amountRupees) }.getOrNull() ?: return@launch
        if (amountPaise <= 0L) return@launch
        val title = note.trim().ifBlank { category }
        database.personalExpenseDao().upsert(
            PersonalExpenseEntity(UUID.randomUUID().toString(), title, category, amountPaise, timestamp)
        )
    }

    fun setMonthlyBudget(amountRupees: String) = viewModelScope.launch(Dispatchers.IO) {
        val amountPaise = runCatching { ExpenseMath.rupeesToPaise(amountRupees) }.getOrNull() ?: return@launch
        if (amountPaise >= 0L) settings.edit { it[budgetKey] = amountPaise }
    }

    fun setSettled(splitId: String, settled: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        database.expenseSplitDao().setSettled(splitId, settled)
    }
}
