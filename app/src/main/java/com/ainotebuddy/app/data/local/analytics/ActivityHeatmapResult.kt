package com.ainotebuddy.app.data.local.analytics

/**
 * Result class for activity heatmap queries
 */
data class ActivityHeatmapResult(
    val date: String,
    val hour: Int,
    val count: Int
)