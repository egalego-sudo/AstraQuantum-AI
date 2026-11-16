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
        if (!record || !record.id || !record.title || !record.content || !Array.isArray(record.sources) || !record.sources.length || !["source_checked", "expert_reviewed"].includes(record.review_status) || !record.sources.every(source => source && typeof source.publisher === "string" && typeof source.url === "string" && source.url.startsWith("https://")) || seen.has(record.id)) return [];
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


export function validateKnowledgeCitations(answer, records = []) {
  const byId = new Map(records.map(record => [record.id, record]));
  const citedIds = new Set();
  const safeAnswer = String(answer || "").replace(/\\[KB:([^\\]\\s]+)\\]/g, (marker, id) => {
    if (!byId.has(id)) return "[unverified knowledge-base reference removed]";
    citedIds.add(id);
    return marker;
  });
  if (!citedIds.size) return safeAnswer;
  const references = [];
  const seenUrls = new Set();
  for (const id of citedIds) {
    const record = byId.get(id);
    for (const source of record.sources || []) {
      if (!source || typeof source.url !== "string" || !source.url.startsWith("https://") || seenUrls.has(source.url)) continue;
      seenUrls.add(source.url);
      references.push("- [KB:" + id + "] " + record.title + " — " + (source.publisher || "Source") + ": " + source.url);
    }
  }
  return safeAnswer + (references.length ? "\\n\\nVerified knowledge-base sources:\\n" + references.join("\\n") : "");
}
