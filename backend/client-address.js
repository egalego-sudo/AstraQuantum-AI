// Resolve the client address from Render's trusted proxy chain.
// Render appends the address of the connecting client to X-Forwarded-For;
// choosing the right-most value avoids trusting a caller-supplied left-most IP.
export function clientAddress(req) {
  const chain = String(req.headers?.["x-forwarded-for"] || "")
    .split(",")
    .map(value => value.trim())
    .filter(Boolean);
  return chain.length ? chain[chain.length - 1] : (req.socket?.remoteAddress || "unknown");
}
