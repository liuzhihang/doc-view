# 2026-11-14 设置交互、取消与生命周期续验

状态：本轮修复及下列验证完成，聚合验收仍有缺口，候选 `1.7.2-SNAPSHOT` 未发布。Marketplace 1.7.1 已核对上架；本文件中的新行为不属于已发布的 1.7.1。

## 范围与隔离

在隔离工作区 `doc-view-release-integration-20261107` 的 `codex/mcp-ui-lifecycle-20261114` 分支实施，起点 `6514a9eff12b395119be53edf80c37b5a7bf12a4`。原 `doc-view` 工作区未作为编辑或原生测试目标，Junie 保留。原工作区开始时记录 HEAD、porcelain 和 584 个文件摘要，结束后单独回读比较。

按照本轮截图，设置打开和范围/程序选择后自动后台检查；操作区按状态互斥显示安装、更新、恢复或检查。新增 MCP 设置快捷入口，工具窗口生成按钮缩短为 Generate/生成并保留完整提示和可访问名称。没有改变 MCP 工具 contract、Skill 内容、磁盘索引格式、默认注册范围或生产平台基线。

## Marketplace 回读

2026-11-14 官方公开 `/api/plugins/15305/updates?size=10` 列出 1.7.1 update `1219746`，`approve=true`、`listed=true`、`hidden=false`，最低 build `263.7031.23`。记录为 `build/mcp-ui-lifecycle-2026-11-14/marketplace-public.json`。这证明已审核并公开，不是仅提交成功；本轮没有重新上传或重建 1.7.1。

## 已定位问题与修复

1. 恢复 OLD_BACKED_UP 事务成功后，在第二次 CLI 检查期间取消，原路径未形成准确终态。新增受控回归先失败，再修复为 `RESTORED_CANCELLED`：`changedFiles=true`、`committed=false`，保留恢复旧版/metadata 的字节，清理 journal/stage/backup；状态仅在取消终态返回时发布。service 10 项定向通过。它是受控服务测试，不等于原生点击命中特定时点。
2. 原 UI 始终显示检查和禁用安装，与截图要求不符；两个 UI 回归先失败，补齐互斥操作与 MCP 快捷入口。自动检查/范围切换/重绘不重复触发、检查不安装/下载、页面 dispose 取消并丢弃迟到结果合计 16 项定向通过。第一轮一项旧断言按“不自动检查”的行为失败；修正测试为受控源，复跑通过，测试不访问真实个人 Skill 或 GitHub。

3. CLI 的 version/discover 取消原先只结束主进程；两个冻结基线测试均失败，分别确认本次记录的 child PID 未退出。修复统一清理自己启动的进程及已观察到的 descendants，有界等待最多 1 秒后清理临时 HOME/CODEX_HOME；5 项 probe 测试复验通过。两个新增测试逐个等待原 shell/child PID 退出，并检查临时目录删除，不依赖事后泛扫描。初次编排与源码撤回交叉且 fixture 缺少 Application 导致失败，不作为产品 RED；稳定夹具改为无 App indicator 与原子 marker 后才取得上述 RED。

## 公开 API 核对

IU `263.7031.23` 和 `263.7307.200` 都注册 `com.intellij.mcpserver.settings`，父分组分别为 Tools 与 AI。因此按 stable ID 导航，使用公开 `Settings.KEY/find/select` 与 `ShowSettingsUtil` predicate overload。两个精确 SDK 编译探针通过，所用类/方法未发现 Internal、Deprecated 或 Experimental 标记；插件代码不引用 MCP 内部 configurable 类或 SettingsEditor。

## 首轮完整包与原生结果

首轮 ZIP SHA-256 `3ffbc38ac7a3a359ec81bce4613dc2bd12f97708b1b6ec0a7231bf46d5641643`，JAR `9ff7610053fb201621ebfc597dceb9f4c97772393bbcd4555af17b328133b9d8`。560 项测试 0 失败/错误/跳过，100 个 suite；配置检查与构建通过。两精确 build Verifier 均 Compatible，各保留 16 项既有 Experimental，无 Internal/Deprecated/Override-only/兼容性错误。首轮测试编译仍有 18 条 test-only removal warnings 和 Gradle 10 弃用提示，不宣称零警告。

