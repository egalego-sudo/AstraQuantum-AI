export function classifyCrossrefDoiResult({ httpStatus, crossrefTitle, titleSimilarity = 0 }) {
  const status = Number(httpStatus) || 0;
  if (status === 404) return { verified: false, status: "CROSSREF_DOI_NOT_FOUND" };
  if (status < 200 || status >= 300) return { verified: null, status: "CROSSREF_LOOKUP_FAILED" };
  if (!String(crossrefTitle || "").trim()) return { verified: null, status: "CROSSREF_RECORD_WITHOUT_TITLE" };
  const verified = Number(titleSimilarity) >= 0.7;
  return { verified, status: verified ? "CROSSREF_TITLE_MATCH" : "CROSSREF_TITLE_MISMATCH" };
}
