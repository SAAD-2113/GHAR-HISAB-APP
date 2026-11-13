# App validation — November 2026 fixes

This document separates automated checks from testing that needs an Android phone. Automated tests run with synthetic records in test databases; they do not exercise the owner's Google account or existing financial records.

## Execution record

**Verified on 13 November 2026:** the complete command below passed on the final source revision. **115 tests passed; zero failures, errors or skips.** Android lint completed with **0 errors, 92 warnings and 1 informational finding**. App and instrumentation APKs built successfully. This is automated host verification, not physical-device acceptance.

| Check | Current result |
| --- | --- |
| JVM/unit and Robolectric/Compose suite (`:app:testDebugUnitTest`) | 115 passed; no failures/errors/skips |
| Android lint (`:app:lintDebug`) | 0 errors; 92 warnings; 1 informational finding |
| APK builds (`:app:assembleDebug`, `:app:assembleDebugAndroidTest`) | Both passed |
| Instrumented database tests (`:app:connectedDebugAndroidTest`) | Not run on a connected Android device |
| Real Google authorization/upload/list/restore | Requires signed APK and actual Google account on an Android phone |
| Hardware biometric/device credential and Android Keystore performance | Requires Android phone testing |

The normal automated command is:

```sh
bash gradlew --no-daemon --max-workers=3 :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest
```

Test XML and HTML reports are under `app/build/test-results/testDebugUnitTest/` and `app/build/reports/tests/testDebugUnitTest/`. Lint reports are under `app/build/reports/`. Passing host tests must not be described as successful live Google Drive or biometric testing.

## Automated coverage and limits

The named test classes passed in the final host run. They identify covered scenarios, not physical-device approval. Lint warnings concern dependency updates, unused resources, icons and other maintenance items; they are retained in the report rather than hidden.

| App area | Automated cases | Limits / further testing |
| --- | --- | --- |
| Startup and onboarding | `Version3StartupTest` opens fresh onboarding. `AppNavigationFlowTest` chooses theme, follows the Drive explanation to local continuation, verifies persisted onboarding/storage preferences, and recreates the real `MainActivity`. | Fresh install and upgrade on physical phones; process death, OEM restore and very slow storage need additional testing. Activity recreation is not process death. |
| Main navigation | `AppNavigationFlowTest` traverses Home → Add Transaction → Home → History → Edit → History → Home, plus Budgets, Planned expenses and Settings, using real navigation and repositories. It checks the fixed bottom add action does not overlap the history link. | Back gestures, system bars, external activity return and varied screen sizes/OEMs still need device checks. This is a smoke flow, not every possible navigation sequence. |
| Transaction forms | `TransactionFormTest` covers income/expense saves, exact fractional amounts, editing, delete confirmation, repeated save requests, failure/retry, small screens, landscape, large fonts, RTL and synthetic keyboard/navigation insets. `TransactionPersistenceTest` reopens a real Room database and checks records/totals. | Synthetic insets do not prove compatibility with every Android keyboard. Actual speech input and accessibility services need device testing. |
| History and filtering | `TransactionsFilteringTest` checks both boundaries of This Month, excludes adjacent months, excludes future records from Last 30 Days, preserves future records in All Time, and combines date/type/search filters with correct totals. Navigation smoke opens a saved record through History. | Manual scrolling, large histories, keyboards and localized presentation need device checks. |
| Budgets | `BudgetFlowTest` edits the warning percentage without losing paisa, creates category budgets, checks monthly/category totals exclude other categories/income/previous-month entries, deletes only budgets, rejects invalid warning percentages, and operates the editor at 1.4× font size on a small screen. | Visual review across phones/themes/languages and sustained use across a month boundary remain manual checks. |
| Planned expenses | `PlannedExpenseFlowTest` creates a recurring plan at 1.3× text, verifies a plan does not count as an actual expense, records a chosen actual amount/payment method, cancels/deletes unpaid plans, checks deletion confirmation, and preserves an expense when deleting its paid plan. `Version3DatabaseTest` checks concurrent payment idempotency and next recurrence creation. | Date-picker interaction, notification permission prompts, actual delivery and OEM background restrictions need device checks. |
| Reminders | `Version3ReminderTest` checks rescheduling/cancellation for changed, disabled or deleted plans and prevents paid/already-delivered plans notifying again. Recurrence calendar arithmetic is covered by `Version3FinanceTest`. | Real notification sound/display, reboot delivery, battery saving and delayed alarms require phone testing. |
| Financial insights and spending alerts | `DailySpendingCalculatorTest` and `Version3FinanceTest` cover reserve/planned commitments, negative funds, future income, month lengths, history confidence and recurrence. `FinancialInsightsUiTest` covers accessible alert toggling, expandable calculation details, permission action and small/large-text/RTL/dark rendering. | Robolectric layout/semantics checks do not establish real notification delivery or user acceptance of the visual design. Review actual rendered screens and TalkBack. |
| Voice transaction parsing | `VoiceTransactionParserTest` and `VoiceEntryRegressionTest` cover incremental description/amount entry, combined phrases, preserving manual edits, exact decimals, selected-language number forms, Unicode scripts/offsets and ambiguous input. | These tests supply recognized text. Microphone permission, speech recognizer availability, recognition quality, accents, offline support and each engine/language must be tested on a phone. They cannot guarantee recognition of every spoken phrase. |
| Preferences and theme | `AppNavigationFlowTest` changes and verifies theme and fractional starting balance, reopens the balance editor without truncation and recreates the Activity. Backup tests cover restored supported preferences. | All language/theme combinations, settings dialogs, accessibility and system locale/theme changes are not exhaustively tested. |
| App PIN and recovery | `SecurityUtilsTest` and `Version3SecurityTest` check verifier behavior, wrong attempts/throttling, legacy PIN compatibility/upgrades, Unicode digits, recovery separation, key-loss failure behavior and preservation of financial preferences. Lock-session logic checks cold/background timeouts. `LockScreenUiTest` checks disabled/in-progress/wrong-PIN states and recovery UI. `SecurityLifecycleTest` checks PIN creation and recovery display across Activity recreation/backgrounding and prevents duplicate retained rollback jobs. | Host tests use controlled key/provider behavior. Actual Android Keystore availability/performance and complete PIN setup/unlock/recovery interaction require device testing. |
| Biometric and device screen lock | Security tests exercise availability decisions, including Android 10 device-credential behavior. Source integrates authentication callbacks and retained Activity handling. | Actual fingerprint/face/system PIN prompts, cancellation, rotation, enrollment changes and background return are **not physically verified**. Test Android 10 and current Android where possible. |
| Google authorization | `GoogleAuthorizationOutcomeTest` checks result/error classification and preserves useful SDK errors returned with a canceled activity result. | This does **not** contact Google or prove OAuth setup. Installed APK package and signing SHA-1 must match the Android OAuth client. Consent, account selection, authorization cancellation and reconnect require real phone/account testing. |
| Drive upload/list/download | `DriveHttpTest` inspects requests/responses through mocked HTTP connections. `DriveReliabilityTest` exercises duplicate prevention, expired tokens, safe errors and upload acknowledgement when refresh fails. `BackupLifecycleTest` recreates/backgrounds the real Activity during an upload backed by a fake Drive service. | HTTP fakes do not prove network access, live API permissions, cross-device visibility or real Google account access. |
| Encrypted backup/import/restore | `Version3BackupTest`, `BackupValidationTest`, `BackupPayloadTest` and `SecurityUtilsTest` cover portable encryption, wrong password, tampering, invalid payloads, preview/restore, stale previews, rollback and interrupted-restore safety. | Real document providers, canceled file pickers, disk-full/device-keystore behavior and restoration from live Drive on a second phone still require manual testing. |
| Data compatibility | `Version3DatabaseTest` migrates a synthetic version-2 database to version 3 while preserving financial records and adding expected defaults. `DatabasePersistenceTest` exists under `androidTest` for instrumented persistence checks. | No signing configuration or financial schema changes are part of these fixes. Tests do not prove compatibility with every historical/corrupted backup or database. |

