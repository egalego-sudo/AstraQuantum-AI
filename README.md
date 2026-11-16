# ASTRA QUANTUM AI PLUS

Android-first personal AI assistant.

**Creator by Bittu Pandit**

## Implemented in the current Android app

- Kotlin + Jetpack Compose Android application.
- Hindi/English AI chat through the ASTRA backend.
- Groq text model and image-analysis model, configured on the server.
- Voice input and text-to-speech output (device speech services required).
- Image selection and image analysis for JPEG, PNG and WEBP.
- Research shortcut that opens a web search in the browser.
- Local notes/projects and chat history stored in app-private preferences.
- Quantum Lab educational model for up to 32 independent Qubits, probability calculation and classical measurement sampling.
- Backend health-check screen.

## Quantum Lab scope and limits

The 32-Qubit Learning Engine calculates independent product-state probabilities on demand and samples measurements without allocating the full state vector. It is a classical educational simulator, not quantum hardware and not a general entanglement-capable quantum circuit simulator. A dense 32-Qubit complex-double state vector would require about 64 GiB for amplitudes alone.

## Build

The GitHub Actions workflow builds a debug APK using Java 17 and Gradle 8.9. The workflow artifact is a test build, not a signed Play Store release. Device installation and runtime behavior still require a real Android-device test.

## Backend

The Node.js backend is in `backend/`. It uses the Groq API and expects `GROQ_API_KEY` to be configured as a secret in the hosting provider. Optional model settings are `GROQ_MODEL` and `GROQ_VISION_MODEL`. See [backend/README.md](backend/README.md).

## Security

- Never commit API keys, passwords, signing keys, or production secrets.
- Keep provider credentials in server-side environment variables.
- The Android app communicates with the backend over HTTPS.
- The current backend includes a basic per-IP rate limit; public production deployment still needs stronger abuse controls, monitoring and operational review.
