# 2026-11-20 候选包 MCP 取消、项目隔离与生命周期续验

## 验收身份

- 候选插件包：`1.7.2-SNAPSHOT`，ZIP SHA-256 `bd0d5a404d061050d8ed7d429aa904550feea1b0956b5d5ced346f90f35999ed`。
- 候选插件 JAR SHA-256 `f55e13cf168985f80710efa5935ee381bed66c13417cb4485b80fb08716075b3`；隔离 profile 的安装 JAR 与包内 JAR 一致。
- 原生宿主：IntelliJ IDEA IU `263.7307.200`，隔离 profile `build/mcp-ab-acceptance-2026-11-20/idea-profile`。
- 客户端：ChatGPT.app 随附 Codex CLI `0.169.0-alpha.11.1`；MCP 协商协议为 `2025-06-18`。
- A/B 均为 `build/mcp-ab-acceptance-2026-11-20/native/` 下的虚构项目。IDEA MCP 只绑定 `127.0.0.1:64342`；JDWP 只绑定 loopback `127.0.0.1:6507`。Codex HOME/CODEX_HOME 独立、未复制凭据或普通 Codex 配置，未触碰 Junie。

证据目录都在 Git 忽略的 `build/mcp-ab-acceptance-2026-11-20/` 下；没有把临时 Codex 数据、IDEA profile 或测试项目加入跟踪文件。

## A/B 查询、生成和冲突

- `codex-app-server-rerun6/events.jsonl` 中 A、B 首次生成均返回 `created=2`、`publishedPathsTotal=6`；重复生成返回 `unchanged=2`、`publishedPathsTotal=0`。该轮后段的测试脚本对后续更新数量预期错误，完整轮次不计为通过。
- 修正夹具并由 IDEA 原生“从磁盘全部重新加载”后，`codex-app-server-rerun7/events.jsonl` 的 A 更新返回 `updated=2`、`publishedPathsTotal=4`；B 的状态仍为同一个 snapshot，B 文件树未变化。
- `codex-app-server-rerun8/events.jsonl` 中 A 的搜索和详情返回更新后的虚构标记；只读调用前后 A/B 文件树均无差异。
- `codex-app-server-rerun11/events.jsonl` 中，在 A 的一份 Markdown 详情页添加虚构手改标记后再次生成返回 `CONFLICT`、`publishedPathsTotal=0`、`indexCommitted=false`，受保护页面字节未被覆盖，B 无变化。测试写入的标记随后恢复为生成版本哈希 `205a4a32142a08c883c68e2d130601a401adf3e7ff24d0359846b3ce51ceb79d`。

这些用例验证传入 A 路径时写入范围只落在 A；`projectPath` 是显式项目路由参数，不是绑定 Codex thread 工作目录的安全边界。测试也按契约显式查询了 B，不能把它描述为“从 A thread 无法选择 B”。

## 4.3 真实取消与生命周期

### 自动化边界

最终候选的定向回归命令 `./gradlew --offline --console=plain --no-parallel test --tests '*DocViewMcpLifecycleTest' --tests '*ProjectDocumentCancellationPublicationTest'` 通过：MCP 生命周期 9 项、文件发布取消 5 项，失败、错误、跳过均为 0。生命周期用例区分 `TIMEOUT`、`PROJECT_NOT_OPEN`、`CANCELLED`，并断言服务释放运行任务一次；发布用例覆盖各文件移动前后、`index.json` 最终提交及迟到取消。

候选完整构建命令 `./gradlew --offline --console=plain --no-parallel -I tools/doc-view-skill/acceptance.init.gradle test buildPlugin verifyPluginProjectConfiguration` 退出码为 0。对应本机忽略证据：`build/openspec-followthrough-20261120/final-full.json`、`build/openspec-followthrough-20261120/final-full.log`、`build/openspec-followthrough-20261120/final-skill-transport-cleanup.json`、`build/openspec-followthrough-20261120/final-skill-transport-cleanup.log`。

