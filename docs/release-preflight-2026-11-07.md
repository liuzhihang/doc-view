# 2026-11-07 发布前核对与整合

2026-11-09 发布决策更新：维护者已批准 `require-restart=true`，并明确允许未补齐的原生验收留待人工验证/用户反馈，不再阻断本次发布；已知功能问题仍先修复。正在准备1.6.0，完整渠道状态以[发布记录](release-1.6.0.md)为准。旧热卸载失败及各候选边界保留，未通过任务不批量勾选；正式MCP与Skill未交付。下文“不发布/阻塞”属于该决策前的历史阶段。

2026-11-09 当前门槛：429 候选（ZIP `6c789b81…8370`）已通过 80 suites / 429 tests、构建及最低 `263.7031.23` / 正式 `263.7307.200` 两个精确 build 的 Verifier。原生补齐 HTML 加载/渲染/空/失败/取消状态、旧回调防覆盖、Copy/Export，以及项目文档生成和搜索取消后重试；测试临时模板和源码已恢复。三轮实际 HTML 关闭后 renderer 子进程退出、FD 均为512，但随后热卸载仍有56条 Doc View histogram rows、类加载器仍被引用，正在分析新 heap。428 的特定原生序列曾卸载成功，不能覆盖429这次失败。搜索取消提示误称“状态检查”已改为通用文案，下一候选构建与原生复验进行中。OS 凭据拒绝/失败重试、最终发布包原生验收和完整资源门槛继续保留；未升级版本、打 tag 或发布，正式 MCP 尚不推进。原工作区584个冻结文件与 HEAD 已复核未变，Junie 保留。

以下日期段落保留对应历史候选的证据边界，当前状态以上段及[续验记录](../openspec/changes/http-client-generation-and-ui/verification-2026-11-08-remaining-native.md)为准。


2026-11-09 补充：421 包已补受控原生目录取消/恢复、预览加载取消、HTTP 同方法在途合并和未保存草稿保护。新增线程断言正在修复；热卸载根已确定为已关闭项目仍保留 Doc View ToolWindowEntry，公开 API 清理方案需 RED/GREEN 与最终包原生复验。受控断点数据不作性能结论；发布门槛仍未通过。
2026-11-09 最新状态：多行输入、Markdown 换行、批量 Undo/Redo 与 Escape 修复包 `cbc2c714…a9a4d` 已通过 421 tests、构建和最低/正式两个精确 build 的 Verifier。原生 Schema 必填/转义/成员保留/拒绝/单次 Undo/Redo、导航/复制、四组 Finder 遮挡，以及整类 Spring/Dubbo、空预览、模板异常/恢复已补验；HTTP L01 空目录与 L02 双项目重载有对应 419 包证据。当前 421 包在新增整类/模板异常使用顺序后热卸载再次失败，已保存日志及 heap，正在定位新的类加载器引用；不能沿用旧 414 包的卸载通过。HTTP L03、受控取消/迟到回调、凭据 OS 拒绝/重试等独立门槛仍开放，未升级或发布。 详见[续验记录](../openspec/changes/http-client-generation-and-ui/verification-2026-11-08-remaining-native.md)。下文保留旧包历史边界。

2026-11-08 HTTP/缓存/搜索续验：修复完整方法说明尾部不可检索；新包 403 tests、构建和两个精确 build 的 Verifier 通过。原生目录及落盘文档搜索、双项目 DTO 缓存/文件保护、HTTP 取消重试与在途关闭已取得证据；固定模型进程首次/热生成各 7 次，p50/p95 分别为 394.121/419.352 ms 与 137.470/180.368 ms。HTTP/UI 完成 34/41，派生索引缓存双项目隔离、完整 L01–L05 和新包 UI/GC 矩阵仍保留，尚不发布。详见[本轮记录](../openspec/changes/http-client-generation-and-ui/verification-2026-11-08-http-cache-search.md)。下文保留对应旧包的历史边界。

