"""Tests for dataset manifest governance validation."""
import unittest

from validate_manifest import validate_manifest


def valid_record(**overrides):
    record = {
        "id": "sample-en-v1",
        "name": "Sample dataset",
        "source_url": "https://example.org/dataset",
        "license_id": "CC-BY-4.0",
        "license_url": "https://creativecommons.org/licenses/by/4.0/",
        "languages": ["en", "hi"],
        "sha256": "a" * 64,
        "usage": "evaluate",
        "split": "test",
        "attribution": "Attribution as specified by the source",
        "review_status": "approved",
    }
    record.update(overrides)
    return record


class DatasetManifestTests(unittest.TestCase):
    def test_empty_manifest_is_valid(self):
        self.assertEqual(validate_manifest({"manifest_version": 1, "datasets": []}), [])

    def test_valid_record_is_accepted(self):
        self.assertEqual(
            validate_manifest({"manifest_version": 1, "datasets": [valid_record()]}),
            [],
        )

    def test_unknown_permission_is_rejected(self):
        errors = validate_manifest({
            "manifest_version": 1,
            "datasets": [valid_record(usage="public")],
        })
        self.assertTrue(any(".usage" in error for error in errors))

    def test_training_requires_approved_review(self):
        errors = validate_manifest({
            "manifest_version": 1,
            "datasets": [valid_record(usage="train", split="train", review_status="pending")],
        })
        self.assertTrue(any("requires approved" in error for error in errors))

    def test_test_split_cannot_be_training_data(self):
        errors = validate_manifest({
            "manifest_version": 1,
            "datasets": [valid_record(usage="train", split="test")],
        })
        self.assertTrue(any("validation/test" in error for error in errors))

    def test_non_https_source_is_rejected(self):
        errors = validate_manifest({
            "manifest_version": 1,
            "datasets": [valid_record(source_url="http://example.org/data")],
        })
        self.assertTrue(any("source_url" in error for error in errors))

    def test_bad_hash_is_rejected(self):
        errors = validate_manifest({
            "manifest_version": 1,
            "datasets": [valid_record(sha256="NOT-A-HASH")],
        })
        self.assertTrue(any("sha256" in error for error in errors))

    def test_duplicate_id_and_hash_are_rejected(self):
        record = valid_record()
        errors = validate_manifest({
            "manifest_version": 1,
            "datasets": [record, dict(record, name="Second record")],
        })
        self.assertTrue(any("id is duplicated" in error for error in errors))
        self.assertTrue(any("sha256 is duplicated" in error for error in errors))

    def test_boolean_size_is_rejected(self):
        errors = validate_manifest({
            "manifest_version": 1,
            "datasets": [valid_record(size_bytes=True)],
        })
        self.assertTrue(any("size_bytes" in error for error in errors))

    def test_non_object_record_is_rejected(self):
        errors = validate_manifest({"manifest_version": 1, "datasets": ["bad"]})
        self.assertTrue(any("must be an object" in error for error in errors))


if __name__ == "__main__":
    unittest.main()
