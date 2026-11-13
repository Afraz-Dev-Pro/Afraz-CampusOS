package com.afraz.campusos.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity
data class CourseEntity(@PrimaryKey val id: String, val name: String, val code: String, val professor: String, val room: String, val attendedClasses: Int = 0, val totalClasses: Int = 0, val targetPercentage: Double = 0.75, val credits: Int = 0)

@Serializable
@Entity(foreignKeys = [ForeignKey(entity = CourseEntity::class, parentColumns = ["id"], childColumns = ["courseId"], onDelete = ForeignKey.CASCADE)], indices = [Index("courseId")])
data class AttendanceLogEntity(@PrimaryKey(autoGenerate = true) val logId: Long = 0, val courseId: String, val timestamp: Long, val status: String)

@Serializable
@Entity(foreignKeys = [ForeignKey(entity = CourseEntity::class, parentColumns = ["id"], childColumns = ["courseId"], onDelete = ForeignKey.CASCADE)], indices = [Index("courseId")])
data class ScheduleSlotEntity(@PrimaryKey val id: String, val courseId: String, val dayOfWeek: Int, val startMinutes: Int, val endMinutes: Int)

@Serializable
@Entity
data class TaskEntity(@PrimaryKey val id: String, val courseId: String?, val title: String, val category: String, val dueEpochMillis: Long, val priority: Int, val isCompleted: Boolean)

@Serializable
@Entity(foreignKeys = [ForeignKey(entity = CourseEntity::class, parentColumns = ["id"], childColumns = ["courseId"], onDelete = ForeignKey.CASCADE)], indices = [Index("courseId")])
data class SyllabusTopicEntity(@PrimaryKey val id: String, val courseId: String, val unitNumber: Int, val topicTitle: String, val isCompleted: Boolean)

@Serializable
@Entity
data class SemesterGradeEntity(@PrimaryKey val semesterNumber: Int, val sgpa: Double, val totalCredits: Int)

@Serializable
@Entity
data class PersonalExpenseEntity(@PrimaryKey val id: String, val title: String, val category: String, val amountPaise: Long, val timestamp: Long)

@Serializable
@Entity
data class FriendContactEntity(@PrimaryKey val id: String, val name: String, val upiId: String)

@Serializable
@Entity
data class ExpenseGroupEntity(@PrimaryKey val id: String, val title: String, val totalAmountPaise: Long, val createdAt: Long)

@Serializable
@Entity(foreignKeys = [ForeignKey(entity = ExpenseGroupEntity::class, parentColumns = ["id"], childColumns = ["groupId"], onDelete = ForeignKey.CASCADE)], indices = [Index("groupId")])
data class ExpenseSplitEntity(@PrimaryKey val id: String, val groupId: String, val friendName: String, val friendUpiId: String, val shareAmountPaise: Long, val isSettled: Boolean)

@Serializable
@Entity
data class WalletDocEntity(@PrimaryKey val id: String, val title: String, val docType: String, val subjectFolder: String?, val localFilePath: String, val mimeType: String, val createdAt: Long)
