---
description: Repository Information Overview
alwaysApply: true
---

# AINoteBuddy Repository Information

## Repository Summary
AINoteBuddy is an Android application for note-taking with AI capabilities. It consists of a mobile app and a companion Wear OS app, built using Kotlin and Jetpack Compose.

## Repository Structure
- **app/**: Main Android mobile application module
- **wear/**: Wear OS companion application module
- **gradle/**: Gradle configuration files and wrapper
- **.zencoder/**: Documentation and configuration files
- **.vscode/**: VS Code editor configuration

### Main Repository Components
- **Mobile App**: Full-featured note-taking application with AI capabilities
- **Wear OS App**: Companion app for smartwatches
- **Shared Infrastructure**: Common code and resources shared between apps

## Projects

### Mobile App
**Configuration File**: app/build.gradle.kts

#### Language & Runtime
**Language**: Kotlin
**Version**: Kotlin 1.9.22
**Build System**: Gradle 8.2.2
**Package Manager**: Gradle
**Compose Version**: 2024.02.00 (BOM)

#### Dependencies
**Main Dependencies**:
- Jetpack Compose (UI, Material3)
- AndroidX (Core, Lifecycle, Navigation)
- Room Database (2.6.1)
- Google Services (Auth, Drive, Ads, Location, Maps)
- ML Kit (Text Recognition)
- Retrofit (API calls)
- Coroutines (Async operations)
- Biometric Authentication
- Camera and Image Processing

#### Build & Installation
```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

#### Testing
**Framework**: JUnit, Espresso
**Test Location**: app/src/androidTest, app/src/test
**Naming Convention**: *Test.kt
**Run Command**:
```bash
./gradlew :app:testDebug
./gradlew :app:connectedAndroidTest
```

### Wear OS App
**Configuration File**: wear/build.gradle.kts

#### Language & Runtime
**Language**: Kotlin
**Version**: Kotlin 1.9.22
**Build System**: Gradle 8.2.2
**Package Manager**: Gradle
**Compose Version**: 1.5.4

#### Dependencies
**Main Dependencies**:
- Wear Compose (1.2.1)
- AndroidX Core (1.12.0)
- Google Play Services Wearable (18.1.0)
- Compose Material3 (1.1.2)
- Compose UI (1.5.4)

#### Build & Installation
```bash
./gradlew :wear:assembleDebug
./gradlew :wear:installDebug
```

## Features & Functionality

### Mobile App
- Note creation, editing, and organization
- AI-powered note processing and suggestions
- Voice recognition for note dictation
- Google Drive sync capabilities
- Location-based notes
- Biometric authentication for secure notes
- Camera integration for image capture
- OCR (Optical Character Recognition) via ML Kit
- QR code generation
- Markdown support
- Widget for quick note access

### Wear OS App
- Quick note viewing and creation
- Sync with mobile app
- Voice input for notes
- Simplified UI for wearable devices