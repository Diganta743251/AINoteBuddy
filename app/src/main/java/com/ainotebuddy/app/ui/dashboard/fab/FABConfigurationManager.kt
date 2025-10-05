package com.ainotebuddy.app.ui.dashboard.fab

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.util.*

private val Context.fabDataStore: DataStore<Preferences> by preferencesDataStore(name = "fab_settings")

@Serializable
data class SerializableFABConfig(
    val style: String = "LINEAR",
    val maxVisibleActions: Int = 6,
    val showLabels: Boolean = true,
    val animationDuration: Int = 300,
    val hapticFeedback: Boolean = true,
    val actions: List<SerializableFABAction> = emptyList()
)

@Serializable
data class SerializableFABAction(
    val typeId: String,
    val isEnabled: Boolean,
    val position: Int,
    val customLabel: String? = null,
    val customIcon: String? = null,
    val customColor: String? = null,
    val shortcutKey: String? = null
)

@Serializable
data class SerializableFABUsage(
    val actionId: String,
    val usageCount: Int = 0,
    val lastUsed: Long = 0L,
    val averageUsagePerDay: Float = 0f
)

class FABConfigurationManager(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private val configKey = stringPreferencesKey("fab_config")
    private val usageKey = stringPreferencesKey("fab_usage")
    
    private val _currentConfig = mutableStateOf(getDefaultFABMenuConfig())
    val currentConfig: State<FABMenuConfig> = _currentConfig
    
    private val _usageStats = mutableStateOf(emptyMap<String, FABActionUsage>())
    val usageStats: State<Map<String, FABActionUsage>> = _usageStats
    
    val configFlow: Flow<FABMenuConfig> = context.fabDataStore.data
        .map { preferences ->
            val configJson = preferences[configKey]
            if (configJson != null) {
                try {
                    val serializableConfig = json.decodeFromString<SerializableFABConfig>(configJson)
                    serializableConfig.toFABMenuConfig()
                } catch (e: Exception) {
                    getDefaultFABMenuConfig()
                }
            } else {
                getDefaultFABMenuConfig()
            }
        }
    
    val usageFlow: Flow<Map<String, FABActionUsage>> = context.fabDataStore.data
        .map { preferences ->
            val usageJson = preferences[usageKey]
            if (usageJson != null) {
                try {
                    val serializableUsage = json.decodeFromString<List<SerializableFABUsage>>(usageJson)
                    serializableUsage.associate { usage ->
                        usage.actionId to FABActionUsage(
                            actionId = usage.actionId,
                            usageCount = usage.usageCount,
                            lastUsed = usage.lastUsed,
                            averageUsagePerDay = usage.averageUsagePerDay
                        )
                    }
                } catch (e: Exception) {
                    emptyMap()
                }
            } else {
                emptyMap()
            }
        }
    
    suspend fun updateConfiguration(config: FABMenuConfig) {
        context.fabDataStore.edit { preferences ->
            val serializableConfig = config.toSerializableConfig()
            preferences[configKey] = json.encodeToString(serializableConfig)
        }
        _currentConfig.value = config
    }
    
    suspend fun toggleAction(actionType: FABActionType, enabled: Boolean) {
        val currentConfig = _currentConfig.value
        val updatedActions = currentConfig.actions.map { action ->
            if (action.type == actionType) {
                action.copy(isEnabled = enabled)
            } else {
                action
            }
        }
        updateConfiguration(currentConfig.copy(actions = updatedActions))
    }
    
    suspend fun reorderActions(fromIndex: Int, toIndex: Int) {
        val currentConfig = _currentConfig.value
        val actions = currentConfig.actions.toMutableList()
        
        if (fromIndex in actions.indices && toIndex in actions.indices) {
            val item = actions.removeAt(fromIndex)
            actions.add(toIndex, item)
            
            // Update positions
            val updatedActions = actions.mapIndexed { index, action ->
                action.copy(position = index)
            }
            
            updateConfiguration(currentConfig.copy(actions = updatedActions))
        }
    }
    
    suspend fun updateActionCustomization(
        actionType: FABActionType,
        customLabel: String? = null,
        customColor: String? = null,
        shortcutKey: String? = null
    ) {
        val currentConfig = _currentConfig.value
        val updatedActions = currentConfig.actions.map { action ->
            if (action.type == actionType) {
                action.copy(
                    customLabel = customLabel,
                    customColor = customColor,
                    shortcutKey = shortcutKey
                )
            } else {
                action
            }
        }
        updateConfiguration(currentConfig.copy(actions = updatedActions))
    }
    
    suspend fun updateMenuStyle(style: FABMenuStyle) {
        val currentConfig = _currentConfig.value
        updateConfiguration(currentConfig.copy(style = style))
    }
    
    suspend fun updateMaxVisibleActions(count: Int) {
        val currentConfig = _currentConfig.value
        updateConfiguration(currentConfig.copy(maxVisibleActions = count.coerceIn(3, 8)))
    }
    
    suspend fun recordActionUsage(actionType: FABActionType) {
        val currentUsage = _usageStats.value
        val actionUsage = currentUsage[actionType.id] ?: FABActionUsage(actionType.id)
        
        val updatedUsage = actionUsage.copy(
            usageCount = actionUsage.usageCount + 1,
            lastUsed = System.currentTimeMillis(),
            averageUsagePerDay = calculateAverageUsage(actionUsage.usageCount + 1, actionUsage.lastUsed)
        )
        
        val newUsageMap = currentUsage + (actionType.id to updatedUsage)
        _usageStats.value = newUsageMap
        
        // Save to DataStore
        context.fabDataStore.edit { preferences ->
            val serializableUsage = newUsageMap.values.map { usage ->
                SerializableFABUsage(
                    actionId = usage.actionId,
                    usageCount = usage.usageCount,
                    lastUsed = usage.lastUsed,
                    averageUsagePerDay = usage.averageUsagePerDay
                )
            }
            preferences[usageKey] = json.encodeToString(serializableUsage)
        }
    }
    
    suspend fun resetToDefault() {
        updateConfiguration(getDefaultFABMenuConfig())
    }
    
    fun getEnabledActions(): List<FABActionConfig> {
        return _currentConfig.value.actions
            .filter { it.isEnabled }
            .sortedBy { it.position }
            .take(_currentConfig.value.maxVisibleActions)
    }
    
    fun getAllActions(): List<FABActionConfig> {
        return _currentConfig.value.actions.sortedBy { it.position }
    }
    
    fun getActionsByCategory(category: FABActionCategory): List<FABActionConfig> {
        return _currentConfig.value.actions
            .filter { it.type.category == category }
            .sortedBy { it.position }
    }
    
    fun generateSmartSuggestions(): List<FABActionSuggestion> {
        val usage = _usageStats.value
        val currentActions = getEnabledActions().map { it.type }
        val suggestions = mutableListOf<FABActionSuggestion>()
        
        // Frequently used but not enabled
        usage.values
            .filter { it.usageCount > 5 && it.actionId !in currentActions.map { action -> action.id } }
            .sortedByDescending { it.averageUsagePerDay }
            .take(3)
            .forEach { usageData ->
                FABActionType.values().find { it.id == usageData.actionId }?.let { actionType ->
                    suggestions.add(
                        FABActionSuggestion(
                            action = actionType,
                            reason = "You've used this ${usageData.usageCount} times",
                            confidence = (usageData.averageUsagePerDay / 10f).coerceAtMost(1f),
                            category = SuggestionCategory.FREQUENTLY_USED
                        )
                    )
                }
            }
        
        // Time-based suggestions
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 6..10 -> {
                if (FABActionType.DAILY_JOURNAL !in currentActions) {
                    suggestions.add(
                        FABActionSuggestion(
                            action = FABActionType.DAILY_JOURNAL,
                            reason = "Perfect time for morning journaling",
                            confidence = 0.8f,
                            category = SuggestionCategory.TIME_BASED
                        )
                    )
                }
            }
            in 14..16 -> {
                if (FABActionType.MEETING_NOTES !in currentActions) {
                    suggestions.add(
                        FABActionSuggestion(
                            action = FABActionType.MEETING_NOTES,
                            reason = "Common meeting time",
                            confidence = 0.7f,
                            category = SuggestionCategory.TIME_BASED
                        )
                    )
                }
            }
        }
        
        // Complementary actions
        if (FABActionType.VOICE_NOTE in currentActions && FABActionType.AI_ASSIST !in currentActions) {
            suggestions.add(
                FABActionSuggestion(
                    action = FABActionType.AI_ASSIST,
                    reason = "Great for transcribing voice notes",
                    confidence = 0.6f,
                    category = SuggestionCategory.COMPLEMENTARY
                )
            )
        }
        
        return suggestions.take(5)
    }
    
    private fun calculateAverageUsage(totalUsage: Int, lastUsed: Long): Float {
        val daysSinceFirstUse = if (lastUsed > 0) {
            ((System.currentTimeMillis() - lastUsed) / (24 * 60 * 60 * 1000)).coerceAtLeast(1)
        } else {
            1
        }
        return totalUsage.toFloat() / daysSinceFirstUse
    }
    
    companion object {
        fun getDefaultFABMenuConfig(): FABMenuConfig {
            return FABMenuConfig(
                style = FABMenuStyle.LINEAR,
                maxVisibleActions = 6,
                showLabels = true,
                animationDuration = 300,
                hapticFeedback = true,
                actions = getDefaultFABActions()
            )
        }
        
        private fun getDefaultSerializableActions(): List<SerializableFABAction> {
            return FABActionType.values()
                .filter { it.defaultEnabled }
                .mapIndexed { index, type ->
                    SerializableFABAction(
                        typeId = type.id,
                        isEnabled = true,
                        position = index
                    )
                }
        }
    }
}

