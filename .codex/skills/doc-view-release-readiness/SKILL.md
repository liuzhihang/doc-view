---
name: doc-view-release-readiness
description: Use when preparing, checking, or reviewing a Doc View plugin release, including changelog, version, plugin verification, manual IDE validation, Marketplace preparation, and rollback notes.
---

# Doc View Release Readiness

Use this skill before publishing or declaring a release candidate ready. `docs/release-checklist.md` is the authoritative validation matrix.

## Read First

- `AGENTS.md`
- `docs/release-checklist.md`
- `CHANGELOG.md`
- `gradle.properties`, `build.gradle`, Gradle wrapper, and `plugin.xml`
- OpenSpec status/context for every change included in the release
- `docs/intellij-compatibility.md` for IntelliJ API/platform changes or full release compatibility review

## Steps

1. Identify release scope and included changes; confirm tasks are complete and required specs are synced/archived or explicitly pending.
2. Check version, changelog, branch/tag target, Marketplace/GitHub release expectations, and rollback version.
3. Inspect staged/unstaged/untracked diff for unintended runtime, descriptor, dependency, resource, generated, or secret-bearing files.
4. Select and run exact automatic/manual validation from the release checklist; distinguish real tests from 0 tests or cache-only results.
5. Inspect Plugin Verifier compatibility/internal/deprecated findings and the actual IDE matrix.
6. Confirm tokens and private endpoints stay outside the repo and logs.
7. Record artifact SHA, skipped validation, residual risks, post-release checks, and rollback notes.

## Guardrails

- Do not run `publishPlugin`, push tags, create a release, merge `master`, or rotate external credentials unless the maintainer explicitly authorizes it.
- Do not treat documentation checks, compilation, or zero discovered tests as runtime coverage.
- Do not publish with unresolved compatibility problems or exposed credentials.
- Do not expose tokens, cookies, private endpoints, or sensitive payloads in reports.
