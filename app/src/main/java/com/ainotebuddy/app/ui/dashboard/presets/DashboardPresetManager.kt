package com.ainotebuddy.app.ui.dashboard.presets

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ainotebuddy.app.ui.dashboard.DashboardWidgetManager
import com.ainotebuddy.app.ui.dashboard.fab.FABConfigurationManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.Contextual

private val Context.presetDataStore: DataStore<Preferences> by preferencesDataStore(name = "preset_settings")

// Serializable DTOs for storing dashboard configuration safely in DataStore
@Serializable
data class SerializableWidgetConfig(
    val typeId: String,
    val isEnabled: Boolean = true,
    val position: Int = 0,
    val customSettings: Map<String, String> = emptyMap()
)

@Serializable
data class SerializableFABActionConfig(
    val typeId: String,
    val isEnabled: Boolean = false,
    val position: Int = 0,
    val customLabel: String? = null
)

@Serializable
data class SerializableFABMenuConfig(
    val style: String,
    val maxVisibleActions: Int,
    val showLabels: Boolean,
    val actions: List<SerializableFABActionConfig>
)

@Serializable
data class SerializablePresetUsage(
    val presetId: String,
    val timesApplied: Int = 0,
    val lastApplied: Long = 0L,
    val userRating: Float = 0f,
    val customizations: Int = 0
)

@Serializable
data class CustomPresetData(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val widgetConfigs: List<SerializableWidgetConfig>,
    val fabConfig: SerializableFABMenuConfig,
    val createdAt: Long,
    val isShared: Boolean = false
)

