---
name: doc-view-release-readiness
description: Use when preparing, checking, or reviewing a Doc View plugin release, including changelog, version, plugin verification, manual IDE validation, Marketplace preparation, and rollback notes.
license: MIT
---

# Doc View Release Readiness

Use this skill before publishing or declaring a release candidate ready.

## Read First

- `AGENTS.md`
- `docs/release-checklist.md`
- `docs/intellij-compatibility.md`
- `CHANGELOG.md`
- `gradle.properties`
- Active change notes and contracts related to the release

## Steps

1. Identify the release scope: documentation-only, bugfix, parser behavior, UI, integration, compatibility, or packaging.
2. Check version and changelog expectations.
3. Inspect diff scope for unintended runtime, descriptor, dependency, resource, or generated-file changes.
4. Choose verification commands from `docs/release-checklist.md`.
5. For runtime releases, run `./gradlew test`, `./gradlew buildPlugin`, and `./gradlew verifyPlugin` as applicable.
6. For compatibility releases, run `runIde` manual checks on core flows.
7. Confirm Marketplace token and platform credentials stay outside the repo.
8. Document rollback notes and any skipped verification with reasons.

## Guardrails

- Do not run `publishPlugin` unless the maintainer explicitly asks and credentials are ready.
- Do not treat documentation-only checks as runtime test coverage.
- Do not publish with unresolved plugin verification errors.
- Do not expose tokens, cookies, or private endpoint details in logs or notes.
