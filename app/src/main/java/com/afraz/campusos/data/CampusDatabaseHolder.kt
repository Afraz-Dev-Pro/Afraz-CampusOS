package com.afraz.campusos.data

import android.content.Context
import androidx.room.Room

object CampusDatabaseHolder {
    @Volatile private var instance: AppDatabase? = null

    fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
        instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "campusos.db")
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build().also { instance = it }
    }
}

