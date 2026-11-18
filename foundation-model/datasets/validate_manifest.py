"""Validate the QUANTUM AI dataset governance manifest using only the stdlib."""
from __future__ import annotations

import hashlib
import json
import re
import sys
from pathlib import Path
from urllib.parse import urlparse

MANIFEST = Path(__file__).with_name("manifest.json")
HEX_SHA256 = re.compile(r"^[0-9a-f]{64}$")
ALLOWED_USAGE = {"train", "evaluate", "reference_only", "not_permitted"}
ALLOWED_SPLITS = {"train", "validation", "test", "unspecified"}
ALLOWED_REVIEW = {"approved", "pending", "rejected"}

REQUIRED = {
    "id", "name", "source_url", "license_id", "license_url",
    "languages", "sha256", "usage", "split", "attribution",
    "review_status",
}


def _https_url(value: object) -> bool:
    if not isinstance(value, str):
        return False
    parsed = urlparse(value)
    return parsed.scheme == "https" and bool(parsed.netloc)


def validate_manifest(data: object) -> list[str]:
    errors: list[str] = []
    if not isinstance(data, dict):
        return ["manifest root must be an object"]
    if type(data.get("manifest_version")) is not int or data.get("manifest_version") != 1:
        errors.append("manifest_version must equal 1")
    datasets = data.get("datasets")
    if not isinstance(datasets, list):
        return errors + ["datasets must be an array"]

    seen_ids: set[str] = set()
    seen_hashes: set[str] = set()
    for index, record in enumerate(datasets):
        prefix = f"datasets[{index}]"
        if not isinstance(record, dict):
            errors.append(f"{prefix} must be an object")
            continue
        missing = REQUIRED - record.keys()
        if missing:
            errors.append(f"{prefix} missing fields: {', '.join(sorted(missing))}")
            continue

        identifier = record["id"]
        if not isinstance(identifier, str) or not identifier.strip():
            errors.append(f"{prefix}.id must be a non-empty string")
        elif identifier in seen_ids:
            errors.append(f"{prefix}.id is duplicated: {identifier}")
        else:
            seen_ids.add(identifier)

        for field in ("name", "license_id", "attribution"):
            if not isinstance(record[field], str) or not record[field].strip():
                errors.append(f"{prefix}.{field} must be a non-empty string")

        for field in ("source_url", "license_url"):
            if not _https_url(record[field]):
                errors.append(f"{prefix}.{field} must be an HTTPS URL")

        languages = record["languages"]
        if (
            not isinstance(languages, list)
            or not languages
            or any(not isinstance(lang, str) or not re.fullmatch(r"[A-Za-z]{2,8}(?:-[A-Za-z0-9]{1,8})*", lang) for lang in languages)
        ):
            errors.append(f"{prefix}.languages must be a non-empty list of BCP-47-like tags")

        digest = record["sha256"]
        if not isinstance(digest, str) or not HEX_SHA256.fullmatch(digest):
            errors.append(f"{prefix}.sha256 must be 64 lowercase hexadecimal characters")
        elif digest in seen_hashes:
            errors.append(f"{prefix}.sha256 is duplicated")
        else:
            seen_hashes.add(digest)

        if not isinstance(record["usage"], str) or record["usage"] not in ALLOWED_USAGE:
            errors.append(f"{prefix}.usage is not an allowed explicit permission")
        if not isinstance(record["split"], str) or record["split"] not in ALLOWED_SPLITS:
            errors.append(f"{prefix}.split is invalid")
        if not isinstance(record["review_status"], str) or record["review_status"] not in ALLOWED_REVIEW:
            errors.append(f"{prefix}.review_status is invalid")
        if record["usage"] == "train" and record["review_status"] != "approved":
            errors.append(f"{prefix}: training use requires approved review_status")
        if record["split"] in {"validation", "test"} and record["usage"] == "train":
            errors.append(f"{prefix}: validation/test data cannot be marked for training")
        if "size_bytes" in record and (
            not isinstance(record["size_bytes"], int)
            or isinstance(record["size_bytes"], bool)
            or record["size_bytes"] < 0
        ):
            errors.append(f"{prefix}.size_bytes must be a non-negative integer")

    return errors



def verify_training_corpus(manifest_path: Path, dataset_id: str, corpus_path: Path) -> dict:
    """Require an approved training record and verify the exact local artifact."""
    try:
        data = json.loads(manifest_path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        raise ValueError(f"cannot read dataset manifest: {exc}") from exc
    errors = validate_manifest(data)
    if errors:
        raise ValueError("dataset manifest is invalid: " + "; ".join(errors))
    matches = [item for item in data["datasets"] if item["id"] == dataset_id]
    if len(matches) != 1:
        raise ValueError(f"dataset_id must identify exactly one manifest record: {dataset_id}")
    record = matches[0]
    if record["usage"] != "train" or record["review_status"] != "approved":
        raise ValueError("dataset is not approved for training")
    if record["split"] != "train":
        raise ValueError("training requires a dataset record with split=train")
    digest = hashlib.sha256(corpus_path.read_bytes()).hexdigest()
    if digest != record["sha256"]:
        raise ValueError("local corpus SHA-256 does not match the approved manifest record")
    return record


def main() -> int:
    try:
        data = json.loads(MANIFEST.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        print(f"Manifest read error: {exc}", file=sys.stderr)
        return 2

    errors = validate_manifest(data)
    if errors:
        print("\n".join(f"ERROR: {error}" for error in errors), file=sys.stderr)
        return 1
    print(f"Dataset manifest valid: {len(data['datasets'])} record(s)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
