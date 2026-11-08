# 路线图

2026-11-09 发布决策更新：维护者已批准 `require-restart=true`，并明确允许未补齐的原生验收留待人工验证/用户反馈，不再阻断本次发布；已知功能问题仍先修复。1.6.0 已打 tag 并发布 GitHub Release，Marketplace update 1215457 已接收待审，完整渠道状态以[发布记录](release-1.6.0.md)为准。旧热卸载失败及各候选边界保留，未通过任务不批量勾选；正式MCP与Skill未交付。下文“不发布/阻塞”属于该决策前的历史阶段。

2026-11-09 历史候选门槛（发布前）：429 候选（ZIP `6c789b81…8370`）已通过 80 suites / 429 tests、构建及最低 `263.7031.23` / 正式 `263.7307.200` 两个精确 build 的 Verifier。原生补齐 HTML 加载/渲染/空/失败/取消状态、旧回调防覆盖、Copy/Export，以及项目文档生成和搜索取消后重试；测试临时模板和源码已恢复。三轮实际 HTML 关闭后 renderer 子进程退出、FD 均为512，但随后热卸载仍有56条 Doc View histogram rows、类加载器仍被引用，正在分析新 heap。428 的特定原生序列曾卸载成功，不能覆盖429这次失败。搜索取消提示误称“状态检查”已改为通用文案，下一候选构建与原生复验进行中。OS 凭据拒绝/失败重试、最终发布包原生验收和完整资源门槛继续保留；未升级版本、打 tag 或发布，正式 MCP 尚不推进。原工作区584个冻结文件与 HEAD 已复核未变，Junie 保留。

以下日期段落保留对应历史候选的证据边界，当前状态以本文首段和[发布记录](release-1.6.0.md)为准；历史详情见[续验记录](../openspec/changes/http-client-generation-and-ui/verification-2026-11-08-remaining-native.md)为准。


2026-11-09 补充：421 包已补受控原生目录取消/恢复、预览加载取消、HTTP 同方法在途合并和未保存草稿保护。新增线程断言正在修复；热卸载根已确定为已关闭项目仍保留 Doc View ToolWindowEntry，公开 API 清理方案需 RED/GREEN 与最终包原生复验。受控断点数据不作性能结论；发布门槛仍未通过。
2026-11-09 最新状态：多行输入、Markdown 换行、批量 Undo/Redo 与 Escape 修复包 `cbc2c714…a9a4d` 已通过 421 tests、构建和最低/正式两个精确 build 的 Verifier。原生 Schema 必填/转义/成员保留/拒绝/单次 Undo/Redo、导航/复制、四组 Finder 遮挡，以及整类 Spring/Dubbo、空预览、模板异常/恢复已补验；HTTP L01 空目录与 L02 双项目重载有对应 419 包证据。当前 421 包在新增整类/模板异常使用顺序后热卸载再次失败，已保存日志及 heap，正在定位新的类加载器引用；不能沿用旧 414 包的卸载通过。HTTP L03、受控取消/迟到回调、凭据 OS 拒绝/重试等独立门槛仍开放，未升级或发布。 详见[续验记录](../openspec/changes/http-client-generation-and-ui/verification-2026-11-08-remaining-native.md)。下文保留旧包历史边界。

2026-11-08 派生缓存/异常/UI 续验：同一 d094 候选补齐双项目派生索引缓存隔离和不同设置值读回，HTTP/UI 5.4 完成，累计 35/41；HTTP 拒绝、写入失败、Dumb Mode、源码变更/删除、依赖禁用与恢复重试取得原生证据，Copy/写回/导出、中英窄窗及有限三轮 GC/一次热卸载也已补验。完整矩阵仍保留。另确认 XML-only/实现类注解的 Dubbo 目录发现属于既有缺口，正在补契约、测试和修复，修复后新候选必须重新核对自动门禁与受影响原生路径。普通 IDEA 误开的虚构项目已在单独授权后关闭，未操作 Junie；最近项目记录不宣称字节未变。详见[本轮记录](../openspec/changes/http-client-generation-and-ui/verification-2026-11-08-remaining-native.md)。未升级版本、打 tag 或发布；下文为相应旧包的历史进度。

