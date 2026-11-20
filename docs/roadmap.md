# 路线图

## 当前状态

截至 2026-11-14，1.7.1 已在 GitHub / Marketplace 正式发布，插件内置 MCP 工具与 Codex Skill 安装/更新设置已交付；Marketplace 上传、审核与公开上架状态以[1.7.1 发布记录](release-1.7.1.md)为准。项目 Markdown/JSON 文档库、搜索 UI 和 `.http` 请求草稿也已交付。

发布不等于全部原生验收完成。1.6.0 的发布例外及热卸载失败保留在[发布记录](release-1.6.0.md)；1.6.1 没有重跑全部旧矩阵。插件要求重启生效，不承诺热更新/热卸载。1.7.1 正式包完成553项测试与两个目标 build 的 Verifier；原生验收通过范围和未覆盖项单独见[发布记录](release-1.7.1.md)，不同候选包结果不合并计算。

## 推进顺序与阻塞项

### 1. 1.7.2 的验收归档

截至 2026-11-20，活动 OpenSpec 为零。工件、测试、原生验收与公开渠道状态见[1.7.2 发布记录](release-1.7.2.md)。

| 变更 | 归档证据 |
| --- | --- |
| [263 基线（已归档）](../openspec/changes/archive/2026-11-20-intellij-263-baseline-upgrade/tasks.md) | 1.7.2 最低为正式 IU `263.7307.200`。构建、Plugin Verifier、重启生命周期、原生写回、导出与资源回归已验。具体工件及覆盖范围见[稳定版记录](../openspec/changes/archive/2026-11-20-intellij-263-baseline-upgrade/verification-2026-11-20-stable-minimum.md)。 |
| [HTTP 与 UI（42/42 已归档）](../openspec/changes/archive/2026-11-20-http-client-generation-and-ui/tasks.md) | 原生本机回环、拒绝/失败保护、取消/在途关闭、固定模型测量、双项目、菜单、预览和写回。完整证据与发布包差异见[原生验收](../openspec/changes/archive/2026-11-20-http-client-generation-and-ui/verification-2026-11-20-final-r2.md)。 |
| [客户端 Skill（25/25 已归档）](../openspec/changes/archive/2026-11-20-doc-view-client-skill/tasks.md) | 真实 Codex 文档工作流、项目关闭时暂存取消、事务清理与官方安装通过。见[官方取消/关闭项目](../openspec/changes/archive/2026-11-20-doc-view-client-skill/verification-2026-11-20-official-cancel.md)及[自然工作流](../openspec/changes/archive/2026-11-20-doc-view-client-skill/verification-2026-11-20-skill-natural.md)。 |

[Clear Cache 已归档](../openspec/changes/archive/2026-11-19-clear-cache-safety/verification.md)：现行 `…` 入口已验证双项目缓存隔离及文件保护；旧目录菜单已被替代。[Schema 必填与写回已归档](../openspec/changes/archive/2026-11-19-openapi3-schema-required-mode/verification.md)：参数编辑器键盘、写回与关闭后资源已验，完整预览及插件卸载仍独立跟踪。

