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
from evaluate_reasoning import evaluate_reasoning_tasks, load_tasks, normalize_answer


class ReasoningBenchmarkTests(unittest.TestCase):
    def _fixtures(self, root: Path, usage="evaluate", split="test"):
        tasks = [
            {"id": "math-1", "category": "math", "prompt": "What is 2 + 2? Answer only.", "reference": "4"},
            {"id": "logic-1", "category": "logic", "prompt": "If A then B. A is true. Is B true? Answer yes or no.", "reference": "yes"},
        ]
        task_file = root / "tasks.jsonl"
        raw = ("".join(json.dumps(task, ensure_ascii=False) + "\n" for task in tasks)).encode("utf-8")
        task_file.write_bytes(raw)
        manifest = root / "manifest.json"
        manifest.write_text(json.dumps({
            "manifest_version": 1,
            "datasets": [{
                "id": "reasoning-fixture", "name": "Synthetic reasoning test fixture",
                "source_url": "https://example.org/reasoning",
                "license_id": "CC0-1.0", "license_url": "https://creativecommons.org/publicdomain/zero/1.0/",
                "languages": ["en"], "sha256": hashlib.sha256(raw).hexdigest(),
                "usage": usage, "split": split, "attribution": "Synthetic unit-test data",
                "review_status": "approved"
            }]
        }), encoding="utf-8")
        model = QuantumGPT(ModelConfig(block_size=32, n_embd=16, n_head=4, n_layer=1))
        checkpoint = root / "model.pt"
        torch.save({
            "format": "quantum-gpt-research-v1", "tokenizer": "utf8-byte-v1",
            "config": model.config_dict(), "state_dict": model.state_dict(),
            "training": {"steps": 1}, "seed": 1,
        }, checkpoint)
        return checkpoint, task_file, manifest

    def test_load_tasks_and_normalization(self):
        tasks = load_tasks(b'{"id":"a","category":"math","prompt":"2+2","reference":"4"}\n')
        self.assertEqual(len(tasks), 1)
        self.assertEqual(normalize_answer("  YES   "), "yes")

    def test_rejects_duplicate_ids_and_unknown_fields(self):
        duplicate = b'{"id":"a","category":"math","prompt":"p","reference":"r"}\n{"id":"a","category":"logic","prompt":"p","reference":"r"}\n'
        with self.assertRaisesRegex(ValueError, "duplicate task id"):
            load_tasks(duplicate)
        with self.assertRaisesRegex(ValueError, "exactly id"):
            load_tasks(b'{"id":"a","category":"math","prompt":"p","reference":"r","extra":1}\n')

    def test_evaluation_produces_per_category_scores(self):
        with tempfile.TemporaryDirectory() as directory:
            checkpoint, task_file, manifest = self._fixtures(Path(directory))
            report = evaluate_reasoning_tasks(
                checkpoint, task_file, "reasoning-fixture", manifest, torch.device("cpu"),
                max_new_tokens=2, seed=12,
            )
            self.assertEqual(report["metrics"]["tasks"], 2)
            self.assertIn("math", report["metrics"]["category_scores"])
            self.assertIn("logic", report["metrics"]["category_scores"])
            self.assertEqual(len(report["task_results"]), 2)
            self.assertIn("not establish broad reasoning ability", report["interpretation"])

    def test_rejects_non_evaluation_dataset(self):
        with tempfile.TemporaryDirectory() as directory:
            checkpoint, task_file, manifest = self._fixtures(Path(directory), usage="train", split="train")
            with self.assertRaisesRegex(ValueError, "not approved for evaluation"):
                evaluate_reasoning_tasks(checkpoint, task_file, "reasoning-fixture", manifest, torch.device("cpu"))

    def test_rejects_changed_task_file(self):
        with tempfile.TemporaryDirectory() as directory:
            checkpoint, task_file, manifest = self._fixtures(Path(directory))
            task_file.write_text('{"id":"x","category":"math","prompt":"1+1","reference":"2"}\n', encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "SHA-256"):
                evaluate_reasoning_tasks(checkpoint, task_file, "reasoning-fixture", manifest, torch.device("cpu"))


if __name__ == "__main__":
    unittest.main()
