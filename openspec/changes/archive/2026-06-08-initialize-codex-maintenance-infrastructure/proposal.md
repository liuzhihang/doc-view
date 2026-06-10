## Why

Doc View will be maintained with Codex-assisted, Vibe Coding workflows, but the repository does not yet have a complete, shared rule set for AI collaboration, architecture decisions, contract design, validation, and release hygiene. Establishing this infrastructure before feature work reduces ambiguity and gives both human maintainers and AI agents a stable operating contract.

## What Changes

- Add a repository-level `AGENTS.md` that defines Codex-readable project rules, Java-only expectations, allowed workflows, validation commands, and non-goals.
- Make `CLAUDE.md` a symlink to `AGENTS.md` so the repository maintains one canonical AI instruction file.
- Add documentation covering architecture, roadmap, contract design, performance guidance, IntelliJ compatibility, Vibe Coding workflow, MCP design, i18n, and release readiness.
- Add `.codex/skills/*/SKILL.md` entries that encode repeatable AI collaboration workflows for this repository.
- Define contract design strategy before implementation work begins, including how API documentation behavior and generated outputs should be specified.
- Define Java-only development constraints for production plugin code and supporting contribution guidance.
- Keep all changes limited to repository infrastructure and documentation, with no business logic, runtime behavior, dependency, or plugin descriptor changes.

## Capabilities

### New Capabilities

- `codex-maintenance-infrastructure`: Defines the repository documentation, AI-agent rules, Codex skill workflows, contract design strategy, Java-only policy, validation process, performance guidance, MCP roadmap, i18n guidance, IntelliJ compatibility policy, and release checklist required for Codex-based maintenance.

### Modified Capabilities

- None.

## Impact

- Affected files: `AGENTS.md`, `CLAUDE.md` as a symlink, `docs/*.md`, `.codex/skills/*/SKILL.md`, and OpenSpec planning artifacts.
- Affected systems: repository maintenance workflow, contributor guidance, AI collaboration process, contract planning, release readiness process.
- Not affected: Java source code, plugin runtime behavior, Gradle configuration, IntelliJ plugin descriptor, bundled dependencies, generated documentation behavior.
