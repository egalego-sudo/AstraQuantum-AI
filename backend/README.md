# ASTRA QUANTUM AI PLUS backend

Node.js HTTP service that keeps the Groq API key on the server, never in the Android APK.

## Requirements and configuration

- Node.js 20 or newer.
- Set `GROQ_API_KEY` in the hosting provider's secret/environment settings.
- Optional:
  - `GROQ_MODEL` (default: `openai/gpt-oss-20b`)
  - `GROQ_VISION_MODEL` (default: `qwen/qwen3.8-27b`)
  - `PORT` (default: `8080`)
  - `ALLOWED_ORIGIN` (default: `*`; restrict this when your deployment needs it)

Do not commit a real API key or a local `.env` file.

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
- Response: `{"text":"..."}`

The `/v1/analyze` route is also accepted by the server and uses the same request handler. PDF analysis is not implemented on the Groq path; the Android file picker therefore offers image formats only.

## Limits and deployment notes

- Prompt length: 12,000 characters.
- Encoded file limit: 10 MiB in the backend; the Android app currently applies a smaller 7 MiB source-file limit.
- Basic rate limit: 20 requests per IP per minute.
- Apply provider-level access controls, stronger abuse protection, monitoring and operational review before public production use.
