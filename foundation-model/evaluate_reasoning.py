"""Governed exact-match reasoning task benchmark for QUANTUM AI checkpoints."""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import time
from collections import defaultdict
from pathlib import Path

import torch

from core import ByteTokenizer, load_checkpoint
from datasets.validate_manifest import verify_evaluation_corpus


def normalize_answer(value: str) -> str:
    """Conservative normalization: Unicode casefold, trim, collapse whitespace."""
    return " ".join(value.casefold().strip().split())


def load_tasks(raw: bytes, max_tasks: int = 500) -> list[dict[str, str]]:
    tasks: list[dict[str, str]] = []
    seen: set[str] = set()
    try:
        decoded = raw.decode("utf-8")
    except UnicodeDecodeError as exc:
        raise ValueError("reasoning task file must be UTF-8") from exc
    for line_number, line in enumerate(decoded.splitlines(), start=1):
        if not line.strip():
            continue
        if len(tasks) >= max_tasks:
            raise ValueError(f"reasoning task count exceeds safety limit of {max_tasks}")
        try:
            item = json.loads(line)
        except json.JSONDecodeError as exc:
            raise ValueError(f"invalid JSON on task line {line_number}") from exc
        if not isinstance(item, dict):
            raise ValueError(f"task line {line_number} must be a JSON object")
        if set(item) != {"id", "category", "prompt", "reference"}:
            raise ValueError(f"task line {line_number} must contain exactly id, category, prompt, reference")
        for key in ("id", "category", "prompt", "reference"):
            if not isinstance(item[key], str) or not item[key].strip():
                raise ValueError(f"task line {line_number}: {key} must be a non-empty string")
        if item["id"] in seen:
            raise ValueError(f"duplicate task id: {item['id']}")
        seen.add(item["id"])
        if len(item["prompt"]) > 4000 or len(item["reference"]) > 1000:
            raise ValueError(f"task line {line_number} exceeds text length limits")
        tasks.append(item)
    if not tasks:
        raise ValueError("reasoning task file contains no tasks")
    return tasks


@torch.no_grad()
def evaluate_reasoning_tasks(
    checkpoint_path: Path,
    task_file: Path,
    dataset_id: str,
    manifest_path: Path,
    device: torch.device,
    max_corpus_mb: int = 8,
    max_tasks: int = 500,
    max_new_tokens: int = 24,
    seed: int = 2026,
) -> dict[str, object]:
    if max_corpus_mb < 1 or max_tasks < 1 or max_new_tokens < 1 or seed < 0:
        raise ValueError("limits must be positive and seed must be non-negative")
    max_bytes = max_corpus_mb * 1024 * 1024
    if task_file.stat().st_size > max_bytes:
        raise ValueError(f"task file exceeds configured {max_corpus_mb} MiB safety limit")
    record = verify_evaluation_corpus(manifest_path, dataset_id, task_file)
    raw = task_file.read_bytes()
    if len(raw) > max_bytes or hashlib.sha256(raw).hexdigest() != record["sha256"]:
        raise ValueError("task file changed after manifest verification or exceeds size limit")
    tasks = load_tasks(raw, max_tasks=max_tasks)

    model, checkpoint_metadata = load_checkpoint(checkpoint_path, map_location=device)
    model.to(device)
    model.eval()
    tokenizer = ByteTokenizer()
    torch.manual_seed(seed)
    category_counts: dict[str, list[int]] = defaultdict(lambda: [0, 0])
    results: list[dict[str, object]] = []
    started = time.perf_counter()

    for task in tasks:
        prompt_ids = tokenizer.encode(task["prompt"])
        prompt_ids = prompt_ids[-model.config.block_size:]
        input_ids = torch.tensor([prompt_ids], dtype=torch.long, device=device)
        generated = model.generate(input_ids, max_new_tokens=max_new_tokens, temperature=1.0, top_k=40)
        completion = tokenizer.decode(generated[0, len(prompt_ids):].tolist())
        expected = normalize_answer(task["reference"])
        actual = normalize_answer(completion)
        correct = actual == expected
        category_counts[task["category"]][1] += 1
        category_counts[task["category"]][0] += int(correct)
        results.append({
            "id": task["id"],
            "category": task["category"],
            "reference": task["reference"],
            "prediction": completion,
            "exact_match": correct,
        })

    elapsed = time.perf_counter() - started
    total_correct = sum(value[0] for value in category_counts.values())
    total = len(tasks)
    categories = {
        name: {"correct": counts[0], "total": counts[1], "exact_match_accuracy": counts[0] / counts[1]}
        for name, counts in sorted(category_counts.items())
    }
    return {
        "benchmark": "quantum-reasoning-exact-match-v1",
        "model": {"parameters": model.parameter_count(), "config": model.config_dict()},
        "dataset": {"id": record["id"], "sha256": record["sha256"], "license_id": record["license_id"], "split": record["split"]},
        "metrics": {
            "tasks": total,
            "exact_match_correct": total_correct,
            "exact_match_accuracy": total_correct / total,
            "category_scores": categories,
            "elapsed_seconds": elapsed,
            "tasks_per_second": total / max(elapsed, 1e-12),
        },
        "reproducibility": {
            "seed": seed,
            "device": str(device),
            "max_new_tokens": max_new_tokens,
            "normalization": "Unicode casefold, trim, collapse whitespace; exact string equality",
            "checkpoint": str(checkpoint_path),
            "checkpoint_training_metadata": checkpoint_metadata.get("training", {}),
        },
        "task_results": results,
        "interpretation": "Exact-match scores on this task file only. They do not establish broad reasoning ability, factuality, safety, or general intelligence. Review prompts, references, and outputs for ambiguity and contamination.",
    }


def parse_args():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--checkpoint", required=True)
    parser.add_argument("--task-file", required=True, help="JSONL file with id/category/prompt/reference")
    parser.add_argument("--dataset-id", required=True, help="approved evaluation dataset ID")
    parser.add_argument("--manifest", default=str(Path(__file__).resolve().parent / "datasets" / "manifest.json"))
    parser.add_argument("--output", default="quantum-reasoning-evaluation.json")
    parser.add_argument("--max-corpus-mb", type=int, default=8)
    parser.add_argument("--max-tasks", type=int, default=500)
    parser.add_argument("--max-new-tokens", type=int, default=24)
    parser.add_argument("--seed", type=int, default=2026)
    parser.add_argument("--cpu", action="store_true")
    return parser.parse_args()


if __name__ == "__main__":
    args = parse_args()
    device = torch.device("cuda" if torch.cuda.is_available() and not args.cpu else "cpu")
    report = evaluate_reasoning_tasks(
        Path(args.checkpoint), Path(args.task_file), args.dataset_id, Path(args.manifest),
        device, args.max_corpus_mb, args.max_tasks, args.max_new_tokens, args.seed,
    )
    output = Path(args.output)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False, indent=2))
