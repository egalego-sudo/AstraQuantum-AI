import test from "node:test";
import assert from "node:assert/strict";
import { normalizeCandidate, deduplicateCandidates } from "./knowledge-ingest.js";

test("normalizes DOI and preserves an unreviewed metadata-only state", () => {
  const item = normalizeCandidate({
    provider: "Crossref",
    title: "  A   Study of Quantum Systems ",
    doi: "https://doi.org/10.1234/ABC",
    authors: ["Ada Lovelace"],
    year: "2024",
    url: "https://doi.org/10.1234/ABC"
  });
  assert.equal(item.doi, "10.1234/ABC");
  assert.equal(item.review_status, "unreviewed");
  assert.match(item.usage_note, /Metadata candidate only/);
  assert.equal(item.year, 2024);
});

test("rejects candidates without a title", () => {
  assert.equal(normalizeCandidate({ provider: "OpenAlex", title: " " }), null);
});

test("deduplicates the same DOI across providers", () => {
  const a = normalizeCandidate({ provider: "OpenAlex", title: "Paper A", doi: "10.1000/xyz" });
  const b = normalizeCandidate({ provider: "Crossref", title: "Paper A duplicate", doi: "10.1000/XYZ" });
  assert.equal(deduplicateCandidates([a, b]).length, 1);
});

test("keeps separate metadata candidates when no DOI is available", () => {
  const a = normalizeCandidate({ provider: "OpenAlex", title: "Paper A" });
  const b = normalizeCandidate({ provider: "Crossref", title: "Paper B" });
  assert.equal(deduplicateCandidates([a, b]).length, 2);
});
