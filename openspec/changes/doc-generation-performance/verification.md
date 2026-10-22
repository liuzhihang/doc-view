# Verification

## 改动前基线

证据来自 `GenerationPerformanceBaselineTest` 在生产代码改动前的目标运行，XML 保存在本机 `/tmp/doc-view-reliability-20261018/performance-baseline-red.xml`。测试运行时间为 2026-10-18 03:14（Asia/Shanghai）。结果为 6 tests、3 failures、0 errors、0 skipped；其中只有两个缓存失败可作为生产缺陷证据。取消测试当时在 `runProcess` 安装 indicator 之前调用 `cancel()`，而 `runProcess` 启动会重置该状态，因此该失败属于无效的预取消 fixture，不能据此认定生产生成路径有缺陷：

- `testSchemaCacheRetainsOnlyCurrentPsiSettingsAndTemplateEpoch`：修改 PSI、设置和模板后，entry 数依次为 `[1, 2, 3, 4]`，缓存内 `Body` 节点从旧代累积至 48。
- `testLateOldEpochCacheWriteCannotRestoreAnExpiredSnapshot`：新 epoch 写入后，迟到的旧 key 仍被接受；最终有 2 个 entries，旧 snapshot 仍可读。

当前测试 fixture 已改为在 `runProcess` 安装 indicator 后、调用生成前取消。修正 fixture 后该用例通过；它证明这个正确作用域下的预取消请求会被当前生成路径中止，不能追溯证明初始 RED 是生产缺陷，也没有验证生成中途取消的延迟或部分结果发布。

Spring/Dubbo 冷热生成、Markdown 渲染和目录扫描的另外 3 项通过。以下时间是 light-fixture 下 7 个样本的 p50/p95，单位毫秒；它们用于描述本机基线，不是 IDE 原生交互耗时或跨机器 SLA。

| Fixture | 方法数 | 每层字段 | 嵌套层数 | Body 节点 | 冷构建 p50/p95 | 热构建 p50/p95 | Markdown p50/p95 | Markdown 字符 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Spring small | 4 | 3 | 1 | 32 | 0.943 / 1.155 | 0.698 / 1.072 | 0.434 / 0.577 | 2,644 |
| Spring medium | 12 | 10 | 3 | 528 | 5.567 / 7.571 | 4.152 / 4.530 | 1.687 / 1.898 | 31,600 |
| Spring large | 30 | 24 | 6 | 5,250 | 24.092 / 26.618 | 20.242 / 21.978 | 3.799 / 8.255 | 348,130 |
| Dubbo medium | 16 | 12 | 4 | 1,040 | 12.769 / 15.929 | 7.975 / 8.422 | 2.070 / 2.439 | 62,476 |

目录扫描固定为 1 个 module。关闭普通接口扫描时记录 8 个 fixture API class、8 个方法，p50/p95 为 0.914 / 79.112 ms。开启后报告 28 个 PSI class、24 个方法，p50/p95 为 5.234 / 28.267 ms；这 28 个 class 包括测试安装的 Spring/Dubbo 注解桩，因此不能解释为 28 个用户业务接口。测试确认普通接口开关能加入 `PlainApi` 并保留已识别的接口；此基线没有证明原生 Tool Window 的端到端刷新时间或完整成员顺序。

基线 XML 报告 Java runtime `25.0.2+10-b329.72`、10 个处理器；当时未记录 Java vendor/VM 名称，因此不能据此标注为 JBR。测试设置的 `idea.build.number` 未提供，IDEA build 字段为 `unknown`。运行时不是 Java 21，也不代表插件的编译目标。后续 harness 将通过 IntelliJ `ApplicationInfo` 读取实际 IDE build，并记录 Java vendor/VM。此运行没有 CPU/allocation profiler，也没有原生 IDEA 取消验收；时间样本只能说明固定 light fixture 的观测结果。

## 缓存与生成 targeted GREEN

