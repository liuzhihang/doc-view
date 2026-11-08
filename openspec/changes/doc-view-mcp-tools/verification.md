# 验证记录

## 2026-10-24：候选客户端准入验证

官方 Codex `0.165.0-alpha.1` app-server 候选在隔离 IU `263.7031.23` 上完成三轮真实 turn-interrupt 取消互操作，MCP 协商版本为 `2025-06-18`。每轮均观察到匹配原 request ID 的 `notifications/cancelled`、IDEA 对应 run 的 `cancelled` 与一次 cleanup、31.005 秒内无迟到服务端完成，以及同一 session 后续 echo 成功。正式 Doc View 插件仍没有 MCP provider 或工具；本节只关闭候选客户端准入 tasks 1.1/1.2。

| 轮次 | delay request ID | interrupt ACK | IDEA started→cancelled→cleanup | 后续 echo | 迟到完成 |
| --- | ---: | ---: | --- | --- | --- |
| 1 | 5 | 102.954 ms | 154 ms / 1 ms | request 6 成功 | 31.005 秒内无 |
| 2 | 7 | 103.796 ms | 73 ms / 1 ms | request 8 成功 | 31.005 秒内无 |
| 3 | 9 | 102.763 ms | 24 ms / 0 ms | request 10 成功 | 31.005 秒内无 |

