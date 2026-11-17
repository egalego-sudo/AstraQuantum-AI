const JSON_OVERHEAD_BYTES = 512;

export function truncateUtf8(value, maxBytes) {
  const text = String(value ?? "");
  const limit = Math.max(0, Number(maxBytes) || 0);
  const bytes = Buffer.from(text, "utf8");
  if (bytes.length <= limit) return text;
  if (limit === 0) return "";
  let clipped = bytes.subarray(0, limit).toString("utf8");
  if (clipped.endsWith("\uFFFD")) clipped = clipped.slice(0, -1);
  return clipped;
}

/**
 * Bound text sent to a model. The byte ceiling is intentionally conservative:
 * UTF-8 bytes are used as an upper-bound proxy, not claimed as an exact tokenizer.
 * Non-text multimodal parts are retained unchanged.
 */
export function fitContentToByteBudget(content, instructionBytes, maxInputBytes) {
  const available = Math.max(0,
    (Number(maxInputBytes) || 0) - (Number(instructionBytes) || 0) - JSON_OVERHEAD_BYTES);
  let remaining = available;
  return (Array.isArray(content) ? content : []).map(part => {
    if (!part || typeof part !== "object" || typeof part.text !== "string") return part;
    const text = truncateUtf8(part.text, remaining);
    remaining = Math.max(0, remaining - Buffer.byteLength(text, "utf8"));
    return { ...part, text };
  });
}

export function estimateInputBytes(instructions, content) {
  const textBytes = (Array.isArray(content) ? content : []).reduce((sum, part) =>
    sum + (typeof part?.text === "string" ? Buffer.byteLength(part.text, "utf8") : 0), 0);
  return Buffer.byteLength(String(instructions || ""), "utf8") + textBytes + JSON_OVERHEAD_BYTES;
}
