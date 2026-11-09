# 路线图

## 当前状态

截至 2026-11-09，最新发布记录为 [1.6.1](release-1.6.1.md)：GitHub Release 已公开，Marketplace 已接收、待审核/上架（以该记录的回读时间为准）。项目 Markdown/JSON 文档库、搜索 UI 和 `.http` 请求草稿已交付；**正式 Doc View MCP 工具和客户端 Skill 尚未交付**。

发布不等于全部原生验收完成。1.6.0 的发布例外及热卸载失败保留在[发布记录](release-1.6.0.md)；1.6.1 没有重跑全部旧矩阵。插件要求重启生效，不承诺热更新/热卸载。不同候选包结果不能合并成最终包的全量验收。

## 推进顺序与阻塞项

### 1. 补齐现有能力的验证欠账

先处理可复现的功能问题，再补同包原生证据。以下八项可按影响并行推进，不要求串行完成；详细任务只在对应 change 维护，此处不复制完整 checklist。

| 范围 / 活动 change | 近期重点 |
| --- | --- |
| [263 基线](../openspec/changes/intellij-263-baseline-upgrade/tasks.md) | 补精确目标 build 的原生资源和生命周期边界；历史热卸载失败不改写为已修复，不推导未来 build 兼容性。 |
| [项目文档库](../openspec/changes/project-document-store/tasks.md) | 完成最终包原生矩阵：项目隔离、生成/查询、冲突、取消/partial/retry、关闭重开 freshness。 |
| [HTTP 与 UI](../openspec/changes/http-client-generation-and-ui/tasks.md) | 补最终包 HTTP Send、异常/取消和完整菜单、主题、窄窗、键盘及跨应用交互矩阵。 |
| [预览生命周期](../openspec/changes/fix-preview-lifecycle/tasks.md) | 核对 JCEF 状态替换、关闭释放与重复打开后的资源边界。 |
| [生成性能](../openspec/changes/doc-generation-performance/tasks.md) | 补自然耗时重复测量、可见取消终态与 HTML 矩阵；受控断点不作性能结论。 |
| [凭据存储](../openspec/changes/integration-credential-storage/tasks.md) | 补 OS 授权拒绝、后端写入失败及重试原生证据，保留项目隔离边界。 |
| [Schema 必填与写回](../openspec/changes/openapi3-schema-required-mode/tasks.md) | 补最终包写回组合及剩余原生边界，不以历史候选 Undo/Redo 结果代替。 |
| [Clear Cache](../openspec/changes/clear-cache-safety/tasks.md) | 对照当前 `…` 入口补缓存隔离、文件保护和选中上下文证据。 |

尚未完成表示证据有缺口，不自动等于已确认的运行时缺陷。真实平台上传的历史排除是未执行，不是通过；下一次发布重新按[发布检查清单](release-checklist.md)确定范围。

### 2. 正式 MCP 工具

由 [doc-view-mcp-tools](../openspec/changes/doc-view-mcp-tools/proposal.md) 跟踪。**当前阻塞是 task 1.3 的基线原生回归和文档库最终验收**；客户端候选的取消准入已经取得证据，但不代表所有客户端版本均可用，详见[取消验收说明](mcp-cancellation.md)。

准入完成后，在同一 Doc View 插件内可选注册到 IDEA MCP Server，与 UI 共用文档服务：先实现只读 `status/search/get`，再实现显式写盘的 `generate`。不另建服务或第二套解析链路。工具契约、项目边界和取消发布要求以 [tasks](../openspec/changes/doc-view-mcp-tools/tasks.md) 为准；落盘格式见[文档库格式](mcp-format.md)。

### 3. 客户端 Skill

由 [doc-view-client-skill](../openspec/changes/doc-view-client-skill/proposal.md) 跟踪，依赖正式工具契约稳定。计划从 GitHub 提供显式安装、检查更新和更新入口，先验收 Codex；Skill 调用正式 MCP，不另起服务、不搬移解析，不自动注册 MCP、改写客户端配置或升级插件。分发与安装尚未实现，任务见 [tasks](../openspec/changes/doc-view-client-skill/tasks.md)。

## 文档与持续门禁

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

历史候选的日期、build、测试数、hash、失败和未验证边界已迁至 [1.4.0 发布记录](release-1.4.0.md#路线图迁入的历史进度摘要)与 [1.6.0 发布记录](release-1.6.0.md#路线图迁入的历史进度摘要)，不再作为当前队列重复排列。最新发布细节见 [1.6.1](release-1.6.1.md)，长期功能证据见各 change verification，已完成 change 见 [OpenSpec 归档](../openspec/changes/archive/)。归档只表示 lifecycle 状态，不是本轮重新运行验证。
