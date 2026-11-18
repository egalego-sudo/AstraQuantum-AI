# QUANTUM AI — Training Dataset Rights and Feasibility Review

**Creator by Bittu Pandit**  
**Review date:** 2026-11-18  
**Purpose:** identify candidate Hindi/English training and evaluation sources without downloading, copying, approving or training on them.

## Current hard gate

`foundation-model/datasets/manifest.json` remains empty. No corpus is approved. A dataset card's public availability or a CC0 label on a packaging layer does not automatically establish rights to every underlying third-party text item. The project must retain source-level provenance, license, attribution, privacy/consent and permitted-use evidence.

The current training tool defaults to a 64 MiB corpus limit and a small byte-level Transformer. That is a reproducibility/pipeline test setup, not a path to train a competitive general-purpose model on a ₹0 budget. No large-scale training run or capability claim is justified.

## Candidate review

| Candidate | Published facts | Potential use | Decision for QUANTUM AI now |
|---|---|---|---|
| AI4Bharat IndicCorpV2 | Dataset card describes about 481M rows, 20.9B tokens across 24 Indic languages and a 275 GB dataset; it says datasets created for the work are CC0. | Hindi/Indic monolingual pretraining candidate after source provenance and rights review. | **Not approved.** 275 GB is far beyond current free storage/compute. The card does not provide item-level source provenance sufficient to automatically clear all underlying content. Do not download or add to manifest yet. |
| AI4Bharat Samanantar | Official project page says 49.6M English–Indic sentence pairs, that AI4Bharat does not own the underlying text, and that the packaging is CC0. The Hugging Face dataset card labels the dataset CC BY-NC 4.0 and lists many third-party source domains. | Translation/alignment research or a separate translation benchmark. | **Not approved.** There is a license-description discrepancy between the official packaging statement and the mirror's CC BY-NC label; underlying text rights remain with source owners. Require written clarification and source-wise license review before use. |
| Mozilla Common Voice Hindi 27.0 | Mozilla Data Collective lists Hindi scripted speech as CC0-1.0, ASR task, MP3, about 546.57 MB. Mozilla's terms require contributors to assure their contributions are their own and do not infringe third-party rights. | Future Hindi speech-recognition/voice model, not text-only language-model pretraining. | **Not approved for current text model.** Audio needs a separate ASR architecture, audio preprocessing, speaker/privacy checks and an independent consent/quality review. |
| Wikimedia text dumps | Wikimedia's terms license contributor text under CC BY-SA 4.0 and GFDL (subject to project-specific exceptions). | Potential source-attributed retrieval or training after compatibility review. | **Not approved yet.** Attribution and ShareAlike obligations, page history, templates, third-party material and downstream model-use compatibility require a deliberate legal review. |
| OpenStax Physics and Biology 2e | The title-specific prefaces state that the books may not be used to train LLMs or ingested into LLM/generative-AI offerings without prior written permission. License terms vary by title. | Only after written permission for AI use. | **Disabled in RAG and training pending permission.** Five existing derived seed records are marked `license_review_pending` and excluded by the knowledge loader. Do not re-enable without written permission. |

## Source references

- IndicCorpV2 dataset card: https://huggingface.co/datasets/ai4bharat/IndicCorpV2
- Samanantar official AI4Bharat page: https://indicnlp.ai4bharat.org/samanantar/
- Samanantar Hugging Face card: https://huggingface.co/datasets/ai4bharat/samanantar
- Common Voice dataset catalogue: https://commonvoice.mozilla.org/en/datasets
- Common Voice terms: https://commonvoice.mozilla.org/en/terms
- Wikimedia Foundation Terms of Use: https://foundation.wikimedia.org/wiki/Terms_of_Use
- OpenStax Physics preface: https://openstax.org/books/physics/pages/preface
- OpenStax Biology 2e preface: https://openstax.org/books/biology-2e/pages/preface

## Recommended zero-budget sequence

1. **Evaluation first:** author a small original Hindi/English benchmark for logic, arithmetic, coding, factuality and instruction following. Keep it in a separate held-out test split, record authorship/provenance and obtain human review before marking it approved.
2. **Training pilot second:** use only original/commissioned text or a small corpus whose exact source, license and AI-training permission have been verified. A small pilot can test the pipeline; it cannot establish broad intelligence.
3. **Keep ASR separate:** Common Voice audio cannot be fed into the current UTF-8 byte text trainer. Build and evaluate a dedicated speech model only after its consent/rights and compute requirements are addressed.
4. **Do not bulk-download:** do not fetch 275 GB IndicCorpV2 or 7.22 GB Samanantar on a zero-budget mobile workflow without a storage/compute plan and written rights clearance.
5. **Human approval is mandatory:** the code may validate hashes and manifest fields, but it cannot grant licenses, determine third-party ownership or approve personal-data consent.

## Current decision

- Approved training datasets: **0**
- Approved held-out evaluation datasets: **0**
- Actual pretraining runs: **0**
- OpenStax-derived knowledge records: **5 retained for audit, disabled from retrieval pending permission**
- Next safe technical task: prepare original benchmark templates and continue independent tokenizer/training reliability work without representing synthetic tests as model training.
