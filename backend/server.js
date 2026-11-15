import http from "node:http";

const PORT = Number(process.env.PORT || 8080);
const API_KEY = process.env.GEMINI_API_KEY;
const MODEL = process.env.GEMINI_MODEL || "gemini-3.8-flash";
const ORIGIN = process.env.ALLOWED_ORIGIN || "*";
const WINDOW_MS = 60_000;
const MAX_REQUESTS = 20;
const hits = new Map();

function send(res, status, data) {
  res.writeHead(status, {
    "content-type": "application/json; charset=utf-8",
    "access-control-allow-origin": ORIGIN,
    "access-control-allow-methods": "POST, GET, OPTIONS",
    "access-control-allow-headers": "content-type",
    "cache-control": "no-store"
  });
  res.end(JSON.stringify(data));
}

function readBody(req) {
  return new Promise((resolve, reject) => {
    let body = "";
    req.on("data", chunk => {
      body += chunk;
      if (body.length > 12 * 1024 * 1024) {
        reject(new Error("Request too large"));
        req.destroy();
      }
    });
    req.on("end", () => {
      try { resolve(JSON.parse(body || "{}")); }
      catch { reject(new Error("Invalid JSON")); }
    });
    req.on("error", reject);
  });
}

async function generate(input) {
  if (!API_KEY) throw new Error("Server is missing GEMINI_API_KEY secret.");
  const prompt = String(input.prompt || "").trim();
  if (!prompt || prompt.length > 12000) throw new Error("Prompt is empty or too long.");
  const parts = [];
  if (input.base64 && input.mimeType) {
    const mimeType = String(input.mimeType);
    if (!["application/pdf", "image/jpeg", "image/png", "image/webp"].includes(mimeType)) {
      throw new Error("Only PDF, JPEG, PNG and WEBP files are supported.");
    }
    if (String(input.base64).length > 10 * 1024 * 1024) throw new Error("File is too large.");
    parts.push({ inline_data: { mime_type: mimeType, data: String(input.base64) } });
  }
  parts.push({ text: prompt });
  const response = await fetch(
    `https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(MODEL)}:generateContent`,
    {
      method: "POST",
      headers: { "content-type": "application/json", "x-goog-api-key": API_KEY },
      body: JSON.stringify({ contents: [{ role: "user", parts }] }),
      signal: AbortSignal.timeout(45_000)
    }
  );
  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    const message = data?.error?.message || `Gemini API returned HTTP ${response.status}`;
    throw new Error(message);
  }
  const text = data?.candidates?.[0]?.content?.parts?.map(p => p.text || "").join("").trim();
  if (!text) throw new Error("Gemini returned no text. Check model access and quota.");
  return text;
}

const server = http.createServer(async (req, res) => {
  if (req.method === "OPTIONS") return send(res, 204, {});
  if (req.method === "GET" && req.url === "/health") return send(res, 200, { ok: true, service: "ASTRA AI backend" });
  if (req.method !== "POST" || !["/v1/generate", "/v1/analyze"].includes(req.url || "")) {
    return send(res, 404, { error: "Not found" });
  }
  const ip = req.socket.remoteAddress || "unknown";
  const now = Date.now();
  const recent = (hits.get(ip) || []).filter(t => now - t < WINDOW_MS);
  if (recent.length >= MAX_REQUESTS) return send(res, 429, { error: "Rate limit reached. Try again in a minute." });
  recent.push(now); hits.set(ip, recent);
  try {
    const input = await readBody(req);
    const text = await generate(input);
    return send(res, 200, { text });
  } catch (error) {
    const message = error?.message || "Internal server error";
    const status = message === "Request too large" ? 413 :
      message === "Invalid JSON" ? 400 :
      message.startsWith("Prompt") || message.startsWith("Only ") || message.startsWith("File ") ? 400 : 502;
    return send(res, status, { error: message });
  }
});

server.listen(PORT, "0.0.0.0", () => {
  console.log(`ASTRA backend listening on port ${PORT}`);
});