候选是官方 `rust-v0.165.0-alpha.1` Apple Silicon release；app-server 与 code-mode-host 归档摘要读回匹配官方 metadata，二进制版本和身份、完整轮次证据见[取消验收说明](../../../docs/mcp-cancellation.md#候选客户端运行证据)及其中的 verified summary。`0.163.0-alpha.11.2` 的历史失败仍保留：两轮透明 wire 都没有取消通知，服务端长任务约 30 秒后正常完成；进程退出引起的取消不计入 turn-interrupt 结果。

本轮使用临时 app-server 进程配置覆盖，未读取普通 Codex 全局配置内容。读回 hash 与先前基线不同，来源未知；不声称字节未变、不归因，也未尝试恢复。0.165 候选通过不表示日常客户端已升级或所有 Codex 版本都支持同样的取消行为。

PoC task 2.4 已依据此前 IU 263.7031.23 / SDK 0.15.0 探针的显式取消、在途插件禁用、服务关闭、项目关闭及有限资源证据，加上本节 Codex 候选三轮互操作关闭。当前用户已批准的开发目标为 IU 263.7031.23 与 Java 25。`intellij-263-baseline-upgrade` 的 full-10 自动构建、67 suites / 352 项测试和 Plugin Verifier 已通过；production Verifier 问题为 0，另有 16 处 Experimental API，测试源码编译另有 15 条 test-only deprecated/removal warnings。full-9 的 350 项、full-8 的 349 项与 full-7 的 344 项全绿为历史结果。full-10 新包原生验收已有部分场景通过：文档库 tamper/恢复专项中，篡改页被 Search/Open 拒绝，精确原 bytes 恢复后 Reload All from Disk、Search、Open 成功，banner 自动显示 `CURRENT · 30 API(s)`，63 个文件 bytes/hash 与 30 个条目的 schema/hash 有效；Spring/Feign/Dubbo 预览、Welcome 重开、GET/form 草稿生成和 HTTP Client 打开也已通过，HTTP Client Run 未执行。另一次 freshness 复测重开后先显示 `unknown`；随后 FindAction 的 Generate 返回 `30 API(s) / No content changes`，不点 Refresh 即自动变为 `CURRENT`，约 +4.7 秒和 +20.45 秒后仍为 current，未复现持续刷新问题。Copy/Export、菜单、完整参数编辑、原生生成取消/partial/retry 与资源基线仍未完成。项目文档库的 261 light fixture 定向通过不能代替其待验的 263 VFS 发布/取消和 UI 验收；旧包原生发现的 Markdown 恢复状态问题已修复并在 full-10 新包通过专项复验。故准入 task 1.3 仍未勾选；正式 MCP adapter、tools/list/tools/call、A/B 产品项目查询和显式生成均未实现或验收，第 2–5 组保持未勾选。已发布版本的支持范围仍是 261，不能将开发候选写成已发布支持。

## 2026-10-24：正式实现只读 API 准备

只读核对精确 IU `263.7031.23` bundled MCP 插件缓存。MCP 插件 ID 是 `com.intellij.mcpServer`；其 descriptor 声明 dynamic 扩展点 `com.intellij.mcpServer.mcpToolsProvider`，接口为 `com.intellij.mcpserver.McpToolsProvider`。`intellij.mcpserver.jar` 的 SHA-256 为 `934a850b539ca4cabafd330dd929f9fe97f97a0159c00dc0522bbab2cd69efc3`，同目录 Kotlin SDK 0.15.0 JAR SHA-256 为 `d85501edcf60d9f9d0c56413a4e01f19913c98d025f6164f416d45e0c86fcadd`。实际 API 签名经 263 JAR 的 `javap -public` 核对：`McpToolsProvider#getTools(): List<McpTool>`；`McpTool#getDescriptor()` 和 `McpTool#call(JsonObject, Continuation<? super McpToolCallResult>): Object`。工具描述符和 schema 可由公开构造器显式创建；`McpToolCallResult.Companion.text/error(String, JsonObject)` 均生成单个 Text 内容，`error` 的 `isError` 为 true。因此设计中的手工输入 schema 与单个 Text(JSON) envelope 可在 Java 中实现，无需 Kotlin、反射或 structuredContent。

263 的 `McpTool.call` 没有单独取消参数；此前 Java PoC 从 `continuation.getContext().get(Job.Key)` 取得该调用的 coroutine Job，并以 Job 取消/`ensureActive` 结束有界任务。该路径已在隔离 IU 263 上有显式取消、cleanup、插件禁用、服务关闭和项目关闭实测；Codex 候选三轮取消闭环见本页首节。生产工具应把 Job 取消桥接到后台任务的 `ProgressIndicator.cancel()`，以便复用现有 `ProjectDocumentIndexService` 的 `status/search/getDetail/getMarkdown(..., ProgressIndicator)` 和 `ProjectDocumentGenerationService.generate(ProgressIndicator)`；后台任务须一次性恢复/失败 continuation，并分别保留客户端取消、期限、项目/插件生命周期与最终 index 提交竞态的语义。

最小实现边界是 provider、固定工具描述/schema 与 JSON envelope adapter、以及项目级 MCP 调度服务；调度服务按必填 `projectPath` 唯一解析已打开项目，限制并发并处理 Job/indicator 与项目生命周期，再调用现有索引和生成服务。生产可选注册应由主 descriptor 的 optional dependency 指向 `com.intellij.mcpServer`，用单独 config-file 注册 provider，确保普通 UI/service 不引用 MCP 类型。已有 Java PoC 是独立插件并使用 required dependency，因此它没有验证生产可选依赖缺失/禁用时的 class-loading 隔离；这仍需专门自动及原生测试。输出 schema/structuredContent 也未作为首版依赖。以上只读核对不关闭准入 task 1.3，不勾选第 2–5 组，也不代表生产注册或调用通过。

## 2026-10-24：工具契约规划（候选运行前的状态）

当时完成四个工具的 OpenSpec artifacts、`tool-contracts.json`、虚构请求/结果样例和用户说明。**生产插件未注册这些工具**；当时尚未下载或运行新的 Codex 候选，也没有修改最低 IDEA build。该段是候选实测前的历史状态；现行候选结果见上节。

已完成：

- `openspec validate doc-view-mcp-tools --strict` 通过。
- 使用仓库外 `jsonschema==4.25.1` 检查四个 inputSchema、结果 Text(JSON) envelope 及其共享定义；与文档库样例合计21个正例、18个拒绝例通过。schema 输入拒绝额外字段和缺少 projectPath；四种工具的成功、INDEX_NOT_FOUND 与 PARTIAL_PUBLICATION 错误样例均可按定义解析。
- `mcp-wire-response.json` 仅演示一个 TextContent 中装 JSON，反序列化后与 search 内容样例一致；没有把它标为真实协议调用，也未承诺 structuredContent/outputSchema API 已验。
- 磁盘字段定义与源 schema 相同，身份/hash/快照样例检查、变更文档链接和 `git diff --check` 通过；详见[文档库验证](../project-document-store/verification.md)及临时 `schema-validation.json`。

当时仍未通过的前置条件是旧 Codex `0.163.0-alpha.11.2` 的 turn-interrupt 取消验收；当时仅确认官方 `rust-v0.165.0-alpha.1` 源码含 PR #56132。上述候选运行前结论已由本页首节的新实测更新；旧版本的实际失败继续保留，完整区分见[取消调查](../../../docs/mcp-cancellation.md)。

候选运行前的历史状态：当时未执行生产 Java MCP adapter、Gradle build/tests、Plugin Verifier、正式工具 tools/list/tools/call、UI 与 MCP 同源结果、项目隔离、真实生成/取消/卸载、资源及性能原生验收；当时的开发/正式基线记录为 261。后续当前 263 full-10 自动构建、67 suites / 352 项测试和 Verifier 已通过，production Verifier 问题为 0；16 处 Experimental API 与 15 条 test-only deprecated/removal warnings 分开保留。full-9/8/7 的 350/349/344 项全绿为历史结果。full-10 新包的原生回归已部分通过，但 Copy/Export、菜单、完整参数编辑、生成取消/partial/retry 与资源基线仍未完成；文档库 tamper/恢复、代表性预览、Welcome 重开及 GET/form 草稿生成打开已有新包证据。正式生产 adapter 与工具仍未实现，文档库最终验收及基线原生矩阵未完成，因此 task 1.3 继续未勾选。精确升级和当前收尾状态见[基线 change](../intellij-263-baseline-upgrade/tasks.md)及其[验证记录](../intellij-263-baseline-upgrade/verification.md)。本轮最终 scope 审计和四个 strict validation 均通过；未提交、推送、发布、sync 或 archive。
