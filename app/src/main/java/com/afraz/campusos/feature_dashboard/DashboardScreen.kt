package com.afraz.campusos.feature_dashboard

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.net.Uri
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.afraz.campusos.core.designsystem.GlassCard
import com.afraz.campusos.core.designsystem.NumberText
import com.afraz.campusos.core.designsystem.StatusDanger
import com.afraz.campusos.core.designsystem.StatusSafe
import com.afraz.campusos.core.designsystem.StatusWarn
import com.afraz.campusos.core.designsystem.TextPrimary
import com.afraz.campusos.core.designsystem.TextSecondary
import com.afraz.campusos.core.util.OemBatteryHelper
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    onOpenToolkit: () -> Unit,
    onOpenScanner: () -> Unit,
    onOpenAcademics: () -> Unit,
    onOpenFinance: () -> Unit,
    onOpenDigitalId: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    var showToolPicker by remember { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(state.nextClass?.startsAt) {
        while (true) {
            now = System.currentTimeMillis()
            kotlinx.coroutines.delay(15_000)
        }
    }
    LazyColumn(modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Spacer(Modifier.height(16.dp))
            Text("CAMPUSOS / STUDENT SPACE", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("Your campus, in one place", color = TextPrimary, fontSize = 27.sp, fontWeight = FontWeight.Bold)
        }
        item {
            GlassCard {
                Text("CAMPUS SHORTCUTS", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onOpenDigitalId, modifier = Modifier.weight(1f)) { Text("Digital ID") }
                    Button(onClick = onOpenFinance, modifier = Modifier.weight(1f)) { Text("Finance") }
                }
            }
        }
        item {
            GlassCard {
                Text("UP NEXT", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                val next = state.nextClass
                if (next == null) {
                    Text("No upcoming class", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
                    Text("Add courses and timetable slots in Academics.", color = TextSecondary)
                    Button(onClick = onOpenAcademics) { Text("Open timetable") }
                } else {
                    Text(next.course.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    Text("${next.course.code} · Room ${next.course.room.ifBlank { "—" }}", color = TextSecondary)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(Date(next.startsAt).toString(), color = TextSecondary)
                        NumberText(countdown(next.startsAt - now), fontSize = 16.sp)
                    }
                }
            }
        }
        state.timetableAssetPath?.let { path -> item {
            GlassCard {
                Text("MY TIMETABLE", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                if (state.timetableAssetMime?.startsWith("image/") == true) {
                    AsyncImage(model = java.io.File(path), contentDescription = "Saved timetable", modifier = Modifier.fillMaxWidth().height(210.dp))
                } else {
                    Button(onClick = {
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", java.io.File(path))
                        context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/pdf").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                    }) { Text("Open timetable PDF") }
                }
            }
        } }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassCard(Modifier.weight(1f)) {
                    Text("ATTENDANCE", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    if (state.aggregateAttendance == null) {
                        Text("No data", color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text("Log classes to see your aggregate.", color = TextSecondary, fontSize = 12.sp)
                    } else {
                        val percentage = state.aggregateAttendance!!.toFloat() / 100f
                        val tint = when { percentage >= 0.75f -> StatusSafe; percentage >= 0.70f -> StatusWarn; else -> StatusDanger }
                        CircularProgressIndicator(progress = { percentage.coerceIn(0f, 1f) }, color = tint)
                        NumberText("${"%.1f".format(state.aggregateAttendance)}%", fontSize = 19.sp)
                        state.lowestAttendance?.let { Text("Watch ${it.code}: ${it.attendedClasses}/${it.totalClasses}", color = StatusWarn, fontSize = 11.sp) }
                    }
                }
                GlassCard(Modifier.weight(1f)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("QUICK TOOLS", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Button(onClick = { showToolPicker = true }) { Text("Edit") }
                    }
                    state.pinnedTools.forEach { tool ->
                        Button(onClick = when (tool) {
                            "Scan Notes" -> onOpenScanner
                            "Timetable", "Assignments" -> onOpenAcademics
                            "Digital ID", "Digital Wallet" -> onOpenDigitalId
                            "Expense Tracker", "Finance" -> onOpenFinance
                            else -> onOpenToolkit
                        }, modifier = Modifier.fillMaxWidth()) { Text(tool) }
                    }
                    Text("${state.pinnedTools.size}/3 pinned", color = TextSecondary, fontSize = 11.sp)
                }
            }
        }
        item {
            GlassCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("URGENT · NEXT 48H", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("${state.tasks.size}", color = if (state.tasks.isEmpty()) StatusSafe else StatusWarn)
                }
                if (state.tasks.isEmpty()) Text("You’re clear", color = StatusSafe)
                state.tasks.forEach { task ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(task.title, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            Text(task.category, color = TextSecondary, fontSize = 12.sp)
                        }
                        NumberText(countdown(task.dueEpochMillis - now), fontSize = 12.sp)
                    }
                }
                if (state.tasks.isNotEmpty()) Button(onClick = onOpenAcademics) { Text("View tasks") }
            }
        }
        if (state.showBatteryWarning && !state.batteryWarningDismissed) item {
            GlassCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Background reminders may be stopped", color = StatusWarn, fontWeight = FontWeight.Bold)
                        Text("Your ${android.os.Build.MANUFACTURER} device may restrict alarms and focus timers.", color = TextSecondary)
                    }
                    Button(onClick = viewModel::dismissBatteryWarning) { Text("×") }
                }
                Button(onClick = {
                    try { context.startActivity(OemBatteryHelper.requestIntent(context)) }
                    catch (_: ActivityNotFoundException) { context.startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:${context.packageName}"))) }
                }) { Text("Allow background activity") }
            }
        }
        item { Spacer(Modifier.height(18.dp)) }
    }
    if (showToolPicker) {
        ModalBottomSheet(onDismissRequest = { showToolPicker = false }) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Pin up to three quick tools", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                DashboardViewModel.TOOL_CATALOG.forEach { tool ->
                    val selected = tool in state.pinnedTools
                    Button(onClick = { viewModel.togglePinnedTool(tool) }, enabled = selected || state.pinnedTools.size < DashboardViewModel.PIN_LIMIT, modifier = Modifier.fillMaxWidth()) {
                        Text(if (selected) "✓  $tool" else tool)
                    }
                }
            }
        }
    }
}

private fun countdown(milliseconds: Long): String {
    if (milliseconds <= 0L) return "Now"
    val minutes = milliseconds / 60_000L
    val days = minutes / (60 * 24)
    val hours = (minutes / 60) % 24
    val remainingMinutes = minutes % 60
    return when {
        days > 0 -> "${days}d ${hours}h"
        hours > 0 -> "${hours}h ${remainingMinutes}m"
        else -> "${remainingMinutes}m"
    }
}
