package com.afraz.campusos.feature_dashboard

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.afraz.campusos.core.designsystem.AccentIndigo
import com.afraz.campusos.core.designsystem.GlassCard
import com.afraz.campusos.core.designsystem.NumberText
import com.afraz.campusos.core.designsystem.StatusDanger
import com.afraz.campusos.core.designsystem.StatusSafe
import com.afraz.campusos.core.designsystem.StatusWarn
import com.afraz.campusos.core.designsystem.SurfaceBento
import com.afraz.campusos.core.designsystem.TextPrimary
import com.afraz.campusos.core.designsystem.TextSecondary
import com.afraz.campusos.core.util.OemBatteryHelper
import com.afraz.campusos.data.CourseEntity
import com.afraz.campusos.data.ScheduleSlotEntity
import com.afraz.campusos.data.TaskEntity
import com.afraz.campusos.ui.theme.CampusOsTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil

@Composable
fun HomeScreen(
    onOpenClasses: () -> Unit,
    onOpenImport: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenFinance: () -> Unit,
    onOpenTools: () -> Unit,
    onOpenScanner: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            kotlinx.coroutines.delay(15_000L)
        }
    }
    HomeScreenContent(
        state = state,
        now = now,
        onOpenClasses = onOpenClasses,
        onOpenImport = onOpenImport,
        onOpenProfile = onOpenProfile,
        onOpenFinance = onOpenFinance,
        onOpenTools = onOpenTools,
        onOpenScanner = onOpenScanner,
        onDismissBatteryNotice = viewModel::dismissBatteryNotice,
        onOpenBatterySettings = {
            try {
                context.startActivity(OemBatteryHelper.requestIntent(context))
            } catch (_: ActivityNotFoundException) {
                context.startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(android.net.Uri.parse("package:${context.packageName}")))
            }
        }
    )
}

@Composable
private fun HomeScreenContent(
    state: HomeState,
    now: Long,
    onOpenClasses: () -> Unit,
    onOpenImport: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenFinance: () -> Unit,
    onOpenTools: () -> Unit,
    onOpenScanner: () -> Unit,
    onDismissBatteryNotice: () -> Unit,
    onOpenBatterySettings: () -> Unit
) {
    val date = remember(now) { SimpleDateFormat("EEEE · d MMMM", Locale.ENGLISH).format(Date(now)) }
    val hour = remember(now) { java.util.Calendar.getInstance().apply { timeInMillis = now }.get(java.util.Calendar.HOUR_OF_DAY) }
    val greeting = when (hour) { in 5..11 -> "Good morning, Afraz"; in 12..16 -> "Good afternoon, Afraz"; in 17..21 -> "Good evening, Afraz"; else -> "Good night, Afraz" }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .padding(horizontal = 18.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(date, color = TextSecondary, fontSize = 13.sp)
                    Text(greeting, color = TextPrimary, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onOpenProfile, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Outlined.Person, contentDescription = "Profile", tint = TextPrimary)
                }
            }
        }
        item { NextClassPanel(state.nextClass, now, onOpenClasses, onOpenImport) }
        item { AttendanceHealth(state.aggregateAttendance, state.weakestCourse) }
        item { TodayTimeline(state.todayClasses, now) }
        state.urgentTask?.let { task ->
            item {
                Surface(color = SurfaceBento, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(9.dp).clip(CircleShape).background(StatusWarn))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("DUE SOON · ${task.category}", color = StatusWarn, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(task.title, color = TextPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                        NumberText(countdown(task.dueEpochMillis - now), fontSize = 12.sp)
                    }
                }
            }
        }
        if (state.pinnedActions.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("QUICK ACTIONS", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.pinnedActions.take(3).forEach { action ->
                            OutlinedButton(
                                onClick = when (action) {
                                    "Scan Notes" -> onOpenScanner
                                    "Assignments", "Timetable" -> onOpenClasses
                                    "Finance" -> onOpenFinance
                                    else -> onOpenTools
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier.weight(1f)
                            ) { Text(action, maxLines = 1, fontSize = 12.sp) }
                        }
                    }
                }
            }
        }
        if (state.batteryRestrictionsActive && !state.batteryNoticeDismissed) {
            item {
                Surface(color = SurfaceBento, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Background limits are active", color = StatusWarn, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            IconButton(onClick = onDismissBatteryNotice, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Outlined.Close, contentDescription = "Dismiss battery notice", tint = TextSecondary)
                            }
                        }
                        Text("Your ${android.os.Build.MANUFACTURER} device may delay class and focus reminders.", color = TextSecondary, fontSize = 13.sp)
                        OutlinedButton(onClick = onOpenBatterySettings) { Text("Review background access") }
                    }
                }
            }
        }
    }
}

@Composable
private fun NextClassPanel(next: HomeClassItem?, now: Long, onAddTimetable: () -> Unit, onImport: () -> Unit) {
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(9.dp).clip(CircleShape).background(AccentIndigo))
            Spacer(Modifier.width(8.dp))
            Text("NEXT CLASS", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            if (next != null) NumberText("· ${countdown(next.startsAt - now).uppercase()}", fontSize = 12.sp)
        }
        Spacer(Modifier.height(12.dp))
        if (next == null) {
            Text("Add your timetable to see your next class here.", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAddTimetable) { Icon(Icons.Outlined.Add, contentDescription = "Add timetable"); Spacer(Modifier.width(4.dp)); Text("Add Timetable") }
                OutlinedButton(onClick = onImport) { Text("Import") }
            }
        } else {
            Text(next.course.name, color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            NumberText("${formatTime(next.slot.startMinutes)}–${formatTime(next.slot.endMinutes)}", fontSize = 15.sp)
            Spacer(Modifier.height(6.dp))
            Text("Room ${next.course.room.ifBlank { "—" }} · ${next.course.professor.ifBlank { "Professor not set" }}", color = TextSecondary, fontSize = 13.sp)
        }
    }
}

