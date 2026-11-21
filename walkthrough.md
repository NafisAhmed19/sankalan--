# Security Summary

**Reviewed Files**: 20
**Issues Resolved**: 5

### Issues Resolved
| Area | File | Line | What Was Happening | Impact | Status | How It Was Fixed |
|---|---|---|---|---|---|---|
| PII in System Logs | `app/src/main/java/com/example/core/cloud/FirestoreModelsAndErrorHandler.kt` | 45 | Firestore error logging serialized the signed-in user's email, UID, and provider emails into `Log.e(...)` (which stays active in release/debug builds). | Medium | Fixed | Redacted `users/<uid>` paths and removed `userId`, `email`, and `providerInfo` from the logged error JSON. |
| Weak PIN Stretching & Missing Lockout | `app/src/main/java/com/example/features/security/AppLockManager.kt` | 66 | Local App Lock hashed 4–6 digit PINs with a single SHA-256 pass and allowed unlimited rapid guesses without cooldown. | Medium | Fixed | Upgraded PIN hashing to 60,000-iteration PBKDF2-HMAC (`PBKDF2WithHmacSHA256` with API <26 fallback), auto-migrating legacy hashes on unlock, and added persistent 30s–120s cooldown lockout after 5 wrong guesses. |
| Backup External Path Injection | `app/src/main/java/com/example/features/export/BackupArchiveManager.kt` | 158 | Restoring a crafted `.sankalan` archive preserved arbitrary local filesystem paths in `blobRef` when no matching `media/` entry existed, and lacked a total extracted byte cap. | Medium | Fixed | Cleared unmapped `blobRef` values on restore so only files safely extracted inside `restored_media/` are referenced, and added a 256 MB / 5,000-entry archive extraction budget. |
| Update URL Authority Spoofing | `app/src/main/java/com/example/features/update/AppUpdateChecker.kt` | 108 | Custom repository and `downloadUrl` validation only checked `startsWith("https://")`, allowing authority-spoofing URLs containing `user@host` (e.g. `https://github.com@evil.example/apk`). | Medium | Fixed | Added `URI`-parsed `isSafeHttpsUrl()` rejecting non-HTTPS schemes, empty hosts, and any `userInfo`/`@` authority component; disabled HTTP caching and ensured connection cleanup. |
| Client Auth & Document ID Guards | `app/src/main/java/com/example/core/cloud/CloudAccountAndFeedbackRepository.kt` | 37 | Snapshot listeners could subscribe before verifying active UID match or signed-in state, and document IDs did not reject `/` path separators. | Low | Fixed | Added client-side auth/UID equality checks before subscribing to streams and rejected blank or `/`-containing IDs in profile/feedback calls. |

### Safeguards Verified
| Area | File | How It's Protected |
|---|---|---|
| Owner-Isolated Cloud Profiles | `firestore.rules` | `/users/{userId}` restricts `get`, `list`, `create`, `update`, and `delete` strictly to `request.auth.uid == userId`, with immutable `userId`, `createdAt`, `authProvider`, strict field allowlists, and `request.time` timestamp validation. |
| Community Feedback Isolation | `firestore.rules` | `/feedback/{feedbackId}` requires Google Sign-In, forbids email/PII storage, validates message bounds (`1..500`), category allowlist, and rating (`1..5`), and restricts updates/deletes to the original author UID. |
| Offline-First Diary Storage | `app/src/main/java/com/example/core/data/local/SankalanDatabase.kt` | All personal diaries, pages, text, sketches, images, and voice clips remain exclusively in the device-private Room database and internal storage, never uploaded to Firestore. |
| Google Sign-In Verification | `app/src/main/java/com/example/features/auth/AuthScreen.kt` | Uses Jetpack `CredentialManager` (`GetSignInWithGoogleOption` / `GetGoogleIdOption`) ID tokens verified server-side by Firebase Authentication; no anonymous or unverified password auth is enabled. |
| Transport & Backup Hardening | `app/src/main/AndroidManifest.xml`, `app/src/main/res/xml/network_security_config.xml` | Enforces `usesCleartextTraffic="false"`, trusts only system CAs, disables cloud/ADB backup (`android:allowBackup="false"`), and isolates background services (`exported="false"`). |
| Release Build Hardening | `app/proguard-rules.pro`, `app/build.gradle.kts` | Enables R8 code shrinking/obfuscation and strips verbose/debug/info logs in release builds. |

## PoC Verification

### Unredacted PII in Error Logs

#### Summary
| Field | Value |
|---|---|
| Issue Type | Sensitive Account Data Logged to System Log |
| Impact | Medium |
| Affected File | `app/src/main/java/com/example/core/cloud/FirestoreModelsAndErrorHandler.kt:35` |
| Test Scenario | Call `handleFirestoreError(IllegalStateException("PERMISSION_DENIED"), OperationType.WRITE, "users/uid_secret_123")` |

#### How It Was Fixed
Removed `email`, `userId`, `tenantId`, and `providerInfo` serialization from `handleFirestoreError()` and replaced `users/<actual_uid>` paths with `users/<uid>`.

