# Tasks

## 1. 分析与发布信息

- [x] 1.1 核对远端分支/tag/Release、原工作区改动、本轮 backlog 归档及其他未完场景；在发布记录中写明范围和真实限制，并以 `openspec validate release-doc-view-1-5-0 --strict` 验证本 change。
- [x] 1.2 更新 1.5.0 版本属性、AGENTS 版本摘要及 2026-11-03 CHANGELOG 条目；读回文件确认 Unreleased 保留且没有改变平台、依赖或 Java 实现。
- [x] 1.3 同步中英欢迎/更新摘要、README 与 Marketplace 描述，并创建发布记录；检查能力、来源条件、现有动作 URI、版本占位符与文件链接，运行 `git diff --check`。

## 2. 最终包验证

- [x] 2.1 对正式 1.5.0 版本运行 `./gradlew --offline test verifyPluginProjectConfiguration buildPlugin verifyPlugin --rerun-tasks --no-build-cache`，核对真实测试计数与 IU 261 Verifier verdict。
- [x] 2.2 从最终 ZIP 读回 descriptor、change-notes 和四份 HTML 资源，核对 1.5.0、兼容基线、当前更新内容及无测试依赖；记录 ZIP/JAR SHA-256 和制品清单。
- [x] 2.3 将同一最终候选安装到隔离原生 IDEA，确认版本/更新摘要和受影响的实体复制流程，核对实际日志、粘贴结果与源码未变；记录尚未覆盖场景及其发布影响。

## 3. 交付与回读

- [ ] 3.1 重新核对远端 SHA 和工作区范围，仅推送本轮修复及发布信息到 develop，并通过 develop → master PR 完成合入；核对合并树与验证过的发布内容一致。
- [ ] 3.2 在对应发布提交创建 v1.5.0 tag 与 GitHub Release，上传最终 ZIP 和校验文件；下载或读取远端制品元数据确认版本、目标提交与 SHA。
- [ ] 3.3 使用现有安全凭据提交 Marketplace，回读更新 ID、版本、渠道、审核/公开状态和包内容；对结果不确定的操作先查询，不重复发布。
- [ ] 3.4 核对对应修复已经交付后，为 #89、#90、#91、#96、#97、#101 写明版本与使用条件并关闭已解决项；逐条回读状态，未解决或未交付项保持打开。
- [ ] 3.5 补齐 `docs/release-1.5.0.md` 的最终提交、制品、验证、渠道和 Issue 状态，核对原工作区保留且发布记录没有凭据；运行 `git diff --check` 和适用 OpenSpec 验证。

## Workflow follow-up

- 所有本 change 的实施与验证完成后同步 `release-metadata-consistency` 规格并归档，再检查归档与长期规格。
- 保留其他未完成 OpenSpec change 的任务和验证缺口，不将本次发布视为其完成或归档。
