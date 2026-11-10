# Hobby Mastery

<p align="center">
  <img src="hobby-mastery-icon.png" width="128" alt="Hobby Mastery app icon" />
</p>

<p align="center">
  <b>Turn daily practice into a 10,000-hour journey.</b><br />
  An offline-first Android app that helps you stay consistent with your hobbies.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white" alt="Platform: Android" />
  <img src="https://img.shields.io/badge/language-Java-orange?logo=openjdk&logoColor=white" alt="Language: Java" />
  <img src="https://img.shields.io/badge/minSdk-24-blue" alt="Minimum SDK: 24" />
  <img src="https://img.shields.io/badge/targetSdk-36-blue" alt="Target SDK: 36" />
  <img src="https://img.shields.io/badge/Gradle-9.3.1-02303A?logo=gradle" alt="Gradle 9.3.1" />
</p>

Hobby Mastery is a gamified hobby tracker built around the idea that mastery comes
from deliberate, consistent practice. Add the hobbies you care about, log the hours
you put in, and watch your streaks, levels, milestones, and badges grow over time —
all stored locally on your device.

> This repository is a fork of [tmjoris/hobbymastery](https://github.com/tmjoris/hobbymastery).

---

## Table of contents

- [Features](#features)
- [How tracking works](#how-tracking-works)
- [Rewards & badges](#rewards--badges)
- [Tech stack](#tech-stack)
- [Project structure](#project-structure)
- [Getting started](#getting-started)
- [Building from the command line](#building-from-the-command-line)
- [Permissions](#permissions)
- [Localization](#localization)
- [Data & privacy](#data--privacy)
- [Known limitations](#known-limitations)
- [Contributing](#contributing)
- [Credits](#credits)
- [License](#license)

## Features

### Track any hobby
- Add unlimited hobbies with a name and an automatically chosen emoji icon
  (music, coding, art, fitness, reading, cooking, chess, and more).
- Each hobby has its own progress bar toward **10,000 hours**.
- Swipe a card left or long-press it to delete the hobby and its progress.

### Log practice your way
- **Tap mode** — tap a hobby card to instantly log **+1 hour**.
- **Enter mode** — tap a card and type the exact number of hours (e.g. `1.5`).
- Every log gives haptic feedback, a short sound, and a confirmation toast.

### Levels
- Each hobby levels up every **10 logged hours** (`level = floor(hours / 10) + 1`).
- Reaching a new level triggers a full-screen celebration with confetti, a jumping
  mascot, and a fanfare.

### Streaks
- Log practice on consecutive days to build a daily streak.
- The streak resets automatically when a day is missed.
- Tap the streak card to see a 7-day history with per-day practice dots.
- Motivational messages change as the streak grows (1, 3, 7, and 30+ days).

### Weekly stats
- A Monday-to-Sunday bar chart of hours practiced this week.
- Today's progress against a configurable **daily goal** (1–5 hours/day).
- A **Path to Mastery** view per hobby with milestone markers at
  100, 500, 1,000, 5,000, and 10,000 hours.

### Rewards
- Experience points (`total hours × 10 XP`).
- Streak badges and per-hobby milestone badges that unlock as you progress.
- A "Mastered Hobbies" row on the profile showing the highest badge earned
  for each hobby.

### Daily reminders
- Optional daily notification at a time you choose (default **8:00 PM**).
- Handles Android 12+ exact-alarm permissions and falls back to an inexact
  alarm when exact scheduling isn't allowed.
- Reminder text is localized to the selected app language.

### Personalization
- **Dark mode** with a dedicated night theme.
- **Sound effects** toggle (practice, click, warning, and level-up sounds).
- **Profile photo** from the camera or gallery, plus an editable display name.
- **Multiple profiles** on one device — each user gets an isolated database and
  their own saved preferences.
- **Six languages** with instant switching.

## How tracking works

| Concept | Rule |
| --- | --- |
| Hours | Logged per hobby; supports decimals |
| Level | `floor(totalHours / 10) + 1` |
| XP | `totalHours × 10` |
| Streak | +1 for each consecutive day with at least one log; resets after a missed day |
| Weekly chart | Hours logged per day, Monday → Sunday |
| Daily goal | User-selected 1–5 hours, shown as `today / goal` |
| Mastery | 10,000 hours per hobby |

## Rewards & badges

**Streak badges**

| Badge | Requirement |
| --- | --- |
| 7-Day Streak | 7 days in a row |
| 30-Day Streak | 30 days in a row |
| 100-Day Streak | 100 days in a row |

**Hobby badges**

| Badge | Requirement |
| --- | --- |
| First Hour | 1 hour total |
| Dedicated | 10 hours total |
| Century | 100 hours total |
| Halfway | 5,000 hours total |
| Master | 10,000 hours total |

**Profile chips** — ⭐ 1h · 🔥 10h · 🥉 100h · 🥈 500h · 🥇 1,000h · 💎 5,000h · 👑 10,000h

## Tech stack

| Layer | Choice |
| --- | --- |
| Language | Java 11 |
| Platform | Android (min SDK 24, target/compile SDK 36) |
| UI | XML layouts, Material Components, RecyclerView, ViewPager-free single-activity navigation |
| Persistence | Room (SQLite) + SharedPreferences |
| Background | `AlarmManager` + `BroadcastReceiver` for reminders |
| Notifications | `NotificationCompat` with a dedicated channel |
| Media | `ToneGenerator` for sound effects, camera/gallery via Activity Result APIs |
| Build | Gradle 9.3.1 with Kotlin DSL and a version catalog (`gradle/libs.versions.toml`) |

## Project structure

```
hobbymastery/
├── app/
│   ├── build.gradle.kts                 # App module config (SDK levels, deps)
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/example/hobbymasterytracker/
│       │   │   ├── SplashActivity.java          # Animated launch screen
│       │   │   ├── WelcomeActivity.java         # Name entry / profile selection
│       │   │   ├── MainActivity.java            # Dashboard, stats, rewards, settings
│       │   │   ├── HobbyApp.java                # Applies dark mode at startup
│       │   │   ├── Hobby.java                   # Room entity
│       │   │   ├── HobbyDao.java                # Room DAO
│       │   │   ├── AppDatabase.java             # Per-user Room database
│       │   │   ├── HobbyAdapter.java            # RecyclerView adapter + emoji picker
│       │   │   ├── StreakManager.java           # Daily streak logic
│       │   │   ├── WeeklyTracker.java           # Mon–Sun hour tracking
│       │   │   ├── ReminderScheduler.java       # Exact/inexact daily alarms
│       │   │   ├── ReminderReceiver.java        # Shows and reschedules reminder
│       │   │   ├── NotificationHelper.java      # Channel + notification builder
│       │   │   ├── SoundManager.java            # Tone-based sound effects
│       │   │   └── LocaleHelper.java            # Language switching
│       │   └── res/
│       │       ├── layout/                      # Activity, fragment, dialog layouts
│       │       ├── drawable/                    # Backgrounds, icons, mascot
│       │       ├── anim/                        # Confetti, mascot, level-up animations
│       │       ├── menu/                        # Bottom navigation menu
│       │       ├── values/                      # Colors, strings, themes
│       │       ├── values-night/                # Dark theme overrides
│       │       └── values-{am,es,fr,sw,zh}/     # Translations
│       ├── test/                                # JVM unit tests
│       └── androidTest/                         # Instrumented tests
├── gradle/
│   ├── libs.versions.toml                       # Dependency version catalog
│   └── wrapper/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew / gradlew.bat
└── hobby-mastery-icon.png
```

### Architecture notes

- **Single activity, four pages.** `MainActivity` hosts the Home, Stats, Rewards,
  and Settings pages and toggles their visibility from the bottom navigation bar.
  The active page is preserved across configuration changes (e.g. dark mode toggles).
- **Reactive data.** `HobbyDao.getAll()` returns `LiveData`, so the dashboard,
  stats, and rewards pages all refresh automatically when hobbies change.
- **Background writes.** Database inserts, updates, and deletes run on a
  single-thread executor to keep the UI thread free.
- **Per-user isolation.** `AppDatabase` builds a separate database per profile
  (`hobby_db_<sanitized_name>`), and preferences are namespaced with the user name.
- **Helper managers.** Streaks, weekly tracking, reminders, sound, and locale
  handling are split into small focused classes rather than living in the activity.

## Getting started

### Prerequisites

- [Android Studio](https://developer.android.com/studio) (latest stable)
- JDK 17 or newer (the Android Studio bundled JDK works)
- Android SDK Platform **36** installed
- An emulator or a physical device running Android 7.0 (API 24) or newer

### Run it

1. Clone the repository:
   ```bash
   git clone https://github.com/MelatDB/hobbymastery.git
   cd hobbymastery
   ```
2. Open the project in Android Studio.
3. Let Gradle sync and download dependencies.
4. Select an emulator or connected device and press **Run ▶**.

On first launch, enter a name on the welcome screen and tap **Get Started**. The
app then opens on the dashboard, where you can add your first hobby with the
**+** button.

## Building from the command line

```bash
# Debug APK
./gradlew assembleDebug

# Windows
gradlew.bat assembleDebug

# Unit tests
./gradlew test

# Instrumented tests (requires a connected device/emulator)
./gradlew connectedAndroidTest

# Release build (minification is disabled by default)
./gradlew assembleRelease
```

The debug APK is written to:

```
app/build/outputs/apk/debug/app-debug.apk
```

Install it on a connected device with:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Android Studio creates `local.properties` with your SDK path automatically. If you
build from the CLI without opening the project first, create it manually:

```properties
sdk.dir=C\:\\Users\\<you>\\AppData\\Local\\Android\\Sdk
```

## Permissions

| Permission | Why it's used |
| --- | --- |
| `VIBRATE` | Haptic feedback when logging practice |
| `POST_NOTIFICATIONS` | Daily practice reminders (Android 13+) |
| `SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM` | On-time reminders (Android 12+) |
| `RECEIVE_BOOT_COMPLETED` | Declared for reminder persistence |
| `READ_MEDIA_IMAGES` | Choosing a profile photo from the gallery (Android 13+) |
| `READ_EXTERNAL_STORAGE` | Gallery access on older Android versions (max SDK 32) |
| `CAMERA` | Taking a profile photo |
| `WRITE_EXTERNAL_STORAGE` | Legacy storage support (max SDK 28) |

Notifications and exact alarms are optional — the app works fully without them.
Exact-alarm permission is requested only when reminders are enabled, with a
fallback to inexact alarms if the user declines.

## Localization

The UI ships in six languages:

| Language | Folder |
| --- | --- |
| English | `values/strings.xml` |
| Amharic | `values-am/strings.xml` |
| Spanish | `values-es/strings.xml` |
| French | `values-fr/strings.xml` |
| Swahili | `values-sw/strings.xml` |
| Chinese | `values-zh/strings.xml` |

Language can be changed at runtime from **Settings → Language**. To add a language,
create a new `values-<code>/strings.xml` with the same keys as the default file and
add its display name and code to the language picker in `MainActivity`.

## Data & privacy

- All hobby data, streaks, and settings are stored **locally** on the device.
- There is no account system, no network calls, and no analytics.
- Each profile gets its own Room database and preferences, so multiple people can
  share a device without mixing their progress.
- Logging out keeps the data on the device so the same profile can be reopened.
- Clearing app data or uninstalling the app removes everything permanently.
- Android's built-in backup is enabled (`allowBackup="true"`), so the system may
  include app data in device backups.

## Known limitations

- Reminders are scheduled when the app is running; because no boot receiver is
  registered yet, a reboot may require reopening the app to reschedule them.
- All data is device-local — there is no cloud sync or export.
- Release builds are not minified by default and no signing config is included.

## Contributing

Contributions are welcome:

1. Fork the repository.
2. Create a feature branch: `git checkout -b feature/my-change`.
3. Make your changes and keep the existing Java/XML style.
4. Run `./gradlew test` and verify the app on an emulator.
5. Open a pull request describing what changed and why.

For bug reports, please include the Android version, device/emulator model, and
steps to reproduce.

## Credits

- Forked from [tmjoris/hobbymastery](https://github.com/tmjoris/hobbymastery).
- Built with [AndroidX](https://developer.android.com/jetpack/androidx),
  [Material Components](https://github.com/material-components/material-components-android),
  and [Room](https://developer.android.com/training/data-storage/room).


