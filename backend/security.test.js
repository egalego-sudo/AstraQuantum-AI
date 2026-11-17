import test from "node:test";
import assert from "node:assert/strict";
import { spawn } from "node:child_process";
import { once } from "node:events";
import { setTimeout as sleep } from "node:timers/promises";
import { fileURLToPath } from "node:url";
import { dirname, resolve } from "node:path";

const backendDir = dirname(fileURLToPath(import.meta.url));

test("private API requires bearer token and keeps health response minimal", async t => {
  const port = 18000 + Math.floor(Math.random() * 20000);
  const child = spawn(process.execPath, ["server.js"], {
    cwd: backendDir,
    env: {
      ...process.env,
      PORT: String(port),
      APP_ACCESS_TOKEN: "test-only-secret-token",
      ALLOWED_ORIGINS: "https://allowed.example",
      GROQ_API_KEY: "test-placeholder"
    },
    stdio: ["ignore", "pipe", "pipe"]
  });
  let stderr = "";
  child.stderr.setEncoding("utf8");
  child.stderr.on("data", chunk => { stderr += chunk; });
  t.after(() => { if (!child.killed) child.kill("SIGTERM"); });

  const base = "http://127.0.0.1:" + port;
  let ready = false;
  for (let attempt = 0; attempt < 60; attempt++) {
    try {
      const response = await fetch(base + "/health");
      if (response.ok) { ready = true; break; }
    } catch {}
    if (child.exitCode !== null) break;
    await sleep(100);
  }
  assert.equal(ready, true, "server should start; stderr: " + stderr);

  const health = await fetch(base + "/health");
  assert.equal(health.status, 200);
  const healthBody = await health.json();
  assert.equal(healthBody.ok, true);
  assert.equal("model" in healthBody, false);
  assert.equal("provider" in healthBody, false);

  const noToken = await fetch(base + "/v1/generate", {
    method: "POST", headers: { "content-type": "application/json" }, body: JSON.stringify({ prompt: "test" })
  });
  assert.equal(noToken.status, 401);

  const wrongToken = await fetch(base + "/v1/generate", {
    method: "POST", headers: { "content-type": "application/json", authorization: "Bearer wrong" },
    body: JSON.stringify({ prompt: "test" })
  });
  assert.equal(wrongToken.status, 401);

  const authorizedButInvalid = await fetch(base + "/v1/generate", {
    method: "POST", headers: { "content-type": "application/json", authorization: "Bearer test-only-secret-token" },
    body: JSON.stringify({})
  });
  assert.equal(authorizedButInvalid.status, 400, "valid token should pass authentication and reach input validation");

  const disallowedOrigin = await fetch(base + "/health", { headers: { origin: "https://evil.example" } });
  assert.equal(disallowedOrigin.headers.get("access-control-allow-origin"), null);

  const allowedOrigin = await fetch(base + "/health", { headers: { origin: "https://allowed.example" } });
  assert.equal(allowedOrigin.headers.get("access-control-allow-origin"), "https://allowed.example");
});
