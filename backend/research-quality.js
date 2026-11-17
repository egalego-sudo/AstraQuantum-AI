const STOP_WORDS = new Set([
  "the","and","for","with","from","that","this","what","when","where","how","does","are","is","in","on","of","to","a","an","by","as","or","be","can","could","should","would","your","you","it","its","about","into","than","then","which","who","why","was","were","has","have","had",
  "क्या","कैसे","किस","किसके","और","के","की","का","में","से","है","हैं","को","पर","एक","भी","यह","वे","द्वारा","लिए","तथा","या","हो","होता","होती","होते","मानव","मस्तिष्क","बताइए","समझाइए","करता","करती","हुआ","हुई"
]);

const TOPIC_RULES = [
  {
    id: "photosynthesis",
    pattern: /photosynth|प्रकाश\s*संश्लेषण|कैल्विन|calvin\s*cycle|light[- ]dependent|प्रकाश[- ]निर्भर|cam\s*plants?/i,
    queries: [
      "photosynthesis light dependent reactions thylakoid membrane photosystem II photosystem I",
      "Calvin Benson cycle stroma RuBP Rubisco 3 phosphoglycerate G3P ATP NADPH",
      "photosynthesis carbon fixation plants chloroplasts light reactions ATP NADPH",
      "photosynthesis C3 C4 CAM carbon fixation plant physiology"
    ],
    anchors: ["photosynth","calvin","chloroplast","photosystem","carbon fixation","light-dependent","cam plant","c3 plant","c4 plant","rubisco","thylakoid","stroma","rubp","g3p","3-pga"],
    context: ["plant","plants","leaf","leaves","botany","chloroplast","photosynthetic","algae","chlorophyll","oxygenic","carbon dioxide","co2"]
  },
  {
    id: "consciousness",
    pattern: /consciousness|चेतना|चेतन|जागरूकता|सचेत|integrated\s+information|global\s+(neuronal\s+)?workspace|\bIIT\b|\bGNW\b|\bGWT\b/i,
    queries: [
      "neural correlates of consciousness brain awareness conscious experience neuroscience review",
      "Integrated Information Theory of consciousness empirical tests criticisms Phi",
      "Global Neuronal Workspace Theory consciousness neural ignition broadcasting evidence",
      "consciousness theories adversarial collaboration IIT global neuronal workspace experimental results",
      "neuroscience consciousness thalamocortical networks perception awareness"
    ],
    anchors: ["consciousness","conscious experience","neural correlates of consciousness","integrated information theory","global neuronal workspace","global workspace","awareness","conscious access","phenomenal consciousness","iit","gnw","gwt"],
    context: ["brain","neural","neuron","neurons","cortex","cortical","thalamus","thalamocortical","prefrontal","perception","cognition","neuroscience","awareness"]
  },
  {
    id: "cell-respiration",
    pattern: /cellular respiration|mitochondri|कोशिकीय श्वसन/i,
    queries: ["cellular respiration glycolysis mitochondria electron transport oxidative phosphorylation ATP"],
    anchors: ["cellular respiration","glycolysis","mitochondri","electron transport","oxidative phosphorylation"],
    context: ["cell","cells","organism","glucose","respiration","atp"]
  },
  {
    id: "genetics",
    pattern: /dna|gene|genetic|आनुवंशिक|डीएनए/i,
    queries: ["DNA genes genetics genome molecular biology chromosomes heredity"],
    anchors: ["dna","gene","genetic","genome","chromosome","heredity"],
    context: ["biology","cell","hereditary","molecular","organism"]
  },
  {
    id: "physics",
    pattern: /quantum|physics|भौतिक|प्रकाश की गति|gravity|गुरुत्व/i,
    queries: ["physics fundamental principles experimental research theory evidence"],
    anchors: ["physics","quantum","gravity","electromagnetic","particle","mechanics"],
    context: ["theory","experiment","energy","matter","field"]
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

function includesTerm(haystack, term) {
  const normalizedTerm = normalize(term);
  if (!normalizedTerm) return false;
  return (" " + haystack + " ").includes(" " + normalizedTerm + " ") || haystack.includes(normalizedTerm);
}

export function buildResearchQueries(query) {
  const original = String(query || "").trim();
  if (!original) return [];
  const matched = TOPIC_RULES.filter(rule => rule.pattern.test(original));
  const expanded = matched.flatMap(rule => rule.queries);
  const unique = [];
  for (const candidate of [original, ...expanded]) {
    const clean = candidate.trim().replace(/\s+/g, " ");
    if (clean && !unique.some(existing => normalize(existing) === normalize(clean))) unique.push(clean);
  }
  return unique.slice(0, 6);
}

export function scoreResearchSource(source, queries) {
  const title = normalize(source?.title || "");
  const abstract = normalize(source?.fullText || source?.abstract || "");
  const combined = title + " " + abstract;
  const rule = TOPIC_RULES.find(item => item.pattern.test((queries || []).join(" ")));
  const matchedAnchors = rule ? rule.anchors.filter(anchor => includesTerm(combined, anchor)) : [];
  const matchedContext = rule ? rule.context.filter(term => includesTerm(combined, term)) : [];

  let best = 0;
  let bestTerms = [];
  const titleTokens = new Set(tokens(title));
  const abstractTokens = new Set(tokens(abstract));
  for (const query of (queries || [])) {
    const queryTokens = [...new Set(tokens(query))];
    if (!queryTokens.length) continue;
    const matchedTitle = queryTokens.filter(token => titleTokens.has(token));
    const matchedAbstract = queryTokens.filter(token => abstractTokens.has(token));
    const score = Math.min(1, (matchedTitle.length * 3 + matchedAbstract.length) / (queryTokens.length * 3));
    if (score > best) {
      best = score;
      bestTerms = [...new Set([...matchedTitle, ...matchedAbstract])];
    }
  }

  if (rule) {
    if (!matchedAnchors.length && !matchedContext.length) {
      return { score: 0, matched_terms: [], topic: rule.id, rejected_reason: "missing_topic_evidence" };
    }
    const anchorBoost = Math.min(0.48, matchedAnchors.length * 0.12);
    const contextBoost = Math.min(0.18, matchedContext.length * 0.04);
    best = Math.min(1, best + anchorBoost + contextBoost);
    bestTerms = [...new Set([...bestTerms, ...matchedAnchors, ...matchedContext])];
    if (!matchedAnchors.length && matchedContext.length < 2) best = Math.min(best, 0.08);
  }

  return {
    score: Number(best.toFixed(4)),
    matched_terms: bestTerms.slice(0, 20),
    topic: rule?.id || "general",
    rejected_reason: best < 0.08 ? "low_query_relevance" : ""
  };
}

export function rankResearchSources(sources, queries, minimumScore = 0.08) {
  return (sources || [])
    .map(source => ({ ...source, relevance: scoreResearchSource(source, queries) }))
    .filter(source => source.relevance.score >= minimumScore)
    .sort((a, b) => b.relevance.score - a.relevance.score);
}
