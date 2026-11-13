package com.afraz.campusos.core.util

import android.util.Base64
import com.afraz.campusos.data.CourseEntity
import com.afraz.campusos.data.ScheduleSlotEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

@Serializable
data class TimetableBundle(val version: Int = 1, val courses: List<CourseEntity>, val slots: List<ScheduleSlotEntity>)

object TimetableCodec {
    private const val MAX_COMPRESSED_BYTES = 2 * 1024 * 1024
    private const val MAX_JSON_BYTES = 8 * 1024 * 1024
    private val json = Json { ignoreUnknownKeys = false }

    fun encode(courses: List<CourseEntity>, slots: List<ScheduleSlotEntity>): String {
        val courseIds = courses.mapTo(hashSetOf()) { it.id }
        require(slots.all { it.courseId in courseIds }) { "Every schedule slot must refer to an included course" }
        require(slots.all { it.dayOfWeek in 1..7 && it.startMinutes in 0..1439 && it.endMinutes in 1..1440 && it.endMinutes > it.startMinutes })
        val bytes = json.encodeToString(TimetableBundle.serializer(), TimetableBundle(courses = courses, slots = slots)).toByteArray(Charsets.UTF_8)
        require(bytes.size <= MAX_JSON_BYTES) { "Timetable is too large to share" }
        val compressed = ByteArrayOutputStream().use { output ->
            GZIPOutputStream(output).use { it.write(bytes) }
            output.toByteArray()
        }
        require(compressed.size <= MAX_COMPRESSED_BYTES) { "Timetable is too large to share" }
        return Base64.encodeToString(compressed, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    fun decode(encoded: String): TimetableBundle {
        require(encoded.length <= MAX_COMPRESSED_BYTES * 2) { "Timetable share code is too large" }
        val zipped = Base64.decode(encoded.trim(), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        require(zipped.size <= MAX_COMPRESSED_BYTES) { "Timetable share code is too large" }
        val jsonBytes = GZIPInputStream(zipped.inputStream()).use { input -> input.readBounded(MAX_JSON_BYTES) }
        val bundle = json.decodeFromString(TimetableBundle.serializer(), jsonBytes.toString(Charsets.UTF_8))
        require(bundle.version == 1) { "Unsupported timetable version: ${bundle.version}" }
        val courseIds = bundle.courses.mapTo(hashSetOf()) { it.id }
        require(bundle.slots.all { it.courseId in courseIds && it.dayOfWeek in 1..7 && it.startMinutes in 0..1439 && it.endMinutes in 1..1440 && it.endMinutes > it.startMinutes }) {
            "Timetable contains invalid schedule slots"
        }
        return bundle
    }

    private fun InputStream.readBounded(maxBytes: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var total = 0
        while (true) {
            val count = read(buffer)
            if (count < 0) break
            total += count
            require(total <= maxBytes) { "Timetable expands beyond the allowed size" }
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }
}
