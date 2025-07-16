plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
}

android {
    namespace = "com.ainotebuddy.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ainotebuddy.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 2
        versionName = "2.0.0"
        manifestPlaceholders["admob_app_id"] = "ca-app-pub-3940256099942544~3347511713"
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        
        // Multi-dex support
        multiDexEnabled = true
        
        // Build config fields
        buildConfigField("String", "VERSION_NAME", "\"${versionName}\"")
        buildConfigField("int", "VERSION_CODE", "${versionCode}")
    }

    signingConfigs {
        create("release") {
            // These should be set via environment variables or gradle.properties
            storeFile = file("../keystore/release.keystore")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "android"
            keyAlias = System.getenv("KEY_ALIAS") ?: "key0"
            keyPassword = System.getenv("KEY_PASSWORD") ?: "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
            
            buildConfigField("boolean", "DEBUG_MODE", "false")
            manifestPlaceholders["app_name"] = "AINoteBuddy"
        }
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            
            buildConfigField("boolean", "DEBUG_MODE", "true")
            manifestPlaceholders["app_name"] = "AINoteBuddy Debug"
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += setOf("META-INF/DEPENDENCIES")
        }
    }
}

dependencies {
    configurations.all {
        exclude(group = "org.jetbrains", module = "annotations-java5")
    }
    // Core Android & Compose
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    
    // Additional core dependencies
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.multidex:multidex:2.0.1")
    
    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    
    // Google Services
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    implementation("com.google.android.gms:play-services-ads:22.6.0")
    implementation("com.google.android.gms:play-services-wearable:18.1.0")
    implementation("com.google.android.gms:play-services-location:21.2.0")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    
    // Google Drive API
    implementation("com.google.apis:google-api-services-drive:v3-rev20240123-2.0.0")
    implementation("com.google.api-client:google-api-client-android:2.2.0")
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Billing
    implementation("com.android.billingclient:billing:6.1.0")
    
    // Biometric authentication
    implementation("androidx.biometric:biometric:1.2.0-alpha05")
    
    // Camera and image processing
    implementation("androidx.camera:camera-core:1.4.0")
    implementation("androidx.camera:camera-camera2:1.4.0")
    implementation("androidx.camera:camera-lifecycle:1.4.0")
    implementation("androidx.camera:camera-view:1.4.0")
    
    // Image loading and processing
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("io.coil-kt:coil-gif:2.6.0")
    
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
    
    // Hilt for dependency injection - temporarily commented out due to annotation processing issues
    // implementation("com.google.dagger:hilt-android:2.50")
    // kapt("com.google.dagger:hilt-android-compiler:2.50")
    // implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    
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
    // implementation("io.noties.markwon:latex:4.6.2") // Not available
    // implementation("io.noties.markwon:math:4.6.2") // Not available
    // implementation("io.noties.markwon:strikethrough:4.6.2") // Not available
    // implementation("io.noties.markwon:table:4.6.2") // Not available
    // implementation("io.noties.markwon:tasklist:4.6.2") // Not available
    // implementation("io.noties.markwon:typography:4.6.2") // Not available
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}


