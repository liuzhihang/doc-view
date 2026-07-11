---
name: doc-view-contract-design
description: Use when defining or reviewing Doc View behavior contracts before changing PSI parsing, DTOs, Markdown output, uploads, exports, settings, UI-visible behavior, or IntelliJ compatibility.
---

# Doc View Contract Design

Use this skill before implementation work that changes user-visible or integration-visible behavior. OpenSpec owns proposal/design/spec/tasks; this skill supplies Doc View domain checks.

## Read First

- `AGENTS.md`
- `docs/contract-design.md`
- The active change status and every `contextFiles` path returned by `openspec instructions apply --change <name> --json`
- `docs/architecture.md` when package, service, DTO, settings, extension, or data flow boundaries are involved
- `docs/performance-guide.md` when PSI, UI, rendering, export, upload, recursion, or cache performance can change
- `docs/intellij-compatibility.md` when IntelliJ API, Gradle platform, descriptor, extension, or compatibility behavior is involved

## Steps

1. Identify the observable area: Spring/Feign/Dubbo PSI, DTO, Markdown, export, upload, settings, UI, performance, or compatibility.
2. Confirm an apply-ready OpenSpec change exists for behavior work; write normative requirements to its `specs/**`, not `docs/contracts/**`.
3. Define Java/settings/platform/external-service inputs and assumptions.
4. Define expected `DocView`/`Body`/`Param`/`Header`, Markdown, payload, UI, notification, or compatibility outputs.
5. Define unsupported, fallback, sensitive-data, and explicitly unchanged cases.
6. Choose direct validation: PSI fixture, DTO assertion, golden Markdown, payload check, `runIde`, or Plugin Verifier.
7. Review recursion, user-template, IntelliJ lifecycle, EDT, and secret-handling risks.

## Guardrails

- Do not change Java while only designing the contract.
- Do not create a parallel contract under `docs/contracts/**`; OpenSpec specs are normative.
- Do not include real tokens, production URLs, or private user code in artifacts or fixtures.
- Do not make implementation details normative unless users or integrations observe them.
- Keep later production implementation Java-only.
