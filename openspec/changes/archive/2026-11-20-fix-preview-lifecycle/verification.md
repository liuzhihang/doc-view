# Verification

## 自动化

- `/tmp/doc-view-reliability-20261018/round8-TEST-*.xml` 对应主线执行的 `./gradlew test verifyPluginProjectConfiguration buildPlugin verifyPlugin --rerun-tasks`：共 96 项，96 passed、0 failed、0 errors、0 skipped；Gradle 18/18 个 task executed。
- 同一轮 `PreviewLifecycleTest` 10/10 passed、0 failed；`SpringMarkdownContractTest` 1/1 passed、0 failed。预览测试包含 Spring/Dubbo 真实 fixture 输出、后台模板生成与 EDT Document 应用断言，以及 viewer 生命周期、源码状态回调和导出快照。
- Plugin Verifier 对 IU 261.22158.277 报告 Compatible，无 deprecated/experimental API 报告。round8 构建的插件 ZIP SHA-256：`b9e1365161403069ec251311b7403ab8f27caaff943ebd2bd2efae2f91997b1d`。
- 上述 round8 验证包含 `PreviewForm` 两处 `isDispatchThread()` 检查及错误时抛 `AssertionError` 的替换；替换保持原有非 EDT 约束。

## 联动范围

- `PreviewForm` 单项 Upload 入口复用已有 `upload(project, docView)` 后台 wrapper。此 change 不修改上传 payload 或服务协议；凭据读取与集成配置行为见[集成凭据 change](../2026-11-20-integration-credential-storage/proposal.md)。本轮预览测试没有验证上传接口或凭据行为。

## 未验证

- Fixture 测试显式关闭 JCEF，因此自动测试本身仍不验证真实 JCEF/JS 渲染；目标 IDE 的状态显示、过期回调与日志结果见下方 429 候选原生记录。
- 完整 4.3 集成矩阵仍未完成：429 候选补充了状态、受控迟到 JSON、关闭、复制和单项导出证据；同包 Spring/Dubbo 整类预览、批量导出及完整 viewer 基线仍需对齐验收。round8 build/Verifier 不替代这些原生检查。

## 列表加载完整后台回调补验

2026-10-18 round10 在原10项基础上增加两个真实 `buildDoc()` 排队测试，共12项通过：用临时替换的应用服务控制返回空列表、抛异常或取消，验证后台Task.run到onSuccess/onThrowable/onCancel的终态，不直接调用状态处理器冒充队列。服务边界断言不在EDT且持有read access。延迟服务在form.dispose()后才返回空结果或抛错，等待后台活动与EDT队列收尾后，文档未再变化、不可导出、viewer数量已恢复且未重新分配。首次失败来自测试循环重复创建同名fixture类，复用类后完整通过；无需为测试新增生产钩子。JCEF分支仍未执行。

命令：`./gradlew test --tests '*OpenApi3SchemaRequiredModeTest' --tests '*SpringMarkdownContractTest' --tests '*PreviewLifecycleTest' --tests '*ConfigurationExperienceTest' --rerun-tasks`，本轮32 tests、0 failure/error/skip。XML备份 `/tmp/doc-view-reliability-20261018/round10-TEST-com.liuzhihang.doc.view.ui.PreviewLifecycleTest.xml`。原生HTML/JCEF/主题仍待环境可用后验收。

## IU 262 原生部分验收（2026-10-18）

- 环境：IntelliJ IDEA IU `262.10968.63`（`/Applications/IntelliJ IDEA.app`），JBR `25.0.4`；隔离 profile `/tmp/doc-view-reliability-20261018/native`。加载 round21 插件包，SHA-256：`01e67f753ce9fa388c0dbb607fe940ddb9a371dc8570aa6347ad27a3c2c42020`。
- 从 `UserController` 第 34 行的 Doc View gutter marker 打开真实 Spring `PreviewForm`；生成正文包含 `/user/respGenericTest`、`POST` 以及请求/响应表。连续两次重新打开该预览均成功。
- 尝试通过 CUA 点击浮窗 Preview 或 Pin 时，浮窗消失并返回主窗口。此现象与窗口重新激活同时发生，不能据此认定为插件运行时缺陷；本次没有确认 JCEF panel 状态替换、释放状态或 viewer 数量。
- 启动日志中唯一已知 `SEVERE` 来自平台 `accessibilityStatusBar` → Maven `TransactionGuard` 的 write-unsafe 调用栈，栈中没有 Doc View 帧；因此不将本次日志描述为零错误，也不把该平台日志归因于插件。
- 本次仅完成真实 Spring 预览打开及重复打开的部分检查。JCEF 生命周期、viewer 释放计数、Dubbo/失败/取消/快速切换和导出仍未原生验证，任务 4.3 保持未勾选。

## Round44/45 自动回归补验（2026-10-18）

Round44 全量执行 `./gradlew test --rerun-tasks`：40 个 suite、197 项测试，0 failures、0 errors、0 skipped；该轮包含本 change 的12项 `PreviewLifecycleTest`。日志：`/tmp/doc-view-reliability-20261018/round44-full-test.log`，XML：`round44-full-test-TEST-*.xml`。

