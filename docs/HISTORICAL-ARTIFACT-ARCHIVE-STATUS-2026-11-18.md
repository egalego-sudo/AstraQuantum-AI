# QUANTUM AI — Historical Build Artifact Archive Status

**Creator by Bittu Pandit**  
**Audit date:** 2026-11-18  
**Repository:** egalego-sudo/Quantum-AI

## Current verified inventory

The GitHub Releases API returned 38 releases and 280 release assets. The accompanying CSV records every returned asset's release tag, filename, byte size, GitHub-reported SHA-256 digest, and official download URL.

| Asset class | Count | Total size |
|---|---:|---:|
| APK | 38 | 2,582,148,343 bytes |
| Patch | 166 | 1,231,867,141 bytes |
| Other (metadata/signatures) | 76 | 88,064 bytes |
| **Total** | **280** | **3,814,103,548 bytes (3.55 GiB)** |

All 280 listed assets have a GitHub-reported SHA-256 digest. These are server-provided asset digests, not an independent re-download/re-hash performed in this audit. The assets remain hosted on their existing GitHub Releases; this change does not duplicate 3.55 GiB of binaries into Git history.

## Historical Actions artifacts for Builds 1–149

The earlier run inventory recorded 147 Android-build runs for this range. A previous artifact metadata pass found 122 runs with at least one unexpired Actions artifact and 25 with no retained artifact at that time. Actions artifacts are temporary and expire; they must be downloaded before their recorded expiry to preserve them. Seven selected targets (Builds 1, 2, 91, 92, 203, 204 and 207) were rebuilt as DEBUG APKs, downloaded and independently SHA-256 checked. Their details are in [LEGACY-APK-RECOVERY-2026-11-18.md](LEGACY-APK-RECOVERY-2026-11-18.md).

## What is not yet complete

- No complete durable copy of all historical Actions artifact ZIPs (APK/source-patch/delta) for Builds 1–149 has been created.
- The 280 release assets have not all been downloaded and independently re-hashed in this audit; GitHub's digest metadata is recorded as a verification reference.
- The seven recovered APKs are DEBUG builds, not original production-signed historical release APKs.
- Missing/expired artifacts must be recorded as unavailable unless recovered from a verifiable user-held backup or original source.

## Integrity and handling

- Do not treat a GitHub digest listing as proof that an independent local file hash was computed.
- Do not replace historical signed APKs with rebuilt DEBUG APKs.
- Do not install legacy DEBUG APKs over the current QUANTUM AI installation.
- Keep the CSV as a manifest and use the official Release URLs to retrieve original assets.
