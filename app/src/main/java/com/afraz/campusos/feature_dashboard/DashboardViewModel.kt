package com.afraz.campusos.feature_dashboard

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NextClassCard(val course: CourseEntity, val slot: ScheduleSlotEntity, val startsAt: Long)

data class DashboardState(
    val nextClass: NextClassCard? = null,
    val courses: List<CourseEntity> = emptyList(),
    val tasks: List<TaskEntity> = emptyList(),
    val aggregateAttendance: Double? = null,
    val lowestAttendance: CourseEntity? = null,
    val pinnedTools: List<String> = listOf("Scan Notes", "Expense Tracker", "Pomodoro"),
    val showBatteryWarning: Boolean = false,
    val batteryWarningDismissed: Boolean = false,
    val timetableAssetPath: String? = null,
    val timetableAssetMime: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    database: AppDatabase,
    private val settings: DataStore<Preferences>,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val pinnedKey = stringSetPreferencesKey("dashboard_pinned_tools")
    private val dismissedKey = booleanPreferencesKey("battery_warning_dismissed")
    private val timetablePathKey = stringPreferencesKey("timetable_asset_path")
    private val timetableMimeKey = stringPreferencesKey("timetable_asset_mime")
    private val preferences = settings.data

    val state: StateFlow<DashboardState> = combine(
        database.courseDao().observeAll(),
        database.scheduleSlotDao().observeAll(),
        database.taskDao().observeAll(),
        preferences
    ) { courses, slots, tasks, prefs ->
        val now = System.currentTimeMillis()
        val byCourse = courses.associateBy { it.id }
        val next = slots.mapNotNull { slot ->
            val course = byCourse[slot.courseId] ?: return@mapNotNull null
            NextClassCard(course, slot, CampusAlarmScheduler.nextClassStart(slot, now - 1))
        }.minByOrNull(NextClassCard::startsAt)
        val total = courses.sumOf { it.totalClasses }
        val attended = courses.sumOf { it.attendedClasses }
        val logged = courses.filter { it.totalClasses > 0 }
        DashboardState(
            nextClass = next,
            courses = courses,
            tasks = tasks.filter { !it.isCompleted && it.dueEpochMillis in now..(now + 48 * 60 * 60_000L) }.sortedBy(TaskEntity::dueEpochMillis),
            aggregateAttendance = if (total == 0) null else attended * 100.0 / total,
            lowestAttendance = logged.minByOrNull { it.attendedClasses.toDouble() / it.totalClasses },
            pinnedTools = orderedPinnedTools(prefs[pinnedKey] ?: DEFAULT_PINNED.toSet()),
            showBatteryWarning = OemBatteryHelper.shouldShow(context),
            batteryWarningDismissed = prefs[dismissedKey] ?: false,
            timetableAssetPath = prefs[timetablePathKey],
            timetableAssetMime = prefs[timetableMimeKey]
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardState())

    fun togglePinnedTool(tool: String) = viewModelScope.launch(Dispatchers.IO) {
        settings.edit { preferences ->
            val pinned = preferences[pinnedKey]?.toMutableSet() ?: DEFAULT_PINNED.toMutableSet()
            if (tool in pinned) pinned.remove(tool) else if (pinned.size < PIN_LIMIT) pinned.add(tool)
            preferences[pinnedKey] = pinned
        }
    }

    fun dismissBatteryWarning() = viewModelScope.launch(Dispatchers.IO) { settings.edit { it[dismissedKey] = true } }

    companion object {
        const val PIN_LIMIT = 3
        val TOOL_CATALOG = listOf("Scan Notes", "Expense Tracker", "Pomodoro", "Assignments", "Timetable", "Digital ID", "Finance", "PDF Maker")
        val DEFAULT_PINNED = listOf("Scan Notes", "Expense Tracker", "Pomodoro")
        private fun orderedPinnedTools(pinned: Set<String>) = TOOL_CATALOG.filter { it in pinned }.take(PIN_LIMIT)
    }
}
