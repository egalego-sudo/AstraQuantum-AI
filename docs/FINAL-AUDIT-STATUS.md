# QUANTUM AI — Whole-Project Audit Status

**Creator by Bittu Pandit**  
**Audit snapshot:** 2026-11-18  
**Repository:** `egalego-sudo/Quantum-AI`  
**Production branch inspected:** `main` at `6b292fda0b894f0f9de844e436a81b74a3b3d78a`

This is an evidence-based status record. Repository structure checks, workflow completion, release metadata and downloaded artifact hashes do not substitute for physical-device acceptance, live-provider tests or independent security testing.

## Ten-point audit

| # | Area | Current evidence | Status / remaining gate |
|---|---|---|---|
| 1 | Source files and project structure | Recursive Git tree for current `main` was fully returned (not truncated): 154 tree entries, 120 tracked files and 34 directories. Core directories `.github`, `ApkPatchLibrary`, `app`, `backend`, `docs`, `knowledge-base` and `scripts` are present. No zero-byte tracked files and no duplicate identical Git blob-SHA groups were found. Root Gradle files, README and .gitignore are present. | **Structural inventory check passed.** This does not certify every source file's behavior or every runtime path. The audit documentation is being corrected in this branch; merge/review is still required. |
| 2 | Builds 1–385 and historical backups | Historical inventory records 385 sequential build numbers, 380 Android-build runs (185 success, 61 failure, 134 cancelled), five Android-run gaps (91, 92, 203, 204, 207), and 36 historical Releases. Current repository has 38 Releases total including Builds 386 and 387. Across those Releases, 280 assets are listed and GitHub reports a digest for every asset. Seven selected missing historical targets (1, 2, 91, 92, 203, 204, 207) were rebuilt as DEBUG APKs, downloaded from Actions, extracted and independently SHA-256 checked against each build's embedded checksum. A combined user-downloadable archive was created; its SHA-256 is recorded in `docs/LEGACY-APK-RECOVERY-2026-11-18.md`. | **Partial.** Seven selected DEBUG APKs have been recovered and verified. They are not production-signed releases. The full historical Actions APK/delta/source artifact sets have not all been downloaded, individually re-hashed and placed in a durable long-term repository/release archive. GitHub Actions artifact retention is temporary. Do not claim all historical binaries are backed up. |
| 3 | Android app and navigation | Earlier source audit reviewed manifest permissions, app lock, file picker, update UI, restricted generated-code preview, product branding and creator credit; navigation mapping and backend Bearer-token policy tests were added. | **Source/CI evidence only; phone acceptance pending.** Screen-by-screen taps, voice, file selection, update installation, lifecycle and data migration require physical-device tests. |
| 4 | AI and model integration | Backend source and bounded request/response handling, provider routing and safe retrieval paths were reviewed in the prior audit. Backend unit suite previously passed 51/51 tests. | **Automated tests passed previously; live calls unverified.** Authenticated production text, vision and research requests still need safe live verification. |
| 5 | Backend and deployment | Backend health endpoint and Render deployment were inspected in the prior audit; service identity and deployment metadata were recorded there. | **Partially verified.** Current health, current environment-variable names/presence, authenticated routes and runtime metrics need a fresh production check. Secret values must not be exposed. |
| 6 | Security and privacy | Prior source review covered bounded upstream response reads, HTTPS/public-host validation, redirect checks, encrypted-preference migration ordering, native patch input limits, malformed-patch smoke tests and dependency audit. | **Static review and automated checks only.** Coverage-guided fuzzing, independent penetration testing, full secret-history scan and device-level security review remain. |
| 7 | Memory and Projects | Prior source review covered encrypted app-private chat history, Projects/notes, opt-in memory sharing and migration ordering. | **Source-reviewed; physical migration/restore test pending.** |
| 8 | Offline mode | Prior source review covered Qwen3-0.6B download bounds, pinned SHA-256, initialization re-check, app-private storage and GPU-to-CPU fallback. | **Not device-verified.** Download, initialization, memory pressure, latency and airplane-mode behavior need a compatible phone test. |
| 9 | Special features | Prior review covered research retrieval, PDF/OCR path, voice/image integrations, calculator, Quantum Lab and builder flows. Quantum Lab is a classical simulator, not proof of physical QPU execution. | **Source/tests only; provider and device acceptance pending.** OCR quality, voice, vision, research, IBM QPU and builder workflows need real tests. |
| 10 | Release and installation | Build 387 is published as a signed release. Build 387 release and signed update APK were checked in the preceding build audit. | **Release published; phone installation/runtime acceptance remains user-device dependent.** User reports installing the update, but the installed version number has not yet been independently read from the phone. |

