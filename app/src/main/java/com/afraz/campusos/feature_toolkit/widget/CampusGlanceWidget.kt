package com.afraz.campusos.feature_toolkit.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.FontWeight
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import com.afraz.campusos.data.CampusDatabaseHolder
import com.afraz.campusos.feature_academics.alarm.CampusAlarmScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

private data class WidgetSnapshot(val course: String, val room: String, val time: String, val attendance: String)

class CampusGlanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = withContext(Dispatchers.IO) {
            runCatching {
                val db = CampusDatabaseHolder.get(context)
                val courses = db.courseDao().getAll().associateBy { it.id }
                val next = db.scheduleSlotDao().getAll().mapNotNull { slot ->
                    val course = courses[slot.courseId] ?: return@mapNotNull null
                    Triple(CampusAlarmScheduler.nextClassStart(slot, System.currentTimeMillis() - 1), course, slot)
                }.minByOrNull { it.first }
                val valid = courses.values.filter { it.totalClasses > 0 }
                val attend = if (valid.isEmpty()) "No attendance data" else {
                    val total = valid.sumOf { it.totalClasses }
                    "${valid.sumOf { it.attendedClasses } * 100 / total}% attendance"
                }
                if (next == null) WidgetSnapshot("No upcoming class", "—", "Add a timetable", attend)
                else WidgetSnapshot(next.second.name, next.second.room, Date(next.first).toString(), attend)
            }.getOrElse { WidgetSnapshot("CampusOS", "—", "Open the app", "Attendance unavailable") }
        }
        provideContent { WidgetContent(snapshot) }
    }
}

@Composable
private fun WidgetContent(snapshot: WidgetSnapshot) {
    Column(
        modifier = GlanceModifier.fillMaxSize().background(ColorProvider(android.graphics.Color.rgb(17, 19, 28))).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.Start
    ) {
        Text("CAMPUSOS · NEXT CLASS", style = TextStyle(color = ColorProvider(android.graphics.Color.rgb(148, 163, 184)), fontSize = 11.sp, fontWeight = FontWeight.Bold))
        Text(snapshot.course, style = TextStyle(color = ColorProvider(android.graphics.Color.rgb(248, 250, 252)), fontSize = 17.sp, fontWeight = FontWeight.Bold))
        Text("Room ${snapshot.room} · ${snapshot.time}", style = TextStyle(color = ColorProvider(android.graphics.Color.rgb(148, 163, 184)), fontSize = 11.sp))
        Text(snapshot.attendance, style = TextStyle(color = ColorProvider(android.graphics.Color.rgb(16, 185, 129)), fontSize = 12.sp, fontWeight = FontWeight.Bold))
    }
}

class CampusWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CampusGlanceWidget()
}
