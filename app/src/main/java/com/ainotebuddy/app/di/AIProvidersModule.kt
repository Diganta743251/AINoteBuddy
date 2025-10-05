package com.ainotebuddy.app.di

import android.content.Context
import com.ainotebuddy.app.ai.local.AINlp
import com.ainotebuddy.app.ai.local.AIProvider
import com.ainotebuddy.app.ai.local.AIProviderFactory
import com.ainotebuddy.app.ai.local.AISummarizer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AIProvidersModule {

    @Provides
    @Singleton
    fun provideAIProvider(@ApplicationContext context: Context): AIProvider = AIProviderFactory.create(context)

    @Provides
    @Singleton
    fun provideAISummarizer(provider: AIProvider): AISummarizer = provider.summarizer

    @Provides
    @Singleton
    fun provideAINlp(provider: AIProvider): AINlp = provider.nlp
}