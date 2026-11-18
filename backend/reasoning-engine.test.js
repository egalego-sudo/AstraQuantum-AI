import test from "node:test";
import assert from "node:assert/strict";
import { buildReasoningInstructions, extractCurrentUserRequest, selectReasoningModes } from "./reasoning-engine.js";

test("detects multiple relevant modes in a Hindi root-cause coding request", () => {
  const modes = selectReasoningModes("Android ऐप में error का root cause खोजकर code fix और regression test करो");
  assert.ok(modes.includes("root_cause"));
  assert.ok(modes.includes("software_engineering"));
});

test("selects research and experiment checks for a scientific hypothesis", () => {
  const modes = selectReasoningModes("इस वैज्ञानिक hypothesis पर research करके experiment design करो");
  assert.ok(modes.includes("research"));
  assert.ok(modes.includes("experiment"));
  assert.ok(modes.includes("scientific_analysis"));
});

test("AI model guidance warns against equating orchestration with trained capability", () => {
  const guide = buildReasoningInstructions("Build a Transformer foundation model with RAG and quantization");
  assert.match(guide, /prompt orchestration from trained model capability/i);
  assert.match(guide, /benchmark evidence/i);
});

test("logic verification checks premises, validity, fallacies and counterexamples", () => {
  const guide = buildReasoningInstructions("इस तर्क में विरोधाभास और कुतर्क खोजकर सही या गलत निष्कर्ष बताओ");
  assert.match(guide, /explicit premises and conclusion/i);
  assert.match(guide, /counterexamples/i);
});

test("pattern analysis separates signal from noise and correlation from causation", () => {
  const guide = buildReasoningInstructions("इस डेटा में pattern और trend पहचानो");
  assert.match(guide, /signal from noise/i);
  assert.match(guide, /correlation alone/i);
});

test("cross-domain synthesis states domain assumptions and limits", () => {
  const guide = buildReasoningInstructions("विज्ञान और अर्थशास्त्र के अलग-अलग विषयों का ज्ञान जोड़ो");
  assert.match(guide, /domain boundaries/i);
});

test("creative synthesis compares novel ideas by feasibility and testability", () => {
  const guide = buildReasoningInstructions("तीन नए विचार और वैकल्पिक समाधान बनाओ");
  assert.match(guide, /testability/i);
});

test("decision guidance compares trade-offs and reversibility", () => {
  const guide = buildReasoningInstructions("इन तीन विकल्पों की तुलना करके निर्णय बताओ");
  assert.match(guide, /reversibility/i);
  assert.match(guide, /trade-offs/i);
});

test("education guidance includes learner level and misconception check", () => {
  const guide = buildReasoningInstructions("मुझे calculus शुरुआती स्तर से समझाइए");
  assert.match(guide, /starting level/i);
  assert.match(guide, /misconception check/i);
});

test("general requests receive a general quality checklist", () => {
  const guide = buildReasoningInstructions("नमस्ते");
  assert.match(guide, /GENERAL ANALYSIS CHECK/);
});

test("empty prompts produce no injected checklist", () => {
  assert.deepEqual(selectReasoningModes(" "), []);
  assert.equal(buildReasoningInstructions(""), "");
});

test("checklist is bounded to four specialized modes", () => {
  const guide = buildReasoningInstructions("AI model research hypothesis experiment code debugging physics decision strategy");
  const specialized = guide.split("\n").filter(line => line.includes(" CHECK:")).length;
  assert.ok(specialized <= 4);
});

test("recognizes every requested programming language family", () => {
  const languages = [
    "Python", "JavaScript", "TypeScript", "Java", "Kotlin", "C", "C++", "C#",
    "CSharp", "Go", "Golang", "Rust", "PHP", "Swift", "SQL", "HTML", "CSS",
    "Verilog", "SystemVerilog", "Bash"
  ];
  for (const language of languages) {
    assert.ok(
      selectReasoningModes(language).includes("software_engineering"),
      `language not recognized: ${language}`
    );
  }
});

test("classifies only the current user request, excluding embedded app instructions", () => {
  const wrapped = [
    "[CURRENT USER REQUEST]",
    "मुझे calculus समझाइए",
    "",
    "[QUANTUM AI CONTEXT INTELLIGENCE · selected history]",
    "पुरानी बातचीत में research, root cause, code और experiment शब्द हैं।",
    "",
    "[QUANTUM AI ADVANCED REASONING ARCHITECTURE]",
    "Always analyze root causes, research and experiments."
  ].join("\n");
  const extracted = extractCurrentUserRequest(wrapped);
  assert.equal(extracted, "मुझे calculus समझाइए");
  assert.deepEqual(selectReasoningModes(extracted), ["education", "scientific_analysis"]);
});

test("leaves direct API prompts unchanged when no app wrapper is present", () => {
  assert.equal(extractCurrentUserRequest("Find the root cause of this bug"), "Find the root cause of this bug");
});
