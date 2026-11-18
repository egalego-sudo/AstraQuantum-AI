import test from "node:test";
import assert from "node:assert/strict";
import { extractAtomTag } from "./arxiv-atom.js";

test("extracts ordinary arXiv Atom fields", () => {
  const xml = '<entry><id>https://arxiv.org/abs/2601.12345</id><published>2025-01-01T00:00:00Z</published><title>Example title</title></entry>';
  assert.equal(extractAtomTag(xml, "id"), "https://arxiv.org/abs/2601.12345");
  assert.equal(extractAtomTag(xml, "published"), "2025-01-01T00:00:00Z");
  assert.equal(extractAtomTag(xml, "title"), "Example title");
});

test("extracts multiline content and tags with attributes", () => {
  const xml = '<summary type="text">First line\nSecond line</summary>';
  assert.equal(extractAtomTag(xml, "summary"), "First line\nSecond line");
});

test("rejects invalid tag names and non-string input", () => {
  assert.equal(extractAtomTag(null, "title"), "");
  assert.equal(extractAtomTag("<title>x</title>", "title>"), "");
});
