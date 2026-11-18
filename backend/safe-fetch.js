const REDIRECT_STATUSES = new Set([301, 302, 303, 307, 308]);

export function isAllowedHttpsUrl(value, allowedHosts) {
  try {
    const url = value instanceof URL ? value : new URL(value);
    return url.protocol === "https:" &&
      url.username === "" &&
      url.password === "" &&
      (url.port === "" || url.port === "443") &&
      allowedHosts.has(url.hostname.toLowerCase());
  } catch {
    return false;
  }
}

/**
 * Fetches only from an explicit HTTPS host allowlist and validates every redirect
 * before following it. The fetch implementation is injectable for deterministic tests.
 */
export async function fetchWithSafeRedirects(
  input,
  options,
  allowedHosts,
  fetchImpl = fetch,
  maxRedirects = 3
) {
  let current = input instanceof URL ? new URL(input.href) : new URL(input);

  for (let redirects = 0; ; redirects += 1) {
    if (!isAllowedHttpsUrl(current, allowedHosts)) {
      throw new Error("Blocked request to a non-approved HTTPS host.");
    }

    const response = await fetchImpl(current, { ...options, redirect: "manual" });
    if (!REDIRECT_STATUSES.has(response.status)) return response;

    const location = response.headers.get("location");
    if (!location) return response;
    if (redirects >= maxRedirects) {
      await response.body?.cancel().catch(() => {});
      throw new Error("Too many redirects.");
    }

    const next = new URL(location, current);
    await response.body?.cancel().catch(() => {});
    if (!isAllowedHttpsUrl(next, allowedHosts)) {
      throw new Error("Blocked redirect to a non-approved HTTPS host.");
    }
    current = next;
  }
}
