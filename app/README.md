# Module :app

Technical deep dive into the Shorts Blocker architecture, component interactions, and data flow.

## Architecture Overview

Shorts Blocker follows a clean architecture pattern with clear separation of concerns:

- **UI Layer**: Jetpack Compose with MVVM
- **Domain Layer**: Business logic and detectors
- **Data Layer**: DataStore for preferences
- **Service Layer**: Accessibility service running independently

## High-Level Architecture

```mermaid
graph TB
    subgraph "UI Layer"
        A[MainActivity] --> B[MainScreen]
        B --> C[MainViewModel]
        C --> D[ServiceState]
        B --> E[Components]
    end

    subgraph "Data Layer"
        F[UserPreferencesProvider]
        G[DataStore]
        F --> G
        H[PackageConstants]
    end

    subgraph "Service Layer"
        I[ShortFormContentBlockerService]
        J[YouTubeShortsDetector]
        K[InstagramReelsDetector]
        L[ShortFormContentDetector Interface]
        I --> L
        J -. implements .-> L
        K -. implements .-> L
    end

    subgraph "Utils"
        M[AccessibilityServiceManager]
    end

    C --> F
    C --> M
    I --> F
    I --> J
    I --> K
    N[Android System] -. accessibility events .-> I
    I -. back action .-> N
    style A fill: #e1f5ff
    style I fill: #ffe1e1
    style F fill: #e1ffe1
    style L fill: #fff4e1
```

## Detailed Component Flow

```mermaid
sequenceDiagram
    participant User
    participant MainActivity
    participant MainViewModel
    participant DataStore
    participant AccessibilityService
    participant Detector
    participant Android
    User ->> MainActivity: Opens App
    MainActivity ->> MainViewModel: Initialize
    MainViewModel ->> DataStore: getTrackedPackages()
    DataStore -->> MainViewModel: List<String>
    MainViewModel -->> MainActivity: ServiceState
    MainActivity ->> User: Shows Status
    User ->> MainActivity: Enable Service
    MainActivity ->> Android: Open Accessibility Settings
    User ->> Android: Enables Service
    Android ->> AccessibilityService: onServiceConnected()
    AccessibilityService ->> DataStore: Get tracked packages
    DataStore -->> AccessibilityService: Package list
    AccessibilityService ->> AccessibilityService: Configure service info

    loop Content Monitoring
        Android ->> AccessibilityService: onAccessibilityEvent()
        AccessibilityService ->> Detector: isShortFormContent()
        Detector ->> Detector: Analyze UI tree
        alt Short-form detected
            Detector -->> AccessibilityService: true
            AccessibilityService ->> AccessibilityService: Check cooldown
            AccessibilityService ->> Android: performGlobalAction(BACK)
        else Not detected
            Detector -->> AccessibilityService: false
        end
    end
```

## Data Flow

```mermaid
flowchart LR
    subgraph User Actions
        A[User Toggles Package]
    end

    subgraph UI Layer
        B[MainScreen] --> C[MainViewModel]
    end

    subgraph Data Layer
        D[UserPreferencesProvider]
        E[(DataStore)]
        D --> E
    end

    subgraph Service Layer
        F[ShortFormContentBlockerService]
        G[Detector]
    end

    A --> B
    C --> D
    D --> E
    E -. observes .-> D
    D -. flow .-> C
    C -. state .-> B
    F -. reads .-> D
    F --> G
    G -. detection result .-> F
    H[Android System] -. events .-> F
    F -. actions .-> H
    style E fill: #f9f, stroke: #333, stroke-width: 3px
    style F fill: #ff9, stroke: #333, stroke-width: 3px
    style H fill: #9f9, stroke: #333, stroke-width: 3px
```

## Detection Algorithm

### YouTube Shorts

```mermaid
flowchart TD
    A[Accessibility Event] --> B{Container Size Check}
    B -->|Too Small| Z[Not Shorts]
    B -->|Large Enough| C{View ID Check}
    C -->|Shelf/Grid/Chip| Z
    C -->|Player ID| D{Full Screen Check}
    C -->|Unknown| D
    D -->|< 85% height| Z
    D -->|≥ 85% height| E{Aspect Ratio}
    E -->|≤ 1 . 5| Z
    E -->|> 1 . 5| F{Player Controls}
    F -->|Found| G[Shorts Detected]
F -->|Not Found but has Player ID|G
F -->|Not Found|Z

style G fill: #90EE90
style Z fill: #FFB6C6
```

### Instagram Reels

```mermaid
flowchart TD
    A[Accessibility Event] --> B{Reels Tab Selected?}
    B -->|No| C{Fragment Container?}
    B -->|Yes| D{Active Video?}
    C -->|No| E{Activity Class?}
    C -->|Yes| D
    E -->|Not Reels| Z[Not Reels]
    E -->|Reels Class| D
    D -->|Indicators < 2| Z
    D -->|Indicators ≥ 2| F[Reels Detected]

style F fill: #90EE90
style Z fill: #FFB6C6
```

## State Management

### ViewModel State Flow

```mermaid
stateDiagram-v2
    [*] --> Initializing
    Initializing --> Checking: checkPermission()
    Checking --> NotGranted: Service Disabled
    Checking --> Granted: Service Enabled
    NotGranted --> Monitoring: openAccessibilitySettings()
    Monitoring --> Checking: Poll every 1s
    Granted --> [*]

    state Granted {
        [*] --> LoadingPackages
        LoadingPackages --> DisplayingActive
        DisplayingActive --> UpdatingPackage: togglePackage()
        UpdatingPackage --> DisplayingActive
    }
```

### Service Lifecycle

```mermaid
stateDiagram-v2
    [*] --> Disconnected
    Disconnected --> Connected: User enables in Settings
    Connected --> Configured: Load preferences & setup
    Configured --> Monitoring: Start listening

    state Monitoring {
        [*] --> Idle
        Idle --> ProcessingEvent: Event received
        ProcessingEvent --> DetectingContent: Call detector
        DetectingContent --> Idle: Not detected
        DetectingContent --> CheckingCooldown: Detected
        CheckingCooldown --> Idle: In cooldown
        CheckingCooldown --> PerformingAction: Cooldown passed
        PerformingAction --> Idle: Back action sent
    }

    Monitoring --> Disconnected: User disables
    Disconnected --> [*]
```
