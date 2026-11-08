# HTTP 取消/性能、双项目缓存与目录搜索续验（2026-11-08）

本轮在独立整合树 `/Users/liuzhihang/workspace/doc-view-release-integration-20261107` 实施和验证。原工作区未改，Junie/Air 保留；未升级公开版本、提交、推送、打 tag、发布 GitHub Release 或上传 Marketplace。发布仍以全部必需门槛通过为条件。

## 候选身份与自动门禁

- 生产唯一修改：`MethodNode` 在构造时保存完整方法说明，Speed Search 使用完整说明快照；按键路径不增加 PSI 读取。保留原有 15 字符业务副标题、Markdown 命名与 HTTP 身份规则。
- 新增真实 `StructureTreeModel` 节点包装及平台搜索函数的回归断言：长描述末尾可检索，显示名称及文件身份不变。初始针对性 RED 为 9 tests / 1 failure；修复后由下述串行全量运行最终确认。
- 冻结 ZIP SHA-256：`50a956be5ee2534be9a2f1cf14fdc8766dc5b4b66c208f3027f2d0ef5928d94e`。
- 冻结/实际安装 JAR SHA-256：`d094f2b7c1d7bf7a706ea1844036595db6a64e933a9b373fbbafc1e7692273f0`。
- 340 个生产 class，全部 major 69；本地验证版本仍为 1.5.0，不能覆盖已经发布的 1.5.0。
- 串行 `test verifyPluginProjectConfiguration buildPlugin verifyPlugin --rerun-tasks --no-build-cache`：77 suites / **403 tests / 0 failures / 0 errors / 0 skipped**，18 tasks 实际执行，BUILD SUCCESSFUL。
- 最低 IU `263.7031.23` 和原生使用的正式 IU `263.7307.200` 两份 Verifier 均为 Compatible；保留 16 项 Experimental API，未出现 Internal、Deprecated 或 compatibility problem。已有测试源码与 Gradle 10 迁移提示不隐去。

首轮全量验证与子代理测试意外重叠，结果目录出现 `in-progress-results-generic.bin` 缺失及索引重建错误。该轮证据作废保留，停止重叠进程后由主线程串行重跑；不能把前一轮日志中的局部成功当作最终通过。此后没有并行 Gradle 测试。

原始资料统一位于 ignored `build/native-validation/2026-11-08/http-cache-search/`：`search-candidate/`、`search-full-gates-serial.log`、`release-verifier/`、`concurrent-run-invalid/`；图像另在相邻 `native-ui-matrix/`。各包边界接续[前一轮记录](verification-2026-11-08-keyboard-resources.md)。

## 原生环境和观察器边界

实际平台 IU `263.7307.200` / JBR `25.0.4.1+1-b635.79`。使用隔离 IDEA Integration Test 和 `unload-clean` profile，仅打开两个虚构项目：既有 `fictional-project` 与新增负载样例 `http-cache-search/fixture-a`。沿用用户已授权的 macOS 辅助功能和测试窗口截图；没有操作普通 IDEA、账号、许可、Junie 对话或外部上传。

临时 Java 观察器只安装在该隔离 profile，不进入生产 ZIP，不修改生产 JAR。观察器 ZIP SHA `2ab2e77551613b95e8b61187eda8de992cf51bc479260667d7136a971719c16e`，记录公开 action/file editor 回调、DTO cache 同键种子、文档哈希及 100 ms Swing Timer。HTTP action 和 fileOpened 时间在读取/计算文档 hash 前获取；结果包含观察器存在时的开销。其项目 allowlist 为上述两个精确目录，不记录文档正文。

观察器会保留种子对应 Project 引用和自身 Timer。因此观察器存在时不验证插件热卸载，不把下面的 HTTP 请求实例回收当作整个项目或类加载器 GC 通过。收尾时正常退出隔离 IDEA，移除观察器并保留其构建和原始记录。

## 目录和文档库搜索

在当前候选包的原生目录中：

1. `Cmd+F` 打开平台 Speed Search，向真实搜索控件输入 `Nativepageitem23`，选中 `pageEndpoint23`。Escape 关闭搜索后 Enter 打开 Markdown，内容含完整描述 `Nativepageitem23.` 与路径 `/pagination/item-23`。副标题和 Scratch 文件名仍为原有截断名称 `Nativepageitem2`。
2. 搜索完整签名 `nativeForm(java.lang.String, java.lang.String)`，通过目录菜单 Jump to Source 跳到 `HttpAcceptanceController.java` 的 `nativeForm`。
3. 从 Export 的 Search Project Docs 进入落盘文档库，搜索 `Nativepageitem23` 得到唯一结果，Open Markdown 打开 `PaginationController-pageEndpoint23-…md`。状态保留 **Freshness is unknown · 57 API(s)**，没有把磁盘快照冒充实时源码。

搜索内容通过 macOS 辅助功能写入可见搜索控件；菜单、Enter 和源码跳转使用真实原生动作。此结果不声称中文输入法逐键组合输入已经验证。此前直接打字出现输入法候选窗的尝试，以及把焦点误留在编辑器的输入均不计为通过；误输入已 Undo，15 个既有 fixture 源文件 hash 均恢复。