主代理在 2026-10-18 03:30（Asia/Shanghai）运行 `GenerationPerformanceBaselineTest`；XML 为 `/tmp/doc-view-reliability-20261018/round12-TEST-com.liuzhihang.doc.view.contract.GenerationPerformanceBaselineTest.xml`。6 tests、0 failures、0 errors、0 skipped。环境由测试读取为 IU `261.22158.277`、JetBrains s.r.o.、OpenJDK 64-Bit Server VM `25.0.2+10-b329.72`、10 processors、每组 7 个样本。此环境使用 Java 25，不代表项目 Java 21 编译目标。

缓存断言读回结果：PSI/settings/template 变化后的 entries 为 `[1, 1, 1, 1]`，缓存 `Body` 节点总数为 12；旧 epoch 迟到写入后 entries 为 1，旧 snapshot 不可读。修正后的预取消 fixture 也通过，但仅证明测试中该取消时序能中止生成。

同一 harness 的修复后采样如下。与改动前样本并列是观测数据，不构成速度提升结论；没有 CPU/allocation profiler，也没有证明跨运行波动或 IDE 原生交互耗时。

| Fixture | 方法数 | Body 节点 | 冷构建 p50/p95 ms | 热构建 p50/p95 ms | Markdown p50/p95 ms |
| --- | ---: | ---: | ---: | ---: | ---: |
| Spring small | 4 | 32 | 0.936 / 1.074 | 0.640 / 0.807 | 0.382 / 0.396 |
| Spring medium | 12 | 528 | 4.600 / 4.696 | 3.739 / 4.339 | 1.634 / 3.532 |
| Spring large | 30 | 5,250 | 28.753 / 31.749 | 20.636 / 21.737 | 3.664 / 4.137 |
| Dubbo medium | 16 | 1,040 | 11.768 / 16.486 | 8.055 / 10.153 | 2.082 / 2.195 |

目录扫描 fixture 报告 1 个 module。注解接口模式为 8 classes/8 methods，p50/p95 `0.592 / 17.877 ms`；普通接口模式为 28 PSI classes/24 methods，p50/p95 `5.977 / 29.821 ms`。28 个类包含本测试安装的 Spring/Dubbo 注解桩，不代表 28 个用户业务接口。

同一 round12 中主代理还运行了 8 个专项测试类，共 41 tests、0 failures、0 errors、0 skipped；XML 见 `/tmp/doc-view-reliability-20261018/round12-TEST-*.xml`。这批包括性能 harness、OpenAPI 3 参数、平台上传、项目文档导出和上传服务测试；它没有包括 `SpringMarkdownContractTest`、`DocViewServiceDuplicateNameCompatibilityTest`、`ApiTreeRefreshTest`，也不是完整 test suite。

这只是性能 change 的专项目标测试结果；完整 `./gradlew test`、`verifyPluginProjectConfiguration`、`buildPlugin`、`verifyPlugin` 和 IU 2026.1 原生取消/响应验收仍待主代理补录。未验证项不能据此描述为通过。

## 中途取消与 JFR 采样

round16 的性能类8项全部通过。新增两项在 Spring/Dubbo 的第一篇真实方法文档生成完成后取消当前任务，断言只构建1个方法且整类调用抛出 PCE、不返回部分列表。之前的预取消检查仍保留。它们验证方法边界的取消，不把它们描述为每种递归深度的取消延迟测量。

round22 的 `GenerationCancellationTraversalTest` 首次运行 1 项失败：测试依靠覆盖 `ProgressIndicator.checkCanceled()` 来观察 Body 树并触发取消；目标平台的取消检查快路径在 indicator 尚未取消时没有调用该观察回调，因此 fixture 未能发出取消信号。该失败不能作为生产递归没有取消检查的证据。修正后的 round25 fixture 在测试专用 Body 子列表实际加入第一个嵌套字段时同步取消 indicator；随后真实 `ParamPsiUtils.buildBodyList()` 遍历在下一个字段加入前抛出 PCE，结果树只保留 `first`，该 targeted 测试 1/1 通过。日志：`/tmp/doc-view-reliability-20261018/round25-cancellation-targeted.log`；XML：`build/test-results/test/TEST-com.liuzhihang.doc.view.contract.GenerationCancellationTraversalTest.xml`。

