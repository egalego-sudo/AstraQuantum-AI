/**
 * Deterministic task framing for QUANTUM AI.
 * This selects verification checklists; it does not expose hidden chain-of-thought
 * or independently prove an answer correct.
 */
const MODES = [
  {
    id: "root_cause",
    pattern: /root cause|कारण|जड़|समस्या.*(क्यों|कहाँ)|त्रुटि|error|bug|debug|failure|विफल|breaks?/i,
    guide: "ROOT-CAUSE CHECK: define the observed symptom; separate symptom from cause; list plausible causes; rank by evidence; test the leading cause; propose a reversible fix; specify a regression check."
  },
  {
    id: "logic_verification",
    pattern: /logic|logical|fallacy|valid argument|invalid argument|contradiction|inconsisten|proof|तर्क|कुतर्क|विरोधाभास|असंगति|सही.*गलत.*तर्क|तार्किक/i,
    guide: "LOGIC VERIFICATION: list the explicit premises and conclusion; test whether the conclusion follows; distinguish validity from truth of premises; look for hidden assumptions, equivocation, circular reasoning, counterexamples and contradictions; state what evidence would settle disputed premises."
  },
  {
    id: "pattern_analysis",
    pattern: /pattern|trend|anomal|recurring|repeated|पैटर्न|रुझान|असामान्यता|दोहराव|आवर्ती/i,
    guide: "PATTERN ANALYSIS: define the observations and comparison baseline; identify repeated structure and exceptions; separate signal from noise; test sample size, selection effects, confounders and spurious correlation; report uncertainty and avoid causal claims from correlation alone."
  },
  {
    id: "cross_domain_synthesis",
    pattern: /interdisciplin|cross.?domain|combine.{0,30}(fields|subjects|disciplines)|different.{0,20}(fields|subjects)|बहुविषयक|अंतरविषयक|अलग.?अलग.{0,20}(विषय|क्षेत्र)|विषयों.{0,20}(जोड़|मिल)|ज्ञान.{0,20}(जोड़|एकीकृत)/i,
    guide: "CROSS-DOMAIN SYNTHESIS: map the concepts and variables shared by the fields; state each field's assumptions and units; distinguish a useful analogy from a proven equivalence; identify where evidence transfers and where domain boundaries prevent transfer."
  },
  {
    id: "creative_synthesis",
    pattern: /new idea|novel concept|innovation|innovate|alternative idea|brainstorm|नई अवधारणा|नए विचार|नया विचार|नवाचार|वैकल्पिक विचार|रचनात्मक समाधान/i,
    guide: "CREATIVE SYNTHESIS: generate materially different candidate ideas; state the mechanism and assumptions for each; compare feasibility, novelty, cost, risk and testability; prefer a small falsifiable prototype over unsupported claims of discovery."
  },
  {
    id: "decision",
    pattern: /decide|decision|choose|compare|comparison|कौन.*(सही|बेहतर)|तुलना|निर्णय|विकल्प|strategy|रणनीति/i,
    guide: "DECISION CHECK: state the goal and hard constraints; compare viable options on benefits, cost, risk, reversibility and evidence; identify trade-offs; recommend one option with conditions that would change the recommendation."
  },
  {
    id: "research",
    pattern: /research|शोध|paper|प्रमाण|source|citation|literature|अध्ययन|स्रोत|सत्यापन|verify/i,
    guide: "RESEARCH CHECK: define the question and scope; distinguish primary research, reviews, metadata and tertiary sources; evaluate methods, date, sample and limitations; compare agreement/conflict; cite only supplied or actually retrieved sources; mark unsupported claims UNVERIFIED."
  },
  {
    id: "experiment",
    pattern: /hypothes|परिकल्पना|experiment|प्रयोग|test.*(plan|design)|परीक्षण.*(योजना|डिजाइन)|simulation|सिमुलेशन/i,
    guide: "EXPERIMENT CHECK: state a falsifiable hypothesis; define independent/dependent variables, controls, sample/data, procedure and metrics; identify confounders and safety limits; predefine what result supports or rejects the hypothesis."
  },
  {
    id: "software_engineering",
    pattern: /code|coding|program|programming|software|api|backend|frontend|android|kotlin|python|javascript|typescript|java|rust|golang|sql|database|github|pull request|deploy|build|compile|c\+\+|c#|\bc\b|\bgo\b|csharp|php|swift|verilog|systemverilog|bash|html|css|कोड|प्रोग्रामिंग|ऐप|एप्लीकेशन|सॉफ्टवेयर/i,
    guide: "SOFTWARE ENGINEERING CHECK: clarify runtime, interfaces and constraints; inspect existing conventions before changing code; make the smallest maintainable change; handle invalid input, errors, resource bounds and security; add focused unit/integration/regression tests; report exactly what was and was not run."
  },
  {
    id: "ai_engineering",
    pattern: /transformer|neural network|tokenizer|pretrain|fine.?tun|llm|foundation model|rag|retrieval|agent|multimodal|quantization|inference|language model|एआई मॉडल|एआई|कृत्रिम बुद्धिमत्ता/i,
    guide: "AI ENGINEERING CHECK: distinguish prompt orchestration from trained model capability; define data provenance/licensing, architecture, compute budget and evaluation split; prevent train/test contamination; measure quality, latency, memory and safety; compare against a baseline; do not claim capability without benchmark evidence."
  },
  {
    id: "history_archaeology",
    pattern: /history|historical|archaeolog|epigraph|inscription|chronolog|इतिहास|पुरातत्व|अभिलेख|शिलालेख|सभ्यता|कालक्रम/i,
    guide: "HISTORY/ARCHAEOLOGY: separate contemporary primary sources, inscriptions/material evidence, later chronicles, oral tradition and modern interpretation; check provenance, dating method, stratigraphic context and chronology; identify contested interpretations and avoid projecting modern categories backwards."
  },
  {
    id: "philosophy_ethics",
    pattern: /philosoph|ethic|epistemolog|metaphysic|moral theory|दर्शन|दर्शनशास्त्र|नीतिशास्त्र|ज्ञानमीमांसा|तत्वमीमांसा/i,
    guide: "PHILOSOPHY/ETHICS: define the central concepts and question type; identify premises and normative assumptions; compare relevant schools fairly; present the strongest counterargument; distinguish conceptual analysis, empirical claim and value judgment."
  },
  {
    id: "law_finance_economics",
    pattern: /legal|law|statute|regulation|court|contract|tax|finance|financial|economics|investment|market|कानून|कानूनी|अधिनियम|न्यायालय|अनुबंध|कर|वित्त|अर्थशास्त्र|निवेश|बाजार/i,
    guide: "LAW/FINANCE/ECONOMICS: state jurisdiction, date and assumptions; distinguish statute, regulation, judgment, policy and opinion; prefer primary official sources; explain uncertainty and material risks; do not present general information as individualized legal, tax or investment advice."
  },
  {
    id: "social_sciences",
    pattern: /psycholog|sociolog|social science|behavioral science|मनोविज्ञान|समाजशास्त्र|सामाजिक विज्ञान|व्यवहार विज्ञान/i,
    guide: "SOCIAL SCIENCES: identify the study design, population, sample and cultural context; distinguish correlation from causation; consider confounders, measurement bias and replication; avoid diagnosing individuals from general research."
  },
  {
    id: "language_literature",
    pattern: /linguistic|language science|literature|literary|grammar|translation|transliteration|script|भाषाविज्ञान|भाषा विज्ञान|साहित्य|व्याकरण|अनुवाद|लिपि/i,
    guide: "LANGUAGE/LITERATURE: identify language, dialect, script, period and genre; distinguish literal translation from interpretation; note textual variants and transliteration choices; support historical language claims with dated primary evidence."
  },
  {
    id: "astronomy_space",
    pattern: /astronom|astrophysic|cosmolog|space science|celestial|खगोल|खगोलशास्त्र|खगोलभौतिकी|ब्रह्मांड विज्ञान|अंतरिक्ष विज्ञान|आकाशीय/i,
    guide: "ASTRONOMY/SPACE SCIENCE: state reference frame, scale, units and observational limits; separate measured observations from theoretical models; check orders of magnitude and uncertainty; distinguish established results from speculative cosmology."
  },
  {
    id: "mathematics",
    pattern: /mathematics|algebra|geometry|calculus|theorem|proof|probability|statistics|गणित|बीजगणित|ज्यामिति|कलन|प्रमेय|प्रमाण|प्रायिकता|सांख्यिकी/i,
    guide: "MATHEMATICS: state definitions and assumptions; show a verifiable derivation; distinguish proof from numerical evidence or heuristic; check domains, edge cases, units and alternative solution paths."
  },
  {
    id: "computer_science",
    pattern: /computer science|algorithm|data structure|operating system|distributed system|complexity|कंप्यूटर विज्ञान|संगणक विज्ञान|एल्गोरिदम|डेटा संरचना|ऑपरेटिंग सिस्टम|वितरित प्रणाली/i,
    guide: "COMPUTER SCIENCE: define the computational model and input constraints; analyze time/space complexity; state invariants and failure cases; consider concurrency, memory safety, security and version/platform assumptions."
  },
  {
    id: "education",
    pattern: /teach|explain|learn|lesson|beginner|expert|समझाओ|समझाइए|सीखना|शिक्षा|पढ़ाई|क्या है|कैसे काम/i,
    guide: "EDUCATION CHECK: identify the learner's starting level; define terms; explain from foundations to the requested depth; use a worked example and a misconception check; distinguish established knowledge from simplified analogy."
  },
  {
    id: "scientific_analysis",
    pattern: /physics|chemistry|biology|math|calculus|algebra|science|scientific|वैज्ञानिक|विज्ञान|भौतिक|रसायन|जीव|गणित|सूत्र|equation/i,
    guide: "SCIENTIFIC CHECK: define quantities and units; state assumptions and applicable domain; check dimensions, boundary cases and uncertainty; distinguish observation, model, interpretation and hypothesis; do not overstate causality."
  }
];