证据：`search-full-description-native.json`、`search-description-enter.json`、`search-signature-native.json`、`search-signature-source.json`、`library-search-confirmed.json`、`library-search-opened.json` 及同名截图。原生辅助功能有时返回空目录行名；后续使用已核对目录结构的键盘选择并检查树焦点，没有按空标签推断选择成功。测试 helper 另增加隐藏控件无屏幕位置的拒绝条件；这些是取证工具修正，不是生产代码变化。

## 双项目 Clear Cache

两个项目在同一 IDEA 进程同时打开。通过 Doc View 原生 `… → Clear Cache` 触发生产 action，而非观察器直接调用 clear。观察器仅负责在动作前后读回：

| 场景 | 实际结果 |
| --- | --- |
| A=`fixture-a` 清理 | 同逻辑 key 的 A DTO cache 条目消失，B 条目和 marker 保留，两个 cache epoch 不变；`CLEAR_CONFIRMED_TARGET_ONLY` |
| B=`fictional-project` 清理 | B 条目消失，A 条目和 marker 保留，两个 epoch 不变；`CLEAR_CONFIRMED_TARGET_ONLY` |
| 磁盘保护 | A 轮 132 个、B 轮 133 个观测文件清理前后 SHA-256 完全相同，包括 HTTP 草稿、Doc View 手写 Markdown 和落盘导出文档 |
| 未保存编辑 | B 清理时 A 的 HTTP 与 B 的 Markdown 同时 `isFileModified=true`；清理后打开状态、文本 hash/长度及未保存状态均保持 |
| 设置窗口项目归属 | A、B 的 `… → Settings` 分别打开 `Settings – fixture-a` 与 `Settings – fictional-project`，随后 Cancel；未保存设置改动 |

首次尝试在缓存种子建立之后编辑 HTTP，导致 A 的 PSI epoch 在点击 clear 前已经变化；观察器正确记录为 `INCONCLUSIVE_OR_UNEXPECTED`。这些记录不计为缓存隔离通过。随后在编辑完成后重新建立种子，取得上述两个有效结果；未删除先前尝试。

直接观察的是 `DtoSchemaCacheService`。`WindowClearAction` 还调用所属项目的 `ProjectDocumentIndexService.clearDerivedCache()`；本轮没有直接观测两项目派生搜索缓存的清除/保留身份。落盘搜索功能与文件保护通过，不等于该内部缓存隔离也已原生证明。不同项目设置值的独立保存/读回也未在本轮执行。因此 **5.4 仍部分完成**，不凭上述结果勾选旧 clear-cache change 的过时入口矩阵。

证据：`probe/output/cache-observation.log`、`clear-a-confirmed-disk-before/after.json`、`clear-b-disk-before/after.json`、`clear-a-unsaved-after.json`、`clear-b-after.json`、`settings-a-project-context.json`、`settings-b-project-context.json`。

## HTTP 取消、重试与在途关闭

取消负载临时扩为 5 分支、7 条嵌套边、97,656 个 JSON 对象，仅用于让真实解析任务可观察；没有生产延迟或替代生成器。该变体完成时可能超过可编辑文件上限，不将它作为成功生成样本。随后恢复 5 分支/6 边基线。

- 从原生目录触发 Generate HTTP Request。实际 IDE Find Action 显示停止快捷键为 **Shift+Command+F2**；使用它打开 Stop Background Processes，并点击可见的 `Generating HTTP request`。观察器确认该快捷键和正式 Stop action 被触发；任务随后消失。
- 取消前、取消后及静置后，13 个 HTTP 文件的集合和 hash 不变；没有迟到草稿。恢复正常大小后重试，恰好新增一个 **7,929,881-byte** HTTP 请求，JSON 完整有效，旧文件保持。
- 在途关闭的第一次尝试因外部 fixture 修改尚未进入 IDE 的 PSI，仍生成了原有大小的 `_4.http`，关闭发生在完成之后。该次不算在途关闭通过，不误报为插件取消失败。
- 复验先通过 IDE Reload All from Disk，并在原生 `CancelNode6.java` 编辑器读回 `branch05`，确认扩大模型已经载入。生成后打开 Close Project 时仍有同一个 HTTP 任务的三个 AX 进度呈现；随后关闭 A 项目，A 窗口消失。前后 21 个 HTTP 文件 hash 相同。
- 从该次生成起约 **62 秒**的静置/采样后，文件集合和 hash 仍一致；`Thread.print` 中无 HTTP service 生成栈。live `GC.class_histogram` 未见 `HttpRequestService`、`Request`、`Draft`、其匿名后台 Task 或 `FileEditorOpenSupport` 的活实例；仍有若干 8-byte 无捕获函数对象，不将它们解释为在途请求。
- 当前 HTTP 路径没有自行注册长期监听器；后台任务的 `onFinished` 清理 active/latestOpen，编辑器回调使用受项目状态保护的 future。结合可见终态、无迟到文件、线程及有限实例检查，完成本次固定场景的 HTTP 任务/回调收尾观察；不推导整个插件、JCEF 或任意 future 的长期资源稳定。

