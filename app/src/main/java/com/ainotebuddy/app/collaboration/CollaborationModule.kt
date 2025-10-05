package com.ainotebuddy.app.collaboration

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing collaboration services
 */
@Module
@InstallIn(SingletonComponent::class)
object CollaborationModule {
    
    @Provides
    @Singleton
    fun provideFirebaseCollaborationService(
        @ApplicationContext context: Context,
        operationalTransformEngine: OperationalTransformEngine
    ): FirebaseCollaborationService {
        return FirebaseCollaborationService(context, operationalTransformEngine)
    }
    
    @Provides
    @Singleton
    fun providePresenceManager(
        firebaseService: FirebaseCollaborationService
    ): PresenceManager {
        return PresenceManager(firebaseService)
    }
    
    @Provides
    @Singleton
    fun provideOperationalTransformEngine(): OperationalTransformEngine {
        return OperationalTransformEngine()
    }
}
