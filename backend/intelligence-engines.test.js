import test from "node:test";
import assert from "node:assert/strict";
import {
  MAX_COMPLETION_CONTINUATIONS,
  buildContinuationPrompt,
  completionLimitNotice,
  extractResponsesText,
  isResponsesIncomplete
} from "./intelligence-engines.js";

test("extracts text from Responses API message blocks", () => {
  assert.equal(extractResponsesText({output:[
    {type:"reasoning",content:[]},
    {type:"message",content:[{type:"output_text",text:"नमस्ते"}]}
  ]}), "नमस्ते");
});

test("only max-output-token incomplete responses request continuation", () => {
  assert.equal(isResponsesIncomplete({status:"incomplete",incomplete_details:{reason:"max_output_tokens"}}), true);
  assert.equal(isResponsesIncomplete({status:"incomplete",incomplete_details:{reason:"content_filter"}}), false);
  assert.equal(isResponsesIncomplete({status:"completed"}), false);
});

test("continuation prompt preserves original request and partial answer", () => {
  const prompt=buildContinuationPrompt("प्रश्न", "पहला भाग");
  assert.match(prompt,/प्रश्न/);
  assert.match(prompt,/पहला भाग/);
  assert.match(prompt,/Do not repeat/);
});

test("continuation is bounded and gives an honest limit notice", () => {
  assert.equal(MAX_COMPLETION_CONTINUATIONS,2);
  assert.match(completionLimitNotice(),/अधूरा/);
});
