import argparse
import hashlib
import json
import sys
import tempfile
import unittest
from unittest.mock import patch
from pathlib import Path

import torch

sys.path.insert(0, str(Path(__file__).resolve().parent))
from train import evaluate, make_batch, scheduled_learning_rate, train


class TrainingTests(unittest.TestCase):
    def test_make_batch_returns_shifted_sequences(self):
        data = torch.arange(40)
        x, y = make_batch(data, batch_size=3, block_size=8, device=torch.device("cpu"))
        self.assertEqual(tuple(x.shape), (3, 8))
        self.assertEqual(tuple(y.shape), (3, 8))
        torch.testing.assert_close(y[:, :-1], x[:, 1:])

    def test_evaluation_is_deterministic_preserves_rng_and_training_state(self):
        from core import ModelConfig, QuantumGPT

        torch.manual_seed(7)
        model = QuantumGPT(ModelConfig(block_size=8, n_embd=16, n_head=4, n_layer=1, dropout=0.2))
        model.train()
        data = torch.randint(0, 258, (128,), dtype=torch.long)
        rng_before = torch.random.get_rng_state().clone()

        first = evaluate(model, data, batch_size=3, block_size=8, device=torch.device("cpu"), batches=5)
        rng_after_first = torch.random.get_rng_state().clone()
        second = evaluate(model, data, batch_size=3, block_size=8, device=torch.device("cpu"), batches=5)

        self.assertEqual(first, second)
        torch.testing.assert_close(rng_before, rng_after_first)
        self.assertTrue(model.training)

    def test_evaluation_rejects_short_validation_data(self):
        from core import ModelConfig, QuantumGPT

        model = QuantumGPT(ModelConfig(block_size=8, n_embd=16, n_head=4, n_layer=1))
        with self.assertRaisesRegex(ValueError, "block_size \\+ 2"):
            evaluate(model, torch.arange(9, dtype=torch.long), 2, 8, torch.device("cpu"), batches=2)

    def test_evaluation_rejects_invalid_batch_count(self):
        from core import ModelConfig, QuantumGPT

        model = QuantumGPT(ModelConfig(block_size=8, n_embd=16, n_head=4, n_layer=1))
        with self.assertRaisesRegex(ValueError, "must be positive"):
            evaluate(model, torch.arange(40, dtype=torch.long), 2, 8, torch.device("cpu"), batches=0)

    def test_cosine_schedule_has_warmup_and_decay(self):
        base = 1e-3
        self.assertAlmostEqual(scheduled_learning_rate(1, 10, 2, base), base / 2)
        self.assertAlmostEqual(scheduled_learning_rate(2, 10, 2, base), base)
        self.assertAlmostEqual(scheduled_learning_rate(3, 10, 2, base), base)
        self.assertLess(scheduled_learning_rate(10, 10, 2, base), base / 10)

    def test_cosine_schedule_rejects_invalid_warmup(self):
        with self.assertRaisesRegex(ValueError, "warmup_steps"):
            scheduled_learning_rate(1, 5, 5, 1e-3)

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
            validation_snapshots = []
            evaluation_calls = 0
            evaluation_losses = [5.0, 1.0, 4.0, 2.0]

            def controlled_evaluate(model, data, batch_size, block_size, device, batches=10):
                nonlocal evaluation_calls
                evaluation_calls += 1
                if evaluation_calls in (2, 4):
                    validation_snapshots.append({
                        key: value.detach().cpu().clone()
                        for key, value in model.state_dict().items()
                    })
                return evaluation_losses[evaluation_calls - 1]

            with patch("train.evaluate", side_effect=controlled_evaluate):
                result = train(args)
            self.assertEqual(result, output)
            self.assertGreater(output.stat().st_size, 0)
            checkpoint = torch.load(output, map_location="cpu", weights_only=True)
            self.assertEqual(checkpoint["format"], "quantum-gpt-research-v1")
            self.assertEqual(checkpoint["tokenizer"], "utf8-byte-v1")
            self.assertEqual(checkpoint["seed"], 1337)
            self.assertEqual(checkpoint["dataset"]["id"], "local-approved-corpus")
            self.assertEqual(checkpoint["dataset"]["sha256"], hashlib.sha256(corpus.read_bytes()).hexdigest())
            self.assertEqual(checkpoint["dataset"]["license_id"], "CC-BY-4.0")
            self.assertEqual(checkpoint["training"]["steps"], 2)
            self.assertEqual(checkpoint["training"]["block_size"], 8)
            self.assertEqual(checkpoint["best_validation_loss"], 1.0)
            self.assertEqual(checkpoint["best_step"], 1)
            self.assertEqual(len(validation_snapshots), 2)
            for key, expected in validation_snapshots[0].items():
                torch.testing.assert_close(checkpoint["state_dict"][key], expected)

            # Reject altered optimizer state before attempting to continue training.
            corrupt = dict(checkpoint)
            corrupt_optimizer = dict(checkpoint["optimizer_state_dict"])
            corrupt_slots = dict(corrupt_optimizer["state"])
            first_slot_id = next(iter(corrupt_slots))
            first_slot = dict(corrupt_slots[first_slot_id])
            first_slot["exp_avg"] = torch.full_like(first_slot["exp_avg"], float("nan"))
            corrupt_slots[first_slot_id] = first_slot
            corrupt_optimizer["state"] = corrupt_slots
            corrupt["optimizer_state_dict"] = corrupt_optimizer
            corrupt_path = root / "corrupt-resume.pt"
            torch.save(corrupt, corrupt_path)
            args.resume = str(corrupt_path)
            args.output = str(root / "should-not-exist.pt")
            with self.assertRaisesRegex(ValueError, "non-finite"):
                train(args)

            # Resume configuration is bound to the original seed and batch shape.
            args.resume = str(output)
            args.seed += 1
            with self.assertRaisesRegex(ValueError, "seed"):
                train(args)
            args.seed -= 1

            # Continue from the saved best step with the same approved data and model config.
            resumed_output = root / "resumed-model.pt"
            args.resume = str(output)
            args.output = str(resumed_output)
            args.steps = 3
            resumed_losses = [5.0, 0.5, 4.0, 0.8]
            resume_calls = 0

            def controlled_resume_evaluate(model, data, batch_size, block_size, device, batches=10):
                nonlocal resume_calls
                resume_calls += 1
                return resumed_losses[resume_calls - 1]

            with patch("train.evaluate", side_effect=controlled_resume_evaluate):
                train(args)
            resumed = torch.load(resumed_output, map_location="cpu", weights_only=True)
            self.assertEqual(resumed["training"]["starting_step"], 1)
            self.assertEqual(resumed["best_step"], 2)
            self.assertEqual(resumed["training"]["resumed_from"], str(output))
            self.assertIn("optimizer_state_dict", resumed)
            self.assertIn("rng_state", resumed)
            self.assertGreater(resumed["metrics"]["tokens_seen"], 0)

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

    def test_training_rejects_corpus_changed_after_approval_check(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            corpus = root / "corpus.txt"
            corpus.write_text(("approved corpus " * 40), encoding="utf-8")
            manifest = root / "manifest.json"
            manifest.write_text(json.dumps({
                "manifest_version": 1,
                "datasets": [{
                    "id": "approved-corpus", "name": "Approved corpus",
                    "source_url": "https://example.org/corpus",
                    "license_id": "CC-BY-4.0",
                    "license_url": "https://creativecommons.org/licenses/by/4.0/",
                    "languages": ["en"],
                    "sha256": hashlib.sha256(corpus.read_bytes()).hexdigest(),
                    "usage": "train", "split": "train",
                    "attribution": "Test", "review_status": "approved"
                }]
            }), encoding="utf-8")
            args = argparse.Namespace(
                text_file=str(corpus), dataset_id="approved-corpus",
                manifest=str(manifest), output=str(root / "out.pt"), steps=1,
                batch_size=1, block_size=8, eval_interval=1, learning_rate=1e-3,
                seed=1, max_corpus_mb=1, width=16, heads=4, layers=1,
                dropout=0.0, cpu=True,
            )
            from datasets.validate_manifest import verify_training_corpus as verify
            def verify_then_mutate(manifest_path, dataset_id, corpus_path):
                record = verify(manifest_path, dataset_id, corpus_path)
                corpus_path.write_text(("tampered corpus " * 40), encoding="utf-8")
                return record

            with patch("train.verify_training_corpus", side_effect=verify_then_mutate):
                with self.assertRaisesRegex(ValueError, "changed after approval verification"):
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
