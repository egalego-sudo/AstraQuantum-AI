import sys
import unittest
from pathlib import Path

import torch

sys.path.insert(0, str(Path(__file__).resolve().parent))
from core import ByteTokenizer, ModelConfig, QuantumGPT


class ByteTokenizerTests(unittest.TestCase):
    def test_round_trip_hindi_english_and_emoji(self):
        tokenizer = ByteTokenizer()
        text = "नमस्ते Quantum AI 🚀"
        self.assertEqual(tokenizer.decode(tokenizer.encode(text)), text)

    def test_special_tokens_are_not_decoded(self):
        self.assertEqual(ByteTokenizer().decode([0, 1, 66]), "@")


class ModelTests(unittest.TestCase):
    def setUp(self):
        torch.manual_seed(7)
        self.config = ModelConfig(block_size=16, n_embd=32, n_head=4, n_layer=2)
        self.model = QuantumGPT(self.config)

    def test_logits_shape_and_finite_loss(self):
        tokens = torch.randint(0, 258, (2, 8))
        logits, loss = self.model(tokens, tokens)
        self.assertEqual(tuple(logits.shape), (2, 8, 258))
        self.assertIsNotNone(loss)
        self.assertTrue(torch.isfinite(loss).item())

    def test_causal_attention_does_not_leak_future_tokens(self):
        self.model.eval()
        first = torch.tensor([[1, 10, 11, 12]])
        changed_future = torch.tensor([[1, 10, 99, 98]])
        with torch.no_grad():
            logits_a, _ = self.model(first)
            logits_b, _ = self.model(changed_future)
        torch.testing.assert_close(logits_a[:, :2], logits_b[:, :2])

    def test_rejects_context_overflow_and_bad_dimensions(self):
        with self.assertRaises(ValueError):
            self.model(torch.ones((1, 17), dtype=torch.long))
        with self.assertRaises(ValueError):
            ModelConfig(n_embd=30, n_head=4)

    def test_generation_preserves_training_state_and_length(self):
        self.model.train()
        prompt = torch.tensor([[1, 12, 13]])
        generated = self.model.generate(prompt, max_new_tokens=3, top_k=8)
        self.assertEqual(generated.shape[1], 6)
        self.assertTrue(self.model.training)

    def test_rejects_invalid_generation_parameters(self):
        with self.assertRaises(ValueError):
            self.model.generate(torch.tensor([[1]]), -1)
        with self.assertRaises(ValueError):
            self.model.generate(torch.tensor([[1]]), 1, temperature=0)

    def test_parameter_count_is_positive(self):
        self.assertGreater(self.model.parameter_count(), 0)


if __name__ == "__main__":
    unittest.main()
