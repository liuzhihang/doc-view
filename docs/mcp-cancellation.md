# MCP 取消传播与验收

核对日期：2026-10-24。本文记录 Codex 客户端取消、IDEA MCP 服务端处理与 Doc View 后续设计的边界。结论依据官方源码/发行元数据、MCP 规范及隔离运行记录；候选客户端结果不代表生产 Doc View MCP 已交付。

## 当前结论

官方 Codex `0.165.0-alpha.1` Apple Silicon app-server 候选经真实模型工具调用和 `turn/interrupt` 完成三轮取消互操作：三次均协商 MCP `2025-06-18`、发送与原工具请求 ID 相符的 `notifications/cancelled`，IDEA 取消并清理对应任务一次，没有迟到的 `completed`，同一 session 的后续 echo 三次均成功。候选准入为 **3/3 passed**，生产基线升级可继续。

这项结果只证明隔离环境中的官方候选客户端与 IU `263.7031.23` MCP 服务端探针闭环。常用桌面 Codex `0.163.0-alpha.11.2` 的既有失败记录保留；候选没有替换日常客户端。该轮记录时生产插件仍声明261。1.6.0已采用IU263/Java25基线，当前构建、Verifier及原生证据另见[发布记录](release-1.6.0.md)；正式MCP工具仍未交付。

## 候选客户端运行证据

