# Proposal

## Why

Doc View 已有界面生成/导出，但尚无生产 MCP 工具。需要让 Codex 等客户端通过 IDEA 已有 MCP Server 查询同一份项目文档，并在显式请求时生成/更新；避免另做插件或重复解析规则。

## What Changes

- 在同一个 Doc View 插件内，通过 `com.intellij.mcpServer` 的可选扩展注册 Java 工具；MCP 组件缺失/禁用时文档 UI 仍可工作，不自建 server。
- 复用 [project-document-store](../project-document-store/proposal.md) 的落盘索引与共享服务，定义 `doc_view_status`、`doc_view_search`、`doc_view_get` 三个只读工具，及单一 `doc_view_generate` 生成/更新工具。
- 公开工具名、JSON 输入/输出、项目作用域、schemaVersion、快照身份、新鲜度、分页、大小/时间边界及错误码；MCP 传输封装与磁盘 JSON 格式分别维护，不混称为同一种索引。
- 查询不写盘；生成/更新明确写盘并复用与按钮相同的冲突、取消、有限发布与进度结果。
- 文档说明三种不同绑定：Doc View 工具注册到 IDEA、项目绑定文档根、客户端连接 IDEA；不为每个项目创建另一台 MCP Server。
- 实施前必须通过实际 Codex 取消准入及精确 IU build 的生产升级回归；只读工具先验收，写工具最后开放，不把有界超时当取消通过。

## Capabilities

### New Capabilities

- `doc-view-mcp-tools`：生产 MCP 注册、工具契约、项目隔离、读写边界及生命周期。

### Modified Capabilities

无。PoC 准入规范、平台升级分别由 [mcp-java-feasibility](../archive/2026-11-09-mcp-java-feasibility/proposal.md) 与 [intellij-263-baseline-upgrade](../intellij-263-baseline-upgrade/proposal.md) 管理，本轮准入修订见下方，原功能验证结果不改变。

## Impact

实现将新增同一插件内的 MCP adapter/可选 descriptor 与 bundled MCP 编译依赖，并调用共享文档服务；保持 Java-only，不序列化含 PSI 的 `DocView`，不引入 Kotlin、跨插件 API、第二个安装包或自建传输层。需要真实 `tools/list`/`tools/call`、输入负例、跨项目、在途取消/卸载及最终包 Plugin Verifier/原生回归。

2026-11-12 实施准入更新：维护者明确要求在核对 1.6.0 上架后继续内置 MCP/Skill。1.6.0 已公开上架（update 1215457），当前 develop 已包含 1.6.2；Java 25 / IU 263.7031.23 基线和项目文档库已交付，原完整 UI/HTTP/资源矩阵的缺口仍在原 change 中保留，不重新作为本轮开发的总前置。Java PoC 与目标 Codex 取消证据已随 `2026-11-09-mcp-java-feasibility` 归档。

本轮允许开始同包可选 adapter，使用新的 1.7.0-SNAPSHOT 开发候选；必须独立完成生产工具的真实 tools/list、只读调用/项目隔离，再接入显式生成和真实取消/恢复。沿用已批准 require-restart 生命周期，验证项目关闭与 IDE 重启后的清理，不承诺动态热卸载。未验证的旧功能矩阵不勾选，新 MCP 验证也不借用 PoC 结论。正式 Skill 发行和新版本发布不随本轮自动执行。
