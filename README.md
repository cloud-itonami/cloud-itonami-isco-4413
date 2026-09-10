# cloud-itonami-isco-4413

Open Occupation Blueprint for **ISCO-08 4413**: Coding, Proofreading and Related Clerks.

This repository designs a forkable OSS business for an independent document-processing clerk: a document-scanning robot performs page scanning and physical filing under a governor-gated actor, so the practice keeps its own processing records instead of renting a closed document-management SaaS.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a document-scanning robot performs page scanning, OCR staging and physical filing under an actor that proposes
actions and an independent **Document Processing Governor** that gates them. The governor never
dispatches hardware itself; `:high`/`:safety-critical` actions (such as
handling confidential or regulated documents) require human sign-off.

A live sample of the operator console (robotics safety console, shared template) is rendered in [docs/samples/operator-console.html](docs/samples/operator-console.html) — pure-data HTML output of `kotoba.robotics.ui`.

## Core Contract

```text
document batch + coding scheme + confidentiality policy
        |
        v
Document Advisor -> Document Processing Governor -> code/proofread, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses, suppress
an operating record, or disclose sensitive data without governor approval and
audit evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `4413`). Required capabilities:

- :robotics
- :forms
- :audit-ledger
- :bpmn

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## Reference implementation (`:maturity :implemented`)

Full itonami Actor pattern (per ADR-2607011000 / CLAUDE.md's Actors
section, alongside `cloud-itonami-isco-6130`, `-8160`, `-2166`, `-2641`,
`-2651`, `-2652`, `-2654`, `-1219`, `-1223`, `-1330`, `-1341`, `-1349`,
`-1412`, `-1439`, `-2144`, `-2320`, `-2411`, `-2422`, `-2431`, `-2621`,
`-2634`, `-3122`, `-3123`, `-3141`, `-3255`, `-3339`, `-3512`, `-4120`,
`-4131`, `-4132`, `-4211`, `-4224`, `-4229` and `-4322`): a real
[`kotoba-lang/langgraph`](https://github.com/kotoba-lang/langgraph)
`StateGraph`, with the Advisor and Governor as distinct graph nodes and
human-in-the-loop interrupt/resume via checkpointing.

```text
:intake -> :advise -> :govern -> :decide -+-> :commit            (:ok? true)
                                           +-> :request-approval   (:escalate? true, interrupt-before)
                                           +-> :hold               (:hard? true)
```

- `src/document_processing/store.kotoba` — `Store` protocol +
  `MemStore`: registered document batches, committed records, an
  append-only audit ledger.
- `src/document_processing/advisor.kotoba` — `Advisor` protocol;
  `mock-advisor` (deterministic, default) proposes a coding or
  proofreading operation from a request; `llm-advisor` wraps a
  `langchain.model/ChatModel` — either way the advisor only ever
  produces a `:propose`-effect proposal, never a committed record, and
  LLM parse failures always yield `confidence 0.0` (forces escalation,
  never fabricated confidence).
- `src/document_processing/governor.kotoba` —
  `DocumentProcessingGovernor/check`: a pure function, wired as its own
  `:govern` node. Hard invariants (unregistered batch, a proposal
  whose `:effect` isn't `:propose`) always route to `:hold`. Escalation
  invariants (`:handle-confidential-document`,
  `:handle-regulated-document`, or low advisor confidence) always
  route to `:request-approval` — an `interrupt-before` node that the
  graph checkpoints and only resumes on explicit human approval
  (`actor/approve!`), matching the README's robotics-premise statement
  that handling confidential or regulated documents always requires
  human sign-off.
- `src/document_processing/actor.kotoba` — `build-graph`, `run-request!`,
  `approve!`: the `langgraph.graph/state-graph` wiring itself.

```bash
clojure -M:test
```

This is what backs this repo's `:maturity :implemented` entry in
[`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation).

## License

AGPL-3.0-or-later.
