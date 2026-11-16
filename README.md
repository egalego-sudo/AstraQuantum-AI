# ASTRA QUANTUM AI PLUS

Android-first personal AI assistant.

**Creator by Bittu Pandit**

## Implemented in the current Android app

- Kotlin + Jetpack Compose Android application.
- Hindi/English AI chat through the ASTRA backend.
- Groq text model and image-analysis model, configured on the server.
- Voice input and text-to-speech output (device speech services required).
- Image selection and image analysis for JPEG, PNG and WEBP.
- In-app research endpoint retrieves Wikipedia overview pages and Crossref scholarly-record metadata, then asks Groq to synthesize a cited report. This is an initial multi-source research layer, not exhaustive web crawling or conclusive fact verification.
- PDF text extraction and AI analysis for text-based PDFs (up to 60 pages and 80,000 extracted characters); scanned/image-only PDFs still need OCR.
- Local notes/projects and chat history stored in app-private preferences.
- PDF document analysis through the backend using the maintained pdf-parse parser.
- Quantum Lab educational model for up to 32 independent Qubits, probability calculation and classical measurement sampling.
- IBM Quantum hardware connector code with server-side IAM token exchange, backend listing, confirmation-gated one-qubit sampler submission and job-status lookup; it requires IBM API key and instance CRN secrets before it can connect to a real QPU.
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
