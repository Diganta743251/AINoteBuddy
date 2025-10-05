plugins {
    `java-library`
    kotlin("jvm")
}

group = "com.ainotebuddy"
version = "1.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    // Lint APIs - align roughly with AGP 8.5+; adjust if needed
    compileOnly("com.android.tools.lint:lint-api:31.4.0")
    compileOnly("com.android.tools.lint:lint-checks:31.4.0")
    testImplementation("com.android.tools.lint:lint-tests:31.4.0")
    testImplementation(kotlin("test"))
}

// Ensure the manifest service file is packaged
tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}