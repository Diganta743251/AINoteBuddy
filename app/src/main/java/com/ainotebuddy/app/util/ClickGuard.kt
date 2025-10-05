package com.ainotebuddy.app.util

object ClickGuard {
    fun allow(lastClickTime: Long, intervalMs: Long = 500, now: Long = System.currentTimeMillis()): Boolean {
        return (now - lastClickTime) > intervalMs
    }
}