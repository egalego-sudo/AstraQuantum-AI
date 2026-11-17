import test from "node:test";
import assert from "node:assert/strict";
import { classifyCrossrefDoiResult } from "./doi-verification.js";

test("distinguishes a DOI absent from Crossref", () => {
  assert.deepEqual(classifyCrossrefDoiResult({ httpStatus: 404 }), {
    verified: false, status: "CROSSREF_DOI_NOT_FOUND"
  });
});

test("distinguishes a temporary Crossref lookup failure", () => {
  assert.deepEqual(classifyCrossrefDoiResult({ httpStatus: 503 }), {
    verified: null, status: "CROSSREF_LOOKUP_FAILED"
  });
});

test("does not verify a Crossref record without a title", () => {
  assert.deepEqual(classifyCrossrefDoiResult({ httpStatus: 200, crossrefTitle: "" }), {
    verified: null, status: "CROSSREF_RECORD_WITHOUT_TITLE"
  });
});

test("verifies a DOI only when the Crossref title similarity reaches threshold", () => {
  assert.deepEqual(classifyCrossrefDoiResult({ httpStatus: 200, crossrefTitle: "A real paper", titleSimilarity: 0.92 }), {
    verified: true, status: "CROSSREF_TITLE_MATCH"
  });
  assert.deepEqual(classifyCrossrefDoiResult({ httpStatus: 200, crossrefTitle: "A different paper", titleSimilarity: 0.2 }), {
    verified: false, status: "CROSSREF_TITLE_MISMATCH"
  });
});
