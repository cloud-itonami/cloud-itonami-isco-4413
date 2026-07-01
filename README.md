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

## License

AGPL-3.0-or-later.
