pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://plugins.gradle.org/m2/") }
    }
    val requestedTasks = gradle.startParameter.taskNames
    val runningOnlyLintTests = requestedTasks.any { it.startsWith(":lint-tests") }
    plugins {
        if (!runningOnlyLintTests) {
            id("com.android.application") version "8.11.1"
            id("org.jetbrains.kotlin.android") version "2.1.0"
        }
        // Always ensure Kotlin JVM is available for pure JVM modules like lint and lint-tests
        id("org.jetbrains.kotlin.jvm") version "2.1.0"
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://plugins.gradle.org/m2/") }
    }
}
rootProject.name = "AINoteBuddy"

val requestedTasks = gradle.startParameter.taskNames
val runningOnlyLintTests = requestedTasks.any { it.startsWith(":lint-tests") }

if (!runningOnlyLintTests) {
    include(":app")
} else {
    println("[settings] Skipping :app for lint-tests run")
}
include(":lint")
include(":lint-tests")
// Temporarily disabled wear module to fix build issues
// include(":wear")
