# IU 263 基线升级验证记录

## 2026-11-09 1.6.0 最终包自动门禁与发布例外

正式1.6.0 ZIP SHA `0f58a4365ac7c026ed588838a34cd71aaf54dfb17b1af7608f24302ba82c4cc4`，JAR SHA `868f87dee6f86727af49b32621c2fc759010eb9b4565d27ba6fdd89ce4abadde`。实际执行全量 `test verifyPluginProjectConfiguration buildPlugin verifyPlugin --rerun-tasks --no-build-cache --no-parallel`：80 suites / 430 tests，0 failures/errors/skips，18 tasks executed；344个生产class均为major69。最低263.7031.23与正式263.7307.200两个Verifier均Compatible，无compatibility/Internal/deprecated/override-only阻断项，保留16项Experimental API及18条test编译警告/Gradle弃用提示。

最终ZIP已读回version1.6.0、since-build263.7031.23、无until-build、require-restart=true、对应CHANGELOG和四份双语摘要。证据在 `build/native-validation/2026-11-08/remaining-native/release-1.6.0-candidate/`。维护者明确允许本次未完成原生场景留待人工与用户反馈，热卸载不再作为本次发布门槛；旧失败原样保留，R2/4.2等未完整验收的tasks仍不勾选。最终包原生及渠道结果以[1.6.0发布记录](../../../docs/release-1.6.0.md)为准。

## 2026-11-09 当前状态：429 功能续验通过，扩展序列热卸载失败

429 候选 ZIP SHA `6c789b81e371656db0cdbfca005c82a4175541bb98ae04106500feece46f8370`；80 suites / 429 tests、Java 25 构建及两个精确 build Verifier 通过。隔离正式 IU 263.7307.200 PID71085 在受控取消、模板失败/恢复、Copy/Export、文档生成/查询取消重试及三轮 HTML 打开/关闭后热卸载失败：`docViewHistogramRows=56`、`targetClassAbsent=false`、`stillReferencedWarning=true`。卸载后参数表单、参数树、预览表单和工具窗口实例均为0，但不能据此宣称类加载器释放。新 heap 已保存，正在离线定位强根；此次运行启用了仅供顺序控制的本机 JDWP，所有 breakpoint helper 已先 resume/detach，不把控制次序结果当作性能或自然时序结果。

证据根目录为 `build/native-validation/2026-11-08/remaining-native/status-cancel-candidate/`：`artifact.json`、`verifier-summary.json`、`resource-summary.json`、`unload-result.json` 及 `diagnostics/after-unload.hprof`。正常退出该隔离进程后，`preservation-after-exit.json` 确认原工作区584个冻结文件与HEAD未变；Junie与普通IDEA保留。HTML状态、取消及Copy/Export详细证据见[续验记录](../http-client-generation-and-ui/verification-2026-11-08-remaining-native.md)。428的项目关闭修复与特定序列卸载成功保留为下节历史证据；R2/4.2仍未完成，未发布、未推进正式MCP。


## 历史：2026-11-09 428 候选项目关闭与文档库 freshness 原生复验

428 候选 ZIP SHA `d377299230eb6ac6ff773f25283664839cd54b4b17c4622ddcee62cb9230efc8`、JAR SHA `a7bcdf905c920a48900fba0680d1b46699e8d8513963b05ddda94e5ef4892b53`；80 suites / 428 tests 全部通过，两个精确 build Verifier 均 Compatible。隔离 IU 263.7307.200 PID 66650 的原生证据在 `build/native-validation/2026-11-08/remaining-native/project-close-candidate/`：未展开的 `fixture-close-unopened` 关闭成功；关闭 `fixture-b` 后 `schema-current-project` 窗口仍在且目录刷新成功；取消应用退出后项目和目录仍保留。取消发生在 `ProjectCloseListener.projectClosing` 之前，不据此声称项目关闭 veto 已验证。已核实的范围是原生 UI/EDT 项目关闭入口，不扩展到任意后台同步 close API。

同一 PID 显式生成后文档库为 Current、65 entries、`hasUnsavedDocuments=false`；重开后 freshness 为 Unknown、65 entries，133 个生成文件 hash 保持，且没有自动触发 source parse。这符合新会话显式生成后重新确认 freshness 的边界，不把 Unknown 写成 Current。三轮 preview/resource 观察均为 `ParamDocEditorForm=0`、`ParamTreeTableView=0`、`DocViewWindowPanel=2`、`PreviewForm=1`、renderer children=0、FD=515；可见 HTML 模式出现在第1/3轮，第2轮为源码模式。三轮 RSS 分别为 4,363,056、4,362,416、4,371,664 KiB；这些有限快照不代表所有面板归零或长期资源稳定。

仅禁用 Doc View 后 `docViewHistogramRows=0`、目标类不存在且没有 still-referenced 告警；卸载日志仍含平台 `FileManagerImpl` 的 possible-invalidation warnings，不描述为整份日志零警告。PID 66650 已正常退出，原工作区冻结的584个文件未变，Junie 保留。原生和自动证据分别见 `unopened-close.json`、`a-close-b-kept-result.json`、`exit-cancel-result.json`、`freshness-generated-result.json`、`freshness-reopened-result.json`、`resource-cycle-summary.json`、`resource-visible-modes.json` 与 `unload-result.json`。

基线 task 6.4 已按上述已复现的 UI/EDT 关闭范围完成；R2 和 4.2 仍待最终整合验收。项目文档库 task 5.4 仍缺原生取消不发布部分结果的验证。429 guard 修正版已有 429 tests 和两个精确 build Verifier 通过，原生复验进行中；其 `ToolWindowFactory` 与文档库生产 entries 和428一致，但428的结果不记作429的整包原生复验。

## 历史：421 候选闭项目根链与修复门槛