2026-11-08 HTTP/缓存/搜索续验：修复完整方法说明尾部不可检索；新包 403 tests、构建和两个精确 build 的 Verifier 通过。原生目录及落盘文档搜索、双项目 DTO 缓存/文件保护、HTTP 取消重试与在途关闭已取得证据；固定模型进程首次/热生成各 7 次，p50/p95 分别为 394.121/419.352 ms 与 137.470/180.368 ms。HTTP/UI 完成 34/41，派生索引缓存双项目隔离、完整 L01–L05 和新包 UI/GC 矩阵仍保留，尚不发布。详见[本轮记录](../openspec/changes/http-client-generation-and-ui/verification-2026-11-08-http-cache-search.md)。下文保留对应旧包的历史边界。

2026-11-08 中文/键盘/JNI 续验已推进：修复参数按钮焦点、表格 Ctrl+Tab 返回按钮，以及目录销毁时的模型、父组件和鼠标路径引用。最新包 402 tests、构建与两个精确 build Verifier 通过；原生键盘写回/Undo、目录菜单/Enter、四种 Pin 状态跨应用，以及同进程两轮目录热卸载通过。中文深浅色、Compact/非 Compact、窄窗和三轮资源结果按各包身份记录；完整取消、跨项目和性能矩阵仍待验，不发布。详见[续验记录](../openspec/changes/http-client-generation-and-ui/verification-2026-11-08-keyboard-resources.md)。以下旧包失败和旧待验描述保留历史边界。

2026-11-08 辅助功能续验：在获准的隔离窗口中补齐参数/预览 Pin 开关的真实 Finder 往返，以及设置、导出和上传选择取消，HTTP/UI 5.8 完成。发现按钮说明未进入浮动提示，已用公开 TooltipDescriptionProvider 修复；最终包 398 tests、构建和两个精确 build Verifier 通过，英语深色的三按钮提示、复制、Schema 写回与 Undo 原生通过。中文/浅色/窄窗/键盘和完整资源矩阵仍待验；JNI 热卸载仍未通过，不发布。详见[纯图标验收记录](../openspec/changes/http-client-generation-and-ui/verification-2026-11-08-icon-buttons.md)。

2026-11-08 卸载 API 续验：已新增仅在 Doc View 自身 `beforePluginUnload` 调用公开 `IconLoader.clearCache()` 的修复。新包在正式 263 的无 UI 卸载对照中类加载器已释放；打开目录后仍被 macOS 无障碍 Local JNI 引用，官方新堆报告确认热卸载未通过。未找到可供插件释放该 JNI 根的公开 API 或官方已发布修复。剩余弹窗验收继续保留；构建、测试修正和两 build Verifier 的精确进度见[本轮记录](../openspec/changes/intellij-263-baseline-upgrade/verification-2026-11-08-unload-api.md)。原工作区与 Junie 保留，不发布。下方未清缓存等描述是此前旧包阶段。

