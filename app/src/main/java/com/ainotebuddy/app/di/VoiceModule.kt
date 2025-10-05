package com.ainotebuddy.app.di

import android.content.Context
import com.ainotebuddy.app.voice.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for voice dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object VoiceModule {

    @Provides
    @Singleton
    fun provideVoiceEngine(
        @ApplicationContext context: Context
    ): VoiceEngine {
        return VoiceEngine(context)
    }
}