package com.afraz.campusos.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.afraz.campusos.data.AppDatabase
import com.afraz.campusos.data.CampusDatabaseHolder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.campusSettings by preferencesDataStore(name = "campus_settings")

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        CampusDatabaseHolder.get(context)

    @Provides
    @Singleton
    fun provideSettings(@ApplicationContext context: Context): DataStore<Preferences> = context.campusSettings
}
