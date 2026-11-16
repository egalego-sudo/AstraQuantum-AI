import { mkdir, readFile, appendFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../knowledge-base/inbox");
const OUTPUT = path.join(ROOT, "research-metadata.jsonl");
const MAX_RESULTS = 10;
const USER_AGENT = "QUANTUM-AI-Knowledge-Research/1.0 (metadata-only; respect provider terms)";

function clean(value, max = 500) {
  return String(value ?? "").replace(/\s+/g, " ").trim().slice(0, max);
}

export function normalizeCandidate({ provider, title, doi, authors = [], year, url }) {
  const safeDoi = clean(doi, 200).replace(/^https?:\/\/(dx\.)?doi\.org\//i, "");
  const safeTitle = clean(title, 700);
  if (!safeTitle) return null;
  return {
    id: safeDoi ? "doi:" + safeDoi.toLowerCase() : provider + ":" + encodeURIComponent(safeTitle.toLowerCase()).slice(0, 120),
    provider: clean(provider, 40),
    title: safeTitle,
    doi: safeDoi || null,
    authors: authors.map(name => clean(name, 120)).filter(Boolean).slice(0, 12),
    year: Number.isInteger(Number(year)) && Number(year) > 0 ? Number(year) : null,
    url: clean(url, 1000),
    retrieved_at: new Date().toISOString(),
    review_status: "unreviewed",
    usage_note: "Metadata candidate only. Verify the underlying source and license before creating a knowledge record."
  };
}

async function getJson(url) {
  const response = await fetch(url, {
    headers: { accept: "application/json", "user-agent": USER_AGENT },
    signal: AbortSignal.timeout(15000)
  });
  if (!response.ok) throw new Error("Provider returned HTTP " + response.status);
  return response.json();
}

async function openAlex(query, limit) {
  const url = new URL("https://api.openalex.org/works");
  url.searchParams.set("search", query);
  url.searchParams.set("per-page", String(limit));
  url.searchParams.set("select", "id,doi,title,publication_year,authorships,primary_location");
  if (process.env.OPENALEX_MAILTO) url.searchParams.set("mailto", process.env.OPENALEX_MAILTO);
  const data = await getJson(url);
  return (data.results || []).map(item => normalizeCandidate({
    provider: "OpenAlex",
    title: item.title,
    doi: item.doi,
    year: item.publication_year,
    authors: (item.authorships || []).map(a => a.author?.display_name),
    url: item.doi || item.primary_location?.landing_page_url || item.id
  })).filter(Boolean);
}

async function crossref(query, limit) {
  const url = new URL("https://api.crossref.org/works");
  url.searchParams.set("query", query);
  url.searchParams.set("rows", String(limit));
  url.searchParams.set("select", "DOI,title,author,published,URL");
  if (process.env.CROSSREF_MAILTO) url.searchParams.set("mailto", process.env.CROSSREF_MAILTO);
  const data = await getJson(url);
  return (data.message?.items || []).map(item => normalizeCandidate({
    provider: "Crossref",
    title: item.title?.[0],
    doi: item.DOI,
    year: item.published?.["date-parts"]?.[0]?.[0],
    authors: (item.author || []).map(a => [a.given, a.family].filter(Boolean).join(" ")),
    url: item.URL
  })).filter(Boolean);
}

async function europePmc(query, limit) {
  const url = new URL("https://www.ebi.ac.uk/europepmc/webservices/rest/search");
  url.searchParams.set("query", query);
  url.searchParams.set("format", "json");
  url.searchParams.set("pageSize", String(limit));
  url.searchParams.set("resultType", "core");
  const data = await getJson(url);
  return (data.resultList?.result || []).map(item => normalizeCandidate({
    provider: "Europe PMC",
    title: item.title,
    doi: item.doi,
    year: item.firstPublicationDate?.slice(0, 4) || item.pubYear,
    authors: (item.authorList?.author || []).map(a => a.fullName),
    url: item.doi ? "https://doi.org/" + item.doi : item.fullTextUrlList?.fullTextUrl?.[0]?.url || ""
  })).filter(Boolean);
}

export function deduplicateCandidates(candidates) {
  const seen = new Set();
  return candidates.filter(item => {
    const key = item.doi ? "doi:" + item.doi.toLowerCase() : item.id;
    if (seen.has(key)) return false;
    seen.add(key);
    return true;
  });
}

export async function collectResearchMetadata(query, limit = MAX_RESULTS) {
  const term = clean(query, 300);
  if (term.length < 3) throw new Error("Search query must contain at least 3 characters.");
  const bounded = Math.max(1, Math.min(MAX_RESULTS, Number(limit) || MAX_RESULTS));
  const candidates = [];
  const providers = [
    ["OpenAlex", () => openAlex(term, bounded)],
    ["Crossref", () => crossref(term, bounded)],
    ["Europe PMC", () => europePmc(term, bounded)]
  ];
  const errors = [];
  for (const [name, run] of providers) {
    try {
      candidates.push(...await run());
    } catch (error) {
      errors.push({ provider: name, message: clean(error.message, 240) });
    }
  }
  return { query: term, candidates: deduplicateCandidates(candidates), errors };
}

export async function appendToInbox(candidates, outputPath = OUTPUT) {
  await mkdir(path.dirname(outputPath), { recursive: true });
  let existing = "";
  try { existing = await readFile(outputPath, "utf8"); } catch (error) {
    if (error.code !== "ENOENT") throw error;
  }
  const existingKeys = new Set(existing.split(/\r?\n/).filter(Boolean).flatMap(line => {
    try {
      const item = JSON.parse(line);
      return [item.doi ? "doi:" + item.doi.toLowerCase() : item.id];
    } catch { return []; }
  }));
  const fresh = candidates.filter(item => {
    const key = item.doi ? "doi:" + item.doi.toLowerCase() : item.id;
    if (existingKeys.has(key)) return false;
    existingKeys.add(key);
    return true;
  });
  if (fresh.length) await appendFile(outputPath, fresh.map(item => JSON.stringify(item)).join("\n") + "\n", "utf8");
  return fresh.length;
}

async function main() {
  const args = process.argv.slice(2);
  const queryIndex = args.indexOf("--query");
  const query = queryIndex >= 0 ? args[queryIndex + 1] : "";
  if (!query) {
    console.error('Usage: npm run knowledge:collect -- --query "quantum mechanics"');
    process.exitCode = 2;
    return;
  }
  const result = await collectResearchMetadata(query);
  const added = await appendToInbox(result.candidates);
  console.log(JSON.stringify({
    query: result.query,
    fetched: result.candidates.length,
    added_to_inbox: added,
    errors: result.errors,
    output: "knowledge-base/inbox/research-metadata.jsonl",
    note: "Metadata only. Nothing is added to the searchable knowledge corpus until a human reviews the source and license."
  }, null, 2));
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  main().catch(error => {
    console.error(error.message);
    process.exitCode = 1;
  });
}
