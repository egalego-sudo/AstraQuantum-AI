# ASTRA QUANTUM AI PLUS backend

Node.js HTTP service that keeps the Groq API key on the server, never in the Android APK.

## Requirements and configuration

- Node.js 20.16 or newer (required by the pinned pdf-parse 2.x package).
- Set `GROQ_API_KEY` in the hosting provider's secret/environment settings.
- Optional:
  - `GROQ_MODEL` (default: `openai/gpt-oss-20b`)
  - `GROQ_VISION_MODEL` (default: `qwen/qwen3.8-27b`)
  - `PORT` (default: `8080`)
  - `ALLOWED_ORIGIN` (default: `*`; restrict this when your deployment needs it)

Do not commit a real API key or a local `.env` file. Keep both Groq and IBM credentials only in Render's private Environment settings.

## Run locally

```sh
npm install
GROQ_API_KEY=your_key_here npm start
```

## Health check

- `GET /health` returns the provider and configured model identifiers. It does not expose the API key.

## Generate API

- `POST /v1/generate`
- JSON body: `{"prompt":"Your question"}`
- For image analysis, also provide `mimeType` and base64-encoded `base64` fields.
- Supported image MIME types: `image/jpeg`, `image/png`, `image/webp`.
- PDF MIME type `application/pdf` is accepted for text extraction (up to 60 pages and 80,000 extracted characters); scanned PDFs need OCR and are not supported yet.
- Response: `{"text":"..."}`

The `/v1/analyze` route is also accepted by the server and uses the same request handler.

## Research API

- `POST /v1/research`
- JSON body: `{"query":"research topic"}`
- Searches five public knowledge/scholarly sources in parallel: Europe PMC, arXiv, OpenAlex, Crossref and Wikipedia.
- Deduplicates indexed records and supplies up to 24 records to the synthesis stage.
- Attempts full-text retrieval for up to three Europe PMC Open Access records and up to two arXiv HTML records, subject to availability and timeouts. A failed or unavailable full-text fetch remains labeled as abstract/metadata only.
- The report asks for claim-level evidence classification: `SUPPORTED`, `PARTLY SUPPORTED`, `CONTRADICTED`, or `UNVERIFIED`, with source IDs and evidence level. This is model-assisted classification, not independent peer review or a guarantee of correctness.
- Response fields include `text`, `sources`, and `research_meta`. Metadata reports source counts/types, which source types returned full text, coverage limitations, and how many records include an abstract or full text.
- The Android client allows up to 150 seconds for this research request.
- This is a bounded indexed-source search, not exhaustive internet search or a guarantee that every source, paper, or claim has been found. arXiv records are preprints unless their publication status is independently confirmed; Wikipedia is a tertiary source. Users should inspect primary sources and cited evidence.

## IBM Quantum hardware connector

- Server routes: `GET /v1/quantum/backends`, `POST /v1/quantum/demo`, and `GET /v1/quantum/jobs/{id}`.
- Configure `IBM_QUANTUM_API_KEY` and `IBM_QUANTUM_SERVICE_CRN` as Render secrets/environment values. Optional `IBM_QUANTUM_BASE_URL` supports `https://quantum.cloud.ibm.com` or `https://eu-de.quantum.cloud.ibm.com`.
- The backend exchanges the API key for a short-lived IAM bearer token and keeps the token only in server memory. The Android app never receives the IBM API key.
- The demo submits a fixed one-qubit sampler circuit to the selected backend only after the user confirms in the app. IBM instance access, available backends, usage allocation and queue availability are controlled by IBM.
- Until those credentials and a valid instance CRN are configured, this is connector code only—not an active QPU connection. Never paste IBM credentials into chat or source files.

## Limits and deployment notes

- Prompt length: 100,000 characters (research evidence is included in the prompt).
- Encoded file limit: 10 MiB in the backend; the Android app currently applies a smaller 7 MiB source-file limit.
- Basic rate limit: 20 requests per IP per minute.
- Apply provider-level access controls, stronger abuse protection, monitoring and operational review before public production use.


### IBM Quantum job submission safety

IBM Quantum hardware job submission is disabled by default. The backend may list available backends when IBM credentials are configured, but it will reject actual job submissions unless the operator explicitly sets `IBM_QUANTUM_SUBMISSIONS_ENABLED=true` in the server environment. Enable this only after reviewing the selected IBM Quantum plan, instance allocation, and usage quota. Keep `IBM_QUANTUM_API_KEY` and `IBM_QUANTUM_SERVICE_CRN` server-side; never place them in the Android app or source repository.
