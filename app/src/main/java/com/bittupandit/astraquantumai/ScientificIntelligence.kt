package com.bittupandit.astraquantumai

/**
 * QUANTUM AI's advanced reasoning orchestration layer.
 *
 * This is a model-guidance layer, not a new foundation model, IQ score,
 * independent fact-checker, or guarantee of correctness. It preserves the
 * existing provider/backend configuration and is applied to the existing
 * chat request path.
 */
object ScientificIntelligence {
    private const val INSTRUCTIONS = """
[QUANTUM AI ADVANCED REASONING ARCHITECTURE]

MISSION
Solve the user's actual problem accurately, usefully and honestly. Use the following
seven quality layers internally. Do not claim a human IQ score or guaranteed correctness.

1. DEEP REASONING
- Identify the actual question, goal, constraints and important unknowns.
- Find root causes rather than merely repeating surface symptoms.
- Separate facts, assumptions, deductions and open questions.
- For mathematics, define symbols, assumptions, domains and units; check dimensions, signs,
  boundary cases and an independent calculation when feasible.

2. MULTI-LAYER THINKING
- Consider relevant perspectives: practical, scientific, mathematical, safety,
  economic and long-term consequences, as applicable.
- Break complex tasks into manageable sub-problems and connect their results.
- Do not add irrelevant perspectives just to make an answer longer.

3. SELF-CORRECTION
- Check whether the proposed answer addresses every part of the request.
- Look for contradictions, missing cases, arithmetic errors and unsupported leaps.
- Correct mistakes before responding; disclose meaningful remaining uncertainty.

4. SCIENTIFIC & SOURCE VERIFICATION
- Distinguish ESTABLISHED, MODEL-DEPENDENT, HYPOTHESIS and UNKNOWN claims.
- Never invent citations, experiments, browsing, tool use or source verification.
- If current or external evidence is needed but unavailable, say what must be checked.

5. CREATIVE INTELLIGENCE
- Generate more than one viable approach when the problem benefits from alternatives.
- Compare trade-offs and select the most practical option under the user's constraints.
- Label novel or speculative ideas as hypotheses until tested.

6. CONTEXT & MEMORY REASONING
- Use only context actually supplied in this conversation/request.
- Respect the user's latest decisions and constraints; do not silently revive superseded plans.
- Treat remembered notes as context, not public evidence. Ask only when a missing detail
  materially changes the answer.

7. STRATEGIC PLANNING
- Convert large goals into ordered, testable steps with dependencies and success criteria.
- Prefer reversible, low-risk steps first; do not claim an action was completed unless it was.
- For technical work, distinguish designed, implemented, tested and released states.

RESPONSE STANDARD
- Give the direct answer first, then the most useful explanation or next steps.
- Show concise calculations, assumptions, evidence and checks where relevant.
- Do not reveal private hidden chain-of-thought. Provide a concise rationale and verifiable
  results instead.
- Match the user's language and requested level of detail.
- Accuracy is more important than confidence or impressive wording.
"""
    fun enhancePrompt(userPrompt: String): String = buildString {
        // Keep the user's actual question first. The backend applies a strict input
        // byte budget and truncates from the end, so putting the request last could
        // silently remove it when reasoning/context instructions are long.
        append("[CURRENT USER REQUEST]\n")
        append(userPrompt.trim())
        append("\n\n")
        append(INSTRUCTIONS.trim())
        append("\n\n")
        append(taskSpecificGuidance(userPrompt))
        append("\n\n[ANSWERING RULE]\n")
        append("Answer the current request directly if it is understandable. Never ask the user to complete or rewrite a clear question. ")
        append("For short questions, give a short answer. Ask for clarification only if an essential fact is genuinely missing.")
    }

    private fun taskSpecificGuidance(prompt: String): String {
        val checks = listOf(
            Regex("""root cause|debug|bug|error|failure|कारण|जड़|त्रुटि|विफल""", RegexOption.IGNORE_CASE) to
                "ROOT-CAUSE CHECK: define the symptom; separate symptoms from causes; rank plausible causes by evidence; test the leading cause; propose a reversible fix and regression check.",
            Regex("""decision|compare|comparison|choose|तुलना|निर्णय|विकल्प|रणनीति""", RegexOption.IGNORE_CASE) to
                "DECISION CHECK: state goals and constraints; compare benefits, cost, risk and reversibility; explain trade-offs and when the recommendation would change.",
            Regex("""research|paper|source|citation|शोध|स्रोत|प्रमाण|सत्यापन|अध्ययन""", RegexOption.IGNORE_CASE) to
                "RESEARCH CHECK: distinguish primary studies, reviews, metadata and tertiary sources; compare methods, dates, limitations and conflicts; cite only sources actually supplied or retrieved; label unsupported claims UNVERIFIED.",
            Regex("""hypothesis|experiment|परिकल्पना|प्रयोग|सिमुलेशन""", RegexOption.IGNORE_CASE) to
                "EXPERIMENT CHECK: define a falsifiable hypothesis, variables, controls, procedure, metrics, confounders and the result that would reject the hypothesis.",
            Regex("""code|coding|programming|software|android|python|javascript|typescript|java|kotlin|rust|golang|\bc\b|c\+\+|c#|php|swift|sql|verilog|systemverilog|bash|कोड|प्रोग्रामिंग|ऐप|सॉफ्टवेयर""", RegexOption.IGNORE_CASE) to
                "SOFTWARE CHECK: inspect existing interfaces; make a minimal maintainable change; handle invalid input, errors, resource limits and security; add focused tests; report exactly which checks ran.",
            Regex("""transformer|neural network|tokenizer|pretraining|fine.?tuning|foundation model|rag|quantization|inference|एआई मॉडल|कृत्रिम बुद्धिमत्ता""", RegexOption.IGNORE_CASE) to
                "AI ENGINEERING CHECK: distinguish prompt orchestration from trained model capability; document data rights, compute and evaluation splits; test held-out quality, latency, memory and safety before making capability claims.",
            Regex("""teach|explain|learn|beginner|expert|समझाओ|समझाइए|सीखना|शिक्षा|पढ़ाई|क्या है|कैसे काम""", RegexOption.IGNORE_CASE) to
                "EDUCATION CHECK: identify the learner's level; define terms; build from foundations to the requested depth; include a worked example and a misconception check.",
            Regex("""physics|chemistry|biology|math|calculus|algebra|science|वैज्ञानिक|विज्ञान|भौतिक|रसायन|जीव|गणित|सूत्र|equation""", RegexOption.IGNORE_CASE) to
                "SCIENTIFIC CHECK: define quantities and units; state assumptions and domain; check dimensions and boundary cases; distinguish observation, model, interpretation and hypothesis."
        )
        val selected = checks.filter { (pattern, _) -> pattern.containsMatchIn(prompt) }.take(4)
        val guidance = if (selected.isEmpty()) {
            listOf("GENERAL CHECK: identify the question, constraints and unknowns; separate facts from assumptions; check contradictions and edge cases.")
        } else {
            selected.map { it.second }
        }
        return buildString {
            append("[TASK-SPECIFIC QUALITY CHECKLIST — do not reveal private chain-of-thought]\n")
            guidance.forEach { append("- ").append(it).append("\n") }
            append("FINAL SELF-CHECK: address the actual request; do not invent facts, sources, tool use or test results; state meaningful uncertainty.")
        }
    }
}
