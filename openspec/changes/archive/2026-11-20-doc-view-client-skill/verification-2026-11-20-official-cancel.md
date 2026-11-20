# 2026-11-20 Skill 官方下载取消

实际宿主为隔离 IDEA IU `263.7307.200`，实际客户端为 `codex-cli 0.169.0-alpha.11.1`。整合 ZIP SHA-256 为 `fe1241263266691aeb239c5ebedddc1e2ff544ebb717a65d6f517f79383fe467`，插件 JAR SHA-256 为 `02deb90e71acaa054fa6267362f26cbb467deea63778aeebdfe98985ec28b7dd`。全局指向独立 IDEA HOME 下的 `.agents/skills/doc-view`。

使用隔离 IDEA 自带的代理设置，将官方 GitHub HTTPS 流量接入仅监听回环的测试代理。TLS 正常端到端校验。代理只观察目标主机和 TLS 记录计数，在 release asset 连接已开始传输应用记录后延缓响应。记录的字节数是 TLS 密文计数。

| 场景 | 原生与磁盘结果 |
| --- | --- |
| 全局从 1.7.0 更新，设置取消 | 01:10:05.322Z 进入 asset gate，设置在 01:10:05.736–05.746Z 点击取消并关闭。1,737 字节仍在 gate 后，01:10:05.904Z 客户端 EOF，关闭设置后约 158 ms 连接结束，早于 12 秒下载超时。旧 Skill 和 metadata SHA 逐字保留，项目范围未改变，暂存和 journal 清理。解除限制后实际从官方更新到 1.7.1，UI 显示已安装并被 Codex 发现。 |
| 当前项目首次安装，设置取消 | 01:20:07.802Z 已阻留 1,737 字节。UI 仍显示 Installing，01:20:11.445–11.480Z 关闭设置，01:20:11.638Z 客户端 EOF。目标和 metadata 均未创建，只有之前已有的锁和检查记录，未留下 stage、backup 或 journal；全局已安装文件和 metadata SHA 均未改变。重新打开设置，仍显示可首次安装。 |

首轮全局取消发生在 gate 开启前。另一轮首条资产连接被上游关闭，第二条在原单连接 gate 未接管的情况下提前关闭。两轮均保留为未通过。测试代理改为覆盖在途 TLS 重试后重新运行，取得表中时序。撤掉代理前项目重试有一次官方检查报不可用，未留下目标文件。既有两个范围暂存取消由独立[暂存复验](verification-2026-11-17.md)记录，两范围官方恢复进度及离线标签见[原生安装记录](verification-2026-11-20.md)。

项目关闭时精确命中下载或暂存、子进程释放和重开全组合继续由 7.4 跟踪。真实自然模型对最终包的只读查询另见[客户端工作流](verification-2026-11-20-skill-natural.md)。

原始时序位于 `build/native-search-down-final-20261119/skill-client-acceptance-20261120/proxy/events-retry.jsonl`。两范围逐项哈希与残留检查位于 `build/openspec-closure-20261120/`。


## 直连重试与正常项目关闭

整合包此前已正常以官方来源将当前项目安装至 1.7.1，实际 Codex 发现且哈希正确。本轮为观察首次安装取消而暂移这份成功安装的文件和 metadata。精确取消后的直连重试时，公共 GitHub API 实测 HTTP 403 且 x-ratelimit-remaining=0。保留失败现场后，在关闭隔离项目期间按 SHA 恢复本轮较早成功的安装。这次重开用于复核持久状态，没有将它登记为第二次下载成功。

整合包正常关闭当前项目并回到 IDEA Home。关闭前后使用 JBR jcmd GC.class_histogram，McpAiSettingsPanel 与 CodexSkillInstallService 均从 1 降到 0；关闭后未发现该 PID 的 Codex 子进程或仍存的 probe 目录。该动作开始时项目无在途下载、暂存任务。原始直方图和恢复哈希位于 build/openspec-closure-20261120/。

重开后页面显示 Installed 1.7.1 和官方更新检查不可用，磁盘使用上述恢复版本。隔离实例已正常退出，代理已停止，隔离代理配置与原插件 JAR 已恢复。

## 精确正式基线候选的在途项目关闭

候选 ZIP SHA-256 df269892b40d71bedb0fdd5ba3e4dd608cd25712ef73d5c70cb2148f3818034d，JAR 2e2a1e607f3c1e5593001c09b9a0993790b473d9eaa9615be912257d9bc643c3，宿主 IU 263.7307.200。隔离项目起始无目标和 metadata。第一轮观测器在类准备后异步安装断点，错过了暂存，实际安装成功并保留该次读回。将第一次成功安装移入夹具备份，再在同一 VM 对已加载的类提前安装断点复验。

04:03:49.942937Z 在 SkillInstallTransaction.executeLocked 的 writeStage 后、校验前暂停单个工作线程，磁盘上可见本次 stage。04:04:04.466–04.477Z 使用 macOS 原生 File → Close Project，界面返回 IDEA Home。04:04:12.020835Z 放行，约 4 毫秒后同工作线程在事务内观察到 ProcessCanceledException。目标、metadata、journal、stage、backup 均无残留，原有检查记录和锁保留。项目关闭后的 Full GC 直方图没有 McpAiSettingsPanel、CodexSkillInstallService 或 SkillInstallTransaction，未见该 IDE 的 Codex 子进程。独立的面板 dispose 行断点未绑定，因此生命周期证据使用实际关闭、同应用工作线程取消、磁盘与 GC 读回。

同一宿主重开项目，MCP / AI 设置恢复可安装；明确点击重试后，自官方源安装 1.7.1，设置显示 Codex 已发现。两套隔离 IDEA 已正常退出，64342/6507 闭合，原隔离 JAR 与 MCP 配置已恢复。时间、断点及资源原始记录位于 build/release-1.7.2-20261120/。
