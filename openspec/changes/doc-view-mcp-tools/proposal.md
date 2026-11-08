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

无。PoC 准入规范、平台升级分别由 [mcp-java-feasibility](../mcp-java-feasibility/proposal.md) 与 [intellij-263-baseline-upgrade](../intellij-263-baseline-upgrade/proposal.md) 管理，不在本 change 放宽门槛。

## Impact

实现将新增同一插件内的 MCP adapter/可选 descriptor 与 bundled MCP 编译依赖，并调用共享文档服务；保持 Java-only，不序列化含 PSI 的 `DocView`，不引入 Kotlin、跨插件 API、第二个安装包或自建传输层。需要真实 `tools/list`/`tools/call`、输入负例、跨项目、在途取消/卸载及最终包 Plugin Verifier/原生回归。

当前正式 MCP adapter 尚未实现。用户已批准开发基线升级至 IU `263.7031.23` 与 Java 25；`263-full-10` 的 67 suites / 352 项自动测试、构建和 Plugin Verifier 已通过。生产 Verifier 问题为 0，另有 16 处 Experimental API；测试源码编译仍有 15 条 test-only deprecated/removal warnings。full-9 的 350 项、full-8 的 349 项与 full-7 的 344 项结果保留为历史。full-10 新包的 Markdown tamper/恢复、Spring/Feign/Dubbo 预览、Welcome 重开及 GET/form 草稿生成和 HTTP Client 打开已通过；HTTP Client Run 未执行。Copy/Export、菜单、完整参数编辑、生成取消/partial/retry 和资源基线等原生验收仍不完整。另一次 freshness 复测中重开项目先看到 `unknown`；随后 FindAction 的 Generate 返回 `30 API(s) / No content changes`，不点 Refresh 即自动变为 `CURRENT`，约 +4.7 秒和 +20.45 秒读回仍为 current，未复现持续刷新问题。项目文档库最终验收仍待完成，因此准入 task 1.3 保持未勾选，不能开始生产 adapter。已发布插件支持范围仍以原发布版 261 为准。Codex `0.163.0-alpha.11.2` 的取消失败保留为历史；`0.165.0-alpha.1` 候选三轮取消通过只关闭客户端准入，不代表生产工具上线。生产写工具、外部配置写入、发布均不能借本 proposal 自动执行。
