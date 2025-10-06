plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-parcelize")
    kotlin("plugin.serialization") version "1.9.22"
    id("com.google.dagger.hilt.android") version "2.44"
}

android {
    namespace = "com.ainotebuddy.app"
    compileSdk = 36  // Required for latest dependencies

    defaultConfig {
        applicationId = "com.ainotebuddy.app"
        minSdk = 24
        targetSdk = 36  // Required for latest dependencies
        versionCode = 3
        versionName = "2.1.0"
        manifestPlaceholders["admob_app_id"] = "ca-app-pub-4084721334097026~2124246597"
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        
        // Multi-dex support
        multiDexEnabled = true
        
        // Build config fields
        buildConfigField("String", "VERSION_NAME", "\"${versionName}\"")
        buildConfigField("int", "VERSION_CODE", "${versionCode}")
        
        // Additional configurations for better APK generation
        setProperty("archivesBaseName", "AINoteBuddy-v${versionName}")
        
        // Resource optimization
        androidResources {
            localeFilters += listOf("en")
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file("../keystore/release.keystore")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: project.findProperty("KEYSTORE_PASSWORD") as String? ?: ""
            keyAlias = System.getenv("KEY_ALIAS") ?: project.findProperty("KEY_ALIAS") as String? ?: ""
            keyPassword = System.getenv("KEY_PASSWORD") ?: project.findProperty("KEY_PASSWORD") as String? ?: ""
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            versionNameSuffix = "-debug"
            buildConfigField("boolean", "DEBUG", "true")
            buildConfigField("boolean", "DEBUG_MODE", "true")
            manifestPlaceholders["app_name"] = "AINoteBuddy Debug"
        }
        
        create("staging") {
            initWith(getByName("release"))
            isDebuggable = true
            isMinifyEnabled = true
            isShrinkResources = true
            versionNameSuffix = "-staging"
            buildConfigField("boolean", "DEBUG", "false")
            buildConfigField("boolean", "DEBUG_MODE", "true")
            manifestPlaceholders["app_name"] = "AINoteBuddy Staging"
            
            // Use debug keystore for easy installation
            signingConfig = signingConfigs.getByName("debug")
            
            // Optimization for testing
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
        
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
            buildConfigField("boolean", "DEBUG", "false")
            buildConfigField("boolean", "DEBUG_MODE", "false")
            manifestPlaceholders["app_name"] = "AINoteBuddy"
            
            // App Bundle optimization
            ndk {
                // Keep only symbol table for debugging without inflating APK size
                debugSymbolLevel = "SYMBOL_TABLE"
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi"
        )
    }



    packaging {
        resources {
            excludes += setOf("META-INF/DEPENDENCIES")
        }
    }
    
    lint {
        abortOnError = false
        warningsAsErrors = false
        checkReleaseBuilds = true
        disable += setOf("MissingPermission", "UnusedAttribute")
    }
}

dependencies {
    // lintChecks(project(":lint"))

    // Core Android & Compose
    implementation(platform(libs.androidx.compose.bom.v20240200))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-window-size-class")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.runtime:runtime-livedata")
    implementation(libs.androidx.activity.compose.v1101)
    implementation(libs.androidx.lifecycle.runtime.ktx.v291)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation(libs.androidx.core.ktx.v1160)
    implementation(libs.androidx.navigation.compose)
    
    // Additional core dependencies
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.multidex:multidex:2.0.1")
    
    // Room Database
    implementation(libs.androidx.room.runtime.v272)
    implementation(libs.androidx.room.ktx.v272)
    ksp(libs.androidx.room.compiler.v272)
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    
    // Google Services
    implementation(libs.google.auth)
    implementation(libs.play.services.ads)
    implementation("com.google.android.gms:play-services-wearable:18.1.0")
    implementation("com.google.android.gms:play-services-location:21.2.0")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    
    // Google Drive API
    implementation(libs.google.drive)
    implementation(libs.google.api.client)
    implementation(libs.gson)
    
    // Billing
    implementation(libs.billing)
    
    // Biometric authentication
    implementation("androidx.biometric:biometric:1.2.0-alpha05")
    
    // AI Integration
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    
    // Camera and image processing
    implementation("androidx.camera:camera-core:1.4.2")
    implementation("androidx.camera:camera-camera2:1.4.2")
    implementation("androidx.camera:camera-lifecycle:1.4.0")
    implementation("androidx.camera:camera-view:1.4.0")
    
    // Image loading and processing
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("io.coil-kt:coil-gif:2.6.0")
    // Palette extraction
    implementation("androidx.palette:palette-ktx:1.0.0")
    
    // ML Kit for OCR and text recognition
    implementation("com.google.mlkit:text-recognition:16.0.0")
    implementation("com.google.mlkit:text-recognition-chinese:16.0.0")
    implementation("com.google.mlkit:text-recognition-devanagari:16.0.0")
    implementation("com.google.mlkit:text-recognition-japanese:16.0.0")
    implementation("com.google.mlkit:text-recognition-korean:16.0.0")
    
    // Work Manager for background tasks
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    
    // DataStore for preferences
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    
    // Security for encryption
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    
    // Firebase for collaborative features
    implementation("com.google.firebase:firebase-database-ktx:20.3.0")
    implementation("com.google.firebase:firebase-auth-ktx:22.3.0")
    implementation("com.google.firebase:firebase-analytics-ktx:21.5.0")
    implementation("com.google.firebase:firebase-firestore-ktx:25.0.0")
    
    // Hilt for dependency injection - required for collaborative features
    implementation("com.google.dagger:hilt-android:2.44")
    ksp("com.google.dagger:hilt-android-compiler:2.44")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
    implementation("androidx.hilt:hilt-work:1.1.0")
    ksp("androidx.hilt:hilt-compiler:1.1.0")
    
    // Fix JavaPoet version conflict
    implementation("com.squareup:javapoet:1.13.0")
    
    // Retrofit for API calls
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    
    // QR code generation
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    
    // Markdown support
    implementation("io.noties.markwon:core:4.6.2")
    implementation("io.noties.markwon:html:4.6.2")
    implementation("io.noties.markwon:image:4.6.2")
    
    // Web scraping for web clipper
    implementation("org.jsoup:jsoup:1.17.2")
    
    // Additional UI components
    implementation("androidx.compose.material:material-icons-core:1.6.1")
    implementation("androidx.compose.material:material-icons-extended:1.6.1")
    
    // Drag and drop reorderable list
    implementation("org.burnoutcrew.composereorderable:reorderable:0.9.6")
    
    // Swipe actions for notes
    implementation("me.saket.swipe:swipe:1.2.0")
    
    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    // implementation("io.noties.markwon:latex:4.6.2") // Not available
    // implementation("io.noties.markwon:math:4.6.2") // Not available
    // implementation("io.noties.markwon:strikethrough:4.6.2") // Not available
    // implementation("io.noties.markwon:table:4.6.2") // Not available
    // implementation("io.noties.markwon:tasklist:4.6.2") // Not available
    // implementation("io.noties.markwon:typography:4.6.2") // Not available
    
    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit.v121)
    androidTestImplementation(libs.androidx.espresso.core.v361)
    androidTestImplementation(platform(libs.androidx.compose.bom.v20240200))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}


