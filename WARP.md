# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Build & Development Commands

### Building the App
```bash
# Debug build
./gradlew assembleDebug

# Release build (requires keystore configuration)
./gradlew assembleRelease

# Install debug build on connected device
./gradlew installDebug

# Clean build
./gradlew clean
```

### Testing
```bash
# Run unit tests
./gradlew testDebug

# Run instrumentation tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Run specific test class
./gradlew testDebug --tests "com.ainotebuddy.app.*"

# Run lint checks
./gradlew lint
```

### Development
```bash
# Generate debug APK
./gradlew :app:assembleDebug

# Build Android App Bundle for Play Store
./gradlew bundleRelease

# Check dependencies
./gradlew dependencies

# Verify Kotlin compilation
./gradlew compileDebugKotlin
```

## Architecture Overview

### Tech Stack
- **Language**: Kotlin 2.1.0
- **UI Framework**: Jetpack Compose with Material3/Material You design
- **Architecture**: MVVM with Repository Pattern
- **Database**: Room Database with SQLite
- **Dependency Injection**: Hilt/Dagger
- **Build System**: Gradle 8.11.1 with KSP (Kotlin Symbol Processing)
- **Min SDK**: 24 (Android 7.0), Target SDK: 36 (Android 15)

### Core Architecture Patterns

#### MVVM + Repository Pattern
- **ViewModels**: Handle UI state and business logic (`viewmodel/` package)
- **Repositories**: Abstract data access layer (`data/repository/` package)
- **DAOs**: Room database access objects (`data/dao/` package)
- **Entities**: Room database entities (`data/` package with `*Entity.kt` files)
- **Domain Models**: Pure Kotlin data classes (e.g., `data/Note.kt`)

#### Dependency Injection Structure
- **Hilt Modules**: Located in `di/` package
  - `AppModule.kt` - Core app dependencies
  - `AIModule.kt` - AI service dependencies
  - `DatabaseModule.kt` - Room database configuration
  - Additional feature-specific modules for collaboration, templates, etc.

#### Multi-Module Project Structure
```
:app - Main Android application module
:lint - Custom lint rules module  
:lint-tests - Tests for custom lint rules
:wear - Wear OS companion app (temporarily disabled)
```

### Key Features Architecture

#### AI Integration
- **Primary AI Service**: `ai/AIService.kt` - Core AI functionality
- **AI Providers**: Multiple AI provider implementations in `ai/` package
- **AI Features**: 
  - Smart categorization and tagging
  - Sentiment analysis
  - Content suggestions
  - OCR text recognition (ML Kit)
  - Voice-to-text conversion

#### Database Architecture  
- **Room Database**: Single source of truth for local data
- **Entities**: `NoteEntity`, `CategoryEntity`, `TagEntity`, `FolderEntity`, etc.
- **Offline-First**: Full offline capability with cloud sync
- **Data Mappers**: Convert between entities and domain models

#### Security Features
- **Biometric Authentication**: Fingerprint/face unlock for secure notes
- **Encryption**: AES encryption for sensitive content
- **Secure Vault**: Special encrypted storage area

#### Collaboration System
- **Real-time Editing**: Operational Transform engine for collaborative editing
- **Firebase Integration**: Real-time database and authentication
- **Presence System**: Show active collaborators
- **Comments**: Threaded commenting system

## Development Guidelines

### Code Organization
- **Package by Feature**: Code organized by feature areas (ai, auth, collaboration, etc.)
- **Clean Architecture**: Separation of concerns with clear dependency directions
- **Compose-First**: All UI built with Jetpack Compose, no XML layouts

### AI Development
- AI services are abstracted through interfaces in the `ai/` package
- Multiple AI provider implementations (OpenAI, local processing, etc.)
- AI features can be toggled in settings and respect "Pause AI Processing" preference
- AI embeddings system for semantic search capabilities

### Testing Strategy
- **Unit Tests**: Located in `src/test/` for business logic
- **Instrumentation Tests**: Located in `src/androidTest/` for UI and integration testing
- **Custom Lint Rules**: Custom lint module with tests for code quality

### Build Configuration
- **Debug Build**: Includes debugging tools, different app name suffix
- **Release Build**: ProGuard enabled, signed with release keystore
- **Multi-dex**: Enabled for large dependency set
- **BuildConfig Fields**: Version info and debug flags

### Key Dependencies to Know
- **Hilt**: `@AndroidEntryPoint` on Activities, `@Inject` for dependency injection
- **Room**: Database entities use `@Entity`, DAOs use `@Dao`
- **Compose**: Modern declarative UI framework
- **ML Kit**: Google's on-device ML for OCR and text recognition
- **Firebase**: Real-time features and authentication
- **WorkManager**: Background tasks (embeddings updates, sync)

### Gradle Tips
- Use `./gradlew --no-daemon` if experiencing daemon issues
- KSP is used instead of KAPT for faster annotation processing
- Version catalogs are used (`gradle/libs.versions.toml`)
- Custom lint rules are automatically applied during builds

### Development Environment Setup
1. Requires Android Studio Arctic Fox or later
2. JDK 17 required for Kotlin 2.1.0
3. Add `google-services.json` to `app/` directory for Firebase features
4. Configure API keys in `local.properties` for external services
5. Set up release keystore for release builds

### Common Development Patterns
- **State Management**: Use `StateFlow` and `Flow` for reactive data streams  
- **Navigation**: Jetpack Navigation Compose for screen navigation
- **Theming**: Material3 dynamic theming based on device wallpaper
- **Async Operations**: Coroutines with proper scope management
- **Error Handling**: Result pattern for error propagation

### Performance Considerations
- **Lazy Loading**: Lists use lazy loading patterns
- **Image Loading**: Coil library for efficient image loading
- **Background Processing**: WorkManager for non-critical background tasks
- **Memory Management**: Proper lifecycle awareness in ViewModels