随后在 `testCurrentCancellationReplacesProgressAndStaleCallbacksCannotReplaceNewState` 增加旧取消回调断言：较新请求已经显示文档时，在 EDT 经既有反射入口调用旧请求的生产取消处理器，确认编辑器正文、Markdown 快照与当前文档对象不变。它与已有旧成功/失败回调守卫互补，不是端到端并发调度测试。Round45 单独执行 `./gradlew test --tests '*PreviewLifecycleTest' --rerun-tasks`，12 项全部通过，0 failures、0 errors、0 skipped。日志：`/tmp/doc-view-reliability-20261018/round45-stale-cancel.log`，XML：`round45-TEST-com.liuzhihang.doc.view.ui.PreviewLifecycleTest.xml`。

Fixture 仍关闭 JCEF；上述结果不能证明 HTML panel 创建、状态切换或释放。生产源码、资源和打包文档未变化，最近完整构建/Verifier 是 Round42（196项测试、IU261 Compatible）；原生实例仍安装 Round34。Mac 锁屏阻挡后续原生验收，相关未完成任务保持不勾选。


## Round50 原生 Markdown 渲染及混合主题补验

IU262 隔离 profile 安装 Round50 包后，从 `doc-demo` 的 `UserController.respGenericTest` 行标记打开真实文档，点击 Preview 后实际显示渲染后的标题、POST路径与参数表（`/tmp/doc-view-reliability-20261018/native-round50-preview-toggle.png`）。深色 IDE/浅色 editor scheme 与浅色 IDE/深色 editor scheme 的浮窗操作区均跟随 IDE 主题、正文保留 editor scheme（`native-round50-preview-dark.png`、`native-round50-preview-light.png`）。本轮完成一次渲染切换，未覆盖反复切换后资源数量/项目关闭/泄漏压力；相关原生生命周期任务不据此整体勾选。图标与背景修正、后续包及普通窗口恢复证据见 [API tree verification](../2026-11-09-api-tree-navigation/verification.md)。

## IU 261 JCEF 生命周期观测与 candidate caaf 原生预览（2026-10-22）

本节只补记本轮增量证据，不覆盖此前记录中的未验证项。早期只读 JDI 观测来自 df21 / ef4a 两个候选包（ZIP SHA-256 分别为 `df21ba775bd79032a3667e7ddef3984efee271a0511140977ceb33415cf50adb`、`ef4a9dbcad7eeeaee8013377f2798cfd7bac4b6f2708f8d29eaec7cbabd649bc`；对应插件 JAR SHA-256 为 `6eec14f6752b1a31aa9f16e1bffde276bacf7d92b6f18b2a24fd85534b0e4c2e`、`d5f8e6d774cac0d355d492fab4e765744275cedd5f2572cf00c599eb38191947`）。JDI 仅读取对象与状态，没有调用 IDE action 或修改 debuggee。最终交互画面来自 candidate caaf：IU `261.22158.277` 隔离 profile，ZIP SHA-256 `caaf605f2a3599a65e6cc0a69b2a6900328a86442d3315a0b4e6bc322a833bc3`，安装插件 JAR SHA-256 `a143d7698663e54d31f905186950616232a93bcbfed663d856216f0e2a0ea950`，与 candidate234 门禁清单一致。两组包的观察不互相替代。

- 早期 JDI 在同一个 `PreviewForm#87778` 观察到 `choosePreviewPanel` 持有 `MarkdownJCEFHtmlPanel#87781` 和 Markdown editor；随后同一 Form 的 dispose 路径先进入 `Disposer.dispose`，再观察到 HTML panel 字段清空。另一次 Loading 期间的 JDI 观察到关闭后 Form 已 disposed、HTML panel/editor 字段为空且后台 indicator 的 `myCanceled=true`。这证明这些被观察到的对象沿关闭路径到达 dispose/字段清空，不证明对象已 GC、长期实例数稳定或 candidate caaf 的 HTML 状态切换通过。
- caaf 的 `UserController` 类级 Spring 预览显示 8 条接口文档项，首项的 Markdown 源正文包含路径、HTTP 方法、请求与响应表（`/tmp/doc-view-native-20261022-all/iu261-caaf-spring-preview.txt`）。这是源码/Markdown 预览成功；没有据此认定 JCEF 已显示 HTML。
- caaf 上对浮窗 `Preview` 与 `Pin` 的 Computer Use 点击都会使浮窗从可观察窗口列表消失；`Raise` 和重新绑定尝试仍只读到主 IDEA 窗口。`Ctrl+Tab` 最终切到主编辑器中的导出 Markdown 文件，不能作为预览浮窗键盘焦点验收。相关 AX 快照为 `/tmp/doc-view-native-20261022-all/iu261-caaf-preview-tab.txt`、`iu261-caaf-preview-pin-attempt.txt`、`iu261-caaf-html-toggle-after-raise.txt`、`iu261-caaf-preview-rebind-click.txt`、`iu261-caaf-preview-keyboard-focus.txt`。这些结果表示本轮无法继续观察浮窗交互，不单独证明插件按钮实现有缺陷或正常。
- caaf 的受控预览加载取消中，IDEA 的 Stop Background Process 列出并选择 `Doc View preview`；Loading 状态在取消前可见，JDI 读到 indicator 从 `myCanceled=false` 变为 `true`，且 `PreviewForm$10#onCancel:781` 在 EDT 命中时 Form 仍未 dispose（`/tmp/doc-view-native-20261022-all/debug/caaf-spring-load-cancel.log`）。恢复线程后 Computer Use 只绑定到主窗口，未取得取消气泡或预览浮窗终态截图；因此只记录取消请求与回调到达，不声称 UI 终态、HTML 状态或无部分结果已原生通过。该暂停/恢复过程也不是自然耗时测量。

