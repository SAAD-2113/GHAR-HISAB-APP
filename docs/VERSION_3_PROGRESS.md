# Version 3 implementation and verification

Version **3.0.0**, version code **4**. Application ID: `com.sindantechnologies.gharhisab`. The Version 3 implementation is present; the owner has configured Google Cloud, but the reported APK/client signing-certificate mismatch still needs correction; production acceptance requires physical-device validation. See [Google Drive setup](GOOGLE_DRIVE_SETUP.md).

## Implemented behavior

- **Transaction forms:** scrolling content and a flexible bottom Save action share one IME inset owner, consume Scaffold padding and respect system navigation. Category grids adapt to width/font scale. Save failures retain inputs; repeated in-flight actions are guarded. Red/gold Classic and Midnight themes remain available.
- **Planned expenses:** create/edit/postpone/cancel/delete, due/overdue summaries, notes/categories, actual payment amount/method and per-plan reminders. Weekly/monthly/yearly intervals retain the original monthly day across short months. Paying a plan creates its actual transaction and next occurrence in one Room transaction; repeated/concurrent payment requests return the existing transaction. Due dates never automatically create payments.
- **Reminders:** optional due-day/1/3/7-day inexact AlarmManager alarms, notification permission explanation, private generic notifications, persistent delivery deduplication, stale-alarm cancellation and reconciliation after edits/deletion/reboot/app updates/time changes. Android can delay delivery. Scheduling failures are separate from successful plan writes, avoiding duplicate plans on retry; the planned-expense screen reports them. Returning to the app retries reconciliation.
- **App lock:** default OFF; optional 4-digit app PIN or Android biometric/device credential; authenticated lock-setting changes; configured background timeouts; cold-start loading gate and secure recent-task previews. Financial navigation/dialogs are removed while locked. New PINs use salted, device-bound Android Keystore HMAC verifiers. Legacy PBKDF2 PIN hashes remain verifiable and upgrade after successful authentication; portable-backup PBKDF2 is unchanged. Failed attempts persist and incur increasing delays after five failures. A randomly generated recovery key can disable a forgotten app PIN without deleting records. Record the displayed key outside the device. App lock does **not** encrypt Room.
- **Backup:** real Google `AuthorizationClient`, minimal Drive `appDataFolder` scope, authenticated HTTPS API upload/list/download, explicit snapshot selection, no-backup/error/cancellation states and revoke-access action. Manual immutable encrypted snapshots are the synchronization model. Sync Now refreshes the list, not a two-way merge. Local encrypted file export/import remains supported.
- **Restore safeguards:** password-derived AES-256-GCM portable files, version/schema/reference/size validation, preview and explicit replacement, stale-preview detection, atomic Room replacement, device-encrypted pre-restore safety archive and rollback. A pending restore journal is recovered before financial UI appears. Storage-finalization failures restore the previous data; unresolved rollback failures block financial UI until startup recovery. PINs, recovery hashes and OAuth tokens are excluded from portable snapshots and Android system backups.
- **Insights:** actual balance and spending history, rolling weighted spending rate, configurable reserve, unpaid/overdue recurring commitments, budget-capped daily allowance, shortfall/trend warnings and an estimated depletion date. Preliminary results are labelled and assumptions explained. Risk notifications are opt-in, private and deduplicated; checks run while the dashboard is active, not through a background forecasting worker.
- **Language/accessibility:** new feature resources in English, Urdu and Punjabi; saved language applied across the Compose resource context; Urdu RTL; scrollable editors/dialogs and wrapping controls. Existing hardcoded English copy/category names elsewhere remain a localization limitation. This is not a claim of complete app-wide translation or physical-device accessibility certification.

## Financial model and migration

All stored money remains integer **paisa**. No rupee/paisa conversion is performed during migration. Intermediate forecast arithmetic uses checked Long operations or BigInteger; presentation may use floating point only for chart/progress ratios.

`discretionary = actual available balance − unpaid commitments through month end − reserve`

`daily limit = max(0, min(discretionary, remaining budget if configured)) / remaining calendar days including today`

Reserve defaults to zero and is editable. Overdue unpaid occurrences are included. Paid planned transactions are excluded from the ordinary daily-spending rate to avoid counting a recurring bill in both that rate and the future commitment schedule. The last seven observed days receive twice the weight of the preceding seven. Estimates are preliminary below ten observed days; depletion dates require a known balance and at least three days. The forecast simulates dated commitments and that spending rate for at most one year, assuming no unreceived future income. It is an estimate, not a guarantee.

Room schema **2 → 3** adds reminder/recurrence preferences and transaction-to-plan provenance. Existing paid-plan references populate the transaction link. Budget rows are copied while normalizing a default that differed between fresh-v2 and v1-migrated databases. The original **1 → 2** migration remains registered. No destructive fallback or clearing of user data is added. Backup payload version 2 accepts v1 backups with defaults for new fields.

## Main files

