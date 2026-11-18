"""Small decoder-only Transformer for QUANTUM AI research.

This is original educational code, not GPT-6 source code or pretrained weights.
"""
from __future__ import annotations

import math

from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Iterable

import torch
from torch import nn
from torch.nn import functional as F


@dataclass(frozen=True)
class ModelConfig:
    vocab_size: int = 258
    block_size: int = 128
    n_embd: int = 128
    n_head: int = 4
    n_layer: int = 2
    dropout: float = 0.0

    def __post_init__(self) -> None:
        integer_fields = ("vocab_size", "block_size", "n_embd", "n_head", "n_layer")
        if any(type(getattr(self, name)) is not int for name in integer_fields):
            raise ValueError("model dimensions must be integers")
        if self.vocab_size < 258:
            raise ValueError("vocab_size must include 256 byte values and two special tokens")
        if self.block_size < 1 or self.n_embd < 1 or self.n_layer < 1 or self.n_head < 1:
            raise ValueError("model dimensions must be positive")
        if self.n_embd % self.n_head:
            raise ValueError("n_embd must be divisible by n_head")
        if isinstance(self.dropout, bool) or not isinstance(self.dropout, (int, float)):
            raise ValueError("dropout must be a finite number in [0, 1)")
        if not math.isfinite(float(self.dropout)) or not 0.0 <= self.dropout < 1.0:
            raise ValueError("dropout must be a finite number in [0, 1)")


class ByteTokenizer:
    """UTF-8 byte tokenizer: PAD=0, BOS=1, byte values occupy [2, 257]."""

    pad_id = 0
    bos_id = 1
    vocab_size = 258

    def encode(self, text: str, add_bos: bool = True) -> list[int]:
        ids = [byte + 2 for byte in text.encode("utf-8")]
        return ([self.bos_id] + ids) if add_bos else ids

    def decode(self, ids: Iterable[int]) -> str:
        raw = bytearray()
        for token in ids:
            value = int(token)
            if 2 <= value < self.vocab_size:
                raw.append(value - 2)
        return raw.decode("utf-8", errors="replace")


class CausalSelfAttention(nn.Module):
    def __init__(self, config: ModelConfig) -> None:
        super().__init__()
        self.n_head = config.n_head
        self.head_dim = config.n_embd // config.n_head
        self.qkv = nn.Linear(config.n_embd, 3 * config.n_embd)
        self.proj = nn.Linear(config.n_embd, config.n_embd)
        self.attn_dropout = nn.Dropout(config.dropout)
        self.resid_dropout = nn.Dropout(config.dropout)
        mask = torch.tril(torch.ones(config.block_size, config.block_size, dtype=torch.bool))
        self.register_buffer("causal_mask", mask.view(1, 1, config.block_size, config.block_size), persistent=False)

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        batch, steps, channels = x.shape
        q, k, v = self.qkv(x).chunk(3, dim=-1)

        def split_heads(tensor: torch.Tensor) -> torch.Tensor:
            return tensor.view(batch, steps, self.n_head, self.head_dim).transpose(1, 2)

        q, k, v = split_heads(q), split_heads(k), split_heads(v)
        scores = (q @ k.transpose(-2, -1)) * (self.head_dim ** -0.5)
        scores = scores.masked_fill(
            ~self.causal_mask[:, :, :steps, :steps],
            torch.finfo(scores.dtype).min,
        )
        weights = self.attn_dropout(F.softmax(scores, dim=-1))
        attended = (weights @ v).transpose(1, 2).contiguous().view(batch, steps, channels)
        return self.resid_dropout(self.proj(attended))


class TransformerBlock(nn.Module):
    def __init__(self, config: ModelConfig) -> None:
        super().__init__()
        self.norm1 = nn.LayerNorm(config.n_embd)
        self.attn = CausalSelfAttention(config)
        self.norm2 = nn.LayerNorm(config.n_embd)
        self.mlp = nn.Sequential(
            nn.Linear(config.n_embd, 4 * config.n_embd),
            nn.GELU(),
            nn.Linear(4 * config.n_embd, config.n_embd),
            nn.Dropout(config.dropout),
        )

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        x = x + self.attn(self.norm1(x))
        return x + self.mlp(self.norm2(x))