### Codex 在途取消

在同一 Codex app-server、thread 和 MCP session 中，JDI 把 exporter 暂停在 `ProjectDocumentExporter.export` 入口后，实际 Codex `turn/interrupt` 被接受；生成 `tools/call` request ID 2 与 IDEA 收到的 `notifications/cancelled.requestId` 2 匹配。恢复 status 从 `generating=true` 变为 `false`，`cleanupObservedMs=128.613`，1 秒观察期无迟到生成完成，7 个文档文件哈希不变。客户端使用本机 synthetic provider 选择工具调用；这不是自然模型或 Skill 指令遵循验收。

本机忽略证据：`build/mcp-ab-acceptance-2026-11-20/codex-cancellation/runs/20261119T175732Z-24926-eeb626c5/result.json`、`build/mcp-ab-acceptance-2026-11-20/codex-cancellation/runs/20261119T175732Z-24926-eeb626c5/mcp-proxy.jsonl`、`build/mcp-ab-acceptance-2026-11-20/jdi-cancellation-runs/20261119T175731Z/jdi-events.jsonl`。

### 最终候选的索引提交前后取消门

2026-11-20 在同一冻结包、隔离 profile、宿主 PID 37955 与 loopback MCP server 上补完两个真实 Codex 取消边界。宿主为 IU 263.7307.200、profile `build/mcp-ab-acceptance-2026-11-20/idea-profile`；候选 ZIP SHA-256 为 `bd0d5a404d061050d8ed7d429aa904550feea1b0956b5d5ced346f90f35999ed`，安装 JAR SHA-256 为 `f55e13cf168985f80710efa5935ee381bed66c13417cb4485b80fb08716075b3`。每轮 Codex 都使用独立临时 HOME/CODEX_HOME、本机合成模型、同一个 app-server/thread/MCP session；未使用自然模型，没有触碰普通 IDEA 配置或 Junie。

| 边界 | 原生停点与 Codex 取消 | 同会话恢复读回 | 发布文件读回 |
| --- | --- | --- | --- |
| 页面已发布、`index.json` 未提交（A） | JDI 在 `ProjectDocumentExporter.atomicReplace` 第 2 次命中暂停；第一份变更文件已完成原子替换。Codex `turn/interrupt` 被接受，`tools/call` ID 2 与 IDEA 的 `notifications/cancelled.requestId` 2 相同；JDI 观测 `Run.cancel("CANCELLED")`。 | 同 app-server/thread/MCP session 的 status turn 完成，`generating=false`、`indexState=ready`、`freshness=unknown`；返回 snapshot 与原磁盘索引的 `97715e0da175c5c7db9ab0b2bdb483a9edc7e13e78bc52bae48183a44b27c704` 相同。1 秒观察期没有迟到的生成完成。`unknown` 原样保留，不解释为 current。 | 两份 API 文件的 SHA-256 与基线不同，`index.json` SHA-256 前后均为 `634066a59c0ae9fdd0e89d6af77f51ee9dd6ff50592e6f1dee622ff6b5a0f9c0`，暂存文件数为 0。被取消的 JSON-RPC 请求不要求再返回业务 partial envelope；验收用 JDI move 顺序和取消后的磁盘哈希证明实际 partial 文件状态。 |
| `index.json` 已提交、缓存安装回调尚未完成（B） | JDI 在 `ProjectDocumentIndexService.installCommittedIndex` 入口暂停，确认最终索引已原子移动。Codex `turn/interrupt` 被接受，request ID 2 匹配取消通知，JDI 观测 `Run.cancel("CANCELLED")`。 | 同 app-server/thread/MCP session 的 status turn 完成，`generating=false`、`indexState=ready`、`freshness=current`；snapshot `b0c258a78ee015409bf0f298445513f3259e2efe1db6b4dd74050a0107872c62` 与新磁盘索引一致。1 秒观察期没有迟到生成完成。 | `index.json` SHA-256 从 `a6751c69039b9eee930e981600a3aa875862d8018a0ab34d2db662a5046d8184` 变为 `937428d23d65d789f8540a5ce22f7306b08051481fe7e101cc289e8882cdf717`；4 份 API Markdown/JSON 详情哈希和 `index.md` 哈希均按新索引逐字节重算匹配，暂存文件数为 0。新发布目录留存于 `runs/20261120T000934Z-44514-29e9b272/published-after-commit/docs/doc-view/`。 |

