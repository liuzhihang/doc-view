## Context

Doc View is a Java 17 IntelliJ IDEA plugin maintained with Gradle and JetBrains platform APIs. The repository already has a lightweight `AGENTS.md` and OpenSpec workflow skills, but it lacks the broader maintenance infrastructure needed for Codex-assisted development: architecture rules, contract strategy, Java-only constraints, validation expectations, performance guidance, MCP planning, i18n policy, and release readiness.

The change is documentation and workflow only. It must not touch Java production code, Gradle behavior, plugin descriptors, resources used at runtime, or integration behavior.

Stakeholders are maintainers, contributors, and AI agents working in this repository. The main design concern is to make constraints explicit enough for future changes without turning the docs into stale duplication of source code.

## Goals / Non-Goals

**Goals:**

- Make `AGENTS.md` the entry point for Codex-readable repository rules.
- Add focused documents under `docs/` for architecture, roadmap, contract design, performance, IntelliJ compatibility, Vibe Coding, MCP design, i18n, and release readiness.
- Add repo-local `.codex/skills/*/SKILL.md` files for repeatable AI collaboration workflows that are specific to Doc View.
- Define a Java-only production code strategy and a validation workflow before future feature work.
- Preserve current runtime behavior completely.

**Non-Goals:**

- No Java source changes.
- No Gradle, dependency, plugin descriptor, template, icon, or message-bundle changes.
- No implementation of MCP features, YApi/ShowDoc/YuQue behavior, parser changes, UI changes, or generated Markdown changes.
- No archival of the OpenSpec change as part of implementation.

## Decisions

### Decision: Use `AGENTS.md` as the primary AI contract

`AGENTS.md` will become the concise rule entry point and will link to deeper documents. `CLAUDE.md` will be a symlink to `AGENTS.md`, so Codex and Claude read the same canonical instructions. This lets AI agents load the critical constraints immediately while keeping long-form architecture and workflow guidance in `docs/`.

Alternative considered: keep separate `AGENTS.md` and `CLAUDE.md` files. This was rejected because duplicated instruction files drift easily. Another alternative was to put all rules directly in `AGENTS.md`; this was rejected because the requested material covers several independent topics and would make the entry file too large to scan.

### Decision: Keep documentation topic-specific

Each requested document will own one maintenance concern:

- `docs/architecture.md`: current plugin architecture, module boundaries, and ownership.
- `docs/roadmap.md`: maintenance roadmap and staged priorities.
- `docs/contract-design.md`: strategy for defining parser, DTO, Markdown, integration, and compatibility contracts.
- `docs/performance-guide.md`: PSI, UI, template, export, and network performance rules.
- `docs/intellij-compatibility.md`: IntelliJ platform compatibility and upgrade rules.
- `docs/vibe-coding-workflow.md`: AI collaboration lifecycle and review expectations.
- `docs/mcp-design.md`: future MCP roadmap and boundaries.
- `docs/i18n.md`: localization policy for messages, UI text, and generated docs.
- `docs/release-checklist.md`: verification and release gate checklist.

Alternative considered: create one handbook document. This was rejected because separate files make future updates easier to review and align with the user's requested file list.

### Decision: Add repo-specific Codex skills for recurring workflows

The repository already contains OpenSpec workflow skills. This change will add Doc View specific skills for maintenance workflows, such as contract design, Java-only implementation discipline, documentation maintenance, and release readiness. These skills will reference `AGENTS.md` and `docs/` instead of duplicating the full rules.

Alternative considered: rely only on OpenSpec skills. This was rejected because OpenSpec guides planning mechanics, while Doc View also needs repository-specific engineering constraints.

### Decision: Treat contract design as documentation-first

The contract strategy will describe how future changes define expected behavior before code changes, including PSI parsing inputs, DTO fields, Markdown output shape, integration request/response expectations, IntelliJ compatibility, and regression fixtures. Contracts will be documented before implementation and converted into tests when behavior changes.

Alternative considered: define contracts only through tests. This was rejected for the initial infrastructure step because the current scope forbids business logic changes and runtime test fixtures may require follow-up work.

### Decision: Enforce Java-only production policy through guidance

The Java-only rule will apply to production plugin code. Supporting repository docs, OpenSpec artifacts, and Codex skill files can remain Markdown/YAML as appropriate. Build scripts remain Gradle files already present in the repository unless a future approved change revises the build system.

Alternative considered: ban all non-Java files. This was rejected because the requested infrastructure itself is documentation and workflow metadata.

## Risks / Trade-offs

- [Risk] Documentation can drift from implementation over time. -> Mitigation: `AGENTS.md` and release checklist will require updating docs when behavior or architecture changes.
- [Risk] Too many rules can slow future changes. -> Mitigation: keep `AGENTS.md` concise and put deeper guidance in topic-specific docs.
- [Risk] Codex skills can duplicate docs and become stale. -> Mitigation: skills should reference docs and encode workflow steps, not restate every policy.
- [Risk] Contract strategy may feel abstract until tests are added. -> Mitigation: tasks will require concrete examples and future-work notes for test fixtures without changing runtime behavior now.

## Migration Plan

1. Expand `AGENTS.md` to reference the new maintenance rules and docs.
2. Replace `CLAUDE.md` with a symlink to `AGENTS.md`.
3. Create the requested `docs/` files.
4. Add Doc View specific `.codex/skills/*/SKILL.md` workflow files.
5. Review all changed files to confirm they are documentation or workflow metadata only.
6. Run lightweight repository verification for non-runtime changes.

Rollback is straightforward: revert the documentation, skill, and OpenSpec artifact changes. No persisted runtime state or plugin migration is involved.

## Open Questions

- None for this infrastructure proposal. Specific MCP server capabilities, contract fixtures, and IntelliJ upgrade targets should be proposed as separate future changes.
