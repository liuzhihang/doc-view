# 2026-11-17 目标客户端取消与项目关闭生命周期续验

## 验收身份

- 插件候选：`1.7.2-SNAPSHOT`，ZIP SHA-256：`f0d5ad184d97b393a9b737488f09e75d4a66cdd36feb4e061e187882cf881dcb`。
- 原生宿主：隔离 IntelliJ IDEA IU `263.7307.200`；目标客户端为 ChatGPT 应用随附的 Codex CLI `0.169.0-alpha.11.1`；MCP 协议协商版本为 `2025-06-18`。
- 使用隔离项目 `build/remaining-acceptance-2026-11-17/native/project-a` 和 loopback IDEA MCP `127.0.0.1:64343`。
- Codex 真实运行 `app-server` 并调用 IDEA 的真实 MCP 工具；本地 synthetic Responses provider 只按测试流程选择工具调用，不代表自然模型或 Skill 工作流。临时运行未读取或复制普通 Codex 凭据，Junie 未触碰。

## 真实 Codex 在途生成取消

第二轮取消在真实 exporter 入口暂停期间执行。取消前，同一 MCP session 的只读 status 返回 `generating=true`。Codex 在同一 app-server 进程、ephemeral thread 和 MCP session 中发送 `turn/interrupt`；turn 以 `interrupted` 结束，IDEA 收到的 `notifications/cancelled.requestId=2` 与生成 `tools/call.id=2` 匹配。JDI 记录服务收到 `Run.cancel("CANCELLED")` 后，操作者放行 exporter 线程，随后观察到 `Run.finish()`；协调记录确认没有注入合成取消。

取消后，status 最终变为 `generating=false`；Codex 在同一 thread/session 的恢复 status turn 正常完成，并在 1 秒观察期内未出现迟到的生成完成事件。取消后的文档库快照与基线一致，127 个文件未变化。

结果记录的 `cleanupObservedMs=130.469` 包含为观察取消而额外暂停 exporter 线程的 100 ms，不是吞吐或纯服务清理性能基线。

证据：Codex harness 结果 `build/mcp-lifecycle-2026-11-17/codex-cancellation/runs/20261117T163137Z-15462-cfece805/result.json`、MCP 双侧代理事件 `build/mcp-lifecycle-2026-11-17/codex-cancellation/runs/20261117T163137Z-15462-cfece805/mcp-proxy.jsonl`、app-server 事件 `build/mcp-lifecycle-2026-11-17/codex-cancellation/runs/20261117T163137Z-15462-cfece805/app-server.events.jsonl`、JDI 取消与 exporter 事件 `build/mcp-lifecycle-2026-11-17/native-client-cancel-r2.jsonl`、取消后的 JDI 放行记录 `build/mcp-lifecycle-2026-11-17/cancel-release-coordination.json`。

### 保留的失败轮次

`20261117T163031Z-15328-36fa5473` 也观测到取消前 `generating=true`，且 Codex 中断及 MCP 取消 request ID 匹配；但调试器暂停的 exporter worker 未及时放行，5 秒清理观察期限到时仍为 `generating=true`，因此没有同 thread 恢复查询。该轮是调试器放行时序未完成的仪器失败，不能计作取消验收通过，也不能据此判断生产取消实现失败。项目文档快照未变化。证据：失败轮次结果 `build/mcp-lifecycle-2026-11-17/codex-cancellation/runs/20261117T163031Z-15328-36fa5473/result.json`。

## 项目关闭与资源回收

另一轮使用同一目标 IDEA/项目，在 `ProjectDocumentExporter.export` 入口暂停真实 `doc_view_generate`。关闭项目时观察到 MCP task service `dispose()` 与 `Run.cancel("PROJECT_NOT_OPEN")`；MCP 调用以 `PROJECT_NOT_OPEN` 错误结束。操作者随后放行暂停线程并观察到 `Run.finish()`。同一 Codex app-server 的同一 ephemeral thread 重开项目后调用 `doc_view_status` 成功：`indexState=ready`、`generating=false`、`freshness=unknown`。这里保留 `unknown`，不描述为 current。该轮使用 app-server 直接协议调用，`modelTurnUsed=false`，不是模型行为验收。

项目关闭后 full GC 观察中，`McpAiSettingsPanel`、`CodexSkillInstallService`、`ProjectDocumentIndexService`、`DocViewMcpTaskService` 与 `DocViewMcpTaskService$Run` 的活实例数均为 0。该结果证明这次项目关闭与 GC 观测，不证明动态卸载整个插件 classloader。

证据：项目关闭 JDI 事件 `build/mcp-lifecycle-2026-11-17/native-project-close-r2.jsonl`、Codex app-server 同 thread 关闭/重开事件 `build/mcp-lifecycle-2026-11-17/codex-app-server-session-r2/events.jsonl`、项目关闭后 GC 实例统计 `build/remaining-acceptance-2026-11-17/after-project-close-gc.json`。

## 最终候选自动检查与 Verifier

最终 ZIP `1.7.2-SNAPSHOT` 共 564 项测试，失败、错误、跳过均为 0；全量结果同时记录两个已验证 build `263.7031.23`、`263.7307.200`，原生运行 build 为 `263.7307.200`。该候选仍未发布。ZIP、全量测试摘要及执行结果见最终构建摘要 `build/remaining-acceptance-2026-11-17/final-build-summary.json`、测试计数 `build/remaining-acceptance-2026-11-17/full-final-counts.json`、测试执行记录 `build/remaining-acceptance-2026-11-17/full-final.json`。

两精确 build 的 Plugin Verifier 最终均为 `Compatible`，各报告 16 项 Experimental API 使用：最低 build 263.7031.23 报告 `build/remaining-acceptance-2026-11-17/minimum-verifier-report/IU-263.7031.23/report.html`、运行 build 263.7307.200 报告 `build/remaining-acceptance-2026-11-17/release-verifier/IU-263.7307.200/report.html`。最低 build 首次 `verifyPlugin` 因清理共享 Plugin Verifier extracted-plugin 目录时出现 `DirectoryNotEmptyException` 退出；随后单独重试 exit code 0，Compatible 报告通过。失败和重试分别见首次结果 `build/remaining-acceptance-2026-11-17/minimum-verifier.json`、重试结果 `build/remaining-acceptance-2026-11-17/minimum-verifier-retry.json`。运行 build 的独立 Verifier exit code 为 0，见结果记录 `build/remaining-acceptance-2026-11-17/release-verifier.json`。

## 尚未完成

- 此轮只覆盖一个项目上的真实客户端取消及项目关闭，不是完整 A/B 读写、冲突、各文件发布边界和最终 index commit 后迟到取消矩阵。
- 合成 provider 决定工具选择；未验自然模型、Skill 安装或 Skill 指令遵循。
- 只验证了项目关闭和一次 full GC；未验证物理卸载插件或动态热卸载。项目重新打开后的 `freshness` 仍为 `unknown`。
- 以上新增证据不覆盖 4.3 的 PSI 遍历/逐文件发布边界和所有终态，不满足 5.3 的完整最终包 A/B 矩阵，也不等同于完成 5.5 全量 change 一致性复核。因此 `tasks.md` 中 4.3、5.3、5.5 继续保持未勾选；本轮不据此归档或发布。
