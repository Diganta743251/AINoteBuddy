package com.ainotebuddy.app.di

import android.content.Context
import com.ainotebuddy.app.data.dao.SecuritySettingsDao
import com.ainotebuddy.app.repository.SecurityRepository
import com.ainotebuddy.app.security.BiometricManager
import com.ainotebuddy.app.security.EncryptionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    @Provides
    @Singleton
    fun provideEncryptionManager(
        @ApplicationContext context: Context
    ): EncryptionManager {
        return EncryptionManager(context)
    }

    @Provides
    @Singleton
    fun provideBiometricManager(
        @ApplicationContext context: Context
    ): BiometricManager {
        return BiometricManager(context)
    }

    @Provides
    @Singleton
    fun provideSecurityRepository(
        securitySettingsDao: SecuritySettingsDao
    ): SecurityRepository {
        return SecurityRepository(securitySettingsDao)
    }
}