## Physical-device acceptance checks

Use synthetic records and a test phone/profile for destructive restore scenarios. Keep existing financial data intact. Record the APK version, package, signing SHA-1, Android version, phone model and result of each check.

1. **Confirm the exact APK and OAuth registration.** Check the installed APK's signing SHA-1 against its Android OAuth client and package. Follow `GOOGLE_DRIVE_SETUP.md`; enabling the Drive API alone is insufficient. Install an update with the same signing identity so existing app data is preserved.
2. **Exercise normal transactions and navigation.** Add income and expenses with fractional amounts, edit and delete only test records, search/filter History, change budgets and create/pay/cancel a test plan. Check totals and restart the app. Verify small/large text, light/dark themes, portrait/landscape, keyboard visibility and TalkBack.
3. **Try voice as separate utterances and complete phrases.** For each supported language/recognizer installed on the phone, say a category/description then an amount, reverse the order, speak them together, correct the amount, and dictate after a manual edit. Confirm only the intended fields change. Check permission denial, cancellation and unavailable/offline recognition.
4. **Test the lock modes independently.** Set an app PIN and keep its recovery code safely; restart, background/resume and try a wrong PIN followed by a correct PIN. Exercise timeout and throttling. Test fingerprint/face and device screen-lock fallback, cancel the prompt, rotate, background and return. Measure visible unlock delay on the actual phone. Use a test profile to test forgotten-PIN recovery.
5. **Verify live Drive, end to end.** Connect the intended Google account, complete consent, create an encrypted backup with a known test backup password, list it, disconnect/reconnect and list it again. On a second test phone/profile using the same account, preview and restore it, then compare transactions, budgets, plans and supported preferences. Test wrong password and canceled authorization without replacing data. Test offline access and retry. A successful account picker alone is not a successful upload or restore.
6. **Verify local backup and reminders.** Export/import an encrypted test backup through the actual Android document picker. Confirm planned-reminder and spending-alert permissions, delivery, deduplication, background behavior and reboot behavior under the phone's battery settings.

The **app-lock recovery code** resets a forgotten app PIN; it is separate from the **backup password** used to decrypt exported/Drive backups. Do not treat the recovery code as a Google credential or a backup decryption password.

## Completion criteria

An automated pass means the specific scenarios above passed under the recorded test environment. Live Google Drive authorization/upload/list/restore, hardware authentication and speech recognition remain unverified until the corresponding physical-device checks are recorded. Do not describe this matrix as exhaustive testing of every phone or every possible app interaction.


## Review artifacts

These images are rendered by Robolectric from the real Compose screens using synthetic data. They are not screenshots from a physical phone.

- [Classic financial insights](images/financial-insights.png)
- [Midnight switch at 1.8× text with RTL layout](images/financial-insights-midnight-rtl.png)
- [App PIN screen](images/app-lock.png)
- [Small-screen Home navigation](images/home-small-screen.png)

The APK retains version 3.0.0 / code 4 and the existing debug signing identity. No signing configuration was changed. The owner approved source and APK publication after these checks. This build is prepared for GitHub prerelease `v3.0.0-rc.2`; physical-device acceptance remains pending.

Final debug APK SHA-256: `3bbae34740f880fde4558b6035f6ace2d7faf33e69fca3179c2fc25c892793b7`.
