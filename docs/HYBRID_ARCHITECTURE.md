# ASTRA QUANTUM AI PLUS — Hybrid Intelligence Architecture

**Creator by Bittu Pandit**

## Product requirement

ASTRA must remain useful when the network is unavailable. It must automatically select between on-device capabilities and remote services, clearly disclose which mode produced an answer, and never pretend that an unavailable online service was used.

## Operating modes

### Online mode
- Existing Android client -> ASTRA HTTPS backend -> Groq models.
- Internet research is performed only through an explicitly configured/search-enabled provider.
- Image analysis can use the existing backend vision endpoint.
- Server secrets remain on the backend; never embed provider API keys in the APK.

### Offline mode
- A quantized, on-device language model runs locally after its model files have been downloaded.
- Local chat, local conversation history, saved notes/projects, and supported on-device tools remain available.
- Offline image understanding is a separate capability and must be reported unavailable until a compatible on-device vision model is integrated.
- Live web search, cloud models, current facts, and remote sync are unavailable without connectivity. Cached results must be labelled with their saved time.

## Hybrid router

1. Check network reachability with bounded timeouts; network presence alone does not prove the backend is reachable.
2. For each request, classify whether it needs live web access, image understanding, a local tool, or general language generation.
3. Prefer the on-device model for privacy-sensitive or offline-capable requests when available.
4. Use the remote backend for requests that need stronger reasoning or live services when online and permitted.
5. If a remote request fails, fall back to the local model when installed; otherwise provide a useful local tool response or explain the limitation.
6. Label each answer ON-DEVICE, ONLINE · GROQ, or CACHED; never silently present cached information as live.
7. Queue only safe, idempotent tasks for later sync. Do not automatically replay payments, messages, or other consequential actions.

## First on-device model selection

- Runtime: Google LiteRT-LM Android Kotlin API, pinned to 0.19.0 for the initial integration.
- Model: litert-community/Qwen3-0.6B, dynamic INT4 artifact Qwen3-0.6B_dynamic_wi4b32_afp32.litertlm.
- Approximate download: 328 MiB; model license: Apache 2.0.
- Model is downloaded on demand into app-private storage and is not bundled in the APK.
- Initial integration uses GPU with CPU fallback. The model card reports around 1.9–2.1 GB peak process footprint on tested phones for the newer export; actual performance and memory use must be verified on the target device.
- The initial offline model handles text only. Offline image understanding is not included in this first integration.

## Local model integration gates

- Choose an Android-compatible runtime after checking its current supported Android versions, model format, license, ABI support, and maintenance status.
- Use a small quantized model first; model size and memory requirements must be measured on target devices.
- Store model files in app-specific storage, show download size/progress, verify checksums, and allow deletion.
- Keep inference off the main UI thread; provide cancellation and memory-pressure handling.
- Do not promise a model is available offline until its files are present and a real inference test passes.

## Security and privacy
- Keep Groq/API credentials on the backend only.
- Make local memory user-controllable and explain that app data removal/uninstall may erase it.
- Do not upload local memories or files unless the user initiates an online action and the UI makes that clear.
- Use HTTPS for remote traffic.
- Keep queued work encrypted where practical and avoid storing secrets in logs.

## Delivery phases
1. Foundation: document mode states, capability routing, explicit answer provenance, and network/backend failure handling.
2. Offline language model: integrate a supported on-device runtime and a downloadable quantized model; verify real offline inference.
3. Offline tools and memory: local calculations, notes, saved context, and robust local chat persistence.
4. Multimodal offline: add on-device speech and image understanding only after device/runtime compatibility tests.
5. Sync and evaluation: safe task queue, conflict handling, latency/memory/battery tests, and online/offline regression suite.

## Acceptance tests
- Airplane mode: app opens, saved projects and chat history remain accessible, and local inference works after model installation.
- No backend: router falls back locally without freezing or falsely claiming Groq was used.
- Online: Groq requests still work and are labelled as remote.
- Stale cache: cached content displays its timestamp and is not described as current.
- Secrets: no provider API key appears in the Android source, APK resources, or logs.

## Current implementation boundary
The current source now includes a LiteRT-LM on-device Qwen3-0.6B model downloader, GPU/CPU engine initialization, an Offline AI screen, and text-chat fallback when the remote backend fails and the local model is installed. The source change still requires a successful CI build and a real airplane-mode inference test before offline LLM capability can be called verified. Remote Groq vision remains online-only; offline image understanding is not implemented. The quantum module remains an educational classical product-state model, not a general quantum circuit simulator or real quantum hardware integration.