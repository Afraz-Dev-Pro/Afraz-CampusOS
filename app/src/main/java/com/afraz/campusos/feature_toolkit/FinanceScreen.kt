package com.afraz.campusos.feature_toolkit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.filled.DirectionsSubway
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.afraz.campusos.core.designsystem.AccentIndigo
import com.afraz.campusos.core.designsystem.GlassCard
import com.afraz.campusos.core.designsystem.NumberText
import com.afraz.campusos.core.designsystem.StatusDanger
import com.afraz.campusos.core.designsystem.StatusSafe
import com.afraz.campusos.core.designsystem.StatusWarn
import com.afraz.campusos.core.designsystem.SurfaceBento
import com.afraz.campusos.core.designsystem.SurfaceElevated
import com.afraz.campusos.core.designsystem.TextPrimary
import com.afraz.campusos.core.designsystem.TextSecondary
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
        floatingActionButton = {
            Button(onClick = { showAddExpense = true }) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Add Expense")
            }
        }
    ) { insets ->
        LazyColumn(
            Modifier.fillMaxSize().padding(insets).padding(horizontal = 18.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Finance", color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    IconButton(onClick = { viewModel.changeMonth(-1) }) { Icon(Icons.Filled.ChevronLeft, "Previous month", tint = TextSecondary) }
                    Text(SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(state.monthStartMillis)), color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    IconButton(onClick = { viewModel.changeMonth(1) }) { Icon(Icons.Filled.ChevronRight, "Next month", tint = TextSecondary) }
                }
            }
            if (wide) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            BudgetOverview(monthSpent, state.monthlyBudgetPaise, currentMonthBudget, onBudgetChange = { currentMonthBudget = it }, onSaveBudget = { viewModel.setMonthlyBudget(currentMonthBudget) })
                            CategoryBreakdown(monthExpenses)
                            SplitBalances(state, splitGroups, onSettle = openHandoff)
                        }
                        Column(Modifier.weight(1f)) {
                            TransactionList(monthExpenses, Modifier.fillMaxWidth())
                        }
                    }
                }
            } else {
                item { BudgetOverview(monthSpent, state.monthlyBudgetPaise, currentMonthBudget, onBudgetChange = { currentMonthBudget = it }, onSaveBudget = { viewModel.setMonthlyBudget(currentMonthBudget) }) }
                item { CategoryBreakdown(monthExpenses) }
                item { TransactionList(monthExpenses, Modifier.fillMaxWidth()) }
                item { SplitBalances(state, splitGroups, onSettle = openHandoff) }
            }
        }
    }

    if (showAddExpense) {
        AddExpenseSheet(
            onDismiss = { showAddExpense = false },
            onSave = { note, category, amount, timestamp ->
                viewModel.addExpense(note, category, amount, timestamp)
                showAddExpense = false
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
            text = { Text("Did you complete the payment to ${split.friendName}?") },
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
        Surface(color = SurfaceBento, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("SPENT THIS MONTH", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                NumberText(formatMoney(spent), fontSize = 30.sp)
                LinearProgressIndicator(
                    progress = { if (budget <= 0) 1f else (spent.toFloat() / budget).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = if (remaining >= 0) AccentIndigo else StatusDanger
                )
                Text(if (remaining >= 0) "${formatMoney(remaining)} remaining" else "${formatMoney(-remaining)} over budget", color = if (remaining >= 0) StatusSafe else StatusDanger, fontWeight = FontWeight.SemiBold)
                Text("Monthly budget ${formatMoney(budget)}", color = TextSecondary, fontSize = 12.sp)
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
        Text("CATEGORY BREAKDOWN", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        financeCategories.forEach { category ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(categoryIcon(category), contentDescription = null, tint = AccentIndigo, modifier = Modifier.size(20.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(category, color = TextPrimary, fontSize = 13.sp)
                        NumberText(formatMoney(totals.getValue(category)), fontSize = 12.sp)
                    }
                    LinearProgressIndicator(progress = { totals.getValue(category).toFloat() / max }, modifier = Modifier.fillMaxWidth(), color = AccentIndigo)
                }
            }
        }
    }
}

@Composable
private fun TransactionList(expenses: List<PersonalExpenseEntity>, modifier: Modifier = Modifier) {
    GlassCard(modifier) {
        Text("TRANSACTIONS", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        if (expenses.isEmpty()) {
            Text("Your spending will appear here when you add an expense.", color = TextSecondary, modifier = Modifier.padding(vertical = 8.dp))
        } else {
            val grouped = expenses.groupBy { startOfDay(it.timestamp) }
            grouped.forEach { (day, dayExpenses) ->
                Text(DateFormat.getDateInstance(DateFormat.FULL).format(Date(day)), color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
                dayExpenses.forEach { expense ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(color = SurfaceElevated, shape = CircleShape, modifier = Modifier.size(38.dp)) {
                            Icon(categoryIcon(normalizeCategory(expense.category)), contentDescription = null, tint = AccentIndigo, modifier = Modifier.padding(9.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(expense.title, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            Text(normalizeCategory(expense.category), color = TextSecondary, fontSize = 12.sp)
                        }
                        NumberText("−${formatMoney(expense.amountPaise)}", fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SplitBalances(
    state: FinanceState,
    groups: Map<String, ExpenseGroupEntity>,
    onSettle: (ExpenseSplitEntity, String) -> Unit
) {
    val unsettled = state.splits.filterNot(ExpenseSplitEntity::isSettled)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("GROUP SPLITS", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        SplitBalanceCard("You owe", "Outgoing payments", StatusDanger, unsettled, groups, onSettle, "No unpaid group splits.")
        SplitBalanceCard("Owed to you", "Incoming balances", StatusSafe, emptyList(), groups, onSettle, "No incoming balances are recorded yet.")
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
    emptyText: String
) {
    Surface(color = SurfaceBento, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = tint, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = TextSecondary, fontSize = 12.sp)
            if (splits.isEmpty()) Text(emptyText, color = TextSecondary, fontSize = 13.sp)
            splits.forEach { split ->
                val groupTitle = groups[split.groupId]?.title ?: "Group expense"
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(split.friendName, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        Text("$groupTitle · Unsettled", color = tint, fontSize = 12.sp)
                        NumberText(formatMoney(split.shareAmountPaise), fontSize = 14.sp)
                    }
                    Button(onClick = { onSettle(split, groupTitle) }) { Text("Settle up") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExpenseSheet(onDismiss: () -> Unit, onSave: (String, String, String, Long) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(financeCategories.first()) }
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = SurfaceElevated,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        Column(
            Modifier.fillMaxWidth().imePadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Add expense", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            OutlinedTextField(amount, { amount = it.filter { ch -> ch.isDigit() || ch == '.' }.take(12) }, label = { Text("Amount ₹") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Text("CATEGORY", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
            Button(onClick = { onSave(note, category, amount, selectedDate) }, enabled = amount.toBigDecimalOrNull()?.signum() == 1, modifier = Modifier.fillMaxWidth()) { Text("Save expense") }
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
