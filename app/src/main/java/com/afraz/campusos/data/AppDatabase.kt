package com.afraz.campusos.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        CourseEntity::class,
        AttendanceLogEntity::class,
        ScheduleSlotEntity::class,
        TaskEntity::class,
        SyllabusTopicEntity::class,
        SemesterGradeEntity::class,
        PersonalExpenseEntity::class,
        FriendContactEntity::class,
        ExpenseGroupEntity::class,
        ExpenseSplitEntity::class,
        WalletDocEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun courseDao(): CourseDao
    abstract fun attendanceLogDao(): AttendanceLogDao
    abstract fun scheduleSlotDao(): ScheduleSlotDao
    abstract fun taskDao(): TaskDao
    abstract fun syllabusTopicDao(): SyllabusTopicDao
    abstract fun semesterGradeDao(): SemesterGradeDao
    abstract fun personalExpenseDao(): PersonalExpenseDao
    abstract fun friendContactDao(): FriendContactDao
    abstract fun expenseGroupDao(): ExpenseGroupDao
    abstract fun expenseSplitDao(): ExpenseSplitDao
    abstract fun walletDocDao(): WalletDocDao
}