2026-11-08 中文/键盘/JNI 续验已推进：修复参数按钮焦点、表格 Ctrl+Tab 返回按钮，以及目录销毁时的模型、父组件和鼠标路径引用。最新包 402 tests、构建与两个精确 build Verifier 通过；原生键盘写回/Undo、目录菜单/Enter、四种 Pin 状态跨应用，以及同进程两轮目录热卸载通过。中文深浅色、Compact/非 Compact、窄窗和三轮资源结果按各包身份记录；完整取消、跨项目和性能矩阵仍待验，不发布。详见[续验记录](../openspec/changes/http-client-generation-and-ui/verification-2026-11-08-keyboard-resources.md)。以下旧包失败和旧待验描述保留历史边界。

2026-11-08 辅助功能续验：在获准的隔离窗口中补齐参数/预览 Pin 开关的真实 Finder 往返，以及设置、导出和上传选择取消，HTTP/UI 5.8 完成。发现按钮说明未进入浮动提示，已用公开 TooltipDescriptionProvider 修复；最终包 398 tests、构建和两个精确 build Verifier 通过，英语深色的三按钮提示、复制、Schema 写回与 Undo 原生通过。中文/浅色/窄窗/键盘和完整资源矩阵仍待验；JNI 热卸载仍未通过，不发布。详见[纯图标验收记录](../openspec/changes/http-client-generation-and-ui/verification-2026-11-08-icon-buttons.md)。

2026-11-08 卸载 API 续验：已新增仅在 Doc View 自身 `beforePluginUnload` 调用公开 `IconLoader.clearCache()` 的修复。新包在正式 263 的无 UI 卸载对照中类加载器已释放；打开目录后仍被 macOS 无障碍 Local JNI 引用，官方新堆报告确认热卸载未通过。未找到可供插件释放该 JNI 根的公开 API 或官方已发布修复。剩余弹窗验收继续保留；构建、测试修正和两 build Verifier 的精确进度见[本轮记录](../openspec/changes/intellij-263-baseline-upgrade/verification-2026-11-08-unload-api.md)。原工作区与 Junie 保留，不发布。下方未清缓存等描述是此前旧包阶段。

状态：**尚未满足“没有其他任务后升级并发布”的条件，未创建新 tag、Release 或 Marketplace 更新。** 本文记录发布准备，不是发布成功记录。

