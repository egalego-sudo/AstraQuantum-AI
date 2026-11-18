"""Minimal next-byte language-model training loop for lawful local text corpora."""
from __future__ import annotations

import argparse
import hashlib
import math
from pathlib import Path

import torch

from core import ByteTokenizer, ModelConfig, QuantumGPT
from datasets.validate_manifest import verify_training_corpus


def make_batch(data: torch.Tensor, batch_size: int, block_size: int, device: torch.device):
    if data.ndim != 1:
        raise ValueError("training data must be a one-dimensional token sequence")
    if data.dtype != torch.long:
        raise ValueError("training data must use torch.long token IDs")
    if batch_size < 1 or block_size < 1:
        raise ValueError("batch_size and block_size must be positive")
    if data.numel() <= block_size + 1:
        raise ValueError("training split must contain at least block_size + 2 tokens")
    starts = torch.randint(0, data.numel() - block_size - 1, (batch_size,))
    x = torch.stack([data[i : i + block_size] for i in starts.tolist()])
    y = torch.stack([data[i + 1 : i + block_size + 1] for i in starts.tolist()])
    return x.to(device), y.to(device)


@torch.no_grad()
def evaluate(model: QuantumGPT, data: torch.Tensor, batch_size: int, block_size: int, device: torch.device, batches: int = 10):
    was_training = model.training
    model.eval()
    losses = []
    try:
        for _ in range(batches):
            x, y = make_batch(data, batch_size, block_size, device)
            _, loss = model(x, y)
            losses.append(float(loss.item()))
    finally:
        model.train(was_training)
    return sum(losses) / len(losses)


def train(args: argparse.Namespace) -> Path:
    if args.steps < 1 or args.batch_size < 1 or args.block_size < 8:
        raise ValueError("steps and batch size must be positive; block size must be at least 8")
    if args.eval_interval < 1 or args.learning_rate <= 0:
        raise ValueError("eval interval and learning rate must be positive")
    if args.seed < 0 or args.max_corpus_mb < 1:
        raise ValueError("seed must be non-negative and max corpus size must be positive")

    corpus_path = Path(args.text_file)
    max_bytes = args.max_corpus_mb * 1024 * 1024
    if corpus_path.stat().st_size > max_bytes:
        raise ValueError(f"corpus exceeds configured {args.max_corpus_mb} MiB safety limit")
    manifest_path = Path(args.manifest)
    approved_record = verify_training_corpus(manifest_path, args.dataset_id, corpus_path)
    # Hash and decode the same byte snapshot to avoid a file-change race between
    # the approval check and the actual training read.
    with corpus_path.open("rb") as corpus_file:
        corpus_bytes = corpus_file.read(max_bytes + 1)
    if len(corpus_bytes) > max_bytes:
        raise ValueError(f"corpus exceeds configured {args.max_corpus_mb} MiB safety limit")
    if hashlib.sha256(corpus_bytes).hexdigest() != approved_record["sha256"]:
        raise ValueError("corpus changed after approval verification; refusing to train")
    text = corpus_bytes.decode("utf-8")
    tokenizer = ByteTokenizer()
    tokens = torch.tensor(tokenizer.encode(text), dtype=torch.long)
    split = int(tokens.numel() * 0.9)
    train_data, val_data = tokens[:split], tokens[split:]
    if train_data.numel() <= args.block_size + 1 or val_data.numel() <= args.block_size + 1:
        raise ValueError("corpus is too small for the selected context length and 90/10 split")

    torch.manual_seed(args.seed)
    if torch.cuda.is_available():
        torch.cuda.manual_seed_all(args.seed)
    device = torch.device("cuda" if torch.cuda.is_available() and not args.cpu else "cpu")
    config = ModelConfig(
        block_size=args.block_size,
        n_embd=args.width,
        n_head=args.heads,
        n_layer=args.layers,
        dropout=args.dropout,
    )
    model = QuantumGPT(config).to(device)
    optimizer = torch.optim.AdamW(model.parameters(), lr=args.learning_rate)
    best_val = math.inf

    for step in range(1, args.steps + 1):
        model.train()
        x, y = make_batch(train_data, args.batch_size, args.block_size, device)
        _, loss = model(x, y)
        optimizer.zero_grad(set_to_none=True)
        loss.backward()
        torch.nn.utils.clip_grad_norm_(model.parameters(), 1.0)
        optimizer.step()

        if step == 1 or step % args.eval_interval == 0 or step == args.steps:
            train_loss = evaluate(model, train_data, args.batch_size, args.block_size, device, batches=5)
            val_loss = evaluate(model, val_data, args.batch_size, args.block_size, device, batches=5)
            print(f"step={step} train_loss={train_loss:.4f} val_loss={val_loss:.4f} device={device}")
            best_val = min(best_val, val_loss)

    output = Path(args.output)
    output.parent.mkdir(parents=True, exist_ok=True)
    temporary = output.with_suffix(output.suffix + ".tmp")
    torch.save(
        {
            "format": "quantum-gpt-research-v1",
            "config": model.config_dict(),
            "state_dict": model.state_dict(),
            "best_validation_loss": best_val,
            "tokenizer": "utf8-byte-v1",
            "seed": args.seed,
            "dataset": {
                "id": args.dataset_id,
                "sha256": approved_record["sha256"],
                "license_id": approved_record["license_id"],
                "source_url": approved_record["source_url"],
            },
            "training": {
                "steps": args.steps,
                "batch_size": args.batch_size,
                "block_size": args.block_size,
                "learning_rate": args.learning_rate,
                "eval_interval": args.eval_interval,
                "max_corpus_mb": args.max_corpus_mb,
            },
        },
        temporary,
    )
    temporary.replace(output)
    print(f"saved={output} parameters={model.parameter_count()} best_validation_loss={best_val:.4f}")
    return output


def parse_args():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--text-file", required=True)
    parser.add_argument("--dataset-id", required=True, help="approved training dataset ID in the manifest")
    parser.add_argument(
        "--manifest",
        default=str(Path(__file__).resolve().parent / "datasets" / "manifest.json"),
        help="dataset governance manifest path",
    )
    parser.add_argument("--output", default="quantum-small.pt")
    parser.add_argument("--steps", type=int, default=500)
    parser.add_argument("--batch-size", type=int, default=16)
    parser.add_argument("--block-size", type=int, default=128)
    parser.add_argument("--width", type=int, default=128)
    parser.add_argument("--heads", type=int, default=4)
    parser.add_argument("--layers", type=int, default=2)
    parser.add_argument("--dropout", type=float, default=0.0)
    parser.add_argument("--learning-rate", type=float, default=3e-4)
    parser.add_argument("--eval-interval", type=int, default=100)
    parser.add_argument("--seed", type=int, default=1337)
    parser.add_argument("--max-corpus-mb", type=int, default=64)
    parser.add_argument("--cpu", action="store_true")
    return parser.parse_args()


if __name__ == "__main__":
    train(parse_args())
