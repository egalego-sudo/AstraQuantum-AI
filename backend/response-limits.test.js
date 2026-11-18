import test from "node:test";
import assert from "node:assert/strict";
import { readLimitedResponseJson, readLimitedResponseText } from "./response-limits.js";

test("reads UTF-8 response bodies within the exact byte budget", async () => {
  const value = "QUANTUM AI हिंदी";
  assert.equal(await readLimitedResponseText(new Response(value), Buffer.byteLength(value)), value);
});

test("rejects a response whose declared content length exceeds the budget", async () => {
  const response = new Response("1234567", { headers: { "content-length": "7" } });
  await assert.rejects(readLimitedResponseText(response, 6), /size limit/);
});

test("rejects a streamed response that exceeds the budget without a length header", async () => {
  const stream = new ReadableStream({
    start(controller) {
      controller.enqueue(new TextEncoder().encode("1234"));
      controller.enqueue(new TextEncoder().encode("5678"));
      controller.close();
    }
  });
  await assert.rejects(readLimitedResponseText(new Response(stream), 6), /size limit/);
});

test("parses JSON only after the response body passes the byte limit", async () => {
  assert.deepEqual(await readLimitedResponseJson(new Response('{"ok":true}'), 32), { ok: true });
  await assert.rejects(readLimitedResponseJson(new Response('{"payload":"too large"}'), 8), /size limit/);
});
