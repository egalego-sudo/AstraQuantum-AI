# QUANTUM AI capability expansion plan

**Product:** QUANTUM AI  
**Creator credit:** Creator by Bittu Pandit  
**Target:** Android-first personal AI assistant; preserve the existing signed-app identity, private local data, and zero-budget operating constraint.

This plan is an engineering checklist, not a claim that every capability is complete. Current work is isolated in PR #37; no feature changes are merged into the production branch yet. A capability is complete only after implementation, automated tests where practical, and a documented device/backend acceptance test.

## 1. Knowledge and reasoning

**Existing foundation**
- Groq-backed Hindi/English chat with a reasoning-oriented system prompt.
- Scientific question mode separates established facts, models, assumptions, uncertainty and unknowns.
- Optional on-device Qwen text model for offline fallback.

**Work to verify/improve**
- Preserve all parts of multi-part questions and relevant conversation context.
- Make online/offline model identity and limitations visible.
- Add regression tests for Hindi/Hinglish, long multi-part prompts, ambiguity, uncertainty and refusal to invent sources.
- Do not claim parity with ChatGPT's proprietary model or hidden reasoning.

**Acceptance**
- A fixed prompt suite passes for Hindi, English, multi-part reasoning, arithmetic explanation and uncertainty handling.
- Offline fallback is clearly labeled and tested separately from online answers.

## 2. Research and source verification

**Existing foundation**
- Bounded parallel retrieval from OpenAlex, Crossref, Europe PMC, arXiv, Semantic Scholar and Wikipedia.
- DOI record/title matching, source deduplication, relevance ranking and bounded open-access full-text retrieval.
- Claim-level evidence labels and source metadata.

**Work to verify/improve**
- Test source failures, rate limits, empty results, duplicate records, DOI mismatch and prompt-injection text inside retrieved pages.
- Keep metadata-only, abstract and full-text evidence visibly distinct.
- Keep the scope honest: this is bounded public-source research, not exhaustive web browsing or guaranteed truth.

**Acceptance**
- Automated tests cover all source-quality and failure cases.
- Each displayed citation maps to a returned source record; no fabricated source is accepted.

## 3. Coding and building

**Existing foundation**
- App Builder and Website Builder screens.
- AI task planning with explicit confirmation before saving a local checkpoint.

**Incremental implementation on this branch**
- Hardened the generated HTML preview WebView: file/content access and network loads are blocked, mixed content is disallowed, multiple windows and automatic JavaScript popups are disabled, Safe Browsing is enabled where supported, and navigation is restricted to the synthetic preview origin.

**Work to verify/improve**
- Verify generated project files, preview, validation and export flows end-to-end on a real device.
- Add safe code-formatting and static validation where supported.
- Never execute untrusted generated code on the production backend without an isolated sandbox, resource limits and explicit user approval.

**Acceptance**
- A sample Android/web project can be generated, previewed, validated and exported without corrupting existing projects.
- Code execution is not advertised unless a real isolated execution service is configured and tested.

## 4. Memory and continuity

**Existing foundation**
- Encrypted local app preferences, opt-in personal-context sharing, recent chat history and project notes.
- Context selection is bounded and runs locally before the request is sent.

**Incremental implementation on this branch**
- Added an editable saved-memory field so a user can inspect, correct or remove individual notes, and save changes to encrypted local preferences.
- The existing explicit opt-in remains separate; turning it off prevents personal context from being added to chat prompts.

**Work to verify/improve**
- Test secure migration from legacy preferences, process restart, app update, empty/corrupt records and opt-out behavior on a real device.
- Never send personal memory to the backend while opt-in is off.

**Acceptance**
- Existing chats and memories survive an in-place update.
- Memory is excluded from outbound prompts when the setting is off and included only when explicitly enabled.

## 5. Multimedia

**Existing foundation**
- Android speech-to-text and text-to-speech, online image analysis, and text-based PDF extraction.

**Incremental implementation on this branch**
- Added a bounded on-device OCR fallback for scanned PDFs, using Latin and Devanagari recognition models.
- OCR processes one page at a time, up to 60 pages and 80,000 characters, and is attempted only when the backend reports that the PDF has no readable text.

**Work to verify/improve**
- Test JPEG, PNG, WEBP, text PDFs, scanned Hindi/English PDFs, large files, unsupported types and network failures on a real Android device.
- Offline vision and audio-file understanding remain unsupported.
- Preserve file-size and privacy safeguards.

**Acceptance**
- Each supported media type has a successful device/backend test and clear unsupported-format handling.

## 6. Calculation and verification

**Existing foundation**
- Science Lab includes reaction balancing, molar-mass/composition, linear and quadratic equations, force calculation, unit conversion and formula catalog.

**Incremental implementation on this branch**
- Added a bounded deterministic arithmetic-expression parser and a dedicated Calculator screen.
- Added tests for precedence, parentheses, powers, decimals, scientific notation, division by zero, invalid code-like input, overflow and input limits.

**Work to verify/improve**
- Run the new tests and add deterministic tests for the remaining Science Lab calculators, edge cases, units and rounding.
- Keep deterministic calculations separate from model-generated estimates.
- Expand only with a defined domain, tested parser and explicit limits.

**Acceptance**
- Known-answer test vectors pass; invalid inputs return a clear error instead of a misleading numeric result.

## 7. Quantum Lab

**Existing foundation**
- Classical dense and sparse state-vector simulation modes and an optional IBM Quantum connector.
- IBM hardware submission is confirmation-gated and disabled until server-side credentials/configuration are provided.

**Work to verify/improve**
- Verify gate operations, normalization, measurement probabilities, entanglement behavior, simulator limits and resource caps.
- Keep classical simulation clearly distinguished from actual QPU jobs.
- Never report a hardware connection or job as successful without a real IBM response.

**Acceptance**
- Bell-state, gate-inverse, normalization and known-circuit tests pass.
- Real hardware is labeled unavailable until IBM credentials and a verified job exist.

## Cross-cutting security and release checks

- Keep Groq/IBM secrets on the server; never embed them in the APK or source.
- Require the private app token on protected API routes; retain HTTPS, Android lock, encrypted local storage, request limits and bounded payloads.
- Preserve the existing application ID and release signing identity so the update installs over the current app.
- Run backend syntax checks, backend tests, Android unit tests, debug build and signed release build.
- Verify the APK signature and update metadata before publishing.
- On a real Android device, verify fingerprint/device lock, old chat/memory migration, creator credit, update detection, online chat, research, file handling and supported offline fallback.
- Treat security as risk reduction, never as a promise that an app is impossible to hack.

## Completion rule

Do not mark a section complete from UI presence or source-code presence alone. Record implementation commit(s), automated test results, and device/backend acceptance evidence. The final completion report must list any remaining limitations plainly.
