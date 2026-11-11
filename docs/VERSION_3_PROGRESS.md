# Version 3 implementation record

## Inspection and implementation checklist

This increment addresses Stage 1 (transaction form stabilization). Stages 2–7 remain open; existing implementations below are not evidence of meeting their acceptance criteria.

- Application ID/package: `com.sindantechnologies.gharhisab`; version code 3, version name 1.2.0.
- Gradle 8.7, AGP 8.4.0, Kotlin/Compose compiler 2.0.0, KSP 2.0.0-1.0.22; Java bytecode target 17; min SDK 29, compile/target SDK 34. The README's API 35 statement is stale.
- Single FragmentActivity, edge-to-edge Compose Material 3, Navigation Compose, manually injected repositories and ViewModels; red/gold Classic and Midnight themes.
- Room 2.6.1 database version 2, with explicit migration 1→2. Transactions, categories, budgets and planned expenses already exist. Monetary `Long` values are **paisa**, not whole rupees. Preserve this unit and existing data.
- Add/edit forms already have sticky bottom actions and scrolling. Both independently apply IME padding to the form and action, without consuming Scaffold padding. Fixed-height actions and four-column, single-line category labels also limit accessibility. The screenshot appears to show an older layout; it cannot establish that the current checkout has precisely the same defect.
- Reminders already use AlarmManager with reboot/update receivers, but individual preferences, genuine recurrence, safe stale-alarm reconciliation, and robust permission/error UX remain incomplete. Mark-paid is not currently one atomic database transaction, so concurrent calls can duplicate payments.
- PIN and biometric/device authentication exist, but startup loads OFF before saved lock settings arrive; selected timeouts are ignored; rate limiting is only in composition memory; recovery and authenticated settings changes need design and tests. Do not describe this implementation as hardened security.
- Local password-encrypted AES-GCM file export/import exists. Drive is an informational dialog, not OAuth or an API integration. Backup schema validation, consistent snapshots, restore preview, rollback, and recovery compatibility need additional work.
- Daily recommendation exists: max(0, balance − unpaid upcoming commitments − reserve) / remaining days including today. No rolling spending analysis or depletion-date model exists. Overdue commitment handling needs correction.
- English/Urdu/Punjabi language selection exists, but translation coverage and application-wide locale switching are incomplete. Transaction-form resources added here do not constitute complete localization.

## Execution checklist

- [x] Inspect existing architecture, schema, units, navigation, themes, reminders, security, backup and calculations.
- [x] Consolidate IME handling at the transaction Scaffold; consume its content padding; respect safe system insets on the action area.
- [x] Allow action height to grow and categories to wrap/adapt to font scale and width.
- [x] Preserve forms on write failures; prevent repeated in-flight save requests; keep one category collector when toggling transaction type.
- [x] Add transaction-form string resources in English, Urdu and Punjabi; fill all existing Urdu/Punjabi resource gaps without suppressing lint.
- [x] Explicitly use Fragment 1.6.2 to fix the pre-existing Activity Result incompatibility introduced by Biometric 1.1’s transitive Fragment 1.2 dependency.
- [x] Build debug APK and instrumentation APK; run lint and unit/host Android UI tests (results below).
- [ ] Validate OEM keyboards, gesture/three-button navigation and physical-device layouts.
- [ ] Stage 2: atomic planned-payment conversion, recurrence, preferences, reconciliation and dashboard.
- [ ] Stage 3: secure lock lifecycle, persistent throttling, authenticated changes and recovery.
- [ ] Stage 4: Google authorization, encrypted Drive backup/recovery, validated restore and rollback.
- [ ] Stage 5: transparent forecasts with calendar, history, shortfall and double-counting tests.
- [ ] Stage 6: application-wide localization, accessibility, theme and visual refinements.
- [ ] Stage 7: device and migration acceptance tests across the complete feature set.

No database schema, migration, monetary units or application version are changed in this increment. Existing financial records remain compatible.

## Verified results (25 October 2026)

- `:app:assembleDebug`: passed; debug APK generated.
- `:app:testDebugUnitTest`: **29 tests passed, zero failures/errors/skips**. This includes 9 Robolectric Compose form tests, 1 file-backed Room persistence test and 19 existing calculation, parser, serialization and cryptography tests.
- Form coverage: expense and income saves with exact paisa values, edit, confirmed delete, repeated save requests, a real SQLite foreign-key failure followed by retry, 320×480 layout, 640×360 landscape, large text, synthetic navigation/IME insets, Midnight theme, RTL, and actual Urdu resource rendering. Synthetic insets do not exercise a physical keyboard or OEM navigation implementation.
- Persistence coverage: insert/edit records, close/reopen the database file, verify the record and income/expense totals, then delete. This is not a complete process-death or migration test.
- `:app:lintDebug`: passed, **0 errors, 40 warnings and 1 informational finding**. Earlier errors were the incompatible Fragment dependency and missing translations; both were resolved. Remaining warnings include pre-existing unused resources, icon placement, SDK checks, date formatting and a database singleton context reference. Added direct test/Fragment dependency declarations also produce version-catalog style warnings. No lint checks were disabled and no baseline was introduced.
- `:app:assembleDebugAndroidTest`: passed. Instrumentation tests were **compiled, not run on a device**.
- No emulator/device is attached; hardware acceleration (`/dev/kvm`) is unavailable. Device authentication, alarms, cross-device restoration and OEM UI acceptance have not been verified.
- Passing the existing cryptographic helper tests does not establish that the complete app-lock or backup lifecycle is secure.

