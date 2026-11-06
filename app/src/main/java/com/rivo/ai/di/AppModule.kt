package com.rivo.ai.di

import android.content.Context
import androidx.room.Room
import com.rivo.ai.data.local.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "rivo_db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideApiConfigDao(db: AppDatabase) = db.apiConfigDao()

    @Provides
    fun provideSystemPromptDao(db: AppDatabase) = db.systemPromptDao()

    @Provides
    fun provideConversationDao(db: AppDatabase) = db.conversationDao()

    @Provides
    fun provideMessageDao(db: AppDatabase) = db.messageDao()

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> {
        return androidx.datastore.preferences.core.PreferenceDataStoreFactory.create(
            produceFile = { context.filesDir.resolve("rivo_prefs.preferences_pb") }
        )
    }
}
