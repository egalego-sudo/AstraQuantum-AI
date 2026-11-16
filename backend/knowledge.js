import { readFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import path from "node:path";

const seedPath = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../knowledge-base/seed/foundations.jsonl");
let cachedRecords;

function terms(value) {
  return String(value || "").toLocaleLowerCase().match(/[\p{L}\p{N}]+/gu) || [];
}

async function loadRecords() {
  if (cachedRecords) return cachedRecords;
  try {
    const raw = await readFile(seedPath, "utf8");
    cachedRecords = raw.split(/\r?\n/).filter(Boolean).flatMap(line => {
      try {
        const record = JSON.parse(line);
        return record && record.id && record.title && record.content && Array.isArray(record.sources) ? [record] : [];
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
  const queryTerms = [...new Set(terms(query))];
  if (!queryTerms.length) return [];
  const records = await loadRecords();
  const ranked = records.map(record => {
    const haystack = terms([record.title, record.domain, record.subdomain, record.content, ...(record.keywords || [])].join(" "));
    const counts = new Map();
    for (const term of haystack) counts.set(term, (counts.get(term) || 0) + 1);
    const score = queryTerms.reduce((sum, term) => sum + (counts.has(term) ? Math.min(3, counts.get(term)) : 0), 0);
    return { record, score };
  }).filter(item => item.score > 0).sort((a, b) => b.score - a.score || a.record.id.localeCompare(b.record.id));
  return ranked.slice(0, Math.max(0, Math.min(8, Number(limit) || 4))).map(item => item.record);
}