字段用例直接调用低层 `ParamPsiUtils.buildBodyList()`，并未构建或发布 `DocView`/Markdown；Spring/Dubbo 方法级取消的整类列表丢弃仍由 round16 中途取消测试证明。`ApiTreeRefreshTest` 覆盖刷新结果回调状态，但没有在真实 `RootNode`/`ModuleNode` 目录遍历中途取消；现有 Preview 状态测试也未证明旧 render cancel 回调不能覆盖较新的请求。因此 tasks 2.3 保持未完成。IU 2026.1 原生取消和响应验收继续由 3.3 跟踪，本记录不将 targeted fixture 结果扩展为原生体验通过。

round31 新增预览发布测试时，`compileTestJava` 因把 `ProgressManager.getProgressIndicator()` 当作静态 API 而失败，未执行测试。测试改用目标平台 API `ProgressManager.getInstance().getProgressIndicator()` 后，round32 执行 `./gradlew test --tests '*GenerationCancellationPublicationTest' --tests '*GenerationCancellationTraversalTest' --rerun-tasks`，2 tests、0 failures、0 errors、0 skipped；编译和测试均成功。日志：`/tmp/doc-view-reliability-20261018/round32-generation-cancel-publication.log`；XML：`/tmp/doc-view-reliability-20261018/round32-TEST-com.liuzhihang.doc.view.contract.GenerationCancellationPublicationTest.xml` 与 `round32-TEST-com.liuzhihang.doc.view.contract.GenerationCancellationTraversalTest.xml`。

`GenerationCancellationPublicationTest` 使用真实 Spring Java PSI 和生产 `PreviewForm.buildDoc()` 后台任务及 `SpringDocViewServiceImpl`，让首个 endpoint 的含嵌套 DTO 文档完成后取消当前任务；整类生成在下一方法边界中止。测试确认取消状态到达预览 UI，`docViewList`、当前文档、导出快照和预览 Markdown cache 均未发布。它验证的是跨方法边界的完整生成/预览取消链路，不是递归内部取消；递归 fixture 仍直接验证低层 `ParamPsiUtils.buildBodyList()`，没有在同一条真实预览链路中触发。DTO schema cache 可以保留已完整解析的 schema，本测试不把该缓存误称为无写入。真实项目目录扫描中途取消及不发布部分目录、较新请求后的旧 render cancel 回调，以及 IU 原生取消体验仍未验证；tasks 2.3 与 3.3 继续保持未完成。

round19 使用临时 Gradle init script，仅给测试 worker 增加 `-XX:StartFlightRecording=filename=/tmp/doc-view-reliability-20261018/generation-profile.jfr,settings=profile,dumponexit=true`。没有修改仓库构建配置或加入生产遥测。实际运行性能8项、Clear Cache 4项、后台结果排队后取消1项、Preview生命周期12项、ApiTreeRefresh 4项，共29项，全部通过。XML：`/tmp/doc-view-reliability-20261018/round19-profile-TEST-*.xml`。

录制文件和统计摘要分别为 `generation-profile.jfr`、`generation-profile-summary.json`（同一临时目录）。JFR有363个 execution samples、2701个 allocation samples；包含生产 Doc View 栈的分别为65和259个。只保留栈内可见性能 harness 的 `measure`/`measureScan` 后，分别是49和220个。栈深度64可能截断更深的调用，计数不是全量调用次数。

- 49个可归因执行样本中，29个包含响应示例生成 `getRespBodyJson`，26个包含 `getFieldsAndDefaultValue`，7个包含 Markdown 渲染。它们是包含式调用栈计数，彼此重叠，不能相加为时间占比。
- 分配样本指向 Markdown/Velocity、响应示例生成和 PSI 路径；JFR weight 是采样估算，不是保留堆大小或泄漏证据。旧 epoch 保留量仍以独立缓存断言为准。
- 这是一份短时测试 worker 采样，含 fixture/IDE 开销；不足以证明稳定热点或支持再增加缓存。响应示例的重复字段遍历可作为将来真实大项目 profile 的候选，本轮不改生成或扫描算法。