export function extractCurrentUserRequest(prompt) {
  const value = String(prompt || "");
  const prefix = "[CURRENT USER REQUEST]\n";
  if (!value.startsWith(prefix)) return value.trim();
  let request = value.slice(prefix.length);
  const boundaries = [
    request.indexOf("\n\n[QUANTUM AI CONTEXT INTELLIGENCE"),
    request.indexOf("\n\n[QUANTUM AI ADVANCED REASONING ARCHITECTURE")
  ].filter(index => index >= 0);
  if (boundaries.length) request = request.slice(0, Math.min(...boundaries));
  return request.trim();
}

export function selectReasoningModes(prompt) {
  const value = String(prompt || "").trim();
  if (!value) return [];
  return MODES.filter(mode => mode.pattern.test(value)).map(mode => mode.id);
}

export function buildReasoningInstructions(prompt) {
  const modes = MODES.filter(mode => mode.pattern.test(String(prompt || "")));
  if (!String(prompt || "").trim()) return "";
  const selected = modes.slice(0, 4);
  const guides = selected.map(mode => mode.guide);
  if (!guides.length) {
    guides.push("GENERAL ANALYSIS CHECK: identify the actual question, constraints and unknowns; separate facts, assumptions and inference; address each requested part; check for contradictions and edge cases; state limitations.");
  }
  return [
    "[TASK-SPECIFIC QUALITY CHECKLIST — use silently; do not reveal private chain-of-thought]",
    ...guides,
    "FINAL SELF-CHECK: answer the user's actual request; do not invent facts, sources, tool use or test results. Give a concise rationale, evidence and uncertainty—not hidden internal reasoning."
  ].join("\n");
}
