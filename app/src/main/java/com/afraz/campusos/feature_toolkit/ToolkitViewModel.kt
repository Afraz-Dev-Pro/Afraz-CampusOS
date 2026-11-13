package com.afraz.campusos.feature_toolkit

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afraz.campusos.core.util.ExpenseMath
import com.afraz.campusos.core.util.PdfGenerator
import com.afraz.campusos.core.util.LocalDocumentPaths
import com.afraz.campusos.data.AppDatabase
import com.afraz.campusos.data.ExpenseGroupEntity
import com.afraz.campusos.data.ExpenseSplitEntity
import com.afraz.campusos.data.FriendContactEntity
import com.afraz.campusos.data.PersonalExpenseEntity
import com.afraz.campusos.data.WalletDocEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject

data class ToolkitState(
    val expenses: List<PersonalExpenseEntity> = emptyList(),
    val friends: List<FriendContactEntity> = emptyList(),
    val groups: List<ExpenseGroupEntity> = emptyList(),
    val splits: List<ExpenseSplitEntity> = emptyList(),
    val documents: List<WalletDocEntity> = emptyList(),
    val monthlyBudgetPaise: Long = 500_000L
)

@HiltViewModel
class ToolkitViewModel @Inject constructor(
    private val database: AppDatabase,
    private val settings: DataStore<Preferences>,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val budgetKey = longPreferencesKey("monthly_budget_paise")
    private val budgetFlow = settings.data.map { it[budgetKey] ?: 500_000L }
    private val mainData = combine(
        database.personalExpenseDao().observeAll(),
        database.friendContactDao().observeAll(),
        database.expenseGroupDao().observeAll(),
        database.expenseSplitDao().observeAll(),
        database.walletDocDao().observeAll()
    ) { expenses, friends, groups, splits, documents -> ToolkitState(expenses, friends, groups, splits, documents) }
    val state: StateFlow<ToolkitState> = combine(mainData, budgetFlow) { data, budget -> data.copy(monthlyBudgetPaise = budget) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ToolkitState())

    fun addExpense(title: String, category: String, amountRupees: String) = viewModelScope.launch(Dispatchers.IO) {
        val paise = runCatching { ExpenseMath.rupeesToPaise(amountRupees) }.getOrNull() ?: return@launch
        if (title.isBlank() || paise <= 0L) return@launch
        database.personalExpenseDao().upsert(PersonalExpenseEntity(UUID.randomUUID().toString(), title.trim(), category, paise, System.currentTimeMillis()))
    }

    fun deleteExpense(id: String) = viewModelScope.launch(Dispatchers.IO) { database.personalExpenseDao().delete(id) }

    fun setMonthlyBudget(amountRupees: String) = viewModelScope.launch(Dispatchers.IO) {
        val paise = runCatching { ExpenseMath.rupeesToPaise(amountRupees) }.getOrNull() ?: return@launch
        settings.edit { it[budgetKey] = paise }
    }

    fun saveFriend(name: String, upiId: String) = viewModelScope.launch(Dispatchers.IO) {
        if (name.isBlank() || !upiId.contains('@')) return@launch
        database.friendContactDao().upsert(FriendContactEntity(UUID.randomUUID().toString(), name.trim(), upiId.trim()))
    }

    fun createEqualSplit(title: String, amountRupees: String, friendIds: List<String>) = viewModelScope.launch(Dispatchers.IO) {
        val friends = database.friendContactDao().getAll().filter { it.id in friendIds }
        val total = runCatching { ExpenseMath.rupeesToPaise(amountRupees) }.getOrNull() ?: return@launch
        if (title.isBlank() || total <= 0L || friends.isEmpty()) return@launch
        val shares = ExpenseMath.splitPaise(total, friends.size + 1)
        val groupId = UUID.randomUUID().toString()
        database.expenseGroupDao().upsert(ExpenseGroupEntity(groupId, title.trim(), total, System.currentTimeMillis()))
        friends.forEachIndexed { index, friend ->
            database.expenseSplitDao().upsert(ExpenseSplitEntity(UUID.randomUUID().toString(), groupId, friend.name, friend.upiId, shares[index + 1], false))
        }
    }

    fun createCustomSplit(title: String, amountRupees: String, friendIds: List<String>, friendSharesRupees: List<String>) = viewModelScope.launch(Dispatchers.IO) {
        val friends = database.friendContactDao().getAll().filter { it.id in friendIds }
        val total = runCatching { ExpenseMath.rupeesToPaise(amountRupees) }.getOrNull() ?: return@launch
        val shares = runCatching { friendSharesRupees.map(ExpenseMath::rupeesToPaise) }.getOrNull() ?: return@launch
        if (title.isBlank() || total <= 0L || friends.isEmpty() || shares.size != friends.size || shares.sum() > total) return@launch
        val groupId = UUID.randomUUID().toString()
        database.expenseGroupDao().upsert(ExpenseGroupEntity(groupId, title.trim(), total, System.currentTimeMillis()))
        friends.forEachIndexed { index, friend ->
            database.expenseSplitDao().upsert(ExpenseSplitEntity(UUID.randomUUID().toString(), groupId, friend.name, friend.upiId, shares[index], false))
        }
    }

    fun setSettled(id: String, settled: Boolean) = viewModelScope.launch(Dispatchers.IO) { database.expenseSplitDao().setSettled(id, settled) }

    fun importDocument(uri: Uri, title: String, subjectFolder: String? = null) = viewModelScope.launch(Dispatchers.IO) {
        val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
        val isPdf = mime == "application/pdf"
        val directory = File(context.filesDir, if (isPdf) "pdfs" else "wallet").apply { mkdirs() }
        val safeTitle = title.replace(Regex("[^A-Za-z0-9._-]"), "_").take(50).ifBlank { "Document" }
        val extension = if (isPdf) ".pdf" else mime.substringAfter('/', "bin").let { ".$it" }
        val target = File(directory, "${safeTitle}-${System.currentTimeMillis()}$extension")
        context.contentResolver.openInputStream(uri)?.use { input -> target.outputStream().use(input::copyTo) } ?: return@launch
        database.walletDocDao().upsert(WalletDocEntity(UUID.randomUUID().toString(), title, if (isPdf) "PDF_NOTE" else "ID_CARD", subjectFolder, target.absolutePath, mime, System.currentTimeMillis()))
    }

    fun createNotesPdf(title: String, notes: String) = viewModelScope.launch(Dispatchers.IO) {
        if (notes.isBlank()) return@launch
        val file = PdfGenerator.fromNotes(context, title, notes.split("\n---PAGE---\n"))
        recordPdf(file, title)
    }

    fun createImagePdf(title: String, imageUris: List<Uri>) = viewModelScope.launch(Dispatchers.IO) {
        if (imageUris.isEmpty()) return@launch
        val file = PdfGenerator.fromImageUris(context, title, imageUris)
        recordPdf(file, title)
    }

    private suspend fun recordPdf(file: File, title: String) {
        database.walletDocDao().upsert(WalletDocEntity(UUID.randomUUID().toString(), title, "PDF_NOTE", null, file.absolutePath, "application/pdf", System.currentTimeMillis()))
    }

    fun deleteDocument(id: String) = viewModelScope.launch(Dispatchers.IO) {
        val doc = database.walletDocDao().getAll().firstOrNull { it.id == id } ?: return@launch
        LocalDocumentPaths.resolveManagedFile(context, doc.localFilePath)?.delete()
        database.walletDocDao().delete(id)
    }
}
