package com.ainotebuddy.app.di

import android.content.Context
import com.ainotebuddy.app.data.PreferencesManager
import com.ainotebuddy.app.ai.embeddings.AIEmbeddings
import com.ainotebuddy.app.ai.embeddings.SimpleAIEmbeddings
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
    fun providePreferencesManager(@ApplicationContext context: Context): PreferencesManager {
        return PreferencesManager(context)
    }

    // AI: lightweight default embeddings implementation; replace with real model later
    @Provides
    @Singleton
    fun provideAIEmbeddings(): AIEmbeddings = SimpleAIEmbeddings()
}