无 profiler 的 round16 重测沿用相同固定 workload、7个样本及 IU261/JBR25环境：Spring large 冷/热 p50为28.773/23.890ms、p95为36.765/25.207ms；相较改动前24.092/20.242ms和26.618/21.978ms，没有提升证据，部分观测值更高。小/中 workload、渲染与目录扫描也有跨轮波动；例如注解目录 p95 在 round12/16 为17.877/77.347ms。保留本次缓存生命周期正确性和协作取消修复，不宣称速度优化、SLA或原生响应验收通过。JFR录制轮的计时不与无录制基线作性能比较。

## 完整回归与包检查

round21 执行 `./gradlew test verifyPluginProjectConfiguration buildPlugin verifyPlugin --rerun-tasks`，35个套件180项测试全部通过，无失败/错误/跳过，包含本change8项及Spring golden、重复名称、目录刷新、Preview生命周期全部测试。实际 IU261.22158.277 Verifier verdict 为 Compatible，构建成功。报告仍有 bundled 可选/跨平台依赖解析提示，不称为零警告。

安装包SHA-256为 `01e67f753ce9fa388c0dbb607fe940ddb9a371dc8570aa6347ad27a3c2c42020`；遍历ZIP内所有JAR确认无本性能harness、testsupport、JUnit/TestFramework或MCP PoC。证据保存于 `/tmp/doc-view-reliability-20261018/round21-fullgate-TEST-*.xml` 和 `round21-package-audit.json`。这完成自动门禁，不替代原生性能/取消体验验收。

round37 的全量门禁共运行 191 项，1 项失败：`GenerationPerformanceBaselineTest.testDirectoryScanBaselineSeparatesNormalInterfaceSetting`。失败发生在重复扫描断言：两次扫描识别的类成员相同，但列表顺序不同。根因是 `AllClassesSearch.findAll()` 不承诺稳定迭代顺序；该结果暴露的是目录顺序契约缺失，不是成员识别变化。后续在 `api-tree-navigation` 中补充类节点确定性排序契约，并增加真实 PSI 顺序回归用例；`ClassNode` 每方法取消检查也在本轮补充。上述改动尚待主代理串行复跑，当前记录不把 round37 失败标记为已修复，也不把新增测试描述为通过。

round38 targeted 共 21 项、1 项失败，其他 20 项通过（`GenerationPerformanceBaselineTest` 8、`ApiTreeNavigationTest` 8、`ApiTreeRefreshTest` 4 均通过）。唯一失败是 `ClassNodeCancellationTest` 的局部子节点数量断言：预期 1、实际 0；此前取消传播、过滤调用数和名称解析调用数断言均通过。失败来自测试对“首个 `MethodNode` 已构造完成并加入列表”的过强假设：测试 seam 在 `DocViewUtils.getName()` 的读操作期间取消，平台在该读操作返回前抛出 PCE，因此构造未完成，`methodNodes.add()` 尚未执行。源码路径与 XML (`/tmp/doc-view-reliability-20261018/round38-TEST-com.liuzhihang.doc.view.ui.window.ClassNodeCancellationTest.xml`) 支持这是测试断言问题，不是需要保留半成品节点的产品行为。已移除对子节点保留的要求，并把 spec 限定为后续方法不再筛选/构造、PCE 外传且部分快照不发布。

round40 `ClassNodeCancellationTest` targeted 1/1 通过。修订后的真实 PSI fixture 断言取消异常向外传播、只进入首个匹配方法的过滤与名称解析，后续方法不再筛选/构造；它不要求被取消打断的局部节点保留。该用例只验证 `ClassNode` 方法循环边界，不等同于真实项目 `RootNode`/`ModuleNode` 扫描途中取消、递归字段的完整生成链路，或 IU 2026.1 原生取消响应。因此性能任务 2.3 仍未完成，待补完整递归链及原生验证；不把 round40 单项结果扩展为整个 requirement 已通过。

## Round42 整批复验（2026-10-18）

主代理完整执行 `./gradlew test verifyPluginProjectConfiguration buildPlugin verifyPlugin --rerun-tasks`：40 个 suite、196 项测试，0 失败、0 错误、0 跳过；构建成功，IU261.22158.277 Plugin Verifier verdict 为 `Compatible`。包括本 change 的最新修改及 Round41 的取消/释放守卫和真实 Scratch 文件失败用例。日志与精确 XML：`/tmp/doc-view-reliability-20261018/round42-fullgate.log`、`round42-fullgate-TEST-*.xml`；包审计为 `round42-package-audit.json`。

