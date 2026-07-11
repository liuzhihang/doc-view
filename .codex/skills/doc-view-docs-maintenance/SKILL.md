---
name: doc-view-docs-maintenance
description: Use when creating or updating Doc View repository documentation, AGENTS.md, AI workflow guidance, OpenSpec configuration, or repo-local Codex skills.
---

# Doc View Docs Maintenance

Use this skill for repository guidance and AI workflow changes.

## Read First

- `AGENTS.md`
- `docs/ai-workflow.md`
- The target canonical doc and its inbound links
- `docs/release-checklist.md` when verification or release guidance changes
- Active OpenSpec context when the documentation describes behavior, architecture, compatibility, or workflow requirements

## Steps

1. Confirm whether the change is mechanical documentation-only or describes a runtime/workflow behavior change.
2. Keep `AGENTS.md` as the concise entry point; put detail in architecture, contract, performance, compatibility, AI workflow, roadmap, or release docs.
3. Keep facts in their authoritative source: versions/build facts in Gradle/plugin files, architecture in source plus `architecture.md`, normative requirements in OpenSpec specs.
4. Keep repo-local skills workflow-oriented and link canonical docs instead of copying project facts or full OpenSpec artifacts.
5. Update indexes and inbound links when a canonical doc or skill moves.
6. Verify OpenSpec status when applicable, file/link existence, stale workflow terms, secrets, `git diff --check`, and diff scope.

## Guardrails

- Do not modify Java, Gradle, `plugin.xml`, runtime resources, templates, icons, forms, or message bundles in documentation-only work.
- Do not maintain `docs/contracts/**` as a second normative spec system.
- Do not claim runtime tests passed when only documentation checks ran.
- Keep examples free of credentials and private project code.
- Keep task checkboxes aligned with actual implementation and verification.
