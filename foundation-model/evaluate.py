"""Reproducible held-out evaluation for QUANTUM AI research checkpoints."""
from __future__ import annotations

import argparse
import hashlib
import json
import math
import time
from pathlib import Path

import torch
import torch.nn.functional as F

from core import ByteTokenizer, load_checkpoint
from datasets.validate_manifest import verify_evaluation_corpus


@torch.no_grad()
def evaluate_checkpoint(
    checkpoint_path: Path,
    corpus_path: Path,
    dataset_id: str,
    manifest_path: Path,
    device: torch.device,
    max_corpus_mb: int = 64,
    generation_tokens: int = 32,
    seed: int = 2026,
) -> dict[str, object]:
    if max_corpus_mb < 1 or generation_tokens < 0 or seed < 0:
        raise ValueError("max_corpus_mb must be positive; generation_tokens and seed must be non-negative")
    max_bytes = max_corpus_mb * 1024 * 1024
    if corpus_path.stat().st_size > max_bytes:
        raise ValueError(f"evaluation corpus exceeds configured {max_corpus_mb} MiB safety limit")
    record = verify_evaluation_corpus(manifest_path, dataset_id, corpus_path)
    with corpus_path.open("rb") as stream:
        raw = stream.read(max_bytes + 1)
    if len(raw) > max_bytes:
        raise ValueError(f"evaluation corpus exceeds configured {max_corpus_mb} MiB safety limit")
    digest = hashlib.sha256(raw).hexdigest()
    if digest != record["sha256"]:
        raise ValueError("evaluation corpus changed after manifest verification")
    text = raw.decode("utf-8")
    tokenizer = ByteTokenizer()
    tokens = torch.tensor(tokenizer.encode(text), dtype=torch.long)
    if tokens.numel() < 3:
        raise ValueError("evaluation corpus must contain at least two UTF-8 bytes")

    model, checkpoint_metadata = load_checkpoint(checkpoint_path, map_location=device)
    model.to(device)
    model.eval()
    block_size = model.config.block_size

    total_loss = 0.0
    total_correct = 0
    total_targets = 0
    started = time.perf_counter()
    for start in range(0, tokens.numel() - 1, block_size):
        x = tokens[start : start + block_size]
        y = tokens[start + 1 : start + block_size + 1]
        if x.numel() == 0 or y.numel() == 0:
            continue
        length = min(x.numel(), y.numel())
        x = x[:length].unsqueeze(0).to(device)
        y = y[:length].unsqueeze(0).to(device)
        logits, _ = model(x)
        total_loss += float(F.cross_entropy(logits.reshape(-1, logits.size(-1)), y.reshape(-1), reduction="sum").item())
        total_correct += int((logits.argmax(dim=-1) == y).sum().item())
        total_targets += length
    evaluation_seconds = time.perf_counter() - started
    mean_loss = total_loss / total_targets
    perplexity = math.exp(min(mean_loss, 80.0))

    generation_seconds = 0.0
    generated_count = 0
    if generation_tokens:
        torch.manual_seed(seed)
        prompt = tokens[: min(8, tokens.numel())].unsqueeze(0).to(device)
        before = time.perf_counter()
        generated = model.generate(prompt, max_new_tokens=generation_tokens, temperature=1.0, top_k=40)
        generation_seconds = time.perf_counter() - before
        generated_count = int(generated.shape[1] - prompt.shape[1])

    dataset_meta = checkpoint_metadata.get("dataset", {})
    checkpoint_dataset_id = dataset_meta.get("id") if isinstance(dataset_meta, dict) else None
    return {
        "benchmark": "quantum-heldout-byte-lm-v1",
        "model": {
            "parameters": model.parameter_count(),
            "config": model.config_dict(),
            "checkpoint_dataset_id": checkpoint_dataset_id,
        },
        "evaluation_dataset": {
            "id": record["id"],
            "sha256": digest,
            "license_id": record["license_id"],
            "split": record["split"],
            "bytes": len(raw),
            "tokens_including_bos": int(tokens.numel()),
        },
        "metrics": {
            "next_token_cross_entropy_nats": mean_loss,
            "perplexity": perplexity,
            "next_token_accuracy": total_correct / total_targets,
            "evaluated_targets": total_targets,
            "evaluation_seconds": evaluation_seconds,
            "evaluation_tokens_per_second": total_targets / max(evaluation_seconds, 1e-12),
            "generated_tokens": generated_count,
            "generation_seconds": generation_seconds,
            "generation_tokens_per_second": generated_count / max(generation_seconds, 1e-12) if generated_count else 0.0,
        },
        "reproducibility": {
            "seed": seed,
            "device": str(device),
            "checkpoint": str(checkpoint_path),
            "checkpoint_training_metadata": checkpoint_metadata.get("training", {}),
        },
        "interpretation": "Byte-level held-out language-model metrics only; not a measure of general intelligence, factuality, reasoning, safety, or assistant quality.",
    }


def write_report(report: dict[str, object], output_path: Path) -> Path:
    """Write standards-compliant UTF-8 JSON followed by one real newline."""
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return output_path

def parse_args():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--checkpoint", required=True)
    parser.add_argument("--text-file", required=True, help="held-out UTF-8 evaluation corpus")
    parser.add_argument("--dataset-id", required=True, help="approved evaluation dataset ID in manifest")
    parser.add_argument("--manifest", default=str(Path(__file__).resolve().parent / "datasets" / "manifest.json"))
    parser.add_argument("--output", default="quantum-evaluation.json")
    parser.add_argument("--max-corpus-mb", type=int, default=64)
    parser.add_argument("--generation-tokens", type=int, default=32)
    parser.add_argument("--seed", type=int, default=2026)
    parser.add_argument("--cpu", action="store_true")
    return parser.parse_args()


if __name__ == "__main__":
    args = parse_args()
    device = torch.device("cuda" if torch.cuda.is_available() and not args.cpu else "cpu")
    result = evaluate_checkpoint(
        Path(args.checkpoint), Path(args.text_file), args.dataset_id,
        Path(args.manifest), device, args.max_corpus_mb,
        args.generation_tokens, args.seed,
    )
    write_report(result, Path(args.output))
    print(json.dumps(result, ensure_ascii=False, indent=2))
