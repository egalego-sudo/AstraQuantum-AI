import { isIP } from "node:net";

// Render routes public traffic through Cloudflare. Prefer Cloudflare's
// visitor-IP header, which is set at the edge, over X-Forwarded-For.
// X-Forwarded-For can contain caller-supplied values and must not be trusted
// as an IP-based security boundary.
export function clientAddress(req) {
  const cloudflareIp = String(req.headers?.["cf-connecting-ip"] || "").trim();
  if (isIP(cloudflareIp)) return cloudflareIp;

  // Local/dev fallback only. Do not fall back to X-Forwarded-For: clients
  // can supply that header themselves, which can bypass per-IP rate limits.
  const socketIp = String(req.socket?.remoteAddress || "").trim();
  return isIP(socketIp) ? socketIp : "unknown";
}
