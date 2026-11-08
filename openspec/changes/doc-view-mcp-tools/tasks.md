# Tasks

## 1. 客户端、平台与文档库准入

- [x] 1.1 在隔离目录核对实际候选 Codex app-server 的官方版本、下载资产和 SHA-256；使用官方 `rust-v0.165.0-alpha.1` Apple Silicon app-server 与同版 code-mode-host，校验发行归档和运行二进制身份，运行版本确认为 `0.165.0-alpha.1`。只用临时进程配置，没有读取普通 Codex 配置内容；其观测 hash 与先前基线不同，来源未知，因此不声称普通配置字节未变，也未尝试恢复。证据见[取消验收说明](../../../docs/mcp-cancellation.md#候选客户端运行证据)。
- [x] 1.2 使用准入候选连接隔离 IU 263.7031.23，保存 `initialize` 协商的 `2025-06-18`、session hash 与 request ID；三轮真实 `turn/interrupt` 均发送匹配的 `notifications/cancelled`，IDEA 每轮记录 `started → cancelled → cleanup`、31.005 秒观察期无迟到完成，且同 session 后续 echo 成功。保留 Codex 0.163.0-alpha.11.2 两轮 wire 失败记录；详细证据见[取消验收说明](../../../docs/mcp-cancellation.md#候选客户端运行证据)。
- [ ] 1.3 在任何 MCP 生产运行时代码前，确认 `mcp-java-feasibility` 2.4 已由真实目标客户端取消证据关闭，`intellij-263-baseline-upgrade` 的精确 IU 263.7031.23 配置、自动回归、Plugin Verifier 和原生回归已完成，且 `project-document-store` 的格式、查询、发布与共享生成服务已按 contract 实现并验证；运行 `openspec status --change mcp-java-feasibility --json`、`openspec status --change intellij-263-baseline-upgrade --json`、`openspec status --change project-document-store --json` 和 `openspec validate mcp-java-feasibility --strict`、`openspec validate intellij-263-baseline-upgrade --strict`、`openspec validate project-document-store --strict`，同时核对对应 tasks 和 verification 中无未完成准入项。

## 2. 工具契约与可选扩展

- [ ] 2.1 按 design 建立 `tool-contracts.json` 和 `examples/`，完整定义四个工具、显式唯一的 `projectPath`、输入约束、Text(JSON) envelope、错误码和成功/失败样例；输入 schema 均拒绝额外字段，MCP schemaVersion 与磁盘 schemaVersion 分开。用 `python3 -m json.tool openspec/changes/doc-view-mcp-tools/tool-contracts.json`、逐个校验 JSON 样例及 `./gradlew test --tests '*DocViewMcpToolContractTest'` 验证契约和 golden 一致；同时覆盖 hasUnsavedDocuments/工具版本，以及路径数组100条上限、*Total计数和文档根路径转换。
- [ ] 2.2 在同一个插件包中加入指向 MCP descriptor 的可选依赖和 Java `McpToolsProvider`，仅调用公开 API、使用单一 Text(JSON) 返回，不新增 server、第二插件、Kotlin 或生产反射；确认 MCP 类不被普通 UI/service 引用。运行 `./gradlew verifyPluginProjectConfiguration` 与 `./gradlew test --tests '*DocViewMcpExtensionRegistrationTest'`，并检查插件 descriptor 的可选加载边界。
- [ ] 2.3 实现安全 JSON 输入解析、Gson 输出、`isError` 业务错误映射与版本化 envelope；未知输入/输出 schema、超限结果、私密异常信息和无效参数均按 spec 失败关闭。用 `./gradlew test --tests '*DocViewMcpToolContractTest' --tests '*DocViewMcpResponseTest'` 对照 `tool-contracts.json` 与 examples 验证。

## 3. 只读项目查询工具

- [ ] 3.1 实现显式 `projectPath` 到唯一已打开 Project 的解析，并只绑定该项目的规范 base path 下 `docs/doc-view/`；拒绝未打开、歧义、关闭项目、无效路径及任意目录输入。用真实 A/B Project fixture（包含相同 API identity）运行 `./gradlew test --tests '*DocViewMcpProjectScopeTest'`，确认缓存、路径和快照不串项目。
- [ ] 3.2 实现 `doc_view_status` 与 `doc_view_search`，只调用文档库只读服务，返回固定 schema、entry 摘要、分页与 freshness；未生成时不触发生成，Dumb Mode 下仍能读取现有 JSON。运行 `./gradlew test --tests '*DocViewMcpReadOnlyToolsTest' --tests '*ProjectDocumentSearchTest'`，覆盖未生成、损坏/未知 schema、unknown/stale、过滤、cursor 失效与 Dumb Mode。
- [ ] 3.3 实现 `doc_view_get`，按 `apiId` 和搜索返回的 `snapshotId` 读取同代结构化详情及可选 Markdown；校验相对路径、身份、schema 和原始字节 hash，损坏或部分发布时拒绝混代且不反解析 Markdown。运行 `./gradlew test --tests '*DocViewMcpDetailToolTest' --tests '*ProjectDocumentDetailReadTest'`，覆盖缺页、手改、越界、旧 snapshot 和大小上限。
- [ ] 3.4 为三项查询接入每项目并发上限、5 秒服务端期限和结果大小上限；测试 status/search/get 前后项目文档逐字节不变、查询不触发 PSI/写盘、第五个并发读取返回 `BUSY`。运行 `./gradlew test --tests '*DocViewMcpReadOnlyToolsTest' --tests '*DocViewMcpConcurrencyTest'`，并以记录环境的 10,000-entry fixture 记录 warm search 与冷加载实测，不将测试 fixture 数值写成原生 IDEA 性能结论。
- [ ] 3.5 在隔离 IU 263.7031.23 上验证 `tools/list` 中三项只读工具的名字/schema 无重复 `projectPath`，并用真实 `tools/call` 验证 A/B 项目、Dumb Mode、readonly hash 和错误路径；确认调用前后无新增、替换或删除文件。把实际 protocol、包身份和结果记录到 `verification.md`。

## 4. 显式生成与取消发布

- [ ] 4.1 将唯一写工具 `doc_view_generate` 接到与 UI 相同的项目文档生成/更新服务，只扫描所选项目支持模块并写固定 `docs/doc-view/`；保留 Markdown/JSON 同源 snapshot、重载身份、未变化不重写及不上传边界。运行 `./gradlew test --tests '*DocViewMcpGenerateToolTest' --tests '*ProjectDocumentExporterTest'`，验证首次生成、重复无变化及明确调用才写盘。
- [ ] 4.2 将生成请求限制为每项目单写者，并复用已有文件所有权、手改冲突、路径边界、旧页保留和跨实例 writer lock；Dumb Mode 返回 `INDEXING`，并发写返回 `BUSY`，不能强制覆盖。运行 `./gradlew test --tests '*DocViewMcpGenerateToolTest' --tests '*ProjectDocumentWriterLockTest'`，逐项验证冲突文件原字节保留及解除冲突后重试成功。
- [ ] 4.3 把 IDEA continuation/job 的真实取消与项目/插件生命周期信号传至生成步骤，检查 PSI 遍历和每个文件发布边界，并保证任务、监听器、暂存文件与缓存只清理一次；超时、项目关闭、插件卸载和客户端取消使用不同终态。运行 `./gradlew test --tests '*DocViewMcpLifecycleTest' --tests '*ProjectDocumentCancellationPublicationTest'`，覆盖取消后同 session 可继续调用。
- [ ] 4.4 实现并验证发布部分取消和 `index.json` 最终提交边界：提交前停止后续 move、保留旧索引、返回 `PARTIAL_PUBLICATION` 与已发布路径；最终原子提交后到达的迟到取消不回滚、不伪报取消，后续 status 返回实际快照及 freshness。运行 `./gradlew test --tests '*DocViewMcpGenerateCancellationTest' --tests '*ProjectDocumentCancellationPublicationTest'`，覆盖首个 move 前、页面后、index.md 后和最终 commit 前后的取消及清理。
- [ ] 4.5 用真实 Codex 候选和最终服务端工具验证生成调用取消，不以 PoC、SDK 对照、HTTP 202 或客户端 turn 状态代替；根据协商的 `protocolVersion` 捕获通知或响应流关闭，逐次关联 request/session/run、partial/commit 结果、cleanup 和同 session echo。将 `docs/user-guide/mcp.md` 更新为已实现行为并同步 `CHANGELOG.md`，链接 schema/example 而不复制字段表；运行 `./gradlew test --tests '*DocViewMcpGenerateToolTest' --tests '*DocViewMcpLifecycleTest'`、JSON 校验和相对链接检查。

## 5. 最终包与原生集成验收

- [ ] 5.1 运行 `./gradlew test buildPlugin verifyPluginProjectConfiguration verifyPlugin --rerun-tasks --console=plain`；记录真实测试数、精确 IU 263.7031.23 与 Plugin Verifier 结果，读回最终 ZIP/JAR 中的可选 MCP descriptor、工具类与依赖，确认无 Internal API 问题、无 PoC/测试类、Java-only 且包内契约版本一致。
- [ ] 5.2 将该最终包安装到独立 IU 263.7031.23 profile，分别验证 MCP 组件启用和禁用/缺失：启用时 `tools/list` 只有四个预期工具，`tools/call` 覆盖 status/search/get；禁用时 Doc View 普通 UI 仍加载且 MCP 实现未被加载。记录实际 build、包 SHA、profile 与日志，不修改普通客户端持久配置。
- [ ] 5.3 在最终包上完成真实 A/B 项目读写与发布验收：只读调用不改文件；显式 generate 首次生成及重复更新正确；A 的请求不能读写 B；手改冲突保持原文；索引提交前取消展示 partial、提交后取消保留 current snapshot；项目关闭/插件卸载后任务终止且同 session 可继续调用。保存可复核的客户端/IDEA 双侧证据和未覆盖项。
- [ ] 5.4 核对 [HTTP/UI change](../http-client-generation-and-ui/tasks.md) 与其 verification 的未完成项，至少保留 4.5、4.6、5.4、5.5、6.3、6.4 的原状态；MCP 或项目文档库验收不得替 HTTP 双项目/草稿保护、原生 UI 矩阵、生命周期和性能场景勾选。
- [ ] 5.5 复核本 change 的 contracts、examples、用户指南、错误码、descriptor、包内容和实际原生结果一致；运行 `openspec status --change doc-view-mcp-tools --json`、`openspec validate doc-view-mcp-tools --strict`、`git diff --check` 与相对链接检查，逐项核对 checkbox 均有实现和证据后再勾选。提交、发布、外部配置写入、sync 与 archive 不属于本 tasks 自动执行范围。
