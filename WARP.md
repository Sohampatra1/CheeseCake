# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Project Overview
CheeseCake is an Android app built with Jetpack Compose that combines hydration tracking with menstrual cycle tracking. The app uses ML Kit for real-time camera verification of drinking actions and Gemini AI for secondary verification.

## Build & Development Commands

### Build
```bash
./gradlew build
```

### Run on Device/Emulator
```bash
./gradlew installDebug
```

### Clean Build
```bash
./gradlew clean
./gradlew build
```

### Run Tests
```bash
# Unit tests
./gradlew test

# Instrumentation tests
./gradlew connectedAndroidTest
```

### Check Dependencies
```bash
./gradlew dependencies
```

## Architecture

### Core Tech Stack
- **UI Framework**: Jetpack Compose with Material 3
- **Dependency Injection**: Hilt/Dagger
- **Database**: Room (SQLite)
- **Persistence**: DataStore for preferences
- **Navigation**: Jetpack Navigation Compose
- **Background Work**: WorkManager for periodic reminders
- **ML/AI**: ML Kit (Face Detection, Object Detection) + Gemini 1.5 Flash API

### Key Components

#### Data Layer
- **AppDatabase**: Room database with two entities:
  - `WaterIntakeRecord`: Tracks hydration logs with timestamps
  - `PeriodRecord`: Tracks menstrual cycle start dates
- **DatabaseModule**: Hilt module providing DAOs and database instance
- DAOs provide Flow-based reactive data access

#### ViewModels (Hilt-injected)
- **MainViewModel**: Manages water intake records
- **PeriodViewModel**: Manages period tracking, calculates cycle predictions (28-day default)
- **CameraViewModel**: Handles camera-based water verification logic

#### UI Screens
1. **MainRootScreen**: Bottom navigation container with two tabs:
   - Tab 0: Hydration (WaterTrackerScreen)
   - Tab 1: Cycles (PeriodTrackerScreen)
2. **WaterTrackerScreen**: Shows daily cup count, manual/camera verification options, reminder toggle
3. **PeriodTrackerScreen**: Shows next predicted period date, cycle status, log button
4. **CameraScreen**: ML Kit-powered verification flow
5. **CalendarScreen**: Historical view (shared by both trackers)

#### Camera Verification Flow
The app uses a multi-stage verification approach:
1. **ML Kit Face Detection**: Detects face landmarks (mouth position) and head tilt angle
2. **ML Kit Object Detection**: Identifies bottles/cups near the mouth
3. **Local Validation**: Checks if object is within distance threshold of mouth + correct head tilt (>10°) for 2+ seconds
4. **Gemini AI Verification**: Final verification step using vision model to confirm actual drinking action
5. **Success**: Record saved, notification dismissed (if alarm mode)

Key verification constants in CameraScreen:
- `REQUIRED_DURATION_MS = 2000L` (2 seconds of valid pose)
- `GRACE_PERIOD_MS = 500L` (tolerance for brief invalid frames)
- `REQUIRED_TILT_ANGLE = 10.0f` (head tilt in degrees)
- `DISTANCE_THRESHOLD = 350.0` (pixels between mouth and object)

#### Background Workers
- **ReminderWorker**: Periodic (30 min) water reminders via notification
- **PeriodWorker**: Checks every 6 hours for upcoming/late periods

#### Notifications
- **NotificationManager**: Handles scheduling and sending notifications
- Water reminders launch app with alarm parameter (`EXTRA_REMINDER_TRIGGER`)
- Period reminders open app to Cycles tab (`EXTRA_OPEN_TAB = 1`)

### Navigation Structure
```
main (MainRootScreen)
├── camera?beep={boolean}
└── calendar
```

### Package Structure
All code is in `com.example.cheesecake`:
- Root: Activities, Application, Database, ViewModels, Workers, Screens
- `ui.theme`: Color, Theme, Type definitions

## Important Notes

### API Keys
- Gemini API key is hardcoded in `GeminiVerifier.kt` (line 14)
- **Security**: This should be moved to BuildConfig or a secure key management solution before production

### Permissions
Required permissions in AndroidManifest:
- `CAMERA`: For drinking verification
- `INTERNET`: For Gemini API calls

### Database Migrations
Database uses `.fallbackToDestructiveMigration()` - schema changes will wipe data. Implement proper migrations for production.

### Testing
- Test instrumentation runner: `androidx.test.runner.AndroidJUnitRunner`
- Unit tests: JUnit 4
- UI tests: Compose UI Test + Espresso

### Firebase Integration
The app includes Firebase Crashlytics for error tracking.

## Development Patterns

### Async Operations
- Use `viewModelScope.launch` for database operations in ViewModels
- Camera analysis runs on dedicated `Executors.newSingleThreadExecutor()`
- Gemini API calls use coroutine scope with suspend functions

### State Management
- ViewModels expose `Flow` and `StateFlow` for reactive UI updates
- Use `collectAsState()` in Composables to observe data
- Preferences stored via SharedPreferences (e.g., reminder toggle state)

### Hilt Usage
- Application class: `@HiltAndroidApp` on `WaterReminderApplication`
- Activities: `@AndroidEntryPoint` on `MainActivity`
- ViewModels: `@HiltViewModel` with `@Inject constructor`
- Modules: `@Module` + `@InstallIn(SingletonComponent::class)` for singletons

### Compose Best Practices in This Codebase
- Screens receive `NavController` for navigation
- ViewModels injected via `hiltViewModel()`
- Use `remember` for non-persisted UI state
- `LaunchedEffect` for side effects and lifecycle-aware operations
