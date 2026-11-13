package com.afraz.campusos.feature_academics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import androidx.room.withTransaction
import com.afraz.campusos.data.AppDatabase
import com.afraz.campusos.data.AttendanceLogEntity
import com.afraz.campusos.data.CourseEntity
import com.afraz.campusos.data.ScheduleSlotEntity
import com.afraz.campusos.data.SemesterGradeEntity
import com.afraz.campusos.data.SyllabusTopicEntity
import com.afraz.campusos.data.TaskEntity
import com.afraz.campusos.core.util.TimetableCodec
import com.afraz.campusos.feature_academics.alarm.CampusAlarmScheduler
import com.afraz.campusos.feature_toolkit.widget.CampusGlanceWidget
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class AcademicsState(
    val selectedDay: Int = 1,
    val courses: List<CourseEntity> = emptyList(),
    val slots: List<ScheduleSlotEntity> = emptyList(),
    val tasks: List<TaskEntity> = emptyList(),
    val topics: List<SyllabusTopicEntity> = emptyList(),
    val grades: List<SemesterGradeEntity> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AcademicsViewModel @Inject constructor(
    private val database: AppDatabase,
    private val alarmScheduler: CampusAlarmScheduler,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val selectedDay = MutableStateFlow(1)
    val state: StateFlow<AcademicsState> = selectedDay.flatMapLatest { day ->
        combine(
            database.courseDao().observeAll(),
            database.scheduleSlotDao().observeAll(),
            database.taskDao().observeAll(),
            database.syllabusTopicDao().observeAll(),
            database.semesterGradeDao().observeAll()
        ) { courses, slots, tasks, topics, grades -> AcademicsState(day, courses, slots, tasks, topics, grades) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AcademicsState())

    init { viewModelScope.launch(Dispatchers.IO) { alarmScheduler.rescheduleAll() } }

    fun selectDay(day: Int) { if (day in 1..7) selectedDay.value = day }

    fun addCourse(name: String, code: String, professor: String, room: String, credits: Int) = viewModelScope.launch(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        database.courseDao().upsert(CourseEntity(id, name.trim(), code.trim(), professor.trim(), room.trim(), credits = credits.coerceAtLeast(0)))
        refreshWidget()
    }

    fun addSlot(courseId: String, day: Int, startMinutes: Int, endMinutes: Int) = viewModelScope.launch(Dispatchers.IO) {
        if (database.courseDao().getById(courseId) == null || day !in 1..7 || startMinutes !in 0..1439 || endMinutes !in 1..1440 || endMinutes <= startMinutes) return@launch
        val slot = ScheduleSlotEntity(UUID.randomUUID().toString(), courseId, day, startMinutes, endMinutes)
        val course = database.courseDao().getById(courseId) ?: return@launch
        database.scheduleSlotDao().upsert(slot)
        alarmScheduler.scheduleClass(slot, course)
        refreshWidget()
    }

    fun deleteSlot(slotId: String) = viewModelScope.launch(Dispatchers.IO) { database.scheduleSlotDao().delete(slotId) }

    fun setTarget(courseId: String, target: Double) = viewModelScope.launch(Dispatchers.IO) {
        if (target in 0.01..1.0) database.courseDao().setTarget(courseId, target)
    }

    fun importTimetable(encoded: String) = viewModelScope.launch(Dispatchers.IO) {
        val bundle = runCatching { TimetableCodec.decode(encoded) }.getOrNull() ?: return@launch
        val idMap = bundle.courses.associate { it.id to UUID.randomUUID().toString() }
        database.withTransaction {
            bundle.courses.forEach { course ->
                database.courseDao().upsert(course.copy(id = idMap.getValue(course.id), attendedClasses = 0, totalClasses = 0))
            }
            bundle.slots.forEach { slot ->
                database.scheduleSlotDao().upsert(slot.copy(id = UUID.randomUUID().toString(), courseId = idMap.getValue(slot.courseId)))
            }
        }
        alarmScheduler.rescheduleAll()
        refreshWidget()
    }

    fun markAttendance(courseId: String, present: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        database.withTransaction {
            database.courseDao().recordAttendance(courseId, if (present) 1 else 0)
            database.attendanceLogDao().insert(AttendanceLogEntity(courseId = courseId, timestamp = System.currentTimeMillis(), status = if (present) "PRESENT" else "ABSENT"))
        }
        refreshWidget()
    }

    fun undoAttendance(courseId: String) = viewModelScope.launch(Dispatchers.IO) {
        database.withTransaction {
            val last = database.attendanceLogDao().latestForCourse(courseId) ?: return@withTransaction
            database.attendanceLogDao().delete(last.logId)
            database.courseDao().undoAttendance(courseId, if (last.status == "PRESENT") 1 else 0)
        }
        refreshWidget()
    }

    fun addTask(title: String, category: String, dueAt: Long, priority: Int, courseId: String?) = viewModelScope.launch(Dispatchers.IO) {
        if (title.isBlank()) return@launch
        val task = TaskEntity(UUID.randomUUID().toString(), courseId, title.trim(), category, dueAt, priority.coerceIn(1, 3), false)
        database.taskDao().upsert(task)
        alarmScheduler.scheduleDeadline(task)
    }

    fun setTaskDone(id: String, done: Boolean) = viewModelScope.launch(Dispatchers.IO) { database.taskDao().setCompleted(id, done) }

    fun addTopic(courseId: String, unit: Int, title: String) = viewModelScope.launch(Dispatchers.IO) {
        if (title.isBlank() || database.courseDao().getById(courseId) == null) return@launch
        database.syllabusTopicDao().upsert(SyllabusTopicEntity(UUID.randomUUID().toString(), courseId, unit.coerceAtLeast(1), title.trim(), false))
    }

    fun setTopicDone(id: String, done: Boolean) = viewModelScope.launch(Dispatchers.IO) { database.syllabusTopicDao().setCompleted(id, done) }

    fun saveGrade(semester: Int, sgpa: Double, credits: Int) = viewModelScope.launch(Dispatchers.IO) {
        if (semester < 1 || sgpa !in 0.0..10.0 || credits < 0) return@launch
        database.semesterGradeDao().upsert(SemesterGradeEntity(semester, sgpa, credits))
    }

    private fun refreshWidget() = viewModelScope.launch(Dispatchers.IO) { CampusGlanceWidget().updateAll(context) }
}