| Area | Paths under `app/src/main/java/com/sindantechnologies/gharhisab/` |
| --- | --- |
| Responsive forms | `ui/screens/transaction/{AddTransactionScreen,EditTransactionScreen,TransactionCategories}.kt` and ViewModels |
| Planned expenses | `data/repository/PlannedExpenseRepository.kt`, `domain/model/Recurrence.kt`, `ui/screens/planned/`, `util/reminder/` |
| Lock lifecycle | `MainActivity.kt`, `ExternalActions.kt`, `ui/screens/security/LockScreen.kt`, `util/security/`, `data/local/datastore/UserPreferencesRepository.kt` |
| Backup/Drive | `data/backup/{BackupManager,BackupPayload,SafetyVault,DriveBackupClient}.kt`, `ui/screens/backup/DriveBackupPanel.kt`, Settings |
| Forecasts | `domain/model/SpendingForecast.kt`, `ui/screens/home/{HomeViewModel,FinancialInsightsCard}.kt` |
| Migration | `data/local/room/GharHisabDatabase.kt`, entities and DAOs |

Also changed: manifest/Android backup exclusions, localized strings, app build/version configuration and regression tests under `app/src/test`.

## Build and tests

Requirements: JDK 17, Android SDK API 34/build-tools 34.0.0. Gradle wrapper 8.7, AGP 8.4.0, Kotlin/Compose compiler 2.0.0; minimum Android API 29. No credentials are needed for an offline build.

```bash
# Managed cloud environment only:
source /workspace/.gharhisab-tools/activate.sh
cd /workspace/GHAR-HISAB-APP

# Cloud or locally configured Android development machine:
bash gradlew --no-daemon --max-workers=3 :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest
```

Verified on 11 November 2026:

- `:app:assembleDebug`: passed; debug APK generated.
- `:app:testDebugUnitTest`: **55 tests passed, zero failures/errors/skips**, including host Android UI tests.
- `:app:lintDebug`: passed with **zero errors, 85 warnings and 2 informational findings**. Remaining warnings include pre-existing dependency-update suggestions, icon placement, date formatting and a database-context reference, plus new direct-dependency/catalog-style suggestions, plural-resource candidates and Compose state boxing suggestions. No lint baseline/check disabling was introduced.
- `:app:assembleDebugAndroidTest`: passed; instrumentation APK compiled, **not executed on a device**.
- `git diff --check`: passed.

Intermediate failures included an API-31-only BigInteger call, a test setup returning a non-Unit value, asynchronous Room test cleanup and an incorrect Kotlin DSL language-split property name. These were corrected; the final build/check command passed. Actual device and Google authorization results are not implied by these host checks.

Coverage includes exact transaction persistence; income/expense/edit/delete and failure retry; small/landscape/large-font/RTL Compose forms with synthetic keyboard/navigation insets; schema-2 migration; 12 concurrent mark-paid requests; recurrence boundaries; negative/unknown balances, weighted spending, future income, shortfalls and planned-payment double counting; encrypted preview/restore/rollback; tampered/wrong-password/unknown-schema rejection; stale previews; interrupted and failed restore recovery; persistent PIN throttling/recovery and lock timing; reminder rescheduling/cancellation/deduplication; actual fresh-install MainActivity startup.

APK: `app/build/outputs/apk/debug/app-debug.apk`. On a connected test device:

```bash
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.sindantechnologies.gharhisab/.MainActivity
bash gradlew :app:connectedDebugAndroidTest
```

Use the original signing key or a separate test device if the installed app has a different signer. Do not uninstall an existing financial database to bypass a signature mismatch.

## Remaining configuration and acceptance limits

- Correct the Android OAuth signing-certificate mismatch described in the [setup guide](GOOGLE_DRIVE_SETUP.md), then exercise real authorization, upload/download and second-device restore. No Google passwords/tokens/keys belong in the repository.
- There is no attached Android device/emulator and `/dev/kvm` is unavailable. Instrumentation tests compile but have not run. Host Robolectric tests do not verify OEM keyboard/navigation, device credentials, Android Keystore hardware, real notification delivery or Android process termination at every restore boundary.
- Test API 29 and current Android releases, both themes, actual IME/nav modes, large fonts, lock/recovery/rotation/backgrounding, denied notifications, reboot and interrupted restores before publishing a production release. The host migration test covers a populated v2 database; a physical v1 upgrade remains part of acceptance.
- Cloud list is limited to the latest 20 snapshots. No background uploads, cloud retention UI or automatic record merge is implemented. Core financial work remains offline.
- PIN-based application access control is not database encryption and cannot protect against an attacker controlling the OS. Four-digit PINs depend on application rate limiting; use the device's secure screen lock as well. Portable backup passwords must be stored safely by the user.

The cloud setup/startup draft has been updated for reuse; publishing that draft and testing a new environment snapshot are separate platform actions.


## Drive reliability follow-up

The read-only audit identified reliability gaps. The owner subsequently authorized local fixes, tests and a test APK, without committing or publishing changes.