Round42 ZIP SHA-256：`e214cd4525968356c936f798191006391415019afe48ee4e5235c02f342372d7`。7 个离线章节与仓库正文逐字节一致，生产包未包含测试框架、夹具或 MCP PoC。测试运行使用 JBR25，生产目标仍为 Java21；构建配置中跳过的 searchable-options 任务不计为测试跳过。完整包与原生已安装版本的区别见 [路线图](../../../docs/roadmap.md#本轮验证与续作入口)。

macOS 锁屏阻挡后续原生操作。IU262 测试实例仍安装 Round34，尚未安装或操作 Round42；本节自动验证不能替代新包原生双击/Enter、项目关闭、多项目隔离、主题及窗口矩阵验收，相关任务保持未完成。

## Round43 Spring 递归取消 targeted（2026-10-18）

主代理运行 `GenerationCancellationPublicationTest` 与 `GenerationCancellationTraversalTest`：共 3 项，0 failures、0 errors、0 skipped。新增 `testCancellationInsideNestedResponseTraversalDoesNotPublishPartialPreview` 使用真实 Spring PSI、生产预览后台任务和 `SpringDocViewServiceImpl`，在唯一嵌套 response 字段的排除检查期间取消当前 indicator。测试确认递归到达该字段、取消状态生效、该 fixture 唯一 endpoint 的 Spring method build 未正常返回、Preview 到达取消终态，且文档列表、当前文档、导出快照、Markdown cache 和 response schema cache 均无部分结果。此 fixture 只有一个 endpoint，不据此声称它阻止了后续 endpoint；既有双 endpoint 方法级用例单独验证方法完成边界取消。

取消触发于字段排除检查期间，不证明该字段的 `Body` 已入列；Round25 的低层 traversal 用例仍负责证明第一个嵌套字段入列后取消会阻止后续字段。日志：`/tmp/doc-view-reliability-20261018/round43-recursive-cancel.log`；XML：`round43-TEST-com.liuzhihang.doc.view.contract.GenerationCancellationPublicationTest.xml`、`round43-TEST-com.liuzhihang.doc.view.contract.GenerationCancellationTraversalTest.xml`。这组 targeted 结果不替代完整生成/目录扫描取消矩阵或 IU 2026.1 原生验收；任务 2.3 和 3.3 保持未完成。

## Round44 全量测试与 Round45 旧取消回调（2026-10-18）

Round44 运行 `./gradlew test --rerun-tasks`，40 个 suite、197 项测试，0 failures、0 errors、0 skipped。包含上述递归取消新用例；未再次执行 buildPlugin 或 Verifier。日志：`/tmp/doc-view-reliability-20261018/round44-full-test.log`；XML：`round44-full-test-TEST-*.xml`；汇总：`round44-test-summary.json`。

Round45 在既有 `PreviewLifecycleTest` 中补充迟到取消回调断言：当前请求为4、当前文档与 Markdown 已就绪时，在 EDT 调用生产 `handleRenderCancellation(3)`，确认编辑器文本、`currentMarkdownText` 和当前文档对象均未改变。测试通过既有反射入口覆盖回调守卫，没有新增生产 hook；它不是一次真实并发渲染或 JCEF 原生操作。`./gradlew test --tests '*PreviewLifecycleTest' --rerun-tasks` 的12项全部通过，0 failures、0 errors、0 skipped。日志：`/tmp/doc-view-reliability-20261018/round45-stale-cancel.log`；XML：`round45-TEST-com.liuzhihang.doc.view.ui.PreviewLifecycleTest.xml`。

上述新增验证补齐递归生成链路和旧取消回调证据；本段记录的是 2026-10-18 Round45 时的状态，当时真实目录扫描中途取消、原生响应与 JCEF 验收尚未完成。2026-10-22 的目录取消验收见下节；本次不据此宣称整类 Spring/Dubbo 预览的 IU 原生取消与耗时场景已通过，3.3 仍保持未完成。Round45 时的插件构建及 Verifier 以 Round42 记录为准，最新 candidate233 门禁见下节。


## IU 261 原生目录刷新取消与 candidate233 门禁（2026-10-22）

在隔离的 IU 261 fixture 项目中进行原生 Tool Window 刷新取消。有效取消证据为 `native-tree-cancel.log` 的 sequence 2/3：后台遍历停在 `ClassNode#updateNode:51`；通过 IDEA 的 Stop Background Process 停止 `Searching Doc View interfaces`，恢复挂起线程后命中 EDT 上的 `DocViewWindowPanel$2#onCancel:160`。`iu261-tree-native-stop-menu.txt` 与 `iu261-tree-native-stop-confirmed.txt` 保存了停止菜单和处理中状态。最终 `iu261-tree-cancel-native-final.png` / `.txt` 显示“Refresh cancelled. Click Refresh to try again.”，并保留 `billing-api`、`catalog-api` 两个模块节点。此前 sequence 1 虽命中同一遍历断点，但恢复后刷新成功完成；该次不计作取消通过。

原生画面证明取消终态和两个既有模块节点仍可见；目录快照不发布由 `ApiTreeRefreshTest` 的完整快照边界回归共同覆盖。该受控调试暂停只验证停止路径，不测 `AllClassesSearch.findAll()` 内部响应，也不代表自然用户操作的取消延迟或扫描耗时。

Native 运行时隔离 profile 内插件 JAR SHA-256 为 `d5f8e6d774cac0d355d492fab4e765744275cedd5f2572cf00c599eb38191947`，与 candidate233 ZIP 内 JAR (`0c041e4f478d15f06fe3f5fa45bc79f4467dee115152132f378ee4ce5c45078f`) 不同，因此不声称整个安装包逐字节相同。对照后，刷新/取消相关的 `ClassNode`、`RootNode`、`ModuleNode`、`DocViewWindowPanel` 和 `DocViewWindowPanel$2` 五个生产 class 与 candidate233 ZIP 内 JAR 逐字节匹配；本节原生证据适用于这些被验收的逻辑。candidate233 ZIP SHA-256 为 `42d495d877b5f2527aae4152d54869a1483de4e4e77d02e1f75f45ea33ca8997`。

本轮 `candidate233-gate-summary.json` 记录 49 个 suite、233 项测试，0 failures、0 errors、0 skipped；`final-candidate-validation.log` 记录构建成功及 IU `261.22158.277` Plugin Verifier `Compatible`。这是自动门禁证据，与上面的原生验证分别记录。

因此 tasks 2.3 的目录取消 native 依赖已完成；tasks 3.3 继续保持未完成，仍需在原生 IDEA 验收整类 Spring/Dubbo 预览取消、Tool Window 与预览的实际响应时间和环境，并且不得把本轮受控暂停当作性能耗时数据。

## IU 261 candidate caaf 整类 Spring 预览与受控取消补验（2026-10-22）

原生环境为 IntelliJ IDEA IU `261.22158.277`、隔离 profile `/tmp/doc-view-native-20261022-all/iu261`，项目为 `/Users/liuzhihang/workspace/doc-demo`。最终交互包 candidate caaf 的 ZIP SHA-256 为 `caaf605f2a3599a65e6cc0a69b2a6900328a86442d3315a0b4e6bc322a833bc3`，profile 内插件 JAR SHA-256 为 `a143d7698663e54d31f905186950616232a93bcbfed663d856216f0e2a0ea950`，与 candidate234 门禁清单相符。该包门禁汇总为 49 suites、234 tests、0 failures/errors/skips；这只是候选包的自动门禁，不替代本节的原生范围。

- `UserController` 整类 Spring 源预览显示 8 条接口文档项，首项 Markdown 正文含 `/user/respGenericTest`、`POST`、请求及响应表；快照为 `/tmp/doc-view-native-20261022-all/iu261-caaf-spring-preview.txt`。这是一次完整类级 Spring 源/Markdown 预览成功，不包含 Dubbo，也不证明 HTML 预览成功。
- 加载期间 editor 中实际显示 `Loading Doc View…`。通过 IDEA Stop Background Process 选择 `Doc View preview` 后，JDI 先观察到 indicator `myCanceled=true`，随后命中 EDT 上 `PreviewForm$10#onCancel:781`；此时 `PreviewForm` 仍未 dispose。证据见 `/tmp/doc-view-native-20261022-all/iu261-caaf-spring-load-paused.txt`、`iu261-caaf-spring-stop-menu.txt`、`iu261-caaf-spring-stop-confirmed.txt` 与 `debug/caaf-spring-load-cancel.log`。恢复线程后 Computer Use 只绑定到主窗口，未取得 popup 取消终态或文档列表/正文快照；因此该证据只证明受控停止请求与取消回调到达，不证明用户可见取消终态或无部分结果。
- 该取消使用 JDI 暂停/恢复配合手动停止，不记录自然生成时长。未取得可用于报告的原生 Spring/Dubbo 生成时间分布或 Tool Window 自然刷新耗时；旧的目录刷新取消证据仍以 2026-10-22 `candidate233` 章节为界，不能移作 candidate caaf 的整类性能数据。

本次未完成 Dubbo 整类预览、用户可见取消后的无部分结果、JCEF 状态替换/取消、自然耗时与 Tool Window 刷新响应矩阵。tasks 3.3 保持未勾选；受控暂停不作为性能结果。

## IU 261 candidate235 原生预览、刷新及状态补验（2026-10-22）

环境为 IU `261.22158.277` 隔离 profile `/tmp/doc-view-native-20261022-all/iu261`，项目为 `/Users/liuzhihang/workspace/doc-demo`。使用 candidate235 ZIP（SHA-256 `2221a77261d1de30f09c202446cd4565080b1aeff733585a9c44587e496f448d`）；profile 内插件 JAR SHA-256 `62b06ff49d5f18c89f104983bd7dce5e028282134a30fe23c6689bbd481a9f78` 与包内 JAR 一致。

- Spring `UserController` 整类预览可见完整 8 项方法列表，首项 Markdown 正文可见请求路径、方法与参数表；Dubbo `AnnotationDubboService` 可见 2 项方法列表，首项 Markdown 正文含 `AnnotationDubboService#queryUserDateAccount` 与参数/返回表。证据分别为 `iu261-2221-spring-natural.txt` 和 `iu261-2221-dubbo-natural.txt`。
- doc-demo Tool Window Refresh 后可见 1 module、5 classes 的树，Refresh 按钮恢复可用，见 `iu261-2221-refresh-natural.txt`。
- `iu261-2221-natural-ui-timings.json` 记录三次“触发操作至首个 Accessibility/AX 可见结果”的单次观察值：Spring 8 项 695 ms、Dubbo 2 项 547 ms、1 module / 5 classes 刷新 867 ms。该区间包含 Computer Use 输入与观察延迟，各场景只有一次、没有冷/热基线或重复样本；它只能作为本机本次交互的观察上界，不能表述为纯生成/扫描耗时、性能提升或 SLA。
- Dubbo 受控加载取消中，JDI 看到 indicator 取消并命中 EDT `onCancel`，当时 Form 未关闭且 `currentDocView=null`；随后按 Esc 关闭时观察到 HTML panel/editor 引用清空，见 `debug/2221-dubbo-cancel-dispose.log`。暂停用于稳定观测，不计入时间；取消后的可见终态文本没有捕获，不据此确认无部分结果。
- 另在 `/tmp` 临时 fixture 验证空类状态为 `No Doc View content.`，无效 Spring 模板显示 `Failed to render Doc View.`；恢复并应用原模板后重新预览成功。证据见 `iu261-2221-preview-empty.txt`、`iu261-2221-preview-render-failure.txt`、`iu261-2221-template-restored-applied.txt`、`iu261-2221-preview-render-recovered.txt`。这些是状态/恢复核对，不是性能样本。

本轮仍没有重复样本及自然冷/热分布，也未确认用户可见取消终态、无部分结果、JCEF HTML 状态替换或完整导出路径。tasks 3.3 保持未勾选；上述一次性交互观察不作为性能结论。