class QuantumGPT(nn.Module):
    """A compact causal language model with next-token cross-entropy loss."""

    def __init__(self, config: ModelConfig = ModelConfig()) -> None:
        super().__init__()
        self.config = config
        self.token_embedding = nn.Embedding(config.vocab_size, config.n_embd)
        self.position_embedding = nn.Embedding(config.block_size, config.n_embd)
        self.embedding_dropout = nn.Dropout(config.dropout)
        self.blocks = nn.Sequential(*(TransformerBlock(config) for _ in range(config.n_layer)))
        self.final_norm = nn.LayerNorm(config.n_embd)
        self.lm_head = nn.Linear(config.n_embd, config.vocab_size, bias=False)
        self.lm_head.weight = self.token_embedding.weight
        self.apply(self._init_weights)

    @staticmethod
    def _init_weights(module: nn.Module) -> None:
        if isinstance(module, nn.Linear):
            nn.init.normal_(module.weight, mean=0.0, std=0.02)
            if module.bias is not None:
                nn.init.zeros_(module.bias)
        elif isinstance(module, nn.Embedding):
            nn.init.normal_(module.weight, mean=0.0, std=0.02)

    def forward(
        self,
        input_ids: torch.Tensor,
        targets: torch.Tensor | None = None,
    ) -> tuple[torch.Tensor, torch.Tensor | None]:
        if input_ids.ndim != 2:
            raise ValueError("input_ids must have shape [batch, sequence]")
        if input_ids.dtype != torch.long:
            raise ValueError("input_ids must use torch.long token IDs")
        batch, steps = input_ids.shape
        if steps < 1 or steps > self.config.block_size:
            raise ValueError("sequence length is outside the configured context window")
        if torch.any(input_ids < 0) or torch.any(input_ids >= self.config.vocab_size):
            raise ValueError("input token id is outside the vocabulary")

        positions = torch.arange(steps, device=input_ids.device)
        x = self.embedding_dropout(self.token_embedding(input_ids) + self.position_embedding(positions))
        logits = self.lm_head(self.final_norm(self.blocks(x)))
        loss = None
        if targets is not None:
            if targets.shape != input_ids.shape:
                raise ValueError("targets must have the same shape as input_ids")
            if targets.dtype != torch.long:
                raise ValueError("targets must use torch.long token IDs")
            if torch.any(targets < 0) or torch.any(targets >= self.config.vocab_size):
                raise ValueError("target token id is outside the vocabulary")
            loss = F.cross_entropy(logits.reshape(-1, logits.size(-1)), targets.reshape(-1))
        return logits, loss

    @torch.no_grad()
    def generate(
        self,
        input_ids: torch.Tensor,
        max_new_tokens: int,
        temperature: float = 1.0,
        top_k: int | None = None,
    ) -> torch.Tensor:
        if not isinstance(max_new_tokens, int) or max_new_tokens < 0:
            raise ValueError("max_new_tokens must be a non-negative integer")
        if input_ids.ndim != 2 or input_ids.size(1) < 1:
            raise ValueError("input_ids must have shape [batch, non-empty sequence]")
        if input_ids.dtype != torch.long:
            raise ValueError("input_ids must use torch.long token IDs")
        if torch.any(input_ids < 0) or torch.any(input_ids >= self.config.vocab_size):
            raise ValueError("input token id is outside the vocabulary")
        if not math.isfinite(float(temperature)) or temperature <= 0:
            raise ValueError("temperature must be finite and positive")
        if top_k is not None and (not isinstance(top_k, int) or top_k < 1):
            raise ValueError("top_k must be a positive integer")
        was_training = self.training
        self.eval()
        try:
            for _ in range(max_new_tokens):
                context = input_ids[:, -self.config.block_size :]
                logits, _ = self(context)
                next_logits = logits[:, -1, :] / temperature
                if top_k is not None:
                    k = min(top_k, next_logits.size(-1))
                    threshold = torch.topk(next_logits, k).values[:, -1, None]
                    next_logits = next_logits.masked_fill(next_logits < threshold, float("-inf"))
                probabilities = F.softmax(next_logits, dim=-1)
                next_token = torch.multinomial(probabilities, num_samples=1)
                input_ids = torch.cat((input_ids, next_token), dim=1)
            return input_ids
        finally:
            self.train(was_training)

    def parameter_count(self) -> int:
        return sum(parameter.numel() for parameter in self.parameters())

    def config_dict(self) -> dict[str, int | float]:
        return asdict(self.config)

def load_checkpoint(
    path: str | Path,
    map_location: str | torch.device = "cpu",
) -> tuple[QuantumGPT, dict[str, object]]:
    """Load a research checkpoint using PyTorch's restricted weights-only loader.

    The checkpoint format, tokenizer, configuration and every state tensor are
    validated before the model is returned. Never load untrusted checkpoints
    with unrestricted pickle deserialization.
    """
    try:
        payload = torch.load(path, map_location=map_location, weights_only=True)
    except Exception as exc:
        raise ValueError("checkpoint could not be safely loaded") from exc

    if not isinstance(payload, dict):
        raise ValueError("checkpoint root must be a dictionary")
    if payload.get("format") != "quantum-gpt-research-v1":
        raise ValueError("unsupported checkpoint format")
    if payload.get("tokenizer") != "utf8-byte-v1":
        raise ValueError("unsupported checkpoint tokenizer")

    config_data = payload.get("config")
    expected_config_keys = set(ModelConfig.__dataclass_fields__)
    if not isinstance(config_data, dict) or set(config_data) != expected_config_keys:
        raise ValueError("checkpoint model configuration is incomplete or contains unknown fields")
    try:
        config = ModelConfig(**config_data)
    except (TypeError, ValueError) as exc:
        raise ValueError("checkpoint model configuration is invalid") from exc

    state = payload.get("state_dict")
    if not isinstance(state, dict) or not all(
        isinstance(key, str) and isinstance(value, torch.Tensor)
        for key, value in state.items()
    ):
        raise ValueError("checkpoint state_dict must contain only named tensors")

    model = QuantumGPT(config)
    expected_state = model.state_dict()
    if set(state) != set(expected_state):
        raise ValueError("checkpoint state_dict keys do not match this model architecture")
    for key, expected_tensor in expected_state.items():
        tensor = state[key]
        if tensor.shape != expected_tensor.shape:
            raise ValueError(f"checkpoint tensor has an invalid shape: {key}")
        if torch.is_floating_point(tensor) and not torch.isfinite(tensor).all().item():
            raise ValueError(f"checkpoint tensor contains non-finite values: {key}")

    if not torch.equal(state["token_embedding.weight"], state["lm_head.weight"]):
        raise ValueError("checkpoint violates tied token/output embedding weights")

    try:
        model.load_state_dict(state, strict=True)
    except (RuntimeError, TypeError) as exc:
        raise ValueError("checkpoint weights are incompatible with the model") from exc

    metadata = {
        key: value
        for key, value in payload.items()
        if key not in {"format", "tokenizer", "config", "state_dict"}
    }
    return model, metadata
