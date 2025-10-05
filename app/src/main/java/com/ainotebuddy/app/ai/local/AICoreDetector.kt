package com.ainotebuddy.app.ai.local

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/**
 * Detect Gemini Nano availability via AICore (Android 14+).
 * This is a conservative placeholder; refine when integrating real AICore APIs.
 */
object AICoreDetector {
    private const val AI_CORE_PACKAGE = "com.google.android.aicore"

    fun isAvailable(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return false
        return try {
            context.packageManager.getPackageInfo(AI_CORE_PACKAGE, PackageManager.PackageInfoFlags.of(0))
            true
        } catch (_: Throwable) {
            false
        }
    }
}