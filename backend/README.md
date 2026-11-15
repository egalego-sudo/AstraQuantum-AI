# ASTRA QUANTUM AI PLUS backend

This small Node.js service keeps the Gemini API key on the server, never in the Android APK.

## Run
- Use Node.js 20 or newer.
- Set `GEMINI_API_KEY` in the hosting provider's secret/environment settings.
- Optional: set `GEMINI_MODEL`, `PORT`, and `ALLOWED_ORIGIN`.
- Run `npm start`.
- Health check: `GET /health`.
- Android app setting: set the service base URL (HTTPS) in Settings.

## API
- `POST /v1/generate` JSON: `{"prompt":"..."}`
- `POST /v1/analyze` JSON: `{"prompt":"...","mimeType":"image/jpeg","base64":"..."}`
- Response: `{"text":"..."}`

The server accepts PDF, JPEG, PNG and WEBP inline files, with a 10 MB encoded-size limit. Apply provider-level access controls and monitoring before public deployment. Do not commit a real API key or an `.env` file.