证据：`cancel-new-stop-list.json`、`cancel-new-before/after/settled.json`、`cancel-retry-result.json`、失败的 `close-inflight-*`、有效的 `close-verified-*`、`close-settled-result.json`、`close-thread-dump.txt`、`close-live-histogram-filtered.txt`。

早期误用 Control+Shift+F2，以及任务已完成后才尝试停止的记录均保留为无效尝试。原生源码删除/变更、Dumb Mode、依赖不可用及写入失败等完整 L01–L05 组合仍未覆盖，**4.5 不勾选**。

## 固定模型性能

同一个 `hot` 方法、3 分支/6 条嵌套边、每对象 8 个 String 字段，1,093 个对象；输出 **433,173 bytes**，各有效样本生成内容 hash 一致。计时从生产 action 的 before 回调到对应 HTTP fileOpened 回调；每次确认打开正确编辑器。正常样本 JSON 均完整有效。

“冷”限定为新 IDEA 进程的首次 HTTP 生成，保留 OS 磁盘缓存和持久索引；不是清空系统缓存的冷启动。热样本为首个成功生成后，同一进程的七次重复生成。所有样本在项目打开后执行，不包含 IDE 启动时间。

| 样本 | 原始时间（ms） | p50（ms） | p95，nearest-rank（ms） |
| --- | --- | --- | --- |
| 进程首次生成，7 次 | 405.320, 372.928, 419.352, 379.683, 394.121, 411.549, 393.041 | 394.121 | 419.352 |
| 热生成，7 次 | 180.368, 161.350, 149.588, 137.470, 123.167, 119.488, 124.150 | 137.470 | 180.368 |

100 ms EDT Timer 的批次间隔与生成区间重叠部分，最大观测间隔 **196 ms**。间隔由批次毫秒数据重建，并非精确输入延迟或完整 EDT profiler；计时也包含观察器存在时的平台开销。不据此宣称性能提升、OS cold 行为或其他项目延迟保证。

两个采样启动尝试因 AX 行标签/窗口路径变化而被 guard 拒绝，未开始生产 HTTP action；改用重新解析窗口和受焦点检查的键盘选择后重新启动采样，未将失败尝试混为有效样本。七次有效冷样本 PID 为 71988、73392、73803、73970、74150、74311、74653。

原始样本/计算：`perf-initial-eight.json`、`perf-cold-2.json` 至 `perf-cold-7.json`、`performance-result.json`、观察器原始 JSONL。4.6 所需用户指南、固定冷/热样本及上述取消/关闭后有限 HTTP 任务收尾检查完成；不代替 5.9 的全插件 UI/GC 矩阵。

## 状态与后续门槛

HTTP/UI **34/41**，本轮新增完成 4.6。其余未完成任务保持未勾选：

- 4.5：完整 L01–L05 原生异常/失效/依赖等矩阵；本轮取消、重试、在途关闭只覆盖其中一部分。
- 5.4：DTO cache/文件保护及设置窗口路由已观察；派生搜索缓存双项目隔离、不同设置值读回尚未覆盖。
- 5.5、5.9、6.3：新包搜索与部分菜单通过，不把前包的所有语言/主题/窄窗/复制写回及卸载结果整体转记到本 ZIP；完整 A/L/U 与 UI/GC 门槛仍未完成。
- 6.4、6.5：本轮现场清理和文档同步不等于整个 change 最终收口。正式 MCP 生产工具仍待前置验收。

原工作区的 584 个冻结文件与 HEAD、原 fixture 的 15 个源文件，以及负载 fixture 的 77 个冻结输入均核对未漂移；临时取消变体已恢复。原生检查没有真实上传或 HTTP 发送；本轮未使用许可激活 profile，旧 A01–A03 回环发送证据仍按原包保留。

## 历史证据边界

若干旧页面曾链接 `intellij-263-baseline-upgrade/verification-2026-11-08-integration-native.md`，当前整合树不存在该正文。本轮修正导航至本节，明确缺失，不伪造原报告。

已存在的阶段摘要保留在[发布前核对](../../../docs/release-preflight-2026-11-07.md)；后续可追溯原生记录为[图标按钮](verification-2026-11-08-icon-buttons.md)及[键盘/资源](verification-2026-11-08-keyboard-resources.md)。本机 `build/native-validation/2026-11-08/desktop-followup/http-execution-result.json` 仍保有旧包 A01–A03 的原生 HTTP Client、三种接收值和 passed 结果；这不是本 search candidate 的发送复验，也不能恢复缺失报告的其他全部细节。

收尾校验：三个受影响 change 的 strict validate、11 份修改 Markdown 的 134 个本地链接与 `git diff --check` 通过。更新 CHANGELOG 后再次执行 buildPlugin，ZIP SHA 与冻结候选一致；此打包检查不冒充再次执行全量测试。现场保存至 `preservation-final.json`、`cleanup.json`；584 个原工作区文件、15 个原 fixture 源文件和 77 个负载输入无漂移，Doc View 保持启用，Junie/Air 未加入禁用列表，临时观察器已移出 profile，隔离进程正常退出。
