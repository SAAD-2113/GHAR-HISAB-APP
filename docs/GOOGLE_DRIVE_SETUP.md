# Google Drive setup and device migration

Version 3 uses Google Identity Services `AuthorizationClient` and the Drive REST API. The owner has configured the Google Cloud project, Drive API, consent screen and Android OAuth client. A November 2026 device report exposed a signing-certificate mismatch between the client screenshot and the cloud-built APK; add the matching client below before retesting. This environment has not independently verified a phone authorization session. Offline transactions, app lock, forecasts and encrypted local backup do not require Google Cloud. Live Google authorization and cross-device restoration still need the setup and device checks below.

## Reported authorization failure: installed APK certificate

The supplied OAuth screenshot registers SHA-1 `35:B3:07:40:BD:70:25:70:61:D1:DD:AD:72:BE:46:F4:8C:A1:9B:F3`.
The cloud-built debug APK uses SHA-1 **`76:BC:E8:4C:C4:6C:4F:8B:4E:AE:13:32:25:30:FF:8D:67:CF:77:C7`**, verified with `apksigner verify --print-certs`.
These certificates do not match. An enabled Drive API does not remove the Android client certificate requirement.

1. Keep the existing client if it serves another build.
2. In the same **GHAR HISAB** project, open **Google Auth Platform → Clients → Create client → Android**.
3. Set package to `com.sindantechnologies.gharhisab` and SHA-1 to the cloud APK fingerprint above.
4. If the consent audience is in Testing, confirm the selected Google account is a test user.
5. Allow configuration propagation, reopen the app, then retry **Connect Google Account**.
6. If it fails, record the displayed status code and check the package/certificate shown by the app against this client. A different installed build may have another certificate.

The app now parses Google's result Intent even when Android returns `RESULT_CANCELED`, so configuration errors retain their actual SDK status instead of being mislabeled as user cancellation. Only an explicit Google cancellation status is described as cancellation. This change improves diagnosis; it cannot register an OAuth client on the owner's behalf.

## Configure the project

1. In [Google Cloud Console](https://console.cloud.google.com/), create or select a project owned by Sindan Technologies. Keep the same project for all GharHisab Android signing clients so installations share the same application's Drive data.
2. Under **APIs & Services → Library**, enable **Google Drive API**.
3. Open **Google Auth Platform** (or OAuth consent screen). Configure app name, support email, developer contact and audience. For an external app in Testing, add each testing Google account as a test user. Complete the branding, privacy-policy and publishing requirements Google shows before public distribution.
4. Configure the requested scope: `https://www.googleapis.com/auth/drive.appdata`. The app requests only its private application-data area; it does not request full Drive access.
5. Under **Clients → Create client → Android**, enter package name **`com.sindantechnologies.gharhisab`** and the **SHA-1 fingerprint of the certificate that signs the installed APK**. Create separate Android clients in this same project for different debug/release certificates. With Play App Signing, use the Play **app signing** certificate for Play-installed builds; the upload certificate alone is insufficient.
6. Install the matching build on a device with supported Google Play services and a Google account. Open **Settings → Google Drive / Backup → Connect Google Account** and complete Google's authorization UI. Choose the test account configured above.

No client secret, web client ID, service-account key or `google-services.json` is needed by this implementation. The Android package/certificate registration identifies the client. Never commit signing keys, tokens, recovery passwords or PIN recovery keys.

To read the fingerprint for your build:

```bash
bash gradlew :app:signingReport
# Alternatively, using Android SDK build-tools:
apksigner verify --print-certs app/build/outputs/apk/debug/app-debug.apk
```

Register the fingerprint shown by **your actual build**. Cloud debug keys can change between environments; use a stable release signing identity for distribution. A different signing key cannot update an existing installation. Preserve financial data and use a separate test device or the original signing key instead of uninstalling the existing app.

## Backup and restore workflow

1. On the old device, connect the Google account and enter a recovery password of at least 12 characters twice. Use a long, unique passphrase and retain it outside the phone. Tap **Back Up Now**, wait for success and check the listed backup time.
2. On a second device, install a build registered in the same Google Cloud project, finish onboarding and connect the same account.
3. Select a listed snapshot (the latest 20 are shown), enter its recovery password and request a preview. Google account authorization alone never replaces local records.
4. Inspect the snapshot date and record counts. Confirm **Restore** only when the replacement is intended. The app rejects the preview if local data has changed since it was prepared.
5. Verify transactions, categories, balances, budgets, recurring plans and preferences. Restart the app and verify the restored data remains available offline.
6. **Restore previous local data** in Settings restores the encrypted safety snapshot made before the replacement. This is a device-local rollback archive, not a substitute for a portable backup.

**Sync Now refreshes the available backup snapshots.** There is no automatic two-way record merging, background cloud upload, or silent last-writer-wins replacement. Uploads create new immutable snapshots; the app does not delete old snapshots. An interrupted upload can leave a duplicate cloud snapshot if retried, but cannot overwrite local financial records.

`appDataFolder` is hidden from the ordinary Drive file browser and cannot be shared like a normal file. Use **Export Encrypted Backup** when a portable file is wanted. Disconnect revokes authorization; it does not erase existing snapshots. If an offline revocation fails, retry online or remove access in the Google account's third-party access settings.

## Encryption and recovery

Portable snapshots contain versioned JSON encrypted locally with AES-256-GCM and a password-derived key (PBKDF2-HMAC-SHA256, 600,000 iterations, random 16-byte salt, random 12-byte IV). The GCM tag detects altered ciphertext. The password and derived key are not uploaded alongside the snapshot. Older v1 payloads and the previous 10,000-iteration backup envelope remain readable; newly exported backups use the stronger envelope.

The Google account grants access to encrypted bytes; it cannot recover a forgotten backup password. **Losing the password can make those backups permanently unrecoverable.** The app PIN recovery key is a separate credential and cannot decrypt a Drive backup. Android Keystore protects the local safety archive and the device-bound app PIN verifier; these keys are intentionally not transferable to another device.

## Troubleshooting and acceptance checks

- Authorization configuration errors: verify exact package, installed signing certificate, Drive API enabled, consent audience/test users and Google Play services. Configuration changes can take time to propagate.
- No backups: verify the same Google account and Cloud project, then refresh. An empty list is displayed explicitly.
- Expired/revoked authorization: reconnect; access tokens are kept only in memory. Closing the panel or relocking can require reconnection.
- Network failure: retry online. Local record entry remains available; cloud failures do not clear the database.
- Invalid password, corrupted or incompatible file: restore stops before mutation. Obtain the correct password or a different snapshot.
- Test consent cancellation, denied permission, offline upload/download, revoked access, corrupted ciphertext and stale restore previews. Verify real Android Keystore behavior and second-device restoration before production release.

The cloud environment built the integration and tested portable encryption/restore logic. The owner must confirm the installed APK certificate matches a configured Android client. This environment has no attached Android device or Google authorization session. It has **not** verified a real Drive upload/download or device-to-device recovery.