421 候选热卸载失败 heap（SHA-256 `985897dd0ec54a064a6cbafd8e60abd31aacc07c57743d80cf5064eeb4fc5cbb`）的强根分析排除了 Class 自环伪根。真实根为 Local JNI 的 ProjectViewRenderer，经平台 Tree/ScrollPaneTracker/ToolWindowManagerImpl.idToEntry，到 Doc View ToolWindow 的 stripeTitleProvider 与插件 ClassLoader。该 manager 的 ProjectImpl `containerState=DISPOSE_COMPLETED`，仍有且仅有一条对应 Doc View 的 entry；内容管理器尚未创建，不能只在面板 dispose 清理。原生旧包根链报告位于 `build/native-validation/2026-11-08/remaining-native/batch-escape-candidate/diagnostics/` 的 `doc-view-classloader-roots-no-class-roots.txt` 和 `toolwindow-entry-audit.txt`。421 阶段另记录 PreviewForm 列表选择时读取 PSI 的 EDT 断言 RED，相关修复及新候选结果见本页当前状态与后续记录。该包热卸载失败保持为该 artifact 的历史失败，不能与428结果混为一包。

2026-11-08 HTTP/缓存/搜索续验：修复完整方法说明尾部不可检索；新包 403 tests、构建和两个精确 build 的 Verifier 通过。原生目录及落盘文档搜索、双项目 DTO 缓存/文件保护、HTTP 取消重试与在途关闭已取得证据；固定模型进程首次/热生成各 7 次，p50/p95 分别为 394.121/419.352 ms 与 137.470/180.368 ms。HTTP/UI 完成 34/41，派生索引缓存双项目隔离、完整 L01–L05 和新包 UI/GC 矩阵仍保留，尚不发布。详见[本轮记录](../http-client-generation-and-ui/verification-2026-11-08-http-cache-search.md)。下文保留对应旧包的历史边界。

2026-11-08 卸载 API 续验：已新增仅在 Doc View 自身 `beforePluginUnload` 调用公开 `IconLoader.clearCache()` 的修复。新包在正式 263 的无 UI 卸载对照中类加载器已释放；打开目录后仍被 macOS 无障碍 Local JNI 引用，官方新堆报告确认热卸载未通过。未找到可供插件释放该 JNI 根的公开 API 或官方已发布修复。剩余弹窗验收继续保留；构建、测试修正和两 build Verifier 的精确进度见[本轮记录](verification-2026-11-08-unload-api.md)。原工作区与 Junie 保留，不发布。下方未清缓存等描述是此前旧包阶段。

