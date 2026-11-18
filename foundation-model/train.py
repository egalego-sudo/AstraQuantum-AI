"""Minimal next-byte language-model training loop for lawful local text corpora."""
from __future__ import annotations

import argparse
import hashlib
import math
import time
from pathlib import Path

import torch

from core import ByteTokenizer, ModelConfig, QuantumGPT, load_checkpoint
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
    if batches < 1 or batch_size < 1 or block_size < 1:
        raise ValueError("evaluation batches, batch_size and block_size must be positive")
    if data.ndim != 1 or data.dtype != torch.long:
        raise ValueError("evaluation data must be a one-dimensional torch.long token sequence")
    max_start = data.numel() - block_size - 1
    if data.numel() <= block_size + 1:
        raise ValueError("evaluation split must contain at least block_size + 2 tokens")

    # Fixed, evenly spaced windows make validation comparable between steps and
    # avoid consuming the training RNG while selecting the best checkpoint.
    sample_count = min(batches * batch_size, max_start + 1)
    starts = torch.linspace(0, max_start, steps=sample_count).round().long().unique(sorted=True)
    was_training = model.training
    model.eval()
    total_loss = 0.0
    total_targets = 0
    try:
        for offset in range(0, starts.numel(), batch_size):
            batch_starts = starts[offset : offset + batch_size].tolist()
            x = torch.stack([data[i : i + block_size] for i in batch_starts]).to(device)
            y = torch.stack([data[i + 1 : i + block_size + 1] for i in batch_starts]).to(device)
            _, loss = model(x, y)
            target_count = y.numel()
            total_loss += float(loss.item()) * target_count
            total_targets += target_count
    finally:
        model.train(was_training)
    return total_loss / total_targets


def scheduled_learning_rate(step: int, total_steps: int, warmup_steps: int, base_learning_rate: float) -> float:
    if total_steps < 1 or step < 1 or step > total_steps:
        raise ValueError("step must be within the configured training schedule")
    if warmup_steps < 0 or warmup_steps >= total_steps:
        raise ValueError("warmup_steps must be non-negative and less than total_steps")
    if base_learning_rate <= 0:
        raise ValueError("base_learning_rate must be positive")
    if warmup_steps and step <= warmup_steps:
        return base_learning_rate * step / warmup_steps
    decay_steps = max(1, total_steps - warmup_steps)
    progress = min(1.0, max(0.0, (step - warmup_steps - 1) / decay_steps))
    return base_learning_rate * 0.5 * (1.0 + math.cos(math.pi * progress))


def _to_cpu_tree(value):
    if isinstance(value, torch.Tensor):
        return value.detach().cpu().clone()
    if isinstance(value, dict):
        return {key: _to_cpu_tree(item) for key, item in value.items()}
    if isinstance(value, list):
        return [_to_cpu_tree(item) for item in value]
    if isinstance(value, tuple):
        return tuple(_to_cpu_tree(item) for item in value)
    return value


