import test from "node:test";
import assert from "node:assert/strict";
import { searchKnowledge } from "./knowledge.js";

test("retrieves a relevant foundational record with its source provenance", async () => {
  const results = await searchKnowledge("speed of light in vacuum", 3);
  assert.ok(results.some(record => record.id === "physics.speed-of-light-vacuum"));
  const match = results.find(record => record.id === "physics.speed-of-light-vacuum");
  assert.ok(match.sources.length > 0);
  assert.match(match.sources[0].url, /^https:\/\//);
});

test("returns no records for an unrelated query", async () => {
  const results = await searchKnowledge("unrelated fictional phrase xyzzy", 4);
  assert.deepEqual(results, []);
});
