package com.ainotebuddy.app.di

import android.content.Context
import com.ainotebuddy.app.search.*
import com.ainotebuddy.app.repository.NoteRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for search dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object SearchModule {

    @Provides
    @Singleton
    fun provideSmartSearchEngine(
        @ApplicationContext context: Context
    ): SmartSearchEngine = SmartSearchEngine(context)
}