取消通知到手动 JDI 放行的观测间隔分别为 18.849 秒和 13.632 秒，其中包含操作者输入 `release` 的时间，不表示插件取消耗时；放行到 Codex 恢复 status 分别为 29.989 毫秒和 25.386 毫秒。两轮 harness `error=null`，均匹配真实取消 request ID，随后 status 返回 `generating=false`。

首个前索引轮次到达真实页面发布与取消，但旧 harness 把 5 秒清理截止时间从 interrupt 起算，超过时间等待人工放行后没有执行恢复查询；它不计通过，原结果保留在 `cancel-gates/runs/20261120T000436Z-42991-efaa4adf/result.json`。修正为从 JDI 放行后起算并重跑后，完整闭环通过；没有并发发起占用文档写锁的 status 请求。

两项目文件指纹也交叉核对：A 取消前索引门运行时 B 的 8 个路径 SHA 未变，B 提交门运行时 A 的 8 个路径 SHA 未变。A/B 文档树初始指纹分别为 `3a25a38d6c69c63529a01ab50f8a34af8940aad8c995151f44af191ceb4b258a`、`0c157bccea1dbe85b306145de4fcc3c1d43447ec660e9ec4f0b9e7ea5e2ca8b8`；两个 fixture 源码标记及测试文档树随后均恢复到这两个原始指纹。交叉检查和恢复记录分别在 `cancel-gates/cross-project-after-a.json`、`cancel-gates/cross-project-after-b.json`、`cancel-gates/cross-project-final-restoration.json`。

本次仅使用 ignored `build/` 测试夹具和证据目录，未修改生产代码。两个取消门补齐后，5.3 列出的当前候选读写、冲突、A/B 隔离、部分发布、提交后快照、项目关闭、宿主重启和插件禁用路径均有对应自动或原生记录，故将 5.3 标记完成。旧 PID 22813 上 B 曾出现的 `stale` 仍找不到原因；后续同因果检查没有复现，保留为未解释的历史观察。

### 项目关闭、正常退出与重启

在项目 A 的生成暂停期间，IDEA 原生关闭 A，JDI 观察到 `DocViewMcpTaskService.dispose()`、`Run.cancel("PROJECT_NOT_OPEN")` 和 exporter 放行后的 `Run.finish()`。同一 Codex app-server 和同一 thread 随后仍能查询 B；该次 B 状态为 `ready/stale`。同一运行根下启动的新 Codex app-server 也能查询 B，但 freshness 仍是 `stale`。本机忽略证据：`build/mcp-ab-acceptance-2026-11-20/project-close/codex-app-server-session/events.jsonl`、`build/mcp-ab-acceptance-2026-11-20/project-close/codex-app-server-session/fresh-app-server-events.jsonl`、`build/mcp-ab-acceptance-2026-11-20/project-close/jdi-runs/20261119T181316Z/jdi-events.jsonl`。

原宿主 PID 22813 随后正常退出；使用同一隔离 profile 和 B 重新启动后的 PID 为 29308。新 Codex 会话对 B 的只读 status 成功，初始 `freshness=unknown`。明确 generate 返回 `unchanged`、0 个 published paths 后，状态成为 `current`；B 的 7 个文件树哈希前后相同，且无 `.tmp`、`.part`、`.stage` 遗留。过程结果见本机忽略证据 `build/mcp-ab-acceptance-2026-11-20/restart-check/host-restart-result.json`。

