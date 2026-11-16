import { readFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import path from "node:path";

const seedPath = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../knowledge-base/seed/foundations.jsonl");
const STOP_WORDS = new Set([
  "a", "an", "the", "is", "are", "of", "in", "on", "for", "to", "and", "or", "what", "which", "how",
  "का", "की", "के", "है", "हैं", "में", "से", "को", "और", "एक", "क्या", "कैसे", "किस", "कौन", "पर"
]);
let cachedRecords;

function terms(value) {
  return String(value || "")
    .normalize("NFKC")
    .toLocaleLowerCase()
    .match(/[\p{L}\p{M}\p{N}]+/gu) || [];
}

async function loadRecords() {
  if (cachedRecords) return cachedRecords;
  try {
    const raw = await readFile(seedPath, "utf8");
    const seen = new Set();
    cachedRecords = raw.split(/\r?\n/).filter(Boolean).flatMap(line => {
      try {
        const record = JSON.parse(line);
        if (!record || !record.id || !record.title || !record.content || !Array.isArray(record.sources) || seen.has(record.id)) return [];
        seen.add(record.id);
        return [record];
      } catch {
        return [];
      }
    });
  } catch (error) {
    if (error.code === "ENOENT") return [];
    throw error;
  }
  return cachedRecords;
}

export async function searchKnowledge(query, limit = 4) {
  const queryTerms = [...new Set(terms(query).filter(term => !STOP_WORDS.has(term)))];
  if (!queryTerms.length) return [];
  const records = await loadRecords();
  const ranked = records.map(record => {
    const titleTerms = new Set(terms(record.title));
    const keywordTerms = new Set(terms(Array.isArray(record.keywords) ? record.keywords.join(" ") : record.keywords));
    const contentTerms = new Set(terms([record.domain, record.subdomain, record.summary, record.content].join(" ")));
    let score = 0;
    let matched = 0;
    for (const term of queryTerms) {
      let weight = 0;
      if (titleTerms.has(term)) weight += 5;
      if (keywordTerms.has(term)) weight += 4;
      if (contentTerms.has(term)) weight += 1;
      if (weight) matched += 1;
      score += weight;
    }
    const coverage = matched / queryTerms.length;
    return { record, score: score * (0.5 + coverage / 2), coverage };
  }).filter(item => item.score > 0)
    .sort((a, b) => b.score - a.score || b.coverage - a.coverage || a.record.id.localeCompare(b.record.id));
  return ranked.slice(0, Math.max(0, Math.min(8, Number(limit) || 4))).map(item => item.record);
}
