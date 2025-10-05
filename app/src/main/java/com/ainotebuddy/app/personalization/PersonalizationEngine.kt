package com.ainotebuddy.app.personalization

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Base Personalization Engine that manages user preferences and basic personalization features
 */
@Singleton
class PersonalizationEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Add basic personalization functionality here
    // This can be expanded based on the app's requirements
}
