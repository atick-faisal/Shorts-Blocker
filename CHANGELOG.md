# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2025-12-06

### 🎉 Initial Release

This is the first public release of Shorts Blocker, an Android accessibility service that helps users block short-form content across multiple platforms.

### ✨ Features

- **Accessibility Service**: Implemented a comprehensive accessibility service to detect and block short-form content
- **Multi-Platform Support**: Block short-form content from multiple apps:
  - YouTube Shorts
  - Instagram Reels
  - (More platforms can be added in the future)
- **App-Specific Detectors**: Intelligent detection system with app-specific detector patterns
  - YouTube Shorts detector with false positive prevention on home screen and Shorts shelf
  - Instagram Reels detector with custom detection logic
- **User Interface**: Modern Material 3 UI with Jetpack Compose
  - Accessibility permission management screen
  - Tracked packages display with toggle functionality
  - Custom theme and typography
- **User Preferences**: DataStore-based preferences system
  - Toggle individual apps on/off
  - Persistent user settings
- **Lifecycle Management**: Proper Android lifecycle management with ViewModel architecture

### 🔧 Configuration

- Firebase integration for crashlytics
- Fastlane configuration for automated build and deployment
- ProGuard rules for release builds
- Spotless code formatting configuration
- Dokka documentation generation

### 📝 Documentation

- Comprehensive README with architecture overview
- Getting started guide
- LICENSE (Apache 2.0)
- CODE_OF_CONDUCT
- CONTRIBUTING guidelines
- Detailed code documentation comments

### 🏗️ Technical Details

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material 3
- **Architecture**: MVVM with ViewModel and Repository patterns
- **Minimum SDK**: API level as defined in build configuration
- **Build System**: Gradle with Kotlin DSL
- **Version Code**: 10000
- **Version Name**: 1.0.0

### 🎨 UI/UX

- Material 3 design system
- Custom color scheme and typography
- Responsive layouts
- Material icons integration
- User-friendly permission request flow

### 🧪 Quality Assurance

- Spotless code formatting enforcement
- Copyright headers on all source files
- Linting rules configured
- Crash reporting via Firebase Crashlytics

---

## Future Plans

- Add support for more short-form content platforms
- Add customizable blocking behaviors
- Implement statistics and usage tracking
- Add user feedback mechanisms

---

[1.0.0]: https://github.com/atick-faisal/Shorts-Blocker/releases/tag/v1.0.0