原生使用 IU263.7307.200、新建合成项目 `build/mcp-ui-lifecycle-2026-11-14/native-project`、原有隔离 HOME。项目未安装时自动检查到官方 Skill1.7.1，页面只显示 Install；隔离 Global 已 current1.7.1时只显示 Check for Updates。范围用键盘切换后自动检查。快捷入口经点击与 Space 都在原设置窗口跳到 AI → MCP Server，Enable 保持未勾选，未改客户端配置。显式点击 Install 后项目首次官方安装完成并由真实 Codex 发现，另一范围文件未变。这是候选插件安装正式 Skill 的证据，不冒充已发布1.7.1插件的新装验收。

关闭 Working 状态的设置后无 Skill 隐式安装、无 probe 临时目录；重新打开自动检查正常。随后在自动检查 Working 时 Cmd+Q，IDEA 显示 Background tasks are running / stop them and exit，确认退出后原PID45307结束且无probe临时目录。它是IDE正常退出检查，不等于单独关闭项目时在途MCP generate已通过。

短 Generate 按钮实际生成62接口/126文件，完整可访问名称保留为 Generate Project Docs；原 Export 完整菜单可键盘执行，再次生成报告 No content changes。两次后均观察到 freshness=stale，原因仍在核查，不能把生成内容成功等同于 current 状态通过。原生拖动未改变设置窗口尺寸，所以不把此次尝试当成窄窗验收；自动布局测试覆盖360→5120宽度，中文/浅色/完整键盘矩阵仍保留。

## 原生发现的面板保留与修复

首轮三次打开设置后，即使窗口关闭并执行 live GC，仍有3个 McpAiSettingsPanel；当时无探测临时目录。新增受控回收测试在外部强持有Swing root、操作结束、dispose后不能回收panel（RED）。定位到自有 DocumentListener 和4个 ActionListener 捕获面板，dispose未解绑。修复只解绑自身注册，保留平台浏览选择器监听器与任务取消逻辑；同一回收用例及设置/自动检查合计17项GREEN。该修复释放面板/任务引用，不承诺清空macOS JNI/可访问性对平台Swing组件的缓存，不引入Internal API，也不改变require-restart策略。

## 修复后的最终候选

最终 ZIP SHA-256 `4264fd845da4453b34826dc6dc6908d3a3d3b399f6bb3be5efaa7656145c2ae9`，JAR `f62fa56d196a8c0f1316935b4176868ba22e097e5315340c6eed91cd4c9124df`。408 个 class 均为 major 69。首轮与最终候选的 production/build 输入仅 `McpAiSettingsPanel.java` 不同，首轮证据单独保留，不能混作同一包。

| 检查 | 最终结果与证据（相对 `build/mcp-ui-lifecycle-2026-11-14/`） |
| --- | --- |
| 全量测试、平台配置、构建 | `full-v2.json/log`，123.43秒，`--rerun-tasks --no-build-cache`；561项、100个suite、0失败/错误/跳过，`build-summary.json`与`full-test-results/` |
| 最低 build IU263.7031.23 | `minimum-verifier-v2.json/log`与`minimum-verifier-v2-report/`：Compatible，16项既有Experimental |
| 原生目标 IU263.7307.200 | `release-verifier-v2.json/log`与`release-verifier-v2/`：Compatible，16项既有Experimental |
| 真实 Codex 受控服务链路 | `real-client/real-codex-lifecycle.json`：16项通过；Codex0.167.0-alpha.4.1，两范围、取消/恢复/重试。使用注入的本地不可变源，不能算官方GitHub/原生按钮的完整矩阵 |
| 独立JVM中断恢复 | `real-client/jvm-crash-recovery.json`：两范围各READY/OLD_BACKED_UP/NEW_IN_PLACE，共6项通过；不能替代真实IDE在每个阶段退出 |

