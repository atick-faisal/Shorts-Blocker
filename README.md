# Shorts Blocker

An Android app that automatically blocks short-form content (YouTube Shorts, Instagram Reels) using accessibility services.

## What It Does

Shorts Blocker detects when you're watching short-form content and automatically hits the back button. No more endless scrolling.

**Supported Apps:**
- YouTube Shorts
- Instagram Reels

## How It Works

1. Uses Android Accessibility Service to monitor app activity
2. Detects short-form content based on UI patterns and screen layout
3. Automatically navigates back when short-form content is detected
4. Cooldown period prevents repeated actions

## Installation

### From Release
1. Download the latest APK from [Releases](../../releases)
2. Install the APK
3. Enable accessibility service in Settings

### Build From Source
See [Getting Started Guide](docs/getting-started.md)

## Usage

1. Open the app
2. Tap "Open Settings" to enable accessibility service
3. Find "Shorts Blocker" in the accessibility settings list
4. Toggle it on
5. Grant permission
6. Return to the app
7. Toggle which apps you want to monitor (YouTube, Instagram)

That's it. The app now monitors your selected apps in the background.

## Privacy

- **No internet permission** - Your data never leaves your device
- **No analytics or tracking** - Zero telemetry
- **Open source** - Verify the code yourself
- Only monitors apps you explicitly enable

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose with Material 3
- **Architecture**: MVVM
- **State Management**: StateFlow + DataStore
- **Detection**: Custom accessibility service with platform-specific detectors

## Requirements

- Android 7.0 (API 24) or higher
- Accessibility service permission

## Development

See [Getting Started Guide](docs/getting-started.md) for development setup.

## License

```
Copyright 2025 Atick Faisal

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## Contributing

PRs welcome. Keep it simple.

## Known Limitations

- Detection may not work on all device configurations
- Some app updates may break detection (will be updated as needed)
- Only works when apps are in foreground

## FAQ

**Q: Why do you need accessibility permission?**  
A: To detect UI elements and navigate back automatically. It's the only way to detect short-form content.

**Q: Does this work on [other platform]?**  
A: Android only. iOS doesn't allow this level of system access.

**Q: Can I add more apps?**  
A: Yes! Check the [Getting Started Guide](docs/getting-started.md) to learn how to implement detectors for other apps.

**Q: Why isn't it detecting content?**  
A: Detection patterns may need updating after app updates. Open an issue with your app version.
