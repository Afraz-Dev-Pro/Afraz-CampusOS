package com.afraz.campusos.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {
    @Query("SELECT * FROM CourseEntity ORDER BY name") fun observeAll(): Flow<List<CourseEntity>>
    @Query("SELECT * FROM CourseEntity ORDER BY name") suspend fun getAll(): List<CourseEntity>
    @Query("SELECT * FROM CourseEntity WHERE id = :id") suspend fun getById(id: String): CourseEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(row: CourseEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(rows: List<CourseEntity>)
    @Query("UPDATE CourseEntity SET attendedClasses = attendedClasses + :present, totalClasses = totalClasses + 1 WHERE id = :id") suspend fun recordAttendance(id: String, present: Int)
    @Query("UPDATE CourseEntity SET attendedClasses = CASE WHEN :present = 1 THEN MAX(0, attendedClasses - 1) ELSE attendedClasses END, totalClasses = MAX(0, totalClasses - 1) WHERE id = :id") suspend fun undoAttendance(id: String, present: Int)
    @Query("UPDATE CourseEntity SET targetPercentage = :target WHERE id = :id") suspend fun setTarget(id: String, target: Double)
    @Query("DELETE FROM CourseEntity") suspend fun clear()
}

@Dao
interface AttendanceLogDao {
    @Query("SELECT * FROM AttendanceLogEntity ORDER BY timestamp DESC") suspend fun getAll(): List<AttendanceLogEntity>
    @Query("SELECT * FROM AttendanceLogEntity WHERE courseId = :courseId ORDER BY timestamp DESC") fun observeForCourse(courseId: String): Flow<List<AttendanceLogEntity>>
    @Query("SELECT * FROM AttendanceLogEntity WHERE courseId = :courseId ORDER BY timestamp DESC, logId DESC LIMIT 1") suspend fun latestForCourse(courseId: String): AttendanceLogEntity?
    @Insert suspend fun insert(row: AttendanceLogEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(rows: List<AttendanceLogEntity>)
    @Query("DELETE FROM AttendanceLogEntity WHERE logId = :logId") suspend fun delete(logId: Long)
    @Query("DELETE FROM AttendanceLogEntity") suspend fun clear()
}

@Dao
interface ScheduleSlotDao {
    @Query("SELECT * FROM ScheduleSlotEntity ORDER BY dayOfWeek, startMinutes") fun observeAll(): Flow<List<ScheduleSlotEntity>>
    @Query("SELECT * FROM ScheduleSlotEntity ORDER BY dayOfWeek, startMinutes") suspend fun getAll(): List<ScheduleSlotEntity>
    @Query("SELECT * FROM ScheduleSlotEntity WHERE dayOfWeek = :day ORDER BY startMinutes") fun observeDay(day: Int): Flow<List<ScheduleSlotEntity>>
    @Query("SELECT * FROM ScheduleSlotEntity WHERE dayOfWeek = :day ORDER BY startMinutes") suspend fun getDay(day: Int): List<ScheduleSlotEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(row: ScheduleSlotEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(rows: List<ScheduleSlotEntity>)
    @Query("DELETE FROM ScheduleSlotEntity WHERE id = :id") suspend fun delete(id: String)
    @Query("DELETE FROM ScheduleSlotEntity") suspend fun clear()
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM TaskEntity ORDER BY dueEpochMillis") fun observeAll(): Flow<List<TaskEntity>>
    @Query("SELECT * FROM TaskEntity ORDER BY dueEpochMillis") suspend fun getAll(): List<TaskEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(row: TaskEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(rows: List<TaskEntity>)
    @Query("UPDATE TaskEntity SET isCompleted = :done WHERE id = :id") suspend fun setCompleted(id: String, done: Boolean)
    @Query("DELETE FROM TaskEntity WHERE id = :id") suspend fun delete(id: String)
    @Query("DELETE FROM TaskEntity") suspend fun clear()
}

@Dao
interface SyllabusTopicDao {
    @Query("SELECT * FROM SyllabusTopicEntity ORDER BY courseId, unitNumber, topicTitle") suspend fun getAll(): List<SyllabusTopicEntity>
    @Query("SELECT * FROM SyllabusTopicEntity WHERE courseId = :courseId ORDER BY unitNumber, topicTitle") fun observeForCourse(courseId: String): Flow<List<SyllabusTopicEntity>>
    @Query("SELECT * FROM SyllabusTopicEntity ORDER BY courseId, unitNumber, topicTitle") fun observeAll(): Flow<List<SyllabusTopicEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(row: SyllabusTopicEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(rows: List<SyllabusTopicEntity>)
    @Query("UPDATE SyllabusTopicEntity SET isCompleted = :done WHERE id = :id") suspend fun setCompleted(id: String, done: Boolean)
    @Query("DELETE FROM SyllabusTopicEntity WHERE id = :id") suspend fun delete(id: String)
    @Query("DELETE FROM SyllabusTopicEntity") suspend fun clear()
}

@Dao
interface SemesterGradeDao {
    @Query("SELECT * FROM SemesterGradeEntity ORDER BY semesterNumber") suspend fun getAll(): List<SemesterGradeEntity>
    @Query("SELECT * FROM SemesterGradeEntity ORDER BY semesterNumber") fun observeAll(): Flow<List<SemesterGradeEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(row: SemesterGradeEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(rows: List<SemesterGradeEntity>)
    @Query("DELETE FROM SemesterGradeEntity") suspend fun clear()
}

@Dao
interface PersonalExpenseDao {
    @Query("SELECT * FROM PersonalExpenseEntity ORDER BY timestamp DESC") fun observeAll(): Flow<List<PersonalExpenseEntity>>
    @Query("SELECT * FROM PersonalExpenseEntity ORDER BY timestamp DESC") suspend fun getAll(): List<PersonalExpenseEntity>
    @Query("SELECT COALESCE(SUM(amountPaise), 0) FROM PersonalExpenseEntity WHERE timestamp >= :from AND timestamp < :until") fun observeTotal(from: Long, until: Long): Flow<Long>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(row: PersonalExpenseEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(rows: List<PersonalExpenseEntity>)
    @Query("DELETE FROM PersonalExpenseEntity WHERE id = :id") suspend fun delete(id: String)
    @Query("DELETE FROM PersonalExpenseEntity") suspend fun clear()
}

@Dao
interface FriendContactDao {
    @Query("SELECT * FROM FriendContactEntity ORDER BY name") fun observeAll(): Flow<List<FriendContactEntity>>
    @Query("SELECT * FROM FriendContactEntity ORDER BY name") suspend fun getAll(): List<FriendContactEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(row: FriendContactEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(rows: List<FriendContactEntity>)
    @Query("DELETE FROM FriendContactEntity WHERE id = :id") suspend fun delete(id: String)
    @Query("DELETE FROM FriendContactEntity") suspend fun clear()
}

@Dao
interface ExpenseGroupDao {
    @Query("SELECT * FROM ExpenseGroupEntity ORDER BY createdAt DESC") fun observeAll(): Flow<List<ExpenseGroupEntity>>
    @Query("SELECT * FROM ExpenseGroupEntity ORDER BY createdAt DESC") suspend fun getAll(): List<ExpenseGroupEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(row: ExpenseGroupEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(rows: List<ExpenseGroupEntity>)
    @Query("DELETE FROM ExpenseGroupEntity") suspend fun clear()
}

@Dao
interface ExpenseSplitDao {
    @Query("SELECT * FROM ExpenseSplitEntity ORDER BY groupId, friendName") fun observeAll(): Flow<List<ExpenseSplitEntity>>
    @Query("SELECT * FROM ExpenseSplitEntity ORDER BY groupId, friendName") suspend fun getAll(): List<ExpenseSplitEntity>
    @Query("SELECT * FROM ExpenseSplitEntity WHERE groupId = :groupId ORDER BY friendName") fun observeForGroup(groupId: String): Flow<List<ExpenseSplitEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(row: ExpenseSplitEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(rows: List<ExpenseSplitEntity>)
    @Query("UPDATE ExpenseSplitEntity SET isSettled = :settled WHERE id = :id") suspend fun setSettled(id: String, settled: Boolean)
    @Query("DELETE FROM ExpenseSplitEntity") suspend fun clear()
}

@Dao
interface WalletDocDao {
    @Query("SELECT * FROM WalletDocEntity ORDER BY createdAt DESC") fun observeAll(): Flow<List<WalletDocEntity>>
    @Query("SELECT * FROM WalletDocEntity ORDER BY createdAt DESC") suspend fun getAll(): List<WalletDocEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(row: WalletDocEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(rows: List<WalletDocEntity>)
    @Query("DELETE FROM WalletDocEntity WHERE id = :id") suspend fun delete(id: String)
    @Query("DELETE FROM WalletDocEntity") suspend fun clear()
}
