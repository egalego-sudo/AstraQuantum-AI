import hashlib
import json
import sys
import tempfile
import unittest
from pathlib import Path

import torch

ROOT = Path(__file__).resolve().parent
sys.path.insert(0, str(ROOT))
from core import ModelConfig, QuantumGPT
from evaluate import evaluate_checkpoint


class EvaluationTests(unittest.TestCase):
    def _fixtures(self, root: Path, usage="evaluate", split="test"):
        corpus = root / "heldout.txt"
        corpus.write_text(("नमस्ते Quantum AI benchmark. " * 8), encoding="utf-8")
        digest = hashlib.sha256(corpus.read_bytes()).hexdigest()
        manifest = root / "manifest.json"
        manifest.write_text(json.dumps({
            "manifest_version": 1,
            "datasets": [{
                "id": "heldout-hi-en", "name": "Held-out Hindi English fixture",
                "source_url": "https://example.org/heldout",
                "license_id": "CC-BY-4.0",
                "license_url": "https://creativecommons.org/licenses/by/4.0/",
                "languages": ["hi", "en"], "sha256": digest,
                "usage": usage, "split": split,
                "attribution": "Synthetic unit-test fixture",
                "review_status": "approved"
            }]
        }), encoding="utf-8")
        model = QuantumGPT(ModelConfig(block_size=16, n_embd=16, n_head=4, n_layer=1))
        checkpoint = root / "model.pt"
        torch.save({
            "format": "quantum-gpt-research-v1",
            "tokenizer": "utf8-byte-v1",
            "config": model.config_dict(),
            "state_dict": model.state_dict(),
            "dataset": {"id": "training-fixture", "sha256": "a" * 64, "license_id": "CC0", "source_url": "https://example.org/train"},
            "training": {"steps": 3},
            "seed": 1,
        }, checkpoint)
        return checkpoint, corpus, manifest

    def test_evaluation_returns_finite_reproducible_metrics(self):
        with tempfile.TemporaryDirectory() as directory:
            checkpoint, corpus, manifest = self._fixtures(Path(directory))
            result = evaluate_checkpoint(checkpoint, corpus, "heldout-hi-en", manifest, torch.device("cpu"), generation_tokens=2)
            metrics = result["metrics"]
            self.assertGreater(metrics["evaluated_targets"], 0)
            self.assertTrue(torch.isfinite(torch.tensor(metrics["next_token_cross_entropy_nats"])).item())
            self.assertTrue(torch.isfinite(torch.tensor(metrics["perplexity"])).item())
            self.assertGreaterEqual(metrics["next_token_accuracy"], 0.0)
            self.assertLessEqual(metrics["next_token_accuracy"], 1.0)
            self.assertEqual(metrics["generated_tokens"], 2)
            self.assertEqual(result["evaluation_dataset"]["split"], "test")
            self.assertIn("not a measure of general intelligence", result["interpretation"])

    def test_evaluation_rejects_training_only_dataset(self):
        with tempfile.TemporaryDirectory() as directory:
            checkpoint, corpus, manifest = self._fixtures(Path(directory), usage="train", split="train")
            with self.assertRaisesRegex(ValueError, "not approved for evaluation"):
                evaluate_checkpoint(checkpoint, corpus, "heldout-hi-en", manifest, torch.device("cpu"), generation_tokens=0)

    def test_evaluation_rejects_hash_mismatch(self):
        with tempfile.TemporaryDirectory() as directory:
            checkpoint, corpus, manifest = self._fixtures(Path(directory))
            corpus.write_text("changed after manifest approval", encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "SHA-256"):
                evaluate_checkpoint(checkpoint, corpus, "heldout-hi-en", manifest, torch.device("cpu"), generation_tokens=0)


if __name__ == "__main__":
    unittest.main()
