# Verification

## 自动化

- `/tmp/doc-view-reliability-20261018/round8-TEST-*.xml` 对应主线执行的 `./gradlew test verifyPluginProjectConfiguration buildPlugin verifyPlugin --rerun-tasks`：共 96 项，96 passed、0 failed、0 errors、0 skipped；Gradle 18/18 个 task executed。
- 同一轮 `PreviewLifecycleTest` 10/10 passed、0 failed；`SpringMarkdownContractTest` 1/1 passed、0 failed。预览测试包含 Spring/Dubbo 真实 fixture 输出、后台模板生成与 EDT Document 应用断言，以及 viewer 生命周期、源码状态回调和导出快照。
- Plugin Verifier 对 IU 261.22158.277 报告 Compatible，无 deprecated/experimental API 报告。round8 构建的插件 ZIP SHA-256：`b9e1365161403069ec251311b7403ab8f27caaff943ebd2bd2efae2f91997b1d`。
- 上述 round8 验证包含 `PreviewForm` 两处 `isDispatchThread()` 检查及错误时抛 `AssertionError` 的替换；替换保持原有非 EDT 约束。

## 联动范围

- `PreviewForm` 单项 Upload 入口复用已有 `upload(project, docView)` 后台 wrapper。此 change 不修改上传 payload 或服务协议；凭据读取与集成配置行为见[集成凭据 change](../integration-credential-storage/proposal.md)。本轮预览测试没有验证上传接口或凭据行为。

## 未验证

- Fixture 测试显式关闭 JCEF，因此未验证 HTML panel 的真实渲染、HTML 状态替换、后台 HTML 转换分支和释放；这些项仍待 JCEF 可用的目标 IDE 验收。
- 原生 IDE popup、Spring/Dubbo 交互及导出验收仍待执行；round8 build/Verifier 不替代原生或 JCEF 验收。

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

IU262 隔离 profile 安装 Round50 包后，从 `doc-demo` 的 `UserController.respGenericTest` 行标记打开真实文档，点击 Preview 后实际显示渲染后的标题、POST路径与参数表（`/tmp/doc-view-reliability-20261018/native-round50-preview-toggle.png`）。深色 IDE/浅色 editor scheme 与浅色 IDE/深色 editor scheme 的浮窗操作区均跟随 IDE 主题、正文保留 editor scheme（`native-round50-preview-dark.png`、`native-round50-preview-light.png`）。本轮完成一次渲染切换，未覆盖反复切换后资源数量/项目关闭/泄漏压力；相关原生生命周期任务不据此整体勾选。图标与背景修正、后续包及普通窗口恢复证据见 [API tree verification](../api-tree-navigation/verification.md)。

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
