import argparse
import hashlib
import json
import sys
import tempfile
import unittest
from pathlib import Path

import torch

sys.path.insert(0, str(Path(__file__).resolve().parent))
from train import make_batch, train


class TrainingTests(unittest.TestCase):
    def test_make_batch_returns_shifted_sequences(self):
        data = torch.arange(40)
        x, y = make_batch(data, batch_size=3, block_size=8, device=torch.device("cpu"))
        self.assertEqual(tuple(x.shape), (3, 8))
        self.assertEqual(tuple(y.shape), (3, 8))
        torch.testing.assert_close(y[:, :-1], x[:, 1:])

    def test_make_batch_rejects_short_data(self):
        with self.assertRaises(ValueError):
            make_batch(torch.arange(8), batch_size=2, block_size=8, device=torch.device("cpu"))

    def test_make_batch_rejects_exactly_one_target_token_short(self):
        with self.assertRaises(ValueError):
            make_batch(torch.arange(9), batch_size=2, block_size=8, device=torch.device("cpu"))

    def test_make_batch_rejects_non_vector_data(self):
        with self.assertRaises(ValueError):
            make_batch(torch.arange(40).reshape(20, 2), batch_size=2, block_size=8, device=torch.device("cpu"))

    def test_make_batch_rejects_non_integer_tokens(self):
        with self.assertRaises(ValueError):
            make_batch(torch.arange(40, dtype=torch.float32), batch_size=2, block_size=8, device=torch.device("cpu"))

    def test_tiny_training_run_writes_checkpoint(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            corpus = root / "corpus.txt"
            output = root / "model.pt"
            corpus.write_text(("Quantum AI शोध और language model training. " * 40), encoding="utf-8")
            manifest = root / "manifest.json"
            manifest.write_text(json.dumps({
                "manifest_version": 1,
                "datasets": [{
                    "id": "local-approved-corpus", "name": "Local approved corpus",
                    "source_url": "https://example.org/corpus",
                    "license_id": "CC-BY-4.0",
                    "license_url": "https://creativecommons.org/licenses/by/4.0/",
                    "languages": ["en", "hi"],
                    "sha256": hashlib.sha256(corpus.read_bytes()).hexdigest(),
                    "usage": "train", "split": "train",
                    "attribution": "Local test fixture", "review_status": "approved"
                }]
            }), encoding="utf-8")
            args = argparse.Namespace(
                text_file=str(corpus),
                dataset_id="local-approved-corpus",
                manifest=str(manifest),
                output=str(output),
                steps=2,
                batch_size=2,
                block_size=8,
                width=16,
                heads=4,
                layers=1,
                dropout=0.0,
                learning_rate=1e-3,
                eval_interval=1,
                seed=1337,
                max_corpus_mb=1,
                cpu=True,
            )
            result = train(args)
            self.assertEqual(result, output)
            self.assertGreater(output.stat().st_size, 0)
            checkpoint = torch.load(output, map_location="cpu", weights_only=True)
            self.assertEqual(checkpoint["format"], "quantum-gpt-research-v1")
            self.assertEqual(checkpoint["tokenizer"], "utf8-byte-v1")
            self.assertEqual(checkpoint["seed"], 1337)
            self.assertGreater(checkpoint["best_validation_loss"], 0)

    def test_training_rejects_corpus_hash_mismatch(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            corpus = root / "corpus.txt"
            corpus.write_text(("changed corpus " * 40), encoding="utf-8")
            manifest = root / "manifest.json"
            manifest.write_text(json.dumps({
                "manifest_version": 1,
                "datasets": [{
                    "id": "approved-but-different-artifact", "name": "Approved corpus",
                    "source_url": "https://example.org/corpus",
                    "license_id": "CC-BY-4.0",
                    "license_url": "https://creativecommons.org/licenses/by/4.0/",
                    "languages": ["en"],
                    "sha256": "a" * 64,
                    "usage": "train", "split": "train",
                    "attribution": "Test", "review_status": "approved"
                }]
            }), encoding="utf-8")
            args = argparse.Namespace(
                text_file=str(corpus), dataset_id="approved-but-different-artifact",
                manifest=str(manifest), output=str(root / "out.pt"), steps=1,
                batch_size=1, block_size=8, eval_interval=1, learning_rate=1e-3,
                seed=1, max_corpus_mb=1, width=16, heads=4, layers=1,
                dropout=0.0, cpu=True,
            )
            with self.assertRaisesRegex(ValueError, "SHA-256"):
                train(args)

    def test_training_rejects_unapproved_dataset(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            corpus = root / "corpus.txt"
            corpus.write_text(("not approved " * 40), encoding="utf-8")
            manifest = root / "manifest.json"
            manifest.write_text(json.dumps({
                "manifest_version": 1,
                "datasets": [{
                    "id": "pending-corpus", "name": "Pending corpus",
                    "source_url": "https://example.org/corpus",
                    "license_id": "CC-BY-4.0",
                    "license_url": "https://creativecommons.org/licenses/by/4.0/",
                    "languages": ["en"],
                    "sha256": hashlib.sha256(corpus.read_bytes()).hexdigest(),
                    "usage": "train", "split": "train",
                    "attribution": "Test", "review_status": "pending"
                }]
            }), encoding="utf-8")
            args = argparse.Namespace(
                text_file=str(corpus), dataset_id="pending-corpus", manifest=str(manifest),
                output=str(root / "out.pt"), steps=1, batch_size=1, block_size=8,
                eval_interval=1, learning_rate=1e-3, seed=1, max_corpus_mb=1,
                width=16, heads=4, layers=1, dropout=0.0, cpu=True,
            )
            with self.assertRaisesRegex(ValueError, "approved review_status"):
                train(args)

    def test_training_rejects_corpus_over_size_limit(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            corpus = root / "corpus.txt"
            corpus.write_text("x" * (1024 * 1024 + 1), encoding="utf-8")
            args = argparse.Namespace(
                text_file=str(corpus), dataset_id="oversize", manifest=str(root / "missing.json"), output=str(root / "out.pt"), steps=1,
                batch_size=1, block_size=8, eval_interval=1, learning_rate=1e-3,
                seed=1, max_corpus_mb=1, width=16, heads=4, layers=1,
                dropout=0.0, cpu=True,
            )
            with self.assertRaises(ValueError):
                train(args)


if __name__ == "__main__":
    unittest.main()
