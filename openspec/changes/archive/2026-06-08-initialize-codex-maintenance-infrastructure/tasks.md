## 1. Repository Rule Entry Point

- [x] 1.1 Expand the existing root `AGENTS.md` into the primary Codex-readable project rule file.
- [x] 1.2 Add links from `AGENTS.md` to every new maintenance document under `docs/`.
- [x] 1.3 Document the Java-only production policy, documentation-only scope for this change, validation commands, and AI collaboration expectations in `AGENTS.md`.
- [x] 1.4 Confirm `AGENTS.md` does not instruct agents to modify Java source, Gradle behavior, plugin descriptors, runtime resources, or business logic for this change.
- [x] 1.5 Replace `CLAUDE.md` with a symlink to `AGENTS.md` so only one AI instruction file must be maintained.

## 2. Maintenance Documentation Suite

- [x] 2.1 Create `docs/architecture.md` describing current Doc View architecture, service boundaries, PSI utilities, UI forms, integrations, settings, and runtime extension points.
- [x] 2.2 Create `docs/roadmap.md` describing staged maintenance priorities for infrastructure, contracts, parser correctness, UI polish, integrations, compatibility, and MCP exploration.
- [x] 2.3 Create `docs/contract-design.md` defining contract-first strategy for PSI parsing, DTO shape, generated Markdown, integration payloads, IntelliJ compatibility, and regression validation.
- [x] 2.4 Create `docs/performance-guide.md` covering PSI read action discipline, EDT/UI responsiveness, template rendering, export behavior, network integration boundaries, caching, and validation.
- [x] 2.5 Create `docs/intellij-compatibility.md` covering target IDE version, bundled plugins, Gradle IntelliJ plugin expectations, API compatibility rules, verification, and upgrade workflow.
- [x] 2.6 Create `docs/vibe-coding-workflow.md` covering OpenSpec proposal/apply/archive flow, Codex collaboration constraints, review checkpoints, and documentation upkeep.
- [x] 2.7 Create `docs/mcp-design.md` documenting MCP roadmap, candidate capabilities, security boundaries, staged adoption, and explicit non-implementation in this change.
- [x] 2.8 Create `docs/i18n.md` documenting localization policy for UI strings, message bundles, generated Markdown language, settings labels, and future translation work.
- [x] 2.9 Create `docs/release-checklist.md` documenting pre-release verification, changelog, plugin verification, manual IDE checks, marketplace preparation, and rollback notes.
- [x] 2.10 Review the documentation suite for consistency with Java 17, Gradle 8.5, IntelliJ IDEA 2024.1, Lombok, Velocity templates, and the existing plugin architecture.

## 3. Repo-local Codex Skills

- [x] 3.1 Add `.codex/skills/doc-view-contract-design/SKILL.md` for future contract-design work.
- [x] 3.2 Add `.codex/skills/doc-view-java-maintenance/SKILL.md` for Java-only feature and bugfix maintenance.
- [x] 3.3 Add `.codex/skills/doc-view-docs-maintenance/SKILL.md` for documentation updates that must stay aligned with `AGENTS.md` and `docs/`.
- [x] 3.4 Add `.codex/skills/doc-view-release-readiness/SKILL.md` for release preparation and verification.
- [x] 3.5 Ensure each new skill references `AGENTS.md` and relevant `docs/` files instead of duplicating long-form policy text.

## 4. Documentation-only Verification

- [x] 4.1 Verify the final changed files are limited to `AGENTS.md`, `CLAUDE.md` as a symlink, `docs/**/*.md`, `.codex/skills/*/SKILL.md`, and OpenSpec planning artifacts.
- [x] 4.2 Verify no Java source files under `src/`, Gradle files, plugin descriptor files, templates, icons, or message bundle resources were modified.
- [x] 4.3 Run `openspec status --change "initialize-codex-maintenance-infrastructure"` and confirm the change artifacts are complete.
- [x] 4.4 Run a lightweight content check that all required docs and skill files exist.
- [x] 4.5 Run `./gradlew test` only if implementation unexpectedly touches Java, Gradle, plugin descriptor, or runtime resource files; otherwise document that runtime tests were skipped because the diff is documentation-only.

Runtime test note: `./gradlew test` was skipped because verification showed this implementation only touched repository guidance, the `CLAUDE.md` symlink, documentation, repo-local Codex skills, and OpenSpec planning artifacts.
