# GharHisab

GharHisab is an Android household finance manager for individuals and families in Pakistan, built by Sindan Technologies.

Version 3.0.0 includes income/expense tracking, budgets, recurring planned expenses and reminders, optional PIN/device lock, encrypted local and Google Drive backups, and spending forecasts. Financial records stay available offline. Drive uses explicit backup and restore; it does not silently merge or overwrite records across devices.

## Build

Use JDK 17 and Android SDK 34/build-tools 34.0.0. Minimum Android version: 10 (API 29); compile/target SDK: 34.

```bash
bash gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest
```

APK: `app/build/outputs/apk/debug/app-debug.apk`. Open the project in Android Studio for device development. The stack is Kotlin 2.0, Compose Material 3, MVVM, Navigation Compose, Room and Preferences DataStore, built with Gradle 8.7/AGP 8.4.

## Test APK

**[Download the latest APK (v3.0.0-rc.3)](https://github.com/SAAD-2113/GHAR-HISAB-APP/raw/refs/heads/main/releases/v3.0.0-rc.3/GharHisab-v3.0.0-rc.3-debug.apk)**. Only the latest APK is kept in [releases/](releases/) on `main`; [GitHub Releases](https://github.com/SAAD-2113/GHAR-HISAB-APP/releases) links to the same file. The automatically generated source archives are not APKs. This is a debug-signed prerelease; real-device acceptance is still pending.

## Configuration and verification

- [Version 3 implementation, formulas, migrations, tests and limitations](docs/VERSION_3_PROGRESS.md)
- [Google Cloud / Drive setup and device migration](docs/GOOGLE_DRIVE_SETUP.md)
- [Current validation coverage and physical-device checklist](docs/APP_VALIDATION.md)
- [Voice entry behavior and supported parsing](docs/VOICE_ENTRY.md)

Google Cloud configuration and real-device acceptance are still required before production release. Offline builds need no OAuth credentials. Never commit signing keys, OAuth tokens or backup recovery credentials.

Classic red/gold, Midnight and system themes are supported. English, Urdu and Punjabi resource bundles are included, with Urdu RTL; some older screens/category names still contain English copy.

## License

Proprietary — Sindan Technologies (2026). All rights reserved.