两份最终Verifier没有Internal、Deprecated、Override-only或兼容性错误。测试编译的18条removal warnings和Gradle10弃用提示仍保留；未宣称零警告。

最终包原生使用同一隔离profile、合成项目与独立HOME，PID47403，日志实际加载Doc View1.7.2-SNAPSHOT，安装包摘要与上述最终ZIP一致：

- 重启后项目Skill自动检查为current1.7.1，可见操作只有Check for Updates。MCP快捷入口在同一设置窗口打开AI → MCP Server，Enable仍未勾选。Global Skill与安装metadata的3个基线文件字节未变；最近检查时间文件按contract更新，不把它算成Skill修改。
- 三次设置打开/关闭后live GC的`McpAiSettingsPanel`数量均为0，打开时为1。对应`SettingsRootPanel`仍为1/2/3，说明平台仍持有Swing组件；不把面板释放描述为JNI根或平台组件缓存全部释放。证据为`v2-gc-settings-*.txt`和`v2-gc-summary.json`。
- 正常Close Project回到IDEA Home后，`CodexSkillInstallService`与`ProjectDocumentIndexService`的实例数均为0（`v2-gc-project-closed.txt`）；重开同一合成项目后设置检查仍为current1.7.1。该用例没有在途MCP generate，不覆盖其取消。
- 重启后的文档快照显示unknown，符合未在本次会话重新验证输入的边界。通过原Export菜单重新生成62接口，报告No content changes，随后显示Current for this session；下一次读取仍为current。已有快照时空状态短Generate按钮不显示，因此第二轮用保留的完整菜单，而不是把不可见的AX节点当成按钮点击成功。
- 最后一次检查曾显示Working，随后Cmd+Q的确认框只有普通退出提示，没有后台任务提示；不能声称命中精确在途取消时点。正常退出后PID结束、probe临时目录为空。首轮实际出现后台任务退出提示的证据保持独立。
- `native-v2-idea.log`本次启动段没有ERROR；退出时仍有平台WindowStateService和Air编辑器tracker的WARN，不宣称所有平台资源已无告警，也未为此停用Junie/Air。

记录见`native-v2-launch.json`、`native-v2-summary.json`、`native-v2-exit.json`。原工作区HEAD、porcelain及584个基线文件摘要均保持一致，见`original-workspace-after.json`。未操作普通IDEA的项目或Junie，也未重新发布1.7.1或发布1.7.2。

## 仍未通过或未执行

1. **首次生成后的stale现象尚未闭环。** 首轮index有效、62entries且`hasUnsavedDocuments=false`；首次创建此前不存在的`docs/`，日志在同一时段记录127项VFS更新。当前过滤忽略`docs/doc-view`及其后代，但祖先目录`docs/`创建可能触发保守失效。这是有源码依据的推断，缺少具体VFS路径/epoch记录；首轮无变化再次生成仍stale的来源尚不能确定。最终包重启后再次生成已current，不能据此倒推首次路径已修复。后续应在pristine合成项目记录输入事件/epoch后重现，定位后修复并复验；当前没有改动该过滤规则。
2. **精确下载/暂存阶段的原生取消仍未命中。** 服务/事务受控测试和实际Working取消各有证据，不能互相替代；恢复完成后再取消的`RESTORED_CANCELLED`终态为本轮受控RED/GREEN证据。
3. **在途MCP生成时单独关闭项目、全部安装阶段的真实IDE退出/重启、完整资源长期基线仍未覆盖。** 正常关闭项目、设置关闭回收和正常退出结果仅覆盖上述路径。继续采用既定`require-restart=true`，不重开热卸载发布门槛。
4. 新交互的中文/浅色/窄窗与完整键盘原生矩阵、手动换CLI后的自动检查仍待补齐；本轮英文、范围键盘切换、快捷链接点击/Space、自动布局测试不能代替全部组合。设置窗口缩小尝试未生效，明确不算通过。

上述缺口保留在聚合tasks；本轮仅勾选有完整证据的7.3和7.5，不归档change。
