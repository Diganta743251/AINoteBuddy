pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

rootProject.name = "lint-tests"

includeBuild("../") {
    dependencySubstitution {
        substitute(module("com.ainotebuddy:lint")).using(project(":lint"))
    }
}