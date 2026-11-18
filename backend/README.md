# Quantum AI backend

Node.js HTTP service that keeps the Groq API key on the server, never in the Android APK.

## Requirements and configuration

- Node.js 20.16 or newer (required by the pinned pdf-parse 2.x package).
- Set `GROQ_API_KEY` in the hosting provider's secret/environment settings.
- Set `APP_ACCESS_TOKEN` as a separate private secret: exactly 64 lowercase hexadecimal characters (generate with `openssl rand -hex 32`). Enter the same value in QUANTUM AI Settings. It is not the Groq API key.
- Optional:
  - `GROQ_MODEL` (default: `openai/gpt-oss-20b`)
  - `GROQ_VISION_MODEL` (default: `qwen/qwen3.8-27b`)
  - `PORT` (default: `8080`)
  - `API_MAX_REQUESTS_PER_MINUTE` (default: `20`)
  - `ALLOWED_ORIGINS` (comma-separated exact origin allowlist for browser CORS; unset means no browser origin is allowed. Native Android requests do not rely on browser CORS.)

Do not commit Groq keys, app access tokens, IBM credentials, or a local `.env` file. Keep all secret values only in Render's private Environment settings.

## Run locally

```sh
npm install
export GROQ_API_KEY="your Groq key"
export APP_ACCESS_TOKEN="$(openssl rand -hex 32)"
npm start
```

Copy that generated `APP_ACCESS_TOKEN` into QUANTUM AI Settings. For Render, store the same 64-character value as a private environment secret; never commit it.

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
- Searches six public knowledge/scholarly sources in parallel: OpenAlex, Crossref, Europe PMC, arXiv, Semantic Scholar and Wikipedia. Semantic Scholar is a metadata/abstract index and may rate-limit unauthenticated requests.
- Deduplicates and ranks indexed records; up to 24 records are returned to the client, while the compact synthesis stage uses at most the top 8 evidence records.
- Attempts full-text retrieval for up to three Europe PMC Open Access records and up to two arXiv HTML records, subject to availability and timeouts. A failed or unavailable full-text fetch remains labeled as abstract/metadata only.
- Up to 12 DOI-bearing results are independently looked up in Crossref; the returned title is compared with the indexed title. The API returns `doi_verified`, `doi_verification`, `crossref_title`, and title similarity per source. Missing DOI records, lookup failures, missing titles, and title mismatches have distinct statuses. A title match verifies only the Crossref DOI record/title match; it does not prove that every citation field or the paper's claims are correct.
- Research runs in explicit phases: source discovery/ranking, Crossref DOI record and title verification, bounded open-access full-text retrieval, then compact evidence synthesis. The pipeline metadata lists these phases and DOI status counts. The report asks for claim-level evidence classification: `SUPPORTED`, `PARTLY SUPPORTED`, `CONTRADICTED`, or `UNVERIFIED`, with source IDs and evidence level. This is model-assisted classification, not independent peer review or a guarantee of correctness.
- Response fields include `text`, `sources`, and `research_meta`. Metadata reports source counts/types, which source types returned full text, coverage limitations, and how many records include an abstract or full text.
- The Android client allows up to 150 seconds for this research request.
- This is a bounded indexed-source search, not exhaustive internet search or a guarantee that every source, paper, or claim has been found. arXiv records are preprints unless their publication status is independently confirmed; Wikipedia is a tertiary source. Users should inspect primary sources and cited evidence.

## Cost-conscious research behavior

- The current Deep Research route uses public endpoints from Wikipedia, Crossref, OpenAlex, Europe PMC, arXiv and Semantic Scholar, performs bounded Crossref DOI/title checks, then uses the already-configured Groq model to synthesize the retrieved records. No additional search-provider API key is required by this route.
- This is bounded source retrieval, not general-purpose browser automation. Public endpoints have their own availability, terms and rate limits; Groq inference remains subject to the user's existing Groq plan, quota and any applicable charges.
- Groq's separate built-in Browser Search and Code Interpreter tools are not enabled by default in this project. Their current pricing/allowances must be confirmed in the Groq account before enabling them; do not assume these tools are free.

## IBM Quantum hardware connector

- Server routes: `GET /v1/quantum/backends`, `POST /v1/quantum/demo`, and `GET /v1/quantum/jobs/{id}`.
- Configure `IBM_QUANTUM_API_KEY` and `IBM_QUANTUM_SERVICE_CRN` as Render secrets/environment values. Optional `IBM_QUANTUM_BASE_URL` supports `https://quantum.cloud.ibm.com` or `https://eu-de.quantum.cloud.ibm.com`.
- The backend exchanges the API key for a short-lived IAM bearer token and keeps the token only in server memory. The Android app never receives the IBM API key.
- The demo submits a fixed one-qubit sampler circuit to the selected backend only after the user confirms in the app. IBM instance access, available backends, usage allocation and queue availability are controlled by IBM.
- Until those credentials and a valid instance CRN are configured, this is connector code only—not an active QPU connection. Never paste IBM credentials into chat or source files.

## Limits and deployment notes

- Groq request bounds: input content is limited by `GROQ_MAX_INPUT_BYTES` (default 7,000 bytes; clamped to 3,000–7,000), with the system instructions accounted for separately by the budget helper. This is a UTF-8 byte limit, not a tokenizer-based token count. Requested output defaults to 4,096 tokens and is capped at 4,096. A Groq HTTP 413 is not retried with the same payload: the backend compacts the input once and lowers that retry's output cap to at most 800 tokens. If the provider explicitly reports an incomplete response due to `max_output_tokens`, the backend may make at most two bounded continuation calls, each capped at 1,200 tokens. `GROQ_TPM_LIMIT` (default 8,000) is currently reported as budget metadata/diagnostic context; it is not an enforced tokenizer-based TPM limiter.
- Optional server overrides: `GROQ_TPM_LIMIT` (diagnostic/metadata value, not a hard TPM enforcement control) and `GROQ_MAX_INPUT_BYTES` (clamped to 3,000–7,000 bytes).
- Encoded file limit: 10 MiB in the backend; the Android app currently applies a smaller 7 MiB source-file limit.
- Basic rate limit: 20 requests per IP per minute (configurable with `API_MAX_REQUESTS_PER_MINUTE`).
- Upstream research/provider response bodies are limited to 2 MiB before JSON parsing or text processing.
- Apply provider-level access controls, stronger abuse protection, monitoring and operational review before public production use.


### IBM Quantum job submission safety

IBM Quantum hardware job submission is disabled by default. The backend may list available backends when IBM credentials are configured, but it will reject actual job submissions unless the operator explicitly sets `IBM_QUANTUM_SUBMISSIONS_ENABLED=true` in the server environment. Enable this only after reviewing the selected IBM Quantum plan, instance allocation, and usage quota. Keep `IBM_QUANTUM_API_KEY` and `IBM_QUANTUM_SERVICE_CRN` server-side; never place them in the Android app or source repository.
