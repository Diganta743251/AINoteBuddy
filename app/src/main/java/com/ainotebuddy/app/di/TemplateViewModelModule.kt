package com.ainotebuddy.app.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
object TemplateViewModelModule {
    // ViewModels with @HiltViewModel don't need manual provision
}