HTML 成功/空/失败/取消的可见状态替换、快速切换与迟到回调、Dubbo、关闭后反复打开的资源基线，以及单项/整类导出矩阵仍未覆盖。JDI 的单次对象身份和字段清空不等于泄漏压力测试；任务 1.4、2.2、4.3 保持未勾选。

## IU 261 candidate235 / 2221 空、失败、取消与释放补验（2026-10-22）

环境为 IU `261.22158.277` 隔离 profile `/tmp/doc-view-native-20261022-all/iu261`；candidate235 ZIP SHA-256 为 `2221a77261d1de30f09c202446cd4565080b1aeff733585a9c44587e496f448d`，profile 内插件 JAR SHA-256 `62b06ff49d5f18c89f104983bd7dce5e028282134a30fe23c6689bbd481a9f78`，与候选包门禁摘要一致。

- 临时 fixture 中的空类 `NativeEmptyController` 打开预览后，源码编辑器显示 `No Doc View content.`（`iu261-2221-preview-empty.txt`、`.jpg`）。这只证明空结果状态文本可见。
- 临时 fixture 保存无效 Spring 模板后，预览显示 `Failed to render Doc View.`；方法列表仍有两项（`iu261-2221-preview-render-failure.txt`、`.jpg`）。随后通过设置 UI 恢复并应用原模板，重新打开预览后显示正常 Markdown（`iu261-2221-template-restored-applied.txt`、`iu261-2221-preview-render-recovered.txt`）。JDI 在失败预览按 Esc 关闭后观察到同一 Form 的 HTML panel 和 Markdown editor 字段均清空（`debug/2221-render-failure-dispose.log`）。这证明该次失败/关闭路径的状态文本和对象引用清理，不证明 JCEF HTML 已显示或对象已被 GC。
- Dubbo 加载取消由 IDEA Stop Background Process 选择 `Doc View preview`；JDI 观察到 indicator `myCanceled=true` 并命中 EDT 上 `PreviewForm$10#onCancel:781`，当时 Form 尚未 dispose、`currentDocView=null`。随后按 Esc 关闭，JDI 在 `dispose:296` 和 `dispose:318` 观察到 panel/editor 引用清空（`debug/2221-dubbo-cancel-dispose.log`）。取消后的可见终态文本没有捕获，因此不声称取消状态显示或完整取消 UI 已验收；受控暂停时间也不是自然耗时。

本轮新增了空结果、模板渲染失败/恢复以及受控取消后显式关闭时的字段清理证据；仍未验证 JCEF HTML 中各状态的真实渲染/替换、快速切换与迟到回调、关闭后重复打开的资源基线及导出矩阵。单次字段清空不构成泄漏压力测试；任务 1.4、2.2、4.3 保持未勾选。

## 2026-11-09：421 项候选的原生整类与空结果

候选 ZIP `cbc2c7142094ec1905fc8443a435b23026eb34052a1b4a1418228f5fc39a9a4d`、JAR `4b587f4884f45653eca10e6817aec3ae063c3dd7de684cc134aab788790dbe3f`，隔离 PID 46071 / 正式 IU `263.7307.200`，JBR 25。421 全量测试和最低/正式两个精确 build Verifier 的门禁详见 OpenAPI change verification，原生证据位于 `build/native-validation/2026-11-08/remaining-native/batch-escape-candidate/`。

从 `HttpAcceptanceController` 类声明打开 Spring 三方法预览、从 `NativeLifecycleDubboService` 接口声明打开 Dubbo 三方法预览，各自然重复三次；方法列表完整，首次循环逐项核对三个正文的路径/方法名，均通过。计时从已准备好的 Doc View 动作原生 Enter 到 AX 读取完整方法列表和首篇 Markdown：Spring 1.493–1.509 秒、Dubbo 1.476–1.502 秒；这些是包含输入/AX 开销的可观察上界，不是纯 PSI 耗时、性能提升或 SLA。证据 `preview-class-result.json`。三份新增测试源码仅在 ignored 虚构 fixture，文件所有权/原源码哈希在 `owned-preview-fixtures.json`，清理另记。

第一轮 Dubbo 定位到实现类，Find Action 的守卫发现只有 Settings 同名行，安全拒绝继续。当前识别 contract 是接口作为文档主体，带 DubboService 的实现用于发现并归属接口；改到接口声明后复验通过。这是准备入口修正，不是产品故障，也没有新增实现类预览入口。

空 Controller 原生预览无方法行，源码模式显示 `No Doc View content.`；实际 JCEF HTML 画面也显示同一空状态，无上一份成功正文。原生 Copy 保持临时剪贴板哨兵不变并恢复原剪贴板；单项 Export 未打开保存选择器，popup 保留。证据 `empty-preview-result.json`、`empty-preview-html.png`。模板失败、取消、在途切换和完整资源释放继续单独验收，未据这几个成功场景勾选 1.4/2.2/4.3。