## Point 1 — structural scan detail

The recursive Git tree response for `main` was complete (`truncated=false`). Counts at the scan:
- Tree entries: 154
- Tracked files: 120
- Directories: 34
- Zero-byte tracked files: 0
- Duplicate identical blob-SHA groups: 0

This is a tracked-tree structural check, not a full semantic code audit or proof that all features work.

## Point 2 — historical build and recovery detail

- Historical inventory: [BUILD-INVENTORY-1-385.md](BUILD-INVENTORY-1-385.md)
- Historical Android-build runs: 380 across build numbers 1–385.
- Run outcomes: 185 success, 61 failure, 134 cancelled.
- Android-build run gaps: 91, 92, 203, 204, 207.
- Historical Release tags: 36 for builds 1–385.
- Current Release count at this audit: 38 (includes Builds 386 and 387).
- Release assets across the 38 Releases: 280; GitHub supplied a digest for each listed asset.
- Seven selected recovered targets: 1, 2, 91, 92, 203, 204, 207.
- Each recovered target is a DEBUG APK. Each extracted APK SHA-256 matched the checksum embedded in its downloaded Actions artifact.
- Combined archive: `QUANTUM-AI-Legacy-APK-Archive-2026-11-18.zip` (created for user download in this conversation; not uploaded as a GitHub Release asset).
- The archive SHA-256 and per-APK hashes are recorded in [LEGACY-APK-RECOVERY-2026-11-18.md](LEGACY-APK-RECOVERY-2026-11-18.md).

### Important backup limitation

The seven selected APKs are not the full historic binary archive. Actions artifacts expire; a GitHub Release asset digest is metadata and is not a substitute for downloading and independently hashing the asset bytes. Historical APKs, source patches and binary deltas outside the seven recovered targets still need durable archival. Recover missing files only from authentic GitHub artifacts, Releases or user-held backups; never fabricate them.

## Remaining gates

1. Complete durable backup of all recoverable historical Actions APK, source-patch and delta artifacts; independently hash each downloaded payload and record unavailable/expired items.
2. Reconcile historical Release assets by downloading and hashing payloads, not only reading their metadata.
3. Verify current Render health and authenticated routes without exposing secrets.
4. Run Android navigation, update, encrypted-data migration and restore tests on the physical phone.
5. Verify authenticated live Groq text, vision, research and PDF flows.
6. Test offline model download/initialization and airplane-mode behavior on a compatible phone.
7. Test updater signature, package identity, full-APK fallback and patch-hash handling on device.
8. Add coverage-guided native patch fuzzing, complete a secret-history scan and obtain independent security review.

Do not mark the entire ten-point project audit complete until the applicable remaining gates have been evidenced. Do not install the recovered historical DEBUG APKs over the current app; their signing identity and update lineage are not certified.


## Superseding Build 387 release-integrity verification (2026-11-18)

This section adds independent byte-level checks performed after the earlier audit snapshot. It does not close the physical-device or full historical-archive gates.

