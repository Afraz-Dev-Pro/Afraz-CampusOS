package com.afraz.campusos.feature_dashboard

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afraz.campusos.core.util.OemBatteryHelper
import com.afraz.campusos.data.AppDatabase
import com.afraz.campusos.data.CourseEntity
import com.afraz.campusos.data.ScheduleSlotEntity
import com.afraz.campusos.data.TaskEntity
import com.afraz.campusos.feature_academics.alarm.CampusAlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class HomeClassItem(val course: CourseEntity, val slot: ScheduleSlotEntity, val startsAt: Long, val endsAt: Long)

data class HomeState(
    val nextClass: HomeClassItem? = null,
    val todayClasses: List<HomeClassItem> = emptyList(),
    val aggregateAttendance: Double? = null,
    val weakestCourse: CourseEntity? = null,
    val urgentTask: TaskEntity? = null,
    val pinnedActions: List<String> = DEFAULT_PINNED,
    val batteryRestrictionsActive: Boolean = false,
    val batteryNoticeDismissed: Boolean = false
) {
    companion object { val DEFAULT_PINNED = listOf("Scan Notes", "Assignments", "Digital ID") }
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    database: AppDatabase,
    private val settings: DataStore<Preferences>,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val pinnedKey = stringSetPreferencesKey("dashboard_pinned_tools")
    private val dismissedKey = booleanPreferencesKey("battery_warning_dismissed")

    val state: StateFlow<HomeState> = combine(
        database.courseDao().observeAll(),
        database.scheduleSlotDao().observeAll(),
        database.taskDao().observeAll(),
        settings.data
    ) { courses, slots, tasks, prefs ->
        val now = System.currentTimeMillis()
        val byCourse = courses.associateBy(CourseEntity::id)
        val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK).let { if (it == Calendar.SUNDAY) 7 else it - 1 }
        val currentDaySlots = slots.filter { it.dayOfWeek == today }
        val todayItems = currentDaySlots.mapNotNull { slot ->
            val course = byCourse[slot.courseId] ?: return@mapNotNull null
            val start = calendarTime(now, slot.startMinutes)
            val end = calendarTime(now, slot.endMinutes)
            HomeClassItem(course, slot, start, end)
        }.filter { it.endsAt > now }.take(2)
        val next = slots.mapNotNull { slot ->
            val course = byCourse[slot.courseId] ?: return@mapNotNull null
            val start = CampusAlarmScheduler.nextClassStart(slot, now - 1)
            HomeClassItem(course, slot, start, start + (slot.endMinutes - slot.startMinutes).coerceAtLeast(0) * 60_000L)
        }.minByOrNull(HomeClassItem::startsAt)
        val logged = courses.filter { it.totalClasses > 0 }
        val total = logged.sumOf(CourseEntity::totalClasses)
        val attended = logged.sumOf(CourseEntity::attendedClasses)
        val urgent = tasks.asSequence()
            .filter { !it.isCompleted && it.dueEpochMillis >= now && it.dueEpochMillis <= now + 48 * 60 * 60_000L }
            .minByOrNull(TaskEntity::dueEpochMillis)
        val catalog = listOf("Scan Notes", "Assignments", "Timetable", "Digital ID", "Finance", "Pomodoro", "PDF Maker")
        val selected = prefs[pinnedKey] ?: HomeState.DEFAULT_PINNED.toSet()
        HomeState(
            nextClass = next,
            todayClasses = todayItems,
            aggregateAttendance = if (total == 0) null else attended * 100.0 / total,
            weakestCourse = logged.minByOrNull { it.attendedClasses.toDouble() / it.totalClasses },
            urgentTask = urgent,
            pinnedActions = catalog.filter { it in selected }.take(3),
            batteryRestrictionsActive = OemBatteryHelper.shouldShow(context),
            batteryNoticeDismissed = prefs[dismissedKey] ?: false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    fun togglePinnedAction(action: String) = viewModelScope.launch(Dispatchers.IO) {
        settings.edit { prefs ->
            val actions = (prefs[pinnedKey] ?: HomeState.DEFAULT_PINNED.toSet()).toMutableSet()
            if (action in actions) actions.remove(action) else if (actions.size < 3) actions.add(action)
            prefs[pinnedKey] = actions
        }
    }

    fun dismissBatteryNotice() = viewModelScope.launch(Dispatchers.IO) {
        settings.edit { it[dismissedKey] = true }
    }

    private fun calendarTime(now: Long, minutes: Int): Long = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, minutes / 60)
        set(Calendar.MINUTE, minutes % 60)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
