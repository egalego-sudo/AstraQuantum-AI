import test from "node:test";
import assert from "node:assert/strict";
import { estimateInputBytes, fitContentToByteBudget, truncateUtf8 } from "./token-budget.js";

test("truncates UTF-8 without leaving a replacement character", () => {
  const clipped = truncateUtf8("हिंदी भाषा", 5);
  assert.ok(Buffer.byteLength(clipped, "utf8") <= 5);
  assert.ok(!clipped.endsWith("\uFFFD"));
});

test("bounds text content after reserving instruction and JSON overhead", () => {
  const content = [{ type: "input_text", text: "x".repeat(10_000) }];
  const bounded = fitContentToByteBudget(content, 1_000, 3_000);
  assert.ok(estimateInputBytes("i".repeat(1_000), bounded) <= 3_000);
  assert.ok(bounded[0].text.length < 10_000);
});

test("preserves non-text image parts while bounding accompanying text", () => {
  const image = { type: "input_image", image_url: "data:image/png;base64,AAAA" };
  const bounded = fitContentToByteBudget([image, { type: "input_text", text: "question ".repeat(1000) }], 500, 2_000);
  assert.deepEqual(bounded[0], image);
  assert.ok(Buffer.byteLength(bounded[1].text, "utf8") <= 988);
});

test("keeps content unchanged when it already fits", () => {
  const content = [{ type: "input_text", text: "short question" }];
  assert.deepEqual(fitContentToByteBudget(content, 100, 2_000), content);
});
