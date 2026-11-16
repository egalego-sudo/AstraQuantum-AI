import test from "node:test";
import assert from "node:assert/strict";
import { searchKnowledge } from "./knowledge.js";

test("retrieves a relevant foundational record with source provenance", async () => {
  const results = await searchKnowledge("speed of light in vacuum", 3);
  assert.ok(results.some(record => record.id === "physics.speed-of-light-vacuum"));
  const match = results.find(record => record.id === "physics.speed-of-light-vacuum");
  assert.ok(match.sources.length > 0);
  assert.match(match.sources[0].url, /^https:\/\//);
});

test("retrieves Hindi knowledge for a Hindi query", async () => {
  const results = await searchKnowledge("निर्वात में प्रकाश की गति", 3);
  assert.ok(results.some(record => record.id === "physics.speed-of-light-vacuum.hi"));
});

test("retrieves scientific concepts across the expanded corpus", async () => {
  const results = await searchKnowledge("DNA genetic information double helix", 3);
  assert.ok(results.some(record => record.id === "biology.dna"));
});

test("respects the maximum result limit", async () => {
  const results = await searchKnowledge("विज्ञान भौतिकी प्रकाश", 100);
  assert.ok(results.length <= 8);
});

test("returns no records for an unrelated query", async () => {
  const results = await searchKnowledge("unrelated fictional phrase xyzzy", 4);
  assert.deepEqual(results, []);
});

test("ignores queries made only of common stop words", async () => {
  const results = await searchKnowledge("the is and का की है", 4);
  assert.deepEqual(results, []);
});

test("does not retrieve records that have not passed source review", async () => {
  const results = await searchKnowledge("atomic number protons nucleus", 8);
  assert.ok(!results.some(record => record.id === "chemistry.atomic-number"));
  assert.ok(!results.some(record => record.id === "chemistry.atomic-number.hi"));
});

test("retrieved records are reviewed and have HTTPS provenance", async () => {
  const results = await searchKnowledge("energy conservation isolated system", 8);
  for (const record of results) {
    assert.ok(["source_checked", "expert_reviewed"].includes(record.review_status));
    assert.ok(record.sources.length > 0);
    for (const source of record.sources) assert.match(source.url, /^https:\/\//);
  }
});