def train(args: argparse.Namespace) -> Path:
    warmup_steps = getattr(args, "warmup_steps", 0)
    resume_path = getattr(args, "resume", None)
    if args.steps < 1 or args.batch_size < 1 or args.block_size < 8:
        raise ValueError("steps and batch size must be positive; block size must be at least 8")
    if args.eval_interval < 1 or args.learning_rate <= 0:
        raise ValueError("eval interval and learning rate must be positive")
    if args.seed < 0 or args.max_corpus_mb < 1:
        raise ValueError("seed must be non-negative and max corpus size must be positive")
    if warmup_steps < 0 or warmup_steps >= args.steps:
        raise ValueError("warmup steps must be non-negative and less than total steps")

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

    start_step = 0
    best_val = math.inf
    best_step = 0
    best_state_dict = None
    best_optimizer_state = None
    best_rng_state = None
    best_cuda_rng_state = None
    best_metrics = {}
    previous_elapsed = 0.0
    if resume_path:
        resumed_model, metadata = load_checkpoint(resume_path, map_location=device)
        if resumed_model.config_dict() != model.config_dict():
            raise ValueError("resume checkpoint model configuration does not match the requested training configuration")
        dataset_metadata = metadata.get("dataset")
        if not isinstance(dataset_metadata, dict) or dataset_metadata.get("id") != args.dataset_id or dataset_metadata.get("sha256") != approved_record["sha256"]:
            raise ValueError("resume checkpoint dataset provenance does not match the approved corpus")
        optimizer_state = metadata.get("optimizer_state_dict")
        rng_state = metadata.get("rng_state")
        if not isinstance(optimizer_state, dict) or not isinstance(rng_state, torch.Tensor):
            raise ValueError("resume checkpoint lacks optimizer/RNG state; start a new run or use a newer resumable checkpoint")
        try:
            model.load_state_dict(resumed_model.state_dict(), strict=True)
            optimizer.load_state_dict(optimizer_state)
        except (RuntimeError, TypeError, ValueError) as exc:
            raise ValueError("resume checkpoint optimizer/model state is incompatible") from exc
        start_step = int(metadata.get("global_step", metadata.get("best_step", 0)))
        best_step = int(metadata.get("best_step", start_step))
        best_val = float(metadata.get("best_validation_loss", math.inf))
        if start_step < 1 or start_step >= args.steps or best_step < 1 or best_step > start_step or not math.isfinite(best_val):
            raise ValueError("resume checkpoint training step or best validation metadata is invalid")
        best_state_dict = _to_cpu_tree(model.state_dict())
        best_optimizer_state = _to_cpu_tree(optimizer.state_dict())
        best_rng_state = rng_state.detach().cpu().clone()
        cuda_rng_state = metadata.get("cuda_rng_state")
        if device.type == "cuda":
            if not isinstance(cuda_rng_state, list) or not cuda_rng_state or not all(isinstance(item, torch.Tensor) for item in cuda_rng_state):
                raise ValueError("resume checkpoint lacks CUDA RNG state for GPU continuation")
            torch.cuda.set_rng_state_all(cuda_rng_state)
            best_cuda_rng_state = _to_cpu_tree(cuda_rng_state)
        torch.set_rng_state(best_rng_state)
        metrics = metadata.get("metrics")
        if isinstance(metrics, dict):
            best_metrics = metrics
            previous_elapsed = float(metrics.get("elapsed_seconds", 0.0))
        print(f"resumed_from={resume_path} start_step={start_step} best_step={best_step}")

    if device.type == "cuda":
        torch.cuda.reset_peak_memory_stats(device)
    run_started = time.perf_counter()
    for step in range(start_step + 1, args.steps + 1):
        model.train()
        learning_rate = scheduled_learning_rate(step, args.steps, warmup_steps, args.learning_rate)
        for group in optimizer.param_groups:
            group["lr"] = learning_rate
        x, y = make_batch(train_data, args.batch_size, args.block_size, device)
        _, loss = model(x, y)
        optimizer.zero_grad(set_to_none=True)
        loss.backward()
        torch.nn.utils.clip_grad_norm_(model.parameters(), 1.0)
        optimizer.step()

        if step == 1 or step % args.eval_interval == 0 or step == args.steps:
            train_loss = evaluate(model, train_data, args.batch_size, args.block_size, device, batches=5)
            val_loss = evaluate(model, val_data, args.batch_size, args.block_size, device, batches=5)
            print(f"step={step} train_loss={train_loss:.4f} val_loss={val_loss:.4f} lr={learning_rate:.8g} device={device}")
            if val_loss < best_val:
                best_val = val_loss
                best_step = step
                best_state_dict = _to_cpu_tree(model.state_dict())
                best_optimizer_state = _to_cpu_tree(optimizer.state_dict())
                best_rng_state = torch.random.get_rng_state().clone()
                best_cuda_rng_state = _to_cpu_tree(torch.cuda.get_rng_state_all()) if device.type == "cuda" else None
                elapsed = previous_elapsed + (time.perf_counter() - run_started)
                tokens_seen = step * args.batch_size * args.block_size
                best_metrics = {
                    "tokens_seen": tokens_seen,
                    "elapsed_seconds": elapsed,
                    "tokens_per_second": tokens_seen / max(elapsed, 1e-9),
                    "peak_cuda_memory_bytes": torch.cuda.max_memory_allocated(device) if device.type == "cuda" else 0,
                }

    if best_state_dict is None or best_optimizer_state is None or best_rng_state is None:
        raise RuntimeError("training completed without a validated resumable checkpoint")
    # Save the model and optimizer state from the same best-validation step.
    model.load_state_dict(best_state_dict, strict=True)

    output = Path(args.output)
    output.parent.mkdir(parents=True, exist_ok=True)
    temporary = output.with_suffix(output.suffix + ".tmp")
    training_metadata = {
        "steps": args.steps,
        "global_step": best_step,
        "starting_step": start_step,
        "batch_size": args.batch_size,
        "block_size": args.block_size,
        "learning_rate": args.learning_rate,
        "eval_interval": args.eval_interval,
        "warmup_steps": warmup_steps,
        "lr_schedule": "linear_warmup_cosine_decay",
        "max_corpus_mb": args.max_corpus_mb,
        "resumed_from": str(resume_path) if resume_path else None,
    }
    payload = {
        "format": "quantum-gpt-research-v1",
        "config": model.config_dict(),
        "state_dict": model.state_dict(),
        "best_validation_loss": best_val,
        "best_step": best_step,
        "global_step": best_step,
        "tokenizer": "utf8-byte-v1",
        "seed": args.seed,
        "dataset": {
            "id": args.dataset_id,
            "sha256": approved_record["sha256"],
            "license_id": approved_record["license_id"],
            "source_url": approved_record["source_url"],
        },
        "training": training_metadata,
        "optimizer_state_dict": best_optimizer_state,
        "rng_state": best_rng_state,
        "cuda_rng_state": best_cuda_rng_state,
        "metrics": best_metrics,
    }
    torch.save(payload, temporary)
    temporary.replace(output)
    print(f"saved={output} parameters={model.parameter_count()} best_step={best_step} best_validation_loss={best_val:.4f} tokens_per_second={best_metrics.get('tokens_per_second', 0.0):.2f}")
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
    parser.add_argument("--warmup-steps", type=int, default=0)
    parser.add_argument("--resume", default=None, help="resume from a validated checkpoint with matching corpus/config")
    parser.add_argument("--cpu", action="store_true")
    return parser.parse_args()


if __name__ == "__main__":
    train(parse_args())
