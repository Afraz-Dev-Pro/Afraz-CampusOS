package com.afraz.campusos.feature_toolkit

import com.afraz.campusos.ui.theme.LocalCampusColors
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsSubway
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import com.afraz.campusos.core.designsystem.GlassCard
import com.afraz.campusos.core.designsystem.NumberText
import com.afraz.campusos.core.util.UpiDeepLinkHelper
import com.afraz.campusos.data.ExpenseGroupEntity
import com.afraz.campusos.data.ExpenseSplitEntity
import com.afraz.campusos.data.PersonalExpenseEntity
import java.math.BigDecimal
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val financeCategories = listOf("Canteen", "Metro", "Printouts", "Other")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceScreen(modifier: Modifier = Modifier, viewModel: FinanceViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptics = LocalHapticFeedback.current
    var showAddExpense by remember { mutableStateOf(false) }
    var expenseBeingEdited by remember { mutableStateOf<PersonalExpenseEntity?>(null) }
    var expensePendingDelete by remember { mutableStateOf<PersonalExpenseEntity?>(null) }
    var showSplitBill by remember { mutableStateOf(false) }
    var selectedForHandoff by remember { mutableStateOf<Pair<ExpenseSplitEntity, String>?>(null) }
    var awaitingReturn by remember { mutableStateOf(false) }
    var handoffWaitingOn by remember { mutableStateOf<Pair<ExpenseSplitEntity, String>?>(null) }
    var pendingConfirmation by remember { mutableStateOf<Pair<ExpenseSplitEntity, String>?>(null) }
    val monthExpenses = remember(state.expenses, state.monthStartMillis) {
        val end = Calendar.getInstance().apply { timeInMillis = state.monthStartMillis; add(Calendar.MONTH, 1) }.timeInMillis
        state.expenses.filter { it.timestamp >= state.monthStartMillis && it.timestamp < end }.sortedByDescending { it.timestamp }
    }
    val splitGroups = remember(state.groups) { state.groups.associateBy(ExpenseGroupEntity::id) }
    val monthSpent = monthExpenses.sumOf(PersonalExpenseEntity::amountPaise)
    var currentMonthBudget by remember(state.monthlyBudgetPaise, state.monthStartMillis) { mutableStateOf((state.monthlyBudgetPaise / 100.0).let { "%.2f".format(Locale.ENGLISH, it) }) }

    DisposableEffect(lifecycleOwner, awaitingReturn, pendingConfirmation) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && awaitingReturn) {
                awaitingReturn = false
                pendingConfirmation = handoffWaitingOn
                handoffWaitingOn = null
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val openHandoff = { split: ExpenseSplitEntity, groupTitle: String ->
        selectedForHandoff = split to groupTitle
    }
    val wide = LocalConfiguration.current.screenWidthDp >= 600
    androidx.compose.material3.Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
    ) { insets ->
        LazyColumn(
            Modifier.fillMaxSize().padding(insets).padding(horizontal = 18.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Finance", color = LocalCampusColors.current.textPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    IconButton(onClick = { viewModel.changeMonth(-1) }) { Icon(Icons.Filled.ChevronLeft, "Previous month", tint = LocalCampusColors.current.textSecondary) }
                    Text(SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(state.monthStartMillis)), color = LocalCampusColors.current.textPrimary, fontWeight = FontWeight.SemiBold)
                    IconButton(onClick = { viewModel.changeMonth(1) }) { Icon(Icons.Filled.ChevronRight, "Next month", tint = LocalCampusColors.current.textSecondary) }
                }
            }
            if (wide) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            BudgetOverview(monthSpent, state.monthlyBudgetPaise, currentMonthBudget, onBudgetChange = { currentMonthBudget = it }, onSaveBudget = { viewModel.setMonthlyBudget(currentMonthBudget) })
                            CategoryBreakdown(monthExpenses)
                            SplitBalances(state, splitGroups, onSettle = openHandoff, onCreateSplit = { showSplitBill = true }, onMarkReceived = { pendingConfirmation = it to (splitGroups[it.groupId]?.title ?: "Group expense") })
                        }
                        Column(Modifier.weight(1f)) {
                            Button(onClick = { expenseBeingEdited = null; showAddExpense = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                                Icon(Icons.Filled.Add, contentDescription = null)
                                Spacer(Modifier.size(8.dp))
                                Text("Add Expense")
                            }
                            TransactionList(monthExpenses, Modifier.fillMaxWidth(), onEdit = { expenseBeingEdited = it; showAddExpense = true }, onDelete = { expensePendingDelete = it })
                        }
                    }
                }
            } else {
                item { BudgetOverview(monthSpent, state.monthlyBudgetPaise, currentMonthBudget, onBudgetChange = { currentMonthBudget = it }, onSaveBudget = { viewModel.setMonthlyBudget(currentMonthBudget) }) }
                item { CategoryBreakdown(monthExpenses) }
                item {
                    Button(onClick = { expenseBeingEdited = null; showAddExpense = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Add Expense")
                    }
                }
                item { TransactionList(monthExpenses, Modifier.fillMaxWidth(), onEdit = { expenseBeingEdited = it; showAddExpense = true }, onDelete = { expensePendingDelete = it }) }
                item { SplitBalances(state, splitGroups, onSettle = openHandoff, onCreateSplit = { showSplitBill = true }, onMarkReceived = { pendingConfirmation = it to (splitGroups[it.groupId]?.title ?: "Group expense") }) }
            }
        }
    }

    if (showAddExpense) {
        AddExpenseSheet(
            initialExpense = expenseBeingEdited,
            onDismiss = { showAddExpense = false },
            onSave = { id, note, category, amount, timestamp ->
                viewModel.saveExpense(id, note, category, amount, timestamp)
                showAddExpense = false
                expenseBeingEdited = null
            }
        )
    }
    expensePendingDelete?.let { expense ->
        AlertDialog(
            onDismissRequest = { expensePendingDelete = null },
            title = { Text("Remove expense?") },
            text = { Text("${expense.title} · ${formatMoney(expense.amountPaise)} will be deleted.") },
            confirmButton = { TextButton(onClick = { viewModel.deleteExpense(expense.id); expensePendingDelete = null }) { Text("Remove") } },
            dismissButton = { TextButton(onClick = { expensePendingDelete = null }) { Text("Cancel") } }
        )
    }
    if (showSplitBill) {
        SplitBillSheet(
            friends = state.friends,
            onAddFriend = viewModel::addFriend,
            onDismiss = { showSplitBill = false },
            onCreate = { title, amount, friendIds, payer, shares ->
                viewModel.createBillSplit(title, amount, friendIds, payer, shares)
                showSplitBill = false
            }
        )
    }
    selectedForHandoff?.let { (split, title) ->
        AlertDialog(
            onDismissRequest = { selectedForHandoff = null },
            title = { Text("Continue to UPI?") },
            text = { Text("Open your UPI app to continue.") },
            confirmButton = {
                TextButton(onClick = {
                    selectedForHandoff = null
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    val launched = UpiDeepLinkHelper.settle(context, split.friendUpiId, split.friendName, title, split.shareAmountPaise)
                    if (launched) {
                        handoffWaitingOn = split to title
                        awaitingReturn = true
                    }
                }) { Text("Open UPI app") }
            },
            dismissButton = { TextButton(onClick = { selectedForHandoff = null }) { Text("Cancel") } }
        )
    }
    pendingConfirmation?.let { (split, _) ->
        AlertDialog(
            onDismissRequest = { pendingConfirmation = null },
            title = { Text("Confirm payment") },
            text = { Text(if (split.direction == "YOU_OWE") "Did you complete the payment to ${split.friendName}?" else "Did ${split.friendName} repay their share?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setSettled(split.id, true)
                    pendingConfirmation = null
                }) { Text("Yes, payment complete") }
            },
            dismissButton = { TextButton(onClick = { pendingConfirmation = null }) { Text("Not yet") } }
        )
    }
}

