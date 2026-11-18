import test from "node:test";
import assert from "node:assert/strict";
import { fetchWithSafeRedirects, isAllowedHttpsUrl } from "./safe-fetch.js";

const hosts = new Set(["api.crossref.org", "www.ebi.ac.uk", "arxiv.org"]);

test("allows only HTTPS URLs on the exact approved host list", () => {
  assert.equal(isAllowedHttpsUrl("https://api.crossref.org/works", hosts), true);
  assert.equal(isAllowedHttpsUrl("https://api.crossref.org:443/works", hosts), true);
  assert.equal(isAllowedHttpsUrl("http://api.crossref.org/works", hosts), false);
  assert.equal(isAllowedHttpsUrl("https://api.crossref.org.evil.test/works", hosts), false);
  assert.equal(isAllowedHttpsUrl("https://user@api.crossref.org/works", hosts), false);
  assert.equal(isAllowedHttpsUrl("https://api.crossref.org:8443/works", hosts), false);
  assert.equal(isAllowedHttpsUrl("https://127.0.0.1/", hosts), false);
  assert.equal(isAllowedHttpsUrl("https://metadata.google.internal/", hosts), false);
});

test("follows a relative redirect on an approved host", async () => {
  const seen = [];
  const fakeFetch = async (url, options) => {
    seen.push({ url: url.href, redirect: options.redirect });
    if (seen.length === 1) return new Response(null, { status: 302, headers: { location: "/works/123" } });
    return new Response("{\"ok\":true}", { status: 200 });
  };

  const response = await fetchWithSafeRedirects("https://api.crossref.org/works", {}, hosts, fakeFetch);
  assert.equal(response.status, 200);
  assert.deepEqual(seen, [
    { url: "https://api.crossref.org/works", redirect: "manual" },
    { url: "https://api.crossref.org/works/123", redirect: "manual" }
  ]);
});

test("blocks redirects to an unapproved host or an insecure scheme", async () => {
  for (const location of ["https://127.0.0.1/admin", "https://evil.example/", "http://api.crossref.org/works"]) {
    const fakeFetch = async () => new Response(null, { status: 302, headers: { location } });
    await assert.rejects(
      fetchWithSafeRedirects("https://api.crossref.org/works", {}, hosts, fakeFetch),
      /Blocked redirect/
    );
  }
});

test("enforces the redirect ceiling", async () => {
  let calls = 0;
  const fakeFetch = async () => {
    calls += 1;
    return new Response(null, { status: 302, headers: { location: "/again" } });
  };
  await assert.rejects(
    fetchWithSafeRedirects("https://api.crossref.org/works", {}, hosts, fakeFetch, 2),
    /Too many redirects/
  );
  assert.equal(calls, 3);
});
