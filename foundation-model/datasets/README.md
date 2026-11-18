# QUANTUM AI dataset governance

This directory is the source-of-truth registry for datasets considered by the
foundation-model training pipeline. It intentionally starts empty: no dataset
is approved merely because it is publicly reachable.

Before adding a dataset, record its provenance, exact license, language coverage,
content checksum, intended split, and explicit permitted use. A dataset with
unknown or ambiguous rights must not be used for training.

## Required record fields

- `id`: stable unique identifier.
- `name`: human-readable dataset name.
- `source_url`: HTTPS origin or canonical dataset page.
- `license_id`: SPDX identifier when available; otherwise a precise license name.
- `license_url`: HTTPS URL to the license terms.
- `languages`: non-empty BCP-47 language-tag list (for example `en`, `hi`).
- `sha256`: SHA-256 of the exact downloaded artifact (64 lowercase hex chars).
- `usage`: one of `train`, `evaluate`, `reference_only`, or `not_permitted`.
- `split`: one of `train`, `validation`, `test`, or `unspecified`.
- `attribution`: attribution text or an explicit statement that none is required.
- `review_status`: `approved`, `pending`, or `rejected`.

Optional fields: `version`, `size_bytes`, `notes`.

## Safety rules

1. Never infer permission from public availability.
2. Do not train on records unless `usage=train` and `review_status=approved`.
3. Keep evaluation/test material out of training.
4. Store exact artifact hashes; a changed artifact requires a new review.
5. Respect opt-outs, personal-data restrictions, retention limits, and applicable law.
6. Do not commit dataset payloads, credentials, or personal data to this repository.

## Validate

From the repository root:

```sh
python foundation-model/datasets/validate_manifest.py
python foundation-model/datasets/test_manifest.py
```

The validator checks registry integrity only. It does not provide legal advice or
prove that a license grants the proposed use; human review remains mandatory.
