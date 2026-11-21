package com.afraz.campusos.feature_dashboard

import com.afraz.campusos.ui.theme.LocalCampusColors
import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Palette
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.afraz.campusos.core.designsystem.GlassCard
import com.afraz.campusos.core.designsystem.NumberText
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
    onOpenThemeStudio: () -> Unit = {},
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
        onOpenThemeStudio = onOpenThemeStudio,
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
    onOpenThemeStudio: () -> Unit,
    onDismissBatteryNotice: () -> Unit,
    onOpenBatterySettings: () -> Unit
) {
    val date = remember(now) { SimpleDateFormat("EEEE · d MMMM", Locale.ENGLISH).format(Date(now)).uppercase(Locale.ENGLISH) }
    val hour = remember(now) { java.util.Calendar.getInstance().apply { timeInMillis = now }.get(java.util.Calendar.HOUR_OF_DAY) }
    val greeting = when (hour) {
        in 5..11 -> "Good morning, Afraz"
        in 12..16 -> "Good afternoon, Afraz"
        in 17..21 -> "Good evening, Afraz"
        else -> "Good night, Afraz"
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val screenWidth = maxWidth
        val contentWidth = if (screenWidth >= 700.dp) 760.dp else screenWidth
        LazyColumn(
            modifier = Modifier
                .width(contentWidth)
                .fillMaxHeight()
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .padding(horizontal = if (screenWidth >= 700.dp) 32.dp else 24.dp),
            contentPadding = PaddingValues(top = 14.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Today", color = LocalCampusColors.current.textPrimary, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        IconButton(
                            onClick = onOpenThemeStudio,
                            modifier = Modifier.size(48.dp).clip(CircleShape).background(LocalCampusColors.current.surfacePrimary)
                        ) {
                            Icon(Icons.Outlined.Palette, contentDescription = "Open theme studio", tint = LocalCampusColors.current.accentPrimary)
                        }
                        IconButton(
                            onClick = onOpenProfile,
                            modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Text("A", color = LocalCampusColors.current.accentPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(date, color = LocalCampusColors.current.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Text(greeting, color = LocalCampusColors.current.textPrimary, fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold)
                    Text("Here's what your campus day looks like.", color = LocalCampusColors.current.textSecondary, fontSize = 14.sp)
                }
            }
            item { NextClassPanel(state.nextClass, now, onOpenClasses, onOpenImport) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Semester at a glance", color = LocalCampusColors.current.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    if (screenWidth < 360.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            AttendanceCard(state.aggregateAttendance, state.weakestCourse, state.trackedCourseCount, Modifier.fillMaxWidth())
                            AttentionCard(state.weakestCourse, Modifier.fillMaxWidth())
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                            AttendanceCard(state.aggregateAttendance, state.weakestCourse, state.trackedCourseCount, Modifier.weight(1.15f))
                            AttentionCard(state.weakestCourse, Modifier.weight(0.85f))
                        }
                    }
                }
            }
            item { TodayTimeline(state.todayClasses, now, onOpenClasses) }
            state.urgentTask?.let { task -> item { DeadlineCard(task, now) } }
            item {
                Text(
                    if (state.nextClass == null) "YOUR CAMPUS · Add your schedule to see today's classes here."
                    else "YOUR CAMPUS · Your schedule and attendance stay on this device.",
                    color = LocalCampusColors.current.textSecondary,
                    fontSize = 11.sp
                )
            }
            if (state.pinnedActions.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("QUICK ACTIONS", color = LocalCampusColors.current.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                    GlassCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Background limits are active", color = LocalCampusColors.current.statusWarning, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            IconButton(onClick = onDismissBatteryNotice, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Outlined.Close, contentDescription = "Dismiss battery notice", tint = LocalCampusColors.current.textSecondary)
                            }
                        }
                        Text("Your ${android.os.Build.MANUFACTURER} device may delay class and focus reminders.", color = LocalCampusColors.current.textSecondary, fontSize = 13.sp)
                        OutlinedButton(onClick = onOpenBatterySettings) { Text("Review background access") }
                    }
                }
            }
        }
    }
}

