/**
 * QUANTUM AI capability orchestration. These layers improve use of the
 * configured foundation model; they do not train or replace that model.
 */
export const MAX_COMPLETION_CONTINUATIONS = 2;

export const INTELLIGENCE_ENGINE_INSTRUCTIONS = `
SIX INTELLIGENCE ENGINES — apply on every answer:
1. KNOWLEDGE: Use relevant retrieved knowledge; prefer primary/official sources for law, science and standards. Keep dates and jurisdiction explicit. A small local corpus is not complete knowledge.
2. REASONING: Decompose multi-part requests and constraints; check arithmetic, units, definitions and examples; address every part. Give key rationale, never hidden chain-of-thought.
3. RESEARCH: Compare supplied sources; distinguish primary studies, abstracts/indexes and tertiary summaries. Cite supplied source IDs beside claims. Never claim browsing/search unless results were supplied.
4. FACT CHECK: Skeptically check names, dates, quantities, law/jurisdiction, calculations and contradictions against available evidence. Label evidence-supported, uncertain, disputed or unchecked. Do not claim independent verification without evidence; disclose source conflicts.
5. MEMORY: Use only context actually supplied by the app and respect privacy opt-in, user corrections and project notes. Never invent missing personal context.
6. COMPLETE RESPONSE: Cover every requested part; do not silently stop mid-sentence. Continue from the exact last point without repeating. If provider limits still prevent completion, clearly say so.
QUALITY GATE: Separate facts, estimates, assumptions and opinions. Use the user's language. Never invent citations, URLs, tests or confidence percentages. State when current verification is unavailable.
`;

export function extractResponsesText(data) {
  return (data?.output || [])
    .filter(item => item?.type === "message")
    .flatMap(item => item.content || [])
    .filter(item => item?.type === "output_text" || item?.type === "text")
    .map(item => item.text || "")
    .join("")
    .trim();
}

export function isResponsesIncomplete(data) {
  return data?.status === "incomplete" &&
    data?.incomplete_details?.reason === "max_output_tokens";
}

export function buildContinuationPrompt(question, partialAnswer) {
  return [
    "Continue this partial answer from its exact end. Do not repeat completed material.",
    "Complete all remaining parts and finish with a conclusion.",
    "Original user request:",
    String(question || "").trim(),
    "Partial answer so far:",
    String(partialAnswer || "").trim()
  ].join("\n\n");
}

export function completionLimitNotice() {
  return "\n\n[उत्तर तकनीकी आउटपुट सीमा के कारण अभी अधूरा हो सकता है। 'जारी रखें' लिखें; उत्तर यहीं से आगे बढ़ेगा।]";
}
