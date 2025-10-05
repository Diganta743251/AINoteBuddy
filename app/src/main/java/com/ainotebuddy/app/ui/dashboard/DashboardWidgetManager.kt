package com.ainotebuddy.app.ui.dashboard

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.Serializable

@Serializable
data class DashboardConfiguration(
    val widgets: List<SerializableWidgetConfig> = DashboardWidgetManager.getDefaultWidgets(),
    val layoutStyle: String = "default",
    val compactMode: Boolean = false
)

@Serializable
data class SerializableWidgetConfig(
    val typeId: String,
    val isEnabled: Boolean,
    val position: Int,
    val customSettings: Map<String, String> = emptyMap()
)

class DashboardWidgetManager(private val context: Context) {
    private val _currentConfig = mutableStateOf(getDefaultConfiguration())
    val currentConfig: State<DashboardConfiguration> = _currentConfig

    // Simple in-memory flow for now
    val configFlow: Flow<DashboardConfiguration> = flowOf(_currentConfig.value)

    suspend fun updateConfiguration(config: DashboardConfiguration) {
        _currentConfig.value = config
    }

    suspend fun toggleWidget(widgetType: DashboardWidgetType, enabled: Boolean) {
        val updated = _currentConfig.value.widgets.map { w ->
            if (w.typeId == widgetType.id) w.copy(isEnabled = enabled) else w
        }
        _currentConfig.value = _currentConfig.value.copy(widgets = updated)
    }

    suspend fun reorderWidgets(fromIndex: Int, toIndex: Int) {
        val widgets = _currentConfig.value.widgets.toMutableList()
        if (fromIndex in widgets.indices && toIndex in widgets.indices) {
            val item = widgets.removeAt(fromIndex)
            widgets.add(toIndex, item)
            val rePos = widgets.mapIndexed { idx, w -> w.copy(position = idx) }
            _currentConfig.value = _currentConfig.value.copy(widgets = rePos)
        }
    }

    suspend fun resetToDefault() {
        _currentConfig.value = getDefaultConfiguration()
    }

    suspend fun applyConfiguration(configs: List<DashboardWidgetConfig>) {
        val serializable = configs.map { cfg ->
            SerializableWidgetConfig(
                typeId = cfg.type.id,
                isEnabled = cfg.isEnabled,
                position = cfg.position,
                customSettings = cfg.customSettings.mapValues { it.value.toString() }
            )
        }
        updateConfiguration(DashboardConfiguration(widgets = serializable))
    }

    fun getEnabledWidgets(): List<DashboardWidgetConfig> {
        return getAllWidgets().filter { it.isEnabled }
    }

    fun getAllWidgets(): List<DashboardWidgetConfig> {
        return _currentConfig.value.widgets
            .sortedBy { it.position }
            .mapNotNull { sw ->
                val t = DashboardWidgetType.values().find { it.id == sw.typeId }
                t?.let {
                    DashboardWidgetConfig(
                        type = it,
                        isEnabled = sw.isEnabled,
                        position = sw.position,
                        customSettings = sw.customSettings
                    )
                }
            }
    }

    companion object {
        fun getDefaultConfiguration(): DashboardConfiguration {
            return DashboardConfiguration(
                widgets = getDefaultWidgets(),
                layoutStyle = "default",
                compactMode = false
            )
        }

        fun getDefaultWidgets(): List<SerializableWidgetConfig> {
            return DashboardWidgetType.values().mapIndexed { index, type ->
                SerializableWidgetConfig(
                    typeId = type.id,
                    isEnabled = type.defaultEnabled,
                    position = index
                )
            }
        }
    }
}

// Helper to convert UI config to serializable form (not strictly required by above code but kept for API parity)
fun DashboardWidgetConfig.toSerializable(): SerializableWidgetConfig {
    return SerializableWidgetConfig(
        typeId = type.id,
        isEnabled = isEnabled,
        position = position,
        customSettings = customSettings.mapValues { it.value.toString() }
    )
}