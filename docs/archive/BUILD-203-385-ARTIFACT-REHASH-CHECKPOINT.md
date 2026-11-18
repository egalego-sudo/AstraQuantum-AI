# QUANTUM AI — Artifact Re-hash Checkpoint (Builds 203–385)

**Repository:** `egalego-sudo/Quantum-AI`  
**Branch:** `archive/build-174-artifacts`  
**Scope:** Continue after the user's verified baseline of 95 ZIPs for Builds 174–202.  
**Execution date:** 2026-11-18

## Verified result

| Range | ZIPs downloaded and locally re-hashed | GitHub SHA-256 match | ZIP integrity |
|---|---:|---:|---:|
| 205–208 | 12 | 12/12 PASS | 12/12 PASS |
| 215–252 | 34 | 34/34 PASS | 34/34 PASS |
| 253–283 | 25 | 25/25 PASS | 25/25 PASS |
| 284–318 | 27 | 27/27 PASS | 27/27 PASS |
| 319–337 | 42 | 42/42 PASS | 42/42 PASS |
| 344–362 | 30 | 30/30 PASS | 30/30 PASS |
| 364–385 | 43 | 43/43 PASS | 43/43 PASS |
| **Total after Build 202** | **213** | **213/213 PASS** | **213/213 PASS** |

The local SHA-256 was calculated from each downloaded ZIP's bytes and compared with the GitHub Actions artifact API digest. ZIP CRC/member integrity was tested independently. The audit did not substitute the API digest for the local calculation.

## Build gaps and partial runs

- Builds 203, 204 and 207: no Android-build workflow run in the inventory (workflow-file runs only).
- Builds 209–214: queried runs returned zero artifacts.
- Builds 215–318: the existing inventory/audit records partial artifact sets and no-artifact runs. All 86 ZIPs present in the three existing manifests were re-downloaded and independently verified.
- Builds 319–385 with retained artifacts: 115 ZIPs were independently verified. The following runs returned zero artifacts: 324, 326–327, 330, 338–343, 356, 363, 365, 367–368, 370–371, 376 and 383. Builds 369 and 372–375, 377–382 and 384–385 had retained artifacts as itemized in the run/artifact API.
- Failed/cancelled workflow status is not treated as a successful Android build. A retained source-patch artifact is recorded as a partial artifact set, not a complete APK release set.

## Corrections and integrity notes

- The earlier CSV manifests for Builds 215–318 had `independent_sha256` values copied from `github_sha256`. That was insufficient proof by itself. This checkpoint records that all 86 corresponding ZIPs were subsequently downloaded and locally re-hashed; all 86 local results matched the GitHub digests and passed ZIP integrity tests.
- A temporary filename/build-label offset was detected while processing Builds 375 and 377. Artifact IDs and workflow-run IDs were used to correct the local labels; the missing Build 378 artifacts were downloaded and verified. The final Build 319–385 count is 115 ZIPs, including Builds 375, 377 and 378 correctly attributed.
- No hash mismatch or ZIP integrity failure remained in the final verified set.
- No artifact was fabricated for a missing/expired/zero-artifact run.

## Explicitly NOT VERIFIED

- APK/AAB cryptographic signing identity and certificate trust.
- APK installability and runtime behavior on a physical Android device.
- Application unit, integration, regression, security, performance and stability tests (this task is historical artifact archival/verification, not an application source-code release build).
- Permanent second binary backup outside GitHub Actions artifact retention.
- Builds 1–173 and any artifacts not present in the referenced manifests or queried runs.

## Safety and checkpoint

- Only the working archive branch is in scope.
- No production deployment, merge, release publication, destructive deletion or irreversible repository action was performed.
- Verified after-202 range: **213 ZIPs**. With the user's stated baseline of 95 ZIPs for Builds 174–202, the stated cumulative count is **308 ZIPs**.
- Next step: preserve a machine-readable per-artifact manifest for Builds 319–385, review repository diff and branch consistency, and independently validate the checkpoint before declaring completion.