资源样本：三轮参数/预览开关后 ParamDocEditorForm/ParamTreeTableView 均为 0，FD 均 515；首轮已观察的新 renderer 在关闭后退出，三个关闭样本均无 renderer 子进程。PreviewForm 每个关闭样本仍为 1，之后整类预览循环后为 3。后续读取 ignored heap 元数据 `build/native-validation/2026-11-08/remaining-native/batch-escape-candidate/diagnostics/preview-form-metadata.txt` 确认被采样的 Form `disposed=true`，`markdownEditor`、`markdownHtmlPanel`、`currentDocView`、`currentMarkdownText`、`docViewList`、`docViewMap` 为空，加载/渲染 indicator map 和 Markdown cache 为空；但 `markdownDocument`、目录/工具栏 Swing 组件、`psiClass` 以及已 disposed popup 等引用仍存在。因此单次关闭后 Form 实例计数不代表 panel 或 editor 未释放，也不能据此宣称对象图已全部清空或通过完整 GC 压力验收。相邻 `diagnostics/toolwindow-entry-audit.txt` 和 `root-class-audit.txt` 显示关闭项目的 `ToolWindowManager` 仍注册 Doc View `ToolWindowEntry`，链中保留 `ProjectViewRenderer` local JNI root；classloader/热卸载问题需按其独立证据和修复任务处理，不把 disposed PreviewForm 本身认作该根因。前一个 tree-ui 候选的卸载通过不代替本包后续复验。

同一候选随后在隔离 fixture-b 将已备份的默认 Spring 模板临时改为 `#if(`，原生预览分别在实际 JCEF 页面和源码模式显示 `Failed to render Doc View.`。Copy 没有改变临时哨兵，原剪贴板随后恢复。通过设置页原生 Apply 恢复 459 字符原模板，再重新打开设置核对全文/SHA `472710c8f7c689789c64368c536cbe57d9b40aca83051d6ad7353dd79297879c` 一致，重新预览 nativeGet 的源码和 HTML 正确。证据 `template-failure-recovery-result.json`、`template-restored.json` 及对应截图。该场景证明失败终态和恢复；它跨了 popup 生命周期，不声称已覆盖同一 popup 的在途旧结果竞态。

## 2026-11-09：原生发现目录选择回调读取 PSI

同一 IU 263.7307.200 原生运行的日志在 `build/native-validation/2026-11-08/unload-clean/log/idea.log:48085` 附近记录 `ThreadingAssertions` 严重错误：EDT 的 Swing `JList.fireSelectionValueChanged` 调用 `PreviewForm.lambda$applyDocViewList$1`，进而由 `PsiClassImpl.getQualifiedName` 抛出 `Read access is allowed from inside read-action only`。这与预览正文能否显示是不同验收维度；前述 421 候选自然预览成功不消除该错误。

新增窄回归 `PreviewLifecycleTest.testSelectedDocumentLabelDoesNotReadPsiOnDispatchThread` 通过真实列表加载任务和 selection listener 观察 `DocView.getPsiClass()` 是否在 EDT 调用，并断言类限定名不变。旧实现的 RED 已由主线程统一运行确认：1 test、1 failure，失败恰为“Swing selection callback must use class-name metadata captured during the background PSI read”；证据 `build/native-validation/2026-11-09/preview-selection-thread/red.log`、`red.xml`，14 个 Gradle task 实际执行，耗时 12 秒。首轮 GREEN 编译因测试漏导入 `java.util.Map` 而在 `compileTestJava` 失败，未运行测试；`green-suite-stale-red-output-not-executed.xml` 是复制的旧 RED 结果，不作为 GREEN 证据。补上 import 和元数据循环中的 `indicator.checkCanceled()` 后，`PreviewLifecycleTest` 全类 13 tests、0 failures/errors/skips，通过，14 个 task 实际执行、耗时 13 秒；证据 `green-suite-2.log`、`green-suite-2.xml`。

生产实现将文档列表和限定名放入不可变快照；限定名在已有后台 read action 中取得，selection listener 不再访问 PSI。上述自动回归已通过，但没有运行全仓测试、构建、Verifier 或新候选原生复验。旧 421 候选日志中的问题仍然有效；必须在包含此修复的新候选中原生选择目录项并检查日志，任务 4.5 及整体集成验收 4.3 保持未完成。

## 2026-11-09：HTML 状态页的 JCEF 结构 RED

IU `263.7307.200` 421 候选 `build/native-validation/2026-11-08/unload-clean/log/idea.log` 在 02:28:41 和 02:58:00 记录 `MarkdownJCEFHtmlPanel - Failed to update the Markdown preview`，异步 JS 错误为 `First body child is expected to be the root of the document!`。第二次在一次 `Doc View preview` 取消后出现，但第一次前后没有对应取消/关闭事件；因此取消会经过这个状态路径，现有日志不支持把问题归为 panel dispose 竞态。Markdown JCEF 的 `ScrollSync.js` 会读取 `document.body.firstChild` 并要求平台 renderer 生成的源码位置属性；旧 `showPreviewStatus()` 手拼的裸 `<html><body><p>…` 不包含该结构。Java 栈在异步 JS worker 处结束，具体的 `setHtml()` 调用无法由该栈单独还原。

