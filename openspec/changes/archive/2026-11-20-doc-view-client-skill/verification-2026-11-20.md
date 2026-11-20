# 2026-11-20 最终候选设置与两范围安装

在隔离 IntelliJ IDEA IU `263.7307.200`、Java 25、macOS Apple Silicon 上使用 `1.7.2-SNAPSHOT` ZIP `2f0284a418459a7e6e102500ec526d6b37fe68e0d4b75cae04664de40337575a`，实际 Codex 为随 ChatGPT 安装的 `codex-cli 0.169.0-alpha.11.1`。全局指隔离 IDEA 的临时 HOME。

- 重新运行 `CodexSkillSettingsTest`，15 项通过，0 失败、错误、跳过。原生页面自动检查，两范围可键盘切换，正确显示各自的安装路径及相关动作；设置快捷链接进入 IDEA 自带 MCP Server 设置。未开启 MCP 服务。
- 官方 1.7.1 在两范围均从空目标完成首次安装。原生显示已安装且由 Codex 发现，两项 metadata 均记录目标范围、实际 Codex build 和正式 commit `0894d9e427827a49eb3e2142bc674c8a5941fc83`；`SKILL.md` 摘要均为 `01d69274b1ee5bc757cb8b7273ba7082314652ac99a7624dd2c1c804bb5ff99f`。项目操作结束时全局目标仍不存在。
- 以官方 1.7.0 的字节与 manifest 字段准备两个隔离旧版夹具。项目范围显示可更新，通过真实官方 1.7.1 更新，且全局旧版元数据及 Skill 保持原字节。全局加入一条合成改动后，设置显示文件有本地修改并禁用更新，原文与旧元数据均保留。随后恢复测试文件原字节。
- 恢复后全局重查显示离线，保留旧版。使用同一 JBR 和精确 API 路径独立确认 GitHub 返回 `HTTP 403`、`x-ratelimit-remaining=0`。未完成本包的全局 1.7.0 → 1.7.1 重试及精确下载取消。两范围无 journal、stage 或 backup 残留，测试结束将全局旧版夹具的安装元数据恢复到先前已完成的 1.7.1 原生记录。
- `SkillInstallTransactionTest` 16 项与 `CodexSkillInstallServiceTest` 10 项通过，0 失败、错误、跳过，包含两个范围暂存取消、进程退出恢复、旧版本保留及锁。与现网复验的受限项分开记录，真实下载阶段取消与 IDEA 重启恢复仍有缺口。
- 原始读回文件位于 `build/openspec-remaining-review-20261120/`，包括 `skill-final-scope-proof.json`、`skill-fresh-{PROJECT,USER}.metadata.json` 和 `skill-network-jbr-recheck.log`。

## 在途 HTTP body 取消回归

为直接复验生产等待/取消路径，提取了共享的 package-private `awaitHttpResponse(future, indicator)`；生产下载仍使用原固定 `HttpClient`、官方 API URL、host/path 校验、redirect allowlist 和响应大小限制，没有新增可配置 URL 或认证。

新增定向测试启动本机 loopback `HttpServer`，由 JDK `HttpClient` 接收一个 8 MiB response body。服务器发送首个 8 KiB 后阻塞，测试先断言 `CompletableFuture` 仍 pending，再通过 production helper 走 100 ms polling 与取消分支。释放服务器后，测试要求客户端 future 异常结束且服务端在发送剩余 body 时观察到 client disconnect。该路径没有访问 GitHub，也未使用凭据。

最终定向测试 `cancelsPendingHttpClientBodyTransferWhenTheProgressIndicatorIsCancelled` 单项通过。第一版断言在 `cancel(true)` 返回后立刻检查 `future.isCancelled()`，在 JDK HTTP future 上观察到 `false`，所以改为断言异常终态及服务端断连；不把这一中间观察误判成生产缺陷。Skill 三类聚合测试曾在 32 项中有 31 项通过、1 项因这条过窄的即时断言失败；调整判定并补齐异常时关闭 loopback 服务后，三类 Skill 测试聚合复跑 32 项全部通过，另有两类 MCP 取消/发布测试 14 项同时通过，0 失败、错误或跳过。日志为 `build/openspec-followthrough-20261120/final-skill-transport-cleanup.log`。

这补上了真实 JDK HTTP transport 的确定性取消回归，但不等于最终插件包从官方 CDN 取消。公共 GitHub API 在本轮回读仍为 `HTTP 403` / rate limit 0，因此官方源在途取消以及隔离 IDEA 重启 journal 恢复继续保留未通过。

