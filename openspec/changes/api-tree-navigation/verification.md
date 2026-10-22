# 验证记录

早期原生尝试（2026-10-18）曾因 Mac 锁定无法操作；后续在 IU262 隔离 profile 完成了下节列出的局部交互检查。早期锁屏记录不再代表当前所有原生场景均未执行，也不作为任何已通过项的证据。

日期：2026-10-18。分支 `develop`，工作树基于 `00f1f94`，尚未提交或发布。

## 自动回归

- 旧节点实现：5 项真实 PSI fixture 中 4 项失败，证明缺少代码/业务分层展示、重载签名、方法图标和刷新替换语义；业务导出路径断言原已通过。
- 第二轮：新增搜索/源码上下文测试及刷新状态回调顺序测试，11 项中 7 项失败。6 项指向尚未实现的搜索、源码和刷新生命周期；另一项来自 light fixture 未提供 java.lang.String 的类型解析，加入显式最小 JDK 类 stub 后再验证。
- 第三轮联合凭据/首页 RED：`ApiTreeNavigationTest` 7/7、`ApiTreeRefreshTest` 4/4 通过，0 error/skip；联合命令整体失败来自其他 change 的 RED。
- 第四轮 `./gradlew test --tests '*Welcome*Test' --tests '*ApiTreeNavigationTest' --tests '*ApiTreeRefreshTest'`：导航两类仍为 11/11 通过；首页 5 项中 3 项是预期 RED，因此整体 exit 1，不能称全量通过。该轮增量表单 instrumentation 有 `Class to bind does not exist` 警告，后续全量 `--rerun-tasks` 必须确认表单可正常创建。

XML 证据保留在 `/tmp/doc-view-reliability-20261018/navigation-red.xml`、`round2-TEST-*ApiTree*.xml`、`round3-TEST-*ApiTree*.xml`、`round4-TEST-*ApiTree*.xml`。临时目录不作为仓库交付附件。

用例覆盖代码主文字/灰色业务名、方法图标、重载、无说明不重复、业务文档路径不变、重复更新、组合搜索文本、失效 PSI 禁止源码跳转、最新刷新结果胜出、旧错误/取消不回写、失败保留树、空结果提示和 dispose 后迟到结果忽略。刷新测试控制真实 Swing panel 的终态回调顺序，原生工具窗口显示/键盘行为仍需单独验收。

## IU262 原生局部检查

在 IU-262.10968.63 隔离 profile（Doc View 1.3.14）实际打开 Doc View Tool Window：新窗口默认位于左侧；树中类/方法代码名作为主文字、业务说明以灰色次文字显示，重载签名可区分；在 Dubbo 方法节点按 Enter 后打开对应 Scratch 文档。该轮只证明这些可见展示和 Enter 路径。现场摘要及 profile/package 记录文件存在：`/tmp/doc-view-reliability-20261018/native-acceptance-progress.md`、`/tmp/doc-view-reliability-20261018/round24-native-package.log`。

## IU262 后续速度搜索、源码导航与主题单项

展开 `UserFacade` 后输入 `queryUserAccount`，实际速度搜索界面显示命中 `queryUserDateAccount`；截图：`/tmp/doc-view-reliability-20261018/native-tree-speed-search.png`。按 Escape 后对选中节点执行 F4，IDE 真实打开 `UserFacade.java` 并定位到单参数 `queryUserDateAccount(String)` 重载第 26 行；辅助功能树和编辑器状态栏记录见 `/tmp/doc-view-reliability-20261018/native-tree-source-navigation-ax.txt`，画面见 `/tmp/doc-view-reliability-20261018/native-tree-source-navigation.png`。

另以 Ctrl+grave 将主题从 IslandsDark 切到 IslandsLight，目视确认快速开始正文及 API 树主文字/灰色次文字可读，然后恢复 IslandsDark。截图 `/tmp/doc-view-reliability-20261018/native-help-light-theme.png` 记录浅色主题下的页面与目录。该项只证明这一次主题切换和恢复，不代表完整主题矩阵通过。

## 待补证据

本批全量测试、Plugin Verifier 与插件包检查已通过（见末节）；原生速度搜索与 F4 源码跳转已有上述局部证据。重复刷新/隐藏后完成、索引与空结果状态、Move To、多项目/深层导航和完整主题矩阵仍未完成。未实际执行的内容不计为通过；无需第三方账号。

## 本批全量回归与平台检查

- `./gradlew test --rerun-tasks`（round7）：96 tests，0 failures、0 errors、0 skipped；其中 ApiTreeNavigation 7 项、ApiTreeRefresh 4 项通过。原始 XML 保留为 `/tmp/doc-view-reliability-20261018/round7-TEST-*.xml`。本轮全量重新编译与 form instrumentation 正常，表单实例回归通过。
- 首次 `verifyPluginProjectConfiguration buildPlugin verifyPlugin`：前两项通过，Verifier 因 3 处 deprecated API 失败；树刷新中的 `ReadAction.compute` 已改为公开 Java `Application.runReadAction(Computable)`，保持后台 PSI 快照与取消检查。最终复跑结果另记。

## 第 1～7 项整批自动门禁通过

2026-10-18 执行 `./gradlew test verifyPluginProjectConfiguration buildPlugin verifyPlugin --rerun-tasks`，18 个任务实际执行并成功；96 tests，0 failures/errors/skipped。本项相关用例全部通过。IU-261.22158.277 Verifier 为 Compatible，无 deprecated、experimental 或 internal API 使用报告。

原始 XML `/tmp/doc-view-reliability-20261018/round8-TEST-*.xml`；报告备份 `verifier-1-7-green/`。该阶段 ZIP SHA-256 为 `b9e1365161403069ec251311b7403ab8f27caaff943ebd2bd2efae2f91997b1d`，副本 `Doc-View-1-7-green.zip`。包仅含生产 jar；descriptor 为 1.3.14 / since-build 261，Java/Markdown 依赖正确，无测试框架/测试 class，7 份帮助 Markdown 与仓库源字节一致。

Round 8 所在阶段的原生验收因 Mac 锁定而跳过；这是当时阶段的记录。后续 IU262 局部操作见上节；该 SHA 仍仅对应 Round 8 包。

## 类节点顺序 targeted 回归通过

round37 全量门禁运行 191 项、1 项失败：`GenerationPerformanceBaselineTest.testDirectoryScanBaselineSeparatesNormalInterfaceSetting`。重复目录扫描返回相同类成员但顺序不一致，定位为 `AllClassesSearch.findAll()` 结果没有稳定迭代顺序。为满足目录展示的可重复顺序，本 change 增加“限定名、源文件路径、源码偏移量”的类排序契约，同时保留扫描成员和类内方法声明顺序，并新增真实 PSI 回归用例。

