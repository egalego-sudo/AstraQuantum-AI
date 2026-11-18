# QUANTUM AI — Hybrid Intelligence Architecture

**Creator by Bittu Pandit**

## Product requirement

QUANTUM AI should remain useful when the network is unavailable. It must distinguish on-device work from remote services and must never claim that an unavailable online service was used.

## Operating modes

### Online mode
- Android client connects to the configured QUANTUM AI HTTPS backend.
- The backend uses configured Groq text and vision models.
- Research retrieval is bounded to supported public indexes and endpoints; it is not exhaustive browser search.
- Provider credentials remain on the backend and must never be embedded in the APK.

### Offline mode
- A quantized on-device language model can run locally after its model files have been downloaded and initialized.
- Local chat history, saved notes/projects and supported on-device tools remain available subject to Android app storage.
- Offline image understanding is not implemented.
- Live web research, cloud models, current facts and remote sync are unavailable without connectivity. Cached material must be labelled with its saved time.

## Hybrid routing requirements

1. Check backend reachability with bounded timeouts; network connectivity alone does not prove the backend is reachable.
2. Classify each request as live research, image understanding, a local tool, or general language generation.
3. Use the on-device model only when it is installed, initialized and suitable for the request.
4. Use the remote backend only when online and permitted.
5. If a remote request fails, fall back to the local model when it is ready; otherwise explain the limitation.
6. Label the answer source accurately (for example, `ON-DEVICE`, `ONLINE · GROQ`, or `CACHED`).
7. Queue only safe, idempotent tasks for later sync. Never automatically replay payments, messages or other consequential actions.

## On-device model

- Runtime: Google LiteRT-LM Android Kotlin API, pinned to 0.19.0 in the current integration.
- Model: `litert-community/Qwen3-0.6B`, dynamic INT4 artifact `Qwen3-0.6B_dynamic_wi4b32_afp32.litertlm`.
- The model is downloaded on demand into app-private storage; it is not bundled in the APK.
- The current integration is text-only. Offline image understanding is not included.
- Model download, initialization, memory use, latency and inference must be tested on the target phone.

## Security and privacy

- Keep Groq and IBM credentials on the backend.
- Keep personal memory user-controllable and disclose when it is sent to the backend.
- Do not upload local memories or files unless the user initiates an online action and the UI makes that clear.
- Use HTTPS for remote traffic.
- Do not store secrets in logs or source files.

## Acceptance tests

- Airplane mode: the app opens and local data remains accessible; offline inference works only after model installation and must be tested.
- Backend unavailable: the app falls back locally when possible and never falsely labels a response as a Groq answer.
- Online: remote requests are labelled as remote.
- Stale cache: cached content shows its timestamp and is not described as current.
- Secrets: no provider API key appears in Android source, APK resources or logs.

## Current implementation boundary

The repository contains a LiteRT-LM Qwen3-0.6B downloader, initialization path, Offline AI screen and text-chat fallback. These source components do not by themselves prove offline operation: a successful CI build and a real airplane-mode inference test are required. Remote Groq vision remains online-only. The Quantum Lab is an educational classical simulation; it is not a general-purpose quantum computer or a physical QPU.
