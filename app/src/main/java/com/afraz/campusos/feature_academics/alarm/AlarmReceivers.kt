package com.afraz.campusos.feature_academics.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.afraz.campusos.R
import com.afraz.campusos.data.CampusDatabaseHolder
import com.afraz.campusos.data.CourseEntity
import com.afraz.campusos.data.ScheduleSlotEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AlarmRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action !in setOf(
                Intent.ACTION_BOOT_COMPLETED,
                "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"
            )
        ) return

        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val database = CampusDatabaseHolder.get(context)
                val courses = database.courseDao().getAll().associateBy { it.id }
                database.scheduleSlotDao().getAll().forEach { slot ->
                    courses[slot.courseId]?.let { scheduleNextClass(context, slot, it) }
                }
                database.taskDao().getAll().filterNot { it.isCompleted }.forEach { task -> scheduleDeadline(context, task) }
            } finally {
                pending.finish()
            }
        }
    }
}

class ClassAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val slotId = intent?.getStringExtra(CampusAlarmScheduler.EXTRA_SLOT_ID) ?: return
        val courseName = intent.getStringExtra(CampusAlarmScheduler.EXTRA_COURSE_NAME).orEmpty()
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val database = CampusDatabaseHolder.get(context)
                val slot = database.scheduleSlotDao().getAll().firstOrNull { it.id == slotId }
                val course = slot?.let { database.courseDao().getById(it.courseId) }
                if (slot != null && course != null) {
                    val startAt = intent?.getLongExtra(CampusAlarmScheduler.EXTRA_CLASS_START, 0L) ?: 0L
                    val leftMinutes = ((startAt - System.currentTimeMillis()).coerceAtLeast(0L) + 59_999) / 60_000
                    postReminder(context, 100_000 + slot.id.hashCode(), "Class starts in ${leftMinutes} min", "${course.name} · ${course.room}")
                    scheduleNextClass(context, slot, course, System.currentTimeMillis() + 16 * 60_000L)
                } else if (courseName.isNotBlank()) {
                    postReminder(context, 100_000 + slotId.hashCode(), "Class in 15 minutes", courseName)
                }
            } finally {
                pending.finish()
            }
        }
    }
}

class DeadlineAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val taskId = intent?.getStringExtra(CampusAlarmScheduler.EXTRA_TASK_ID) ?: return
        val title = intent.getStringExtra(CampusAlarmScheduler.EXTRA_TASK_TITLE).orEmpty()
        val due = intent.getLongExtra(CampusAlarmScheduler.EXTRA_TASK_DUE, System.currentTimeMillis())
        val hours = ((due - System.currentTimeMillis()).coerceAtLeast(0L) + 3_599_999) / 3_600_000
        val remaining = if (hours < 1) "less than 1 hour" else if (hours < 48) "$hours hours" else "${hours / 24} days ${hours % 24} hours"
        postReminder(context, 200_000 + taskId.hashCode(), "Deadline in $remaining", title)
    }
}

private fun scheduleNextClass(context: Context, slot: ScheduleSlotEntity, course: CourseEntity, afterMillis: Long = System.currentTimeMillis() - 1) {
    val classStart = CampusAlarmScheduler.nextClassStart(slot, afterMillis)
    val triggerAt = maxOf(System.currentTimeMillis() + 1_000, classStart - 15 * 60_000L)
    schedule(context, triggerAt, CampusAlarmScheduler.classPendingIntent(context, slot, course, classStart))
}

private fun scheduleDeadline(context: Context, task: com.afraz.campusos.data.TaskEntity) {
    if (task.dueEpochMillis <= System.currentTimeMillis()) return
    val triggerAt = maxOf(System.currentTimeMillis() + 1_000, task.dueEpochMillis - 48 * 60 * 60_000L)
    schedule(context, triggerAt, CampusAlarmScheduler.deadlinePendingIntent(context, task))
}

private fun schedule(context: Context, time: Long, pending: android.app.PendingIntent) {
    val manager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
    try {
        manager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, time, pending)
    } catch (_: SecurityException) {
        manager.setAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, time, pending)
    }
}

private fun postReminder(context: Context, id: Int, title: String, message: String) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        manager.createNotificationChannel(NotificationChannel("campus_reminders", "Campus reminders", NotificationManager.IMPORTANCE_HIGH))
    }
    val notification = NotificationCompat.Builder(context, "campus_reminders")
        .setSmallIcon(R.drawable.ic_stat_campus)
        .setContentTitle(title)
        .setContentText(message)
        .setStyle(NotificationCompat.BigTextStyle().bigText(message))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .build()
    manager.notify(id, notification)
}