候选来自官方 [`rust-v0.165.0-alpha.1` release](https://github.com/openai/codex/releases/tag/rust-v0.165.0-alpha.1)。官方 GitHub [release metadata](https://api.github.com/repos/openai/codex/releases/tags/rust-v0.165.0-alpha.1) 声明 Apple Silicon app-server 资产 `codex-app-server-aarch64-apple-darwin.tar.gz` 的 SHA-256 为 `f99678f8b60657ee1e466ebcd4e2d6202c63bc9d12b9872ff4b8a8ac4b56f4af`；下载包摘要与此一致。解压后的可执行文件版本为 `codex-app-server 0.165.0-alpha.1`，SHA-256 为 `a2fea747e927713c65a2de22a1e7c1be84009e24d53f85948649ec41bb949c18`。同版本 `codex-code-mode-host` 归档摘要与官方元数据一致，为 `f1562ac9b54dd63fe724541da27fd65d99c10baef5a4cbb35fe67702bdd789ba`，解压后二进制 SHA-256 为 `196752c15bf96abd802271e5dda9587ffe5310a80ef7080c4af7615e687388b0`。候选是 prerelease，仅在临时 MCP 配置中运行。

隔离运行使用实际 Codex app-server、模型发起的两个白名单探针工具和真实 `turn/interrupt`，没有用自制客户端补发通知。MCP initialize 协商版本为 `2025-06-18`，会话 hash 为 `ffa259deefd382a3`。三轮 30 秒 delay 请求 JSON-RPC ID `5`、`7`、`9` 分别收到以该 ID 为目标的 `notifications/cancelled`；IDEA 探针记录 run `1`、`2`、`3` 各一次 `cancelled(reason=request_cancelled)` 和一次 cleanup，31.005 秒观察期内没有迟到的服务端 `completed`。后续 echo request ID `6`、`8`、`10` 全部成功。会话值只保留 hash；IDEA 日志不含 JSON-RPC ID，run 与 wire request 的对应关系按单一隔离 session 中的顺序和时间戳核对。

延迟口径分开记录：Codex 的 `turn/interrupt` 请求至 RPC ACK 分别为 102.954 ms、103.796 ms、102.763 ms；interrupt 请求至 IDEA `cancelled` 约为 6.359 ms、8.809 ms、4.622 ms。IDEA 的 `started` 至 `cancelled` 分别为 154 ms、73 ms、24 ms；`cancelled` 至 cleanup 分别为 1 ms、1 ms、0 ms。154 ms 是第一轮从 IDEA 接到 `started` 到记录取消的时间，不是 interrupt 请求耗时。

原始证据位于 `/tmp/doc-view-mcp-candidate-20261024/codex-run-04/`：

- `summary.json` SHA-256：`7392cd675d13dde7b5a7acb3d0510a7374e2a623994648fb6b1e0447f64aea4e`
- `transcript.jsonl` SHA-256：`3f91f7b3278c6fbe64be8588aa8e12917665214b12c540b6aa25346195d5dbe7`
- `wire.jsonl` SHA-256：`cba2f0d41ce6abe2be429849712a84edd655d5f7902640659c1d681b56151fef`
- `idea-codex-events.log` SHA-256：`874519f685674ef5b71070f2fe0b075967b8a0d6fe20a51cb6f252ad23c5783c`

按 transcript 修正 runner ACK 字段竞态后的摘要见 `/tmp/doc-view-mcp-candidate-20261024/final/codex-run-04-verified-summary.md`，SHA-256 为 `37912237924ab21c0552aef0eb9f56ea005fcea42a3b1b1e5263cf81e711568b`；机器可读摘要 JSON SHA-256 为 `9bf14fb4bd995b46e43df5f597429d7b22400f61df786087998e3975ef8c7639`。run-04 原始 summary 未被覆盖。

summary 中 `interrupt_response` 字段因 runner 等待竞态为空；原始 app-server transcript 仍保存三次 `turn/interrupt` `{result:{}}` ACK，延迟按 transcript 时间戳计算。早先 `codex-run` 1–3 的环境失效记录不计入通过轮次，也未覆盖或删除。HTTP 202 空响应本身不作为取消证明，判定使用了协议通知、IDEA 任务终止与 cleanup、无迟到完成及 echo 恢复的联合证据。

## 历史 Codex 0.163 失败记录

常用桌面 Codex `0.163.0-alpha.11.2` 在 2026-10-23 的实测中未向 IDEA 发出可观察的取消信号。隔离 wire 记录显示，在途 30 秒工具调用在 `turn/interrupt` 后仍以 HTTP 200 正常完成，IDEA 日志为 `started → completed → cleanup`；同 session 后续 echo 成功。该轮未保存 initialize 协商版本，因此不能追溯指定它应采用哪个协议分支。原始证据见 `/tmp/doc-view-mcp-upgrade-20261023/codex/delayed-run/gate-result.json` 和 `wire.jsonl`，完整记录见[基线升级验证记录](../openspec/changes/intellij-263-baseline-upgrade/verification.md)。

Codex [PR #56132](https://github.com/openai/codex/pull/56132) 于 2026-10-22 合并，merge SHA 为 `2dbe6bfa44195e0c63b9a2ee59f46482bfd64fae`。官方 [`rust-v0.165.0-alpha.1` tag 源码](https://github.com/openai/codex/blob/rust-v0.165.0-alpha.1/codex-rs/rmcp-client/src/tool_request.rs#L14-L49)含有取消 guard；来源与发行检查见上文。源码线索曾作为待复验候选，后由本节运行证据完成客户端准入。

## MCP 协议分支

每次复验都要保存 initialize 响应中的 `protocolVersion`，并按实际协商版本判断取消方式。MCP `2025-06-18` 通过 `notifications/cancelled` 携带原请求 ID 取消；接收方应停止处理、释放资源，且不为已取消请求再发送响应。取消通知是 fire-and-forget，发送方应忽略之后到达的响应，并妥善处理信号与完成之间的竞态。详见官方 [2025-06-18 取消规范](https://modelcontextprotocol.io/specification/2025-06-18/basic/utilities/cancellation)。

| 协商版本 | 取消信号 | 判定边界 |
| --- | --- | --- |
| `2025-06-18` | 对原 JSON-RPC request ID 发送 `notifications/cancelled`。 | 2026-10-24 Codex 候选按此方式完成三轮实测。HTTP 202 只表示通知请求被接收，不能单独证明任务停止。 |
| `2025-11-25` | 对原 JSON-RPC request ID 发送 `notifications/cancelled`。 | 同样核对服务端任务终止、cleanup 和迟到响应处理；见 [2025-11-25 取消规范](https://github.com/modelcontextprotocol/modelcontextprotocol/blob/main/docs/specification/2025-11-25/basic/utilities/cancellation.mdx#L203-L236)及[传输规范](https://github.com/modelcontextprotocol/modelcontextprotocol/blob/main/docs/specification/2025-11-25/basic/transports.mdx#L266-L269)。 |
| `2026-07-28` | 关闭对应工具调用的 request-scoped SSE response stream。 | 不把普通会话断开当作用户取消；核对该请求工作停止且不再向其发送消息。见 [2026-07-28 Streamable HTTP 取消规则](https://github.com/modelcontextprotocol/modelcontextprotocol/blob/main/docs/specification/2026-07-28/basic/transports/streamable-http.mdx#L230-L240)。 |

## IDEA 服务端和 Doc View 边界

IU `263.7031.23` 隔离 Java 探针在收到显式 MCP 取消后记录了任务取消和 cleanup；候选客户端运行进一步验证同 session 可继续调用。这只覆盖测试用探针和该隔离 build，不代表生产 Doc View 工具已实现。正式工具仍计划注册在 Doc View 插件内、由 IDEA 自带 MCP Server 托管，不另建 server；取消上下文要传到实际 PSI、搜索、文件读取或生成工作，并在长循环中检查，cleanup 要幂等。插件卸载和项目关闭的生命周期验收仍需覆盖。相关平台依据见 JetBrains [扩展点列表](https://plugins.jetbrains.com/docs/intellij/intellij-community-plugins-extension-point-list.html#comintellijmcpserver)和[Disposer 指南](https://plugins.jetbrains.com/docs/intellij/disposers.html)。

读接口可设置 5 秒服务端上限，文档生成可设 120 秒上限。超时是 Doc View 的资源保护，不能替代 MCP 客户端取消通知或保证 Codex turn 已中断。

文档库按文件发布；失败时报告已发布和未发布部分，不声称整批事务回滚。文件所有权与部分发布规则见[项目文档库设计](../openspec/changes/project-document-store/design.md)。

当前发布最低 IDEA build 仍为 261。用户已明确批准生产目标升级到 IU `263.7031.23` 与 Java 25 源码/API/classfile（major 69）；取消互操作 3/3 已通过。构建期 IntelliJ Platform Gradle Plugin 已按正式版 2.17.0 选定，其官方映射更新了 build 262 起的 Java 25 要求；本项目仍须运行配置检查、构建、测试、Plugin Verifier、最终 ZIP 读回和 IU 263 原生回归后才能更新支持范围。本文结果不代表正式 MCP 查询工具交付。
