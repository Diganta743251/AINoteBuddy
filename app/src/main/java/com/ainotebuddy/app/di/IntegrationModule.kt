package com.ainotebuddy.app.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module for integration dependencies
 * Classes use @Inject constructors directly, no manual bindings needed
 */
@Module
@InstallIn(SingletonComponent::class)
object IntegrationModule {
    // No explicit bindings needed - using @Inject constructors
}