@Composable
private fun AttendanceHealth(aggregate: Double?, weakest: CourseEntity?) {
    Surface(color = SurfaceBento, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("ATTENDANCE HEALTH", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            if (aggregate == null || weakest == null) {
                Text("Attendance starts when you log your first class.", color = TextPrimary, fontWeight = FontWeight.Medium)
                Text("Your overall status and recovery guidance will appear here.", color = TextSecondary, fontSize = 12.sp)
            } else {
                val weakestRatio = weakest.attendedClasses.toDouble() / weakest.totalClasses
                val color = when { aggregate >= 75.0 -> StatusSafe; aggregate >= 70.0 -> StatusWarn; else -> StatusDanger }
                val cue = if (weakestRatio >= weakest.targetPercentage) {
                    "${weakest.code} is on track for its ${(weakest.targetPercentage * 100).toInt()}% target."
                } else {
                    val classes = ceil((weakest.targetPercentage * weakest.totalClasses - weakest.attendedClasses) / (1.0 - weakest.targetPercentage)).toInt().coerceAtLeast(1)
                    "Attend next $classes classes to reach ${(weakest.targetPercentage * 100).toInt()}%."
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NumberText("${"%.1f".format(Locale.ENGLISH, aggregate)}%", fontSize = 22.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(when { aggregate >= 75.0 -> "On track"; aggregate >= 70.0 -> "Attention"; else -> "Needs recovery" }, color = color, fontWeight = FontWeight.Bold)
                }
                Text("Lowest: ${weakest.name} · ${weakest.attendedClasses}/${weakest.totalClasses}", color = color, fontSize = 13.sp)
                Text(cue, color = TextSecondary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun TodayTimeline(classes: List<HomeClassItem>, now: Long) {
    Column {
        Text("TODAY", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
        if (classes.isEmpty()) {
            Text("No more classes scheduled for today.", color = TextSecondary, fontSize = 14.sp)
        } else {
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.width(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(AccentIndigo))
                    Box(Modifier.width(2.dp).height((classes.size * 100).dp).background(AccentIndigo.copy(alpha = 0.55f)))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("NOW", color = AccentIndigo, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    classes.forEachIndexed { index, item ->
                        Surface(color = SurfaceBento, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    NumberText("${formatEpochTime(item.startsAt)}–${formatEpochTime(item.endsAt)}", fontSize = 12.sp)
                                    Text(item.course.name, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                    Text("${item.course.code} · ${item.course.room}", color = TextSecondary, fontSize = 12.sp)
                                }
                                if (index == 0 && item.startsAt <= now) Text("NEXT", color = AccentIndigo, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(minutes: Int): String = String.format(Locale.getDefault(), "%02d:%02d", minutes / 60, minutes % 60)
private fun formatEpochTime(epochMillis: Long): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(epochMillis))
private fun countdown(milliseconds: Long): String {
    if (milliseconds <= 0L) return "NOW"
    val minutes = (milliseconds + 59_999L) / 60_000L
    val days = minutes / (60 * 24)
    val hours = (minutes / 60) % 24
    val remaining = minutes % 60
    return when { days > 0 -> "${days}d ${hours}h"; hours > 0 -> "${hours}h ${remaining}m"; else -> "$minutes MIN" }
}

@Preview(showBackground = true, backgroundColor = 0xFF07080C)
@Composable
private fun HomeStatePreviewToggles() {
    var populated by remember { mutableStateOf(true) }
    val now = remember { System.currentTimeMillis() }
    val sampleCourse = CourseEntity("preview", "Data Structures", "CS204", "Dr. Rao", "B-204", 14, 18, 0.75, 4)
    val sampleSlot = ScheduleSlotEntity("preview-slot", sampleCourse.id, java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK).let { if (it == 1) 7 else it - 1 }, 600, 650)
    val sampleState = if (populated) HomeState(
        nextClass = HomeClassItem(sampleCourse, sampleSlot, now + 18 * 60_000L, now + 68 * 60_000L),
        todayClasses = listOf(HomeClassItem(sampleCourse, sampleSlot, now + 18 * 60_000L, now + 68 * 60_000L)),
        aggregateAttendance = 77.8,
        weakestCourse = sampleCourse,
        urgentTask = TaskEntity("preview-task", sampleCourse.id, "Submit lab report", "ASSIGNMENT", now + 6 * 60 * 60_000L, 1, false),
        batteryRestrictionsActive = true
    ) else HomeState(pinnedActions = emptyList())
    CampusOsTheme {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { populated = true }) { Text("Populated State") }
                OutlinedButton(onClick = { populated = false }) { Text("Empty First-Run State") }
            }
            HomeScreenContent(sampleState, now, {}, {}, {}, {}, {}, {}, {}, {})
        }
    }
}
