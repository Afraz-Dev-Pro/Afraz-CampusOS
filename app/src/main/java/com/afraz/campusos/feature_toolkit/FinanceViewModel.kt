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
import com.afraz.campusos.data.FriendContactEntity
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
    val splits: List<ExpenseSplitEntity> = emptyList(),
    val friends: List<FriendContactEntity> = emptyList()
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
        database.friendContactDao().observeAll(),
        budget
    ) { expenses, groups, splits, friends, budgetPaise ->
        FinanceState(monthlyBudgetPaise = budgetPaise, expenses = expenses, groups = groups, splits = splits, friends = friends)
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

    fun saveExpense(id: String?, note: String, category: String, amountRupees: String, timestamp: Long) = viewModelScope.launch(Dispatchers.IO) {
        val amountPaise = runCatching { ExpenseMath.rupeesToPaise(amountRupees) }.getOrNull() ?: return@launch
        if (amountPaise <= 0L) return@launch
        val title = note.trim().ifBlank { category }
        database.personalExpenseDao().upsert(
            PersonalExpenseEntity(id ?: UUID.randomUUID().toString(), title, category, amountPaise, timestamp)
        )
    }

    fun deleteExpense(id: String) = viewModelScope.launch(Dispatchers.IO) { database.personalExpenseDao().delete(id) }

    fun addFriend(name: String, upiId: String) = viewModelScope.launch(Dispatchers.IO) {
        if (name.isBlank() || !upiId.trim().contains('@')) return@launch
        database.friendContactDao().upsert(FriendContactEntity(UUID.randomUUID().toString(), name.trim(), upiId.trim()))
    }

    fun createBillSplit(
        title: String,
        amountRupees: String,
        friendIds: List<String>,
        paidByFriendId: String?,
        customSharesRupees: List<String>?
    ) = viewModelScope.launch(Dispatchers.IO) {
        val friends = database.friendContactDao().getAll().filter { it.id in friendIds }
        val total = runCatching { ExpenseMath.rupeesToPaise(amountRupees) }.getOrNull() ?: return@launch
        if (title.isBlank() || total <= 0L || friends.isEmpty()) return@launch
        val selectedPayer = friends.firstOrNull { it.id == paidByFriendId }
        if (paidByFriendId != null && selectedPayer == null) return@launch
        val shares = if (customSharesRupees == null) {
            ExpenseMath.splitPaise(total, friends.size + 1)
        } else {
            val parsed = runCatching { customSharesRupees.map(ExpenseMath::rupeesToPaise) }.getOrNull() ?: return@launch
            if (paidByFriendId == null && parsed.size != friends.size) return@launch
            if (parsed.sum() > total) return@launch
            parsed
        }
        val groupId = UUID.randomUUID().toString()
        database.expenseGroupDao().upsert(ExpenseGroupEntity(groupId, title.trim(), total, System.currentTimeMillis()))
        if (selectedPayer == null) {
            val friendShares = if (customSharesRupees == null) shares.drop(1) else shares
            friends.forEachIndexed { index, friend ->
                val amount = friendShares.getOrNull(index) ?: return@forEachIndexed
                if (amount > 0) database.expenseSplitDao().upsert(
                    ExpenseSplitEntity(UUID.randomUUID().toString(), groupId, friend.name, friend.upiId, amount, false, "OWED_TO_YOU")
                )
            }
        } else {
            val myShare = if (customSharesRupees == null) shares.first() else shares.firstOrNull() ?: 0L
            if (myShare <= 0) return@launch
            database.expenseSplitDao().upsert(
                ExpenseSplitEntity(UUID.randomUUID().toString(), groupId, selectedPayer.name, selectedPayer.upiId, myShare, false, "YOU_OWE")
            )
        }
    }

    fun setMonthlyBudget(amountRupees: String) = viewModelScope.launch(Dispatchers.IO) {
        val amountPaise = runCatching { ExpenseMath.rupeesToPaise(amountRupees) }.getOrNull() ?: return@launch
        if (amountPaise >= 0L) settings.edit { it[budgetKey] = amountPaise }
    }

    fun setSettled(splitId: String, settled: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        database.expenseSplitDao().setSettled(splitId, settled)
    }
}

