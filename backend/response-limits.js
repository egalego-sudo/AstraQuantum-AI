/** Read an upstream response with a hard decoded-byte ceiling. */
export async function readLimitedResponseText(response, maxBytes) {
  if (!Number.isSafeInteger(maxBytes) || maxBytes < 1) {
    throw new TypeError("maxBytes must be a positive safe integer.");
  }

  const declaredLength = Number(response?.headers?.get?.("content-length"));
  if (Number.isFinite(declaredLength) && declaredLength > maxBytes) {
    try { await response.body?.cancel(); } catch {}
    throw new Error("Upstream response exceeds the configured size limit.");
  }

  if (!response?.body) return "";
  const reader = response.body.getReader();
  const chunks = [];
  let totalBytes = 0;
  try {
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      if (!value) continue;
      totalBytes += value.byteLength;
      if (totalBytes > maxBytes) {
        try { await reader.cancel(); } catch {}
        throw new Error("Upstream response exceeds the configured size limit.");
      }
      chunks.push(Buffer.from(value));
    }
  } finally {
    try { reader.releaseLock(); } catch {}
  }
  return Buffer.concat(chunks, totalBytes).toString("utf8");
}

export async function readLimitedResponseJson(response, maxBytes) {
  return JSON.parse(await readLimitedResponseText(response, maxBytes));
}
