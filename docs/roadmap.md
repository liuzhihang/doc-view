# 路线图

## 当前状态

截至 2026-11-13，1.7.1 已向 GitHub 正式发布，插件内置 MCP 工具与 Codex Skill 安装/更新设置已交付；Marketplace 上传、审核与公开上架状态以[1.7.1 发布记录](release-1.7.1.md)为准。项目 Markdown/JSON 文档库、搜索 UI 和 `.http` 请求草稿也已交付。

发布不等于全部原生验收完成。1.6.0 的发布例外及热卸载失败保留在[发布记录](release-1.6.0.md)；1.6.1 没有重跑全部旧矩阵。插件要求重启生效，不承诺热更新/热卸载。1.7.1 正式包完成553项测试与两个目标 build 的 Verifier；原生验收通过范围和未覆盖项单独见[发布记录](release-1.7.1.md)，不同候选包结果不合并计算。

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

由 [doc-view-mcp-tools](../openspec/changes/doc-view-mcp-tools/proposal.md) 跟踪。按本轮授权，task 1.3 已以明确保留旧原生缺口的方式准入开发；客户端候选的取消准入已经取得证据，但不代表所有客户端版本均可用，详见[取消验收说明](mcp-cancellation.md)。

1.7.1 已在同一 Doc View 插件内通过可选扩展注册到 IDEA MCP Server，与 UI 共用文档服务，提供只读 `status/search/get` 及须由用户明确请求的 `generate`。不另建服务或第二套解析链路。工具契约、项目边界和未完成的取消/生命周期验收以 [tasks](../openspec/changes/doc-view-mcp-tools/tasks.md) 为准；落盘格式见[文档库格式](mcp-format.md)。

最终1.7.1包553项测试通过（0失败/错误/跳过）；两个精确 IDEA build 的 Verifier 均 Compatible，各保留16项 Experimental 提示。本系列对应候选在263.7307.200原生验证真实 Codex 生成取消与查询恢复；最终1.7.1包另通过两范围注入 journal、重启后的恢复。取消必须在精确时机到达的边界、在途退出和其他未覆盖生命周期组合仍未完成；结果及对客户端、build 的限制见[发布记录](release-1.7.1.md)与[取消说明](mcp-cancellation.md)。

### 3. 客户端 Skill

由 [doc-view-client-skill](../openspec/changes/doc-view-client-skill/proposal.md) 跟踪。GitHub v1.7.1 已含正式 Skill ZIP 和 manifest，IDEA 设置提供当前项目/全局范围的安装、检查更新与更新；Skill 调用正式 MCP，不另起服务、不搬移解析，不自动注册 MCP、改写客户端配置或升级插件。准确安装、取消、恢复证据与客户端限制见[发布记录](release-1.7.1.md)，实现任务仍按 [tasks](../openspec/changes/doc-view-client-skill/tasks.md) 追踪。

设置页采用“当前项目 / 全局”两种固定目录并后台自动检测 Codex；两范围官方首次安装在 1.7.0 完成，最终 1.7.1 包通过两范围 1.7.0→1.7.1 官方更新、Codex 发现和 journal 注入重启恢复。设置页 Working 时关闭窗口取消，两范围均保留旧文件树及 metadata，重试更新成功；两范围修改保护、相互隔离通过，操作后没有 journal/stage/backup 残留。精确下载/暂存时点取消及全部生命周期矩阵未全部覆盖；写入目前仅在 macOS Apple Silicon 与 Codex 0.167.0-alpha.4.1 准入，其他 OS/client build fail closed。自然语言工作流的验证范围和失败记录也见[发布记录](release-1.7.1.md)。

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

历史候选的日期、build、测试数、hash、失败和未验证边界已迁至 [1.4.0 发布记录](release-1.4.0.md#路线图迁入的历史进度摘要)与 [1.6.0 发布记录](release-1.6.0.md#路线图迁入的历史进度摘要)，不再作为当前队列重复排列。最新仓库发布记录见 [1.7.1](release-1.7.1.md)，Marketplace 状态以该记录的官方回读为准；长期功能证据见各 change verification，已完成 change 见 [OpenSpec 归档](../openspec/changes/archive/)。归档只表示 lifecycle 状态，不是本轮重新运行验证。
