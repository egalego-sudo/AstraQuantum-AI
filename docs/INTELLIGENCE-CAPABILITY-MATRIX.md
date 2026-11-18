# QUANTUM AI — Intelligence and capability matrix

**Creator by Bittu Pandit**

This matrix describes what the current application can assist with, what is implemented as deterministic software, and what is still unverified. A language model may draft, explain or reason about a domain; that is not equivalent to a certified expert system, compiler, scientific instrument or autonomous agent.

## Current request path

1. Android app collects the user's prompt and optional context under the existing privacy opt-in.
2. Backend applies bounded input handling, task-aware reasoning checks and relevant local knowledge retrieval.
3. Online requests use the configured Groq model. Research requests use bounded public scholarly-source discovery and evidence ranking.
4. The response is returned with the app's existing safety and output limits.
5. The independent `foundation-model/` Transformer is a separate research prototype. It is not yet the production chat model.

## Capability matrix

| Domain | Current assistance | Deterministic implementation / evidence | Remaining limitation |
|---|---|---|---|
| Deep reasoning | Decomposition, root-cause framing, comparisons, strategies, uncertainty checks | Task-aware backend reasoning engine and tests | Model output still requires external evaluation; private chain-of-thought is not exposed |
| Knowledge and education | Explanations from beginner to advanced across science, maths, history, computing, finance, language and other subjects | Local source-attributed knowledge retrieval; science formula/calculator modules | Coverage is not exhaustive; legal/financial/medical answers need jurisdiction and qualified review |
| Mathematics | Explain algebra, geometry, calculus and solve worked examples | Deterministic calculator and selected science calculators | Symbolic algebra/CAS coverage and all formula domains are not complete |
| Programming | Draft, explain, review and debug Python, JS/TS, Java/Kotlin, C/C++, C#, Go, Rust, PHP, Swift, SQL, Verilog/SystemVerilog, Bash, HTML/CSS | Existing app/backend, tests, code-generation screens and CI | Language suggestions are model-generated; not every language has a compiler/runtime in the app; untrusted code is not run on production servers |
| Software engineering | Android/web/backend/API/database design, tests, debugging, GitHub workflow assistance | Existing Android and Node backend, automated CI, bounded input/response handling | Real-device acceptance, production deployment and some integrations remain separate gates |
| Independent AI model | Architecture and training experiments | Original small decoder-only Transformer, byte tokenizer, CPU training/checkpoint path and unit tests | Randomly initialized research model; no pretrained QUANTUM AI weights, lawful large-scale corpus or competitive benchmark yet |
| RAG and knowledge | Search local source-attributed records and add bounded context | Knowledge retrieval and citation validation modules | Seed corpus is small; retrieval is not proof and does not replace live comprehensive search |
| Deep research | Discover and compare scholarly records, inspect available abstracts/open full text, verify DOI metadata | OpenAlex, Crossref, Europe PMC, arXiv, Semantic Scholar and bounded Wikipedia retrieval; ranking and DOI checks | Not exhaustive internet search; metadata-only sources cannot establish claims; provider availability varies |
| Scientific investigation | Form hypotheses, plan experiments, compare theories and draft reports | Research-quality pipeline and task-aware experiment checklist | No physical experiments or independent scientific certification are performed by the app |
| Multimodal | Image analysis, speech input/output and bounded PDF text/OCR paths | Existing Android/backend integrations | Offline vision/audio understanding and some scanned-document cases are not verified |
| Memory and agents | Use selected conversation/project context with user opt-in | Bounded local context selection and privacy gate | Not a universal persistent autonomous agent; external actions require explicit integrations and confirmations |
| Deployment and operations | Help diagnose CI/build/deployment issues | GitHub Actions workflows and repository automation | A passing CI run does not prove production health or a successful real-phone installation |

## Evidence labels

- **IMPLEMENTED:** source code exists and is connected to the application path.
- **AUTOMATED TESTED:** the relevant test suite passed on the identified commit.
- **DEVICE VERIFIED:** tested on a real Android device with evidence.
- **PRODUCTION VERIFIED:** live service behavior was checked in its actual production environment.
- **NOT VERIFIED:** no sufficient test evidence exists yet.

Do not promote an item from one status to another based only on a UI screen, prompt, README or successful compilation.

## Evaluation plan

Build a fixed bilingual evaluation suite with:
- Multi-part reasoning and instruction coverage.
- Root-cause diagnosis with known injected faults.
- Arithmetic, units, algebra and selected science answer keys.
- Hindi/English education explanations with misconception checks.
- Code generation and static/unit tests for representative supported languages.
- Research citation mapping, DOI mismatch, source conflicts, metadata-only evidence and prompt injection.
- Hallucination/uncertainty handling and refusal to fabricate sources.
- Latency, memory, battery and thermal measurements on representative Android devices.
- Separate online Groq, third-party offline model and future QUANTUM AI-owned model scores.

Every benchmark must record the exact model identifier, prompt set version, date, data provenance, scoring method and failures. Never combine different model identities into one score.

## Safety and claims

- Never claim GPT-6 source code, weights, hidden prompts or parity.
- Never call the experimental Transformer a capable general assistant before held-out evaluation.
- Do not train on private conversations, secrets or unlicensed data.
- Do not execute arbitrary generated code in the production backend.
- Treat legal, medical, financial and safety-critical output as general information requiring appropriate professional verification.
- Scientific research support is not proof of a new discovery.
