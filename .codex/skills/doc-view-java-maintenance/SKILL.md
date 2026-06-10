---
name: doc-view-java-maintenance
description: Use when implementing or reviewing Java-only Doc View production changes, especially PSI parsing, services, DTOs, UI, integrations, settings, export, or IntelliJ Platform behavior.
license: MIT
---

# Doc View Java Maintenance

Use this skill for production plugin work after the behavior contract is clear.

## Read First

- `AGENTS.md`
- `docs/architecture.md`
- `docs/contract-design.md`
- `docs/performance-guide.md`
- `docs/intellij-compatibility.md`
- The active OpenSpec proposal, design, specs, and tasks when present

## Steps

1. Confirm the change is allowed to touch runtime code; documentation-only changes must not use this skill to edit Java.
2. Locate the narrow package boundary: `service`, `utils`, `dto`, `ui`, `integration`, `config`, `provider`, or `action`.
3. Preserve Java-only production code. Do not add Kotlin, Groovy, scripts, generated runtime sources, or new production languages.
4. Prefer existing services, DTOs, utilities, and IntelliJ API patterns before adding new abstractions.
5. For PSI changes, validate both Spring and Dubbo impact and guard recursion, invalid elements, and Dumb Mode.
6. For UI changes, keep action `update` lightweight and avoid long work on EDT.
7. For integration changes, protect tokens and make failures understandable.
8. Run the smallest verification set that proves the change, then report exact commands and results.

## Guardrails

- Do not modify `plugin.xml`, Gradle files, or dependencies unless the OpenSpec change explicitly requires it.
- Do not change generated Markdown shape without a contract update.
- Do not mark tasks complete before code and verification are actually done.
- Do not revert unrelated user changes.
