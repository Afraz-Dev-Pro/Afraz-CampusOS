package com.afraz.campusos.feature_academics

import com.afraz.campusos.ui.theme.LocalCampusColors
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.afraz.campusos.core.designsystem.GlassCard
import com.afraz.campusos.core.designsystem.NumberText
import com.afraz.campusos.core.util.BunkAdvice
import com.afraz.campusos.core.util.BunkCalculator
import com.afraz.campusos.core.util.TimetableCodec
import com.afraz.campusos.core.util.TimetableQr
import com.afraz.campusos.core.util.CourseGrade
import com.afraz.campusos.core.util.GradeCalculator
import com.afraz.campusos.data.CourseEntity
import com.afraz.campusos.data.TaskEntity
import com.afraz.campusos.data.SyllabusTopicEntity
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AcademicsScreen(modifier: Modifier = Modifier, viewModel: ClassesViewModel = hiltViewModel()) =
    ClassesScreen(modifier, viewModel)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ClassesScreen(modifier: Modifier = Modifier, viewModel: ClassesViewModel = hiltViewModel()) {
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
    var selectedCourseId by remember { mutableStateOf("") }
    var showCourseSheet by remember { mutableStateOf(false) }
    var showAcademicTools by remember { mutableStateOf(false) }
    var gradeSemester by remember { mutableStateOf("1") }
    var sgpa by remember { mutableStateOf("") }
    var targetCgpa by remember { mutableStateOf("8.0") }
    var totalSemesters by remember { mutableStateOf("8") }
    var courseGrades by remember { mutableStateOf(mapOf<String, String>()) }
    var customGradeScale by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current
    val expanded = LocalConfiguration.current.screenWidthDp >= 600
    LaunchedEffect(state.courses) {
        if (state.courses.none { it.id == selectedCourseId }) selectedCourseId = state.courses.firstOrNull()?.id.orEmpty()
    }
    var shareCode by remember { mutableStateOf("") }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var timetableMessage by remember { mutableStateOf("") }
    var pyqSearch by remember { mutableStateOf("") }
    val timetableAssetPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) { viewModel.saveTimetableAsset(uri); timetableMessage = "Timetable file saved for Dashboard" }
    }
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
            Text("Classes", color = LocalCampusColors.current.textPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Your timetable, attendance and progress stay on this device.", color = LocalCampusColors.current.textSecondary)
        }
        item {
            GlassCard {
                Text("WEEKLY TIMETABLE", color = LocalCampusColors.current.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("Choose a day, add courses, then add one or more time slots. Save Course stores the subject details so they can be scheduled and tracked for attendance.", color = LocalCampusColors.current.textSecondary, fontSize = 12.sp)
                OutlinedButton(onClick = { timetableAssetPicker.launch(arrayOf("image/*", "application/pdf")) }) { Text("Add timetable image or PDF") }
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    items(dayNames.take(6).size) { index ->
                        val dayNumber = index + 1
                        Button(
                            onClick = { viewModel.selectDay(dayNumber) },
                            modifier = Modifier.heightIn(min = 48.dp).width(56.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = if (state.selectedDay == dayNumber) LocalCampusColors.current.accentPrimary else LocalCampusColors.current.surfaceElevated
                            )
                        ) { Text(dayNames[index], maxLines = 1) }
                    }
                }
                Spacer(Modifier.height(8.dp))
                val daySlots = state.slots.filter { it.dayOfWeek == state.selectedDay }
                if (daySlots.isEmpty()) Text("No classes for ${dayNames[state.selectedDay - 1]}.", color = LocalCampusColors.current.textSecondary)
                daySlots.forEach { slot ->
                    val course = state.courses.firstOrNull { it.id == slot.courseId }
                    Surface(color = LocalCampusColors.current.surfacePrimary, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            NumberText("${slot.startMinutes.toTime()}–${slot.endMinutes.toTime()}", fontSize = 12.sp)
                            Column(Modifier.weight(1f)) {
                                Text(course?.name ?: "Course removed", color = LocalCampusColors.current.textPrimary, fontWeight = FontWeight.SemiBold)
                                Text("${course?.code.orEmpty()} · ${course?.professor.orEmpty()}", color = LocalCampusColors.current.textSecondary, fontSize = 12.sp)
                            }
                            Surface(color = LocalCampusColors.current.surfaceElevated, shape = RoundedCornerShape(8.dp)) {
                                Text(course?.room?.ifBlank { "Room —" } ?: "Room —", color = LocalCampusColors.current.textSecondary, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp))
                            }
                        }
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
                if (timetableMessage.isNotBlank()) Text(timetableMessage, color = LocalCampusColors.current.textSecondary)
            }
        }
        item {
            GlassCard {
                Text("ADD COURSE", color = LocalCampusColors.current.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                        timetableMessage = "Course saved. Add a time slot below to place it on your timetable."
                        courseName = ""; courseCode = ""; professor = ""; room = ""
                    }
                }, enabled = courseName.isNotBlank() && courseCode.isNotBlank()) { Text("Save course") }
            }
        }
        if (state.courses.isNotEmpty()) item {
            GlassCard {
                Text("ADD TIMETABLE SLOT", color = LocalCampusColors.current.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.courses.forEach { course ->
                        Button(onClick = { slotCourse = course.id }, enabled = slotCourse != course.id) { Text(course.code) }
                    }
                }
                OutlinedTextField(slotStart, { slotStart = it }, label = { Text("Start time (HH:mm)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(slotEnd, { slotEnd = it }, label = { Text("End time (HH:mm)") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = {
                    val start = slotStart.toMinutesOrNull()
                    val end = slotEnd.toMinutesOrNull()
                    if (slotCourse.isNotBlank() && start != null && end != null && end > start) {
                        viewModel.addSlot(slotCourse, state.selectedDay, start, end)
                        timetableMessage = "Added to ${dayNames[state.selectedDay - 1]}"
                    } else timetableMessage = "Choose a course and enter a valid end time after the start time."
                }, enabled = slotCourse.isNotBlank()) { Text("Add to ${dayNames[state.selectedDay - 1]}") }
                Text("Selected day: ${dayNames[state.selectedDay - 1]}", color = LocalCampusColors.current.textSecondary)
            }
        }
        if (expanded && state.courses.isNotEmpty()) {
            item(key = "course-split-pane") {
                CourseSplitPane(
                    courses = state.courses,
                    topics = state.topics,
                    selectedId = selectedCourseId,
                    onSelect = { selectedCourseId = it }
                )
            }
        }
        item { Text("COURSES & ATTENDANCE", color = LocalCampusColors.current.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
        items(state.courses, key = { "course-${it.id}" }) { course ->
            AttendanceCard(
                course,
                onPresent = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); viewModel.markAttendance(course.id, true) },
                onAbsent = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); viewModel.markAttendance(course.id, false) },
                onUndo = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); viewModel.undoAttendance(course.id) },
                onTargetChange = { viewModel.setTarget(course.id, it) },
                onDetails = { selectedCourseId = course.id; showCourseSheet = true }
            )
        }
        item {
            GlassCard {
                Text("PYQ VAULT", color = LocalCampusColors.current.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(pyqSearch, { pyqSearch = it }, label = { Text("Search subject, code or semester") }, modifier = Modifier.fillMaxWidth())
                val papers = state.pyqs.filter { paper -> pyqSearch.isBlank() || paper.subjectName.contains(pyqSearch, true) || paper.courseCode.contains(pyqSearch, true) || paper.semester.toString() == pyqSearch }
                if (papers.isEmpty()) Text("No papers match this search yet.", color = LocalCampusColors.current.textSecondary)
                papers.forEach { paper ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(paper.subjectName, color = LocalCampusColors.current.textPrimary, fontWeight = FontWeight.SemiBold)
                            Text("${paper.courseCode} · semester ${paper.semester} · ${paper.year}", color = LocalCampusColors.current.textSecondary, fontSize = 12.sp)
                        }
                        Button(onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(paper.pdfUrl))) } }) { Text("Open PDF") }
                    }
                }
            }
        }
        item {
            GlassCard {
                Text("ASSIGNMENTS & EXAMS", color = LocalCampusColors.current.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf("PENDING", "COMPLETED", "ASSIGNMENTS", "EXAMS").forEach { filter ->
                        Button(onClick = { taskFilter = filter }, enabled = taskFilter != filter, modifier = Modifier.heightIn(min = 48.dp)) { Text(filter) }
                    }
                }
                state.tasks.filter { task -> when (taskFilter) {
                    "ASSIGNMENTS" -> task.category == "ASSIGNMENT"
                    "EXAMS" -> task.category == "EXAM"
                    "PENDING" -> !task.isCompleted
                    "COMPLETED" -> task.isCompleted
                    else -> true
                } }.forEach { task -> TaskRow(task, onDone = { viewModel.setTaskDone(task.id, !task.isCompleted) }) }
            }
        }
        items(state.courses, key = { "syllabus-${it.id}" }) { course ->
            val topics = state.topics.filter { it.courseId == course.id }
            var topicText by remember(course.id) { mutableStateOf("") }
            var topicUnit by remember(course.id) { mutableStateOf("1") }
            GlassCard {
                Text("${course.name.uppercase()} · SYLLABUS", color = LocalCampusColors.current.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                val progress = if (topics.isEmpty()) 0f else topics.count { it.isCompleted }.toFloat() / topics.size
                Text("${(progress * 100).toInt()}% revised", color = LocalCampusColors.current.textPrimary, fontWeight = FontWeight.SemiBold)
                CircularProgressIndicator(progress = { progress }, color = LocalCampusColors.current.statusSafe)
                topics.forEach { topic ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(topic.isCompleted, onCheckedChange = { viewModel.setTopicDone(topic.id, it) })
                        Text("Unit ${topic.unitNumber} · ${topic.topicTitle}", color = LocalCampusColors.current.textPrimary)
                    }
                }
                OutlinedTextField(topicText, { topicText = it }, label = { Text("Add a syllabus topic") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(topicUnit, { topicUnit = it.filter(Char::isDigit).take(2) }, label = { Text("Unit number") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = { viewModel.addTopic(course.id, topicUnit.toIntOrNull() ?: 1, topicText); topicText = "" }, enabled = topicText.isNotBlank()) { Text("Add topic") }
            }
        }
        item {
            GlassCard {
                Text("ACADEMIC TOOLS", color = LocalCampusColors.current.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("Keep grade planning separate from your daily timetable.", color = LocalCampusColors.current.textSecondary, fontSize = 13.sp)
                Button(onClick = { showAcademicTools = true }) { Text("Open SGPA & CGPA calculator") }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
    if (!expanded && showCourseSheet && state.courses.isNotEmpty()) {
        val selected = state.courses.firstOrNull { it.id == selectedCourseId } ?: state.courses.first()
        ModalBottomSheet(onDismissRequest = { showCourseSheet = false }) {
            CourseDetail(selected, state.topics.filter { it.courseId == selected.id }, Modifier.fillMaxWidth().padding(20.dp))
        }
    }
    if (showAcademicTools) {
        ModalBottomSheet(onDismissRequest = { showAcademicTools = false }) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("SGPA & TARGET CGPA", color = LocalCampusColors.current.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(gradeSemester, { gradeSemester = it.filter(Char::isDigit) }, label = { Text("Semester") }, modifier = Modifier.fillMaxWidth())
                state.courses.forEach { course ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("${course.code} · ${course.credits} cr", color = LocalCampusColors.current.textPrimary, modifier = Modifier.weight(1f))
                        OutlinedTextField(courseGrades[course.id].orEmpty(), { courseGrades = courseGrades + (course.id to it.uppercase().take(2)) }, label = { Text("Grade") }, modifier = Modifier.weight(1f))
                    }
                }
                OutlinedTextField(customGradeScale, { customGradeScale = it }, label = { Text("Custom scale: O=10,E=9,A=8,B=7,C=6,D=5,F=0") }, modifier = Modifier.fillMaxWidth())
                val scale = GradeCalculator.parseScale(customGradeScale)
                val calculatedSgpa = scale?.let { GradeCalculator.sgpa(state.courses.map { course -> CourseGrade(course.credits, courseGrades[course.id].orEmpty()) }, it) }
                Text(calculatedSgpa?.let { "Calculated SGPA: ${"%.2f".format(it)}" } ?: "Enter course grades to calculate SGPA", color = LocalCampusColors.current.textSecondary)
                OutlinedTextField(sgpa, { sgpa = it }, label = { Text("Or enter SGPA manually (0–10)") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = {
                    val value = calculatedSgpa ?: sgpa.toDoubleOrNull() ?: -1.0
                    viewModel.saveGrade(gradeSemester.toIntOrNull() ?: 1, value, state.courses.sumOf { it.credits })
                }) { Text("Save semester grade") }
                Text("Recorded: ${state.grades.sortedBy { it.semesterNumber }.joinToString { "S${it.semesterNumber} ${"%.2f".format(it.sgpa)}" }.ifBlank { "none" }}", color = LocalCampusColors.current.textSecondary)
                OutlinedTextField(targetCgpa, { targetCgpa = it }, label = { Text("Desired final CGPA") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(totalSemesters, { totalSemesters = it.filter(Char::isDigit) }, label = { Text("Total semesters") }, modifier = Modifier.fillMaxWidth())
                val count = totalSemesters.toIntOrNull() ?: 0
                val target = targetCgpa.toDoubleOrNull() ?: 0.0
                if (count > 0 && target in 0.0..10.0 && state.grades.size <= count) {
                    val remaining = count - state.grades.size
                    val required = if (remaining == 0) 0.0 else ((target * count - state.grades.sumOf { it.sgpa }) / remaining)
                    Text(if (remaining == 0) "All semesters are recorded" else "Required average in next $remaining semesters: ${"%.2f".format(required)} SGPA", color = if (required > 10.0) LocalCampusColors.current.statusDanger else LocalCampusColors.current.textPrimary)
                }
            }
        }
    }
}

@Composable
private fun AttendanceCard(course: CourseEntity, onPresent: () -> Unit, onAbsent: () -> Unit, onUndo: () -> Unit, onTargetChange: (Double) -> Unit, onDetails: () -> Unit) {
    var targetText by remember(course.id) { mutableStateOf((course.targetPercentage * 100).toInt().toString()) }
    val summary = BunkCalculator.calculate(course.attendedClasses, course.totalClasses, course.targetPercentage)
    val tint = when (val advice = summary.advice) {
        BunkAdvice.NoClasses -> LocalCampusColors.current.textSecondary
        BunkAdvice.OnTheEdge -> LocalCampusColors.current.statusWarning
        is BunkAdvice.SafeToBunk -> LocalCampusColors.current.statusSafe
        is BunkAdvice.MustAttend -> LocalCampusColors.current.statusDanger
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
                Text(course.name, color = LocalCampusColors.current.textPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onDetails))
                Text("${course.code} · ${course.attendedClasses}/${course.totalClasses}", color = LocalCampusColors.current.textSecondary)
                Text(message, color = tint, fontSize = 12.sp)
            }
            NumberText(summary.percentage?.let { "%.1f%%".format(it) } ?: "—", fontSize = 14.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(onClick = onPresent, modifier = Modifier.weight(1f).heightIn(min = 48.dp), contentPadding = PaddingValues(horizontal = 4.dp)) { Text("Present", fontSize = 11.sp, maxLines = 1) }
            Button(onClick = onAbsent, modifier = Modifier.weight(1f).heightIn(min = 48.dp), contentPadding = PaddingValues(horizontal = 4.dp)) { Text("Absent", fontSize = 11.sp, maxLines = 1) }
            Button(onClick = onUndo, enabled = course.totalClasses > 0, modifier = Modifier.weight(1f).heightIn(min = 48.dp), contentPadding = PaddingValues(horizontal = 4.dp)) { Text("Undo", fontSize = 11.sp, maxLines = 1) }
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
            Text(task.title, color = LocalCampusColors.current.textPrimary, fontWeight = FontWeight.SemiBold)
            val remaining = task.dueEpochMillis - System.currentTimeMillis()
            val dueDate = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(task.dueEpochMillis))
            val countdown = if (remaining < 0) "${task.title} is past due · Due $dueDate · Mark complete when finished" else "${remaining / 86_400_000}d ${(remaining / 3_600_000) % 24}h left · Due $dueDate"
            val priority = when (task.priority) { 1 -> "HIGH"; 2 -> "MED"; else -> "LOW" }
            Text("${task.category} · $priority · $countdown", color = when { remaining < 0 -> LocalCampusColors.current.statusDanger; remaining < 48 * 60 * 60_000L -> LocalCampusColors.current.statusWarning; task.priority == 1 -> LocalCampusColors.current.statusDanger; task.priority == 2 -> LocalCampusColors.current.statusWarning; else -> LocalCampusColors.current.textSecondary }, fontSize = 12.sp)
        }
    }
}

@Composable
private fun CourseSplitPane(courses: List<CourseEntity>, topics: List<SyllabusTopicEntity>, selectedId: String, onSelect: (String) -> Unit) {
    val selected = courses.firstOrNull { it.id == selectedId } ?: courses.first()
    GlassCard {
        Text("COURSE DETAILS", color = LocalCampusColors.current.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(0.8f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                courses.forEach { course ->
                    Surface(
                        color = if (course.id == selected.id) LocalCampusColors.current.accentPrimary.copy(alpha = 0.22f) else LocalCampusColors.current.surfacePrimary,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { onSelect(course.id) }
                    ) {
                        Column(Modifier.padding(10.dp)) {
                            Text(course.name, color = LocalCampusColors.current.textPrimary, fontWeight = FontWeight.SemiBold)
                            Text(course.code, color = LocalCampusColors.current.textSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }
            CourseDetail(selected, topics.filter { it.courseId == selected.id }, Modifier.weight(1.2f))
        }
    }
}

@Composable
private fun CourseDetail(course: CourseEntity, topics: List<SyllabusTopicEntity>, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(course.name, color = LocalCampusColors.current.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("Professor · ${course.professor.ifBlank { "Not set" }}", color = LocalCampusColors.current.textSecondary)
        Text("Room · ${course.room.ifBlank { "Not set" }}", color = LocalCampusColors.current.textSecondary)
        val attendance = com.afraz.campusos.core.util.BunkCalculator.calculate(course.attendedClasses, course.totalClasses, course.targetPercentage)
        Text("Attendance · ${attendance.percentage?.let { "%.1f%%".format(it) } ?: "No classes logged"} (${course.attendedClasses}/${course.totalClasses})", color = LocalCampusColors.current.textPrimary)
        val progress = if (topics.isEmpty()) 0f else topics.count { it.isCompleted }.toFloat() / topics.size
        Text("Syllabus progress · ${(progress * 100).toInt()}%", color = LocalCampusColors.current.textPrimary)
        androidx.compose.material3.LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
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

