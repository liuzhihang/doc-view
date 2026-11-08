# 验证记录

2026-11-09 最新状态：树 UI 修复包 `03f48336…4205b` 通过 414 tests、构建与两个精确 build 的 Verifier。原生 XML/Dubbo 目录与导航、文档库 XML freshness 和三轮资源复验通过；解除树 UI 展开监听器旧路径后，同一 PID19963 真实禁用插件，GC 插件对象记录归零、目标类消失、无类加载器保留告警。store 1.6 与基线 6.3 完成；此前 partial/retry、复制/写回、HTTP 草稿、导出和 Finder 遮挡证据仍按各包记录。同包 GET/JSON/Form 原生回环发送通过；HTTP L01–L03、Schema/预览/凭据等独立原生门槛仍保留。新发现描述单元格将 LF 改为空格，已在原生写回与自动测试复现，正在修复，尚不升级或发布。 详见[续验记录](verification-2026-11-08-remaining-native.md)。下文保留旧包历史边界。

2026-11-08 派生缓存/异常/UI 续验：同一 d094 候选补齐双项目派生索引缓存隔离和不同设置值读回，HTTP/UI 5.4 完成，累计 35/41；HTTP 拒绝、写入失败、Dumb Mode、源码变更/删除、依赖禁用与恢复重试取得原生证据，Copy/写回/导出、中英窄窗及有限三轮 GC/一次热卸载也已补验。完整矩阵仍保留。另确认 XML-only/实现类注解的 Dubbo 目录发现属于既有缺口，正在补契约、测试和修复，修复后新候选必须重新核对自动门禁与受影响原生路径。普通 IDEA 误开的虚构项目已在单独授权后关闭，未操作 Junie；最近项目记录不宣称字节未变。详见[本轮记录](verification-2026-11-08-remaining-native.md)。未升级版本、打 tag 或发布；下文为相应旧包的历史进度。

2026-11-08 HTTP/缓存/搜索续验：修复完整方法说明尾部不可检索；新包 403 tests、构建和两个精确 build 的 Verifier 通过。原生目录及落盘文档搜索、双项目 DTO 缓存/文件保护、HTTP 取消重试与在途关闭已取得证据；固定模型进程首次/热生成各 7 次，p50/p95 分别为 394.121/419.352 ms 与 137.470/180.368 ms。HTTP/UI 完成 34/41，派生索引缓存双项目隔离、完整 L01–L05 和新包 UI/GC 矩阵仍保留，尚不发布。详见[本轮记录](verification-2026-11-08-http-cache-search.md)。下文保留对应旧包的历史边界。

2026-11-08 后续新包：已修复自身卸载前的平台禁用图标缓存清理，无 Doc View UI 的正式 build 卸载对照通过；目录使用后的 JNI 类加载器保留仍未通过。新增修复不自动继承旧包的完整原生验收结论，剩余弹窗和资源矩阵继续开放，详见[卸载 API 续验](../intellij-263-baseline-upgrade/verification-2026-11-08-unload-api.md)。下段为此前 final-v2 阶段结果。

## 当前状态

2026-11-08 最新辅助功能续验：获准操作隔离实例后完成参数/预览 Pin 开关真实 Finder 往返、设置/导出/上传选择取消，5.8 完成。公开 TooltipDescriptionProvider 补齐三按钮说明，RED 后全量 398 tests、构建及两个精确 build Verifier 通过；最终包英语深色 tooltip、复制、Schema 写回及 Undo 实测通过。中文/浅色/窄窗/键盘与完整资源矩阵仍待验，JNI 热卸载未通过，不发布。精确包、屏幕阅读模式对照与恢复、有限 GC 观察见[纯图标按钮补充记录](verification-2026-11-08-icon-buttons.md)。

