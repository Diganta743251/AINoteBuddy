package com.ainotebuddy.app.di

import com.ainotebuddy.app.ai.AIEngine
import com.ainotebuddy.app.ai.DefaultAIEngine
import com.ainotebuddy.app.ai.DefaultVoiceHeuristicsGateway
import com.ainotebuddy.app.ai.VoiceHeuristicsGateway
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AIEngineModule {
    @Binds
    @Singleton
    @com.ainotebuddy.app.ai.GeminiEngine
    abstract fun bindGeminiEngine(impl: DefaultAIEngine): AIEngine

    @Binds
    @Singleton
    abstract fun bindVoiceHeuristicsGateway(impl: DefaultVoiceHeuristicsGateway): VoiceHeuristicsGateway
}

// Removed AIAssistant facade provider to avoid FIR companion plugin conflicts