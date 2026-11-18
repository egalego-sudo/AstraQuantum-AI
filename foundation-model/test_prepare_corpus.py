import json
import sys
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parent
sys.path.insert(0, str(ROOT))
from datasets.prepare_corpus import prepare_corpus
from datasets.validate_manifest import validate_manifest


class PrepareCorpusTests(unittest.TestCase):
    def _run(self, root: Path, seed=7):
        source = root / "source.txt"
        source.write_text("नमस्ते\nhello\nनमस्ते\n\nlogic task\nmath task\n", encoding="utf-8")
        train, validation, manifest = root / "train.txt", root / "validation.txt", root / "manifest.json"
        report = prepare_corpus(
            source, train, validation, manifest, "sample",
            "https://example.org/data", "CC0-1.0",
            "https://creativecommons.org/publicdomain/zero/1.0/",
            ["hi", "en"], validation_fraction=0.25, seed=seed,
        )
        return report, train, validation, manifest

    def test_prepares_deduplicated_disjoint_splits_and_pending_manifest(self):
        with tempfile.TemporaryDirectory() as directory:
            report, train, validation, manifest_path = self._run(Path(directory))
            train_docs = set(train.read_text(encoding="utf-8").splitlines())
            validation_docs = set(validation.read_text(encoding="utf-8").splitlines())
            self.assertEqual(report["duplicate_documents_removed"], 1)
            self.assertEqual(report["blank_lines_removed"], 1)
            self.assertFalse(train_docs & validation_docs)
            self.assertEqual(len(train_docs) + len(validation_docs), 4)
            manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
            self.assertEqual(validate_manifest(manifest), [])
            self.assertTrue(all(item["usage"] == "not_permitted" for item in manifest["datasets"]))
            self.assertTrue(all(item["review_status"] == "pending" for item in manifest["datasets"]))
            self.assertEqual(manifest["datasets"][1]["split"], "validation")

    def test_is_deterministic_for_same_seed(self):
        with tempfile.TemporaryDirectory() as first, tempfile.TemporaryDirectory() as second:
            _, train_a, val_a, _ = self._run(Path(first), seed=99)
            _, train_b, val_b, _ = self._run(Path(second), seed=99)
            self.assertEqual(train_a.read_bytes(), train_b.read_bytes())
            self.assertEqual(val_a.read_bytes(), val_b.read_bytes())

    def test_rejects_same_output_path(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source = root / "source.txt"
            source.write_text("one\ntwo\n", encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "distinct"):
                prepare_corpus(
                    source, root / "same.txt", root / "same.txt", root / "manifest.json",
                    "sample", "https://example.org/data", "CC0",
                    "https://example.org/license", ["en"],
                )

    def test_rejects_invalid_source_url_and_too_few_documents(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source = root / "source.txt"
            source.write_text("one\n", encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "HTTPS"):
                prepare_corpus(
                    source, root / "train", root / "validation", root / "manifest",
                    "sample", "http://example.org/data", "CC0",
                    "https://example.org/license", ["en"],
                )


if __name__ == "__main__":
    unittest.main()
