# QUANTUM AI — Draft Bilingual Reasoning Benchmark v1

**Creator by Bittu Pandit**

## Status: DRAFT — NOT APPROVED

This is an original, assistant-authored draft task set prepared for human review. It is not copied from an external benchmark. It is not in the dataset manifest and cannot be used by the governed evaluation CLI until the prompts, references, answer conventions, language quality and rights are reviewed and explicitly approved.

## Contents

- 24 short exact-answer tasks.
- English and Hindi prompts.
- Categories: logic, mathematics, physics, chemistry, biology, astronomy, computer science, programming, comprehension and arithmetic.
- Intended for smoke-testing answer formatting and basic reasoning only.

## Important limitations

1. Exact-match scoring is strict. Equivalent answers with different wording or notation may be scored wrong.
2. This is a small, low-difficulty draft and is not representative of broad intelligence, expert knowledge, factuality, safety or robustness.
3. Some tasks are intentionally deterministic arithmetic or textbook facts; this does not measure research ability or long-form reasoning.
4. Human reviewers must independently verify every reference answer and check Hindi naturalness, ambiguity, cultural assumptions and category balance.
5. Do not add a manifest entry with `review_status=approved` until that review and the permitted-use decision are complete.

## File

`reasoning-benchmark-v1.jsonl` uses the exact schema required by `evaluate_reasoning.py`: `id`, `category`, `prompt`, `reference`.

## Approval path

After review, record the dataset SHA-256, source/provenance, license decision, language coverage, intended use and split in the manifest. Keep this draft separate from training data; it is intended for a held-out evaluation split only.
