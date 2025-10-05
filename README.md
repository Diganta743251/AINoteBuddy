# AI NoteBuddy

**AI-powered note-taking app with OCR, voice notes, and smart organization**

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-orange.svg)](https://developer.android.com/jetpack/compose)
[![Material You](https://img.shields.io/badge/Design-Material%20You-purple.svg)](https://m3.material.io)

## 🚀 Overview

AI NoteBuddy is a cutting-edge Android note-taking application that combines artificial intelligence with powerful productivity features. Built with Kotlin and Jetpack Compose, it offers a modern, intuitive interface with Material You design principles.

### Key Features

- 🤖 **AI-Powered Assistance** - Smart suggestions, summaries, and content generation
- 📸 **OCR Text Recognition** - Extract text from images and documents instantly
- 🎤 **Voice Notes** - Convert speech to text with advanced voice recognition
- ☁️ **Cloud Sync** - Seamless Google Drive integration across devices
- 🔒 **Secure Vault** - Biometric authentication for sensitive notes
- 📱 **Material You Design** - Dynamic theming that adapts to your style
- 🏷️ **Smart Organization** - Auto-categorization and intelligent tagging
- 🔍 **Advanced Search** - Find any note instantly with powerful search
- 📊 **Rich Text Editor** - Full markdown support with formatting tools
- 🌙 **Dark Mode** - Automatic theme switching for comfort

## 📱 App Architecture

### Mobile App
- **Package**: `com.ainotebuddy.app`
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 36 (Android 15)
- **Architecture**: MVVM with Repository Pattern
- **Database**: Room Database with SQLite
- **UI Framework**: Jetpack Compose with Material3

### Wear OS Companion
- **Package**: `com.ainotebuddy.wear`
- **Wear Compose**: 1.2.1
- **Features**: Quick note viewing, voice input, sync with mobile app

## 🛠️ Technology Stack

### Core Technologies
- **Language**: Kotlin 1.9.22
- **Build System**: Gradle 8.2.2
- **UI Framework**: Jetpack Compose (BOM 2024.02.00)
- **Design System**: Material3 with Material You

### Key Dependencies
- **AndroidX Core**: 1.12.0
- **Compose UI**: Latest stable
- **Room Database**: 2.6.1
- **Navigation Compose**: Latest
- **Lifecycle ViewModel**: Latest
- **Coroutines**: For async operations
- **Retrofit**: API communication
- **Google Services**: Auth, Drive, Ads, Location, Maps
- **ML Kit**: Text recognition and AI features
- **Biometric**: Fingerprint and face authentication
- **CameraX**: Camera integration
- **WorkManager**: Background tasks

### AI & ML Features
- **Google ML Kit**: Text recognition (OCR)
- **Speech Recognition**: Voice-to-text conversion
- **AI Processing**: Smart content analysis and suggestions
- **Natural Language Processing**: Content categorization

## 🏗️ Project Structure

```
app/
├── src/main/java/com/ainotebuddy/app/
│   ├── ai/                     # AI services and processing
│   ├── auth/                   # Authentication services
│   ├── data/                   # Database entities and DAOs
│   ├── features/               # Feature-specific screens
│   ├── repository/             # Data repositories
│   ├── ui/                     # UI components and screens
│   │   ├── components/         # Reusable UI components
│   │   ├── dashboard/          # Main dashboard screens
│   │   ├── settings/           # Settings screens
│   │   └── theme/              # Theme and styling
│   ├── viewmodel/              # ViewModels for MVVM
│   ├── sync/                   # Cloud sync services
│   ├── security/               # Security and encryption
│   ├── voice/                  # Voice recognition
│   └── utils/                  # Utility classes
├── res/
│   ├── drawable/               # Vector drawables and icons
│   ├── layout/                 # XML layouts (legacy)
│   ├── mipmap-*/               # App icons
│   ├── values/                 # Strings, colors, themes
│   └── xml/                    # Configuration files
└── wear/                       # Wear OS companion app
```

## 🎨 Features Overview

### Core Note-Taking
- **Rich Text Editor**: Full markdown support with live preview
- **Voice Notes**: High-quality speech-to-text conversion
- **Image Integration**: Attach images with OCR text extraction
- **Quick Notes**: Fast note creation with shortcuts
- **Templates**: Pre-built note templates for different use cases

### AI-Powered Features
- **Smart Suggestions**: AI-generated content recommendations
- **Auto-Categorization**: Intelligent note organization
- **Content Summarization**: AI-powered note summaries
- **Tag Generation**: Automatic tag suggestions
- **Search Enhancement**: AI-improved search results

### Organization & Management
- **Folders**: Hierarchical note organization
- **Tags**: Flexible tagging system
- **Categories**: Smart categorization
- **Favorites**: Quick access to important notes
- **Archive**: Clean workspace with archived notes

### Sync & Backup
- **Google Drive Sync**: Automatic cloud synchronization
- **Cross-Device Access**: Notes available on all devices
- **Backup & Restore**: Complete data backup solutions
- **Version History**: Track note changes over time
- **Offline Support**: Full functionality without internet

### Security & Privacy
- **Biometric Lock**: Fingerprint and face authentication
- **Secure Vault**: Protected storage for sensitive notes
- **Encryption**: End-to-end encryption for cloud sync
- **Privacy Controls**: Granular privacy settings
- **Local Storage**: Option for local-only storage

### Productivity Tools
- **Calendar Integration**: Schedule and reminder features
- **Location Notes**: GPS-based note organization
- **Collaboration**: Share notes with others
- **Export Options**: Multiple export formats
- **Widget Support**: Home screen quick access

## 🚀 Getting Started

### Prerequisites
- Android Studio Arctic Fox or later
- JDK 11 or higher
- Android SDK 24+
- Google Services configuration

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/AINoteBuddy.git
   cd AINoteBuddy
   ```

2. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an existing project"
   - Navigate to the cloned directory

3. **Configure Google Services**
   - Add your `google-services.json` file to `app/` directory
   - Configure Google Drive API credentials
   - Set up AdMob (optional)

4. **Build and Run**
   ```bash
   ./gradlew :app:assembleDebug
   ./gradlew :app:installDebug
   ```

### Development Setup

1. **API Keys Configuration**
   - Create `local.properties` file
   - Add required API keys:
     ```properties
     OPENAI_API_KEY=your_openai_key
     GOOGLE_MAPS_API_KEY=your_maps_key
     ```

2. **Database Setup**
   - Room database auto-initializes on first run
   - No manual setup required

3. **Testing**
   ```bash
   ./gradlew :app:testDebug
   ./gradlew :app:connectedAndroidTest
   ```

## 📱 App Screens & Navigation

### Main Navigation
- **Dashboard**: Central hub with note overview
- **Note Editor**: Rich text editing with AI features
- **Search**: Advanced search with filters
- **Settings**: App configuration and preferences
- **Vault**: Secure notes with biometric access

### Feature Screens
- **AI Settings**: Configure AI providers and preferences
- **Sync Settings**: Cloud sync configuration
- **Import/Export**: Data management tools
- **Calendar View**: Time-based note organization
- **Collaboration**: Shared notes management

## 🔧 Configuration

### Build Variants
- **Debug**: Development build with debugging enabled
- **Release**: Production build with optimizations

### Gradle Configuration
```kotlin
android {
    compileSdk 36
    defaultConfig {
        minSdk 24
        targetSdk 36
        versionCode 2
        versionName "2.0.0"
    }
}
```

### ProGuard Rules
- Optimized for release builds
- Preserves essential classes for functionality
- Configured for Google Services compatibility

## 🧪 Testing

### Unit Tests
- Located in `app/src/test/`
- JUnit framework
- Mockito for mocking
- Run: `./gradlew :app:testDebug`

### Instrumentation Tests
- Located in `app/src/androidTest/`
- Espresso for UI testing
- Room database testing
- Run: `./gradlew :app:connectedAndroidTest`

## 📦 Release Management

### Keystore Configuration
- Release keystore: `keystore/release.keystore`
- Debug keystore: `keystore/debug.keystore`
- Configured in `app/build.gradle.kts`

### Version Management
- Version code: Auto-incremented
- Version name: Semantic versioning (x.y.z)
- Release notes: Updated for each version

### Play Store Release
- App Bundle (.aab) generation
- Automated testing pipeline
- Staged rollout configuration

## 🤝 Contributing

### Development Guidelines
- Follow Kotlin coding standards
- Use ktlint for code formatting
- Write comprehensive tests
- Document new features

### Pull Request Process
1. Fork the repository
2. Create feature branch
3. Make changes with tests
4. Submit pull request
5. Code review and merge

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 Support

### Documentation
- [User Guide](docs/user-guide.md)
- [API Documentation](docs/api.md)
- [Troubleshooting](docs/troubleshooting.md)

### Contact
- **Email**: support@ainotebuddy.com
- **GitHub Issues**: [Report bugs](https://github.com/yourusername/AINoteBuddy/issues)
- **Discord**: [Community chat](https://discord.gg/ainotebuddy)

## 🎯 Roadmap

### Version 2.1.0
- [ ] Enhanced AI features
- [ ] Improved collaboration tools
- [ ] Advanced export options
- [ ] Performance optimizations

### Version 2.2.0
- [ ] Tablet optimization
- [ ] Android TV support
- [ ] Advanced theming options
- [ ] Plugin system

### Future Plans
- [ ] Web companion app
- [ ] Desktop applications
- [ ] Advanced AI integrations
- [ ] Enterprise features

---

**Made with ❤️ by the AI NoteBuddy Team**

*Transform your note-taking experience with AI-powered intelligence.*