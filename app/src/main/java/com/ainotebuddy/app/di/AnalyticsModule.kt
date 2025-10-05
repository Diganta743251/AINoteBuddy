package com.ainotebuddy.app.di

import android.content.Context
import androidx.room.Room
import com.ainotebuddy.app.data.local.analytics.AnalyticsDatabase
import com.ainotebuddy.app.data.repository.AnalyticsRepository
import com.ainotebuddy.app.data.repository.AnalyticsRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton
import javax.inject.Qualifier

@Module
@InstallIn(SingletonComponent::class)
object AnalyticsModule {

    @Provides
    @Singleton
    fun provideAnalyticsDatabase(
        @ApplicationContext context: Context
    ): AnalyticsDatabase {
        return Room.databaseBuilder(
            context,
            AnalyticsDatabase::class.java,
            "ainotebuddy_analytics.db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    @Singleton
    fun provideNoteActivityDao(database: AnalyticsDatabase) = database.noteActivityDao()

    @Provides
    @Singleton
    fun provideTagUsageDao(database: AnalyticsDatabase) = database.tagUsageDao()

    @Provides
    @Singleton
    fun provideAnalyticsRepository(
        database: AnalyticsDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    ): AnalyticsRepository {
        return AnalyticsRepositoryImpl(database, ioDispatcher)
    }

    @Provides
    @Singleton
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}

@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class IoDispatcher
