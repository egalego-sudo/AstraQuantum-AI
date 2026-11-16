const STOP_WORDS = new Set([
  "the","and","for","with","from","that","this","what","when","where","how","does","are","is","in","on","of","to","a","an","by","as","or","be","can","could","should","would","your","you","it","its",
  "क्या","कैसे","किस","किसके","और","के","की","का","में","से","है","हैं","को","पर","एक","भी","यह","वे","द्वारा","लिए","तथा","या","हो","होता","होती","होते"
]);

const TOPIC_RULES = [
  {
    id: "photosynthesis",
    pattern: /photosynth|प्रकाश\s*संश्लेषण|कैल्विन|calvin\s*cycle|light[- ]dependent|प्रकाश[- ]निर्भर|cam\s*plants?/i,
    queries: [
      "photosynthesis plants light-dependent reactions Calvin cycle",
      "photosynthesis C3 C4 CAM plants night carbon fixation",
      "chloroplast photosystem ATP NADPH carbon fixation plant leaves"
    ],
    anchors: ["photosynth","calvin","chloroplast","photosystem","carbon fixation","light-dependent","cam plant","c3 plant","c4 plant"],
    context: ["plant","plants","leaf","leaves","botany","chloroplast","photosynthetic","algae"]
  },
  {
    id: "cell-respiration",
    pattern: /cellular respiration|mitochondri|कोशिकीय श्वसन/i,
    queries: ["cellular respiration glycolysis mitochondria electron transport"],
    anchors: ["cellular respiration","glycolysis","mitochondri","electron transport"],
    context: ["cell","cells","organism","glucose","respiration"]
  },
  {
    id: "genetics",
    pattern: /dna|gene|genetic|आनुवंशिक|डीएनए/i,
    queries: ["DNA genes genetics genome molecular biology"],
    anchors: ["dna","gene","genetic","genome","chromosome"],
    context: ["biology","cell","hereditary","molecular"]
  },
  {
    id: "physics",
    pattern: /quantum|physics|भौतिक|प्रकाश की गति|gravity|गुरुत्व/i,
    queries: ["physics fundamental principles experimental research"],
    anchors: ["physics","quantum","gravity","electromagnetic","particle"],
    context: ["theory","experiment","energy","matter"]
  }
];

function normalize(value) {
  return String(value || "").toLowerCase().normalize("NFKC")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/[^\p{L}\p{N}]+/gu, " ")
    .trim();
}

function tokens(value) {
  return normalize(value).split(/\s+/u)
    .filter(token => token.length > 1 && !STOP_WORDS.has(token));
}

export function buildResearchQueries(query) {
  const original = String(query || "").trim();
  if (!original) return [];
  const normalized = normalize(original);
  const matched = TOPIC_RULES.filter(rule => rule.pattern.test(original) || rule.pattern.test(normalized));
  const expanded = matched.flatMap(rule => rule.queries);
  const unique = [];
  for (const candidate of [original, ...expanded]) {
    const clean = candidate.trim().replace(/\s+/g, " ");
    if (clean && !unique.some(existing => normalize(existing) === normalize(clean))) unique.push(clean);
  }
  return unique.slice(0, 5);
}

export function scoreResearchSource(source, queries) {
  const title = normalize(source?.title || "");
  const abstract = normalize(source?.fullText || source?.abstract || "");
  const combined = title + " " + abstract;
  const rule = TOPIC_RULES.find(item => item.pattern.test((queries || []).join(" ")));
  const matchedAnchors = rule ? rule.anchors.filter(anchor => combined.includes(normalize(anchor))) : [];
  const matchedContext = rule ? rule.context.filter(term => combined.includes(normalize(term))) : [];
  if (rule && matchedAnchors.length === 0) {
    return { score: 0, matched_terms: [], topic: rule.id, rejected_reason: "missing_topic_anchor" };
  }
  if (rule && matchedContext.length === 0) {
    return { score: 0, matched_terms: matchedAnchors, topic: rule.id, rejected_reason: "missing_domain_context" };
  }

  let best = 0;
  let bestTerms = [];
  for (const query of (queries || [])) {
    const queryTokens = [...new Set(tokens(query))];
    if (!queryTokens.length) continue;
    const titleTokens = new Set(tokens(title));
    const abstractTokens = new Set(tokens(abstract));
    const matchedTitle = queryTokens.filter(token => titleTokens.has(token));
    const matchedAbstract = queryTokens.filter(token => abstractTokens.has(token));
    const score = Math.min(1, (matchedTitle.length * 3 + matchedAbstract.length) / (queryTokens.length * 3));
    if (score > best) {
      best = score;
      bestTerms = [...new Set([...matchedTitle, ...matchedAbstract])];
    }
  }
  if (rule) {
    const anchorBoost = Math.min(0.3, matchedAnchors.length * 0.12);
    const contextBoost = Math.min(0.15, matchedContext.length * 0.05);
    best = Math.min(1, best + anchorBoost + contextBoost);
    bestTerms = [...new Set([...bestTerms, ...matchedAnchors, ...matchedContext])];
  }
  return {
    score: Number(best.toFixed(4)),
    matched_terms: bestTerms.slice(0, 16),
    topic: rule?.id || "general",
    rejected_reason: best < 0.12 ? "low_query_relevance" : ""
  };
}

export function rankResearchSources(sources, queries, minimumScore = 0.12) {
  return (sources || [])
    .map(source => ({ ...source, relevance: scoreResearchSource(source, queries) }))
    .filter(source => source.relevance.score >= minimumScore)
    .sort((a, b) => b.relevance.score - a.relevance.score);
}
