package com.afraz.campusos.feature_academics.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.afraz.campusos.data.AppDatabase
import com.afraz.campusos.data.CourseEntity
import com.afraz.campusos.data.ScheduleSlotEntity
import com.afraz.campusos.data.TaskEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CampusAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase
) {
    suspend fun rescheduleAll() {
        val courses = database.courseDao().getAll().associateBy { it.id }
        database.scheduleSlotDao().getAll().forEach { slot ->
            courses[slot.courseId]?.let { scheduleClass(slot, it) }
        }
        database.taskDao().getAll().filterNot { it.isCompleted }.forEach(::scheduleDeadline)
    }

    fun scheduleClass(slot: ScheduleSlotEntity, course: CourseEntity, afterMillis: Long = System.currentTimeMillis() - 1) {
        val start = nextClassStart(slot, afterMillis)
        val triggerAt = maxOf(System.currentTimeMillis() + 1_000, start - FIFTEEN_MINUTES)
        scheduleExact(triggerAt, classPendingIntent(context, slot, course, start))
    }

    fun scheduleDeadline(task: TaskEntity) {
        if (task.isCompleted || task.dueEpochMillis <= System.currentTimeMillis()) return
        val triggerAt = maxOf(System.currentTimeMillis() + 1_000, task.dueEpochMillis - FORTY_EIGHT_HOURS)
        scheduleExact(triggerAt, deadlinePendingIntent(context, task))
    }

    private fun scheduleExact(triggerAt: Long, pending: PendingIntent) {
        val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        try {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        } catch (_: SecurityException) {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    companion object {
        const val EXTRA_SLOT_ID = "class_slot_id"
        const val EXTRA_COURSE_NAME = "class_course_name"
        const val EXTRA_CLASS_START = "class_start_millis"
        const val EXTRA_TASK_ID = "deadline_task_id"
        const val EXTRA_TASK_TITLE = "deadline_task_title"
        const val EXTRA_TASK_DUE = "deadline_task_due"
        private const val FIFTEEN_MINUTES = 15 * 60_000L
        private const val FORTY_EIGHT_HOURS = 48 * 60 * 60_000L

        fun nextClassStart(slot: ScheduleSlotEntity, afterMillis: Long): Long {
            val base = Calendar.getInstance().apply { timeInMillis = afterMillis }
            val currentDay = base.get(Calendar.DAY_OF_WEEK).let { if (it == Calendar.SUNDAY) 7 else it - 1 }
            val daysAhead = (slot.dayOfWeek - currentDay + 7) % 7
            val target = (base.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, daysAhead)
                set(Calendar.HOUR_OF_DAY, slot.startMinutes / 60)
                set(Calendar.MINUTE, slot.startMinutes % 60)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= afterMillis) add(Calendar.DAY_OF_YEAR, 7)
            }
            return target.timeInMillis
        }

        fun classPendingIntent(context: Context, slot: ScheduleSlotEntity, course: CourseEntity, classStart: Long): PendingIntent {
            val intent = Intent(context, ClassAlarmReceiver::class.java).apply {
                action = "${context.packageName}.CLASS_ALARM"
                data = Uri.parse("campusos://class/${Uri.encode(slot.id)}")
                putExtra(EXTRA_SLOT_ID, slot.id)
                putExtra(EXTRA_COURSE_NAME, course.name)
                putExtra(EXTRA_CLASS_START, classStart)
            }
            return PendingIntent.getBroadcast(context, slot.id.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        fun deadlinePendingIntent(context: Context, task: TaskEntity): PendingIntent {
            val intent = Intent(context, DeadlineAlarmReceiver::class.java).apply {
                action = "${context.packageName}.DEADLINE_ALARM"
                data = Uri.parse("campusos://deadline/${Uri.encode(task.id)}")
                putExtra(EXTRA_TASK_ID, task.id)
                putExtra(EXTRA_TASK_TITLE, task.title)
                putExtra(EXTRA_TASK_DUE, task.dueEpochMillis)
            }
            return PendingIntent.getBroadcast(context, task.id.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
    }
}