class DashboardPresetManager(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private val usageKey = stringPreferencesKey("preset_usage")
    private val currentPresetKey = stringPreferencesKey("current_preset")
    
    private val widgetManager = DashboardWidgetManager(context)
    private val fabManager = FABConfigurationManager(context)
    
    private val _currentPreset = mutableStateOf<DashboardPresetType?>(null)
    val currentPreset: State<DashboardPresetType?> = _currentPreset
    
    private val _usageStats = mutableStateOf(emptyMap<String, PresetUsageStats>())
    val usageStats: State<Map<String, PresetUsageStats>> = _usageStats
    
    val currentPresetFlow: Flow<DashboardPresetType?> = context.presetDataStore.data
        .map { preferences ->
            val presetId = preferences[currentPresetKey]
            presetId?.let { id ->
                DashboardPresetType.values().find { it.id == id }
            }
        }
    
    val usageStatsFlow: Flow<Map<String, PresetUsageStats>> = context.presetDataStore.data
        .map { preferences ->
            val usageJson = preferences[usageKey]
            if (usageJson != null) {
                try {
                    val serializableUsage = json.decodeFromString<List<SerializablePresetUsage>>(usageJson)
                    serializableUsage.associate { usage ->
                        usage.presetId to PresetUsageStats(
                            presetId = usage.presetId,
                            timesApplied = usage.timesApplied,
                            lastApplied = usage.lastApplied,
                            userRating = usage.userRating,
                            customizations = usage.customizations
                        )
                    }
                } catch (e: Exception) {
                    emptyMap()
                }
            } else {
                emptyMap()
            }
        }
    
    suspend fun applyPreset(presetType: DashboardPresetType) {
        val preset = getPresetByType(presetType)
        
        // Apply widget configuration
        widgetManager.applyConfiguration(preset.widgetConfigs)
        
        // Apply FAB configuration
        fabManager.updateConfiguration(preset.fabConfig)
        
        // Update current preset
        context.presetDataStore.edit { preferences ->
            preferences[currentPresetKey] = presetType.id
        }
        _currentPreset.value = presetType
        
        // Record usage
        recordPresetUsage(presetType)
    }
    
    suspend fun applyPresetWithCustomizations(
        presetType: DashboardPresetType,
        customWidgets: List<com.ainotebuddy.app.ui.dashboard.DashboardWidgetConfig>? = null,
        customFABConfig: com.ainotebuddy.app.ui.dashboard.fab.FABMenuConfig? = null
    ) {
        val preset = getPresetByType(presetType)
        
        // Apply custom widget configuration or default
        widgetManager.applyConfiguration(customWidgets ?: preset.widgetConfigs)
        
        // Apply custom FAB configuration or default
        fabManager.updateConfiguration(customFABConfig ?: preset.fabConfig)
        
        // Update current preset
        context.presetDataStore.edit { preferences ->
            preferences[currentPresetKey] = presetType.id
        }
        _currentPreset.value = presetType
        
        // Record usage with customization flag
        recordPresetUsage(presetType, hasCustomizations = customWidgets != null || customFABConfig != null)
    }
    
    suspend fun clearCurrentPreset() {
        context.presetDataStore.edit { preferences ->
            preferences.remove(currentPresetKey)
        }
        _currentPreset.value = null
    }
    
    suspend fun ratePreset(presetType: DashboardPresetType, rating: Float) {
        val currentStats = _usageStats.value[presetType.id] ?: PresetUsageStats(presetType.id)
        val updatedStats = currentStats.copy(userRating = rating)
        
        updateUsageStats(presetType.id, updatedStats)
    }
    
    private suspend fun recordPresetUsage(presetType: DashboardPresetType, hasCustomizations: Boolean = false) {
        val currentStats = _usageStats.value[presetType.id] ?: PresetUsageStats(presetType.id)
        val updatedStats = currentStats.copy(
            timesApplied = currentStats.timesApplied + 1,
            lastApplied = System.currentTimeMillis(),
            customizations = if (hasCustomizations) currentStats.customizations + 1 else currentStats.customizations
        )
        
        updateUsageStats(presetType.id, updatedStats)
    }
    
    private suspend fun updateUsageStats(presetId: String, stats: PresetUsageStats) {
        val currentUsage = _usageStats.value.toMutableMap()
        currentUsage[presetId] = stats
        _usageStats.value = currentUsage
        
        // Save to DataStore
        context.presetDataStore.edit { preferences ->
            val serializableUsage = currentUsage.values.map { usage ->
                SerializablePresetUsage(
                    presetId = usage.presetId,
                    timesApplied = usage.timesApplied,
                    lastApplied = usage.lastApplied,
                    userRating = usage.userRating,
                    customizations = usage.customizations
                )
            }
            preferences[usageKey] = json.encodeToString(serializableUsage)
        }
    }
    
    fun getPresetByType(type: DashboardPresetType): DashboardPreset {
        return when (type) {
            DashboardPresetType.TASK_FOCUSED -> getTaskFocusedPreset()
            DashboardPresetType.BUSINESS_PROFESSIONAL -> getBusinessProfessionalPreset()
            DashboardPresetType.PROJECT_MANAGER -> getProjectManagerPreset()
            DashboardPresetType.CREATIVE_WRITER -> getCreativeWriterPreset()
            DashboardPresetType.VISUAL_ARTIST -> getVisualArtistPreset()
            DashboardPresetType.CONTENT_CREATOR -> getContentCreatorPreset()
            DashboardPresetType.STUDENT -> getStudentPreset()
            DashboardPresetType.RESEARCHER -> getResearcherPreset()
            DashboardPresetType.TEACHER -> getTeacherPreset()
            DashboardPresetType.PERSONAL_JOURNAL -> getPersonalJournalPreset()
            DashboardPresetType.MINIMALIST -> getMinimalistPreset()
            DashboardPresetType.POWER_USER -> getPowerUserPreset()
        }
    }
    
    fun getAllPresets(): List<DashboardPreset> {
        return getAllDashboardPresets()
    }
    
    fun getPresetsByCategory(category: PresetCategory): List<DashboardPreset> {
        return getAllDashboardPresets().filter { it.type.category == category }
    }
    
    fun getRecommendedPresets(): List<DashboardPreset> {
        val usage = _usageStats.value
        
        // Get presets with high ratings or frequent usage
        val highRatedPresets = usage.values
            .filter { it.userRating >= 4.0f }
            .sortedByDescending { it.userRating }
            .take(3)
            .mapNotNull { stats ->
                DashboardPresetType.values().find { it.id == stats.presetId }?.let { type ->
                    getPresetByType(type)
                }
            }
        
        // If no high-rated presets, return popular defaults
        return if (highRatedPresets.isNotEmpty()) {
            highRatedPresets
        } else {
            listOf(
                getTaskFocusedPreset(),
                getStudentPreset(),
                getMinimalistPreset()
            )
        }
    }
    
    fun getPopularPresets(): List<DashboardPreset> {
        val usage = _usageStats.value
        
        return usage.values
            .sortedByDescending { it.timesApplied }
            .take(5)
            .mapNotNull { stats ->
                DashboardPresetType.values().find { it.id == stats.presetId }?.let { type ->
                    getPresetByType(type)
                }
            }
    }
    
    fun searchPresets(query: String): List<DashboardPreset> {
        return getAllDashboardPresets().filter { preset ->
            preset.type.displayName.contains(query, ignoreCase = true) ||
            preset.type.description.contains(query, ignoreCase = true) ||
            preset.type.category.displayName.contains(query, ignoreCase = true)
        }
    }
    
    suspend fun createCustomPreset(
        name: String,
        description: String,
        category: PresetCategory,
        widgetConfigs: List<com.ainotebuddy.app.ui.dashboard.DashboardWidgetConfig>,
        fabConfig: com.ainotebuddy.app.ui.dashboard.fab.FABMenuConfig
    ): String {
        val customPresetId = "custom_${System.currentTimeMillis()}"
        
        // Save custom preset to DataStore
        val serializableWidgets = widgetConfigs.map { cfg ->
            SerializableWidgetConfig(
                typeId = cfg.type.id,
                isEnabled = cfg.isEnabled,
                position = cfg.position,
                customSettings = cfg.customSettings.mapValues { it.value.toString() }
            )
        }
        val serializableFabActions = fabConfig.actions.map { act ->
            SerializableFABActionConfig(
                typeId = act.type.id,
                isEnabled = act.isEnabled,
                position = act.position,
                customLabel = act.customLabel
            )
        }
        val serializableFab = SerializableFABMenuConfig(
            style = fabConfig.style.name,
            maxVisibleActions = fabConfig.maxVisibleActions,
            showLabels = fabConfig.showLabels,
            actions = serializableFabActions
        )
        val customPreset = CustomPresetData(
            id = customPresetId,
            name = name,
            description = description,
            category = category.name,
            widgetConfigs = serializableWidgets,
            fabConfig = serializableFab,
            createdAt = System.currentTimeMillis()
        )
        
        saveCustomPreset(customPreset)
        
        // Apply the configuration immediately
        widgetManager.applyConfiguration(widgetConfigs)
        fabManager.updateConfiguration(fabConfig)
        
        // Set as current preset
        context.presetDataStore.edit { preferences ->
            preferences[currentPresetKey] = customPresetId
        }
        
        return customPresetId
    }
    
    private suspend fun saveCustomPreset(preset: CustomPresetData) {
        val customPresetsKey = stringPreferencesKey("custom_presets")
        
        context.presetDataStore.edit { preferences ->
            val existingPresets = preferences[customPresetsKey]?.let { jsonStr ->
                try {
                    json.decodeFromString<List<CustomPresetData>>(jsonStr)
                } catch (e: Exception) {
                    emptyList()
                }
            } ?: emptyList()

            val updatedPresets = existingPresets + preset
            preferences[customPresetsKey] = json.encodeToString(updatedPresets)
        }
    }
    
    suspend fun getCustomPresets(): List<CustomPresetData> {
        val customPresetsKey = stringPreferencesKey("custom_presets")
        
        return context.presetDataStore.data.map { preferences ->
            preferences[customPresetsKey]?.let { jsonStr ->
                try {
                    json.decodeFromString<List<CustomPresetData>>(jsonStr)
                } catch (e: Exception) {
                    emptyList()
                }
            } ?: emptyList()
        }.first()
    }
    
    suspend fun deleteCustomPreset(presetId: String) {
        val customPresetsKey = stringPreferencesKey("custom_presets")
        
        context.presetDataStore.edit { preferences ->
            val existingPresets = preferences[customPresetsKey]?.let { jsonStr ->
                try {
                    json.decodeFromString<List<CustomPresetData>>(jsonStr)
                } catch (e: Exception) {
                    emptyList()
                }
            } ?: emptyList()

            val updatedPresets = existingPresets.filter { it.id != presetId }
            preferences[customPresetsKey] = json.encodeToString(updatedPresets)
        }
        
        // If this was the current preset, clear it
        if (_currentPreset.value?.id == presetId) {
            clearCurrentPreset()
        }
    }
    
    fun getPresetPreview(presetType: DashboardPresetType): PresetPreview {
        val preset = getPresetByType(presetType)
        val enabledWidgets = preset.widgetConfigs.filter { it.isEnabled }
        val enabledActions = preset.fabConfig.actions.filter { it.isEnabled }
        
        return PresetPreview(
            presetType = presetType,
            enabledWidgets = enabledWidgets.size,
            totalWidgets = preset.widgetConfigs.size,
            fabStyle = preset.fabConfig.style,
            enabledActions = enabledActions.size,
            totalActions = preset.fabConfig.actions.size,
            widgetTypes = enabledWidgets.map { it.type.displayName },
            actionTypes = enabledActions.map { it.type.displayName },
            estimatedSetupTime = calculateSetupTime(enabledWidgets.size, enabledActions.size)
        )
    }
    
    private fun calculateSetupTime(widgetCount: Int, actionCount: Int): String {
        val baseTime = 30 // seconds
        val widgetTime = widgetCount * 10
        val actionTime = actionCount * 5
        val totalSeconds = baseTime + widgetTime + actionTime
        
        return when {
            totalSeconds < 60 -> "< 1 min"
            totalSeconds < 120 -> "1-2 min"
            totalSeconds < 300 -> "2-5 min"
            else -> "5+ min"
        }
    }
}

/**
 * Preview information for a preset
 */
data class PresetPreview(
    val presetType: DashboardPresetType,
    val enabledWidgets: Int,
    val totalWidgets: Int,
    val fabStyle: com.ainotebuddy.app.ui.dashboard.fab.FABMenuStyle,
    val enabledActions: Int,
    val totalActions: Int,
    val widgetTypes: List<String> = emptyList(),
    val actionTypes: List<String> = emptyList(),
    val estimatedSetupTime: String = "< 1 min"
)

