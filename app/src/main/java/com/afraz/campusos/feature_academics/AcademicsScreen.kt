package com.afraz.campusos.feature_academics

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.afraz.campusos.core.designsystem.BgCanvas
import com.afraz.campusos.core.designsystem.GlassCard
import com.afraz.campusos.core.designsystem.NumberText
import com.afraz.campusos.core.designsystem.StatusDanger
import com.afraz.campusos.core.designsystem.StatusSafe
import com.afraz.campusos.core.designsystem.StatusWarn
import com.afraz.campusos.core.designsystem.TextPrimary
import com.afraz.campusos.core.designsystem.TextSecondary
import com.afraz.campusos.core.util.BunkAdvice
import com.afraz.campusos.core.util.BunkCalculator
import com.afraz.campusos.core.util.TimetableCodec
import com.afraz.campusos.core.util.TimetableQr
import com.afraz.campusos.core.util.CourseGrade
import com.afraz.campusos.core.util.GradeCalculator
import com.afraz.campusos.data.CourseEntity
import com.afraz.campusos.data.TaskEntity
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

@Composable
fun AcademicsScreen(modifier: Modifier = Modifier, viewModel: AcademicsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var courseName by remember { mutableStateOf("") }
    var courseCode by remember { mutableStateOf("") }
    var professor by remember { mutableStateOf("") }
    var room by remember { mutableStateOf("") }
    var credits by remember { mutableStateOf("3") }
    var slotCourse by remember { mutableStateOf("") }
    var slotStart by remember { mutableStateOf("09:00") }
    var slotEnd by remember { mutableStateOf("10:00") }
    var taskTitle by remember { mutableStateOf("") }
    var taskCategory by remember { mutableStateOf("ASSIGNMENT") }
    var taskPriority by remember { mutableIntStateOf(2) }
    var dueDays by remember { mutableStateOf("1") }
    var taskFilter by remember { mutableStateOf("PENDING") }
    var gradeSemester by remember { mutableStateOf("1") }
    var sgpa by remember { mutableStateOf("") }
    var targetCgpa by remember { mutableStateOf("8.0") }
    var totalSemesters by remember { mutableStateOf("8") }
    var courseGrades by remember { mutableStateOf(mapOf<String, String>()) }
    var customGradeScale by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current
    var shareCode by remember { mutableStateOf("") }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var timetableMessage by remember { mutableStateOf("") }
    val qrImagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val decoded = runCatching {
                context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
            }.getOrNull()
            if (decoded != null) {
                val code = runCatching { TimetableQr.decode(decoded) }.getOrNull()
                if (code != null) { viewModel.importTimetable(code); timetableMessage = "Timetable QR imported" }
                else timetableMessage = "Could not read a timetable QR from that image"
                decoded.recycle()
            } else timetableMessage = "Could not open selected image"
        }
    }

    LazyColumn(modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Spacer(Modifier.height(16.dp))
            Text("Academics", color = TextPrimary, fontSize = 29.sp, fontWeight = FontWeight.Bold)
            Text("Your timetable, attendance and progress stay on this device.", color = TextSecondary)
        }
        item {
            GlassCard {
                Text("WEEKLY TIMETABLE", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    dayNames.forEachIndexed { index, day ->
                        Button(onClick = { viewModel.selectDay(index + 1) }, shape = RoundedCornerShape(10.dp)) { Text(day) }
                    }
                }
                Spacer(Modifier.height(8.dp))
                val daySlots = state.slots.filter { it.dayOfWeek == state.selectedDay }
                if (daySlots.isEmpty()) Text("No classes for ${dayNames[state.selectedDay - 1]}.", color = TextSecondary)
                daySlots.forEach { slot ->
                    val course = state.courses.firstOrNull { it.id == slot.courseId }
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(course?.name ?: "Course removed", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            Text("${course?.code.orEmpty()} · ${course?.professor.orEmpty()} · ${course?.room.orEmpty()}", color = TextSecondary, fontSize = 12.sp)
                        }
                        NumberText("${slot.startMinutes.toTime()}–${slot.endMinutes.toTime()}", fontSize = 12.sp)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        runCatching { TimetableCodec.encode(state.courses, state.slots) }
                            .onSuccess { code -> shareCode = code; qrBitmap = runCatching { TimetableQr.create(code) }.getOrNull(); timetableMessage = "Share code ready" }
                            .onFailure { timetableMessage = it.message ?: "Unable to create timetable code" }
                    }, enabled = state.courses.isNotEmpty() && state.slots.isNotEmpty()) { Text("Share / QR") }
                    Button(onClick = { qrImagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Text("Import QR image") }
                }
                if (shareCode.isNotBlank()) {
                    qrBitmap?.let { Image(it.asImageBitmap(), contentDescription = "Timetable QR code", modifier = Modifier.fillMaxWidth().height(220.dp)) }
                    OutlinedTextField(shareCode, { shareCode = it }, label = { Text("Paste this URL-safe share code") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { viewModel.importTimetable(shareCode); timetableMessage = "Timetable import started" }) { Text("Import code") }
                        Button(onClick = {
                            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, shareCode), "Share timetable"))
                        }) { Text("Send") }
                    }
                }
                if (timetableMessage.isNotBlank()) Text(timetableMessage, color = TextSecondary)
            }
        }
        item {
            GlassCard {
                Text("ADD COURSE", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(courseName, { courseName = it }, label = { Text("Course name") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(courseCode, { courseCode = it }, label = { Text("Code") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(credits, { credits = it.filter(Char::isDigit) }, label = { Text("Credits") }, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(professor, { professor = it }, label = { Text("Professor") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(room, { room = it }, label = { Text("Room") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = {
                    if (courseName.isNotBlank() && courseCode.isNotBlank()) {
                        viewModel.addCourse(courseName, courseCode, professor, room, credits.toIntOrNull() ?: 0)
                        courseName = ""; courseCode = ""; professor = ""; room = ""
                    }
                }) { Text("Save course") }
            }
        }
        if (state.courses.isNotEmpty()) item {
            GlassCard {
                Text("ADD TIMETABLE SLOT", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                state.courses.forEach { course ->
                    Button(onClick = { slotCourse = course.id }, enabled = slotCourse != course.id, modifier = Modifier.padding(end = 6.dp)) { Text(course.code) }
                }
                OutlinedTextField(slotStart, { slotStart = it }, label = { Text("Start time (HH:mm)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(slotEnd, { slotEnd = it }, label = { Text("End time (HH:mm)") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = {
                    val start = slotStart.toMinutesOrNull()
                    val end = slotEnd.toMinutesOrNull()
                    if (slotCourse.isNotBlank() && start != null && end != null) viewModel.addSlot(slotCourse, state.selectedDay, start, end)
                }) { Text("Add to ${dayNames[state.selectedDay - 1]}") }
            }
        }
        item { Text("BUNK METER", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
        items(state.courses, key = { "course-${it.id}" }) { course ->
            AttendanceCard(course, onPresent = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); viewModel.markAttendance(course.id, true) }, onAbsent = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); viewModel.markAttendance(course.id, false) }, onUndo = { viewModel.undoAttendance(course.id) }, onTargetChange = { viewModel.setTarget(course.id, it) })
        }
        item {
            GlassCard {
                Text("ASSIGNMENT / EXAM", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(taskTitle, { taskTitle = it }, label = { Text("Task title") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(onClick = { taskCategory = "ASSIGNMENT" }, enabled = taskCategory != "ASSIGNMENT") { Text("Assignment") }
                    Button(onClick = { taskCategory = "EXAM" }, enabled = taskCategory != "EXAM") { Text("Exam") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(1 to "HIGH", 2 to "MED", 3 to "LOW").forEach { (value, label) ->
                        Button(onClick = { taskPriority = value }, enabled = taskPriority != value) { Text(label) }
                    }
                }
                OutlinedTextField(dueDays, { dueDays = it.filter(Char::isDigit).take(3) }, label = { Text("Due in days") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = {
                    val days = dueDays.toIntOrNull() ?: 1
                    viewModel.addTask(taskTitle, taskCategory, System.currentTimeMillis() + days.coerceAtLeast(0) * 86_400_000L, taskPriority, null)
                    taskTitle = ""
                }, enabled = taskTitle.isNotBlank()) { Text("Save task") }
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf("ALL", "ASSIGNMENTS", "EXAMS", "PENDING", "DONE").forEach { filter ->
                        Button(onClick = { taskFilter = filter }, enabled = taskFilter != filter) { Text(filter) }
                    }
                }
                state.tasks.filter { task -> when (taskFilter) {
                    "ASSIGNMENTS" -> task.category == "ASSIGNMENT"
                    "EXAMS" -> task.category == "EXAM"
                    "PENDING" -> !task.isCompleted
                    "DONE" -> task.isCompleted
                    else -> true
                } }.forEach { task -> TaskRow(task, onDone = { viewModel.setTaskDone(task.id, !task.isCompleted) }) }
            }
        }
        items(state.courses, key = { "syllabus-${it.id}" }) { course ->
            val topics = state.topics.filter { it.courseId == course.id }
            var topicText by remember(course.id) { mutableStateOf("") }
            var topicUnit by remember(course.id) { mutableStateOf("1") }
            GlassCard {
                Text("${course.name.uppercase()} · SYLLABUS", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                val progress = if (topics.isEmpty()) 0f else topics.count { it.isCompleted }.toFloat() / topics.size
                Text("${(progress * 100).toInt()}% revised", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                CircularProgressIndicator(progress = { progress }, color = StatusSafe)
                topics.forEach { topic ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(topic.isCompleted, onCheckedChange = { viewModel.setTopicDone(topic.id, it) })
                        Text("Unit ${topic.unitNumber} · ${topic.topicTitle}", color = TextPrimary)
                    }
                }
                OutlinedTextField(topicText, { topicText = it }, label = { Text("Add a syllabus topic") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(topicUnit, { topicUnit = it.filter(Char::isDigit).take(2) }, label = { Text("Unit number") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = { viewModel.addTopic(course.id, topicUnit.toIntOrNull() ?: 1, topicText); topicText = "" }, enabled = topicText.isNotBlank()) { Text("Add topic") }
            }
        }
        item {
            GlassCard {
                Text("SGPA & TARGET CGPA", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(gradeSemester, { gradeSemester = it.filter(Char::isDigit) }, label = { Text("Semester") }, modifier = Modifier.fillMaxWidth())
                state.courses.forEach { course ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("${course.code} · ${course.credits} cr", color = TextPrimary, modifier = Modifier.weight(1f))
                        OutlinedTextField(courseGrades[course.id].orEmpty(), { courseGrades = courseGrades + (course.id to it.uppercase().take(2)) }, label = { Text("Grade") }, modifier = Modifier.weight(1f))
                    }
                }
                OutlinedTextField(customGradeScale, { customGradeScale = it }, label = { Text("Custom scale: O=10,E=9,A=8,B=7,C=6,D=5,F=0") }, modifier = Modifier.fillMaxWidth())
                val scale = GradeCalculator.parseScale(customGradeScale)
                val calculatedSgpa = scale?.let { GradeCalculator.sgpa(state.courses.map { course -> CourseGrade(course.credits, courseGrades[course.id].orEmpty()) }, it) }
                Text(calculatedSgpa?.let { "Calculated SGPA: ${"%.2f".format(it)}" } ?: "Enter course grades to calculate SGPA", color = TextSecondary)
                OutlinedTextField(sgpa, { sgpa = it }, label = { Text("Or enter SGPA manually (0–10)") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = {
                    val value = calculatedSgpa ?: sgpa.toDoubleOrNull() ?: -1.0
                    viewModel.saveGrade(gradeSemester.toIntOrNull() ?: 1, value, state.courses.sumOf { it.credits })
                }) { Text("Save semester grade") }
                Text("Recorded: ${state.grades.sortedBy { it.semesterNumber }.joinToString { "S${it.semesterNumber} ${"%.2f".format(it.sgpa)}" }.ifBlank { "none" }}", color = TextSecondary)
                OutlinedTextField(targetCgpa, { targetCgpa = it }, label = { Text("Desired final CGPA") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(totalSemesters, { totalSemesters = it.filter(Char::isDigit) }, label = { Text("Total semesters") }, modifier = Modifier.fillMaxWidth())
                val count = totalSemesters.toIntOrNull() ?: 0
                val target = targetCgpa.toDoubleOrNull() ?: 0.0
                if (count > 0 && target in 0.0..10.0 && state.grades.size <= count) {
                    val remaining = count - state.grades.size
                    val required = if (remaining == 0) 0.0 else ((target * count - state.grades.sumOf { it.sgpa }) / remaining)
                    Text(if (remaining == 0) "All semesters are recorded" else "Required average in next $remaining semesters: ${"%.2f".format(required)} SGPA", color = if (required > 10.0) StatusDanger else TextPrimary)
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun AttendanceCard(course: CourseEntity, onPresent: () -> Unit, onAbsent: () -> Unit, onUndo: () -> Unit, onTargetChange: (Double) -> Unit) {
    var targetText by remember(course.id) { mutableStateOf((course.targetPercentage * 100).toInt().toString()) }
    val summary = BunkCalculator.calculate(course.attendedClasses, course.totalClasses, course.targetPercentage)
    val tint = when (val advice = summary.advice) {
        BunkAdvice.NoClasses -> TextSecondary
        BunkAdvice.OnTheEdge -> StatusWarn
        is BunkAdvice.SafeToBunk -> StatusSafe
        is BunkAdvice.MustAttend -> StatusDanger
    }
    val message = when (val advice = summary.advice) {
        BunkAdvice.NoClasses -> "No classes logged yet"
        BunkAdvice.OnTheEdge -> "On the edge! Don't bunk next class"
        is BunkAdvice.SafeToBunk -> "Safe to bunk next ${advice.classes} classes"
        is BunkAdvice.MustAttend -> "Attend next ${advice.classes} classes for ${advice.targetPercent}%"
    }
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator(progress = { ((summary.percentage ?: 0.0) / 100.0).toFloat().coerceIn(0f, 1f) }, color = tint)
            Column(Modifier.weight(1f)) {
                Text(course.name, color = TextPrimary, fontWeight = FontWeight.Bold)
                Text("${course.code} · ${course.attendedClasses}/${course.totalClasses}", color = TextSecondary)
                Text(message, color = tint, fontSize = 12.sp)
            }
            NumberText(summary.percentage?.let { "%.1f%%".format(it) } ?: "—", fontSize = 14.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(onClick = onPresent) { Text("+ Present") }
            Button(onClick = onAbsent) { Text("+ Absent") }
            Button(onClick = onUndo, enabled = course.totalClasses > 0) { Text("Undo") }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(targetText, { targetText = it.filter(Char::isDigit).take(3) }, label = { Text("Target %") }, modifier = Modifier.weight(1f))
            OutlinedButton(onClick = { targetText.toIntOrNull()?.let { if (it in 1..100) onTargetChange(it / 100.0) } }) { Text("Set target") }
        }
    }
}

@Composable
private fun TaskRow(task: TaskEntity, onDone: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(task.isCompleted, onCheckedChange = { onDone() })
        Column(Modifier.weight(1f)) {
            Text(task.title, color = TextPrimary, fontWeight = FontWeight.SemiBold)
            val remaining = task.dueEpochMillis - System.currentTimeMillis()
            val countdown = if (remaining < 0) "Overdue" else "${remaining / 86_400_000}d ${(remaining / 3_600_000) % 24}h left"
            val priority = when (task.priority) { 1 -> "HIGH"; 2 -> "MED"; else -> "LOW" }
            Text("${task.category} · $priority · $countdown · ${DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(task.dueEpochMillis))}", color = when (task.priority) { 1 -> StatusDanger; 2 -> StatusWarn; else -> TextSecondary }, fontSize = 12.sp)
        }
    }
}

private fun Int.toTime(): String = "%02d:%02d".format(this / 60, this % 60)
private fun String.toMinutesOrNull(): Int? = runCatching {
    val parts = split(":")
    require(parts.size == 2)
    val hour = parts[0].toInt()
    val minute = parts[1].toInt()
    require(hour in 0..23 && minute in 0..59)
    hour * 60 + minute
}.getOrNull()