// Extension functions for conversion
fun FABMenuConfig.toSerializableConfig(): SerializableFABConfig {
    return SerializableFABConfig(
        style = style.name,
        maxVisibleActions = maxVisibleActions,
        showLabels = showLabels,
        animationDuration = animationDuration,
        hapticFeedback = hapticFeedback,
        actions = actions.map { it.toSerializableAction() }
    )
}

fun FABActionConfig.toSerializableAction(): SerializableFABAction {
    return SerializableFABAction(
        typeId = type.id,
        isEnabled = isEnabled,
        position = position,
        customLabel = customLabel,
        customIcon = customIcon,
        customColor = customColor,
        shortcutKey = shortcutKey
    )
}

fun SerializableFABConfig.toFABMenuConfig(): FABMenuConfig {
    return FABMenuConfig(
        style = FABMenuStyle.valueOf(style),
        maxVisibleActions = maxVisibleActions,
        showLabels = showLabels,
        animationDuration = animationDuration,
        hapticFeedback = hapticFeedback,
        actions = actions.mapNotNull { it.toFABActionConfig() }
    )
}

fun SerializableFABAction.toFABActionConfig(): FABActionConfig? {
    val actionType = FABActionType.values().find { it.id == typeId }
    return actionType?.let {
        FABActionConfig(
            type = it,
            isEnabled = isEnabled,
            position = position,
            customLabel = customLabel,
            customIcon = customIcon,
            customColor = customColor,
            shortcutKey = shortcutKey
        )
    }
}