@Composable
private fun NextClassPanel(next: HomeClassItem?, now: Long, onOpenClasses: () -> Unit, onImport: () -> Unit) {
    val colors = LocalCampusColors.current
    Surface(color = colors.heroCardBg, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 24.dp, vertical = 22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(colors.heroBadgeBg))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (next == null) "NEXT CLASS"
                    else "NEXT CLASS · STARTS IN ${countdown(next.startsAt - now).uppercase(Locale.ENGLISH)}",
                    color = colors.heroCardText, fontSize = 11.sp, fontWeight = FontWeight.Bold
                )
            }
            if (next == null) {
                Text("Your next class will appear here.", color = colors.heroCardText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Add a timetable to keep today's plan close.", color = colors.heroCardText.copy(alpha = 0.78f), fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = onOpenClasses, modifier = Modifier.height(48.dp)) { Text("Add timetable") }
                    OutlinedButton(onClick = onImport, modifier = Modifier.height(48.dp)) { Text("Import") }
                }
            } else {
                Text(next.course.name, color = colors.heroCardText, fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold)
                Text("${next.course.code} · ${next.course.professor.ifBlank { "Professor not set" }}", color = colors.heroCardText.copy(alpha = 0.8f), fontSize = 14.sp)
                Box(Modifier.fillMaxWidth().height(1.dp).background(colors.heroCardText.copy(alpha = 0.18f)))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    NumberText("${formatTime(next.slot.startMinutes)} – ${formatTime(next.slot.endMinutes)}", fontSize = 17.sp, color = colors.heroCardText)
                    Spacer(Modifier.weight(1f))
                    Surface(color = colors.heroBadgeBg, shape = RoundedCornerShape(50.dp)) {
                        Text("Room ${next.course.room.ifBlank { "—" }}", color = colors.heroBadgeText, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp))
                    }
                }
                TextButtonLike("Open today's schedule  ›", onOpenClasses)
            }
        }
    }
}

@Composable
private fun TextButtonLike(label: String, onClick: () -> Unit) {
    val colors = LocalCampusColors.current
    androidx.compose.material3.TextButton(onClick = onClick, contentPadding = PaddingValues(0.dp)) {
        Text(label, color = colors.heroCardText.copy(alpha = 0.85f), fontSize = 12.sp)
    }
}

@Composable
private fun AttendanceCard(aggregate: Double?, weakest: CourseEntity?, trackedCount: Int, modifier: Modifier = Modifier) {
    OutlinedWarmCard(modifier) {
        Text("ATTENDANCE", color = LocalCampusColors.current.textSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AttendanceRing(aggregate, 72.dp)
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Overall", color = LocalCampusColors.current.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(if (weakest == null) "No subjects tracked" else "$trackedCount subjects tracked", color = LocalCampusColors.current.textSecondary, fontSize = 11.sp, lineHeight = 14.sp)
            }
        }
        Spacer(Modifier.height(7.dp))
        Text(
            if (aggregate == null) "Log a class to start tracking."
            else if (aggregate >= 75.0) "On track for your 75% goal"
            else "Your attendance needs a little attention",
            color = if (aggregate == null || aggregate >= 75.0) LocalCampusColors.current.textSecondary else LocalCampusColors.current.statusDanger,
            fontSize = 11.sp,
            lineHeight = 14.sp
        )
    }
}

@Composable
private fun AttentionCard(weakest: CourseEntity?, modifier: Modifier = Modifier) {
    OutlinedWarmCard(modifier) {
        Text(if (weakest == null) "ATTENDANCE" else "NEEDS ATTENTION", color = LocalCampusColors.current.textSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        if (weakest == null || weakest.totalClasses == 0) {
            Text("No subject data yet", color = LocalCampusColors.current.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.height(6.dp))
            Text("Mark attendance in Classes to see your recovery plan.", color = LocalCampusColors.current.textSecondary, fontSize = 11.sp, lineHeight = 15.sp)
        } else {
            val ratio = weakest.attendedClasses.toDouble() / weakest.totalClasses
            val percent = ratio * 100.0
            val mustAttend = ceil((weakest.targetPercentage * weakest.totalClasses - weakest.attendedClasses) / (1.0 - weakest.targetPercentage)).toInt().coerceAtLeast(0)
            val statusColor = if (percent >= weakest.targetPercentage * 100) LocalCampusColors.current.statusSafe else LocalCampusColors.current.statusDanger
            Text(weakest.name, color = LocalCampusColors.current.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 2, lineHeight = 18.sp)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberText("${percent.toInt()}%", fontSize = 22.sp, color = statusColor)
                Text(
                    if (percent < weakest.targetPercentage * 100) "Attend next $mustAttend classes to reach ${(weakest.targetPercentage * 100).toInt()}%"
                    else "On track for your ${(weakest.targetPercentage * 100).toInt()}% goal",
                    color = LocalCampusColors.current.textSecondary, fontSize = 10.sp, lineHeight = 13.sp
                )
            }
        }
    }
}

