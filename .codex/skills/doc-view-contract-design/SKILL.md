---
name: doc-view-contract-design
description: Use when defining or reviewing Doc View behavior contracts before changing PSI parsing, DTOs, Markdown output, uploads, exports, settings, UI-visible behavior, or IntelliJ compatibility.
license: MIT
---

# Doc View Contract Design

Use this skill before implementation work that changes user-visible or integration-visible behavior.

## Read First

- `AGENTS.md`
- `docs/contract-design.md`
- `docs/architecture.md`
- `docs/intellij-compatibility.md` when IntelliJ API behavior is involved
- `docs/performance-guide.md` when PSI, UI, rendering, export, or upload performance can be affected

## Steps

1. Identify the behavior area: Spring PSI, Dubbo PSI, DTO, Markdown, export, upload, settings, UI, or compatibility.
2. Confirm whether a change note and contract exist; create or update them before runtime implementation.
3. Define inputs: Java sample, annotations, Javadoc, settings, platform state, and external service assumptions.
4. Define expected outputs: `DocView` shape, nested `Body`/`Param`/`Header`, Markdown sections, payloads, UI state, or notifications.
5. Define unsupported or unchanged cases explicitly.
6. Choose validation: unit test, fixture, golden Markdown, payload check, `runIde`, or manual verification.
7. Record risks around PSI recursion, user templates, IntelliJ API compatibility, and sensitive data.

## Guardrails

- Do not change Java code while only designing the contract.
- Do not include real tokens, production URLs, or private user code in contract examples.
- Do not treat implementation details as contract unless users or integrations can observe them.
- Keep production implementation Java-only when the contract later becomes code.
