# ASTRA QUANTUM AI PLUS — Project Handoff & Backup Guide

**Creator by Bittu Pandit**

This file is the restart guide for continuing the project from another Android phone, laptop, or computer.

## Canonical source of truth

- Repository: https://github.com/egalego-sudo/AstraQuantum-AI
- Default branch: `main`
- Latest verified source revision at the time this guide was written: see the latest commit on the main branch.
- Full source ZIP: https://github.com/egalego-sudo/AstraQuantum-AI/archive/refs/heads/main.zip

All source files and their Git commit history are stored in this GitHub repository. Download the ZIP again whenever you need the newest snapshot. Do not continue from an older ZIP if newer commits exist on GitHub.

## Project layout

- `.github/workflows/android-build.yml` — CI workflow for backend syntax check and Android debug APK build.
- `README.md` — project overview and implemented feature boundaries.
- `build.gradle.kts` — Android Gradle Plugin and Kotlin plugin versions.
- `settings.gradle.kts` — plugin and dependency repositories.
- `gradle.properties` — Gradle/Android build settings.
- `app/build.gradle.kts` — Android app dependencies and build configuration.
- `app/src/main/AndroidManifest.xml` — app permissions, app label, optional native libraries, launcher activity.
- `app/src/main/java/com/bittupandit/astraquantumai/MainActivity.kt` — Compose UI, chat, navigation, research shortcuts, memory/projects, Quantum Lab, offline AI screen, backend requests.
- `app/src/main/java/com/bittupandit/astraquantumai/OfflineAiEngine.kt` — on-device model download and LiteRT-LM engine integration.
- `backend/server.js` — Node.js API server and Groq connection.
- `backend/package.json` — backend runtime definition.
- `backend/.env.example` — names of expected environment variables only; no live secret.
- `backend/README.md` — backend setup and API notes.
- `docs/HYBRID_ARCHITECTURE.md` — online/offline design, boundaries, and acceptance tests.
- `docs/HANDOFF.md` — this restart and backup guide.

## Build environment

- Android: Java 17, Android Gradle Plugin 8.7.3, Kotlin Gradle Plugin 2.4.20, Gradle 8.9, compileSdk 35.
- Backend: Node.js 20+.
- CI: GitHub Actions workflow `.github/workflows/android-build.yml`.
- CI creates a debug APK artifact when a build succeeds. The artifact is temporary (14-day retention) and is not a signed Play Store release.

## Online backend configuration

The Android app calls the ASTRA backend at the URL configured in the app's Settings screen. The backend deployment currently uses Render:
- Service URL: https://astraquantum-ai.onrender.com
- Render dashboard: https://dashboard.render.com/web/srv-dbt2v3vf3r2c73bpcn0g

The Groq API key and any other live environment values are intentionally NOT stored in this repository or source ZIP. They must remain in Render's private Environment settings. Never paste API keys into GitHub source files, screenshots, or chat messages.

## Current implementation and limitations

- Online Groq text chat and image analysis are implemented in source.
- Voice input and text-to-speech use Android/device speech services.
- The Research screen calls /v1/research, which retrieves Wikipedia overview text and Crossref scholarly metadata, then returns a Groq-generated report with a source list. This is a first research layer; it is not exhaustive web search, and source retrieval does not by itself prove a claim. Browser search shortcuts remain available.
- Local notes, projects and chat history are stored in app-private local storage/preferences. Personal Memory supports user-authored notes and an explicit opt-in switch before notes are sent with AI prompts; semantic retrieval, automatic memory proposals and cross-device sync are not implemented.
- The local Quantum Lab is an educational classical independent-product-state model, not quantum hardware or a general entanglement-capable circuit simulator. A separate IBM Quantum REST connector is now in source for backend listing, a confirmation-gated one-qubit sampler job and job status; it is not active until IBM API key and instance CRN are added as Render secrets and tested against an accessible backend.
- Offline Qwen3-0.6B model download, LiteRT-LM initialization, and text fallback are present in source but must pass CI and a real airplane-mode phone test before being described as verified.
- Offline vision is not implemented.
- PDF text extraction and AI analysis are implemented in source for text-based PDFs; current limits are 7 MiB upload, up to 60 pages and 80,000 extracted characters. Scanned/image-only PDFs require OCR, which is not yet integrated.
- A debug APK is not a Play Store-signed release.

## Latest build diagnosis

A recent CI build failed because LiteRT-LM 0.19.0 dependencies contain Kotlin metadata version 2.4.0 while the project was using Kotlin compiler 2.0.21. The project Kotlin Gradle plugins were upgraded to 2.4.20 to resolve this compiler metadata mismatch. The new CI run must complete successfully before the source is considered build-verified. Check the Actions tab in GitHub for the latest run and its logs.

## Resume from a new computer

1. Sign in to the same GitHub account.
2. Open the repository above.
3. Download the latest `main.zip`, or clone the repository:
   `git clone https://github.com/egalego-sudo/AstraQuantum-AI.git`
4. Open the project root (the folder containing `settings.gradle.kts`) in Android Studio.
5. Use JDK 17 and allow Gradle sync to finish.
6. Run the GitHub Actions workflow or build a debug APK with:
   `gradle --no-daemon assembleDebug`
7. Keep Render secrets in Render; do not add them to the downloaded source.
8. Before making changes, pull the latest `main` branch. Commit and push each stable change so it is saved remotely.

## Backup rule

GitHub `main` is the canonical, persistent source backup. The ZIP is a portable snapshot of the branch at the time it is downloaded. For a second independent backup, download the ZIP to a laptop/drive and retain the repository URL. A downloaded ZIP does not include Render's private environment variables, GitHub secrets, model weights downloaded by the app, or any data stored privately on a phone.