2026-11-08：最终整合包完成 Copy/Export、Schema 写回与 Undo、HTTP 草稿、文档分页/冲突保护/重开及有限资源原生回归。包和生产输入未变，复用此前 395 tests 和两个 Verifier；未重新全量运行。原生 HTTP Client 的 GET/JSON POST/表单 POST 三种回环执行通过，部分桌面菜单可见结果已补齐；维护者真实激活 Finder 后参数弹窗 Pin 开启这一场景通过。热卸载仍失败，未登录 profile 的官方分析分别发现无障碍 JNI 和平台图标缓存保留 ClassLoader；无界面对照实例归零但类仍存活，未标成功。完整弹窗/菜单与其余矩阵仍未通过，R2/4.2 保留未完成。原工作区与 Junie 保留，隔离诊断进程已退出，主隔离激活配置保留，无发布动作。详见[新整合包原生记录](../http-client-generation-and-ui/verification-2026-11-08-http-cache-search.md#历史证据边界)。下文保留各时点历史状态。

2026-11-07 后续发布前核对：远端已发布 1.5.0，本轮 263 改动已在独立工作树整合，并修复查询取消检查缺口；395 tests、构建及两个精确 build 的 Verifier 通过，整合包原生验收未运行，R2/4.2 仍未完成。包与发布边界见[发布前核对](../../../docs/release-preflight-2026-11-07.md)。

## 2026-11-07 当前续验

截至 2026-11-07，full-17 已修复七个 Keymap 组的中英文标题，70 suites / 362 tests、构建及 EAP `263.7031.23` / 正式 `263.7307.200` 两个 Verifier 通过，仍有 16 Experimental usages。同包原生已验中英 Keymap、参数 Copy JSON、预览 Copy/Export、中文 Schema 写回与一次 Undo、上传菜单取消、目录 Enter，以及 Pin 开/关跨应用草稿保留和前台交互。三轮关闭后静置约 150 秒，表单、Markdown JCEF panel、RemoteBrowser 和客户端 Java 对象全部归零，FD 无逐轮增长；正常重启/退出后对应 IDEA 与 CEF 进程已消失。完整桌面视觉遮挡、`…`/目录右键菜单可见结果、CEF native peer/listener/性能及其余矩阵仍未通过，R2/4.2 保持未勾选，正式 MCP 不推进。Junie 保留，临时语言与源码已恢复；最低 build 仍为 `263.7031.23`。详见[full-17 记录](verification-2026-11-07-full17.md)；[同日前序记录](verification-2026-11-07.md)及下方历史失败/修复边界保留。


## 2026-11-06 修复与复验

已修复内部点击误触发 popup mouse-out cancel，以及静态 renderer 强持有最近参数表单。full-12 无 agent 原生复验通过预览 Copy/Export、代表性 Schema 写回和 Undo；随后 full-13 完成 renderer 修复，69 suites / 359 tests 全绿、完整构建及 EAP `263.7031.23` / 正式 `263.7307.200` 两个 Verifier 均 Compatible，无 production compatibility/internal/deprecated/override-only 问题，Experimental 仍 16。full-13 安装和隔离身份已核对，但 Mac 锁定导致最终包的原生参数/Copy/Export 回归及关闭后 GC 未执行。完整菜单、JCEF/native memory/listener 与性能矩阵继续未完成；R2、4.2 保持未勾选，正式 MCP 不推进。具体包身份、失败、修复和证据见[11 月 6 日记录](verification-2026-11-06.md)。

## 2026-11-05 更新

正式版 `IU-263.7307.200` 已核对。full-10 在该 build 的 Verifier 发现 13 处 XML/DOM 类解析问题；补齐显式 XML bundled 依赖后，full-11 完整 352 项测试和 EAP/正式版两个精确 build 的严格 Verifier 通过，仍保留 16 Experimental usages。原生新增通过 XML-only Dubbo 预览、工具窗口 Markdown 导出及参数 JSON 复制/剪贴板读回；预览 Markdown Copy、参数写回、完整菜单和资源验收仍未通过，正式 MCP 不推进，最低 build 暂不改变。新包身份、失败与修复过程、原生观察及本轮持久证据位置见[正式版补验记录](verification-2026-11-05.md)。下文 full-10 / 2026-10-25 结论为历史阶段，不代表当前 full-11 的全部验收状态。

## 当前结论（2026-10-25）

**full-10 自动构建、352 项测试、IU 263 Plugin Verifier 与新 ZIP 读回已通过；生产升级仍等待完整原生回归。** Codex 取消客户端准入 3/3、IU 263 精确平台解析、Java 25/IPGP 2.17.0 配置与生产 classfile 核验也已通过。官方 `codex-app-server 0.165.0-alpha.1` prerelease 候选在隔离 IU `263.7031.23` 上通过三轮真实 `turn/interrupt` 取消、服务端 cleanup 与同 session echo；`0.163.0-alpha.11.2` 的历史失败和官方 TypeScript SDK 补充结果均保留。Verifier 没有生产 deprecated/internal/override-only/compatibility 问题，但测试源码编译另有 15 条 test-only deprecated/removal warnings，且仍报告 16 处 Experimental API。新包 Markdown tamper/精确恢复、GET/form 生成与打开、Spring/Feign/Dubbo 预览及 Welcome 关闭重开等代表场景已通过；Copy/Export 等原生矩阵仍未完成。

2026-10-25 在同一 full-10 包新增通过文档库分页/末页打开、用户编辑冲突恢复、真实 I/O 部分发布及重试、8,030 接口生成的原生在途取消及取消后重试、关闭/重开 freshness。实际取消通过原生任务列表的键盘 Cancel 按钮，前五次未生效的鼠标尝试不计通过；此项不替代 MCP 协议取消或 HTTP 生成取消。生产/构建输入 222/222 未变，本轮未重跑 Gradle。详细矩阵和边界见[文档库补验记录](../project-document-store/verification.md)。本轮隔离 PID `41280` 已退出，默认两个配置目录冻结的 785 个文件、原 fixture 84 个文件与 Junie 保留，收尾证据在 `/tmp/doc-view-native-20261025/cleanup.json`。Copy/Export、完整菜单/参数编辑与资源/GC 基线等仍有缺口，4.2 保持未勾选；下文旧阶段的“原生取消/partial/retry 待验”由本次已列明的文档库场景更新，其他范围保持原状态。

旧配置尝试使用 IPGP 2.16.0 构建 IU `263.7031.23` 时，检查器仍将 since-build 映射到 Java 21 并产生警告；该失败证据保留。正式 IPGP 2.17.0 已进入 `build.gradle`，其 release notes 和 tag 源码把 build 262 起映射到 Java 25。`verifyPluginProjectConfiguration` 在 2.17.0 下真实通过且无 source/target compatibility 警告。JBR `25.0.4.1` 的生产编译实际 javac 参数为 `--release 25`，未传 `-source` / `-target`；full-10 最终 ZIP 内 329 个生产 class 均为 major 69。证据见 `/tmp/doc-view-store-impl-20261024/baseline-263-ipgp217-config.log`、`/tmp/doc-view-store-impl-20261024/baseline-263-javac-flags.log` 与 full-10 package audit。IU `263.7031.23` 平台及 Verifier 固定输入已从实际元数据读回。

较早全量运行 `263-full-5` 执行 339 个测试、16 个失败；`targeted-6` 执行 52 个测试、3 个失败。历史根因包括 light fixture 版本资源读取、菜单名称期望、异步打开测试的同步轮询和旧 Scratch 文件残留。版本读取、菜单合同、异步等待及 MethodDoc 测试隔离已修复。之后发现并修复了 `requestOpenFile` Future 异常未进入同步 catch、导致 UI 过早显示“已打开”的生产问题。

针对性复验 `targeted-7` 为 51 tests / 0 failures / 0 errors / 0 skipped，其中包含 HTTP 生成、MethodDoc Scratch、Welcome 打开生命周期、文档库 UI、图标与异步打开支持测试。full-7 执行 344 tests 全绿，full-8 为 349 tests 全绿，full-9 为 67 suites / 350 tests 全绿；最新全量复验 `263-full-10` 共执行 67 suites / 352 tests，0 failure、0 error、0 skipped；18 个 Gradle tasks 均执行，`BUILD SUCCESSFUL`，用时 1 分 55 秒。测试源码编译有 15 条 test-only deprecated/removal warnings；它们与生产 Plugin Verifier 问题分开记录。日志与冻结 XML、IDE 日志、Verifier 和性能结果在 `/tmp/doc-view-store-impl-20261024/baseline-263-full-10.log` 与 `/tmp/doc-view-store-impl-20261024/263-full-10-results/`。此前 full-5/targeted-6 的失败继续作为历史证据保留。

full-10 的 Plugin Verifier 目标 `IU-263.7031.23` 返回 Compatible，生产报告没有 deprecated、internal、override-only 或 compatibility 问题；另有 16 处 Experimental API 使用，记录为平台风险。最终 ZIP SHA-256 为 `b238f5412d134ff4311ba5422fb78d50562bfecf80826964858984d5d69bcc0b`，大小 722,118 bytes；嵌套插件 JAR SHA-256 为 `b3615cd1f5a44de8dd472c3f2c8715ecd4126fe6892374fc307a5a1d7ca6f267`。descriptor 和构建元数据均为插件 `com.liuzhihang.doc-view` 1.4.0、`since-build=263.7031.23`；329 个生产 class 全为 major 69，ZIP 仅有一个插件 JAR、没有测试依赖或额外条目。full-10 相对 full-7 的冻结源码清单差异仅为 `ProjectDocumentIndexService.java`，源码清单含 222 个文件。冻结审计见 `/tmp/doc-view-store-impl-20261024/263-full-10-results/package-audit.json` 与 `/tmp/doc-view-mcp-candidate-20261024/final/263-baseline-full-10/identity.json`。

此前一次 `compileJava` 尝试首先在已移除的 `PsiKeyword.VOID` 引用处失败，随后出现的 Lombok getter 编译错误为级联错误；该引用已改为公开 PSI 类型比较。后续生产 classfile 与 javac flags 已按上段证据验证；2.17.0 配置运行中的 `compileJava` 显示 `UP-TO-DATE`，因此不将那次配置命令误记为新编译。当前精确解析输入为 `263.7031.23-EAP-SNAPSHOT`，实际平台身份已读回为 `IU-263.7031.23`。full-10 自动构建、测试、Verifier 与 ZIP 检查已通过；16 处 Experimental API 与 15 条 test-only deprecated/removal warnings 分别保留。新包原生功能回归仍未完成；不要把自动检查或候选客户端准入写成生产插件完整验收。

本轮只测试虚构项目和独立探针，不把探针当成正式 Doc View MCP 产品。正式工具应在同一个 Doc View 插件内注册到 IDEA 的 `com.intellij.mcpServer`，复用文档服务；生产搜索、详情和生成/更新 MCP 尚未交付。正式 MCP 仍受 IU 263 原生回归及项目文档库验收阻塞。

## 环境与身份

| 项目 | 实际读回 |
| --- | --- |
| IDEA | 官方完整 Apple Silicon DMG，`IU-263.7031.23`，JBR `25.0.4.1+1-b635.72` |
| DMG SHA-256 | `c173e165d6365387302694d1fd9cd0d3b02f420f4af942fcd9ed80a37e1c00d2` |
| IDEA MCP SDK | `0.15.0`，JAR SHA-256 `d85501edcf60d9f9d0c56413a4e01f19913c98d025f6164f416d45e0c86fcadd` |
| 取消探针 | Java PoC `0.1.1`，JAR SHA-256 `8dfecb3f7491fa722cd456331a26cb83c8e0f6de003e16cb369206756bc56d12` |
| 历史实际 Codex | app 随附 CLI `0.163.0-alpha.11.2`；使用现有默认模型 `gpt-6-astra`，未覆盖模型配置；该版本取消失败 |
| 当前取消候选 | 官方 `codex-app-server 0.165.0-alpha.1`，Apple Silicon；临时配置与模型工具调用，app-server SHA-256 `a2fea747e927713c65a2de22a1e7c1be84009e24d53f85948649ec41bb949c18` |
| Codex 调用方式 | 临时 `codex app-server --listen stdio://`，由模型真实调用 MCP 工具，再通过 `turn/interrupt` 中断；没有用手写 HTTP 请求冒充 Codex |
| 补充客户端 | 官方 `@modelcontextprotocol/sdk@1.32.1`，固定 lockfile，临时目录安装；最终安装启用 TLS 校验且可由 `npm ci` 重现 |

IDEA 专用 profile、config/system/log/plugins、虚构项目均在 `/tmp/doc-view-mcp-263-20261023/`；该轮 IDEA PID 为 `73113`，仅监听 `127.0.0.1:64343`，Brave mode 保持关闭。临时 Codex 只允许两个测试工具：`doc_view_java_poc_describe_demo` 与 `doc_view_java_poc_bounded_delay`。该次 0.163 测试未添加持久客户端注册。两轮 wire 复验使用透明回环代理 `127.0.0.1:64344`；代理转发真实 Codex 流量，不补发取消通知、不改请求语义。

## 历史 Codex 0.163 实际互操作

本机证据根目录：`/tmp/doc-view-mcp-upgrade-20261023/codex/`。各轮保存 app-server transcript、IDEA 日志、摘要和临时参数；wire 轮另存 JSON-RPC 方法、ID、会话 hash、时间及 HTTP 状态。会话标识只展示 hash，不在仓库写入原始 session ID。

| 场景 | 原调用与结果 | 判定 |
| --- | --- | --- |
| 直接连接 IDEA | 实际注册、echo、3 次 30 秒调用及 turn interrupt；同 session 后续 echo 3/3 成功。run 3 仍在约 30.362 秒后正常完成 | 调用/恢复 passed；取消 failed |
| 第一轮透明 wire | request ID `5`，server run `6`，session hash `77e727a5468e35ac`；发出后约 160 ms interrupt；无 `notifications/cancelled`，原 HTTP 请求 30.725 秒后返回 200 | failed |
| 第二轮透明 wire | request ID `5`，server run `11`，session hash `a331a0c3b60990e6`；确认服务端启动后再等 2 秒 interrupt；无 `notifications/cancelled`，原 HTTP 请求 30.412 秒后返回 200 | failed；排除立即中断的时序疑点 |

wire 未记录 initialize 响应中的协商协议版本，此字段保持未验证；后续复验应补记，以区分不同协议的通知/断流取消语义。本次同时观察到无取消通知、原响应未提前关闭且任务正常完成，失败结论不只依赖通知缺失。

两轮 wire 在中断后继续保留 Codex 进程和会话，观察超过原任务期限；均未提前关闭原响应连接，同 session 的 request ID `6` echo 成功。IDEA 日志分别记录 `completed` 和一次正常 `cleanup`，未记录对应 `request_cancelled`。这是正常任务结束的清理，不能算取消成功。第一轮 direct 的 run 4/5 在客户端进程退出时取消，不归因于先前的 turn interrupt。

第二轮关键时间（Asia/Shanghai）：

- `23:44:30.950`：IDEA run 11 `started` / `owner_bound`。
- `23:44:32.971`：Codex `turn/interrupt`；turn `01a1696f-566f-7772-916f-8b016119541a` 随后变为 `interrupted`。
- `23:44:38.823`：同 MCP session 的 echo 发出并成功。
- `23:45:01.357`：原 run 11 `completed` / `cleanup`，未被 interrupt 停止。

证据：`direct-run/`、`wire-run/gate-result.json`、`wire-run/wire.jsonl`、`delayed-run/gate-result.json`、`delayed-run/wire.jsonl`、各目录的 `transcript.jsonl` 和 `idea-codex.log`。驱动摘要 `finished=true` 只表示驱动结束，准入结果以 `gate_passed=false` 为准。

## Codex 0.165.0-alpha.1 实际取消互操作（run-04）

证据目录为 `/tmp/doc-view-mcp-candidate-20261024/codex-run-04/`，由实际 Codex app-server 和模型工具调用完成；不是 TypeScript SDK 或自制 HTTP 客户端。候选 release、官方归档摘要、实际版本和二进制 SHA 见[取消调查](../../../docs/mcp-cancellation.md)。MCP initialize 请求与响应均为 `2025-06-18`，session hash `ffa259deefd382a3`；只注册两个虚构探针工具，项目参数为 `/tmp/doc-view-mcp-263-20261023/fictional-project`，工具未读取项目源码。

| 轮次 | Codex turn 状态 | 延迟工具 request ID | IDEA run | wire 取消通知 | `started→cancelled` | `cancelled→cleanup` | 后续 echo ID |
| --- | --- | ---: | ---: | --- | ---: | ---: | --- |
| 1 | interrupted | 5 | 1 | `notifications/cancelled`, requestId 5 | 154 ms | 1 ms | 6，成功 |
| 2 | interrupted | 7 | 2 | `notifications/cancelled`, requestId 7 | 73 ms | 1 ms | 8，成功 |
| 3 | interrupted | 9 | 3 | `notifications/cancelled`, requestId 9 | 24 ms | 0 ms | 10，成功 |

三轮每次都在 `started` 后由 Codex `turn/interrupt` 取消。interrupt 至 app-server 匹配 RPC ACK 分别为 102.954 ms、103.796 ms、102.763 ms；interrupt 至 IDEA 记录 `cancelled` 约为 6.359 ms、8.809 ms、4.622 ms。每轮取消后观察 31.005 秒，未见服务端 `completed`。取消通知的 HTTP 响应为 202 空体；这只是通知传输结果，准入通过依据是匹配的原 request ID、服务端实际取消与单次 cleanup、无迟到完成和同 session echo 恢复的联合证据。IDEA 日志没有 RPC ID，服务端 run 与 wire request 的对应关系按单一 session 内顺序和时间戳关联。

原始证据摘要：`summary.json` SHA-256 `7392cd675d13dde7b5a7acb3d0510a7374e2a623994648fb6b1e0447f64aea4e`；`transcript.jsonl` SHA-256 `3f91f7b3278c6fbe64be8588aa8e12917665214b12c540b6aa25346195d5dbe7`；`wire.jsonl` SHA-256 `cba2f0d41ce6abe2be429849712a84edd655d5f7902640659c1d681b56151fef`；`idea-codex-events.log` SHA-256 `874519f685674ef5b71070f2fe0b075967b8a0d6fe20a51cb6f252ad23c5783c`。原始 `summary.json` 因 runner 等待竞态把 `interrupt_response` 写成 null；transcript 中保存了三次 `{result:{}}` ACK。审计后另生成校正摘要 `/tmp/doc-view-mcp-candidate-20261024/final/codex-run-04-verified-summary.md`（SHA-256 `37912237924ab21c0552aef0eb9f56ea005fcea42a3b1b1e5263cf81e711568b`）和 JSON（SHA-256 `9bf14fb4bd995b46e43df5f597429d7b22400f61df786087998e3975ef8c7639`）；没有改写原始记录。先前 `codex-run` 1–3 属于环境无效尝试，不计入三轮结果。

**配置完整性限制：**校正摘要对全局 Codex 配置只比较哈希、大小和 mtime，没有读取配置内容；当时记录的当前哈希与启动前快照不同，但文件 mtime 早于 run-04。差异来源无法归因，未尝试恢复。run-04 runner 与 transcript 没有配置写操作；因此本轮不声称全局配置前后完全一致。

## 官方 SDK 对照

`/tmp/doc-view-mcp-upgrade-20261023/client-sdk/live-result.json` 与 `server-log-correlation.json` 记录同一个官方 SDK Client/session 下的正常 5 秒调用及三轮取消：

| JSON-RPC request ID | IDEA run | 服务端结果 |
| --- | --- | --- |
| `3` | `8` | 一次 `cancelled`（reason=`request_cancelled`），一次 `cleanup`，无 `completed` |
| `5` | `9` | 一次 `cancelled`（reason=`request_cancelled`），一次 `cleanup`，无 `completed` |
| `7` | `10` | 一次 `cancelled`（reason=`request_cancelled`），一次 `cleanup`，无 `completed` |

SDK 通过 AbortSignal 触发自身协议实现发送取消；取消 notification 与原 tools/call 均为 HTTP 202 空响应，没有迟到成功、transport error 或 unhandled rejection；每轮同 session 的 echo 成功，最终 DELETE 200 / Client close 成功。映射依据是隔离会话顺序与毫秒时间关联，IDEA 探针日志本身不含 JSON-RPC ID。

首轮驱动错误地等原请求响应后才 abort，未执行有效取消；`live-result-run1.json` 保留为失败记录，修复驱动后以上三轮复验通过。SDK lockfile SHA-256 为 `590fbfdb9622517108206253c0d55578c15eff7793c4e7793a21253411d2b57a`；没有增加生产依赖。

## 公开实现排查与修复边界

官方同版本源码中，Direct/Code Mode 调用共用 approved MCP call 路径，其 await 边界未显式传入 turn cancellation token；这是排查线索，实际二进制结论仍以线上取证为准。`code_mode_host=false` 并非可靠的“切换 Direct”开关；`direct_only_tool_namespaces` 属于开发中的 Code Mode 配置域，尚未做 A/B，不能当已验证修复。本轮没有找到并验证可在 Doc View 内修复 Codex 缺失取消信号的公开方案，未改客户端二进制、未补发通知冒充互操作通过。

一手依据：[Codex 同版本 MCP 调用实现](https://github.com/openai/codex/blob/rust-v0.163.0-alpha.11.2/codex-rs/core/src/mcp_tool_call.rs)、[RMCP binding](https://github.com/openai/codex/blob/rust-v0.163.0-alpha.11.2/codex-rs/codex-mcp/src/binding.rs)、[官方 SDK AbortSignal 处理](https://github.com/modelcontextprotocol/typescript-sdk/blob/ff07b001194fe60ee9deb2121cf119057565796d/src/shared/protocol.ts)。详细只读审计在 `codex/cancellation-audit.md`。

## 构建准备与未运行项

### Verifier 首轮失败与公开 API 迁移

首轮 Verifier `/tmp/doc-view-store-impl-20261024/baseline-263-package-verifier-2.log` 的实际目标是 `IU-263.7031.23`，二进制 verdict 为 Compatible，但严格门禁因 11 处 deprecated usage 与 1 处 Internal usage 失败。原始报告完整保存在 `/tmp/doc-view-store-impl-20261024/verifier-263-first-failed/`；此为已修复的历史结果。

按精确 SDK / 官方源码迁移：DynamicBundle 使用 class-aware 构造；DOM 文件识别覆盖单参数 `isMyFile`；Gson 用与原 `setLenient(false)` 等价的 `Strictness.LEGACY_STRICT`；项目根监听改用 `ModuleRootListener.TOPIC`；插件版本由 `DocViewBuildInfo` 读取构建生成的 `META-INF/doc-view-build.properties`，并严格校验 plugin ID/version；后台 snapshot 使用可让出写锁的 `ReadAction.nonBlocking`。五处文件打开改用弃用注解指定的 `requestOpenFile` / `FileEditorOpenRequest`，保留选中和聚焦。该打开 API 在 263 仍标为 **Experimental**；full-8 Verifier 报告 16 处 Experimental API 使用，但没有 deprecated、internal、override-only 或 compatibility 项。异步打开的成功、失败和取消路径已通过自动测试；实际 IDE 文件打开仍由独立原生回归覆盖。

来源：[263 ReadAction](https://github.com/JetBrains/intellij-community/blob/idea/263.7031.23/platform/core-api/src/com/intellij/openapi/application/ReadAction.java)、[263 NonBlockingReadAction](https://github.com/JetBrains/intellij-community/blob/idea/263.7031.23/platform/core-api/src/com/intellij/openapi/application/NonBlockingReadAction.java) 与本地固定 SDK 字节码。构建资源版本读取已由轻量测试覆盖；安装后的真实插件加载仍应纳入最终原生回归。

只读工件检查找到官方平台、Java/Platform 测试框架工件和 `intellij.platform.credentialStore.impl` 模块，固定输入版本为 `263.7031.23-EAP-SNAPSHOT`；裸 `263.7031.23` Maven POM 返回 404。Gradle 使用完整 EAP 工件坐标，实际平台与 Verifier 报告均读回 `IU-263.7031.23`。IU 263 `product-info.json` 将 `intellij.platform.credentialStore.impl` 映射到 `lib/intellij.platform.credentialStore.impl.jar`，其中含真实 `InMemoryCredentialStore`；该模块通过 `testBundledModule` 仅进入测试 classpath。IPGP 2.17.0 已由官方 release、release notes、tag 源码及 Plugin Portal marker/实现工件核实：release commit `61cf376`，发布日期 2026-06-26；源码将 build 262 映射为 Java 25，Plugin Portal marker、实现 POM 与 2,334,210-byte 实现 JAR 均返回 HTTP 200。2.17.0 项目配置检查、JBR25 `--release 25` 参数及 major69 生产 class 核验已通过。早先 2.16.0 Java 21 release 配置失败证据见 `/tmp/doc-view-mcp-263-20261024/baseline-263-config-compile-3` 及 `/tmp/doc-view-mcp-upgrade-20261023/baseline-audit.md`。

| 门禁 | 本轮状态 |
| --- | --- |
| 安装包、SDK 与探针身份 | passed |
| Codex 注册、真实调用、同 session 后续调用 | passed |
| Codex 候选 `0.165.0-alpha.1` 在途取消 | passed，3/3；见 run-04 |
| 官方 SDK 取消及会话恢复 | passed，补充证据 |
| IU 263 平台工件解析 | passed；固定输入 `263.7031.23-EAP-SNAPSHOT`，平台和 Verifier 实际目标读回 `IU-263.7031.23` |
| Java 25 与正式 IPGP since-build 映射组合 | IPGP 2.17.0 配置检查 passed，无 source/target 警告；JBR25 实际 `--release 25`，最终 ZIP 内329个生产 class 为 major69 |
| test source 编译与全量 tests | passed；full-10 为 67 suites / 352 tests / 0 failures / 0 errors / 0 skipped；另有 15 条 test-only deprecated/removal warnings；full-9 为 350、full-8 为 349、full-7 为 344 项历史全绿；targeted-7 为 51 / 0 / 0 / 0 |
| Plugin Verifier | passed，目标 `IU-263.7031.23` / Compatible；production 报告无 deprecated、internal、override-only、compatibility 问题，另有 16 Experimental usages |
| 最终 263 ZIP 检查 | passed；full-10 ZIP SHA、嵌套 JAR、descriptor、build metadata、329 个 major69 production classes 与无 test-only entries 已读回；该包 hash 单独绑定后续原生验收 |
| 263 生产包原生功能回归 | partial；full-10 新包身份已核对，重开后 `unknown` 符合预期。Markdown tamper/恢复专项 passed：搜索和打开拒绝被篡改页；按精确原 bytes 恢复后，Reload All from Disk、Search、Open 均成功，banner 自动显示 `CURRENT · 30 API(s)`（未执行 Generate/Clear Cache）；63 个文件的 bytes/hash 已恢复，30 个条目的 schema/hash 有效。另一次复测中关闭再重开先显示 `unknown`；随后通过 FindAction 执行 Generate，返回 `30 API(s) / No content changes`，不点 Refresh 即自动变为 `CURRENT`；之后约 +4.7 秒、+20.45 秒读回仍为 current。精确的 1 秒/3 秒采样未记录；先前即时看到 `unknown` 的观察也没有精确时间戳。未复现持续刷新问题，结果与异步状态刷新行为一致。另已通过 Spring/Feign/Dubbo 预览、GET/form 生成打开和 Welcome 重开场景；Copy/Export tab 无可观察焦点，操作未能可靠完成。菜单/右键可达性、完整参数编辑、原生生成取消/partial/retry、资源基线等矩阵仍未完成 |
| HTTP/UI 其他未验项 | 保留原状态，不由代表性 GET/form 草稿验收替代 |

full-10 新包目前已在隔离 IU `263.7031.23` 安装，运行身份与嵌套 JAR hash 匹配本页所列 ZIP。重开后的 `unknown` 是 freshness contract 的预期状态。新包 tamper/恢复专项通过：先篡改 Markdown 后 Search/Open 拒绝；按精确原 bytes 恢复，Reload All from Disk、Search、Open 成功，banner 自动显示 `CURRENT · 30 API(s)`，未执行 Generate 或 Clear Cache；63 个文件的 bytes/hash 与 30 个条目的 schema/hash 均核对通过。另一次 no-op generation 复测先关闭再重开，banner 显示 `unknown`；随后通过 FindAction 执行 Generate，返回 `30 API(s) / No content changes`，不点 Refresh 即自动变为 `CURRENT`，约 +4.7 秒和 +20.45 秒的后续观察仍为 current。没有精确 1 秒/3 秒采样，先前即时读数也没有时间戳；未复现持续刷新问题，观察与异步状态刷新行为一致。

full-10 新包上的 GET/form 代表路径：选中正确的 Doc View tree 方法后，通过 Cmd+Shift+A 的 `Generate HTTP Request` 分别生成 GET 和 form `_2.http`，两个文件均在 HTTP Client tab 打开。磁盘审计确认新文件内容正确、创建时间为 05:36:30/05:36:43 且晚于包启动，六个旧草稿的 SHA/mtime 未变。本轮实际 HTTP Client Run 未执行，原因未记录；因此只确认草稿生成和打开，不确认请求发送。证据：`full10/native/http-full10-generation-disk-audit.json`。其他原生矩阵仍未完成。

full-10 新包还通过了 Spring、Feign、Dubbo 文档预览内容核对；Welcome 页面打开、Cmd+W 关闭、再打开成功。Copy/Export tab 后没有可观察焦点，未能可靠操作，相关通过状态不作推定。树右键菜单在 CUA 中也未观察到，原生编辑器右键同样无显示，归为自动化限制，不归因于 Doc View。完整参数编辑矩阵、生成取消与 partial/retry、性能/资源基线仍待验。各项证据与包身份见 历史 `full-10 原生报告`（原路径 `/tmp/doc-view-mcp-candidate-20261024/final/263-baseline-full-10/native/native-report.md`；2026-11-06 链接检查发现本机文件已不可用，仅保留此前记录，不作为本轮可重读证据）；GET/form 磁盘审计见 `/tmp/doc-view-mcp-candidate-20261024/final/263-baseline-full-10/native/http-full10-generation-disk-audit.json`。

## 历史取消 PoC 轮次的清理与保护

以下读回仅对应此前 Codex 取消 PoC 轮次，不代表 full-10 包的当前原生验证已清理：当时隔离 MCP Server 已关闭；IDEA PID `73113` 退出，`64343` / `64344` 无监听，临时代理退出，DMG 已卸载，探针禁用列表恢复。`/tmp/doc-view-mcp-upgrade-20261023/cleanup.json` 记录：普通环境冻结的 58 个文件全部未变；Codex 全局配置 SHA 未变；生产 `gradle.properties`、`build.gradle` 和已有改动的 `plugin.xml` 与该轮起点一致；Git HEAD 未变。

HTTP/UI G4 ZIP SHA-256 为 `68852145f231627e93a3716d2fb47d6e96236dac2b634441c50872f72db510eb`；其既有 IU261 自动/原生证据不能当作本轮 263 生产包结果。

full-10 功能验收只发生在独立 IU `263.7031.23` 实例 PID `44922`；普通 IDEA PID `93930`（selector `IntelliJIdea2026.2`）与 Junie/profile 保留。此后 CUA 再读 EAP 路径时意外启动 PID `46026`；检查到它只有 `idea.paths.selector=IntelliJIdea2026.3`，没有 `idea.properties.file`，因此使用默认 263 配置，不属于隔离验收，也没有用作产品证据。该意外启动将 MCP 监听端口从 `64342` 改到 `64343`；端口已恢复，MCP enable/consent 保持读回原值。没有默认 263 文件的启动前基线，因此不声称该配置目录的全部文件未变。最终 cleanup 读回确认 PID `44922`、`46026`、`46009` 均不存在，没有残留测试进程；`64343`/`64344` 无监听，`64342` 仅由普通 IDEA PID `93930` 占用；Junie SHA 与基线一致，隔离日志 1,173 行、0 个 ERROR/FATAL 且记录 shutdown。隔离 profile 与 IDEA mount 保留，资源/GC 基线仍未运行。过程与收尾证据见 `/tmp/doc-view-mcp-candidate-20261024/final/263-baseline-full-10/native/unexpected-relaunch-and-port-restoration.json`、`cleanup.json` 和 `idea-full10-startup-through-shutdown.log`。本 change 未执行发布、push、tag、sync 或 archive。

## 文档与范围检查

四个本轮 OpenSpec changes 的 strict validation 均通过；最终 `git diff --check`、文档尾随空白与链接检查均通过。最终只读 scope audit 确认 HEAD 仍为冻结基线 `7b2e0a849fe0fe54df6df08792579a361b712428`，本轮 98 个 scope delta 全部归入 baseline263（16）、HTTP/UI（15）、MCP 文档（10）和 store（57），无未分类项；222/222 冻结源码/构建文件 hash 一致，`.agents`/`.codex` 既有改动保留，staged paths 为空。未执行发布、push、tag、sync 或 archive。证据见 `/tmp/doc-view-store-impl-20261024/scope-audit-final.json`、`/tmp/doc-view-store-impl-20261024/final-openspec-validation.json`。当前 **15/16** 项完成：任务 1.1–1.4、2.1–2.3、3.1–3.3、4.1、4.3、4.4 与 5.1–5.2。4.2 仍未完成；正式 Doc View MCP 仍受原生回归与项目文档库最终验收约束。不要把 263 MCP 探针或旧记录中的 MCP PoC **13/14** 当作正式 Doc View MCP 产品验收。

**历史取消 PoC 阶段范围（不代表 full-10 最终范围）：**当时的 494 文件冻结基线比较仅涵盖路线图和 MCP PoC 文档及本 change 新增文件；该记录保留在 `/tmp/doc-view-mcp-upgrade-20261023/final-preservation.json`。full-10 当前范围以 `/tmp/doc-view-store-impl-20261024/scope-audit-final.json` 为准：539 个仓库基线文件、98 个本轮 scope delta，已归类为 baseline263 16、HTTP/UI 15、MCP 文档 10、store 57，无未分类项。

## 2026-10-24：上游取消修复候选与运行验收

官方 Codex PR #56132 于 2026-10-22 合并；精确 tag `rust-v0.165.0-alpha.1` 的 tool_request 源码包含 CancelOnDrop。隔离候选已实际运行并通过三轮取消互操作；固定提交、来源、官方资产摘要与运行矩阵见[取消调查](../../../docs/mcp-cancellation.md)。旧客户端 0.163 的 wire 失败仍保留。

以下为 full-8/full-7 阶段的历史记录，自动结果与最终产物已由本页当前结论中的 full-10 更新：当时已批准 Java 25 升级，Codex 门槛、精确目标解析、IPGP 2.17.0 配置、349 项测试、Verifier 和候选 ZIP 检查均已通过，full-7 的 344 项全绿另作历史记录。Verifier 当时仍报告 16 处 Experimental API；IU `263.7031.23` 原生回归仍在独立验收，不能据此宣称生产升级整体通过。固定 EAP snapshot 是解析输入，产品验收 build 为 `IU-263.7031.23`。0.163 历史失败、官方 SDK 补充验证和 263 MCP 服务端探针结论保持为各自范围的证据。

现行发布/仓库基线在变更验收前仍按已验证状态声明；本 change 中的 263/Java25 目标尚未发布。新增[项目文档库/UI计划](../project-document-store/proposal.md)可独立推进；[正式MCP计划](../doc-view-mcp-tools/proposal.md)仍依赖取消准入及平台升级完整验收。

### 项目关闭清理 targeted RED/GREEN（2026-11-09）

已在现有 `DocViewToolWindowFactory.init` 中建立由 toolWindow disposable 拥有的 application bus 连接，订阅公开 `ProjectCloseListener.projectClosing`，仅匹配本 project 并直接调用 `ToolWindow.remove()`。没有新增 extension、延时等待、Internal API 或 JNI 操作；范围是已复现的原生 UI/EDT 关闭入口，任意后台同步 close API 的线程契约未获确认，未宣称覆盖。

`DocViewToolWindowLifecycleTest` 首轮因 proxy 其他项目缺少真实路径，在平台 BuildManager listener 抛 NPE；这是测试夹具失败，不能计为业务 RED。改用真实独立临时项目并正常关闭清理后，RED-2 为 2 tests/1 failure，精确断言 owning project removeCount expected1/actual0。生产修复后 2 tests/0 failures/errors/skips，验证未提前创建内容、before-save/其他项目不移除、EDT同步移除、自身parent销毁后连接解绑与重复通知不再次移除。证据在 `build/native-validation/2026-11-09/project-close-lifecycle/{red,red-2,green}.{log,xml}`。完整构建、两build Verifier和最终包原生6.4仍待复验。

## 2026-11-09：1.6.0 最终包重启生效烟测

最终 ZIP `0f58a4365ac7c026ed588838a34cd71aaf54dfb17b1af7608f24302ba82c4cc4` 在正式 IU `263.7307.200`、无调试器的隔离实例验证：Doc View 1.6.0 停用后，Apply/OK 显示“Restart IntelliJ IDEA to apply changes in plugins?”；日志明确该插件要求 restart。正常退出 PID 89512 后，以同一隔离配置显式启动 PID 90274，设置页回读 Disabled，工具窗口不再出现；随后正常退出。安装、更新、卸载的全部排列尚未运行，7.4 保持未完成，本结果只证明停用/重启路径。

原生 Copy、65 接口文档生成、3/3 查询与 133 文件只读校验见 [1.6.0 发布记录](../../../docs/release-1.6.0.md)。证据位于 `build/native-validation/2026-11-08/remaining-native/release-1.6.0-candidate/` 的 `native-summary.json`、`restart-prompt.json`、`disabled-after-restart-settings.json`、`cleanup-preservation.json`。20 个既有 fixture 源文件未变，3 个临时源已清理，117 个原文档库文件已恢复；原工作区 HEAD 与 584 个文件哈希未变，Junie 未操作。