[预览生命周期已归档](../openspec/changes/archive/2026-11-20-fix-preview-lifecycle/verification.md#2026-11-20-预览组件分离与最终原生回归)。正式 IDEA 中复验源码按需创建 JCEF、关闭后资源基线、取消以及 Spring/Dubbo 预览与导出。

[项目文档库已归档](../openspec/changes/archive/2026-11-19-project-document-store/verification-2026-11-19-final-lifecycle.md)：同一最终包完成从首次生成到取消、重试和重开的原生验收。

[凭据存储已归档](../openspec/changes/archive/2026-11-20-integration-credential-storage/verification.md#2026-11-20-验收范围复核)。保留项目隔离、重启回读和同步失败重试的证据。系统提示与异步持久化按 IDEA/OS 诊断。

[生成性能已归档](../openspec/changes/archive/2026-11-20-doc-generation-performance/verification.md#2026-11-20-最终候选的预览刷新与取消)。最终候选的整类预览与刷新各完成三次原生测量；受控取消后未出现部分预览，目录保留原有 62 项。

尚未完成表示证据有缺口，不自动等于已确认的运行时缺陷。真实平台上传的历史排除是未执行，不是通过；下一次发布重新按[发布检查清单](release-checklist.md)确定范围。

### 2. 正式 MCP 工具

内置 MCP 的 OpenSpec 已完成验收。工具作为 Doc View 同一插件内的可选扩展注册到 IDEA MCP Server，与界面共用文档服务。正式工具提供只读 `status/search/get`，以及须由用户明确请求的 `generate`。工具边界见[长期规范](../openspec/specs/doc-view-mcp-tools/spec.md)，落盘结构见[文档库格式](mcp-format.md)。

同一开发候选已通过双项目读写隔离、冲突保护、索引提交前后两种真实 Codex 取消、同会话恢复、项目关闭及 IDEA 重启／插件停用。证据见[验收记录](../openspec/changes/archive/2026-11-20-doc-view-mcp-tools/verification-2026-11-20-native-acceptance.md)。

### 3. 客户端 Skill

由 [doc-view-client-skill](../openspec/changes/archive/2026-11-20-doc-view-client-skill/proposal.md) 跟踪并已完成归档。GitHub v1.7.1 已含正式 Skill ZIP 和 manifest，IDEA 设置提供当前项目/全局范围的安装、检查更新与更新；Skill 调用正式 MCP，不另起服务、不搬移解析，不自动注册 MCP、改写客户端配置或升级插件。准确安装、取消、恢复证据与客户端限制见[1.7.1 发布记录](release-1.7.1.md)；1.7.2 候选状态见[候选发布记录](release-1.7.2.md)，完整实现任务与验收记录保留在归档 [tasks](../openspec/changes/archive/2026-11-20-doc-view-client-skill/tasks.md)。

设置页采用“当前项目 / 全局”两种固定目录并后台自动检测 Codex。1.7.1 的写入准入限于 macOS Apple Silicon 与 Codex 0.167.0-alpha.4.1。1.7.2 在 macOS Apple Silicon 与 Codex 0.169.0-alpha.11.1 上完成两范围 Skill 与真实工作流验收。完整工件和公开渠道见[1.7.2 发布记录](release-1.7.2.md)。

## 文档与持续门禁

2026-11-17—18：开发候选已针对实际 Codex `0.169.0-alpha.11.1` 完成两范围发现、本地源安装/更新与历史安装记录保护；修复首次生成freshness误报，最终564项测试、构建和两目标Verifier通过。同包原生首次生成、两范围暂存取消、Codex在途取消和项目关闭/重开通过；官方安装重试受GitHub限流阻塞，下载/更新取消、journal重启及显示组合仍待验。稳定版1.7.1的客户端限制不变，证据与未完成项见[11月17日续验](../openspec/changes/archive/2026-11-20-doc-view-client-skill/verification-2026-11-17.md)。

- 完整手册在 [GitHub 用户指南](user-guide/index.md) 维护，随分支和 tag 回溯；IDE 内置页面只提供欢迎信息与当前更新摘要。帮助链接合并后核对公开可达，本地存在不等于线上已发布。
- 新行为先明确 [contract](contract-design.md)，按范围补 PSI/Markdown 回归；性能与平台边界遵循[性能指南](performance-guide.md)和[兼容性策略](intellij-compatibility.md)。不新增旧格式双轨或兼容分支。
- 每项 task 只有实现与验证完成后才勾选；未执行、部分通过和已归档均不能替代当前验收。发布、推送或 tag 需另有明确授权，旧发布授权不延续到下一版。
- 原生验证使用专用 `doc-demo`，保留既有改动并恢复临时设置；订阅普通额度不可用时停止，不使用 Credits 或激活重置权益。

## 暂缓范围

- 全仓库 AI Wiki、LLM 自动总结/维护、向量存储及独立 Wiki 插件：先验证轻量 API Wiki 的实际需求，不增加第二套解析、存储或生成体系。
- 完整 OpenAPI 导入/导出：参数位置、响应码、多媒体类型、components、security、`$ref` 和组合 schema 不纳入近期队列。
- 双向同步、多人协作和新生产语言：没有明确差异化需求前暂缓。
- Air 深度集成及历史文档导入：保持[只读评估](ai-context-assessment.md)，不把 Air、ACP 或 MCP 视为已集成能力；只有历史目录/差异存在实测缺口时才另立只读快照 change，不重复建设 Markdown 查看器。

## 历史证据入口

历史候选的日期、build、测试数、hash、失败和未验证边界见 [1.4.0 发布记录](release-1.4.0.md#路线图迁入的历史进度摘要)和 [1.6.0 发布记录](release-1.6.0.md#路线图迁入的历史进度摘要)。当前版本见 [1.7.2](release-1.7.2.md)，Marketplace 状态以该记录的官方回读为准。长期功能证据见各 change verification 和 [OpenSpec 归档](../openspec/changes/archive/)。
