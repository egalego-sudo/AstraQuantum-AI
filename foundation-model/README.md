# QUANTUM AI — Foundation Model Lab

**Creator by Bittu Pandit**

This directory is an original, small-scale research implementation of a decoder-only Transformer language model. It is a foundation for learning, experimentation and reproducible engineering—not a copy of GPT-6 and not a claim of GPT-6-level capability.

## Scope and limitations

- Implements byte-level UTF-8 tokenization, causal self-attention, pre-normalized Transformer blocks, next-token training and autoregressive sampling.
- Includes `load_checkpoint()` with PyTorch's restricted `weights_only=True` loader and validation of format, tokenizer, config, tensor keys/shapes, finite values and tied embeddings.
- Starts with randomly initialized weights. It does not include pretrained weights, proprietary model code, hidden prompts, private datasets or OpenAI model parameters.
- The default model is intentionally tiny for correctness tests and educational experiments. It is not a competitive assistant and cannot be expected to answer general questions until trained on a large, lawful, high-quality corpus.
- Training quality depends on data quality, tokenizer design, compute, optimization, evaluation and safety work. A successful unit test proves software behavior only—not intelligence.
- Never place API keys, personal data, copyrighted bulk corpora without rights, or production user conversations in training data.

## Requirements

Python 3.10+ and PyTorch 2.2+.

```bash
python -m pip install torch
python -m unittest discover -s foundation-model -p 'test_*.py' -v
```

## Train a small experiment

Prepare a UTF-8 plain-text file that you have the right to use:

```bash
python foundation-model/train.py --text-file ./your-lawful-corpus.txt --dataset-id YOUR_APPROVED_DATASET_ID --steps 500 --batch-size 16 --block-size 128 --output ./quantum-small.pt
```

The script first checks `foundation-model/datasets/manifest.json` (or `--manifest`) and refuses training unless `--dataset-id` identifies a record with `usage=train`, `review_status=approved`, `split=train`, and an exact matching SHA-256. The script then splits the text sequentially into train/validation regions, reports loss, clips gradients and saves model configuration plus state dict. Each checkpoint also records the approved dataset ID, SHA-256, license ID, source URL, and key training hyperparameters so an experiment can be audited later. Load a saved research checkpoint with `from core import load_checkpoint; model, metadata = load_checkpoint('./quantum-small.pt')`; the loader rejects unsupported formats and malformed weights. The validation split is not a substitute for a held-out benchmark. Use small values first; training memory grows with batch size, context length, width and layer count.

## Next research gates

1. Curate and document licensed multilingual datasets with provenance and deduplication; training now requires an approved manifest record and matching artifact SHA-256.
2. Evaluate tokenizer alternatives for Hindi/English and code.
3. Add deterministic benchmark suites and contamination checks.
4. Profile memory/latency and choose realistic training hardware.
5. Add safety tuning, instruction tuning and human evaluation only after base-model validation.
6. Compare against public baselines using identical held-out tasks and disclose compute/data.

**Honesty rule:** call this an experimental Transformer implementation until a trained checkpoint and independent evaluations justify stronger claims.

## Held-out benchmark evaluation

The evaluation CLI measures byte-level next-token cross-entropy, perplexity,
next-token accuracy, evaluation throughput, and autoregressive generation
throughput. It requires a separate, human-approved manifest record with
`usage=evaluate` and `split=validation` or `split=test`; the exact artifact
SHA-256 is checked before evaluation. Keep this corpus held out from training.

```bash
python foundation-model/evaluate.py \
  --checkpoint ./quantum-small.pt \
  --text-file ./held-out-test.txt \
  --dataset-id YOUR_APPROVED_EVALUATION_DATASET_ID \
  --manifest foundation-model/datasets/manifest.json \
  --output ./quantum-evaluation.json \
  --cpu
```

Results include model configuration, parameter count, dataset provenance,
hardware/device, seed, and checkpoint training metadata. Compare models only
on the same frozen test artifact and settings; do not tune on the test split.
Perplexity and token accuracy measure byte-level language modeling, not
reasoning, factuality, instruction following, safety, or general intelligence.
The generated text is not quality-scored by this harness. A separate curated
task suite and human review are required for those claims.

## Curated reasoning task benchmark

For a task-level check, prepare a UTF-8 JSONL file with one object per line.
Each object must contain exactly `id`, `category`, `prompt`, and
`reference`. Use human-reviewed, unambiguous questions and expected answers.
Register the exact JSONL artifact in the dataset manifest with
`usage=evaluate`, `review_status=approved`, and a held-out
`split=validation` or `split=test` before running.

```bash
python foundation-model/evaluate_reasoning.py \
  --checkpoint ./quantum-small.pt \
  --task-file ./reasoning-heldout.jsonl \
  --dataset-id YOUR_APPROVED_REASONING_DATASET_ID \
  --manifest foundation-model/datasets/manifest.json \
  --output ./reasoning-report.json \
  --max-new-tokens 24 \
  --cpu
```

The report includes exact-match accuracy overall and by category, raw model
outputs, task references, dataset provenance, checkpoint metadata, and seed.
Normalization only case-folds Unicode, trims ends, and collapses whitespace;
it does not forgive semantically equivalent wording or alternate numeric
formats. This deliberately strict score needs human review for ambiguous
answers. Do not tune on the test split. The current randomly initialized
research model is expected to perform poorly until it is trained; this harness
measures performance and does not itself improve model capability.


## Deterministic corpus preparation (approval remains manual)

For a small, auditable first pass, prepare a UTF-8 text file with one
document/example per line. The utility normalizes Unicode to NFC, removes
blank lines and exact duplicate documents, then makes a deterministic,
seeded train/validation split. It writes SHA-256 hashes and a manifest
template for both output artifacts.

```bash
python foundation-model/datasets/prepare_corpus.py \
  --input ./lawful-source.txt \
  --train-output ./prepared/train.txt \
  --validation-output ./prepared/validation.txt \
  --manifest-output ./prepared/manifest-draft.json \
  --dataset-prefix my-reviewed-corpus \
  --source-url https://example.org/source-description \
  --license-id CC-BY-4.0 \
  --license-url https://creativecommons.org/licenses/by/4.0/ \
  --languages hi,en \
  --validation-fraction 0.1 \
  --seed 2026
```

**Important:** generated manifest records deliberately use
`usage=not_permitted` and `review_status=pending`. This tool does not
grant copyright permission or approve data. A human must verify source
rights, license compatibility, attribution, privacy/consent, personal or
sensitive data, quality, contamination, and split suitability. Only then
may an authorized maintainer edit the manifest to reflect the actual
permissions and approval decision. The current utility performs exact
deduplication only; it does not detect paraphrases, benchmark leakage,
personal information, or harmful content. Keep the original source and
document your review.