窄测试首轮因测试代理缺少 `javax.swing.JPanel` 导入在 `compileTestJava` 失败，没有运行测试，不计业务 RED。随后主线程运行 `testHtmlStatusUsesMarkdownRendererOutputWithRootSourceRange` 确认 1 test、1 failure：预期是公开 `MarkdownUtil.generateMarkdownHtml()` 生成的 `<body md-src-pos="0..27">…`，实际是旧实现拼接的裸 HTML。证据：`build/native-validation/2026-11-09/preview-status-html/red-2.log`、`red-2.xml`。`PreviewForm` 已改为用公开 Markdown renderer 的非阻塞 read task 生成状态页，并在新状态、正文成功和 dispose 时取消/代际失效；测试新增更新状态、正文先完成及 dispose 先发生三种迟到结果场景。首轮 GREEN 编译因缺少 `javax.swing.JPanel` 导入失败，没有运行测试；第二轮执行 17 项时，两个测试因在 EDT 阻塞 `CountDownLatch.await()` 而失败，这是测试等待方式错误，不能作为生产代码失败证据。改为用 `PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()` 泵送 UI 事件后，`PreviewLifecycleTest` 17 tests、17 passed、0 failed、0 errors、0 skipped；14 个 Gradle task 实际执行、耗时 16 秒。日志和 XML：`build/native-validation/2026-11-09/preview-status-html/green-suite-2.log`、`green-suite-2.xml`。task 1.5 自动化完成；fixture 禁用 JCEF，含修复候选的原生 JCEF 状态切换及无 JS 结构错误仍未验，因此 tasks 1.4、4.3、4.6 保持未完成。

## 2026-11-09：428 候选空状态与 promise 取消误报警

428 候选在正式 IU `263.7307.200` 的原生 JCEF 空预览实际显示 `No Doc View content.`，此前的 `First body child is expected to be the root of the document!` 结构错误未再出现。空状态证据位于 `build/native-validation/2026-11-09/project-close-candidate/empty-preview-result`。同一运行的 `build/native-validation/2026-11-08/unload-clean/log/idea.log` 03:43:03 有一条新 `WARN`：`Unable to render Doc View preview status`，底层异常类型名为 `org.jetbrains.concurrency.AsyncPromise$Companion$CANCELED$1`。这属于 promise 取消 sentinel；旧错误回调只过滤 `ProcessCanceledException`，因此将预期取消误记成渲染失败。空状态真实 HTML 渲染和结构错误消失已部分通过，但日志仍有取消误报，不能据此宣称 JCEF 场景完全通过。

修复使用公开 `CancellablePromise.isCancelled()` 状态判定预期取消，不引用 sentinel 类型；未取消的真实 renderer exception 仍继续记录 warning。定向测试 `testCancelledNonBlockingPromiseIsNotLoggedAsStatusFailure` 用 `ReadAction.nonBlocking().submit()` 创建真实平台 promise、调用 `cancel()` 并捕获 `onError` 回调，同时验证未取消 renderer failure 仍可记录。首轮编译因 `ReadAction.nonBlocking()` 接受 `Callable` 而测试误传 `Computable`，停在 `compileTestJava`，没有运行测试；改正后 `PreviewLifecycleTest` 18 tests、18 passed、0 failed、0 errors、0 skipped，14 个 Gradle task 实际执行、耗时 17 秒。证据：`build/native-validation/2026-11-09/preview-status-html/green-suite-2.log`、`green-suite-2.xml`。task 1.6 自动化已完成。`CancellablePromise.isCancelled()` 来自其公开 `Future` 接口，不依赖内部异常类型。

截至本节所述的 428 候选，含此 guard 的下一包尚待原生复验 WARN 消失；该时点的未完成项随后由 429 候选单独复验，见下节。不能将 428 的部分通过扩展为同包全验收。

## 2026-11-09：429 候选原生 JCEF 状态、预览生命周期与资源结果

隔离正式 IU `263.7307.200`（JBR `25.0.4.1+1-b635.79`）安装 `status-cancel-candidate`，PID 71085；安装读回 ZIP SHA-256 `6c789b81e371656db0cdbfca005c82a4175541bb98ae04106500feece46f8370`、JAR SHA-256 `9828f7d3f864f60fd28521e25c0a36782e24cfbb56a13aa2c5e3157110b33aa2`。该包 artifact 记录 80 suites / 429 tests、0 failures/errors/skips，344 个 class 的 major 均为 69。安装及自动门禁身份见 `build/native-validation/2026-11-08/remaining-native/status-cancel-candidate/{native-install.json,artifact.json}`；IDE build 来自同一隔离启动日志。

原生 JCEF 状态与时序：`loading-html-confirmed.png` / `rendering-html.png` 记录加载中和渲染中，`empty-result.json` 确认空状态 `No Doc View content.` 可见且无剪贴板改变、结构错误或意外状态 warning；`template-failure-recovery-result.json` 记录故意使用 `#if(` 后源码与 HTML 错误状态可见，Copy 哨兵保留，随后通过原生设置恢复并重开原始 459 字符模板，SHA-256 `472710c8f7c689789c64368c536cbe57d9b40aca83051d6ad7353dd79297879c`，源码与 HTML 均成功恢复。`controlled-preview-cancel-result.json` 两次在后台任务中暂停后，通过 IDEA 原生 Stop 取消，源码和 HTML 均显示取消终态，Copy 哨兵不变；断点仅暂停 worker、未调用生产取消 API。这些是受控功能交错，不是自然延迟或性能测量。

`late-render-result.json` 将旧 render 暂停在 `PreviewForm` 行 879，先选中新的 JSON 文档，再恢复旧任务；HTML 与源码仍显示新 JSON，且未出现 selection read-access assertion。`close-render-result.json` 在 render indicator 注册后暂停，原生 Escape 先关闭 popup，再恢复旧任务；没有迟到结果重建 popup。单项 Copy 复制当前 JSON 的 624 字符，排除旧 GET 并恢复剪贴板；单项 Export 写出 778 bytes，hash 与当前复制的 JSON 文档一致且排除旧 GET。批量 Export 不在这两个结果内。所有 JDI 断点只控制执行顺序，不作耗时或性能结论。

