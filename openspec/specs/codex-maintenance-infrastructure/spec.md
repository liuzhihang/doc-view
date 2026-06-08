## Purpose

Doc View maintains a single repository rule entry point, supporting documentation, repo-local Codex skills, and verification guidance for Codex-based maintenance workflows.

## Requirements

### Requirement: Codex-readable repository rules

The repository SHALL provide an `AGENTS.md` file at the repository root that AI agents can use as the primary source of project rules. The file MUST describe the project scope, Java-only production policy, business-logic freeze for this change, validation commands, architecture entry points, documentation index, and AI collaboration expectations. `CLAUDE.md` MUST point to `AGENTS.md` as a symlink so the repository has one canonical instruction source.

#### Scenario: Codex loads project rules

- **WHEN** an AI agent starts work in the repository
- **THEN** `AGENTS.md` provides enough rules and links for the agent to understand the allowed workflow before editing files

#### Scenario: Runtime behavior remains out of scope

- **WHEN** this infrastructure change is implemented
- **THEN** `AGENTS.md` states that Java source, Gradle behavior, plugin descriptors, and runtime resources are not part of the change

#### Scenario: Claude instructions reuse AGENTS

- **WHEN** a maintainer inspects `CLAUDE.md`
- **THEN** it is a symlink to `AGENTS.md` rather than a second copy of the same guidance

### Requirement: Maintenance documentation suite

The repository SHALL include the requested documentation files under `docs/`: `architecture.md`, `roadmap.md`, `contract-design.md`, `performance-guide.md`, `intellij-compatibility.md`, `vibe-coding-workflow.md`, `mcp-design.md`, `i18n.md`, and `release-checklist.md`.

#### Scenario: Required docs exist

- **WHEN** a maintainer lists the `docs/` directory after implementation
- **THEN** every requested documentation file exists with content specific to Doc View maintenance

#### Scenario: Documentation covers acceptance topics

- **WHEN** a maintainer reads the documentation suite
- **THEN** contract strategy, Java-only policy, performance guidance, MCP roadmap, IntelliJ compatibility, Vibe Coding workflow, i18n policy, and release checklist are all documented

### Requirement: Contract design strategy

The repository SHALL document a contract-first strategy for future behavior changes. The strategy MUST cover generated Markdown shape, PSI parsing expectations, DTO fields, platform integration payloads, IntelliJ compatibility assumptions, and how future changes should convert contracts into validation.

#### Scenario: Future feature begins with a contract

- **WHEN** a future feature changes parsing, generated documentation, DTO mapping, integration behavior, or compatibility behavior
- **THEN** the maintainer can use `docs/contract-design.md` to define the expected contract before changing Java code

### Requirement: Java-only production development policy

The repository SHALL document that production plugin implementation remains Java-only unless a future accepted proposal changes that policy. The policy MUST allow Markdown documentation, OpenSpec artifacts, Codex skills, YAML metadata, and existing Gradle build files as repository infrastructure.

#### Scenario: Production implementation language is constrained

- **WHEN** a future change proposes runtime plugin code
- **THEN** the documented policy requires Java production code and rejects new Kotlin, Groovy, scripting, or generated runtime-language additions unless separately approved

### Requirement: Performance and compatibility guidance

The repository SHALL document performance and IntelliJ compatibility rules for future changes, including PSI read actions, UI thread discipline, template generation costs, network integration boundaries, export behavior, target IDE compatibility, and plugin verification expectations.

#### Scenario: Future behavior change has performance guardrails

- **WHEN** a future change touches PSI parsing, UI rendering, template generation, export, or remote integration behavior
- **THEN** maintainers can consult the performance and compatibility docs for required constraints and verification steps

### Requirement: MCP roadmap and boundaries

The repository SHALL document an MCP design roadmap that identifies potential MCP capabilities, staged adoption, security boundaries, transport assumptions, and why MCP work is not implemented in this infrastructure change.

#### Scenario: MCP remains roadmap-only

- **WHEN** this infrastructure change is implemented
- **THEN** no MCP server, runtime integration, dependency, or plugin behavior is added

### Requirement: Repo-local Codex skills

The repository SHALL include Doc View specific `.codex/skills/*/SKILL.md` files that encode repeatable AI collaboration workflows. These skills MUST reference `AGENTS.md` and the relevant `docs/` files rather than duplicating all documentation content.

#### Scenario: AI workflow can use repo skills

- **WHEN** an AI agent works on contract design, Java-only implementation planning, documentation maintenance, or release readiness
- **THEN** a repo-local Codex skill provides workflow steps and points back to the canonical repository docs

### Requirement: Documentation-only implementation

The implementation of this change SHALL be limited to repository guidance, the `CLAUDE.md` symlink, OpenSpec artifacts, documentation, and Codex skill metadata. It MUST NOT modify Java source files, Gradle configuration, plugin descriptors, runtime resources, generated templates, tests that alter behavior, or dependency declarations.

#### Scenario: No runtime behavior change

- **WHEN** a maintainer reviews the final diff for this change
- **THEN** all changed files are documentation, the `CLAUDE.md` symlink, OpenSpec planning files, or `.codex/skills/*/SKILL.md` files and no runtime plugin behavior changes are present
