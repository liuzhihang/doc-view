# 首次生成 freshness 续验（2026-11-17）

- 工作区：`/Users/liuzhihang/workspace/doc-view-release-integration-20261107`
- 验证基线：`codex/mcp-ui-lifecycle-20261114`，HEAD `6514a9eff12b395119be53edf80c37b5a7bf12a4`
平台配置：IntelliJ `263.7031.23-EAP-SNAPSHOT`；测试 JBR `25.0.4.1`。

## 根因与复现

- 干净项目没有 `docs/`，实际生成成功后 freshness 为 `current`，提交 epoch 为 9。首次刷新输出时 LocalFileSystem 只报告项目内容根下的 `VFileCreateEvent <project>/docs`。旧监听器把该目录当作输入目录变更，将 epoch 从 9 提到 10，状态变成 `stale`。证据：`build/remaining-acceptance-2026-11-17/freshness-fixture-red5.xml`。
- 单纯忽略这条目录事件仍不安全：生成完成、首次 VFS 刷新前，在 `docs/input-source/nested/` 预置 Java 和 XML 文件后，VFS 同样只报告 `<project>/docs` 创建事件；旧行为保持 epoch 9 和 `current`，漏掉了真实输入变化。证据：`build/remaining-acceptance-2026-11-17/freshness-counterexample-red1.xml`。

## 修复

只延后判断固定输出根 `docs/doc-view/` 的直接容器 `<project>/docs` 创建事件。VFS `after` 阶段通过公开的 `VFileCreateEvent.getFile()` 和 `VirtualFile.getChildren()` 检查直接子项：唯一子项是 `doc-view` 目录时不推进 epoch；出现任何其它子项或无法确认时保守标记 `stale`。没有递归遍历，也没有调用 `Internal` API。其他创建、修改、移动、改名和删除事件继续按原规则处理。

真实生成用例确认纯输出首刷后仍为 `current`，在 docs 容器首刷前放入 nested Java/XML 则为 `stale`，容器已存在时新增 Java/XML 也仍为 `stale`。单测反例 GREEN 证据：`build/remaining-acceptance-2026-11-17/freshness-counterexample-green1.xml`。

## 验证结果

- 定向完整类：`./gradlew test --tests '*ProjectDocumentIndexServiceTest' --no-build-cache --rerun-tasks --console=plain --no-parallel`，`freshness-index-service-final`：22 tests，0 failures，0 errors，0 skipped。XML：`build/remaining-acceptance-2026-11-17/freshness-index-service-final.xml`。
- OpenSpec：`openspec validate project-document-store --strict` 通过。
- 此阶段只覆盖 freshness 定向测试和 OpenSpec；完整构建、Plugin Verifier及首次生成原生回归的后续结果见本文末节。

## 测试夹具清理复验（2026-11-18）

完整套件在 `ProjectDocumentGenerationLifecycleTest` 暴露一个额外的 `skipped=1` 差异。失败诊断同时发现，刚删除的测试源 `freshness.FirstGenerationApi` 仍可由 Spring API 候选扫描返回：VFS 对象有效且仍在项目内容根中，但其磁盘路径已不存在（`NoSuchFileException`）。因此这是共享测试项目上的 VFS/索引清理残留，不是生产快照生成失败。

两个首次生成用例的 `finally` 清理现在会删除临时源文件和 `docs/` 输出，再递归刷新测试项目根并等待索引就绪，最后分别断言 Java PSI 与 `SpringPsiUtils` 候选中均没有 `freshness.FirstGenerationApi`。这保留了用例间清理回归断言，并确保后续测试读取已同步的 VFS/索引状态。

- 原始全套失败与候选诊断：`build/remaining-acceptance-2026-11-17/full-diagnostic-results/TEST-com.liuzhihang.doc.view.service.export.ProjectDocumentGenerationLifecycleTest.xml`。
- 清理复验：`./gradlew test --tests '*ProjectDocumentIndexServiceTest' --no-build-cache --rerun-tasks --console=plain --no-parallel`，22 tests、0 failures、0 errors、0 skipped。XML：`build/remaining-acceptance-2026-11-17/freshness-index-service-cleanup-green3.xml`；执行记录：`build/remaining-acceptance-2026-11-17/freshness-index-service-cleanup-green3.json`。

## 最终候选构建、Verifier 与首次生成原生回读（2026-11-18）

- 完整候选 `1.7.2-SNAPSHOT` 的全量测试最终通过：564 tests、100 suites、0 failures、0 errors、0 skipped。最终 ZIP SHA-256 为 `f0d5ad184d97b393a9b737488f09e75d4a66cdd36feb4e061e187882cf881dcb`；汇总记录：`build/remaining-acceptance-2026-11-17/final-build-summary.json`。当前仍未发布。
- Plugin Verifier 对最低目标 `263.7031.23` 和原生目标 `263.7307.200` 均报告 Compatible，每个目标有 16 条 Experimental API 使用。最低目标第一次 Gradle `verifyPlugin` 被共享 verifier 提取目录的 `DirectoryNotEmptyException` 阻断；同一目标重试成功。证据：`build/remaining-acceptance-2026-11-17/minimum-verifier.json`（首次环境失败）、`minimum-verifier-retry.json` 与 `minimum-verifier-report/IU-263.7031.23/`（成功）、`release-verifier.json` 与 `release-verifier/IU-263.7307.200/`（成功）。首次缓存清理异常不是插件兼容性 verdict。
- 原生首次生成在隔离 IDEA `263.7307.200`、进程 PID `11112`、项目 `native/project-a` 上使用最终 ZIP；`final1-launch.json` 记录的 ZIP SHA 与最终构建汇总一致。点击 Generate Project Docs 后，索引回读为 schema v1、62 entries、snapshot `71bdf909486f35d557e23acc2d12aab2a1ae35c6e4bd9b357361fab348981b2d`；落盘核对为 62 份 Markdown、62 份详情 JSON 与两个索引文件，共 126 个文档输出文件（另有锁文件）。随后的状态读取为 `CURRENT`。证据：`final1-launch.json`、`ui-actions.jsonl`、`first-generation-readback.json` 及 `native/project-a/docs/doc-view/`。
- 完整套件早期的 `skipped=1` 与 `FirstGenerationApi` 残留由测试夹具删除磁盘文件后未同步刷新 VFS/索引引起；原始候选诊断仍保留在 `full-diagnostic-results/TEST-com.liuzhihang.doc.view.service.export.ProjectDocumentGenerationLifecycleTest.xml`。本轮测试夹具清理已先删除临时源和输出，再刷新项目根并等待索引；最终 564 项全量测试通过，未发现生产生成逻辑失败。
- 本轮原生证据仅覆盖最终 ZIP 的首次全量生成与状态回读，不代表已完成旧项目发现、搜索/分页、详情与 Markdown 打开、外部改写 fail-closed、取消/partial/retry、项目关闭/重开 freshness 等完整原生矩阵；OpenSpec task 5.4 保持未完成。
