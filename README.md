# ASTRA QUANTUM AI PLUS

Android-first personal AI assistant.

**Creator by Bittu Pandit**

## Implemented in the current Android app

- Kotlin + Jetpack Compose Android application.
- Hindi/English AI chat through the ASTRA backend.
- Groq text model and image-analysis model, configured on the server.
- Voice input and text-to-speech output (device speech services required).
- Image selection and image analysis for JPEG, PNG and WEBP.
- In-app research endpoint retrieves Wikipedia overview pages plus Crossref and OpenAlex scholarly-record metadata/available abstracts, deduplicates overlapping DOI/URL records, then asks Groq to synthesize a source-ID-cited report. Availability and source quality vary by topic; this is not exhaustive web crawling, full-text review, or conclusive fact verification.
- Optional Deep Research mode directly in AI Chat. It uses the existing backend research route and its bounded public-source retrieval (Wikipedia, Crossref, OpenAlex, Europe PMC and arXiv); it does not add a separate paid search provider or change Groq/Gemini credentials. Coverage, abstracts and full text vary; this is not exhaustive web browsing or guaranteed verification.
- PDF text extraction and AI analysis for text-based PDFs (up to 60 pages and 80,000 extracted characters); scanned/image-only PDFs still need OCR.
- Local notes/projects and chat history stored in app-private preferences.
- User-authored personal memory and project notes with an explicit opt-in switch before they are included in AI prompts; memory stays local unless the user enables this. When enabled, the Context Intelligence Engine selects recent turns plus older relevant turns from the saved local chat, preserves key numbers/units within a bounded prompt, and instructs the model not to ask again for facts already present. This is retrieval from the app's local chat history, not a guarantee of perfect recall or a semantic vector database.
- PDF document analysis through the backend using the maintained pdf-parse parser.
- Quantum Lab educational pure-state state-vector simulator for up to 10 Qubits, with H/X/Y/Z/S/T/RX/RY/RZ, CNOT and CZ gates, Bell-state and two-qubit Grover presets, QFT/inverse-QFT and a limited Shor order-finding demonstration for N=15. This is classical simulation, not a physical QPU. The Shor demo applies the modular-function oracle mathematically rather than compiling a full reversible gate-by-gate circuit.
- IBM Quantum hardware connector code with server-side IAM token exchange, backend listing, confirmation-gated one-qubit sampler submission and job-status lookup; it requires IBM API key and instance CRN secrets before it can connect to a real QPU.
- Backend health-check screen.

## Five Core architecture

The app is organized around five user-facing capability domains:

1. **Perception Core** — voice input, image understanding through the online Vision endpoint, and text-based PDF extraction/analysis. Offline image understanding and guaranteed offline speech are not implemented.
2. **Reasoning Core** — AI conversation and problem analysis through the configured Groq model; a small on-device text model is available as a separate offline option when installed.
3. **Knowledge Core** — local user-authored memory with opt-in prompt sharing, plus an initial Wikipedia/Crossref research workflow. Retrieved references are not independently or conclusively verified.
4. **Action Core** — includes an AI-assisted task planner that creates a structured, reviewable plan and saves it to the local Projects checkpoint only after explicit confirmation. It does not yet execute actions in external apps or modify device settings; consequential actions must require explicit user review and confirmation.
5. **Quantum Learning Core** — classical educational probability/measurement simulation for independent product-state qubits, plus an optional IBM Quantum connector. The simulator is not a general quantum computer or entanglement-capable circuit simulator.

The **Cores** screen is an architecture dashboard that routes each domain to its current working module and states important capability limits. It does not imply that planned or device-dependent features are already complete.

## Five Intelligence capabilities

These capabilities work across the five Cores and have explicit implementation boundaries:

1. **Evidence Intelligence** — Deep Research retrieves from Wikipedia, Crossref and OpenAlex where available, removes duplicate DOI/URL records, and instructs the model to separate supported claims, actual source disagreements, uncertainty/limitations and a qualitative confidence label. Citations are tied to retrieved source IDs. This is evidence-aware synthesis, not independent proof, full-text review or exhaustive fact-checking. The backend also returns the retrieved source count, types and limitations.
2. **Long-Term Personal Intelligence** — with the user's opt-in enabled, the app can include locally saved Memory, project/checkpoint notes and a limited recent chat window in the next AI request. This is local app history/context, not automatic access to all separate ChatGPT conversations or a semantic memory database. Context is not sent when the opt-in is off.
3. **Action Intelligence** — the app can generate a structured task plan and, after the user confirms, save it as a local project checkpoint. Connected external app actions and autonomous task completion are not yet implemented. Any future consequential action must have a preview and explicit user confirmation.
4. **Independent Offline Intelligence** — the Qwen3-0.6B on-device text model can serve as a text-chat fallback when the remote service fails, after the model is downloaded and initialized. Offline vision, offline document reasoning and guaranteed offline speech are not implemented.
5. **Multimodal Intelligence** — voice transcription can feed the chat prompt; a selected image can be analyzed with its prompt by the online Vision model; text-based PDFs can be extracted and analyzed. A single continuous session combining multiple images, audio files and documents is not yet implemented. OCR for scanned PDFs is also not implemented.

The app's Cores dashboard describes these boundaries instead of presenting planned features as completed functionality.

## Quantum Lab scope and limits

The current Quantum Lab uses a dense pure-state state-vector simulator for 1–10 Qubits (up to 1,024 complex amplitudes). It supports single-qubit H/X/Y/Z/S/T/RX/RY/RZ gates and two-qubit CNOT/CZ operations, plus Bell-state and two-qubit Grover demonstrations, QFT/inverse-QFT, and an educational Shor order-finding demonstration for N=15. These are classical calculations, not execution on quantum hardware. The Shor demonstration applies its modular-function oracle mathematically and is not a full reversible gate-by-gate Shor circuit. A separate IBM Quantum REST connector is present but requires server-side credentials, explicit submission enablement, and a verified IBM job before a real QPU connection can be claimed.

## Build

The GitHub Actions workflow builds a debug APK using Java 17 and Gradle 8.9. The workflow artifact is a test build, not a signed Play Store release. Device installation and runtime behavior still require a real Android-device test.

## Backend

The Node.js backend is in `backend/`. It uses the Groq API and expects `GROQ_API_KEY` to be configured as a secret in the hosting provider. Optional model settings are `GROQ_MODEL` and `GROQ_VISION_MODEL`. See [backend/README.md](backend/README.md).

## Security

- Never commit API keys, passwords, signing keys, or production secrets.
- Keep provider credentials in server-side environment variables.
- The Android app communicates with the backend over HTTPS.
- The current backend includes a basic per-IP rate limit; public production deployment still needs stronger abuse controls, monitoring and operational review.