随后在 B 保持 current 时原生重新打开并关闭 A。打开 A 前、打开后和关闭后，B 的 JDI epoch 都为 `inputEpoch=0`、`committedInputEpoch=0`，snapshot 均为 `21587a5077b2bf37f200c65bd253c2c3dc5b9021412d5de296216888eacff1b0`；两次 Codex status 都是 `ready/current`，`generating=false`。打开 A 未创建第二个 `ProjectDocumentIndexService` 实例。说明单独开关 A 没能复现前述 B stale 结果。

本机忽略证据：`build/mcp-ab-acceptance-2026-11-20/epoch-isolation/b-current-baseline.json`、`build/mcp-ab-acceptance-2026-11-20/epoch-isolation/b-before-open-a-jdi.txt`、`build/mcp-ab-acceptance-2026-11-20/epoch-isolation/b-after-open-a-jdi.txt`、`build/mcp-ab-acceptance-2026-11-20/epoch-isolation/b-after-open-status.json`、`build/mcp-ab-acceptance-2026-11-20/epoch-isolation/b-after-close-a-jdi.txt`、`build/mcp-ab-acceptance-2026-11-20/epoch-isolation/b-after-close-status.json`。

### 插件禁用后的工具发现

在隔离 IDEA 中只禁用 Doc View 插件并重启，MCP Server 仍运行于 loopback。新的真实 Codex 会话配置明确允许四个 Doc View 工具；`mcpServerStatus/list` 仍显示 server `connected`，工具表为空，四个 Doc View 名称均不可见。没有发起工具调用。B 文档树在本轮前后不变、无临时文件，Codex app-server 以 0 退出。验收后 MCP Server 已由维护者关闭，隔离宿主退出。

本机忽略证据：`build/mcp-ab-acceptance-2026-11-20/tools-disabled/no-doc-view-tools-result.json`、`build/mcp-ab-acceptance-2026-11-20/tools-disabled/codex-session/no-doc-view-tools-events.jsonl`。这验证的是插件禁用且按重启生效后工具不再发现，不代表物理卸载 MCP Server 插件或热卸载 Doc View。

## 未通过或未完成项

1. PID 22813 上那次 B `stale` 的旧 epoch 没有在进程退出前通过 JDI 捕获，进程已退出后无法补读。新的 B current → 打开 A → 关闭 A 因果复验未让 B epoch 增长，也未重现 stale；旧结果的具体原因仍未知。保留该观察，不据此修改代码，也不将其写成已通过。
2. 2026-11-19 同候选提交后取消轮次曾与辅助 pre-cancel status probe 竞争写锁，整体结果不通过；2026-11-20 移除并发 probe、由 JDI 停点驱动取消后，同一提交后边界通过。原失败轮次仍保留，不作为当前验收结果。

## 契约与文档一致性复核

4.3 和 5.3 自动及原生验收均已在 `tasks.md` 标记完成。5.3 的历史 B `stale` 观察仍未解释，失败的旧 harness 轮次保留；2026-11-20 最终候选两个取消门的成功重跑已补齐各自失败窗口。包内 `mcp/tool-contracts.json` 与 OpenSpec `tool-contracts.json` 的字节及 SHA-256 完全一致（`400aacb3bc373117e01288d995be7969d3e962c2ace58880a4a1775752ec2a17`）；安装 JAR 与候选 ZIP 内 JAR SHA-256 一致（`f55e13cf168985f80710efa5935ee381bed66c13417cb4485b80fb08716075b3`）。插件 descriptor 保留 `com.intellij.mcpServer` 可选依赖并注册 `DocViewMcpToolsProvider`；真实客户端在启用时发现四工具、插件禁用重启后四工具均不可见。MCP change 相关 18 份 Markdown 中检查了 165 个本地链接目标，缺失 0；change contracts/examples 的 9 份 JSON 均可解析。`openspec validate doc-view-mcp-tools --strict` 与本 change 文件范围的 `git diff --check` 均通过。5.5 的一致性复核已完成。