- GitHub Actions Android Build run [44237351176](https://github.com/egalego-sudo/Quantum-AI/actions/runs/44237351176), run number 387, completed with conclusion `success`; its single build job completed successfully.
- The signed-update Actions artifact was downloaded. Its ZIP extracted successfully and contained `app-release.apk` (104,060,996 bytes).
- The extracted APK SHA-256 was independently computed as `0cf5bfa4c8fd128d07fa8c6985e75c719d560c0b6096a482243dd27d98a85103`. This exactly matches the GitHub Release asset digest and the `targetApkSha256` value in Build 387 update metadata.
- The multi-base update artifact was downloaded and extracted. Metadata identifies package `com.bittupandit.astraquantumai`, versionCode `387`, versionName `1.0.387`, and signer certificate SHA-256 `d6396923299c943b5726af5b48126969e257d55e09f987286d2b68b238312b6d`.
- The public update-signing certificate was extracted from `assets/update_signer.pem` inside the APK. Its DER SHA-256 independently matches the metadata signer-certificate fingerprint.
- Detached `UPDATE-METADATA.sig` verification against the public certificate and exact metadata bytes returned OpenSSL `Verified OK`.
- All six published patch files were extracted; each file's independently computed SHA-256 and byte size matched its corresponding metadata record. The six base builds are 386, 385, 384, 382, 381 and 150.
- This confirms release-payload integrity and metadata authenticity for Build 387. The GitHub Actions workflow log also shows `apksigner verify --print-certs` was invoked on the APK and that six base patches were reconstructed and verified by the workflow.
- The APK has not been installed on a physical Android device in this audit. Installed-app version, update continuity, runtime behavior, device-specific stability and offline inference remain NOT VERIFIED.
- The signed APK is 104,060,996 bytes (about 99.2 MiB). A conversation copy was created for user download; this does not constitute a durable repository backup.


- The 2026-11-18 release-asset CSV was independently reconciled against the live GitHub Releases API: 38 releases and 280 assets on both sides, with zero missing rows, zero extra rows, zero mismatched names/sizes/digests/download URLs, and a valid SHA-256 digest present for every listed asset. This verifies the inventory metadata only; it does not mean all 280 asset payloads were downloaded or independently hashed.

**Build 387 artifact-integrity conclusion:** APK digest, metadata digest, update-signing certificate fingerprint, detached metadata signature and all six patch hashes/sizes match. **Whole-project audit remains incomplete.**


## Render deployment status recheck (2026-11-18)

- Render service `srv-dbt2v3vf3r2c73bpcn0g` was queried through the connected Render account. It is a Node web service named `AstraQuantum-AI`, on the Free plan, configured for the repository `egalego-sudo/Quantum-AI`, branch `main`, root directory `backend/`, with automatic deploys enabled.
- The latest deployment record returned status `live` for commit `225d4c5bcf9c692d0ebee317e1d7aff79bd44d59`. The service was not suspended at query time.
- The service configuration currently reports an empty `healthCheckPath`; therefore Render's configured deploy health-check gate is absent. This is recorded as a configuration gap, not proof that the backend is unhealthy. No service configuration was changed.
- CPU/memory/request/latency metrics and a fresh request-log check are **NOT VERIFIED** in this pass because the Render metrics/log tools require the user to select a workspace first. The available workspace is `Manju's workspace` (`tea-dbt2quui0phs73ehif60`); no workspace was selected by the assistant.
- A live deployment status does not prove a successful authenticated Groq request or full application functionality.


## Delta base and ABI cross-check

- Cross-checking the six `baseApkSha256` values in Build 387 metadata against the corresponding `ASTRA-QUANTUM-AI.apk` GitHub Release digests for Builds 386, 385, 384, 382, 381 and 150 returned six exact matches. This independently confirms that the metadata's selected base APK identities match the published release inventory.
- APK ZIP inspection found native library directories for `arm64-v8a`, `armeabi-v7a`, `x86` and `x86_64`. This confirms packaged ABI entries, not runtime compatibility on every device.


## Latest automated CI evidence (2026-11-18)

- Capability CI run [44240641223](https://github.com/egalego-sudo/Quantum-AI/actions/runs/44240641223) for PR #44 completed successfully. Its single job passed all listed steps: backend dependency install and npm audit, backend unit tests, build-inventory validation, Android navigation validation, native patch-parser smoke tests, Android NDK/CMake setup, Android unit tests, Android lint and Debug APK build.
- The backend suite reported **51 tests, 51 passed, 0 failed, 0 skipped**. `npm audit --audit-level=high` reported **0 vulnerabilities** for the installed backend dependency tree.
- The inventory validator reported 385 sequential build rows, 380 Android runs, five accounted gaps, 36 releases and all 380 source commits present in Git. The navigation validator reported 19 unique menu items and all routes reachable, with creator credit and product branding present.
- Capability CI run [44239847767](https://github.com/egalego-sudo/Quantum-AI/actions/runs/44239847767) for PR #43 also completed successfully with the same configured validation stages.
- These are automated CI results on the corresponding PR heads, not physical-device acceptance or independent penetration-test results.


## Focused source-security recheck (2026-11-18)

- Backend rate-limit identity logic prefers a valid Cloudflare visitor IP and deliberately does not trust caller-controlled `X-Forwarded-For`; its tests cover spoofed forwarding headers, invalid addresses and IPv6.
- Backend research fetching uses an explicit HTTPS host allowlist, rejects credentials and non-443 ports, validates each redirect target, and caps redirect depth. Tests are included in the current 51-test backend suite.
- Upstream response readers enforce both declared content-length and streamed-byte limits before parsing JSON; tests cover exact UTF-8 byte budgets and oversized streamed bodies.
- Backend private routes reject missing private-token configuration and compare supplied bearer tokens using `timingSafeEqual` after checking equal byte lengths. CORS uses an explicit origin allowlist.
- Android external-link policy restricts navigation to HTTPS public DNS names and rejects IP literals and local/private naming suffixes. The update verifier authenticates raw metadata bytes with the embedded X.509 public key, requires an approved GitHub Release URL, and checks package identity and a strictly newer version.
- The updater verifies patch SHA-256 before native patch parsing, then verifies reconstructed/full APK hash, package, version and signer against trusted metadata and the installed app. Build 387 release metadata and its six patches were independently checked above.
- No source-code changes were made during this focused recheck. These are targeted static observations backed by CI tests, not a full manual audit of every Android/backend code path or a penetration test.


## Offline engine and update-compatibility source review (2026-11-18)

- Offline model download is initiated only after an explicit user action, writes to app-private storage, bounds the downloaded byte count, checks the pinned SHA-256, and rechecks the full model hash before initialization. GPU initialization falls back to CPU when GPU initialization fails; cancellation is rethrown rather than swallowed.
- The model is identified in the UI as Qwen3-0.6B (328 MiB). Its download, integrity and runtime behavior have **NOT VERIFIED** on the user's physical device or in airplane mode.
- The Android package/application ID and Kotlin namespace remain `com.bittupandit.astraquantumai`. The old package ID is an update-lineage compatibility anchor; changing it would create a different Android app and could prevent in-place updates. User-facing app branding is QUANTUM AI. Legacy internal artifact names and service identifiers are not, by themselves, visible app branding and should not be renamed without a compatibility/migration plan.


## Encrypted preference migration recheck (2026-11-18)

- The current migration helper copies supported legacy preference types into encrypted preferences with a synchronous commit, clears the legacy plaintext store only after the encrypted copy succeeds, and writes the completion marker only after cleanup succeeds.
- The migration is idempotent and includes retry handling for failed encrypted copy, failed legacy cleanup and failed completion-marker commits. The current unit tests exercise these failure/retry cases and cleanup of stale legacy data.
- This is source and unit-test evidence. Real user-data migration, process-death recovery and restore behavior on the user's installed phone remain **NOT VERIFIED**.


## App-lock and Personal Context privacy recheck (2026-11-18)

- The app lock is enabled by default and authenticates through Android `BIOMETRIC_STRONG` or `DEVICE_CREDENTIAL`; the activity resets its unlocked state when backgrounded while the lock is enabled. The app also sets `FLAG_SECURE` to block ordinary screenshots/recents previews.
- The app stores chat history, saved memory, projects and the private API token through the encrypted preferences instance. Personal Context sharing defaults to `false`; the prompt builder returns only the user's question when opt-out is active. When opted in, the selected history/memory/project context is bounded before being sent.
- Unit tests explicitly verify that opt-out excludes conversation history, saved memory and project notes, and that opt-in context remains bounded and can retrieve relevant older numerical context.
- Backend security tests verify minimal unauthenticated health output, 401 for absent/incorrect tokens, 503 when the private token is not configured, and CORS allowlist behavior.
- Physical-device biometric/PIN lifecycle, screenshot behavior, encrypted preference restore, and real authenticated API requests remain **NOT VERIFIED** on the user's phone.


## Latest main tree and pending-PR reconciliation (2026-11-18)

- A fresh recursive tree scan at current `main` commit `c3fef058bfe2f6dec9b80fab4c892b08268a47ff` returned `truncated=false`: 157 tree entries, 123 tracked files and 34 directories; zero zero-byte tracked files and zero duplicate identical-blob groups.
- PR #43 and PR #44 are merged. Their configured Capability CI runs completed successfully.
- PR #41 remains open and is not mergeable: GitHub reports `mergeable_state=dirty`; its branch is 6 commits ahead and 6 behind current `main`, with the conflicting change limited to `docs/FINAL-AUDIT-STATUS.md`. It is stale documentation, not a pending production-code fix. It has not been merged or modified.
- The new branch `audit/verify-build387-release-integrity` is based on current `main`, currently 10 commits ahead and zero behind, and changes only `docs/FINAL-AUDIT-STATUS.md`. No PR has been opened.


## Native APK patch parser source recheck (2026-11-18)

- Native BSDIFF parser rejects patch files above 80 MiB, APK input/output above 150 MiB, malformed headers/segments, out-of-range output controls and control streams exceeding one million tuples.
- Allocation sizes are checked against platform `SIZE_MAX`; signed offset arithmetic uses checked addition; old/patch/output inode aliasing is rejected; output is written to a temporary file, fsynced and atomically renamed only after reconstruction succeeds.
- JNI bridge validates non-null paths and releases acquired UTF strings in its cleanup path.
- Host smoke tests cover null arguments, missing/truncated/invalid patches, out-of-bounds control segments and oversized APK/patch limits; latest Capability CI reports this stage passed.
- Coverage-guided fuzzing, sanitizer runs across supported ABIs and device-level patch reconstruction remain **NOT VERIFIED**. No native code was changed in this recheck.


## Android signing-secret exposure hardening (2026-11-18)

- Focused workflow review found the Android build job injected all four release-signing secrets into every run, including `workflow_dispatch` runs on non-`main` refs. Although release signing/publishing steps were gated later, earlier steps on a manually selected branch could still read job-level signing secrets.
- On this audit branch, the four job-level secret expressions were changed so the keystore, keystore password, alias and key password resolve only when `github.ref == 'refs/heads/main'`; non-main builds receive empty values and therefore cannot enter the signed-release path.
- This preserves debug builds on non-main refs and preserves the existing signed-release behavior on `main`. No signing secret values were read or exposed.
- The changed workflow file was re-fetched and the four expressions verified. **CI validation of this new workflow change is pending**; it has not been merged or run against production. A Capability CI run must validate the workflow syntax/behavior before merge.
- Workflow-level `contents: write` remains broader than ideal because the current single build job also publishes Releases. Splitting build and publish into separate permission-scoped jobs is a further hardening opportunity and is not included in this minimal fix.


- Cross-workflow permissions review found `capability-ci.yml`, `knowledge-base-tests.yml` and `legacy-rebuild.yml` declare `contents: read` and contain no repository signing-secret references. The Android build workflow is the only inspected workflow that needs write access for Release publication; its workflow-level write permission remains a documented least-privilege gap.


- As defense in depth, the release-capable Android build job now has a job-level condition requiring `refs/heads/main`. Non-main manual dispatches are skipped entirely; pull-request/debug builds remain handled by the read-only Capability CI workflow. The signing-secret expressions remain main-only as a second guard.


- A focused static assertion over the branch's re-fetched workflow returned 7/7 checks true: main-only job guard; each of the four signing secrets gated to main; checkout credentials disabled; and release publication still main-only. This is a static text/structure check, not a GitHub Actions execution or full YAML/schema validation.