@Composable
private fun OutlinedWarmCard(modifier: Modifier = Modifier, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val colors = LocalCampusColors.current
    Surface(
        color = colors.surfacePrimary,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
        modifier = modifier
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun AttendanceRing(aggregate: Double?, size: Dp) {
    val colors = LocalCampusColors.current
    val progress = (aggregate ?: 0.0).toFloat().div(100f).coerceIn(0f, 1f)
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 7.dp.toPx()
            val inset = stroke / 2
            drawArc(colors.borderSubtle.copy(alpha = 0.35f), -90f, 360f, false, topLeft = Offset(inset, inset), size = Size(this.size.width - stroke, this.size.height - stroke), style = Stroke(stroke, cap = StrokeCap.Round))
            if (aggregate != null) drawArc(colors.accentPrimary, -90f, 360f * progress, false, topLeft = Offset(inset, inset), size = Size(this.size.width - stroke, this.size.height - stroke), style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Text(if (aggregate == null) "—" else "${aggregate.toInt()}%", color = LocalCampusColors.current.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TodayTimeline(classes: List<HomeClassItem>, now: Long, onOpenClasses: () -> Unit) {
    val colors = LocalCampusColors.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Today", color = LocalCampusColors.current.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            androidx.compose.material3.TextButton(onClick = onOpenClasses, contentPadding = PaddingValues(0.dp)) {
                Text("FULL SCHEDULE  ›", color = LocalCampusColors.current.accentPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        if (classes.isEmpty()) {
            Text("No more classes scheduled for today.", color = LocalCampusColors.current.textSecondary, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp))
        } else {
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.width(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(18.dp).clip(CircleShape).background(colors.canvas).padding(4.dp).clip(CircleShape).background(colors.statusDanger))
                    Box(Modifier.width(3.dp).height((classes.size * 92).dp).background(LocalCampusColors.current.statusDanger.copy(alpha = 0.9f)))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    val first = classes.first()
                    val ongoing = first.startsAt <= now && first.endsAt > now
                    Text(if (ongoing) "NOW · class in progress" else "NOW · between classes", color = LocalCampusColors.current.statusDanger, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(if (ongoing) "Ends at ${formatEpochTime(first.endsAt)}" else "Next up in ${countdown(first.startsAt - now).lowercase(Locale.ENGLISH)}", color = LocalCampusColors.current.textSecondary, fontSize = 12.sp)
                    classes.forEach { item ->
                        Column(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                            Text(item.course.name, color = LocalCampusColors.current.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            Text("${formatEpochTime(item.startsAt)} · Room ${item.course.room.ifBlank { "—" }}", color = LocalCampusColors.current.textSecondary, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeadlineCard(task: TaskEntity, now: Long) {
    val colors = LocalCampusColors.current
    Surface(
        color = colors.surfacePrimary,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).clip(CircleShape).background(LocalCampusColors.current.statusDanger))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(task.title, color = LocalCampusColors.current.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text("${task.category.replaceFirstChar { it.titlecase(Locale.ENGLISH) }} due ${if (task.dueEpochMillis - now < 24 * 60 * 60_000L) "today" else "tomorrow"}", color = LocalCampusColors.current.textSecondary, fontSize = 12.sp)
            }
            Text(countdown(task.dueEpochMillis - now), color = LocalCampusColors.current.textPrimary, fontSize = 12.sp)
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

@Preview(showBackground = true)
@Composable
private fun HomeStatePreviewToggles() {
    var populated by remember { mutableStateOf(true) }
    val now = remember { System.currentTimeMillis() }
    val sampleCourse = CourseEntity("preview", "Data Structures", "CSE 204", "Prof. N. Sharma", "B-204", 14, 18, 0.75, 4)
    val sampleSlot = ScheduleSlotEntity("preview-slot", sampleCourse.id, java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK).let { if (it == 1) 7 else it - 1 }, 600, 650)
    val sampleState = if (populated) HomeState(
        nextClass = HomeClassItem(sampleCourse, sampleSlot, now + 18 * 60_000L, now + 68 * 60_000L),
        todayClasses = listOf(HomeClassItem(sampleCourse, sampleSlot, now + 18 * 60_000L, now + 68 * 60_000L)),
        aggregateAttendance = 78.0,
        trackedCourseCount = 4,
        weakestCourse = sampleCourse.copy(name = "Physics", attendedClasses = 10, totalClasses = 14),
        urgentTask = TaskEntity("preview-task", sampleCourse.id, "Lab report due tomorrow", "ASSIGNMENT", now + 6 * 60 * 60_000L, 1, false)
    ) else HomeState(pinnedActions = emptyList())
    CampusOsTheme {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { populated = true }) { Text("Populated State") }
                OutlinedButton(onClick = { populated = false }) { Text("Empty First-Run State") }
            }
            HomeScreenContent(sampleState, now, {}, {}, {}, {}, {}, {}, {}, {}, {})
        }
    }
}