@Composable
private fun BudgetOverview(spent: Long, budget: Long, budgetText: String, onBudgetChange: (String) -> Unit, onSaveBudget: () -> Unit) {
    val remaining = budget - spent
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(color = LocalCampusColors.current.surfacePrimary, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("SPENT THIS MONTH", color = LocalCampusColors.current.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                NumberText(formatMoney(spent), fontSize = 30.sp)
                LinearProgressIndicator(
                    progress = { if (budget <= 0) 1f else (spent.toFloat() / budget).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = if (remaining >= 0) LocalCampusColors.current.accentPrimary else LocalCampusColors.current.statusDanger
                )
                Text(if (remaining >= 0) "${formatMoney(remaining)} remaining" else "${formatMoney(-remaining)} over budget", color = if (remaining >= 0) LocalCampusColors.current.statusSafe else LocalCampusColors.current.statusDanger, fontWeight = FontWeight.SemiBold)
                Text("Monthly budget ${formatMoney(budget)}", color = LocalCampusColors.current.textSecondary, fontSize = 12.sp)
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(budgetText, onBudgetChange, label = { Text("Monthly budget ₹") }, modifier = Modifier.weight(1f), singleLine = true)
            OutlinedButton(onClick = onSaveBudget) { Text("Save") }
        }
    }
}

@Composable
private fun CategoryBreakdown(expenses: List<PersonalExpenseEntity>) {
    val totals = financeCategories.associateWith { category ->
        expenses.filter { normalizeCategory(it.category) == category }.sumOf(PersonalExpenseEntity::amountPaise)
    }
    val max = totals.values.maxOrNull()?.coerceAtLeast(1L) ?: 1L
    GlassCard {
        Text("CATEGORY BREAKDOWN", color = LocalCampusColors.current.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        financeCategories.forEach { category ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(categoryIcon(category), contentDescription = "${category} category", tint = LocalCampusColors.current.accentPrimary, modifier = Modifier.size(20.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(category, color = LocalCampusColors.current.textPrimary, fontSize = 13.sp)
                        NumberText(formatMoney(totals.getValue(category)), fontSize = 12.sp)
                    }
                    LinearProgressIndicator(progress = { totals.getValue(category).toFloat() / max }, modifier = Modifier.fillMaxWidth(), color = LocalCampusColors.current.accentPrimary)
                }
            }
        }
    }
}

@Composable
private fun TransactionList(
    expenses: List<PersonalExpenseEntity>,
    modifier: Modifier = Modifier,
    onEdit: (PersonalExpenseEntity) -> Unit,
    onDelete: (PersonalExpenseEntity) -> Unit
) {
    val compact = LocalConfiguration.current.screenWidthDp < 380
    GlassCard(modifier) {
        Text("TRANSACTIONS", color = LocalCampusColors.current.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        if (expenses.isEmpty()) {
            Text("Your spending will appear here when you add an expense.", color = LocalCampusColors.current.textSecondary, modifier = Modifier.padding(vertical = 8.dp))
        } else {
            val grouped = expenses.groupBy { startOfDay(it.timestamp) }
            grouped.forEach { (day, dayExpenses) ->
                Text(DateFormat.getDateInstance(DateFormat.FULL).format(Date(day)), color = LocalCampusColors.current.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
                dayExpenses.forEach { expense ->
                    if (compact) {
                        Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TransactionCategoryIcon(expense)
                                Column(Modifier.weight(1f)) {
                                    Text(expense.title, color = LocalCampusColors.current.textPrimary, fontWeight = FontWeight.SemiBold)
                                    Text(normalizeCategory(expense.category), color = LocalCampusColors.current.textSecondary, fontSize = 12.sp)
                                }
                                NumberText("−${formatMoney(expense.amountPaise)}", fontSize = 13.sp)
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TransactionActions(expense, onEdit, onDelete)
                            }
                        }
                    } else {
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TransactionCategoryIcon(expense)
                            Column(Modifier.weight(1f)) {
                                Text(expense.title, color = LocalCampusColors.current.textPrimary, fontWeight = FontWeight.SemiBold)
                                Text(normalizeCategory(expense.category), color = LocalCampusColors.current.textSecondary, fontSize = 12.sp)
                            }
                            NumberText("−${formatMoney(expense.amountPaise)}", fontSize = 13.sp)
                            TransactionActions(expense, onEdit, onDelete)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionCategoryIcon(expense: PersonalExpenseEntity) {
    Surface(color = LocalCampusColors.current.surfaceElevated, shape = CircleShape, modifier = Modifier.size(38.dp)) {
        Icon(categoryIcon(normalizeCategory(expense.category)), contentDescription = "${normalizeCategory(expense.category)} transaction", tint = LocalCampusColors.current.accentPrimary, modifier = Modifier.padding(9.dp))
    }
}

@Composable
private fun TransactionActions(
    expense: PersonalExpenseEntity,
    onEdit: (PersonalExpenseEntity) -> Unit,
    onDelete: (PersonalExpenseEntity) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        IconButton(onClick = { onEdit(expense) }, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Filled.Edit, contentDescription = "Edit ${expense.title}", tint = LocalCampusColors.current.accentPrimary, modifier = Modifier.size(19.dp))
        }
        IconButton(onClick = { onDelete(expense) }, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Filled.Delete, contentDescription = "Remove ${expense.title}", tint = LocalCampusColors.current.statusDanger, modifier = Modifier.size(19.dp))
        }
    }
}

@Composable
private fun SplitBalances(
    state: FinanceState,
    groups: Map<String, ExpenseGroupEntity>,
    onSettle: (ExpenseSplitEntity, String) -> Unit,
    onCreateSplit: () -> Unit,
    onMarkReceived: (ExpenseSplitEntity) -> Unit
) {
    val outgoing = state.splits.filter { !it.isSettled && it.direction == "YOU_OWE" }
    val incoming = state.splits.filter { !it.isSettled && it.direction == "OWED_TO_YOU" }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("GROUP SPLITS", color = LocalCampusColors.current.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Button(onClick = onCreateSplit, modifier = Modifier.heightIn(min = 48.dp)) { Text("Split a bill") }
        }
        SplitBalanceCard("You owe", "Outgoing payments", LocalCampusColors.current.statusDanger, outgoing, groups, onSettle, { }, "No unpaid group splits.")
        SplitBalanceCard("Owed to you", "Incoming balances", LocalCampusColors.current.statusSafe, incoming, groups, { _, _ -> }, onMarkReceived, "No incoming balances are recorded yet.")
    }
}

@Composable
private fun SplitBalanceCard(
    title: String,
    subtitle: String,
    tint: Color,
    splits: List<ExpenseSplitEntity>,
    groups: Map<String, ExpenseGroupEntity>,
    onSettle: (ExpenseSplitEntity, String) -> Unit,
    onMarkReceived: (ExpenseSplitEntity) -> Unit,
    emptyText: String
) {
    Surface(color = LocalCampusColors.current.surfacePrimary, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = tint, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = LocalCampusColors.current.textSecondary, fontSize = 12.sp)
            if (splits.isEmpty()) Text(emptyText, color = LocalCampusColors.current.textSecondary, fontSize = 13.sp)
            splits.forEach { split ->
                val groupTitle = groups[split.groupId]?.title ?: "Group expense"
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(split.friendName, color = LocalCampusColors.current.textPrimary, fontWeight = FontWeight.SemiBold)
                        Text("$groupTitle · Unsettled", color = tint, fontSize = 12.sp)
                        NumberText(formatMoney(split.shareAmountPaise), fontSize = 14.sp)
                    }
                    if (split.direction == "YOU_OWE") {
                        Button(onClick = { onSettle(split, groupTitle) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Settle up") }
                    } else {
                        OutlinedButton(onClick = { onMarkReceived(split) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Mark received") }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExpenseSheet(
    initialExpense: PersonalExpenseEntity?,
    onDismiss: () -> Unit,
    onSave: (String?, String, String, String, Long) -> Unit
) {
    var amount by remember(initialExpense?.id) { mutableStateOf(initialExpense?.let { BigDecimal.valueOf(it.amountPaise, 2).toPlainString() }.orEmpty()) }
    var note by remember(initialExpense?.id) { mutableStateOf(initialExpense?.title.orEmpty()) }
    var category by remember(initialExpense?.id) { mutableStateOf(initialExpense?.category?.let(::normalizeCategory) ?: financeCategories.first()) }
    var selectedDate by remember(initialExpense?.id) { mutableStateOf(initialExpense?.timestamp ?: System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = LocalCampusColors.current.surfaceElevated,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        Column(
            Modifier.fillMaxWidth().imePadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(if (initialExpense == null) "Add expense" else "Edit expense", color = LocalCampusColors.current.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            OutlinedTextField(amount, { amount = it.filter { ch -> ch.isDigit() || ch == '.' }.take(12) }, label = { Text("Amount ₹") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Text("CATEGORY", color = LocalCampusColors.current.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            financeCategories.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { option ->
                        if (option == category) Button(onClick = { category = option }, modifier = Modifier.weight(1f)) { Text(option) }
                        else OutlinedButton(onClick = { category = option }, modifier = Modifier.weight(1f)) { Text(option) }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Date · ${DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(selectedDate))}")
            }
            OutlinedTextField(note, { note = it }, label = { Text("Note (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Button(onClick = { onSave(initialExpense?.id, note, category, amount, selectedDate) }, enabled = amount.toBigDecimalOrNull()?.signum() == 1, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(if (initialExpense == null) "Save expense" else "Save changes") }
        }
    }
    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = dateToUtcMillis(selectedDate))
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { selectedDate = dateFromUtcMillis(it) }
                    showDatePicker = false
                }) { Text("Select") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = pickerState) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SplitBillSheet(
    friends: List<com.afraz.campusos.data.FriendContactEntity>,
    onAddFriend: (String, String) -> Unit,
    onDismiss: () -> Unit,
    onCreate: (String, String, List<String>, String?, List<String>?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var friendName by remember { mutableStateOf("") }
    var friendUpi by remember { mutableStateOf("") }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var payerFriendId by remember { mutableStateOf<String?>(null) }
    var customSplit by remember { mutableStateOf(false) }
    val customShares = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateMapOf<String, String>() }
    var yourShare by remember { mutableStateOf("") }
    val selectedFriends = friends.filter { it.id in selectedIds }
    val parsedTotal = amount.toBigDecimalOrNull()
    val customValues = if (payerFriendId == null) selectedFriends.map { customShares[it.id]?.toBigDecimalOrNull() } else listOf(yourShare.toBigDecimalOrNull())
    val customValid = !customSplit || (customValues.isNotEmpty() && customValues.all { it != null && it.signum() > 0 } &&
        customValues.filterNotNull().fold(BigDecimal.ZERO, BigDecimal::add) <= (parsedTotal ?: BigDecimal.ZERO))
    val canCreate = title.isNotBlank() && (parsedTotal?.signum() ?: 0) > 0 && selectedFriends.isNotEmpty() && customValid

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = LocalCampusColors.current.surfaceElevated,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        Column(
            Modifier.fillMaxWidth().imePadding().navigationBarsPadding().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Split a bill", color = LocalCampusColors.current.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Save each person’s share and settle your part with UPI.", color = LocalCampusColors.current.textSecondary)
            OutlinedTextField(title, { title = it.take(60) }, label = { Text("Bill or group name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(amount, { amount = it.filter { c -> c.isDigit() || c == '.' }.take(12) }, label = { Text("Total bill · ₹") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Text("WHO PAID?", color = LocalCampusColors.current.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = payerFriendId == null, onClick = { payerFriendId = null }, label = { Text("I paid") })
                if (payerFriendId != null) {
                    val payer = selectedFriends.firstOrNull { it.id == payerFriendId }
                    FilterChip(selected = true, onClick = { payerFriendId = null }, label = { Text("${payer?.name ?: "Friend"} paid") })
                }
            }
            Text("PARTICIPANTS", color = LocalCampusColors.current.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            if (friends.isEmpty()) Text("Add a friend with their UPI ID to start a split.", color = LocalCampusColors.current.textSecondary)
            friends.forEach { friend ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = friend.id in selectedIds,
                        onClick = {
                            selectedIds = if (friend.id in selectedIds) selectedIds - friend.id else selectedIds + friend.id
                            if (friend.id !in selectedIds && payerFriendId == friend.id) payerFriendId = null
                        },
                        label = { Text("${friend.name} · ${friend.upiId}") },
                        modifier = Modifier.weight(1f)
                    )
                    if (friend.id in selectedIds && payerFriendId == null) {
                        OutlinedButton(onClick = { payerFriendId = friend.id }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Paid") }
                    }
                }
            }
            OutlinedTextField(friendName, { friendName = it.take(40) }, label = { Text("Add friend name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(friendUpi, { friendUpi = it.take(100) }, label = { Text("Friend UPI ID") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedButton(
                onClick = { onAddFriend(friendName, friendUpi); friendName = ""; friendUpi = "" },
                enabled = friendName.isNotBlank() && friendUpi.contains('@'),
                modifier = Modifier.heightIn(min = 48.dp)
            ) { Text("Save friend") }
            FilterChip(selected = customSplit, onClick = { customSplit = !customSplit }, label = { Text(if (customSplit) "Custom shares" else "Use equal split") })
            if (customSplit) {
                if (payerFriendId == null) {
                    selectedFriends.forEach { friend ->
                        OutlinedTextField(
                            customShares[friend.id].orEmpty(),
                            { customShares[friend.id] = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("${friend.name} share · ₹") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                } else {
                    OutlinedTextField(yourShare, { yourShare = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Your share · ₹") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
                if (!customValid) Text("Enter positive shares that do not exceed the total bill.", color = LocalCampusColors.current.statusDanger, fontSize = 12.sp)
            }
            Button(
                onClick = {
                    val ids = selectedFriends.map { it.id }
                    val payer = payerFriendId
                    val shares = if (!customSplit) null else if (payer == null) selectedFriends.map { customShares[it.id].orEmpty() } else listOf(yourShare)
                    onCreate(title, amount, ids, payer, shares)
                },
                enabled = canCreate,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) { Text("Create bill split") }
        }
    }
}

private fun normalizeCategory(category: String): String = when {
    category.equals("Canteen", ignoreCase = true) -> "Canteen"
    category.equals("Metro", ignoreCase = true) -> "Metro"
    category.equals("Printouts", ignoreCase = true) -> "Printouts"
    else -> "Other"
}

private fun categoryIcon(category: String): ImageVector = when (normalizeCategory(category)) {
    "Canteen" -> Icons.Filled.Restaurant
    "Metro" -> Icons.Filled.DirectionsSubway
    "Printouts" -> Icons.Filled.Print
    else -> Icons.AutoMirrored.Filled.ReceiptLong
}

private fun formatMoney(paise: Long): String = "₹" + BigDecimal.valueOf(paise, 2).setScale(2).toPlainString()
private fun startOfDay(time: Long): Long = Calendar.getInstance().apply {
    timeInMillis = time
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis
private fun dateToUtcMillis(time: Long): Long = Calendar.getInstance().apply {
    timeInMillis = time
    val year = get(Calendar.YEAR); val month = get(Calendar.MONTH); val day = get(Calendar.DAY_OF_MONTH)
    timeInMillis = 0
    set(Calendar.YEAR, year); set(Calendar.MONTH, month); set(Calendar.DAY_OF_MONTH, day)
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis
private fun dateFromUtcMillis(time: Long): Long {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US).apply { timeInMillis = time }
    return Calendar.getInstance().apply {
        clear()
        set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH), 12, 0, 0)
    }.timeInMillis
}

