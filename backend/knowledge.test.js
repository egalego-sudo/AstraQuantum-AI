import test from "node:test";
import assert from "node:assert/strict";
import { searchKnowledge, validateKnowledgeCitations } from "./knowledge.js";

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


test("excludes OpenStax-derived English and Hindi records until written AI-ingestion permission is reviewed", async () => {
  const english = await searchKnowledge("Calvin cycle Rubisco RuBP 3-PGA G3P ATP NADPH", 8);
  const hindi = await searchKnowledge("प्रकाश संश्लेषण कैल्विन चक्र RuBP G3P", 8);
  const newton = await searchKnowledge("Newton second law force mass acceleration", 8);
  const energy = await searchKnowledge("conservation of energy isolated system", 8);
  const disabledIds = new Set([
    "physics.newtons-second-law",
    "physics.conservation-of-energy",
    "biology.photosynthesis",
    "biology.photosynthesis.calvin-cycle",
    "biology.photosynthesis.calvin-cycle.hi"
  ]);
  for (const record of [...english, ...hindi, ...newton, ...energy]) {
    assert.ok(!disabledIds.has(record.id), `license-pending record was retrieved: ${record.id}`);
  }
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
  const results = await searchKnowledge("speed of light vacuum", 8);
  for (const record of results) {
    assert.ok(["source_checked", "expert_reviewed"].includes(record.review_status));
    assert.ok(record.sources.length > 0);
    for (const source of record.sources) assert.match(source.url, /^https:\/\//);
  }
});


test("keeps valid knowledge citations and removes unknown record IDs", () => {
  const records = [{
    id: "physics.speed-of-light-vacuum",
    title: "Speed of light in vacuum",
    sources: [{ publisher: "NIST", url: "https://physics.nist.gov/example" }]
  }];
  const answer = "Supported [KB:physics.speed-of-light-vacuum]. Unsupported [KB:made-up.record].";
  const result = validateKnowledgeCitations(answer, records);
  assert.ok(result.includes("[KB:physics.speed-of-light-vacuum]"));
  assert.match(result, /unverified knowledge-base reference removed/);
  assert.ok(!result.includes("[KB:made-up.record]"));
});

test("adds provenance URLs only for records actually cited", () => {
  const records = [
    { id: "record.a", title: "Record A", sources: [{ publisher: "Publisher A", url: "https://example.org/a" }] },
    { id: "record.b", title: "Record B", sources: [{ publisher: "Publisher B", url: "https://example.org/b" }] }
  ];
  const result = validateKnowledgeCitations("Evidence [KB:record.a].", records);
  assert.ok(result.includes("https://example.org/a"));
  assert.ok(!result.includes("https://example.org/b"));
});

test("leaves deep-research numeric citations unchanged when no local records were retrieved", () => {
  const answer = "Finding supported by [1] and [2].";
  assert.equal(validateKnowledgeCitations(answer, []), answer);
});
