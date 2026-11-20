# 2026-11-20 Workspace metadata freshness verification

## Contract and implementation

The active `project-document-store` delta excludes only `.idea/workspace.xml` from current-session freshness inputs. The VFS event path and the shared PSI `Document` path use the same normalized path predicate. A filename suffix guard avoids constructing `Path` for the usual Java/XML changes; exact parent and filename checks then match only `.idea/workspace.xml`. Java, other XML under `.idea`, and ordinary XML named `workspace.xml` remain tracked; project content-root, settings, and template changes retain their stale behavior.

The regression `ProjectDocumentIndexServiceTest.testWorkspaceXmlMetadataIsIgnoredButOrdinaryXmlRemainsTracked` covers:

- `.idea/workspace.xml` VFS refresh and unsaved `Document` edit keep a committed snapshot `current`.
- `.idea/misc.xml` and a project-root `workspace.xml` VFS edit make it `stale`.
- An ordinary project-root `workspace.xml` `Document` edit makes it `stale`.

## RED / GREEN

The tests ran in an isolated copy of the previously completed 263.7307 assessment under `build/workspace-freshness-2026-11-20/scratch-red`. The copied assessment source was unchanged for RED; only the regression test was added. For GREEN, the scratch copy received the production `ProjectDocumentIndexService.java` change and the same test. No main-worktree Gradle build or package task was run.

| Run | Command scope | Result |
| --- | --- | --- |
| RED | `ProjectDocumentIndexServiceTest.testWorkspaceXmlMetadataIsIgnoredButOrdinaryXmlRemainsTracked` against the old service | 1 test failed as expected: `.idea/workspace.xml` produced `STALE`, expected `CURRENT`. |
| GREEN | `ProjectDocumentIndexServiceTest` against the patched service | 22 tests passed, 0 skipped, 0 failed. |

The green class includes existing checks for settings/template freshness, module-root changes, Java and XML VFS edits, XML `Document` edits, generated-output exclusion, and outside-root changes. This confirms those existing stale/current boundaries alongside the new regression.

The test JVM was `/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home`, JBR 25.0.4.1+1-b635.79. An initial attempt using the default JDK 21 stopped before tests because no Java 25 toolchain was found; that environment failure is preserved separately and is not counted as RED.

## Local evidence and limits

All relative `build/...` paths below are local-only evidence in this workspace; they are ignored build artifacts and are not part of the plugin package.

- RED log: `build/workspace-freshness-2026-11-20/evidence/red-final.log`
- RED JUnit XML: `build/workspace-freshness-2026-11-20/evidence/red-final-test-results.xml`
- Initial JDK 21 toolchain failure: `build/workspace-freshness-2026-11-20/evidence/initial-jdk21-toolchain-failure.log`
- GREEN log: `build/workspace-freshness-2026-11-20/evidence/green-index-service-final.log`
- GREEN JUnit XML: `build/workspace-freshness-2026-11-20/evidence/green-index-service-final-test-results.xml`
- Scratch used for both runs: `build/workspace-freshness-2026-11-20/scratch-red`

The completed assessment package and Verifier report were not overwritten: `build/baseline-minimum-2637307-assessment/build/distributions/Doc View-1.7.2-SNAPSHOT.zip` remains SHA-256 `408e598592e8657a59d5e3e5ed936b79721f6b85687cfbf9bfe0e8e66d4020c2`; `build/baseline-minimum-2637307-assessment/verifier-offline/IU-263.7307.200/report.html` remains SHA-256 `8bfb7c5a8ab2604fd398acd149302c2c7b5db810baac0ce8ee60dc88f753c841`.

This deterministic service test does not replace the final full test/build/Verifier run or native Codex/IDEA Skill freshness observation. Those remain under the parent change review. The OpenSpec task checklist was left unchanged pending that review.

## 最终整合包的原生复验

正式 IU 263.7307.200 的隔离 C 上，IDEA 原生开启项目视图“显示成员”并执行全部保存。磁盘 .idea/workspace.xml 从 eddf2bb…21364 变为 e5114fa…89df4，新增 ProjectViewState/showMembers=true。随后真实 Codex app-server 的 status-only 读回同一快照 current，9 个源文件、7 个文档文件及 Skill 哈希保持原样。独立 Java Javadoc 改动经原生 Reload All 导致同快照 stale。恢复文件原字节后仍为 stale，显式重新生成得到 unchanged 和 current。普通 XML 的 VFS/Document 仍能触发 stale，由上述 22 项服务测试覆盖。具体包身份、调用与反例见[真实会话记录](verification-2026-11-20-skill-natural.md)。
