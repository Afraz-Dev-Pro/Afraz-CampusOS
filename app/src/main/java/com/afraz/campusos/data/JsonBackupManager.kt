package com.afraz.campusos.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import com.afraz.campusos.feature_toolkit.widget.CampusGlanceWidget
import com.afraz.campusos.core.util.LocalDocumentPaths

@Serializable
data class CampusBackupV1(
    val version: Int = 1,
    val exportedAtEpochMillis: Long,
    val courses: List<CourseEntity>,
    val attendanceLogs: List<AttendanceLogEntity>,
    val scheduleSlots: List<ScheduleSlotEntity>,
    val tasks: List<TaskEntity>,
    val syllabusTopics: List<SyllabusTopicEntity>,
    val semesterGrades: List<SemesterGradeEntity>,
    val personalExpenses: List<PersonalExpenseEntity>,
    val friends: List<FriendContactEntity>,
    val expenseGroups: List<ExpenseGroupEntity>,
    val expenseSplits: List<ExpenseSplitEntity>,
    val walletDocuments: List<WalletDocEntity>
)

@Singleton
class JsonBackupManager @Inject constructor(
    private val database: AppDatabase,
    @ApplicationContext private val context: Context
) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = false }

    suspend fun createBackup(): CampusBackupV1 = CampusBackupV1(
        exportedAtEpochMillis = System.currentTimeMillis(),
        courses = database.courseDao().getAll(),
        attendanceLogs = database.attendanceLogDao().getAll(),
        scheduleSlots = database.scheduleSlotDao().getAll(),
        tasks = database.taskDao().getAll(),
        syllabusTopics = database.syllabusTopicDao().getAll(),
        semesterGrades = database.semesterGradeDao().getAll(),
        personalExpenses = database.personalExpenseDao().getAll(),
        friends = database.friendContactDao().getAll(),
        expenseGroups = database.expenseGroupDao().getAll(),
        expenseSplits = database.expenseSplitDao().getAll(),
        walletDocuments = database.walletDocDao().getAll()
    )

    suspend fun writeJson(output: OutputStream) {
        output.bufferedWriter(Charsets.UTF_8).use { it.write(json.encodeToString(CampusBackupV1.serializer(), createBackup())) }
    }

    suspend fun writeGzip(output: OutputStream) {
        GZIPOutputStream(output).use { gzip -> gzip.write(json.encodeToString(CampusBackupV1.serializer(), createBackup()).toByteArray(Charsets.UTF_8)) }
    }

    suspend fun writeToUri(uri: Uri, compressed: Boolean = false) {
        val output = context.contentResolver.openOutputStream(uri) ?: error("Unable to open backup destination")
        if (compressed) writeGzip(output) else writeJson(output)
    }

    suspend fun restore(input: InputStream) {
        val buffered = BufferedInputStream(SizeLimitedInputStream(input, MAX_BACKUP_BYTES))
        buffered.mark(2)
        val first = buffered.read()
        val second = buffered.read()
        buffered.reset()
        val decompressed = if (first == 0x1f && second == 0x8b) GZIPInputStream(buffered) else buffered
        val source = SizeLimitedInputStream(decompressed, MAX_BACKUP_BYTES)
        val backupJson = source.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val payload = json.decodeFromString(CampusBackupV1.serializer(), backupJson)
        require(payload.version == 1) { "Unsupported CampusOS backup version: ${payload.version}" }
        require(payload.walletDocuments.all { LocalDocumentPaths.resolveManagedFile(context, it.localFilePath) != null }) {
            "Backup contains a document path outside CampusOS storage"
        }
        database.withTransaction {
            database.expenseSplitDao().clear()
            database.attendanceLogDao().clear()
            database.scheduleSlotDao().clear()
            database.syllabusTopicDao().clear()
            database.taskDao().clear()
            database.expenseGroupDao().clear()
            database.courseDao().clear()
            database.semesterGradeDao().clear()
            database.personalExpenseDao().clear()
            database.friendContactDao().clear()
            database.walletDocDao().clear()

            database.courseDao().insertAll(payload.courses)
            database.expenseGroupDao().insertAll(payload.expenseGroups)
            database.attendanceLogDao().insertAll(payload.attendanceLogs)
            database.scheduleSlotDao().insertAll(payload.scheduleSlots)
            database.taskDao().insertAll(payload.tasks)
            database.syllabusTopicDao().insertAll(payload.syllabusTopics)
            database.semesterGradeDao().insertAll(payload.semesterGrades)
            database.personalExpenseDao().insertAll(payload.personalExpenses)
            database.friendContactDao().insertAll(payload.friends)
            database.expenseSplitDao().insertAll(payload.expenseSplits)
            database.walletDocDao().insertAll(payload.walletDocuments)
        }
        CampusGlanceWidget().updateAll(context)
    }

    suspend fun restoreFromUri(uri: Uri) {
        val input = context.contentResolver.openInputStream(uri) ?: error("Unable to open backup file")
        restore(input)
    }
}


private const val MAX_BACKUP_BYTES = 16L * 1024L * 1024L

/**
 * Caps both the stored backup bytes and decompressed JSON bytes so oversized files fail
 * before they can consume unbounded memory or enter the database transaction.
 */
private class SizeLimitedInputStream(input: InputStream, private val maxBytes: Long) : FilterInputStream(input) {
    private var bytesRead = 0L

    override fun read(): Int {
        if (bytesRead >= maxBytes) {
            if (super.read() == -1) return -1
            throw IOException("Backup exceeds the 16 MiB size limit")
        }
        val value = super.read()
        if (value != -1) bytesRead++
        return value
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (bytesRead >= maxBytes) return read().let { if (it == -1) -1 else buffer[offset].toInt() }
        val allowedLength = minOf(length.toLong(), maxBytes - bytesRead).toInt()
        val count = super.read(buffer, offset, allowedLength)
        if (count > 0) bytesRead += count
        return count
    }
}