原生 journal 重启复验的隔离 profile 与路径计算说明位于 `build/openspec-remaining-review-20261120/skill-native-journal-recovery-20261120/README.md`；它只准备空目录，没有启动 IDEA 或伪造 journal。


## 2026-11-20 下载恢复与两范围真实 IDE 重启复验

GitHub 公共额度恢复后，用本次 IDEA 同一 JBR 对 release list、manifest asset、ref 及对应 tag 请求读回 HTTP 200。新候选 ZIP `bd0d5a404d061050d8ed7d429aa904550feea1b0956b5d5ced346f90f35999ed`，安装 JAR `f55e13cf168985f80710efa5935ee381bed66c13417cb4485b80fb08716075b3`，隔离 IDEA IU `263.7307.200`，Codex `0.169.0-alpha.11.1`，同一隔离 profile 的两个 scope 以已验证的正式 1.7.0/1.7.1 资产与 metadata 准备 `OLD_BACKED_UP` 中断事务。夹具在 IDE 停止后注入，未声称现场制造崩溃。1.7.0 与 1.7.1 的 `SKILL.md` 字节相同，版本与源 commit 通过 metadata 分别核对。

重新启动后，先对当前项目点击“恢复 Skill”。原生页面显示“正在恢复 Doc View Skill”，随后按既有恢复后更新流程读到「已安装 1.7.1 · 可用 1.7.1」，工具提示确认 Codex 已发现。该动作结束后用方向键切换到全局，路径指向隔离 HOME 的 `.agents/skills/doc-view`，仍显示原全局事务待恢复。再明确点击全局“恢复 Skill”，同样显示处理进度、官方 1.7.1 已安装且 Codex 已发现。两个范围最终 metadata、发行 tag、commit、Skill SHA 均通过读回，journal、stage、backup、metadata 临时备份均为空。正常完成的恢复按钮会恢复旧事务并继续显式更新，本轮未在恢复与官方检查之间中断，按实际最终 1.7.1 检验。

新包全量 583 项、构建、配置检查、Gradle `verifyPlugin` 的最低 build 与独立 Plugin Verifier 的两个精确 build 均通过。定向 Skill 三类 32 项包含生产 HTTP 等待逻辑取消、两范围暂存取消、程式退出恢复和失败回滚；这组回归与当前包真实 IDEA 重启恢复合并覆盖 4.3。官方下载期间主动取消、设置/项目关闭时的进程资源全矩阵另行跟踪。在隔离同包 MCP / AI 页面也点击“打开 MCP Server 设置”，实际进入 IDEA 内置 MCP 的启用/端口页面。

证据位于 `build/openspec-followthrough-20261120/` 的 `skill-network-after-reset.log`、`skill-journal-setup.json`、`skill-journal-recovered.json`、`final-full.log` 与 Verifier 日志。


### 当前包两范围首次官方安装

另一个干净隔离 profile 的 Project、Global 目标均初始不存在。当前包页面自动检测同一 Codex build，明确点击项目 Install 后出现“正在安装 Doc View Skill”，最终显示 1.7.1 已安装且 Codex 已发现；磁盘校验该项目 `SKILL.md` 且全局目录仍不存在。方向键切换到全局后，原生页面读回独立的隔离 HOME 路径；从空目录明确点击同一安装动作，最终同样显示 1.7.1 已安装且已被 Codex 发现。两范围 metadata 的版本和客户端值正确，正文 SHA-256 都是 `01d69274b1ee5bc757cb8b7273ba7082314652ac99a7624dd2c1c804bb5ff99f`。实际版本、点击、状态和磁盘读回证据保存在 `build/openspec-followthrough-20261120/skill-app/` 的 `first-install-meta.json`、`first-project-readback.json` 和 `first-both-readback.json`。

结合前述同包官方更新、恢复与构建/Verifier，6.4 通过。检查更新后立即关闭设置的尝试未观察到在途阶段，不作为取消通过。官方下载 body 取消仍由 5.3 / 7.4 跟踪。


同一新装 profile 临时通过 IDEA 自带 HTTP Proxy 设置指向一个无监听的 loopback 端口。单独设置 JVM proxy 属性时 IDEA 仍在线，该尝试不计为离线。显式检查后，Project 和 Global 均显示“无法检查 Skill 更新，已保留现有文件”，Project 的版本摘要明确显示“上次查到 1.7.1”。两范围的安装正文与 metadata SHA-256 逐一与离线前相同，journal 仍为空。还原隔离 IDE 的代理设置后退出；没有改变机器网络、普通 IDEA 或全局用户 HOME。此项证明已安装文件在官方源不可达时保留，不替代官方下载在途取消。