日志核对 `status-and-selection-log-check.json` 中五类模式均为 0 命中：Doc View preview-status warning、ThreadingAssertions、JCEF `First body child`、Markdown preview update failure、EDT 访问错误；其中故意触发的 Velocity 模板异常是预期项。三轮 `resource1-html.png` 至 `resource3-html.png` 由主线逐张核对；`resource-summary.json` 显示每轮关闭后的 FD 均为 512、ParamDocEditorForm/ParamTreeTableView 为 0、PreviewForm 与 DocViewWindowPanel 各为 1，且打开时观察到的新增 CEF renderer 子进程在关闭后消失。有限轮次不能证明长期稳定或所有插件对象已释放。

**此包动态卸载失败。** `unload-result.json` 为 56 条 Doc View 类直方图记录、目标类仍存在、平台仍报告 plugin still referenced；关闭采样后 `PreviewForm` 的离线目标实例数为 0，不能据此把 classloader 标为已卸载。`diagnostics/classloader-roots.txt` 与 `toolbar-trace-audit.txt` 定位到 `Local JNI → NioFileSystemTree$FileRenderer → Tree → UniversalFileChooser$Panel$FileView → ActionToolbarImpl.myCreationTrace → Throwable.backtrace → ExportUtils / PreviewForm$6 → PluginClassLoader`。这是当前堆的引用链证据，不代表已修复或已确定可由插件安全解除。

无调试器的同包对照 PID 78877 通过真实 Export 写出 618-byte `nativeGet.md`（`no-debug-export-control/export-result.json`，明确 `debuggerEnabled=false`）；禁用插件后仍有 20 条 Doc View 类直方图记录和 still-referenced warning，`diagnostics/classloader-roots.txt` 复现同一 file chooser toolbar creation-trace 链。故不能把该引用归因于调试器。卸载根因与 release gate 仍开放。

因此 tasks 1.4、4.5、4.6 已由此候选补齐其各自的 JCEF 状态、selection/log 和在途关闭验收；2.2 的按需创建/失败释放及 4.3 的整类预览、批量导出与完整资源基线仍保持未完成。429 查询取消显示的旧 `Project document status check was cancelled.` 属于共享文档库错误提示，不是 preview 的 `Loading Doc View was cancelled.`；通用查询文案的新版本尚待后续候选原生读回。插件热卸载未通过，不得据 428 历史通过、表单归零或 renderer 子进程退出宣称本包 classloader 已释放。

## 2026-11-20 最终候选整类预览、批量导出与资源样本

