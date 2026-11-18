import test from "node:test";
import assert from "node:assert/strict";
import { Readable } from "node:stream";
import { readJsonBody } from "./read-json-body.js";

test("preserves Hindi and emoji when UTF-8 code points are split across chunks", async () => {
  const expected = { prompt: "नमस्ते QUANTUM AI 🚀 — परीक्षण" };
  const bytes = Buffer.from(JSON.stringify(expected), "utf8");

  for (let split = 1; split < bytes.length; split += 1) {
    const request = Readable.from([bytes.subarray(0, split), bytes.subarray(split)]);
    assert.deepEqual(await readJsonBody(request), expected, `split at byte ${split}`);
  }
});

test("rejects request bodies that exceed the configured byte limit", async () => {
  const request = Readable.from([Buffer.from('{"prompt":"123456789"}')]);
  await assert.rejects(readJsonBody(request, { maxBytes: 8 }), {
    message: "Request too large"
  });
});

test("rejects malformed JSON and invalid byte limits", async () => {
  await assert.rejects(readJsonBody(Readable.from(["{broken"])), {
    message: "Invalid JSON"
  });
  await assert.rejects(readJsonBody(Readable.from(["{}"]), { maxBytes: 0 }), {
    name: "RangeError"
  });
});
