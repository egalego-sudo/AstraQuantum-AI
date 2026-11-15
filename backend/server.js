import http from "node:http";

const PORT = Number(process.env.PORT || 8080);
const API_KEY = process.env.OPENAI_API_KEY;
const MODEL = process.env.OPENAI_MODEL || "gpt-5.4-mini";
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
  if (!API_KEY) throw new Error("Server is missing OPENAI_API_KEY secret.");
  const prompt = String(input.prompt || "").trim();
  if (!prompt || prompt.length > 12000) throw new Error("Prompt is empty or too long.");

  const content = [];
  if (input.base64 && input.mimeType) {
    const mimeType = String(input.mimeType).toLowerCase();
    const base64 = String(input.base64).replace(/^data:[^;]+;base64,/, "");
    if (base64.length > 10 * 1024 * 1024) throw new Error("File is too large.");
    if (mimeType === "application/pdf") {
      content.push({
        type: "input_file",
        filename: "upload.pdf",
        file_data: `data:application/pdf;base64,${base64}`
      });
    } else if (["image/jpeg", "image/png", "image/webp"].includes(mimeType)) {
      content.push({
        type: "input_image",
        image_url: `data:${mimeType};base64,${base64}`,
        detail: "auto"
      });
    } else {
      throw new Error("Only PDF, JPEG, PNG and WEBP files are supported.");
    }
  }
  content.push({ type: "input_text", text: prompt });

  const response = await fetch("https://api.openai.com/v1/responses", {
    method: "POST",
    headers: {
      "content-type": "application/json",
      "authorization": `Bearer ${API_KEY}`
    },
    body: JSON.stringify({
      model: MODEL,
      input: [{ role: "user", content }],
      max_output_tokens: 2048,
      store: false
    }),
    signal: AbortSignal.timeout(60_000)
  });
  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    const message = data?.error?.message || `OpenAI API returned HTTP ${response.status}`;
    throw new Error(message);
  }
  const text = data.output
    ?.filter(item => item.type === "message")
    .flatMap(item => item.content || [])
    .filter(item => item.type === "output_text")
    .map(item => item.text || "")
    .join("")
    .trim();
  if (!text) throw new Error("OpenAI returned no text. Check API access, model availability and billing.");
  return text;
}

const server = http.createServer(async (req, res) => {
  if (req.method === "OPTIONS") return send(res, 204, {});
  if (req.method === "GET" && req.url === "/health") {
    return send(res, 200, { ok: true, service: "ASTRA AI backend", provider: "OpenAI", model: MODEL });
  }
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
    console.error("ASTRA request failed:", message);
    return send(res, status, { error: message });
  }
});

server.listen(PORT, "0.0.0.0", () => {
  console.log(`ASTRA backend listening on port ${PORT}; provider=OpenAI; model=${MODEL}`);
});
