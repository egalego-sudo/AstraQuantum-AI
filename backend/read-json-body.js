import { StringDecoder } from "node:string_decoder";

const DEFAULT_MAX_BYTES = 12 * 1024 * 1024;

/**
 * Parses a bounded UTF-8 JSON request body without corrupting multi-byte
 * characters when the HTTP stream splits a code point across chunks.
 */
export function readJsonBody(req, { maxBytes = DEFAULT_MAX_BYTES } = {}) {
  if (!Number.isSafeInteger(maxBytes) || maxBytes < 1) {
    return Promise.reject(new RangeError("maxBytes must be a positive safe integer."));
  }

  return new Promise((resolve, reject) => {
    const decoder = new StringDecoder("utf8");
    let body = "";
    let bodyBytes = 0;
    let settled = false;

    const fail = error => {
      if (settled) return;
      settled = true;
      reject(error);
    };

    req.on("data", chunk => {
      if (settled) return;
      const buffer = Buffer.isBuffer(chunk) ? chunk : Buffer.from(chunk);
      bodyBytes += buffer.length;
      if (bodyBytes > maxBytes) {
        fail(new Error("Request too large"));
        req.destroy();
        return;
      }
      body += decoder.write(buffer);
    });

    req.on("end", () => {
      if (settled) return;
      body += decoder.end();
      try {
        resolve(JSON.parse(body || "{}"));
      } catch {
        reject(new Error("Invalid JSON"));
      }
    });

    req.on("error", fail);
  });
}
