# QUANTUM AI — Foundation Model Lab

**Creator by Bittu Pandit**

This directory is an original, small-scale research implementation of a decoder-only Transformer language model. It is a foundation for learning, experimentation and reproducible engineering—not a copy of GPT-6 and not a claim of GPT-6-level capability.

## Scope and limitations

- Implements byte-level UTF-8 tokenization, causal self-attention, pre-normalized Transformer blocks, next-token training and autoregressive sampling.
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
python foundation-model/train.py --text-file ./your-lawful-corpus.txt --steps 500 --batch-size 16 --block-size 128 --output ./quantum-small.pt
```

The script splits the text sequentially into train/validation regions, reports loss, clips gradients and saves model configuration plus state dict. The validation split is not a substitute for a held-out benchmark. Use small values first; training memory grows with batch size, context length, width and layer count.

## Next research gates

1. Curate and document licensed multilingual datasets with provenance and deduplication.
2. Evaluate tokenizer alternatives for Hindi/English and code.
3. Add deterministic benchmark suites and contamination checks.
4. Profile memory/latency and choose realistic training hardware.
5. Add safety tuning, instruction tuning and human evaluation only after base-model validation.
6. Compare against public baselines using identical held-out tasks and disclose compute/data.

**Honesty rule:** call this an experimental Transformer implementation until a trained checkpoint and independent evaluations justify stronger claims.
