---
name: doc-view-docs-maintenance
description: Use when creating or updating Doc View repository documentation, AGENTS.md, OpenSpec guidance, or repo-local Codex skills.
license: MIT
---

# Doc View Docs Maintenance

Use this skill for documentation and AI workflow changes.

## Read First

- `AGENTS.md`
- The target file under `docs/`
- `docs/vibe-coding-workflow.md`
- `docs/release-checklist.md` when release or verification guidance changes

## Steps

1. Confirm whether the change is documentation-only or describes a runtime behavior change.
2. Keep `AGENTS.md` as the concise entry point and link to deeper docs instead of duplicating long sections.
3. Update the most specific doc for the topic: architecture, roadmap, contract design, performance, compatibility, workflow, MCP, i18n, or release.
4. If adding a repo-local skill, make it workflow-oriented and reference canonical docs instead of copying all rules.
5. Check that docs use current project facts: Java 17, Gradle 8.5, IntelliJ IDEA 2024.1, plugin version 1.3.11, Java and Markdown bundled plugins.
6. Verify file existence, links, and diff scope.

## Guardrails

- Do not modify Java source, Gradle files, `plugin.xml`, runtime resources, templates, icons, or message bundles for documentation-only tasks.
- Do not claim tests passed when only documentation checks were run.
- Keep examples free of credentials and private project code.
- Keep OpenSpec task checkboxes aligned with actual completed work.
