package com.ainotebuddy.app.di

import com.ainotebuddy.app.ai.AIService
import com.ainotebuddy.app.ai.AIServiceProvider
import com.ainotebuddy.app.ai.AIServiceProviderImpl
import com.ainotebuddy.app.data.repository.organization.OrganizationNoteRepository
import com.ainotebuddy.app.data.repository.organization.OrganizationNoteRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AIModule {
    
    @Binds
    @Singleton
    abstract fun bindAIServiceProvider(
        aiServiceProviderImpl: AIServiceProviderImpl
    ): AIServiceProvider
    
    @Binds
    @Singleton
    abstract fun bindOrganizationNoteRepository(
        organizationNoteRepositoryImpl: OrganizationNoteRepositoryImpl
    ): OrganizationNoteRepository
    @Binds
    @Singleton
    abstract fun bindOrganizationRepository(
        organizationRepositoryImpl: com.ainotebuddy.app.data.repository.OrganizationRepositoryImpl
    ): com.ainotebuddy.app.data.repository.OrganizationRepository
}

@Module
@InstallIn(SingletonComponent::class)
object AIServiceModule {
    
    @Provides
    @Singleton
    fun provideAIService(): AIService {
        return AIService()
    }
    
    @Provides
    @ApplicationScope
    @Singleton
    fun provideApplicationScope(): kotlinx.coroutines.CoroutineScope {
        return kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default + kotlinx.coroutines.SupervisorJob())
    }
}