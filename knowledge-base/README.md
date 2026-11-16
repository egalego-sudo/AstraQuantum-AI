# QUANTUM AI Knowledge Base

**Creator by Bittu Pandit**

This directory is the versioned foundation for QUANTUM AI's source-attributed knowledge collection. It is data for retrieval (RAG), not model weights and not a pretrained foundation model.

## Design goals

- Prefer primary and authoritative sources; retain source URL, publisher, access date, language and license/usage notes.
- Store atomic, independently retrievable knowledge records rather than huge unstructured text dumps.
- Keep evidence separate from generated explanations. Never present an uncited generated claim as verified fact.
- Support Hindi and English metadata and retrieval; preserve original source wording where licensing permits.
- Record uncertainty, date sensitivity and review status.
- Do not ingest private, paywalled, copyrighted bulk text or personal data without permission.
- Keep provider credentials out of this repository.

## Layout

- `schema/knowledge-record.schema.json`: record contract.
- `sources/source-registry.json`: initial public-source registry and intended use.
- `seed/foundations.jsonl`: 22 starter records in English and Hindi, with source links.
- Future ingested material should be placed in domain-specific partitions (for example `science/`, `math/`, `computing/`, `history/`) with provenance retained.

## Retrieval pipeline (implementation plan)

1. Fetch only from approved public APIs/datasets and respect their terms, rate limits and attribution rules.
2. Normalize text and metadata; deduplicate by DOI, canonical URL or content hash.
3. Split long documents into bounded passages while preserving document and page/section references.
4. Validate each record against the JSON Schema.
5. Index lexical search first; add multilingual embeddings/vector search only when a compatible model and storage are configured.
6. Retrieve a small set of relevant passages and pass them to the answer model with explicit source IDs.
7. Return citations and label claims as supported, partly supported, contradicted or unverified.
8. Evaluate retrieval precision, citation correctness, freshness and Hindi/English parity before release.

## Current status

The current starter corpus contains 22 records across English and Hindi, including mathematics, physics, chemistry, biology, astronomy and computing. It establishes a repository data contract and a small reviewed seed set; it is not yet a comprehensive encyclopedia. It does **not** yet connect this directory to the running backend, create embeddings, train model weights, or claim a large-scale corpus. Those require separate implementation, tests and a build.

## Record fields

Every record includes a stable ID, topic, language, claim/text, source references, evidence type, review status and optional validity dates. Source provenance must travel with the record into the final answer.

## Search behavior

The backend currently uses bounded local lexical retrieval with title/keyword weighting, Unicode-aware tokenization and a small bilingual stop-word list. It returns at most eight records (normally four are passed into generation). This is not semantic/vector search; Hindi-English transliteration and broad paraphrase matching remain future work.