2026-11-08 续验：最终整合包在独立 IU `263.7307.200` 完成部分原生验收，包括 Copy/Export、Schema 写回与 Undo、HTTP 新草稿保留旧编辑、文档分页/冲突保护/重开 unknown，以及三轮预览后的有限资源释放。维护者激活隔离实例后，HTTP Client 实际执行 GET、JSON POST、表单 POST 均为 200，回环参数及请求头断言通过；桌面截图补齐部分 Options/目录菜单。维护者真实激活 Finder 后，参数弹窗 Pin 开启时不再覆盖 Finder，此单一场景通过。热卸载仍失败：未登录诊断 profile 的官方分析分别发现 Swing 无障碍 Local JNI 与平台图标缓存保留 ClassLoader 的引用链；无界面对照也失败，不能把实例归零当作卸载成功。完整弹窗、菜单及生命周期/性能矩阵未通过。230 项生产/构建输入及包 hash 未变，复用此前 395 tests 和两个 Verifier，不宣称本轮重跑。原工作区与 Junie 保留，隔离诊断进程已退出，主隔离激活配置保留。详见[新整合包原生记录](../openspec/changes/http-client-generation-and-ui/verification-2026-11-08-http-cache-search.md#历史证据边界)；下文“原生未运行”等表述保留为 11 月 7 日的阶段状态。

## 已发布基线与本地差异

- 2026-11-07 实际读取 GitHub：最新正式版本为 [1.5.0](https://github.com/liuzhihang/doc-view/releases/tag/v1.5.0)，2026-11-03 发布；远端 `develop` / `master` 均为 `877f0c9cafefc065a92650d99989b42a53ff1cde`。
- 同日 Marketplace 公开 API 返回插件 `15305` 更新 `1211221`：`version=1.5.0`、`approve=true`、`listed=true`、`since=261.0`。1.5.0 已公开上架；11 月 3 日记录中的待审核是历史状态。
- 原工作区 HEAD 为 `7b2e0a849fe0fe54df6df08792579a361b712428`，配置版本仍是 1.4.0，包含尚未发布的 Java 25 / IU 263、HTTP/UI、项目文档库和 MCP 规划。不能直接从旧基线发布，否则会遗漏已交付的 1.5.0 功能。
- 已建立独立工作树 `/Users/liuzhihang/workspace/doc-view-release-integration-20261107`，分支 `codex/release-263-integration`，以远端 `877f0c9` 为基础；按明确清单移入 181 个路径，排除 `.agents/`。原工作区 584 个文件 hash 与 HEAD 保持原样。
- 7 个冲突文件逐项整合，保留 1.5.0 的实体 Markdown 复制、方法/字段版本标签、依赖源码注释、路由及字段表修复，同时保留本轮 263、HTTP/UI 和文档库实现。没有覆盖远端分支或公开制品。

## 本次发布范围

拟纳入 HTTP 请求生成与 UI、Java 25 / IU 263 兼容性升级、落盘 Markdown/JSON 文档库。正式 MCP provider、MCP generate/search/get/status 和客户端 Skill 安装/更新继续作为后续独立功能，不作为本版已交付能力。

独立工作树目前保留上游 `pluginVersion=1.5.0`，仅用于整合验证，**不得用当前验证包覆盖或重复发布 1.5.0**。后续可采用未占用的 1.6.0；发布前须再次检查远端占用，并同步 CHANGELOG、双语欢迎/更新页、描述、指南与 ZIP descriptor。最终版本包的构建和原生安装验证不能由开发验证包自动替代。

当前 JSON v1 单独保留字段级 `since/version`，方法级标签仅通过 Markdown 模板展示；结构化方法元数据扩展需联动 Schema 与测试，本轮未静默增加字段。

## 本轮验证

- 整合首轮全量实际运行 76 suites / 392 tests，2 failures、0 errors、0 skipped，Gradle exit 1。原始结果保留于独立工作树 `build/integration-verification/red/`。
- 失败一：参数编辑器测试假设只有两个底部动作，与已发布 1.5.0 的 Markdown 复制入口冲突。已按动作语义识别三项动作，保留原生实现；`ParamDocEditorPresentationTest` 与 `ParamDocMarkdownCopyTest` 共 2 suites / 5 tests 通过，exit 0。
- 失败二：31,655,679 bytes 合法索引解析期间取消，5 个实测样本中 p95 为 512.483 ms，超过既定 500 ms。取消没有丢失，但格式解析/校验阶段缺少取消检查，不能靠放宽阈值或只保留成功重跑掩盖此结果。最终修复和复验结果见下一节。
- 首轮 EAP `IU-263.7031.23` Verifier 为 Compatible、16 Experimental usages；这不能抵消测试失败，也不能替代最终修复包的两个 build 复验。

## 最终修复与自动门禁

- 参数编辑器保留 1.5.0 的三项动作，测试按 JSON、Markdown、写回源码的实际语义选择，未删除已发布入口。
- 格式读取器为 UTF-8/JSON 分块读取、索引条目校验、canonical JSON 和 SHA-256 计算加入同一查询的活动检查；查询层保留取消、超时和项目关闭错误分类，缓存安装前再次检查。仍保持严格 UTF-8、重复 key、尾随数据、大小/深度及完整性校验。
- 条目校验的定点用例先真实失败：`Index entry validation must observe cancellation before decoding the DTO`；修复后通过。中途误用 Java 21 的工具链启动失败及静态 ReadContext 访问 project 的编译失败也保留，后者已改为显式传入 Project。未把环境失败记成测试通过。
- 最终针对性回归：3 suites / 28 tests；最终全量：**76 suites / 395 tests，0 failures/errors/skipped**。`test buildPlugin verifyPluginProjectConfiguration verifyPlugin --rerun-tasks --no-build-cache --console=plain` 实际执行 18 tasks，1m59s、exit 0。
- 同一最终 ZIP 的 IU `263.7031.23` 与正式 `263.7307.200` 两个 Verifier 均为 **Compatible、16 Experimental usages**，实际 exit 0。production compatibility/internal/deprecated/override-only 问题为 0；仍有 17 条 test-only deprecated/removal 编译警告和 Gradle 弃用提示。
- 最终全量运行中的近 32 MiB 索引取消样本为 `[0.146917, 0.120875, 0.121667, 0.144708, 0.117584]` ms，p95 **0.147 ms**，5/5 观察到 `readIndex` 在途且取消被处理，0 missed、0 completedBeforeCancel。另有 JSON 读取、完整性计算及条目校验的定点取消用例。以上是 light fixture；Gson 的内存树到 DTO 转换仅在调用边界检查，不承诺任意子阶段/任意环境的延迟，也不替代原生或 MCP 协议取消验收。

| 验证产物 | 结果 |
| --- | --- |
| ZIP SHA-256 | `3b304d5d5945de78686579adae22bdea26af839d7199dde453e761fcccabc9bd` |
| 包内 JAR SHA-256 | `2fb839b9bf7525c5806eeb4cb59306abd5d96b5349835856162e0c6dbf2ffb5a` |
| 大小 / 类 | 734,203 bytes；单一生产 JAR；336 classes 全部 major 69；未打入 JUnit、测试类或 PoC |
| descriptor | version 1.5.0，仅用于整合验证；since-build 263.7031.23，无 until-build；**不覆盖公开 1.5.0，也不是待上传正式包** |
| 冻结输入 | 230 个生产/构建输入与最终包构建时一致；原工作区 584 个文件与 HEAD 保持原样 |
| 原生验证 | **本次整合包未执行**；full-17 的原生结果是旧包历史，不能转记到此 ZIP |

原始日志、XML、两版 Verifier、性能样本、冻结输入及包读回位于独立工作树 `build/integration-verification/`；最终材料在 `final/`。三个活动 changes 的 strict validate、文档相对链接与 `git diff --check` 通过。没有 commit、push、tag、Release、Marketplace 上传、sync/archive，也没有启动或修改日常 IDEA、Junie 和原生隔离 profile。

## 仍需完成的发布步骤

1. 整合缺陷修复与当前开发验证包的自动门禁已完成；后续正式版本定稿后，仍需核对其版本文案、最终 ZIP 和实际安装结果，不把当前 1.5.0 编号验证包当作新 Release。
2. 完成当前版本的原生门槛：独立 `…`/目录右键菜单、弹窗跨应用桌面视觉遮挡、HTTP 异常/取消与发送、A/B Clear Cache、文档库最终包生命周期及剩余资源/性能场景。对应 tasks 保留未勾选。
3. 原生缺口中，菜单和合成遮挡属于当前工具观测限制，不是已确认运行时缺陷。full-17 的三轮 Java/FD 资源回归通过，但不等于 CEF native peer/listener/长期资源矩阵通过。所有历史证据按包 hash 分开。
4. 条件满足后定稿新的版本号和发布说明，冻结确切文件与最终包，按 develop → master 流程合入，tag 指向发布 commit，发布 GitHub Release 和 Marketplace，并读取实际渠道状态及制品。

发布门槛来自 [发布检查清单](release-checklist.md) 与 [Release Readiness 技能](../.codex/skills/doc-view-release-readiness/SKILL.md)。清单要求“活动 OpenSpec changes 已完成 tasks”；技能要求先核对发布范围、任务、工作区差异和最终包。本次授权包含条件成立后的发布，不把当前未完成项改为通过，也不因发布意图自动归档 change。

具体未完成范围见 [HTTP/UI tasks](../openspec/changes/http-client-generation-and-ui/tasks.md)、[基线 tasks](../openspec/changes/intellij-263-baseline-upgrade/tasks.md)、[文档库 tasks](../openspec/changes/project-document-store/tasks.md) 与 [full-17 原生记录](../openspec/changes/intellij-263-baseline-upgrade/verification-2026-11-07-full17.md)。真实平台账号上传仍按既有授权排除；Junie 与日常 IDEA profile 保留。
