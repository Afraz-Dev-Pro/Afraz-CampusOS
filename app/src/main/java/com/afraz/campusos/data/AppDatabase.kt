package com.afraz.campusos.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE ExpenseSplitEntity ADD COLUMN direction TEXT NOT NULL DEFAULT 'YOU_OWE'")
            }
        }
    }

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

