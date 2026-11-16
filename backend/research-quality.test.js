import test from "node:test";
import assert from "node:assert/strict";
import { buildResearchQueries, rankResearchSources, scoreResearchSource } from "./research-quality.js";

test("expands Hindi photosynthesis questions into focused scientific searches", () => {
  const queries = buildResearchQueries("प्रकाश संश्लेषण में प्रकाश-निर्भर प्रतिक्रियाएँ और कैल्विन चक्र क्या हैं?");
  assert.ok(queries.some(query => /photosynthesis/i.test(query)));
  assert.ok(queries.some(query => /Calvin cycle/i.test(query)));
  assert.ok(queries.length >= 3);
});

test("rejects unrelated ATP synthase and astrophysics papers for photosynthesis", () => {
  const queries = buildResearchQueries("photosynthesis plants Calvin cycle ATP NADPH");
  const ranked = rankResearchSources([
    { title: "Electric Field Driven Torque in ATP Synthase", abstract: "ATP synthase enzyme and proton movement." },
    { title: "Multi-messenger Observations of a Binary Neutron Star Merger", abstract: "Neutron stars and gravitational waves." },
    { title: "Photosynthesis and the Calvin Cycle in Green Plants", abstract: "Chloroplast photosystems, plant leaves, carbon fixation and light-dependent reactions." }
  ], queries);
  assert.equal(ranked.length, 1);
  assert.match(ranked[0].title, /Photosynthesis/);
  assert.ok(ranked[0].relevance.score >= 0.12);
});

test("does not accept a paper merely because it mentions ATP and NADPH", () => {
  const queries = buildResearchQueries("प्रकाश संश्लेषण में ATP और NADPH की भूमिका");
  const score = scoreResearchSource({
    title: "ATP production in mitochondria",
    abstract: "Mitochondrial ATP synthase and NADPH metabolism in pancreatic cells."
  }, queries);
  assert.equal(score.score, 0);
  assert.equal(score.rejected_reason, "missing_topic_anchor");
});

test("ranks a direct photosynthesis source above a tangential source", () => {
  const queries = buildResearchQueries("photosynthesis");
  const ranked = rankResearchSources([
    { title: "ATP metabolism in animal cells", abstract: "ATP and energy in cells." },
    { title: "Photosynthesis in plants", abstract: "Chloroplast, photosystem and carbon fixation in leaves." }
  ], queries);
  assert.equal(ranked[0].title, "Photosynthesis in plants");
});

test("keeps general research queries usable without forcing a biology filter", () => {
  const queries = buildResearchQueries("history of the printing press");
  const ranked = rankResearchSources([
    { title: "History of the Printing Press", abstract: "Printing technology in Europe." },
    { title: "Photosynthesis in plants", abstract: "Chloroplasts and carbon fixation." }
  ], queries);
  assert.equal(ranked.length, 1);
  assert.equal(ranked[0].title, "History of the Printing Press");
});

test("deduplicates expanded search queries", () => {
  const queries = buildResearchQueries("photosynthesis");
  assert.equal(new Set(queries.map(query => query.toLowerCase())).size, queries.length);
});
