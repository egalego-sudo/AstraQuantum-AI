import argparse
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

    def test_tiny_training_run_writes_checkpoint(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            corpus = root / "corpus.txt"
            output = root / "model.pt"
            corpus.write_text(("Quantum AI शोध और language model training. " * 40), encoding="utf-8")
            args = argparse.Namespace(
                text_file=str(corpus),
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

    def test_training_rejects_corpus_over_size_limit(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            corpus = root / "corpus.txt"
            corpus.write_text("x" * (1024 * 1024 + 1), encoding="utf-8")
            args = argparse.Namespace(
                text_file=str(corpus), output=str(root / "out.pt"), steps=1,
                batch_size=1, block_size=8, eval_interval=1, learning_rate=1e-3,
                seed=1, max_corpus_mb=1, width=16, heads=4, layers=1,
                dropout=0.0, cpu=True,
            )
            with self.assertRaises(ValueError):
                train(args)


if __name__ == "__main__":
    unittest.main()
