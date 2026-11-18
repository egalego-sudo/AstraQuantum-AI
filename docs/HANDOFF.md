# QUANTUM AI — Project Handoff & Backup Guide

**Creator by Bittu Pandit**

This document describes the current repository and how to resume work safely.

## Canonical source

- Repository: https://github.com/egalego-sudo/Quantum-AI
- Default branch: `main`
- Source ZIP: https://github.com/egalego-sudo/Quantum-AI/archive/refs/heads/main.zip
- Current maintenance branch (review before merging): `maintenance/quantum-ai-consistency-fixes`

The GitHub repository is the canonical source backup. Download the ZIP from the branch you intend to use; a ZIP is only a snapshot and does not include deployment secrets, phone-local app data, or downloaded model weights.

## Project layout

- `.github/workflows/android-build.yml` — backend checks and Android debug/release build workflow.
- `README.md` — implemented capabilities and their limits.
- `app/` — Kotlin + Jetpack Compose Android application.
- `ApkPatchLibrary/` — native/library support used by the Android update mechanism.
- `backend/` — Node.js API service, research retrieval, knowledge handling and tests.
- `knowledge-base/` — knowledge schema, seed records and source registry.
- `docs/HYBRID_ARCHITECTURE.md` — online/offline architecture and acceptance gates.
- `docs/CAPABILITY-EXPANSION-PLAN.md` — staged engineering plan.

The existing Android application ID and Kotlin namespace are `com.bittupandit.astraquantumai`. Keep them unchanged unless a deliberate application-identity migration is planned; changing them can prevent in-place updates and access to existing app-private data.

## Build environment

- Android: Java 17, Gradle 8.9, compile SDK 35.
- Backend: Node.js 20.16 or newer.
- CI: GitHub Actions workflows under `.github/workflows/`.
- CI debug APK artifacts are temporary test builds, not automatically Play Store-ready signed releases.

Check the latest GitHub Actions run before treating a source revision as build-verified. A successful CI build does not replace installation and acceptance testing on a real Android device.

## Backend deployment

The connected Render workspace currently lists the backend service as:
- Service: `AstraQuantum-AI` (Render display name)
- Repository: https://github.com/egalego-sudo/Quantum-AI
- Branch: `main`
- Root directory: `backend/`
- Runtime URL: https://astraquantum-ai.onrender.com
- Dashboard: https://dashboard.render.com/web/srv-dbt2v3vf3r2c73bpcn0g

The Android app currently uses this HTTPS backend URL as its default. The Render service name and URL are legacy infrastructure identifiers; do not rename the service or change its URL without updating the app configuration and verifying the deployment.

Keep `GROQ_API_KEY`, IBM Quantum credentials, signing keys and any access tokens in private provider environment settings. Never commit real secrets to GitHub or paste them into chat.

## Current implementation boundaries

- Online Hindi/English chat and image analysis use the configured Groq backend.
- Voice input and text-to-speech depend on Android/device speech services.
- Research uses bounded public-source retrieval and model-assisted synthesis; it is not exhaustive web search or independent proof of claims.
- Local notes, projects and chat history are stored in app-private preferences. Personal-context sharing requires the app's explicit opt-in.
- The arithmetic calculator is bounded and does not execute arbitrary code.
- Generated HTML previews use a restricted WebView; do not treat preview isolation as a general-purpose code-execution sandbox.
- Text PDFs are extracted by the backend. For scanned PDFs, the Android app attempts bounded on-device Latin and Devanagari OCR; quality still requires real-device acceptance testing.
- The local Quantum Lab is a classical educational simulator, not a physical quantum processor. IBM Quantum connector code requires credentials and successful hardware-job verification before a live QPU connection can be claimed.
- The optional Qwen3-0.6B on-device text model requires model download and initialization. Offline capability must be verified in airplane mode on a real device before being described as proven.

## Resume safely

1. Open the repository above and inspect the latest commit.
2. Download the ZIP for the intended branch or clone the repository.
3. Open the project root (the folder containing `settings.gradle.kts`) in Android Studio.
4. Use JDK 17 and allow Gradle sync to finish.
5. Run backend tests with `npm test --prefix backend`.
6. Run the Android CI workflow or build locally with `gradle --no-daemon assembleDebug`.
7. Install and test the resulting APK on a real Android device.
8. Keep provider credentials and signing material in their private secret stores.

## Backup and compatibility rules

- Preserve the Android application ID, namespace, legacy preference keys and update-signing identity unless a migration plan explicitly covers them.
- Treat Build releases and patch files as historical snapshots/deltas; do not copy every historical build over the current source tree.
- Do not claim all Builds 1–385 are archived until every requested release/patch is inventoried and verified.
- GitHub source does not contain Render secrets, user phone data or downloaded model weights.
