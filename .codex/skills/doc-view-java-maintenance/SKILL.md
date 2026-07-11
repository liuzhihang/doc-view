---
name: doc-view-java-maintenance
description: Use when implementing or reviewing Java-only Doc View production changes, especially PSI parsing, services, DTOs, UI, integrations, settings, export, or IntelliJ Platform behavior.
---

# Doc View Java Maintenance

Use this skill for production plugin work after an observable behavior contract is apply-ready.

## Read First

- `AGENTS.md`
- `docs/architecture.md`
- Every active apply `contextFiles` path returned by OpenSpec; runtime work requires proposal/design/spec/tasks context
- `docs/contract-design.md` for PSI, DTO, Markdown, upload, export, settings, writeback, or UI behavior
- `docs/performance-guide.md` for PSI, recursion, cache, background task, rendering, export, upload, or UI responsiveness
- `docs/intellij-compatibility.md` for IntelliJ API, Gradle platform, descriptor, extension, or compatibility work

## Steps

1. Confirm the active OpenSpec change authorizes runtime files and defines expected behavior and validation.
2. Locate the narrow package boundary: `service`, `utils`, `dto`, `ui`, `integration`, `config`, `provider`, or `action`.
3. Write the failing contract test or fixture before production code.
4. Preserve Java-only production code and prefer existing services, DTOs, utilities, and public IntelliJ API patterns.
5. For PSI changes, validate Spring/Feign/Dubbo impact as applicable and guard recursion, invalid elements, Dumb Mode, read actions, and project disposal.
6. For UI/action work, keep `update` lightweight and heavy work off EDT.
7. For integration work, protect secrets and make authentication/network/server failures understandable.
8. Implement the smallest change, run targeted then full verification, and update OpenSpec tasks only after success.

## Guardrails

- Do not change generated Markdown shape, payloads, settings, UI, or compatibility without matching OpenSpec requirements.
- Do not modify `plugin.xml`, Gradle, dependencies, or resources unless the approved change explicitly requires them.
- Do not add Kotlin, Groovy, scripts, generated runtime sources, or new production languages.
- Do not mark tasks complete before code and exact verification are complete.
- Do not revert unrelated user changes.