## Important changed files

- `app/src/main/java/com/sindantechnologies/gharhisab/ui/screens/transaction/AddTransactionScreen.kt` and `EditTransactionScreen.kt`: one IME inset owner, consumed Scaffold padding, safe bottom action insets, flexible action height, translated labels and visible save errors.
- `TransactionCategories.kt` in the same directory: shared adaptive category grid, wrapping labels, radio-button selection semantics and localized payment labels.
- `AddTransactionViewModel.kt` and `EditTransactionViewModel.kt`: guard repeated in-flight saves, catch database write failures without reporting success, retain form values for retry. Add mode now maintains a single category subscription.
- `app/src/main/res/values*/strings.xml`: transaction labels and complete resource-key coverage for existing Urdu/Punjabi bundles. Corrected the inaccurate onboarding claim that private local storage encrypts the database. Many hardcoded strings elsewhere and app-wide locale activation remain outside this increment.
- `app/build.gradle.kts`: compatible Fragment dependency; real host JSON implementation for the existing JSON test; Robolectric/Compose test dependencies and Android instrumentation dependencies.
- `TransactionFormTest.kt` and `TransactionPersistenceTest.kt` under `app/src/test`: behavior and persistence regression coverage.

## Build and install

Cloud environment:

```bash
source /workspace/.gharhisab-tools/activate.sh
cd /workspace/GHAR-HISAB-APP
bash gradlew --no-daemon --max-workers=3 :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest
```

The environment provides checksum-verified Temurin JDK 17.0.21+9, Android command-line tools 19.0, API 34, build-tools 34.0.0 and Gradle 8.7. The activation script uses the platform proxy and system CA trust store, without disabling TLS verification. Setup files and dependency caches live outside the checkout in `/workspace/.gharhisab-tools`. The environment draft contains the installation script and startup instructions; publication and restoration in a new task have not been tested.

APK: `app/build/outputs/apk/debug/app-debug.apk`.

On a connected test device with USB debugging enabled:

```bash
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.sindantechnologies.gharhisab/.MainActivity
bash gradlew :app:connectedDebugAndroidTest
```

This APK uses the cloud environment’s debug signing key. An existing installation signed with another key cannot be updated with it. Preserve financial data; use the original signing key or a separate test device instead of uninstalling the existing app to bypass a signature mismatch.

## Remaining Version 3 work and configuration

This is an incremental Stage 1 delivery, **not the completed Version 3 release**. Existing reminder/security/backup/insight code was inspected, not reimplemented or certified in this increment.

- **Reminders:** implement per-plan timing/enabled preferences, real recurrence, atomic/idempotent payment conversion, edit/postpone reconciliation and permission explanations. Current alarms attempt exact delivery with fallback, and automatically use due-day/one-day/three-day timings. Android may delay delivery; permissions alone do not prove scheduling works.
- **App lock:** fix cold-start loading, enforce selected background timeout, persist throttling, protect recent-task previews, authenticate lock changes and define a recovery strategy. Default OFF is preserved. Current PIN hashing and device authentication need lifecycle/device testing; app lock does not encrypt Room.
- **Drive:** there is no working account connection, Drive upload/download or synchronization. The Settings dialog currently describes intended behavior and must be replaced by a truthful operational UI. Implement a Google Cloud project with Drive API enabled, OAuth consent/test-user configuration and an Android OAuth client for the application ID and signing-certificate SHA-1. Client configuration is not an embedded client secret; do not put tokens or recovery credentials in the repository.
- **Encrypted restoration:** existing local files use password-derived AES-GCM. A password permits migration in principle, but version validation, bounded input, strong KDF evolution, consistent DB snapshots, restore preview, safety backup/rollback and an actually tested second-device workflow remain required. Losing the recovery password can make backups unrecoverable. Do not silently overwrite newer local records.
- **Forecasts:** current daily limit is `max(0, available balance − upcoming commitments − protected reserve) / remaining days including today`, using integer paisa. The reserve defaults to zero and is editable. No depletion date, rolling-history rate, confidence model or opt-out risk notifications are implemented. Correct overdue commitments and prevent double-counting paid plans before extending the model.
- **Data preservation:** no migration or monetary-unit conversion was introduced. Keep the existing version-2 schema and 1→2 migration intact for this increment; future schema changes require explicit migration tests with existing data.
- **Device acceptance:** verify actual small/large screens, large fonts, Urdu layouts, keyboard open/closed, gesture/three-button navigation, app restarts and both themes. Then test notification delivery, lock lifecycle and cross-device restore as their stages are implemented.