round38 targeted 中 `ApiTreeNavigationTest` 8/8、`ApiTreeRefreshTest` 4/4 与 `GenerationPerformanceBaselineTest` 8/8 通过，round40 `ClassNodeCancellationTest` 1/1 通过。round38 的唯一失败来自 `ClassNodeCancellationTest` 旧断言要求保留尚未完成构造的局部 `MethodNode`；它不属于上述三个已通过测试，已在测试及性能 change contract 中修正。排序用例覆盖真实 PSI 下类名排序、多次扫描一致、类内方法声明顺序不变；刷新回归同时确认目录成员语义保持。证据对应 round38 targeted XML `/tmp/doc-view-reliability-20261018/round38-TEST-com.liuzhihang.doc.view.ui.window.ApiTreeNavigationTest.xml`、`round38-TEST-com.liuzhihang.doc.view.ui.window.ApiTreeRefreshTest.xml`、`round38-TEST-com.liuzhihang.doc.view.contract.GenerationPerformanceBaselineTest.xml`。此记录完成 API tree 的类顺序 targeted 项，不代表新一次全量 Gradle 门禁或额外 IU262 原生场景已通过。

## Round42 整批复验（2026-10-18）

主代理完整执行 `./gradlew test verifyPluginProjectConfiguration buildPlugin verifyPlugin --rerun-tasks`：40 个 suite、196 项测试，0 失败、0 错误、0 跳过；构建成功，IU261.22158.277 Plugin Verifier verdict 为 `Compatible`。包括本 change 的最新修改及 Round41 的取消/释放守卫和真实 Scratch 文件失败用例。日志与精确 XML：`/tmp/doc-view-reliability-20261018/round42-fullgate.log`、`round42-fullgate-TEST-*.xml`；包审计为 `round42-package-audit.json`。

