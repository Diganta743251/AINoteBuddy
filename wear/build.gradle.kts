plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.ainotebuddy.wear"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ainotebuddy.wear"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    configurations.all {
        exclude(group = "org.jetbrains", module = "annotations-java5")
    }
    implementation("androidx.wear.compose:compose-material:1.2.1")
    implementation(libs.androidx.activity.compose.v1101)
    implementation(libs.androidx.core.ktx.v1160)
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.20")
    implementation("com.google.android.gms:play-services-wearable:18.1.0") // Wear OS Data Layer API
    implementation(libs.gson)
    implementation("androidx.compose.material3:material3:1.1.2")
    implementation("androidx.compose.ui:ui:1.5.4")
    implementation("androidx.compose.runtime:runtime:1.5.4")
    implementation("androidx.compose.foundation:foundation:1.5.4")
} 