#### Step-by-Step Trace
| Step | What Happens in the Code | Outcome |
|---|---|---|
| 1 | Exception + path `users/uid_secret_123` arrive at `handleFirestoreError()` | Input enters formatter |
| 2 | Path segments are inspected (`segments[0] == "users"`) and `authSummary` records only `signedIn`/`emailVerified` booleans | UID masked to `users/<uid>`; emails excluded |
| 3 | Output JSON is written to `Log.e` and returned | **Handled Safely** (zero PII in log string) |

#### Conclusion
**Fix Verified** - Neither the user's Gmail address nor their raw UID is emitted to `logcat` on Firestore errors.

---

### Unlimited Wrong PIN Attempts & Single-Pass PIN Hash

#### Summary
| Field | Value |
|---|---|
| Issue Type | Weak Key Derivation & Missing Brute-Force Rate Limiting |
| Impact | Medium |
| Affected File | `app/src/main/java/com/example/features/security/AppLockManager.kt:66` |
| Test Scenario | Submit 5 consecutive incorrect PIN guesses (`"0000"`) followed immediately by the valid PIN (`"4321"`), and test verifying a legacy `sha256_v1` stored hash |

#### How It Was Fixed
Switched new/migrated PINs to 60,000-iteration PBKDF2-HMAC (`hashPinStrong`), persisted failed attempt counts and cooldown timestamps (`verifyPin`), and blocked guessing + keypad taps during active cooldowns.

#### Step-by-Step Trace
| Step | What Happens in the Code | Outcome |
|---|---|---|
| 1 | 5th wrong PIN `"0000"` enters `AppLockManager.verifyPin()` | Constant-time PBKDF2 verification fails; `failedAttempts` reaches `5` |
| 2 | Cooldown schedule sets `pin_lockout_until_ms = clockMs() + 30_000` | 30s lockout persisted |
| 3 | Subsequent call `verifyPin("4321")` before 30s checks `remainingLockoutSeconds() > 0` at entry | **Handled Safely** (immediately rejected until cooldown expires; UI disables keypad with live timer) |

#### Conclusion
**Fix Verified** - Brute-force PIN guessing is throttled on-device and stored hashes require PBKDF2 work-factor derivation.

---

### Arbitrary Local Path Injection via Restored Backup Manifest

#### Summary
| Field | Value |
|---|---|
| Issue Type | Untrusted Archive Path Reference Injection & Unbounded Extraction |
| Impact | Medium |
| Affected File | `app/src/main/java/com/example/features/export/BackupArchiveManager.kt:158` |
| Test Scenario | Import a `.sankalan` ZIP whose `manifest.json` contains an image element with `blobRef = "/data/data/com.example/shared_prefs/sankalan_lock_store.xml"` and no corresponding `media/` zip entry |

#### How It Was Fixed
Forced `readFullBackup()` to assign `blobRef` strictly from the verified `extractedMediaPaths` map (setting any unmapped path to `null`), alongside a 256 MB total extracted byte cap and 5,000-entry limit.

#### Step-by-Step Trace
| Step | What Happens in the Code | Outcome |
|---|---|---|
| 1 | Crafted ZIP enters `BackupArchiveManager.readFullBackup()` | Archive entries parsed within size/count caps; `extractedMediaPaths` stays empty |
| 2 | Manifest element `blobRef` is looked up in `extractedMediaPaths` | Lookup returns `null` |
| 3 | `el.copy(blobRef = mapped)` overwrites injected path with `null` | **Handled Safely** (no private local path retained) |

#### Conclusion
**Fix Verified** - Imported backups can reference only media safely extracted inside `filesDir/restored_media/`.

---

### Authority-Spoofing HTTPS URLs in Update Manifests

#### Summary
| Field | Value |
|---|---|
| Issue Type | URL Authority Spoofing (`user@host`) |
| Impact | Medium |
| Affected File | `app/src/main/java/com/example/features/update/AppUpdateChecker.kt:84` |
| Test Scenario | Pass `"https://github.com@evil.example/malware.apk"` as `downloadUrl` in `UpdateReleaseManifest` |

#### How It Was Fixed
Added `URI`-parsed `isSafeHttpsUrl()` rejecting non-HTTPS schemes, blank hosts, and any `userInfo` / `@` character in the URI authority.

#### Step-by-Step Trace
| Step | What Happens in the Code | Outcome |
|---|---|---|
| 1 | Manifest with `downloadUrl = "https://github.com@evil.example/malware.apk"` enters `parseUpdateManifest()` | Invokes `isSafeHttpsUrl(parsed.downloadUrl)` |
| 2 | `URI.userInfo` is non-null (`"github.com"`) and `rawAuthority` contains `'@'` | Returns `false` |
| 3 | `parseUpdateManifest()` returns `UpdateCheckResult.Failed` and `openReleasePage()` refuses launch | **Handled Safely** |

#### Conclusion
**Fix Verified** - Update manifest and release-page links cannot disguise a malicious host behind `https://github.com@evil.example`.