正式 IU `263.7307.200` 隔离运行，安装 ZIP SHA-256 `2f0284a418459a7e6e102500ec526d6b37fe68e0d4b75cae04664de40337575a`，JAR SHA-256 `942422587aa51c79c69d9321375b279f66cd1765e07041ceea68d506984056cb`。恢复隔离 profile 的本机 JBR 25 SDK 定义后，Spring 整类和注解 Dubbo 分别打开及关闭三次，正文与方法数符合夹具。Spring 使用“全部导出”写入隔离证据目录；IDEA 显示成功，落盘的 `HttpAcceptanceController.md` 包含 `/native/users/{id}`、`/native/json` 和 `/native/form`。原生 HTML 预览从 GET 切换 JSON，再立即切换 Form，最后选中 `nativeForm` 且页面显示 `/native/form` 和 POST。该次未受控注入迟到回调。取消与重试证据见[最终性能复验](../2026-11-20-doc-generation-performance/verification.md#2026-11-20-最终候选的预览刷新与取消)。

GC 与 JCEF 仍未结案。同一进程保留 IDEA 内置 Markdown 编辑器标签，关闭文档弹窗时曾有 3 个 `PreviewForm` 实例，后续关闭样本降至 1。打开 Doc View HTML 时观察到 2 个 `MarkdownJCEFHtmlPanel`，关闭后再次采样仍看到 2 个 panel、2 个 RemoteBrowser 和同一 JCEF 子进程。采样包含 IDEA 内置 Markdown viewer，仅凭类计数无法归属到具体弹窗或判断泄漏。没有取得对象根路径，也未重跑此包的空、失败与取消 HTML 组合，所以 2.2、4.3 保留。原始样本与本次导出文件在 `build/openspec-remaining-review-20261120/`。

### 源码模式提前创建 JCEF 的原生复现

关掉独立 Markdown 标签后正常重启同一隔离 IDEA。首次打开 Doc View 前，full GC 没有 `PreviewForm`、`MarkdownJCEFHtmlPanel` 或 `RemoteBrowser`，也没有该 IDEA 的 CEF 子进程。打开默认 Markdown 源码预览且未切换 HTML 时，full GC 样本仍看到三类实例各 1 个，且出现新的 JCEF 子进程。源码初始化无条件调用 `markdownHtmlPanel()`，和原生观察相符。样本是 `no-viewer-at-clean-start.txt` 和 `clean-open-source.txt`。

### 按需创建修复及再验

修复包 ZIP SHA-256 `998f952318afe03d46b676f7dcd72cec79a58db612784a83888b7c15f305463a`，JAR SHA-256 `d41c3edecc48e76bd6d17d287763481d59bc52698cc445bdbf58d74a3daef27f`。Java 25、单 JAR、418 个 class 的 major 均为 69。定向 `PreviewLifecycleTest` 19 项通过，新增“进入 HTML 时源码渲染已经在途”的正文回填用例；启用 opt-in 验收源集的全量为 100 suites / 581 tests，0 failures / errors / skipped，构建和配置检查通过。Plugin Verifier 1.411 在 `IU 263.7031.23` 与 `IU 263.7307.200` 均 Compatible，各有 16 项既有 Experimental API 使用。

同包在干净隔离会话验证如下。

- 启动及打开默认源码预览时，无 `MarkdownJCEFHtmlPanel`、`RemoteBrowser` 或 JCEF 子进程；源码包含 `nativeGet` 和正确正文。
- 首次切换到 HTML 后各出现 1 个 panel / RemoteBrowser 和一个 JCEF 子进程，渲染后显示 `/native/users/{id}` 和 GET。再快速切换到 `nativeForm` 后显示 `/native/form` 和 POST。
- 按 Esc 关闭后约 150 秒的 full GC 仍有 1 个 `PreviewForm`、1 个 panel / RemoteBrowser 及原 CEF 子进程。同一进程的对象元数据确认 Form 与 popup 均已 disposed，Form 的源码 viewer、HTML panel、当前 Markdown/DocView 引用均为空，两个工作 indicator map 与 Markdown cache 为空。此结果证实直接引用已清理，仍需查清平台存活对象的归属和释放时机。
- 此包的 HTML 空、失败与取消组合，以及重复关闭后完整回到资源基线，保留在 2.2 / 4.3。原始数据保存在 `build/openspec-remaining-review-20261120/`。
- 后续对象图检查中，存活的 `PreviewForm`、`MarkdownJCEFHtmlPanel` 和 `RemoteBrowser` 均查到以 JBR macOS `CAccessible.timer` 为静态根的强引用路径。该记录来自受辅助功能观测的会话。正常退出隔离 IDEA 后，对应 IDEA 与 JCEF 子进程均已结束。现场能证明关闭路径清理了直接引用及进程退出后的释放，关闭 popup 后回到完整基线仍未通过。未操作 JBR 的内部计时器。仅保留类名和引用结构，临时堆文件已清除。

## 2026-11-20 同一会话重开预览

使用 `998f9523…463a` 候选。打开 Spring 源码预览，切换 HTML 并关闭，在同一进程重新执行空类的 Doc View 预览。IDEA 出现意外异常，日志显示 `PreviewForm.choosePreviewPanel` 在初始化时解引用尚未创建的 `markdownHtmlPanel`。原因为显示模式是一个全局会话标志，而懒创建的面板属于单个 popup。

新增独立资源回归，先在旧实现执行并命中同一空指针，随后保留会话的模式偏好，将当前模式和面板分配到各自窗口。JCEF 不可用时，新窗口保持源码模式；可用且已选 HTML 时，新窗口先准备自身面板，由正常加载流程渲染状态或正文。定向回归与完整 `PreviewLifecycleTest` 已通过，对应记录位于 `build/openspec-followthrough-20261120/`。

## 2026-11-20 修复包原生预览复验

隔离 IntelliJ IDEA IU `263.7307.200` 通过从磁盘升级到 `1.7.2-SNAPSHOT`。ZIP SHA-256 `bd0d5a404d061050d8ed7d429aa904550feea1b0956b5d5ced346f90f35999ed`，安装 JAR SHA-256 `f55e13cf168985f80710efa5935ee381bed66c13417cb4485b80fb08716075b3`。全量 100 suites、583 tests，0 failures / errors / skipped；`buildPlugin` 和 `verifyPluginProjectConfiguration` 通过。Plugin Verifier 对 `263.7031.23`、`263.7307.200` 均 Compatible，每个 build 报告 16 项既有 Experimental usages。原始证据保存在 `build/openspec-followthrough-20261120/`。

- 在同一进程先打开 Spring HTML、关闭并重开，新的浮窗保留 HTML 模式，正常渲染 `/native/users/{id}`。再打开无方法的虚构 Spring 类，HTML 出现“没有 Doc View 内容。”，原空指针未复现。注解 Dubbo `annotatedCatalog` 的源码预览可读。单项导出的 Markdown 包含 `FictionalAnnotatedService#annotatedCatalog`，Spring 整类导出包含 `/native/users/{id}`、`/native/json`、`/native/form`。已回读两个落盘文件。
- 在列表加载阶段暂停工作线程，原生 `⌘⇧F2` 选中“Doc View 预览”取消，实际显示“正在停止”。恢复线程后任务退出并回到主窗口。在第二次受控复验中，通过调试器调用该任务自己的 `ProgressIndicator.cancel()` 保持浮窗在前台，HTML 和切回的源码视图均显示“已取消加载 Doc View。”。第二次仅用于观察终态，不作为原生快捷键触发的独立证据或耗时测量。
- 另一个虚构项目使用无效 Velocity 模板 `#if(`。同一包在源码编辑器与 JCEF HTML 中都显示“渲染 Doc View 失败。”。按 Esc 回到原 Java 编辑器。该进程没有独立 Markdown 编辑器；full GC 样本中 `EditorImpl$MyScrollPane` 的数量从 4 降至 3，但 `PreviewForm` 和 `MarkdownJCEFHtmlPanel` 关闭后仍各存活 1 个。正常退出后 IDEA PID 22446 和 CEF PID 22581 均消失。存活项延续此前已记录的 macOS 辅助功能根路径疑点，本轮只证明关闭时源码编辑器对应资源下降，不声称已定位此轮的确切持有者或跨窗口完整 GC 基线。2.2 / 4.3 保持未完成。

对同一正式版 JBR 的 `java.desktop` 字节码做只读复核。`CAccessible$AXChangeNotifier` 在 `AccessibleSelection` 事件中停止旧计时器，将捕获当前 notifier 的单次 Swing Timer 存回静态 `CAccessible.timer`。单次回调会转发通知，其字节码不清空该字段。此前 heap 的第一段强引用链和该实现一致，说明等待计时器到期并不能验证对象释放。仍需用不相关的原生可访问选择事件替换最后一次通知、对照原窗口样本，才能判断面板有无其他存活链。复核输出 `build/openspec-followthrough-20261120/jbr-accessible-selection.javap.txt`。Doc View 沿用 [平台 Disposer](https://plugins.jetbrains.com/docs/intellij/disposers.html) 释放自己的资源。


再用同一包、另一个隔离进程 PID 28920 验证从 GET 切到 HTML 后立即点 `nativeForm`，最终 HTML 显示 `/native/form` 和 POST。关闭后，从内置文件查找列表按 ↓ 选中另一条无关记录，未打开文件。四次 live GC 的 `EditorImpl` 数量依次为 3、4、4、3，说明该次最终回到原编辑器计数；`PreviewForm`、`MarkdownJCEFHtmlPanel`、`RemoteBrowser` 仍从各 0 到各 1 并保持在 1。未证实本次事件实际替换了 JBR timer 的目标，也没有证明浏览器层回到原基线。对应 `preview-selection-{baseline,open,closed,other-list}.hist`。

## 2026-11-20 预览组件分离与最终原生回归

旧包的堆记录显示 macOS JBR 辅助功能仍可暂时持有已关闭的 Swing 树。定向回归把 HTML 面板实际挂入预览容器并关闭，旧代码失败，组件仍有 parent。修复在释放 viewer 和 HTML 面板之前先移除预览内容。只处理 Doc View 自己的组件，没有调用 JBR 内部 API。

冻结包 ZIP SHA-256 `a389dce4c82b991071e0ce0351597489b1c3b17345ee20d625e00676b4f166e7`，安装 JAR `53bed374acd5b9dbb5a49a11f7b9dcc1c64bba888e49713e8f2c35ddfc46b305`。定向测试 21/21；全量标准 Gradle 测试 99 suites、582 项，0 失败、错误或跳过。构建、配置检查通过，同 ZIP 在 `263.7031.23` 与 `263.7307.200` 的 Verifier 1.411 均 Compatible，各 16 处已知 Experimental API。复验日志和哈希在 `build/openspec-closure-20261120/`。

同一包在正式 IU `263.7307.200`、JBR 25、macOS Apple Silicon 验收：

- 仅打开 Markdown 源码预览不会创建 HTML panel 或新增 browser。基线为 2 个 editor、0 个 PreviewForm/MarkdownJCEFHtmlPanel、1 个宿主已有 RemoteBrowser。源码预览为 3/1/0/1，切换 HTML 后为 3/1/1/2。按 Esc 后面板归零、浏览器回到 1，后续 live GC 中四个计数全部恢复到 2/0/0/1，原 Java 编辑器可用。Spring 批量导出关闭后的采样同样恢复至基线。
- 同一会话重开恢复 HTML 模式。Spring GET、JSON、Form 连续切换后最终显示 `/native/form` 和 POST；批量导出 `HttpAcceptanceController.md` 含三条路径。整类 Dubbo 预览显示 `FictionalAnnotatedService#annotatedCatalog` 与 Dubbo 标记，单项导出也回读到对应签名。既有定向测试覆盖旧结果迟到。
- 空类显示“没有 Doc View 内容。”。独立虚构项目的无效 Velocity 模板在 HTML 与源码都显示“渲染 Doc View 失败。”。失败预览关闭后最终未见 PreviewForm 或新增 MarkdownJCEFHtmlPanel/RemoteBrowser。初始化或开始显示失败的资源释放由 EditorFactory 夹具覆盖。
- 同一 ZIP 在另一次隔离启动中用 JDI 只暂停预览 worker，实际按 `⌘⇧F2` 并选中“Doc View 预览”。IDEA 显示“正在停止”，BridgeTaskSupport 记录 UI cancellation，放行后未见迟到内容重新打开 popup。第一轮旧诊断器也调用了 indicator cancel，保留该轮但不算独立原生触发证据。独立一轮只用 JDI 调用活动任务的 indicator cancel 以保留前台窗口，真实 HTML 和源码都显示“已取消加载 Doc View。”，随后 Esc 关闭。
- 调试轮次复查发现有一个仍未关闭的 Pin 弹窗，JDI 读回 `disposed=false`。关闭该虚构项目后，live GC 中 PreviewForm、MarkdownJCEFHtmlPanel 和 RemoteBrowser 均归零。此轮按项目生命周期验收。相应日志未出现 Markdown HTML 根结构或 EDT PSI read-access 错误。插件热卸载不在本 change 验收内。

原生样本见 `jcef-native-{baseline,source,html,closed,settled,export-closed,failure-settled}.hist` 与 `jcef-cancel-project-settled.hist`，诊断器输出见 `jcef-native-cancel-clean.log`、`jcef-visible-cancel.log`。本轮还看到 GET 文档冗余的 Content-Type，已交由共享 Spring 文档模型路径独立核查。
