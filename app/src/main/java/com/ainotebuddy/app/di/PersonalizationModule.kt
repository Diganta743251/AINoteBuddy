package com.ainotebuddy.app.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module for personalization dependencies
 * Classes use @Inject constructors directly, no manual bindings needed
 */
@Module
@InstallIn(SingletonComponent::class)
object PersonalizationModule {
    // No explicit bindings needed - using @Inject constructors
}