2026-11-08 整合包续验：HTTP 原生动作生成正确的 Path/Query/Header 草稿，重复生成保留旧编辑并新建 `_1.http`；Copy/Export、Schema 写回与 Undo、参数及预览跨应用交互和有限资源回归通过。维护者激活隔离实例后，A01–A03 的 GET、JSON POST、表单 POST 由原生 HTTP Client 执行并返回 200，接收端逐项断言通过，生成本身没有发送；部分 Options/目录右键菜单已由桌面截图确认。真实激活 Finder 后参数弹窗 Pin 开启这一遮挡场景通过，其余弹窗矩阵仍缺证据。热卸载仍失败，官方本机分析取得无障碍 JNI 与平台图标缓存保留 ClassLoader 的不同引用链，修复尚未通过。完整菜单、性能与生命周期矩阵也未通过。4.5、4.6、5.4、5.5、5.8、5.9、6.3–6.5 不勾选；不把 full-17 旧包证据算作整合包全量通过。详见[本轮记录](verification-2026-11-08-http-cache-search.md#历史证据边界)。

2026-10-23：规划完成后，用户明确授权 apply，按原生 PoC → 唯一方案 → HTTP 修复/UI 的顺序实施。下方规划检查保留为历史记录，当前实施证据逐项追加；任务勾选不使用“规划完成”代替“实现完成”。

2026-11-07 最新 full-17：补齐七个 Keymap 组的中英文标题，真实 RED 后 targeted 15 tests、全量 70 suites / 362 tests 和两个精确 build 的 Verifier 均通过。原生中英 Keymap、Copy/Export、中文 Schema 写回与 Undo、菜单取消、跨应用草稿/交互和有限三轮资源回归已有同包证据；完整视觉遮挡、`…`/目录右键与全矩阵仍未通过。新增 task 5.10 完成，累计 31/40；5.8–5.9 保留。详情见[full-17 续验](../intellij-263-baseline-upgrade/verification-2026-11-07-full17.md)。full-14/16 的字体、图标与前序原生证据见[同日前序记录](../intellij-263-baseline-upgrade/verification-2026-11-07.md)和[图标清单](icon-audit-2026-11-07.md)，不混用不同包结论。

历史 2026-10-23 续验为下方“续验与 G4 包”：53 suites / 288 tests，全量构建与 IU261 Verifier 通过，29/36 tasks 完成。新增超大请求拒绝已在 G4 原生复验；旧 G3 的 UI/耗时观察按包身份保留，不能合并成 G4 的全量原生验收。HTTP Client 发送仍受许可阻塞，完整取消/关闭、资源及菜单矩阵未完成。

## Apply 基线与范围

- `develop` / `7b2e0a849fe0fe54df6df08792579a361b712428`；初始 tracked 修改仅 `docs/roadmap.md`，未跟踪为本 change 和既有 `.agents/`。453 个 tracked 文件及 6 个 `.agents/` 文件 SHA-256 已冻结于 `/tmp/doc-view-http-20261023/baseline.json`；不改动 `.agents/`，不提交/推送或发布。
- 已读取 repo-local apply 的全部 contextFiles、项目领域指南及关联 active changes；`async-method-doc-preview`/`api-tree-navigation`/`localize-plugin-ui` 为已有实现，`clear-cache-safety` 等剩余原生验收仍由原 change 跟踪。
- 本轮 runtime 范围：HTTP `service/http/`、`CatalogHttpClientAction`、`CustomFileUtils.openHttp`；UI `CatalogOpenAction`、工具窗口附加动作、`plugin.xml` 和中英文 bundle；仅按 contract 必要时抽取共享 PSI。测试新增 H/L/U 和相关既有回归；独立探针限定 `tools/http-client-poc/`；文档限定本 change、roadmap、HTTP 用户指南及 Unreleased。新增文件完整路径会随实现记录。
- 目标 SDK 已核对 `Resources/product-info.json`：缓存 distribution `idea-2026.1-aarch64` 为 IU `261.22158.277`。`/Applications/IntelliJ IDEA.app` 是 262，不用其结果冒充目标版验证。
- 本轮隔离输出为 `/tmp/doc-view-http-20261023/`，原生 profile 为其 `ide/config`、`ide/system`、`ide/plugins`、`ide/log`；普通 profile 与原有 `/tmp/doc-view-native-20261022-all/` 不作本轮写入目标。
- R 基线：按 tasks 的 R 命令显式 `--rerun-tasks`，4 suites / 19 tests，0 failures/errors/skips；14 actionable tasks 全部执行。日志 `/tmp/doc-view-http-20261023/baseline-R.log`，XML 副本 `/tmp/doc-view-http-20261023/baseline-R/`。源码及 golden hash 在上述 baseline.json；未改 golden。
- 旧实现 RED：H 的 4 项真实 PSI → `CustomFileUtils.openHttp` → Scratch 输出全部按目标断言失败（path 未替换、form 类型与位置、header 丢失、query `null`）；U 的工具栏失败，修正“选中已展开 root”的无效夹具后，非方法展开与工具栏 2/2 均失败；L 的真实生产解析在 EDT 断言 1/1 失败。对应日志/XML 保存在 `red-HU`、`red-U-fixed`、`red-L` 同名目录及 `.log`，没有把夹具误通过当成行为已覆盖。

## 规划检查

- 文件与 capability 对应：proposal、design、两份 spec、tasks 和本记录已创建；两项 capability 共 22 个 requirements、56 个 scenarios，每项 requirement 均有场景。
- `openspec validate http-client-generation-and-ui --strict`：通过，无警告。
- `openspec status`：4/4 规划 artifacts 完成；`instructions apply` 返回 `state=ready`、`total=36`、`complete=0`。CLI 规划完成不表示运行时完成。
- 本地链接/锚点：检查本 change 与 roadmap 的 7 份 Markdown、64 个本地链接/锚点，全部可解析；未将远端网页可达性算作本地链接检查。
- 格式与范围：`git diff --check` 通过，并另查未跟踪新文件的行尾空白/末尾换行。tracked diff 只有 `docs/roadmap.md`，新增只有本 change 的 7 份 metadata/文档；既有 `.agents/` 未触碰，无 runtime 文件变化。
- 凭据占位：人工核对样例为虚构内容、host/认证值为变量；新文档中未发现所查 token/private-key 模式。此项是规划内容检查，不是运行时安全或第三方平台验收。

## P1 原生路线决策记录

结论：选择唯一 Java 自生成路线；生成后打开 IDEA HTTP Client，不提供双模式或失败 fallback。具体调用链、SDK 字节码、descriptor、包及日志见 [PoC 证据](poc-evidence.md) 和 [探针 README](../../../tools/http-client-poc/README.md)。

| 项目 | 实际证据/结论 |
| --- | --- |
| 目标 SDK / 测试包身份 | IU 261.22158.277，Java 21；r3 探针 ZIP SHA-256 `1964b24ee5934be96c1a7061f4f9399d48007889f020705d31465a60e4da7ff6` |
| 独立编译/Verifier | r1 缺 description 导致 invalid/0 scheduled；r2 修复 descriptor 后发现 probe 的 deprecated ReadAction；r3 全部修复再验，1/1 Compatible，无 internal/override-only/deprecated，3 条 Experimental 单列。报告 `/tmp/doc-view-http-client-poc-20261023-r3/` |
| API/上下文 | Spring helper → gutter builder → manager；Endpoints Action 依赖 URL_TARGET_INFO，并调用同一 manager。given-file 入口有 Internal 标记，排除 |
| 决定性失败 | manager 公共生成方法 `void`，字节码 64 launch、67 pop Job、68 return。没有完成/取消句柄，无法满足 gate 4；主线已独立读回核对 |
| Spring / Feign 请求矩阵 | 未运行：上游生命周期硬拒绝后的候选不适用；不声称任何语义矩阵通过 |
| 编码、示例/default、required、媒体类型 | 未运行：同上 |
| 不支持输入、取消/关闭、用户编辑保护 | 未运行：同上；这些仍是选定生产路线的必需验收 |
| 原生候选加载 | extracted SDK 启动退出 137；官方 DMG 的 SHA 与代码签名已核实，完整 app 的隔离启动成功，但 Ultimate 模块关闭使 Spring Web/探针未加载。CUA 确认 HTTP Client、Java、Markdown 已启用；未启用试用或修改许可。加载仍未验证，环境限制不作为选路依据 |
| 生产依赖/文案 | 无新增 bundled/API 依赖；检测 HTTP Client plugin 可用性；统一“生成 HTTP 请求” |

2.3–2.5 按已批准的硬拒绝出口完成“候选评估不适用”，实际未运行子项见上表。任务勾选表示候选评估已结束，不表示原生生成验收通过。2.6 的独立 ZIP 编译/Verifier 实际已执行；候选 IDE 加载不因该结果判通过。

## 生产实现与问题修复

- 唯一路线：`CatalogHttpClientAction` → `CustomFileUtils.openHttp` → 项目级 `HttpRequestService` → `HttpRequestBuilder` → 不含 PSI 的文本快照 → 新 `.http` Scratch。旧同步 DocView 构建/HTTP 拼接已移除；没有新增生产依赖、语言、平台基线或运行时 fallback。
- HTTP 绑定使用 Spring wire name/location/default/required；路径、query、form 分组件编码，Header 单独写入，JSON/form 的 Content-Type 校验后合并。认证头使用变量；不支持输入明确拒绝。沿用既有有界 body helper，未修改共享 PSI/Markdown 解析规则或现有 Markdown golden 断言。
- 后台使用可取消任务与 non-blocking read action；只提交所选源码文档到 PSI，不强制保存磁盘。保留方法智能指针，合并同方法在途工作，源/项目/取消/依赖状态在发布前复核；每次完成后重新生成均创建新草稿，不覆盖旧编辑内容。
- 主工具栏保留五个常用入口、两个分隔线；设置/清缓存注册到工具窗口附加菜单，旧 action ID 保留，目录不再出现项目级清缓存。文档动作改名为“打开 Markdown 文档”，HTTP 统一“生成 HTTP 请求”；非方法不再展开目录，继续复用平台图标。

发现问题后均先定位、修复、复验，记录如下：

| 问题 | 证据与修复 | 复验 |
| --- | --- | --- |
| 初期 H 的 String 无法解析、常量 fixture 非法 | fixture 缺 JAVA_21；类注解使用尚不可见的自身常量。修正 fixture 与合法限定常量，未放宽生产 PSI 求值 | H 转绿；已有类型/常量输出保留 |
| 初期 L 的任务队列在 headless 环境同步执行 | 开启测试专用的 `intellij.progress.task.ignoreHeadless` 并在 teardown 恢复；取消测试改用真正外层用户进度，避免把 NBRA 内层取消/重试当成用户取消 | L 后台/read lock/取消/重试断言通过 |
| G1 旧导出测试被前序产物污染 | `testActionPublishesProjectSnapshotAfterBackgroundScan` 留下 `docs/doc-view/index.md`，后续 Dumb Mode 测试误判；仅在该测试的 setup/teardown 清理自己 fixture 的导出目录并刷新 VFS | G2/G3 全部导出测试通过；未改导出生产逻辑 |
| G1 新增 deprecated API | `PluginDescriptor.isEnabled()` 改为公开的 `PluginManagerCore.isLoaded(PluginId)` | G2/G3 Verifier 无该报告 |
| 原生发现 Spring DEFAULT_NONE 被截断，H 曾假绿 | 生产/H fixture 使用 10 字符；PoC/native fixture 使用 15 字符。对照 [Spring 官方 ValueConstants](https://raw.githubusercontent.com/spring-projects/spring-framework/main/spring-web/src/main/java/org/springframework/web/bind/annotation/ValueConstants.java) 校准为完整 16 字符。先改 fixture 取得 RED，再改生产；required/optional/空 default/Header 均断言 | `native-found-red-HL`：H 25 项中 7 项失败；G3 H25 全绿；最终原生 `q={{query_q}}` 正确 |
| 文件/流容器绕过 body 拒绝 | `InputStream[]`、`MultipartFile[]`、两种对应 List 负例确认旧实现返回部分 JSON；数组/集合元素递归校验后拒绝，合法 DTO/标量数组保持支持 | 上述 H RED 包含容器负例；G3 全绿 |
| 发布前依赖变化误报源码变化 | 真实 builder 完成后将 client availability 变 false；`native-found-red-L` 1/1 失败，错误为 stale_source。改为按实际状态区分 indexing/client/stale，用户取消和销毁保持静默 | G3 L14 全绿。写动作复检的极短竞态窗口未单独原生复现 |

`native-found-red-H` 首次运行因新 L fixture 遗漏 latch 声明而编译失败；`native-found-red-HL` 的 L 用例首次因试图阻塞测试自身 EDT 而失败。这两次不算生产 RED；修正 fixture 后 `native-found-red-L` 才取得上述真实原因断言失败。所有日志均保留。

## 首轮自动检查与 G3 包

原始输出根目录：`/tmp/doc-view-http-20261023/`。G 的实际命令为 tasks 中的完整命令，均显式 `--rerun-tasks`。

| 轮次 | 实际结果 |
| --- | --- |
| G1 | 52 suites / 280 tests，1 failure，0 errors/skips；Verifier 1/1 Compatible 但有新增 deprecated API，整体失败。日志 `final-G-1.log`，XML/Verifier 副本 `final-G-1/` |
| G2 | 52 suites / 280 tests，全绿；配置、打包、Verifier 通过，18/18 tasks 实际执行。此包后来发现哨兵问题，不能作为最终验收包 |
| G3（续验前） | **52 suites / 283 tests，0 failures/errors/skips**；配置、打包、Verifier 全部通过，**18/18 tasks 实际执行**，耗时 1m17s。日志 `final-G-3.log`，XML/Verifier/汇总 `final-G-3/`。续验发现大文件只读问题后由 G4 替代 |

G3 中 H 为 25 项，L 为 14 项；U 相关五个 suite 共 18 项（布局 7、缓存 4、action 本地化 1、descriptor 本地化 1、图标 5）。R 的四个 suite/19 项包含于完整回归；上传 mock、Markdown、导出及既有业务测试均包含在 283 项内，真实平台上传仍排除。测试用 HTTP Client availability 注入只为验证生成服务，不能作为插件真实加载或许可可用证据。

该轮 G3 包为 `build/distributions/Doc View-1.4.0.zip`，580966 bytes，SHA-256（当前工作区产物已由 G4 替换）：

```text
bd0455ee1ed9efc8487a3b90a3a6565499b7f36f75f97e311603d35ba656c130
```

包内只有 `Doc View/lib/doc-view-1.4.0.jar`；descriptor ID 为 `com.liuzhihang.doc-view`、版本 1.4.0、since-build 261，依赖仍是 platform/java/markdown/jcef。未包含 PoC、testsupport、H/L/U 测试类或失败候选。生产 HTTP 类只有 Java 生成器、快照、错误与项目服务。包清单、descriptor、hash 及安装逐文件字节对照保存在 `final-package-audit.json` 和 `final-G-3/package-audit.json`。这是 Unreleased 验证包，不代表新的 1.4.0 正式发布或上传。

Verifier 对 IU `261.22158.277` 实际 scheduled/finished 均为 1，结果 Compatible；最终报告无 compatibility/deprecated/internal/override-only 问题。Gradle 自身弃用提示、测试 API 编译警告及测试环境 JCEF 输出单列为日志背景，不当作原生功能通过证据。

## G3 包首轮原生验收矩阵

实际运行官方 IU `261.22158.277` 完整 app，隔离目录为上文 `ide/`；最终包安装后逐文件比对 JAR，启动日志确认为 `Loaded custom plugins: Doc View (1.4.0)`。普通 262 实例没有作为本轮验收目标。

使用独立 `native-project` 虚构 Spring/Feign 工程，未编辑 `doc-demo`。窗口由 CUA 操作；请求动作通过 **Find Action 的 Doc View “Generate HTTP Request”** 触发，没有把它报告成右键菜单已点击。英文、深色、普通布局下观察到五个主入口。`…`/目录菜单尝试后未取得可靠弹出菜单状态，因此菜单可达性仍待验。

原生证据位于 `native-evidence/g3-*.txt`、`g3-ui-toolbar.png`；生成文件位于 `ide/config/extensions/doc-view-http/56f8a0d55b98db32df2a4bb7854894fbf54a70bdea702a0e3bc2a6a15577717a/`。较早 `package-2c307f288189/` 是 G2 包观察，不混入 G3 最终通过结论。所有 `.http` 的文件/hash 清单见 `native-evidence/draft-manifest.json`。

| ID | 自动证据 | 最终包原生结果与未覆盖项 |
| --- | --- | --- |
| A01 | H 路径替换、query UTF-8 编码、Header、名称冲突断言通过 | **生成/打开通过**：`GET {{host}}/api/users/42?q=a%26b%20%E7%A9%BA%E6%A0%BC` + `X-Trace: demo`（get `_3.http`）。HTTP Client 实际发送/接收端对照受许可阻塞 |
| A02 | H JSON/query/header 组合与媒体类型断言通过 | **生成/打开通过**：POST `/api/json?dryRun=true`，单一 JSON Content-Type、X-Trace、含 name/count 的 JSON（json `_2.http`）；实际发送受阻 |
| A03 | H 表单位置/编码与空可选表单通过 | **生成/打开通过**：POST `/api/form`，form-urlencoded、`name=Alice+%26+Bob`，URL 无重复 form（form `_1.http`）；实际发送受阻 |
| A04 | H path/value、限定编译期常量、前缀与尾斜杠通过 | 类 `/api` + 方法路径已见；常量和全部别名组合未逐项原生验收 |
| A05 | H Spring 名/Java 名优先、Parameter.in 冲突与路径负例通过 | wire name 冲突专用样例未原生执行 |
| A06 | H 默认值/哨兵/required/optional、空 default、Header 通过 | **required 代表场景通过**：`q={{query_q}}`（required `_2.http`）。外部修正注解 fixture 后首次仍读到旧 VFS/PSI 值；打开 RequestParam.java 确认完整 16 字符并刷新后复验通过，前一次结果也保留；完整 default/optional 矩阵未原生全验 |
| A07 | H Header 数值、重复、CRLF、Content-Type 合并/拒绝、认证变量通过 | 正常 Header 已见；冲突/认证/CRLF 的可见错误矩阵未原生全验 |
| A08 | H 独立 Feign fixture、静态前缀及拒绝边界通过 | **通过**：Feign `create(boolean)` 生成 `POST {{host}}/remote/items?dryRun={{query_dryrun}}`，不采用注解中的示例域名 |
| A09 | H 多映射/动态/条件/继承等负例通过 | **多路径代表场景通过**：可见 “The endpoint mapping is ambiguous or unsupported.”，未新建 unsupported 草稿；其余负例未原生全验 |
| A10 | H 不支持绑定/媒体类型/多 body/文件流容器通过 | 原生完整拒绝矩阵未运行 |
| A11 | H 无 body 不推导 Content-Type、集合/数组/递归 JSON 通过 | GET、Feign query POST 无额外 Content-Type 已见；大递归 DTO 响应性未测 |
| A12 | L 未保存 Java 文档提交 PSI、磁盘原内容保留通过 | 未在最终包原生编辑源码复验 |
| L01 | U 非方法/Dubbo/无效节点隐藏、无展开副作用通过 | 最终包方法生成可用；类/模块/空/Dubbo 完整交互矩阵未验 |
| L02 | L 同项目重载与稳定身份通过 | 单项目、目录方法与活动 HTTP/注解编辑器不同的代表路径已见；**双项目自动与原生隔离、切换选择仍未验** |
| L03 | L 同方法合并、旧磁盘/未保存 Document 保留及新建通过 | **再次生成/手工编辑保护通过**：form `_1.http` 改为 `name=manual-edit` 后，新建 `_2.http` 保持源码示例，切回旧文件仍为手工内容。原生在途快速点击、未保存状态精确取证仍未全验 |
| L04 | L 取消、源码变化、服务销毁无迟到草稿通过 | 最终包原生取消/关闭项目/源删除矩阵未运行；服务 dispose 测试不冒充真实关闭项目 |
| L05 | L 写入错误清理、依赖缺失/变化、Dumb Mode、异常后重试通过 | 实际客户端已加载但执行受许可限制；原生禁用组件/索引/写入失败未验 |
| L06 | L 后台/read lock、取消和受管任务清理通过 | **冷/热各 7 次、p50/p95、关闭后监听器/任务观测未运行**；不声称性能提升或资源基线已验 |
| U01 | U 五入口/分隔线/附加 action identity 与窗口项目上下文通过 | 五个主入口及布局已见；原生 `…` 设置/清缓存和平台菜单可达性仍未确认 |
| U02 | 本地化/图标和动作状态自动回归通过 | 仅英文深色普通布局；中英切换、浅色、Compact、窄窗、完整键盘矩阵未验 |
| U03 | 单项目 Clear Cache 保留手写 Markdown Scratch 与普通文件通过 | **A/B 项目、HTTP 草稿、导出产物、未保存编辑经 Clear Cache 后的保护仍未验**；不借用 L03 再生成证据 |
| U04 | 既有 Markdown、上传 mock、导出、动作回归通过 | **Enter 打开 json.md 通过**；双击、源码导航、预览/复制/导出及上传菜单取消未在最终包全部复验；不执行真实上传 |

许可阻塞证据：HTTP 编辑器显示 **“Activate IDE license to use HTTP Client”**，Run All Requests 禁用；未启用试用、登录或改动许可。回环接收端只绑定 `127.0.0.1:53041`，接收记录为 0；未用 curl 或其它客户端替代 HTTP Client 冒充 A01–A03 通过。生成过程中未观察到回环请求，但这不构成网络层安全审计。

## 首轮清理、范围与剩余工作

- 已退出本轮隔离 IDEA、停止本轮 loopback 服务并卸载临时官方 DMG 挂载；普通 IDEA 保持原样。没有修改主题/语言或许可，临时 profile 留在证据目录。
- `doc-demo` 与普通 261/262 profile 选定配置共 58 个冻结文件，结束时 hash 全部一致；既有 `.agents/` 6 文件 hash 一致。核对范围见 `native-preserve-baseline.json`、`scope-audit.json`，不扩大为普通 profile 全目录未变化的声明。
- 额外必要文档路径为 `openspec/changes/clear-cache-safety/tasks.md`：只追加新入口待验说明，原有未完成 checkbox 保留。没有修改其它 change 状态、发布记录、版本、依赖、品牌 SVG、共享 PSI 或用户 golden。
- 实现与自动门禁已完成；上述原生/双项目/性能未覆盖使本 change **仍在实施中**，不可 sync/archive 或称为全部验收完成。后续优先：有 HTTP Client 执行许可的目标环境完成 A01–A03 回环接收；补双项目与 Clear Cache 保护；完成菜单/语言/布局及取消/性能原生矩阵。正式 MCP 仍属于另一个 change。
- 未提交、推送、发布或更新线上状态；所有较早失败、fixture 失误、未运行和许可阻塞均保留，未以自动绿灯替代原生验收。

首轮 S：`openspec validate http-client-generation-and-ui --strict` 通过；当时两项 capability 为 22 requirements / 59 scenarios。14 份本轮 Markdown 的 102 个本地链接/锚点可解析；`git diff --check` 及新文件行尾空白/末尾换行检查通过。literal manifest 共 50 个修改/新增路径（排除既有 `.agents/`），保存在 `static-S.json`；当时 28/36 tasks 完成。

## 续验与 G4 包（2026-10-23）

用户要求继续验证后，新增证据放在 `/tmp/doc-view-http-20261023-followup/`（下称续验目录）。续验前冻结工作区 492 个文件，见 `continuation-baseline.json`；保留已有实现和 `.agents/`，不推进 MCP、不提交/推送或发布。

### 新增自动验证与失败修复

| 检查 | 实际覆盖及结果 |
| --- | --- |
| 两项目身份、编辑器/目录上下文 | `HttpRequestProjectIsolationTest` 2 项：真实项目 A 完整关闭后再建 B，模块名、源码相对路径、类名和签名相同，但 Scratch 项目目录不同，各自编辑器打开所属草稿；另一项用真实 action 启动后改变树选择，证明原捕获方法不会被活动编辑器或后续选择替换。**不是两项目同时打开的 HTTP 路由测试** |
| A/B Clear Cache | `ClearCacheActionTest` 新增 1 项：同时打开两个真实 TestProjectManager 项目，对 A 执行 WindowClearAction 后，B 同键缓存保留；A 的手写 Markdown/HTTP Scratch、未保存 HTTP Document、打开编辑器及实际导出的 index/接口页逐字不变 |
| 初期 fixture 失败 | 第一次 helper 模块重命名漏处理 checked exception，编译失败、0 tests；第二次 7 tests / 3 failures：A 的 Light fixture basePath 已被上一用例清理、两个同时 Heavy fixture 的全局 EditorListenerTracker 互相报漏。修复真实目录及 fixture 生命周期后，`targeted-project-tests-3.log` 的 7 项全绿；这些是测试夹具问题，不算生产 RED |
| 超大请求 RED | 原生 G3 25,488,492-byte 请求进入只读 LargeFileEditor。先补 contract，再加入 L 的 UTF-8 和来源注释边界测试；`oversize-red.log` 的 2/2 都因不该创建的 Scratch 实际创建而失败，是真实生产 RED，XML 保存在 `oversize-red-results/` |
| 超大请求 GREEN | 在后台形成完整文本后、Scratch 创建前调用平台内容加载 gate，按 UTF-8 字节数拒绝；新增专属中英原因，保留旧草稿及重试能力。`oversize-green.log`：5 suites / 25 tests 全绿（L16、项目2、缓存5、本地化2）；覆盖中文字符数小但字节数超限、来源注释推过边界、旧未保存内容不变，以及超过洞察限额仍可编辑的请求 |
| 最终 G4 | `./gradlew test verifyPluginProjectConfiguration buildPlugin verifyPlugin --rerun-tasks`：**53 suites / 288 tests，0 failures/errors/skips，18/18 Gradle tasks 实际执行，1m20s**。Verifier 在 IU 261.22158.277 实际完成 1/1，Compatible，无新增 compatibility/internal/deprecated/override-only 报告。日志 `final-G-4.log`，XML/Verifier/summary 在 `final-G-4/` |

平台 API 依据：`SingleRootFileViewProvider.isTooLargeForContentLoading(VirtualFile, Long)` 在目标 SDK 为公开静态方法，类与方法均无 Internal/Deprecated 标记；按 `.http` 扩展名和传入字节数检查完整内容加载上限，严格大于才拒绝。LargeFileEditor 对实际文件使用对应单参数方法。未直接使用 Internal 的 `FileSizeLimit` 或弃用的 `FileUtilRt` 限额；G4 Verifier 已检查实际调用。代码洞察限额与完整内容加载限额不同。

该门禁发生在请求文本构造后，UTF-8 编码还会分配字节数组；**不代表任意规模 DTO 展开的峰值内存已受控**。现有递归深度保护也不等于字段总量上限。G4 的测试环境 JCEF/Gradle 弃用和测试 API 编译警告继续单列，不当成原生功能通过。

### G3 补充 UI 与耗时观察

这一组仍使用 SHA-256 `bd0455ee1ed9efc8487a3b90a3a6565499b7f36f75f97e311603d35ba656c130` 的 G3 包，实际官方 IU 261.22158.277；安装 JAR 已逐字节核对。

- 英文浅色、Compact、约 314 px 窄工具窗口中五个入口可见；切为中文并重启后，刷新/导出/上传/展开/折叠显示正确，双击 get 方法打开内容正确的 Markdown。证据为续验目录 `ui-light-compact*`、`ui-zh-light-compact*`、`ui-zh-doubleclick-markdown.txt`。
- `…` 与右键菜单尝试未取得可靠弹出状态；源码导航尝试也未确认跳到 Java 文件。不因点击过按钮就勾选菜单可达性或源码导航。预览/复制/导出、上传菜单取消、完整键盘矩阵仍未收口。
- 固定虚构模型为 3 分支、6 层边、每层 8 标量字段，实际每个 JSON 有 1,093 个对象，完整请求 440,857 bytes。7 个同构不同类首次生成 + 同一个类重复生成 7 次，14 个文件都完整 JSON 解析成功；原始样本、文件/hash 和对象数在 `observations/l06-raw.json`、`l06-verified.json`。

| 条件 | 7 次原始样本（ms，按执行顺序） | p50 / p95（ms） |
| --- | --- | --- |
| 不同类型首次使用 | 789.4、869.2、912.4、848.3、830.5、1492.1、1284.6 | 869.2 / 1492.1 |
| 同类型热重复 | 775.9、1315.2、1154.8、723.3、1536.4、1235.5、1037.9 | 1154.8 / 1536.4 |

起点为 CUA 按下 Find Action Return 前，终点为 AX 确认新的正确 HTTP 编辑器；含 CUA/窗口观察开销，p95 使用 nearest-rank。HTTP 路径不经过 DtoSchemaCache，所以清缓存不能制造 HTTP 冷缓存。这些样本**不是进程冷启动，也不证明性能提升**；不替代 L06 的完整冷/热及资源验收，也不自动归入 G4。

取消尝试中出现任务进度，但没有可靠证据证明点击在发布前送达；草稿最终仍创建，因此不计原生取消通过，也不足以单独定性取消实现失效。忙时关闭项目的尝试被新草稿切换打断；随后只确认了空闲项目关闭和线程栈中无 HTTP 服务/解析栈采样，不能代替关闭在途任务、监听器释放或 GC 证据。相关 `observations/cancel-*`、`close-*`、`threads-after-idle-project-close.txt` 均保留。

### G4 包身份与原生复验

当前产物 `build/distributions/Doc View-1.4.0.zip` 为 **581415 bytes**，SHA-256：

```text
68852145f231627e93a3716d2fb47d6e96236dac2b634441c50872f72db510eb
```

`final-G-4/package-audit.json` 保存 ZIP/JAR/descriptor 清单：仍只有 `Doc View/lib/doc-view-1.4.0.jar`，ID/version/since-build/dependencies 不变，没有 PoC/test 类或第二种生产生成路线。包已安装到同一隔离 profile 并逐字节对照；启动日志确认为 IU 261.22158.277 / Doc View 1.4.0。本包为 Unreleased 验证产物，不改变正式 1.4.0 发布状态。

| 场景 | G4 原生观察 |
| --- | --- |
| 超大请求拒绝 | Reload All from Disk 后在编辑器确认 CancelNode 的第 5 分支；同一会产生 25,488,492-byte 请求的输入得到明确英文超限通知，仍停留源码编辑器，37 份既有 HTTP 文件 hash 均不变，没有新草稿或只读大文件。证据 `native-source.txt`、`native-size-rejected.txt/png` |
| 普通请求与重试 | 随后 get 正常生成 `_4.http`：正确 path、UTF-8 query 和 X-Trace，HTTP Client 编辑器已打开。实际发送仍显示许可提示、Run All 禁用。证据 `native-get.txt` |
| 大但可编辑的请求 | 将压力 fixture 恢复 4 分支并 Reload 后，生成 5,658,147-byte `_9.http`；仅显示 2.56 MB 洞察阈值提示，仍为 HTTP Client 编辑器。实际插入并保存注释成功，保存后 5,658,176 bytes，JSON 仍完整解析。证据 `native-editable-large*.txt/png` |
| Clear Cache 单项目代表场景 | 通过 Find Action 选中描述为当前项目 Doc View schema cache 的动作执行；41 份 Markdown/HTTP 文件 hash 全部保留，当前编辑器仍打开。本次已保存注释，不声称原生未保存保护或 A/B 隔离已验，也不替代新 `…` 菜单可达性。证据 `native-clear*.json/txt` |

G4 只新增上述 get 与可编辑大请求两份文件；旧 37 份无改动，见 `native-draft-delta.json`。G4 原生运行日志无 `ERROR` 级记录，不能因此推导全部场景通过。其余 A/L/U 的原生结果沿用各包各自记录，不将 G3 观察汇总成 G4 的完整验收。

### 本轮清理与剩余项

两轮隔离实例均已退出，临时主题/语言等配置 XML 已按各自启动前备份恢复，压力 fixture 恢复为 4 分支；新配置文件备份后移除。官方 DMG 已卸载，本轮没有启动回环服务。结束时 `doc-demo`/普通 261/262 profile 的 58 个冻结文件及既有 `.agents/` 6 文件 hash 一致；详见 `final-G-4/cleanup.json`，不扩大为普通 profile 全目录证明。

4.2 的自动用例已达到任务列出的两项目分别生成、重载、选择与焦点隔离标准，因此完成 **29/36**。同时双项目原生路由仍归 6.3；5.4 的同时 A/B 自动保护虽已通过，新窗口菜单原生场景仍待验。4.5、4.6、5.4、5.5、6.3–6.5 保持未勾选：需要可执行 HTTP Client 的目标许可环境，以及完整菜单/键盘/原生取消和关闭、冷启动/资源矩阵。未启动正式 MCP 开发，未 sync/archive、提交、推送或发布。

续验最终 S：严格 OpenSpec 校验通过，`instructions apply` 读回 36 total / 29 complete / 7 remaining；当前为 22 requirements / 60 scenarios。14 份 Markdown 的 102 个本地链接/锚点可解析，差异及新文件空白检查通过；literal manifest 共 52 条路径（含先前实现，排除既有 `.agents/`），续验实际修改 13 个既有路径、新增 2 个测试文件。结果保存在续验目录 `static-S.json`、`final-apply.json`。OpenSpec status 的规划完成标志不表示运行时验收全部完成。


## 2026-10-23 后续原生检查

使用同一 G4 ZIP（SHA-256 `68852145f231627e93a3716d2fb47d6e96236dac2b634441c50872f72db510eb`）重新启动已有隔离 HTTP profile。原生 Markdown source/preview 同时显示 `/api/json`、POST、Content-Type、X-Trace、dryRun 及 Body 字段；截图保存在 `/tmp/doc-view-next-20261023/http-g4-markdown-preview.png`。多次通过 AX 和实际坐标操作工具窗口 Options，未取得弹出菜单的可靠原生状态，因此没有将菜单项可达性勾选通过，也没有据此认定代码注册错误。独立静态复核仍确认 `setAdditionalGearActions` 与目录 `PopupHandler` 注册链路完整。

IU 261 的真实取消动作 ID 为 `StopBackgroundProcesses`，标签为 `Stop Background Processes…`，Mac keymap 继承的快捷键为 Ctrl+Shift+F2；它可列出当前项目可取消后台任务，但本轮没有实际取消成功证据。原生菜单、取消与关闭等未验项继续保留，任务仍为 **29/36**。本轮未修改 HTTP 生产代码或补跑无关 Gradle 检查。

该 HTTP 隔离实例已正常退出；5 个发生变化的已有 profile XML 按启动前内容恢复，记录为 `http-profile-restore.json`。随后使用另一独立 profile 推进 MCP PoC，MCP 工具发现不作为 HTTP/UI 验收证据。