- Drive transfers, authorization resolutions, restore previews and completion/error state now belong to an Activity-scoped ViewModel. Rotation and temporary UI removal for app locking do not cancel its jobs. Credentials remain memory-only; no tokens, passwords or plaintext are written to saved-state bundles. This is not a persistent background worker and does not promise transfer completion after process death.
- An acknowledged upload remains successful even if its subsequent list refresh fails. A lost upload response is explicitly unconfirmed; refresh before retrying to avoid duplicate snapshots.
- Errors distinguish authorization/status codes, HTTP responses, connectivity, decryption, invalid payloads, stale previews, local backup creation and restoration. Raw exception/server bodies, tokens and financial data are not included in Drive error messages.
- Successful startup recovery clears the blocking flag on the same BackupManager instance. A failed recovery leaves it blocked.
- Both supported backup formats must contain their originally required integer balance/reserve fields. Missing/null/fractional/string monetary values and overflowing starting-balance/income arithmetic are rejected before mutation. Existing optional v1 defaults remain supported. No signing configuration, Room schema or migration was changed.
- The list still intentionally shows at most 20 snapshots. Pagination, automatic cloud uploads and two-way synchronization are outside this focused patch.

New checks exercise the real HTTP adapter with controlled in-memory connections, retained controller state, a Robolectric MainActivity recreation/background cycle, recovery retry and malformed backup rejection. These are automated host checks, not live Google or physical-device results. Verification on 11 November 2026: the complete `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest` command passed. **71 tests passed, zero failures/errors/skips**. Lint reported **zero errors, 86 warnings and 1 informational findings**. The debug APK and instrumentation APK were generated; instrumentation tests were not run on a phone. One new test fixture initially omitted required constructor arguments; that compile failure was corrected before the successful run. Source whitespace checks passed.

After the successful local checks, the owner authorized pushing the reliability fixes and publishing the tested debug APK as a GitHub prerelease. Live Google Drive and physical-device acceptance remain pending.


## November 13 usability and device-report fixes

- Financial insights now emphasize the daily allowance, show compact supporting amounts and one status message, and expand calculation details on demand. Spending-risk notifications use a full-width accessible switch with explicit Android notification-permission guidance. Home summaries adapt to larger text. Dark-mode action text now uses a lighter red accent (8.71:1 against the background instead of 2.85:1); filled action text has 7.96:1 contrast. The add action occupies a fixed bottom bar after a small-screen navigation test reproduced the old floating button covering the history link.
- Voice entry merges successive utterances without clearing existing fields, removes monetary phrases from descriptions, preserves manual edits and exact paisa, and supports common English/Urdu/Punjabi number forms and digit scripts. Unsupported language models and calendar dates get actionable guidance. Android 11+ recognition-service visibility is declared. See [voice behavior and limits](VOICE_ENTRY.md).
- New app PINs use a device-bound, non-exportable Android Keystore HMAC key with persistent rate limiting. Legacy PINs verify using their old algorithm and upgrade after a successful check; old recovery keys remain usable. Strong portable-backup password derivation is unchanged. Actual unlock timing must be measured on a phone.
- Device authentication permits enrolled biometrics and Android screen-lock fallback. Pending prompts, errors, PIN creation/recovery display, and local rollback jobs survive Activity recreation/backgrounding. A second rollback cannot start while the retained first operation runs.
- Google authorization now inspects result payloads even when the Activity reports cancellation. SDK configuration errors retain their status; unknown interruptions are not presented as proof the user canceled. The app displays its public package/certificate identity when authorization fails. The supplied client fingerprint does not match the cloud APK: follow [the exact OAuth correction](GOOGLE_DRIVE_SETUP.md).
- Budget, starting-balance and reserve editors preserve fractional amounts. Budget warning percentages are restricted to 1–100. History filters have upper bounds, and Drive-to-local onboarding waits for preference persistence before navigating.

No signing configuration, Room schema, existing user records or portable-backup encryption parameters were changed. New tests use synthetic data. The current verification record and remaining physical-phone checks are in [APP_VALIDATION.md](APP_VALIDATION.md). The owner approved committing these fixes, pushing them to GitHub and publishing the tested APK as prerelease `v3.0.0-rc.2`. Physical-device acceptance remains pending.


## Dark-theme and splash branding follow-up — 14 November 2026

- Midnight filled buttons and selected controls use the same deep red (`#B71C1C`) as Classic, with white content (6.57:1 contrast). Text actions and focused labels on dark surfaces use a light neutral foreground, keeping them legible independently of button fills. Classic control styling is preserved.
- Sindan Technologies lettering now has opaque white interiors and stronger dark outlines. High-quality image filtering is used at the original splash logo size: the existing 40 dp normal / 32 dp compact height limits, padding and placement are unchanged.
- Host-rendered splash previews cover Classic and Midnight, including compact layouts. These are automated previews, not physical-phone screenshots. See [the current validation report](APP_VALIDATION.md) for the final build results and device-testing limits.