2026-11-08：独立工作树已整合已发布 1.5.0 与 263/HTTP/UI/文档库改动；首轮按钮测试和索引取消问题已修复，同一整合包此前 395 tests、构建和两个 Verifier 通过，本轮补齐 Copy/Export、Schema 写回与 Undo、文档分页/冲突保护及有限资源回归。隔离实例由维护者激活后，原生 HTTP Client 的 GET/JSON POST/表单 POST 回环执行通过，部分桌面菜单可见结果已补齐；真实激活 Finder 后，参数弹窗 Pin 开启这一遮挡场景通过。热卸载仍失败，官方本机分析已分别取得无障碍 JNI 与平台图标缓存保留 ClassLoader 的引用链，修复方案尚未验证。完整弹窗、菜单与剩余矩阵未通过，未升级或发布。详见[发布前核对记录](release-preflight-2026-11-07.md)和[新整合包原生记录](../openspec/changes/http-client-generation-and-ui/verification-2026-11-08-http-cache-search.md#历史证据边界)；下方 full-17 及更早结果仍是对应旧包证据。

Doc View 的近期目标是稳定“Java 源码 → `DocView` → Markdown/导出/上传”主链路，再逐步补齐 IDE 内的文档查找、配置和 AI 检索能力。项目 Markdown 导出和 IDEA 普通文件搜索已实现；结构化 JSON 文档库与文档搜索 UI 正在开发验收；正式 MCP 工具及客户端 Skill 仍受各自准入门槛约束。产品与 UI 方向及其证据、假设和验收建议见[设计研究](design.md)。

设置项有效性、平台配置状态及现有用户文档的补充核对见[设置与用户文档评估](settings-audit.md)。有源码证据的集成设置可靠性与凭据存储排在视觉改版前，各项使用独立 change。

## 1.4.0 发布状态

截至 2026-10-22，1.4.0 最终候选包已构建：49 suites、237 tests 全部通过，18 个 Gradle actionable tasks 执行，IU 261.22158.277 Plugin Verifier 为 `Compatible`。最终包已在官方 IU261 中进行部分原生 smoke：加载 1.4.0、JCEF sandbox enabled、升级通知显示 1.4.0、参数编辑器能打开并读取字段、欢迎页 HTML 能显示；Schema 写回及其他残余场景仍未全验。ZIP SHA-256、包清单、原生烟测和剩余验收边界见[1.4.0 发布记录](release-1.4.0.md)。[PR #109](https://github.com/liuzhihang/doc-view/pull/109) 已合并，`v1.4.0` tag 与 GitHub Release 已公开。Marketplace 于 2026-10-22 的读回记录为已接收更新 `1200396`、尚未审核/公开；本路线图未刷新该状态，详见发布记录。

真实平台上传按维护者要求排除；OS PasswordSafe 异常路径、Schema 原生写回完整性和 JCEF 特殊状态仍有验证缺口，不得写为全量验收通过或归档。MCP Java 探针是独立开发 PoC，不属于 1.4.0 生产交付或自动发布阻断项。

## 有序队列

截至 2026-11-07，full-17 已修复七个 Keymap 组的中英文标题，70 suites / 362 tests、构建及 EAP `263.7031.23` / 正式 `263.7307.200` 两个 Verifier 通过，仍有 16 Experimental usages。同包原生已验中英 Keymap、参数 Copy JSON、预览 Copy/Export、中文 Schema 写回与一次 Undo、上传菜单取消、目录 Enter，以及 Pin 开/关跨应用草稿保留和前台交互。三轮关闭后静置约 150 秒，表单、Markdown JCEF panel、RemoteBrowser 和客户端 Java 对象全部归零，FD 无逐轮增长；正常重启/退出后对应 IDEA 与 CEF 进程已消失。完整桌面视觉遮挡、`…`/目录右键菜单可见结果、CEF native peer/listener/性能及其余矩阵仍未通过，R2/4.2 保持未勾选，正式 MCP 不推进。Junie 保留，临时语言与源码已恢复；最低 build 仍为 `263.7031.23`。详情见[full-17 续验记录](../openspec/changes/intellij-263-baseline-upgrade/verification-2026-11-07-full17.md)；full-13/14/16 和下方旧日期均保留为历史阶段。

下表记录推进顺序；推进与验证原则见[横向门禁](#横向门禁)和[发布检查清单](release-checklist.md)。每项进入开发时都应有独立 OpenSpec change 和相应验证，并非每项都在技术上依赖前一项。未完成项以各 change 的 tasks 和 verification 为准。

| 顺序 | Change / 范围 | 目标与推进条件 |
| --- | --- | --- |
| 1 | [`fix-template-settings-lifecycle`](../openspec/changes/fix-template-settings-lifecycle/proposal.md) | 模板草稿、Apply/Reset/Cancel 状态与资源释放问题已修复，自动回归及原生验收完成，并已随 1.4.0 交付。详见[验证记录](../openspec/changes/fix-template-settings-lifecycle/verification.md)。 |
| 2 | [`fix-preview-lifecycle`](../openspec/changes/fix-preview-lifecycle/proposal.md) | 预览生成、后台任务列表和渲染切换已实现并完成自动验证；原生打开/重开与主题场景已验。JCEF 预览窗口关闭及释放的剩余边界见[验证记录](../openspec/changes/fix-preview-lifecycle/verification.md)。 |
| 3 | [`fix-integration-settings-state`](../openspec/changes/fix-integration-settings-state/proposal.md) | 设置空值、地址规范化、YApi 校验/部分提交、语雀导航和配置注册 ID 已修复并通过回归；原生 Apply/Revert 与缺配置导航已验。真实平台上传按维护者要求从本轮验收排除，不作阻断；详见[验证记录](../openspec/changes/fix-integration-settings-state/verification.md)。 |
| 4 | [`integration-credential-storage`](../openspec/changes/integration-credential-storage/proposal.md) | 已修复 PasswordSafe 同 serviceName 的跨项目覆盖：生成键按平台与项目位置隔离，并由真实 IU261 InMemoryCredentialStore 取得 RED/GREEN。IU261 原生已验证 A/B 保存、同包重启回读及清除 A 后 B 仍保留；OS 授权/拒绝和后端失败重试仍待验。真实上传本轮排除，旧 XML 不迁移。详见[验证记录](../openspec/changes/integration-credential-storage/verification.md)。 |
| 5 | [`api-tree-navigation`](../openspec/changes/api-tree-navigation/proposal.md) | 目录排序、源码/文档导航、刷新/隐藏、菜单、上传入口、深浅主题及 Compact Mode 等已实现；主要原生矩阵已验。证据范围和未覆盖项见[验证记录](../openspec/changes/api-tree-navigation/verification.md)。 |
| 6 | [`doc-view-configuration`](../openspec/changes/doc-view-configuration/proposal.md) | 设置分类、模板预览与规则说明已实现；截至 2026-10-22，各页代表字段保存/撤销/取消、A/B 项目隔离和重启读回已补验，并修复三平台窄窗口清除标签裁切。即时主题切换由维护者人工确认通过；凭据原生验收另见 credential change。任务与剩余范围见[验证记录](../openspec/changes/doc-view-configuration/verification.md)。 |
| 7 | [`doc-view-welcome`](../openspec/changes/doc-view-welcome/proposal.md) | 已改为随包 HTML/CSS + JCEF 欢迎页与更新页，7 个设置帮助入口打开 GitHub 指南。键盘专项按用户要求暂缓；截至 2026-10-22，发布记录确认六份 GitHub 指南已公开且与本地一致。详见[验证记录](../openspec/changes/doc-view-welcome/verification.md)和[1.4.0 发布记录](release-1.4.0.md)。 |
| 8 | [`openapi3-schema-required-mode`](../openspec/changes/openapi3-schema-required-mode/proposal.md) | `requiredMode` 三态、优先级、安全写回及 DTO golden 已实现；421 项候选补齐原生多行/必填/成员保留/缺少成员拒绝/批量 Undo/Redo。其余资源与发布门槛独立跟踪。详见[验证记录](../openspec/changes/openapi3-schema-required-mode/verification.md)。 |
| 9 | [`openapi3-parameter-metadata`](../openspec/changes/openapi3-parameter-metadata/proposal.md) | 方法参数名称、描述、示例及 Spring 绑定/required 冲突规则已实现并通过自动回归。详见[验证记录](../openspec/changes/openapi3-parameter-metadata/verification.md)。 |
| 10 | [`doc-generation-performance`](../openspec/changes/doc-generation-performance/proposal.md) | 已处理缓存 epoch、过期结果写回和递归取消。IU261 原生目录扫描取消后保留旧树、不发布部分结果已验；历史候选包补过整类 Spring/Dubbo 源预览和刷新的一次可见时间上界；取消后的可见终态、重复测量及 HTML 矩阵仍待补。详见[验证记录](../openspec/changes/doc-generation-performance/verification.md)。 |
| 11 | [`doc-export-workspace`](../openspec/changes/doc-export-workspace/proposal.md) | 项目 Markdown、稳定身份/源码链接、冲突保护和后台快照已实现。IU261 已补覆盖确认、扫描取消和关闭项目不发布；IU261/IU262 已补两模块索引及页面/源码导航。导出前关闭项目仍创建空目录的缺陷已修复并原生复验；目录动作通过 Find Action 执行，未冒充右键菜单点击。详见[验证记录](../openspec/changes/doc-export-workspace/verification.md)。 |
| 12 | [`mcp-java-feasibility`](../openspec/changes/mcp-java-feasibility/proposal.md) | IU 261 的历史协议取消失败保留；IU 263.7031.23 / SDK 0.15.0 隔离探针的取消、项目/服务/插件生命周期与有限资源检查通过。Codex 0.163.0-alpha.11.2 的 turn interrupt 未取消服务端任务；官方 0.165.0-alpha.1 候选随后在隔离 IU 263 上三轮真实取消互操作通过，PoC 2.4 已完成。当前开发配置已提高到 IU263/Java25，完整自动矩阵通过，原生回归仍在进行；尚未发布。详见[验证记录](../openspec/changes/mcp-java-feasibility/verification.md)和[取消验收说明](mcp-cancellation.md)。 |
| 13 | [`doc-view-mcp-tools`](../openspec/changes/doc-view-mcp-tools/proposal.md) | 正式 MCP 放在同一个 Doc View 插件内，可选注册到 IDEA MCP Server；与界面按钮共用文档服务。只读 status/search/get 与显式 generate 的共享文档库 contract 已定义，生产工具尚未实现。Codex 候选取消门槛已通过，精确263的构建和Verifier已通过，但 task 1.3 仍等待原生回归和文档库最终验收；详见 [tasks](../openspec/changes/doc-view-mcp-tools/tasks.md)。 |
| 14 | 轻量 API Wiki / [`doc-export-index-readability`](../openspec/changes/doc-export-index-readability/proposal.md) | 已将宽表改为模块/类/方法分组；IU261/IU262 两模块的接口页、重载及 Document/Source 导航已原生验证。模型目录只在实测缺口后考虑；不拆独立 Wiki 插件，也不要求 MCP 先成功。 |
| 15 | Air 可选协作评估 | 已核对官方 ACP/MCP 边界并定义只读流程；原生账户/会话和文件读取未验。保持可选评估，不把 Air、ACP 或 MCP 视为已集成能力。见[评估记录](ai-context-assessment.md#可执行的只读验收流程)。 |
| 16 | [`platform-upload-stability`](../openspec/changes/platform-upload-stability/proposal.md) | 语雀错误分类、响应校验、编码、超时、资源关闭与取消已修复并通过本机 HTTP/上传服务测试。真实上传按维护者要求移为本轮非阻断排除项，第三方账户未访问，不标记实测通过。详见[验证记录](../openspec/changes/platform-upload-stability/verification.md)。 |
| 17 | `doc-import-viewer` | [源码评估完成](ai-context-assessment.md#历史文档导入与查看路线图-17)：不重复建设 Markdown 查看器。先完成第 11/14 项原生阅读验证；只有历史目录/差异存在实测缺口时，才另立只读快照 change。 |
| 18 | [`clear-cache-safety`](../openspec/changes/clear-cache-safety/proposal.md) | Clear Cache 已改为只清当前项目 schema 缓存，不再删除手写 Scratch；旧版自动回归及工具栏入口已验，目录菜单入口仍未完成；当前 HTTP/UI change 已将入口移至窗口 `…`，A/B 自动缓存隔离和文件保护已补验；原生新入口及 A/B 原生场景仍待验。详见[验证记录](../openspec/changes/clear-cache-safety/verification.md)。 |
| 19 | [`annotation-text-fallback`](../openspec/changes/annotation-text-fallback/proposal.md) | 空白注解回退及 Swagger2 常量名处理已修复，并通过回归测试。详见[验证记录](../openspec/changes/annotation-text-fallback/verification.md)。 |
| 20 | [`async-method-doc-preview`](../openspec/changes/async-method-doc-preview/proposal.md) | 方法文档以可取消后台文本快照生成，再于 EDT 打开 Scratch；IU261 原生 Enter/双击、任务取消和项目关闭后丢弃迟到结果已验，未产生部分 Scratch。受控断点不作为自然耗时证据。详见[验证记录](../openspec/changes/async-method-doc-preview/verification.md)。 |

## 下一轮迭代路线（截至 2026-10-24）

现有项目导出提供人读 Markdown；IDEA **Find in Files → Project** 可搜索这些普通文件。结构化 JSON 文档库和相应 UI 属于 [`project-document-store`](../openspec/changes/project-document-store/proposal.md)，可先在当前平台基线上独立实施，不等待 Codex 取消门槛。正式 MCP 属于 [`doc-view-mcp-tools`](../openspec/changes/doc-view-mcp-tools/proposal.md)，客户端安装与更新 Skill 由 [`doc-view-client-skill`](../openspec/changes/doc-view-client-skill/proposal.md) 规划；三者均未作为生产能力交付。格式、使用入口和取消边界见[文档库格式](mcp-format.md)、[MCP 使用说明](user-guide/mcp.md)与[取消验收说明](mcp-cancellation.md)。

HTTP 请求生成与工具窗口收纳由 [`http-client-generation-and-ui`](../openspec/changes/http-client-generation-and-ui/proposal.md) 跟踪：已选择 Java 自生成 `.http` 文件交由 IDEA HTTP Client 的路线，代码和自动回归已有进展；完整原生菜单、发送、取消/关闭及资源矩阵仍有未完成任务。实际范围以该 change 的 [tasks](../openspec/changes/http-client-generation-and-ui/tasks.md) 和[验证记录](../openspec/changes/http-client-generation-and-ui/verification.md)为准，不表示整个 change 已完成。

| 顺序 | 阶段 | 范围与门槛 |
| --- | --- | --- |
| 1 | 项目文档库与 UI | 固定 `docs/doc-view/` 下的 JSON 摘要/详情及共享生成、搜索和打开 UI 已实现。IU263/Java25 的 full-10 自动回归 67 suites / 352 tests 全绿，构建和严格 Verifier 通过；另有 15 条 test-only deprecated/removal warnings 与 16 处 Experimental API 使用，分别记录。近期修复了恢复后的 Markdown 状态仍显示 `INCONSISTENT`；针对性自动回归已纳入 full-10。full-10 ZIP SHA-256 为 `b238f5412d134ff4311ba5422fb78d50562bfecf80826964858984d5d69bcc0b`。该包部分原生场景已通过：精确还原 Markdown 后重新加载、Search/Open 成功，banner 自动显示 `CURRENT · 30 API(s)`，63 个文件 bytes/hash 与 30 个条目 schema/hash 有效；Spring/Feign/Dubbo 预览、Welcome 关闭重开、GET/form 草稿生成并在 HTTP Client 打开也已通过。no-op 场景重开后初始显示 `unknown`，之后 Generate 返回 `30 API(s) / No content changes`，不点 Refresh 即自动变为 `CURRENT`，约 +4.7 秒与 +20.45 秒读回仍为 current，未复现持续刷新问题。HTTP Client Run 未执行。2026-10-25 同包新增通过文档库20+5分页/末页打开、用户编辑冲突保护与恢复、真实I/O部分发布/重试、8,030接口生成在途取消/取消后重试及关闭重开freshness。上述为 full-10 阶段证据；full-17 已补代表性 Copy/Export、写回/Undo 和有限资源回归，独立菜单、完整参数/HTTP取消及资源矩阵仍待完成。具体包与收尾读回见[验证记录](../openspec/changes/intellij-263-baseline-upgrade/verification.md)。 |
| 2 | Codex 取消候选实测（与第 1 项并行） | 官方 `0.165.0-alpha.1` app-server 候选在隔离 IU 263.7031.23、协议 `2025-06-18` 上三轮真实 turn interrupt 取消通过；旧 `0.163.0-alpha.11.2` 失败记录保留，不能推导所有 Codex 版本行为相同。证据见[取消说明](mcp-cancellation.md)。 |
| 3 | 精确 263 生产基线 | 用户已授权按 [`intellij-263-baseline-upgrade`](../openspec/changes/intellij-263-baseline-upgrade/proposal.md) 使用最低 `IU-263.7031.23` 与 Java 25。最新 full-17 的 362 tests、构建、配置检查及 EAP/正式版两个 Verifier 通过，production compatibility/deprecated/internal/override-only 为 0，保留 16 Experimental 和 15 条 test-only 警告。同包代表性 Copy/Export、中文写回/Undo、中英 Keymap 与有限资源回归通过；独立菜单、视觉遮挡及完整生命周期矩阵未完。文档库取消/partial/retry 的 full-10 历史证据另行保留，不混成同一包全量验收。此为未发布开发包，不推导未来 build 兼容性。 |
| 4 | 正式 MCP 工具 | 文档库最终验收和 263 生产基线验收后，在同一个 Doc View 插件中实现可选 MCP provider，注册到 IDEA 自带 MCP Server。先交付只读 `status/search/get`，再交付显式写盘的 `generate`（同时负责首次生成与更新）；不另建 MCP 服务或解析链路。客户端候选准入已通过，正式工具尚未实现；263 新包原生验收为 partial，项目文档库最终验收和若干原生边界仍未完成，task 1.3 未关闭。 |
| 5 | 客户端 Skill | 工具契约稳定后，按 [`doc-view-client-skill`](../openspec/changes/doc-view-client-skill/proposal.md) 从 GitHub 提供显式安装、检查更新和更新入口，先对 Codex 客户端验收。Skill 调用正式 MCP 工具，不另起服务、不搬移解析；安装不自动注册 MCP、改写客户端配置或升级插件。分发与安装尚未实现。 |

JCEF、Schema 写回、PasswordSafe 异常与重试、HTTP/UI 最终原生验收及 Clear Cache 等旧欠账仍按各自活动 change 跟踪，可与文档库和候选客户端实测并行；未完成不表示已观察到运行时缺陷。只有实现和验证完成后才能勾选，不自动 sync/archive 未完成 change。真实平台上传仍按 1.4.0 发布记录列为未执行排除项，不标为通过。

## 用户文档维护

用户追加的主要界面中英化由独立 [`localize-plugin-ui`](../openspec/changes/localize-plugin-ui/proposal.md) 跟踪，不把中英文设置搜索视为 UI 翻译。该 change 保持用户模板、源码和生成文档语言不变；原生语言/窄窗/问号验收与提交发布判断集中在[本轮记录](validation-2026-10-20.md)。

文档工作与迭代同步。仓库已建立[用户指南](user-guide/index.md)入口，包含快速开始、设置、模板、平台集成和故障排查，并由 README 链接。语雀内容作为迁移和核对来源；没有更新远程语雀。

完整使用文档只在 GitHub 仓库 `docs/user-guide/` 维护，按代码分支和 tag 审阅回溯；IDE 内置页面只提供欢迎信息和当前版本更新说明，不打包完整手册。设置帮助在默认浏览器打开对应 GitHub 章节。指南的 `master` 链接须在合并后核对公开可达；本地文件存在不等于线上正文已发布。详见[文档维护评估](settings-audit.md#6-文档整体维护方案)和[设计研究](design.md#44-文档来源与阅读入口)。

Clear Cache 的删除语义与名称/描述空字符串回退已分别纳入第 18、19 项。设置中的 `hideLeft` 与 `excludeParamTypes` 死字段由第 6 项处理，不另立重复待办。

## 上一轮发布范围与持续门禁

截至 2026-10-22，1.4.0 已合并到 master，tag 与 GitHub Release 已公开，Marketplace 已接收更新；Marketplace 审核/上架状态仅按该日发布记录所载读回，不代表本次刷新。此前授权已用于该次发布，不构成下一版本的推送、tag、Marketplace 发布或其他外部写入授权；详见[1.4.0 发布记录](release-1.4.0.md)。

维护者要求本轮 1.4.0 评估排除三平台真实上传验收；这是未执行的非阻断项，不代表平台端到端通过。OS PasswordSafe 异常路径、Schema 写回、JCEF、取消/性能和 Clear Cache 等仍未完成的任务继续由各自 OpenSpec change 跟踪。只有实现与验证完成后才能勾选；不自动 sync/archive 未完成 change，也不把局部原生验收推导为整条队列完成。

既有执行约束继续适用：订阅额度已用满、剩余为零或普通额度不可用时停止任务，不使用 Credits，也不激活重置权益。IDE 原生验证使用专用 `doc-demo`；保留其中既有改动，并在验证后恢复临时设置。

## 历史验证记录与续作入口

candidate 235 是此前阶段的历史候选，不是当前 1.4.0 最终包。此前候选包的完整测试数、hash 和 Verifier 记录保留在[2026-10-20 验证记录](validation-2026-10-20.md)；最终包的结果、未完成原生验收和发布状态以[1.4.0 发布记录](release-1.4.0.md)为准。不同候选包的结果不能合并成同一个包的全量矩阵。

每个功能的长期证据保留在对应 OpenSpec verification。仓库目前没有独立 handoff 文件可清理；无需为清理而删除正式 OpenSpec artifacts、skills 或验证证据。

## 暂缓范围

- 全仓库 AI Wiki、LLM 自动总结/维护、向量存储及独立 Wiki 插件：先验证轻量 API Wiki 的需求，不提前增加第二套解析、存储或生成体系。
- 完整 OpenAPI 规范导入/导出：需要覆盖参数位置、响应码、多媒体类型、components、security、`$ref` 和组合 schema；当前不纳入近期队列。
- 双向同步、多人协作和新生产语言：没有明确差异化需求前暂缓。
- Air 深度集成：保持可选评估，不把 Air、ACP 或 MCP 视为已集成能力。

## 历史归档记录

下列目录名仅说明相应 OpenSpec change 已位于归档目录；本轮没有复核其实现、测试或发布状态，不将归档本身作为本轮验证证据。

- `maintenance-workflow-baseline`（`2026-07-11`）
- `markdown-contract-baseline`（`2026-07-11`）
- `openapi3-operation-summary`（`2026-07-11`）
- `compatibility-deprecation-cleanup`（`2026-07-11`）
- `remove-commons-collections`（`2026-09-16`）

## 横向门禁

- 行为 change 按 [Contract 设计](contract-design.md)定义新版输入、可观察输出、不支持场景和验证方式。新版 Markdown shape 或模板变量可直接替换旧格式，并同步更新现有 golden 与指南；不实现向后兼容、双轨输出或迁移逻辑。正常错误状态、Dumb Mode、取消、持久化和资源释放仍按当前目标 IDE 的 contract 验证；详见[版本演进策略](intellij-compatibility.md#版本演进策略)。
- 触及解析或文档输出的 change 按范围补充真实 PSI fixture 与 Markdown contract 覆盖，关注 Feign、Dubbo、递归、泛型、集合/Map、void 和缺少注释等场景。
- PSI/UI/网络路径遵守[性能指南](performance-guide.md)；IntelliJ API 与平台升级遵守[兼容性策略](intellij-compatibility.md)。
- 发布前执行[发布检查清单](release-checklist.md)。未执行的检查不描述为通过，归档状态不替代当前验证。
