plugins {
    kotlin("jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}



dependencies {
    // Depend on local lint project for both compile and test to aid IDE resolution
    implementation(project(":lint"))
    testImplementation(project(":lint"))
    testRuntimeOnly(project(":lint"))
    testImplementation("com.android.tools.lint:lint-tests:31.4.0")
    testImplementation(kotlin("test"))
}

sourceSets {
    test {
        java.setSrcDirs(listOf("src/test/java"))
    }
}