Round42 ZIP SHA-256：`e214cd4525968356c936f798191006391415019afe48ee4e5235c02f342372d7`。7 个离线章节与仓库正文逐字节一致，生产包未包含测试框架、夹具或 MCP PoC。测试运行使用 JBR25，生产目标仍为 Java21；构建配置中跳过的 searchable-options 任务不计为测试跳过。完整包与原生已安装版本的区别见 [路线图](../../../docs/roadmap.md#本轮验证与续作入口)。

macOS 锁屏阻挡后续原生操作。IU262 测试实例仍安装 Round34，尚未安装或操作 Round42；本节自动验证不能替代新包原生双击/Enter、项目关闭、多项目隔离、主题及窗口矩阵验收，相关任务保持未完成。

## Round 46 普通接口开关与树结果局部核对

Round 46 已退出旧包，并在 IU 262 隔离 profile 安装 Round 42 插件 JAR（SHA-256：`993fdc650c5b2f0a4a4aa0c45d292c8130051c1938df4df36f63b1263019a2fc`）。在 `doc-demo` 关闭普通接口识别并刷新后，树中显示 `User2Controller`、`UserController`、`XmlDubboService`；重新开启并刷新后，树中恢复为五项，增加 `AnnotationDubboService` 和 `UserFacade`。证据：`/tmp/doc-view-reliability-20261018/native-round46-normal-interface-off-tree-ax.txt`、`native-round46-normal-interface-restored-ax.txt`。此项只验证该项目这两种状态下的目录结果。

另一次 Refresh、Hide、Reopen 操作只核对了最终树状态，没有记录 Hide 发生时刷新任务仍在运行，因此不声称验证了隐藏动作取消进行中刷新或迟到结果处理。Round 46 安装的仍是 Round 42 JAR；此后正在进行的全插件图标和 methodName-only 树展示改动不在该包中，Round 46 的截图和辅助文本不能作为该新实现的原生证据。

本轮后，Round 42 章节中的“测试实例仍安装 Round34、尚未操作 Round42”仅描述当时状态；已由上述 Round 46 安装与局部核对更新。跨项目完整隔离、刷新进行中隐藏、Move To、多项目/深层导航、完整主题矩阵及新树展示仍按各自未完成范围处理。

## Round 47 methodName 展示与图标 targeted 回归

在 2026-10-18 对本 change 修改的真实 PSI 与图标契约执行两轮针对性测试。首轮 RED 命令为 `./gradlew test --tests '*ApiTreeNavigationTest' --tests '*PluginIconContractTest' --rerun-tasks`：13 tests、7 failures、0 errors；两项旧方法行签名断言失败，五项图标断言命中旧 `DocViewIcons` 操作字段、缺少 iconMapper/尺寸资源及两个导出按钮并列。随后实现 methodName-only 主文字、签名和方法说明 tooltip、完整搜索文本、平台动作图标、尺寸/主题品牌资源、iconMapper base 映射及 Export popup，再以同一命令取得 GREEN：`ApiTreeNavigationTest` 8/8、`PluginIconContractTest` 5/5，0 failures、0 errors、0 skipped。日志与 XML：`/tmp/doc-view-reliability-20261018/round47-icons-red/` 和 `/tmp/doc-view-reliability-20261018/round47-icons-green/`。

图标契约测试逐项反射确认 plugin.xml 的 `AllIcons` 字段存在于 IU 261 SDK，验证所有带图标 action/group 不再引用通用自绘资源、Tool Window 使用独立 13×13 品牌图、New UI tool-window/gutter 两个 base path 通过 `DocViewIconMappings.json` 登记、深浅与尺寸变体文件存在、plugin logo 为 40×40，并确认两个原 action ID 保留且只有一个 toolbar Export popup。New UI 映射按平台示例只保留两个 base 条目，尺寸及 `_dark` 变体由图标加载按约定文件名选择，避免多条变体映射重复指向同一旧路径。真实 PSI 用例同时覆盖重载独立、灰色业务次文字、无说明回退、完整 tooltip/搜索及既有业务文件路径。

这次是 targeted 测试，不是全量测试、Plugin Verifier 或原生 IDEA 验收。新 New UI/浅深主题图标、gutter、通知、Preview/参数编辑器与导出 popup 均需在新构建原生检查；Round 46 证据使用旧 JAR，不可替代。相关 fullgate 留待主代理执行，任务 3.1/3.3 与原生任务继续保持未完成。

## Round 49 Preview 工具栏主题配色问题与修正

原生 Round 49 在深色 IDE 界面和浅色 editor scheme 组合下发现 PreviewForm 底部动作图标背景变白、浅色平台图标对比不足；截图 `/tmp/doc-view-reliability-20261018/native-round49-preview-dark.png`。源码确认左右工具栏组件及承载面板的四处背景都取自 `markdownEditor.getBackgroundColor()`，因此编辑器配色覆盖了 IDE 工具栏表面。

本轮将这四处 toolbar/面板背景改为 `UIUtil.getPanelBackground()`。Markdown 编辑器的创建、高亮器和内容配色未修改。Round49 当时尚未运行测试或 Plugin Verifier，也没有在新包上复验截图；后续自动验证见下节。深色 IDE/浅色 editor scheme 及反向组合仍保留为原生验收项（tasks 4.6）。

## Round 48–50 自动门禁与包审计

后续自动验证分轮完成，不能合并描述为同一轮重新运行全部测试：

- Round48 执行 `./gradlew test verifyPluginProjectConfiguration buildPlugin verifyPlugin --rerun-tasks`，41 suites、202 tests，0 failures、0 errors、0 skipped；IU-261.22158.277 Plugin Verifier verdict 为 `Compatible`。日志与 XML：`/tmp/doc-view-reliability-20261018/round48-fullgate.log`、`round48-fullgate-TEST-*.xml`。
- Round49 针对全插件图标运行 5 项测试并通过，构建及 IU261 Plugin Verifier 为 `Compatible`。此轮不是全量测试；图标测试日志/XML及构建证据位于 `/tmp/doc-view-reliability-20261018/round49-*`。
- Round50 为 Preview 工具栏/面板背景主题修正补做 targeted 验证：`PreviewLifecycleTest` 与 `PluginIcon*Test` 共 17 tests，0 failures、0 errors、0 skipped；构建成功，IU261 Plugin Verifier 为 `Compatible`。日志、XML 与包审计：`/tmp/doc-view-reliability-20261018/round50-preview-theme.log`、`round50-TEST-*.xml`、`round50-package-audit.json`。

Round50 ZIP SHA-256 为 `c692e2436662756f1b69cff45a915016276501602b51f2545620c3a47f8566ef`，JAR SHA-256 为 `2b3054fabdc608499a249901594b17fb8460c1f5e78bed2723e71e610391e265`。包审计确认 7 份离线指南与仓库文件逐字节一致，且未包含测试框架。Round48 是本记录所述最近一次全量 202-test 门禁；Round49/50 是后续 targeted 测试与各自构建/Verifier，未再次运行全量 202 tests。

上述是 Round50 自动证据当时的状态，不能代替原生 IDEA 场景；Round50 后续原生主题组合已记录在文末，任务 4.5 的其余图标矩阵仍待核验。任务 3.3 等主代理完成最终范围和 OpenSpec 检查后再勾选。

## Gutter 图标与全局 editor scheme

主代理在原生复验中观察到：深色 IDE UI theme 搭配浅色全局 editor scheme 时，固定的浅灰 M↓ gutter 图标对白色 gutter 背景对比不足。IU261 的 `LineMarkerProvider.getLineMarkerInfo(PsiElement)` 不提供当前 editor 参数，`LineMarkerInfo`/`GutterMark` 也没有按 editor 读取 scheme 的入口；公开 `EditorColorsManager.isDarkEditor()` 按全局 scheme 背景判断明暗，`IconLoader.getDarkIcon(Icon, boolean)` 可为缓存图标选择对应明暗变体。目标 IU261 SDK 与 `idea/261.22158.277` 官方源码均未将这两个方法标记为 internal、experimental 或 deprecated。

两处类/方法 gutter marker 现使用全局 scheme 深浅选择图标变体。该记录写入时尚未运行 Gradle、未在新包复验；Round51 随后完成了定向自动验证并开始新包原生验收（见下节）。逐 editor scheme override 不在此实现保证范围内，任务 4.7 仍待主代理在当前包完成原生验证。

## Round49–51 原生局部验收与最新包状态

### Round49：API 树和弹出入口

在 IU262 原生窗口中确认类名/方法名为主文字、业务说明为灰色次文字；`UserFacade` 两个重载方法显示为独立节点，辅助功能文本中的 tooltip 保留完整参数签名。证据：`/tmp/doc-view-reliability-20261018/native-round49-tree-dark-full-ax.txt`。Export 工具栏菜单实际显示 `Export Markdown` 和 `Export Project Docs` 两项（`native-round49-export-menu-dark-ax.txt/.png`）；参数编辑器 popup 图标可读（`native-round49-param-editor-dark-ax.txt/.png`）。

### Round50：签名搜索、Preview 主题与可见图标

在速度搜索中输入 `com.liuzhihang.doc.dto.UserReqVo`，实际命中 `respGenericTest`；见 `native-round50-signature-search-ax.txt/.png`。PreviewForm 在深色 IDE/浅色 editor scheme 和浅色 IDE/深色 editor scheme 两种组合下均保持工具栏清晰，Markdown 正文继续使用对应 editor scheme；实际切换渲染模式后成功显示 Markdown。截图与辅助文本：`native-round50-preview-dark.png/-ax.txt`、`native-round50-preview-light.png/-ax.txt`、`native-round50-preview-toggle.png/-ax.txt`、`native-round50-light-ui-dark-editor.png`。

浅色 Compact Mode 下展开 API 树，方法行和 stripe 图标可辨；Plugin Manager 中新的 40×40 logo 可见。证据：`native-round50-compact-light-tree.png/-ax.txt`、`native-round50-plugin-logo.png/-ax.txt`。尝试打开右键菜单及 Upload 子菜单时浮层未能保持打开，不能据此判断其菜单图标已通过；通知图标完整主题矩阵也未验。因此 4.5 保持未勾。

### Round51：gutter 修正自动验证和当前原生状态

Round51 针对全局 editor scheme 选择 gutter 明暗变体运行 5 项图标测试，0 failures/errors/skipped；构建成功，IU-261.22158.277 Plugin Verifier verdict 为 `Compatible`。日志、XML 与包审计：`/tmp/doc-view-reliability-20261018/round51-gutter-theme.log`、`round51-TEST-com.liuzhihang.doc.view.contract.PluginIconContractTest.xml`、`round51-package-audit.json`。Round51 ZIP SHA-256 为 `147e8ca9840f2336c68cba44462066a473f03f2f2c92a5160add5b9eba22a977`，JAR SHA-256 为 `2b002b0c7bf833b860162b5af64dcb0a0159240c885b1b9261f66f5a08cfe158`；审计确认 7 份离线指南字节一致、安装 JAR 与产物一致，未包含测试框架或 MCP PoC。

Round51 随后完成下面记录的 gutter 原生核对与普通实例恢复；Round49/50 仍分别对应各自安装包的原生证据，不混为同一轮测试。


### Round51：行标记、退出与恢复

在 IU262 隔离 profile 安装并校验 Round51 JAR 后，分别核对浅色 IDE/深色全局 editor scheme、深色 IDE/浅色全局 editor scheme。类与方法 gutter 使用对应 editor scheme 的变体；切换后重新观察，图标与正文背景可区分。截图为 `/tmp/doc-view-reliability-20261018/native-round51-light-ui-dark-editor.png` 和 `native-round51-dark-ui-light-editor.png`。点击第34行方法标记，实际打开 `UserController.respGenericTest` 文档，正文包含 `/user/respGenericTest` 与 `POST`；记录为 `native-round51-gutter-click-ax.txt`。该验收不覆盖逐 editor 的独立配色覆盖。

测试实例经原生 Confirm Exit 正常退出后，按原始 manifest 恢复 `doc-demo` 的两个既有 Doc View 设置文件；本轮新增且为空的 ShowDoc 设置及24个生成文件移入可恢复临时备份。34个既有 tracked 文件与本轮基线逐字节一致，项目 HEAD 未变。证据为 `round51-cleanup.json`，未记录凭据内容。

普通 IDEA 已重新打开 `/Users/liuzhihang/workspace/doc-view`，原生窗口为 `doc-view – roadmap.md [doc-view]`；进程打开普通 `IntelliJIdea2026.2` 日志路径，隔离测试进程已关闭。证据为 `round51-ordinary-restored.json`、`native-round51-ordinary-restored-ax.txt`。普通实例未安装本轮测试包。

Round50 启动段日志（13:02:02 至13:07:19）没有 ERROR/SEVERE 或图标映射异常；全日志中04:00的旧 Expand/EDT慢操作报告不归入此轮。目录右键菜单、Upload 子菜单和通知完整主题矩阵仍保留待验，不因资源与 descriptor 测试通过而勾选4.5。没有提交、推送、发布、sync 或 archive。

## Round52：目录 Upload 禁用复现与上下文修复

2026-10-18 在 IU262 隔离 profile、`doc-demo` 上继续验收，开始时安装的是 Round51 JAR，SHA-256 已重新核对。用户手动在 `respGenericTest` 上打开右键菜单，并提供截图：Jump to Source、Open、Export、Upload、Clear Cache、Http Client 均可见，深色主题图标与文字可辨，但 Upload 灰显。原始截图备份为 `/tmp/doc-view-reliability-20261018/round52/user-context-dark-upload-disabled.png`。这只证明当时的方法节点深色菜单状态，不证明类节点、浅色菜单或上传子菜单已通过。

Computer Use 的右键、Ctrl-click、Show Context Menu 与临时快捷键尝试均未得到保持打开的目录菜单，IDEA 自带编辑器右键也出现相同观察结果；Export 工具栏 popup 可打开。用户手动打开菜单后，工具读取仍只捕获主窗口。`manual-context-dark.png/-ax.txt` 文件名表示尝试读取用户手动菜单的记录，文件内没有菜单，不能作为通过证据。工具控制/捕获受阻与实际 Upload 禁用是两件事，未据此修改 popup 关闭逻辑。

源码确认：三种 `CatalogUpload*Action` 继承的 `AbstractAction.update()` 要求 EDITOR、PSI_FILE，但树 DataContext 不提供它们，而目录 actionPerformed 只需要项目、树和所选节点。修复在目录动作基类覆写 update，以项目有效、非 Dumb Mode、目录树存在控制可用状态，保持 BGT，不读取 Swing 选择、不生成文档或访问平台。同继承链对工具窗口 Upload 的影响一并纳入 proposal/design/spec/tasks；上传执行和 payload 未改。实际 Preview 由 `PreviewForm` 动态创建动作，本次未将旧注册类的测试冒充为实际 Preview 行为修复。

目录定向验证使用 JBR25，命令 `JAVA_HOME='/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home' ./gradlew test --tests '*CatalogUploadActionTest' --rerun-tasks`。RED 为3 tests、1 failure，正向无编辑器上下文用例命中旧行为；GREEN 为3 tests、0 failures/errors/skipped。覆盖三平台可用、缺项目/树、真实 Dumb Mode，并用抛错树桩保证 update 不读取选择。证据为 `/tmp/doc-view-reliability-20261018/round52-upload-{red,green}.log/.xml`。其他自动门禁与新包原生结果在后续段落单独记录。

### 最终自动门禁与包

工具窗口的初始 RED 位于 `round52-toolbar-upload-red.log` 及对应 XML，失败断言明确为 `WindowUploadAction should be enabled for its own data context`。后续将测试收窄、重命名为 `WindowUploadActionTest`，定向3 tests 全通过，覆盖无 EDITOR/PSI_FILE、空 root 仍可用、缺 project/root/toolbar、已销毁项目与 Dumb Mode。见 `round52-window-upload-green.log` 和 `round52-window-upload-green-TEST-*.xml`。两个 Preview 旧 action 的本轮实验改动已撤回，并与 Round52 开始时 SHA-256 逐一核对一致。

最终命令为 `JAVA_HOME='/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home' ./gradlew test verifyPluginProjectConfiguration buildPlugin verifyPlugin --rerun-tasks`：43 suites、208 tests，0 failures/errors/skipped，构建成功，IU-261.22158.277 Plugin Verifier 为 `Compatible`。该结果对应 `/tmp/doc-view-reliability-20261018/round52-final-fullgate.log` 和 `round52/final-TEST-*.xml`；之前的 `round52-intermediate-fullgate.log` 是收窄范围前的中间包，不作为最终包证据。编译仍有既有警告，searchable-options 任务依配置跳过，不将其混同为测试跳过。

最终 ZIP SHA-256 为 `c8bfd5d0713bf94f0cac6484ec6ddcc1875549f71a70f035a43d5945e2bef329`，JAR 为 `e6ab05becb2ff7dcf4656dcea020d78c57166c9d80a5b16ae56c0cd872bcfbee`。6份用户指南和 CHANGELOG 共7份打包帮助逐字节匹配，未包含测试类或 JUnit，隔离 profile 安装 JAR 与构建产物一致；见 `round52/package-audit.json`。

### 最终包原生复验

IU262 隔离 profile 于13:37:21启动。打开 Doc View 后工具窗口 Upload 按钮实际出现；点击后列表显示 `Upload to YApi`、`Upload to ShowDoc`、`Upload to 语雀`，深色主题文字清楚。按 Esc 返回 `doc-demo – UserController.java`，未选择平台、未跳转设置或出现上传结果。证据为 `round52/window-upload-dark.png/-ax.txt` 和 `window-upload-dark-escape-ax.txt`。这验证的是工具窗口 Upload 列表，不替代目录右键 Upload 或 Preview Upload。

新包类节点右键的 Computer Use 尝试仍只返回主窗口，未据此判定产品失败。用户随后在当前测试实例手工确认：UserController 类和 respGenericTest 方法的右键 Upload 均可展开 YApi、ShowDoc、语雀，按 Esc 可返回；实际 Preview 的 Upload 也显示三平台，按 Esc 后预览保留，没有选择平台或发送文档。用户提供的方法右键展开截图与 Preview 列表截图分别备份为 `round52/user-context-upload-dark-passed.png`、`round52/user-preview-upload-dark-passed.png`。工具在用户确认后读取到仍打开的 Preview（`preview-after-manual-confirmation-ax.txt`），然后按 Esc 返回主窗口。类节点和取消行为以用户手工确认作为证据，不冒充工具捕获。

上述截图均为深色 IDE 菜单；Preview 的白色正文来自浅色 editor scheme，不能当作浅色 IDE 主题验收。因此4.5.c已完成，4.5.a/b的深色部分通过，浅色部分及通知等完整图标矩阵仍未完成，父任务4.5保持未勾。主题选择弹层在 Computer Use 操作后同样未能保持，未取得新包浅色菜单证据，不沿用旧包截图补足。实际平台上传及返回链接未在本轮执行。

### 测试实例退出与项目保护

测试实例通过原生 Confirm Exit 正常退出，进程检查确认已停止。`doc-demo` 的34个既有 tracked 文件及 HEAD 与 Round52 开始时一致，两个既有 Doc View 设置文件也逐字节相同，本轮无新增 Doc View 设置文件，无需覆盖恢复。临时 Show Context Menu keymap 已在此前测试停止时移出隔离 profile，并恢复其平台默认 keymap；普通配置未改。清理审计见 `round52/cleanup.json`。从最终包启动13:37:21至正常退出的日志段没有 ERROR/SEVERE；旧启动段的错误状态不归入本轮。


普通 IDEA 已重新打开 `doc-view – roadmap.md [doc-view]`，仅一个 IDEA 主进程运行，其日志路径是普通 `~/Library/Logs/JetBrains/IntelliJIdea2026.2/idea.log`，没有打开隔离日志。证据为 `round52/ordinary-restored.json` 和 `ordinary-restored-ax.txt`。隔离 profile 正常退出后没有保留 active_keymap 覆盖或临时 keymap 文件；备份仍在 `round52/keymap-backup/`。普通实例未安装测试包，仓库 HEAD 仍为 `00f1f944c7b22cc03219f06324d177fecf9e848f`；未提交、推送、发布、sync 或 archive。

## Round53：浅色弹出列表和通知图标原生验收

2026-10-18 使用同一 Round52 最终包，在 IU262.10968.63 / JBR25 的隔离 profile 打开专用 `doc-demo`。ZIP 与安装 JAR 的 SHA-256 已重新核对，分别仍为 `c8bfd5d0713bf94f0cac6484ec6ddcc1875549f71a70f035a43d5945e2bef329`、`e6ab05becb2ff7dcf4656dcea020d78c57166c9d80a5b16ae56c0cd872bcfbee`。本轮没有发现新的图标缺陷，没有改生产代码，也未重跑 Round52 的208项全量测试或构建。以下证据均位于 `/tmp/doc-view-reliability-20261018/round53/`，不是长期发布附件。

### 通知图标：两种 IDE 主题均通过

通过原生 Theme 动作切换 Islands Light 与 Islands Dark；editor scheme 可以与 UI 明暗相反，通知判断以 IDE 界面背景为准。

| 通知 | 实际触发方式 | 原生结果及证据 |
| --- | --- | --- |
| 上传成功的 Doc View 品牌图标 | 当前方法执行 Upload To ShowDoc，仅向本机 loopback HTTP 夹具发送请求，夹具返回成功 | 浅色背景使用深灰 M↓，深色背景使用浅灰 M↓，标题、正文与操作链接可读；`notification-brand-{light,dark}.png/-ax.txt` |
| ERROR | 同一本机夹具返回受控错误 | 红色错误图标与“接口上传 ShowDoc 失败”正文在两种主题可辨；`notification-error-{light,dark}.png/-ax.txt` |
| INFORMATION | Export Project Docs 首次成功及重复导出 | 蓝色信息图标可辨；首次导出23接口、24文件，重复导出显示内容未变化；`notification-info-{light,dark}.png/-ax.txt` |
| WARNING | 临时在导出目标放置不带所有权标记的索引，触发实际冲突保护 | 黄色警告图标与取消原因可辨；未覆盖临时索引，浅色复验时其余23份接口页字节不变；`notification-warning-{light,dark}.png/-ax.txt`、`generated-before-conflict.json` |

ShowDoc 测试前原生设置状态为两个凭据均未配置。只保存本机夹具使用的虚构测试值；共4次请求，成功/错误各2次，`requests.jsonl` 仅记录方法、路由、字节数和响应模式，不记录请求正文或凭据。没有连接真实 ShowDoc/YApi/语雀，没有打开通知中的平台链接；该结果证明真实插件通知 UI，不代表真实平台上传与返回链接已验收。

### 浅色菜单：工具证据与用户手工确认

工具窗口 Export 显示 `Export Markdown`、`Export Project Docs`，两个平台导出图标清晰；工具窗口 Upload 显示 YApi、ShowDoc、语雀三项，按 Esc 返回主窗口。证据为 `toolbar-export-light.png/-ax.txt`、`toolbar-upload-light.png/-ax.txt`。

在 `UserController.respGenericTest` 使用实际 Doc View 预览动作打开 Preview，再点击其 Upload，浅色列表三平台文字完整。按 Esc 后 Preview 仍在，正文及 Upload/Export/Copy 工具栏保留，没有选择平台或发送额外请求。证据为 `preview-upload-light.png/-ax.txt`、`preview-upload-light-escape-ax.txt`、`preview-after-upload-light-escape.png`。

类节点右键的工具触发仍立即收起，工具未取得浅色目录右键及其 Upload 子菜单截图。首轮记录因此保留待验；用户随后针对本轮 Islands Light 的完整手工核对问题回复“核对没问题”，确认 UserController 类、respGenericTest 方法的右键菜单图标/文字清晰，Upload 子菜单三平台选项正常、Esc 可返回，方法 Preview Upload 列表也正常；操作范围为只展开和取消，不选择上传平台。

据此补齐4.5.a/b，4.5.c追加浅色取消行为确认。该证据是用户手工反馈，没有新增浅色目录菜单截图，不冒充工具捕获，也不使用深色截图替代。4.5.d/e仍分别记录通知和工具可操作列表；父任务4.5涉及更广的 New UI/Compact Mode、参数编辑器等组合，保持未勾，不将本次确认扩展为未执行场景已通过。未修改 popup 关闭行为来规避控制工具限制。

### 清理、恢复与日志

在原生 ShowDoc 设置页清空测试地址，勾选两项清除凭据并 Apply，状态读回为 `API Key: 未配置；API Token: 未配置`，见 `showdoc-after-cleanup-ax.txt`。停止仅监听 loopback 的 HTTP 夹具。测试实例通过原生 Confirm Exit 正常退出后，将本轮新增的空 ShowDoc 设置和24份生成文档移入 `cleanup-backup/`；34个既有 tracked 文件、HEAD、两份既有 Doc View 设置与本轮基线逐字节一致，无新增 untracked 源码文件，见 `cleanup.json`。未覆盖已有源码或原设置。

普通 IDEA 已恢复窗口 `doc-view – roadmap.md [doc-view]`。进程核对只剩一个 IDEA 主进程，打开普通 `~/Library/Logs/JetBrains/IntelliJIdea2026.2/idea.log`，没有打开隔离日志；见 `ordinary-restored.json` 和 `ordinary-restored-ax.txt`。普通实例未安装测试包。

13:59:02启动至14:08:34退出日志段没有 ERROR/FATAL/SEVERE 或 Doc View 异常栈帧；平台启动的 WARN/INFO 另行保留，不宣称日志零警告，见 `log-summary.json`。本轮只更新路线图、tasks 和本验收记录，未提交、推送、发布、sync 或 archive。

## Round54：Move To、刷新与隐藏窗口

2026-10-18 在 IU262.10968.63 / JBR25、Islands Light 的隔离 profile，继续使用专用 `doc-demo` 和 Round52 最终包。ZIP 和已安装 JAR 的 SHA-256 与 Round52 记录相同；本轮未修改生产代码、资源、测试或构建配置，也未重跑208项测试、构建或 Verifier。原生证据与临时诊断程序位于 `/tmp/doc-view-reliability-20261018/round54/`，不是永久发布附件。

### Move To、重启与右侧导航

Computer Use 能读取 IDEA 原生 Move To 菜单，但点击后菜单收起且窗口未移动；未将该控制限制判为插件缺陷。用户手动执行 Move To → Right Top 并回复“已移到右侧”，工具随后独立核对右侧工具栏和实际窗口，见 `move-right-confirmed.png/-ax.txt`。正常退出测试实例后重新打开同一项目，Doc View 入口仍在右侧，点击后目录也在右侧，见 `right-after-restart.png/-ax.txt`。

右侧方法导航使用原生键盘选中临时接口的 `refreshAlpha`，再执行当前 macOS keymap 的 Jump to Source（⌘↓）；实际打开 `DocViewNativeRefreshFixture.java`，导航栏为 `refreshAlpha`，编辑器定位第6行12列，见 `right-navigation-passed.png/-ax.txt`。此前行点击/F4的尝试没有确认目标方法选择，保留为尝试记录，不作为成功证据。停靠恢复及普通实例恢复结果在本轮收尾段记录。

### 连续刷新及新结果优先

原生完整展开目录，基线为5个类/接口、23个方法。连续执行3次 Refresh，完成后再完整展开；类、方法、说明及顺序均与基线一致，没有累积重复节点。证据为 `baseline-expanded-all-ax.txt`、`refresh-{1,2}-ax.txt`、`refresh-3-start-ax.txt`、`refresh-3-complete-all-ax.txt`。

在专用项目临时新增普通接口 `DocViewNativeRefreshFixture`，含 `refreshAlpha`、`refreshBeta`，使用 IDEA 的 Reload All from Disk 同步。随后通过线程级断点暂停一次已构建快照但尚未进入完成回调的旧请求（generation=3），在源码中增加 `refreshGamma`，再次原生同步并 Refresh。新请求完成后，实际目录出现三个方法；再恢复旧线程，目录仍有三个方法且“正在查找接口…”消失，没有被旧快照覆盖。见 `overlap-old-refresh-pending-ax.txt`、`overlap-new-complete-ax.txt`、`overlap-old-released-final.png/-ax.txt`。

临时源码随后移入可恢复证据目录，刷新后目录回到原有5个类/接口、23个方法，完整节点内容及顺序再次与基线一致；见 `cleanup-restored-all.png/-ax.txt` 和 `measurements.json`。既有源码未被覆盖。

### 刷新期间隐藏与重新打开

使用本机 JDI 连接隔离测试 JVM 的 loopback 端口50254，断点位于已安装 JAR 的 `DocViewWindowPanel$2.run` 第148行、BCI46。已对实际安装 JAR 的字节码核对：`runReadAction` 已返回，`result` 已写入，此时尚未执行 `onSuccess`。只暂停事件线程（`ApplicationImpl_pooled_thread_6`），不暂停 EDT、不持有 PSI 读锁，不调用目标方法、不改字段，不在插件里增加延迟或诊断分支。所有界面操作仍通过 Computer Use。

generation=2 在06:30:16 UTC命中断点后，原生界面显示“正在查找接口…”；点击工具窗口 Hide 后，Doc View 主面板消失、后台仍显示 Doc View Searching，焦点在 `UserController.java`。06:30:30 UTC恢复线程，后台任务结束，主面板保持隐藏、编辑器焦点不变。再次点击右侧 Doc View 并展开，看到新增接口及两个方法，证明隐藏期间的真实结果已发布。见 `hidden-test-refresh-pending-ax.txt`、`hidden-while-refresh-paused.png/-ax.txt`、`hidden-after-refresh-complete.png/-ax.txt`、`hidden-refresh-new-fixture.png/-ax.txt`。

这是受控调度下的真实 IDEA 后台任务与原生窗口验收；不声称测量自然刷新耗时或完成性能压力测试。诊断辅助程序于06:32:07 UTC正常 detach，未遗留暂停线程。它只保存在临时证据目录，不进入插件或仓库。

本轮完成的刷新/隐藏与右侧导航不覆盖索引中状态、空结果、多项目/深层导航或完整主题矩阵；聚合任务3.2.d.c与4.5仍保持未勾。

### 清理、恢复与边界

临时验证源码已移入证据目录，原生 Reload All from Disk 与 Refresh 后恢复原有目录。测试实例通过原生 Confirm Exit 正常退出；34个既有 tracked 文件、项目 HEAD、两份原有 Doc View 设置及 untracked 清单与本轮基线一致，没有新增 Doc View 设置，见 `cleanup.json`。JDI 已 detach，测试进程退出后端口50254没有监听。14:21:54首次启动至本轮正常退出的隔离日志没有 ERROR/FATAL/SEVERE；平台 WARN/INFO 不据此宣称为零，见 `log-summary.json`。

工具执行 Move To → Left Top 和拖动恢复未改变右侧停靠，原生菜单还返回过失效/离屏元素错误。没有将这些工具尝试视为完成；已请求用户手工恢复，但本轮收尾时尚未取得该反馈，因此保留隔离测试 profile 的右侧布局并正常关闭实例，3.2.d.d当时仍未勾。已告知用户无需继续操作已关闭的测试窗口。此限制不影响已观察的向右移动、重启位置保留和右侧目录功能，不修改平台菜单或停靠代码来规避控制工具限制。

普通 IDEA 已恢复到本轮开始时的 `doc-view – ai-context-assessment.md [doc-view]` 窗口。进程审计确认仅一个 IDEA 主进程，打开普通 `~/Library/Logs/JetBrains/IntelliJIdea2026.2/idea.log`，未打开隔离日志，见 `ordinary-restored.json`、`ordinary-restored-ax.txt`。普通实例没有安装本轮测试包。

本轮相对开始时仅更新 `docs/roadmap.md`、本 change 的 `tasks.md` 与 `verification.md`；源码/资源及其他原有改动保持不变。`openspec validate api-tree-navigation --strict` 与 `git diff --check` 通过。未提交、推送、发布、sync 或 archive。

### 后续手工回复的对象核对

用户随后回复“已恢复到左侧”。再次只读核对时，唯一 IDEA 主进程仍为普通 `doc-view`，窗口为 `doc-view – ai-context-assessment.md [doc-view]`；其原生 AX 将 Doc View 入口列在 Left Toolbar 下，确认普通窗口已在左侧。隔离 `doc-demo` 此时已关闭；由该项目 `.idea/workspace.xml` 的 ProjectId 定位隔离 profile 对应 workspace，Doc View 的保存 anchor 仍为 `right`。因此记录用户反馈及普通窗口的实际状态，不把它作为隔离 `doc-demo` 已移回左侧的证据，3.2.d.d当时仍保留末项待验。未再次关闭普通窗口或改写布局 XML，未改变先前已通过的右侧移动、重启保留、刷新和隐藏结论。

### 用户最终手工验收确认

在上述对象核对说明后，用户明确回复“我已经手动测试完毕”。据此接受 Move To 移回左侧末项的用户手工验收确认，将3.2.d.d标记完成，并同步路线图。本次确认没有新增工具截图、隔离 profile 磁盘复核或重启测试，不将用户确认描述为工具独立验证，也不改变此前保存布局核对的历史事实。索引中状态、空结果、多项目/深层导航及完整主题矩阵未因此扩展为已验收，3.2.d.c与4.5继续保持未勾。

本次仅更新验收文档，没有修改运行时代码或操作 IDEA 窗口；普通 IDEA 保持此前恢复状态，未提交或发布。

## Round55：索引中、空结果与多项目深层导航

2026-10-18 在 IU262.10968.63 / JBR25、Islands Light 的隔离 profile 使用专用 `doc-demo`。仍为 Round52 最终包：ZIP SHA-256 `c8bfd5d0713bf94f0cac6484ec6ddcc1875549f71a70f035a43d5945e2bef329`，已安装 JAR SHA-256 `e6ab05becb2ff7dcf4656dcea020d78c57166c9d80a5b16ae56c0cd872bcfbee`。开始时保存工作区437个文件的 hash 与 Git 状态，保留所有既有未提交改动；原生 AX、截图、夹具及审计结果位于 `/tmp/doc-view-reliability-20261018/round55/`。

### 索引中刷新与恢复

通过 IDEA 的 Find Action 执行平台内部 `Enter Dumb Mode`，只用于隔离验收，不写入插件实现。平台日志在22:23:09进入 Dumb Mode，原生状态栏显示 `Running dumb mode…`。点击真实 Refresh 按钮后，Doc View 显示“等待 IDEA 索引完成…”，保留原目录，没有误报无接口；见 `dumb-refresh-attempt-ax.txt` 和同名 PNG。随后执行 `Exit Dumb Mode`，平台22:23:35退出；无需再次点击 Refresh，Doc View 自动显示“正在查找接口…”，完成后等待/查找提示消失并恢复5个类/接口，见 `index-exit-ax.txt`、`index-complete-ax.txt` 和 `log-summary.json`。

静态审查一度根据 `WindowRefreshAction extends AnAction` 推断索引中按钮会被禁用；实际原生点击证明当前 IU262 toolbar 路径可执行，平台 action 更新规则也区分工具栏与右键菜单，因此没有据此增加不必要的修复。此证据验证真实 IDE 的受控 Dumb Mode 等待与恢复，不代表大型项目自然索引时长或性能测试。

### 真实空结果与源码恢复

将两个 Controller 与三个已识别接口的5份源码按原路径移入可恢复备份，执行原生 Reload All from Disk 与 Refresh。真实扫描完成后，目录显示“未找到接口，请检查识别注解与普通接口扫描设置。”，原类/方法行在画面中清空；见 `empty-moved.json`、`empty-result-ax.txt` 和 `empty-result.png`。AX 仍保留无名称的 disabled 行壳，不将其误读为可见旧节点；以实际截图核对为空目录。

随后按原路径恢复5份源码，原生同步并 Refresh 后恢复原有5个类/接口；见 `empty-recovered-ax.txt`。全部夹具清理后再次 Expand 核对5个类/接口、23个方法，见 `cleanup-tree-ax.txt/.png` 和 `measurements.json`。34个既有 tracked 文件逐字节 hash 与本轮基线一致。空结果来自实际 PSI 扫描，没有替换 root 或调用测试回调。

### 同名接口隔离与深层导航

在 `doc-demo` 的深层包 `com.liuzhihang.doc.validation.deep.level.one.two` 临时新增 `DirectoryProbe`，含 `locate(String)`、`locate(long)`，业务标记为 `PROJECT_A_DOC_DEMO`。另在证据目录创建独立 `DocViewDirectoryPeer` Java 项目，使用相同限定名和两个方法、标记为 `PROJECT_B_PEER`；其 `peer-deep` 模块另外包含 `com.example.deep.navigation.DeepDirectoryProbe` 的两个 `read` 重载。两项目通过 New Window 同时打开；只在临时项目原生启用普通接口扫描，未修改 doc-demo 原设置。

| 原生操作 | 实际结果 | 主要证据 |
| --- | --- | --- |
| 展开 A 目录 | 只出现 A 标记，不出现 B 或额外模块标记 | `project-a-fixture-ax.txt` |
| A 的 `locate(long)` 转到源码、Enter 打开文档 | `doc-demo – DirectoryProbe.java` 定位9:12；文档含 A 标记、`DirectoryProbe#locate` 和 `id` 的 `long` 类型 | `project-a-source-nav-ax.txt`、`project-a-doc-nav-ax.txt` |
| 展开 B 两模块目录 | `peer` 只含 B 标记，`peer-deep` 含额外模块标记，不含 A 标记 | `project-b-expanded-ax.txt` |
| B 的 `locate(long)` 转到源码、Enter 打开文档 | `DocViewDirectoryPeer – DirectoryProbe.java [peer]` 定位9:12；文档路径和正文属于 B，参数为 `long` | `project-b-source-nav-ax.txt`、`project-b-doc-nav-ax.txt` |
| 额外模块 `read(long)` 转到源码、Enter | `DeepDirectoryProbe.java [peer-deep]` 定位9:12；文档含 `SECOND_MODULE_LONG` 和 `long` 参数 | `second-module-source-nav-ax.txt`、`second-module-doc-nav-ax.txt/.png` |
| 切回 A，再次刷新、按完整签名搜索后跳转 | 目录仍只有 A 标记；精确搜索 `locate(long)` 命中目标，源码仍为 A 的9:12 | `project-a-after-peer-refresh-ax.txt`、`deep-signature-search-pasted-ax.txt`、`project-a-after-peer-navigation-ax.txt/.png` |

目录结构仍为模块 → 类 → 方法；深层包不会变成额外的树层级。第一次 Computer Use 键入括号被输入工具省略，搜索框实际为 `locatelong`，未作为通过依据；随后原生粘贴完整 `locate(long)` 并读回精确查询与选中节点后才记录通过。临时项目 B 的目录签名将 `String` 显示为简单名，未在本轮验证其 JDK/类型解析配置；重载定位与文档核对明确使用 `long`，不据此宣称完成临时项目编译或全部类型解析验收。

### 清理、日志与验证边界

临时 A 源码及本轮三个生成文档目录已移入证据目录，新增空目录已清理。原生同步/刷新后恢复原目录，随后通过 Confirm Exit 正常关闭隔离 IDEA 的两个项目窗口。`cleanup.json` 确认34个既有文件、HEAD、两份原有 Doc View 设置和 untracked 清单与基线一致，无新增设置；临时第二项目留在证据目录，不进入仓库。22:22:33启动至22:28:50正常退出的隔离日志没有 ERROR/FATAL/SEVERE；保留平台 WARN/INFO，不宣称零警告。

普通 IDEA 恢复原 `doc-view – ai-context-assessment.md [doc-view]` 窗口和普通 profile，未安装测试包。仅更新本 change 的 tasks、verification 与路线图；没有发现需修改运行时代码的缺陷，未重跑历史208项测试、构建或 Plugin Verifier。索引、空结果、多项目/深层导航分别由3.2.d.g/h/i记录完成；完整主题矩阵仍未覆盖，3.2.d.c和4.5保持未勾。未提交、推送、发布、sync 或 archive。

恢复审计确认仅一个 IDEA 主进程，打开普通 `~/Library/Logs/JetBrains/IntelliJIdea2026.2/idea.log`，未打开隔离日志，见 `ordinary-restored.json` 与原生 AX/截图。`scope-audit.json` 确认相对本轮基线仅3份验收文档变化、HEAD不变。`openspec validate api-tree-navigation --strict` 和 `git diff --check` 均通过；独立只读复核确认本轮各项原生证据支持结论。

## Round56：深浅主题与 Compact 图标矩阵

2026-10-18，继续在 IU262.10968.63 / JBR25 的隔离 profile 使用专用 `doc-demo`。仍使用 Round52 最终包，ZIP SHA-256 为 `c8bfd5d0713bf94f0cac6484ec6ddcc1875549f71a70f035a43d5945e2bef329`，安装 JAR SHA-256 为 `e6ab05becb2ff7dcf4656dcea020d78c57166c9d80a5b16ae56c0cd872bcfbee`。本轮未修改运行时代码、资源、测试或构建配置，未重跑历史208项测试、构建或 Verifier。开始时保存437个工作区文件 hash、Git 状态，以及 doc-demo 的34个既有文件与两份设置 hash。证据位于 `/tmp/doc-view-reliability-20261018/round56/`，不是永久发布附件。

### 四种组合及证据来源

通过 IDEA 原生 Theme 操作选择 Islands Light / Islands Dark，通过 Compact Mode 的 ON/OFF 实际读回确认密度。`normal` 表示 Compact 关闭，`compact` 表示开启；不能从历史截图没有记录开关推断为关闭。初始设置是 Islands Light、Compact 开、编辑器 Islands Dark，收尾恢复 UI 主题和密度。

| 表面 | Light 普通 | Light Compact | Dark 普通 | Dark Compact |
| --- | --- | --- | --- | --- |
| 目录、工具栏、工具窗口入口、Controller gutter | `light-normal-controller-final` | `light-compact-controller-final` | `dark-normal-controller` | `dark-compact-controller` |
| Export 两项及图标 | `light-normal-export` | `light-compact-export` | `dark-normal-export` | `dark-compact-export` |
| 工具窗口 Upload 三平台列表 | `light-normal-upload` | `light-compact-upload` | `dark-normal-upload` | `dark-compact-upload` |
| Preview 稳定显示后的 Settings、Pin、查看、上传、导出、复制图标 | `light-normal-preview` | `light-compact-preview` | `dark-normal-preview` | `dark-compact-preview` |
| 参数编辑器 Settings、Pin、JSON、确认图标 | `light-normal-param-final` | `light-compact-param` | `dark-normal-param` | `dark-compact-param` |
| Doc View 品牌成功通知及动作 | `light-normal-notification-success` | `light-compact-notification-success` | `dark-normal-notification-success` | `dark-compact-notification-success` |
| 类/方法右键、目录 Upload、Preview Upload、Esc 返回并保留 Preview | 用户手工确认 | 用户手工确认 | 用户手工确认 | 用户手工确认 |

表内文件名对应 `.png` 与 `-ax.txt`。图像由主流程及独立只读复核确认是目标画面；AX 用于补充控件语义，不能替代图标视觉判断。浅色普通参数编辑器的早期图片较小，最终以 `light-normal-param-final` 为准。浅色 Controller 的早期画面不作为最终依据，以 `*-controller-final` 为准。Export 菜单包含 Export Markdown / Export Project Docs；Upload 包含 YApi / ShowDoc / 语雀，平台子项采用文字，不要求凭空增加平台 logo。

Computer Use 触发目录右键和 Preview Upload 时仍会使菜单立即收起，因此没有修改产品的 popup 生命周期来绕开控制工具。向用户明确列出四种组合、类与方法两种节点、三平台选项及 Esc 行为后，用户回复“四组均正常，已恢复 Light＋Compact 开”。该行结论来自用户手工验收，没有伪称取得四组目录右键或 Preview Upload 截图；工具窗口 Upload 的独立截图也不替代这项反馈。

Preview 外框/工具栏遵循 IDE UI theme，正文源码编辑器保留 editor scheme。浅色外框配深色正文、深色外框配浅色正文符合本 change 的4.6约定，不能当作主题错误。四格截图都能看到 Settings 齿轮和 Pin；参数编辑器深色普通模式的齿轮疑点经原图复核排除。仅验收稳定显示状态，不据静态图片声称完成逐帧动画或瞬时闪烁检测。

### 通知与 Plugin Manager

复用 Round53 的本机 HTTP 夹具，服务仅监听 `127.0.0.1:51101`，读取后丢弃请求体，日志只记录方法、路径、字节数和响应模式。本轮通过工具窗口 ShowDoc 上传入口分别在四种组合触发成功通知：每组23个接口，共92次 loopback POST。真实通知中的 Doc View 品牌图标、成功文案、查看/复制动作均可读，见表中四份通知证据与 `requests.jsonl`。没有连接真实 ShowDoc、YApi 或语雀，未点击通知外链；不将该验收扩大为真实平台端到端测试。

INFORMATION / WARNING / ERROR 默认平台图标沿用同一最终包 Round53 的 Islands Light/Dark 实测；本轮额外四格检查覆盖品牌成功通知与通知容器。源码没有 Compact 专用通知资源或分支，因此按“通知类型的两主题覆盖 + 品牌通知的四组合覆盖”记录，不声称重新执行全部通知类型与密度的笛卡尔积。

`dark-compact-plugin-logo`、`dark-normal-plugin-logo` 和 `light-normal-plugin-logo` 显示当前1.3.14已安装列表中的独立蓝底 M↓ 标识。40×40 静态 logo 没有主题/Compact 分支；浅深列表识别度通过。右侧 Marketplace 详情仍有加载状态，这些截图只证明本地列表 logo，不证明在线详情完整加载。本轮没有更新、启停或重新安装任何第三方插件。

### 清理、恢复与日志

在原生 ShowDoc 设置清空本机地址，勾选两项清除并 Apply，实际读回 `API Key: 未配置；API Token: 未配置`，见 `showdoc-cleanup-final-ax.txt/.png`。停止本机 HTTP 夹具并确认51101端口无监听。测试实例通过 Confirm Exit 正常退出；将本轮新增的空 `DocViewShowDocSettings.xml` 移入可恢复 `cleanup-backup/`。34个既有 tracked 文件、HEAD、两份原有设置和 untracked 清单均与开始时一致。过程中一次焦点错误导致 UserController 编辑器出现未保存输入，立即通过原生 Undo 恢复；最终源码 hash 和 `input-recovery-ax.txt`、Controller 最终画面确认原内容保留。

Git 状态另有8个既有新增文件由 `AM` 变为 `A`，其工作树内容 hash 未变。本轮没有执行 git add、reset、checkout 或任何暂存操作；开始时未冻结这些文件的 index blob，不能仅凭内容 hash 宣称暂存区也完全不变。不擅自回写暂存区，差异与核对结果单独保存在 `cleanup.json` / `doc-demo-status-diff.txt`。

本轮22:34:43启动至正常退出的隔离日志没有 ERROR/FATAL/SEVERE，见 `current-run.log`、`log-summary.json`。AX 中有 IDE error occurred 入口，但当前日志没有可对应的本轮错误；更早日志的 Doc View SlowOperations 不属于本轮时段，未冒充已复现或修复。平台 WARN 保留，不能宣称零警告。

普通 IDEA 已恢复原 `doc-view – ai-context-assessment.md [doc-view]` 窗口。审计确认仅一个 IDEA 主进程，使用普通 `~/Library/Logs/JetBrains/IntelliJIdea2026.2/idea.log`，未使用隔离日志；普通实例没有安装测试包，见 `ordinary-restored.json/-ax.txt/.png`。本轮仅更新路线图、本 change 的 tasks 与 verification；3.2.d.c、4.5按上述分项证据完成。不扩展为全部路线图、第三方主题或在线发布验收，未提交、推送、发布、sync 或 archive。

`openspec validate api-tree-navigation --strict`、`git diff --check` 均通过；`openspec instructions apply --change api-tree-navigation --json` 返回37/37、`all_done`。`scope-audit.json` 确认相对本轮437文件基线仅上述3份文档变化，仓库 HEAD 不变。完成状态不触发自动归档或发布。
