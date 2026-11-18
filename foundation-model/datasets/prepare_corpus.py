"""Prepare auditable line-delimited text splits; never auto-approve data for training."""
from __future__ import annotations

import argparse
import hashlib
import json
import random
import unicodedata
from pathlib import Path
from urllib.parse import urlparse


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def prepare_corpus(
    source: Path,
    train_output: Path,
    validation_output: Path,
    manifest_output: Path,
    dataset_prefix: str,
    source_url: str,
    license_id: str,
    license_url: str,
    languages: list[str],
    validation_fraction: float = 0.1,
    seed: int = 2026,
    max_input_mb: int = 128,
    max_documents: int = 1_000_000,
) -> dict:
    if not 0.01 <= validation_fraction <= 0.5:
        raise ValueError("validation_fraction must be between 0.01 and 0.5")
    if seed < 0 or max_input_mb < 1 or max_documents < 2:
        raise ValueError("seed must be non-negative and size limits positive")
    if not dataset_prefix.strip():
        raise ValueError("dataset_prefix must be non-empty")
    for label, url in (("source_url", source_url), ("license_url", license_url)):
        parsed = urlparse(url)
        if parsed.scheme != "https" or not parsed.netloc:
            raise ValueError(f"{label} must be a valid HTTPS URL")
    if not license_id.strip() or not languages or any(not item.strip() for item in languages):
        raise ValueError("license_id and language tags must be supplied")
    outputs = [train_output.resolve(), validation_output.resolve(), manifest_output.resolve()]
    if len(set(outputs)) != 3 or source.resolve() in outputs:
        raise ValueError("source and all output paths must be distinct")

    if source.stat().st_size > max_input_mb * 1024 * 1024:
        raise ValueError(f"input exceeds configured {max_input_mb} MiB safety limit")

    documents: list[str] = []
    seen: set[str] = set()
    duplicate_count = 0
    blank_count = 0
    with source.open("r", encoding="utf-8-sig", newline="") as stream:
        for line_number, line in enumerate(stream, start=1):
            normalized = unicodedata.normalize("NFC", line).strip()
            if not normalized:
                blank_count += 1
                continue
            if len(normalized) > 100_000:
                raise ValueError(f"document on line {line_number} exceeds 100,000 characters")
            digest = hashlib.sha256(normalized.encode("utf-8")).hexdigest()
            if digest in seen:
                duplicate_count += 1
                continue
            seen.add(digest)
            documents.append(normalized)
            if len(documents) > max_documents:
                raise ValueError(f"document count exceeds safety limit of {max_documents}")

    if len(documents) < 2:
        raise ValueError("at least two unique non-empty documents are required")
    random.Random(seed).shuffle(documents)
    validation_count = max(1, round(len(documents) * validation_fraction))
    validation_count = min(validation_count, len(documents) - 1)
    validation_docs = documents[:validation_count]
    train_docs = documents[validation_count:]

    train_output.parent.mkdir(parents=True, exist_ok=True)
    validation_output.parent.mkdir(parents=True, exist_ok=True)
    manifest_output.parent.mkdir(parents=True, exist_ok=True)
    train_output.write_text("\n".join(train_docs) + "\n", encoding="utf-8")
    validation_output.write_text("\n".join(validation_docs) + "\n", encoding="utf-8")

    records = []
    for suffix, path, split in (
        ("train", train_output, "train"),
        ("validation", validation_output, "validation"),
    ):
        records.append({
            "id": f"{dataset_prefix}-{suffix}",
            "name": f"{dataset_prefix} {suffix} split (review required)",
            "source_url": source_url,
            "license_id": license_id,
            "license_url": license_url,
            "languages": languages,
            "sha256": sha256_file(path),
            "usage": "not_permitted",
            "split": split,
            "attribution": "REPLACE WITH REQUIRED ATTRIBUTION AFTER RIGHTS REVIEW",
            "review_status": "pending",
            "size_bytes": path.stat().st_size,
        })
    manifest = {"manifest_version": 1, "datasets": records}
    manifest_output.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    return {
        "input_documents": len(documents) + duplicate_count,
        "unique_documents": len(documents),
        "duplicate_documents_removed": duplicate_count,
        "blank_lines_removed": blank_count,
        "train_documents": len(train_docs),
        "validation_documents": len(validation_docs),
        "seed": seed,
        "train_sha256": records[0]["sha256"],
        "validation_sha256": records[1]["sha256"],
        "manifest_template": str(manifest_output),
        "approval_status": "NOT APPROVED: review rights, license, attribution, privacy, quality and contamination before changing manifest fields",
    }


def parse_args():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", required=True, help="UTF-8 text, one document per line")
    parser.add_argument("--train-output", required=True)
    parser.add_argument("--validation-output", required=True)
    parser.add_argument("--manifest-output", required=True)
    parser.add_argument("--dataset-prefix", required=True)
    parser.add_argument("--source-url", required=True)
    parser.add_argument("--license-id", required=True)
    parser.add_argument("--license-url", required=True)
    parser.add_argument("--languages", required=True, help="comma-separated BCP-47-like tags, e.g. hi,en")
    parser.add_argument("--validation-fraction", type=float, default=0.1)
    parser.add_argument("--seed", type=int, default=2026)
    parser.add_argument("--max-input-mb", type=int, default=128)
    parser.add_argument("--max-documents", type=int, default=1_000_000)
    return parser.parse_args()


if __name__ == "__main__":
    args = parse_args()
    report = prepare_corpus(
        Path(args.input), Path(args.train_output), Path(args.validation_output),
        Path(args.manifest_output), args.dataset_prefix, args.source_url,
        args.license_id, args.license_url,
        [item.strip() for item in args.languages.split(",") if item.strip()],
        args.validation_fraction, args.seed, args.max_input_mb, args.max_documents,
    )
    print(json.dumps(report, ensure_ascii=False, indent=2))
