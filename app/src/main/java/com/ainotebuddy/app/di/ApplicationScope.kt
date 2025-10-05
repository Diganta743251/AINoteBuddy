package com.ainotebuddy.app.di

import javax.inject.Qualifier

/**
 * Qualifier for application-scoped coroutine scope
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ApplicationScope