# 1.7.0 最新候选验收与发布准备

2026-11-13，隔离工作区 `doc-view-release-integration-20261107`。版本仍为 `1.7.0-SNAPSHOT`，没有提交、推送、打官方 tag 或发布。旧候选的失败和结果保留，不覆盖为本轮通过。全部机器证据位于 `build/release-readiness-2026-11-13/`。

## 本轮修复

- 检查更新的成功时间写入独立、有界的 `last-check.json`，按当前项目/全局归属分开，不写 Skill 文件、安装 metadata 或 writer lock。仅成功完成官方查询后更新，取消/离线保留旧时间；本地状态写入失败显示 ERROR。
- 设置页以小号说明文字显示最近成功检查时间。原生检查发现“未修改任何文件”与独立状态写盘冲突，修正中英文说明为“未修改 Skill 文件”。
- README、插件介绍、欢迎页同步内置 MCP 与两范围 Skill 候选能力；保留尚未正式发布、精确客户端限制及无兼容发行包时不可安装的说明。

## 最终构建与包身份

`full-r11` 执行 `./gradlew test buildPlugin verifyPluginProjectConfiguration -I tools/doc-view-skill/acceptance.init.gradle --rerun-tasks --no-build-cache --no-parallel --console=plain`，111.05秒，99个suite、531项测试，0失败/错误/跳过。没有用缓存命中冒充重跑。

| 项目 | 结果 |
| --- | --- |
| 插件 ZIP SHA-256 | `2a8adad9610058f1b94466a0b755b7f6515d05e042613dc2d948e1310a37e95e` |
| 插件 JAR SHA-256 | `022228c283be2885500fed38ebede262dab3a8d0c818d9e44328cd96c94b7f51` |
| descriptor | `1.7.0-SNAPSHOT`、since `263.7031.23`、`require-restart=true`、同包可选 MCP descriptor |
| 字节码 | 407个生产class，全部major69；无Test/PoC类 |
| 最低build Verifier | IU263.7031.23，Compatible，68.74秒 |
| 原生目标build Verifier | IU263.7307.200，Compatible，62.84秒 |

两个Verifier均只有16项既有Experimental API使用，没有Internal/Deprecated/Override-only或兼容性错误。最终ZIP与冻结副本逐字节相同，见 `final-package-and-cleanup.json`。本轮没有改动最低平台基线。

## 两范围 Skill 服务回归

最终全量检查再次运行真实 `codex-cli 0.167.0-alpha.4.1` 的两范围安装/发现、更新、受控取消、离线、错误发现回滚、重试和手改冲突，共16项；两范围READY/OLD_BACKED_UP/NEW_IN_PLACE各执行子JVM `Runtime.halt(73)`并由新进程恢复，共6项，全部通过。

证据：`real-client/real-codex-lifecycle.json`、`real-client/jvm-crash-recovery.json`。发行源仍为本地不可变候选的测试注入，调用实际生产安装服务和真实Codex发现；不等于官方GitHub下载或IDEA按钮安装验收。使用临时HOME/CODEX_HOME，未安装到个人目录。

## 最新包原生设置与可选加载

在隔离 IU263.7307.200 安装上述同一ZIP：

- 禁用宿主MCP组件时，Doc View工具窗口、既有62项文档库与搜索入口、MCP/AI设置页正常加载。`jcmd VM.class_hierarchy`显示设置页类已加载、MCP Provider类未加载。宿主Debugger/Coverage的MCP toolset依赖提示与Doc View无关；恢复宿主MCP后它们不再构成此测试的依赖缺失。
- 当前项目真实官方检查成功，但没有兼容Skill资产，Install保持禁用，显示准确说明与15:23的检查时间。全局独立记录为15:17。
- IDEA正常退出/重启后，以隔离IDEA HTTP代理 `127.0.0.1:9` 制造真实网络失败，两范围显示离线并分别保留15:23/15:17，不创建Skill、安装metadata或锁。状态文件分别为 `2026-11-13T07:23:33.784963Z`、`2026-11-13T07:17:53.043472Z`。这是检查状态的应用重启持久化，不是安装journal的IDEA重启恢复。
- 代理已恢复No Proxy/空host/默认port80，MCP禁用列表恢复原值。没有操作普通IDEA或Junie。

## 最新包 MCP 与取消时序

启用隔离MCP后，A/B项目共6次真实 status/search/get通过协议及快照schema检查，tools/list只含4项Doc View工具，两项目254个文档文件hash不变。证据 `native-enabled-r11/protocol/result.json`；这一组使用直接HTTP客户端。

另外使用真实Codex app-server与本地合成模型驱动 `doc_view_generate`，取消前通过同session只读探针证明 `generating=true`。下表的run均位于 `build/mcp-skill-2026-11-12/codex-cancellation/runs/`：

| Run | 结果 |
| --- | --- |
| `20261113T073237Z-65047-620ffae0` | 失败保留：匹配取消通知已到达，紧接着的Codex查询仍为generating=true；127个文档文件不变，后续独立查询已false。不能把客户端中断当服务端完成。 |
| `20261113T073833Z-65291-735cc751` | 复验通过：从发出interrupt起5.703ms观察到清理完成，随后真实Codex同process/thread/session查询false。 |
| `20261113T073911Z-65470-7bf048a3` | 复验通过：首次探针仍true，70.351ms内观察到false；随后真实Codex同session恢复。 |
| `20261113T073859Z-65393-a6919e2c` | harness self-test通过，仅证明合成目标的客户端/测量链路，不作为IDEA结果。 |

根因证据：`DocViewMcpTaskService.Run.cancel`只设置取消原因和indicator；后台工作在检查取消并进入finally后移除active run。这是协作式异步清理，原harness把通知收到与清理完成错误地视为同一时刻。仅修正测试harness：在原session内进行最多5秒的只读状态观测，记录每次耗时/结果，再由真实Codex执行独立恢复调用；不伪造响应，不重连，不修改生产代码来提前清除busy状态。5秒是测试观察上限，不是新增生产SLA。

两次真实复验均协商 `2025-06-18`，取消requestId匹配generate；各自1秒观察期没有迟到完成，127个文档文件hash不变。这仍不覆盖发布部分文件时取消、最终index提交边界、项目关闭，以及自然语言模型遵循Skill的行为。见 `cancellation-review.json`。

## 收尾与未覆盖项

- 隔离MCP已停用、64343端口关闭，IDEA正常退出；原禁用插件列表恢复。普通用户Skill目标和全局安装状态目录仍不存在。
- 原工作区HEAD、Git状态和584文件hash与基线一致。Junie保留，未发送Junie对话。
- 官方稳定Skill资产尚不存在，最终IDEA按钮从官方源安装/更新、真实HTTP取消、安装journal在IDEA重启后的恢复仍待验证。
- 自然模型Skill工作流、`$doc-view`菜单可见性、中文/浅色/完整键盘矩阵和上述MCP发布/项目生命周期边界未覆盖。活动task不因531项测试通过而整体勾选。
- 正式版本、官方commit/tag/Skill manifest和Marketplace尚未创建。本地临时仓库的Skill包不能作为官方来源证明；发布顺序见[1.7.0发布准备](../../../docs/release-1.7.0.md)。
