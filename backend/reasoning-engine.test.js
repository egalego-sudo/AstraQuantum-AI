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

test("history and archaeology guidance distinguishes evidence types and dating", () => {
  const guide = buildReasoningInstructions("इतिहास और पुरातत्व के प्रमाणों का कालक्रम सहित विश्लेषण करो");
  assert.match(guide, /primary sources/i);
  assert.match(guide, /dating method/i);
});

test("law and finance guidance requires jurisdiction, date and risk disclosure", () => {
  const guide = buildReasoningInstructions("भारत में इस कानूनी और वित्तीय नियम की जानकारी दो");
  assert.match(guide, /jurisdiction, date and assumptions/i);
  assert.match(guide, /individualized legal, tax or investment advice/i);
});

test("philosophy and social-science guidance separates value and empirical claims", () => {
  const guide = buildReasoningInstructions("दर्शन और मनोविज्ञान के इस प्रश्न की तुलना करो");
  assert.match(guide, /value judgment/i);
  assert.match(guide, /cultural context/i);
});

test("language and literature guidance distinguishes translation from interpretation", () => {
  const guide = buildReasoningInstructions("इस प्राचीन साहित्य का भाषावैज्ञानिक और अनुवाद विश्लेषण करो");
  assert.match(guide, /literal translation from interpretation/i);
});

test("astronomy guidance requires reference frames and observational limits", () => {
  const guide = buildReasoningInstructions("खगोल विज्ञान में इस ब्रह्मांडीय दूरी की गणना करो");
  assert.match(guide, /reference frame/i);
});

test("mathematics guidance requires derivation and proof boundaries", () => {
  const guide = buildReasoningInstructions("calculus theorem का proof दीजिए");
  assert.match(guide, /verifiable derivation/i);
});

test("computer science guidance checks complexity and invariants", () => {
  const guide = buildReasoningInstructions("Analyze this distributed system algorithm and its complexity");
  assert.match(guide, /time\/space complexity/i);
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
