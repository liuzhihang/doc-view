# Verification

## 已完成的 targeted 证据

Round 36 的 targeted 运行通过 `MethodDocScratchPreviewTest` 4 项 PSI 回归用例和 `DocViewBackgroundTasksTest` 1 项取消回投用例。覆盖非 EDT 生成与模板内容、生成中取消、失效方法 PSI、生成异常通知，以及回投前取消。

Round 37 全量运行发现 191 项测试，其中唯一失败位于 `GenerationPerformanceBaselineTest` 的目录顺序断言；该失败由性能测试维护者单独处理，不能记作本 change 的 targeted 结果，也不能记作全量门禁通过。

## 发布边界用例排查与结果

Round 39 首次编译新增测试时，两个匿名 `ScratchFileService` 测试替身未实现 IU261 所需的 `VirtualFileEnumerationAware.extractFileEnumeration()`，导致 `compileTestJava` 失败。测试替身按目标 API 修正；之后又因替身没有可靠控制实际 `RootType.findFile` 所用的已缓存服务，Round 40 的 Scratch 创建/写入失败用例均等待通知超时。该次结果没有证明生产通知路径存在缺陷。

后续 fixture 改用真实 Scratch service：创建失败场景使用超过文件系统单一文件名长度限制的方法名；写入失败场景监听真实 VFS `fileCreated` 事件，将生成的 `status.md` 设为只读，并断言注入成功。两者都先确认 Markdown 生成完成，再确认收到通知且没有打开失败的 Scratch 文档。

Round 41 targeted 结果为 9 项通过、0 失败、0 错误、0 跳过：

- `MethodDocScratchPreviewTest`：6 项通过，其中包括 Scratch 创建失败和写入失败两项。
- `DocViewBackgroundTasksTest`：2 项通过，其中包括 `testQueuedReadResultIsDroppedWhenProjectDisposesBeforeEdtDelivery`。
- `ClassNodeCancellationTest`：1 项通过。

XML 报告：`/tmp/doc-view-reliability-20261018/round41-TEST-com.liuzhihang.doc.view.ui.window.MethodDocScratchPreviewTest.xml`、`/tmp/doc-view-reliability-20261018/round41-TEST-com.liuzhihang.doc.view.utils.DocViewBackgroundTasksTest.xml`、`/tmp/doc-view-reliability-20261018/round41-TEST-com.liuzhihang.doc.view.ui.window.ClassNodeCancellationTest.xml`；Gradle 日志：`/tmp/doc-view-reliability-20261018/round41-publication-boundaries.log`。

`testQueuedReadResultIsDroppedWhenProjectDisposesBeforeEdtDelivery` 在回调排队后让代理 `Project.isDisposed()` 返回 `true`，验证回投守卫丢弃结果。它没有关闭真实 IDEA 项目，也不证明完整项目关闭生命周期下的行为。

## 尚未完成的集成验证

Round 42 全量 Gradle 门禁由主代理运行中，本文件更新时尚无结果，故任务 3.1 保持未勾选。IU 2026.1 原生双击/Enter、真实项目关闭与真实 IDE Scratch 发布边界尚未完成；原生场景不可用时不得将 targeted 测试替代为原生通过，任务 3.2 保持未勾选。

## Round42 整批复验（2026-10-18）

主代理完整执行 `./gradlew test verifyPluginProjectConfiguration buildPlugin verifyPlugin --rerun-tasks`：40 个 suite、196 项测试，0 失败、0 错误、0 跳过；构建成功，IU261.22158.277 Plugin Verifier verdict 为 `Compatible`。包括本 change 的最新修改及 Round41 的取消/释放守卫和真实 Scratch 文件失败用例。日志与精确 XML：`/tmp/doc-view-reliability-20261018/round42-fullgate.log`、`round42-fullgate-TEST-*.xml`；包审计为 `round42-package-audit.json`。

Round42 ZIP SHA-256：`e214cd4525968356c936f798191006391415019afe48ee4e5235c02f342372d7`。7 个离线章节与仓库正文逐字节一致，生产包未包含测试框架、夹具或 MCP PoC。测试运行使用 JBR25，生产目标仍为 Java21；构建配置中跳过的 searchable-options 任务不计为测试跳过。完整包与原生已安装版本的区别见 [路线图](../../../docs/roadmap.md#本轮验证与续作入口)。

macOS 锁屏阻挡后续原生操作。IU262 测试实例仍安装 Round34，尚未安装或操作 Round42；本节自动验证不能替代新包原生双击/Enter、项目关闭、多项目隔离、主题及窗口矩阵验收，相关任务保持未完成。

## Round46 新包原生文档打开

2026-10-18 在 IU262 隔离 profile 安装 Round42 后，对 `doc-demo` 的 `UserController.respGenericTest` 方法节点执行双击，实际打开 Scratch `用户相关接口/测试接口.md`，内容包含 `/user/respGenericTest`、POST 及真实参数 Markdown。该结果验证了当前后台生成路径的最终文档打开，不证明生成中途取消、项目关闭或所有方法类型的原生边界；任务 3.2 保持未全部完成。普通接口开关与树恢复证据同时记录在 `api-tree-navigation/verification.md` 的 Round46 节。

## IU 261 原生方法打开、取消与项目关闭验收（2026-10-22）

环境：隔离官方 IntelliJ IDEA IU `261.22158.277`，macOS `27.0.1`、JBR `25.0.2+10-b329.72`、aarch64；项目为专用验证项目 `/Users/liuzhihang/workspace/doc-demo`。安装包清单 [package.json](/tmp/doc-view-native-20261022-all/iu261/package.json) 记录 ZIP SHA-256 `df21ba775bd79032a3667e7ddef3984efee271a0511140977ceb33415cf50adb` 和插件 JAR SHA-256 `6eec14f6752b1a31aa9f16e1bffde276bacf7d92b6f18b2a24fd85534b0e4c2e`；隔离实例 [idea.log](/tmp/doc-view-native-20261022-all/iu261/log/idea.log) 记录 IU build 与运行时环境。

### 未暂停的成功打开路径

- 在 `UserFacade.queryUserDateAccount` 方法节点按 Enter，IDEA 打开 `普通接口/查询用户昨日账户信息/分级.md`，文档含该方法的 Markdown 正文与参数；见 [iu261-method-enter.txt](/tmp/doc-view-native-20261022-all/iu261-method-enter.txt)。
- 双击 `UserFacade.mapUser` 方法节点，IDEA 打开完整 `普通接口/mapUser.md` Scratch，正文包含请求、响应和嵌套字段；见 [iu261-method-doubleclick.txt](/tmp/doc-view-native-20261022-all/iu261-method-doubleclick.txt)。
- 这两条成功路径没有使用调试暂停。生成期间捕获的 [iu261-method-generating.txt](/tmp/doc-view-native-20261022-all/iu261-method-generating.txt) 仍能读取 IDEA 编辑器、Doc View 方法树和 `Generate method documentation` 后台进度；取消后的 [iu261-method-cancel-confirmed.txt](/tmp/doc-view-native-20261022-all/iu261-method-cancel-confirmed.txt) 显示 `Stopping - Generate method documentation`，可见界面在后台生成/取消期间仍能响应。

### 受控取消与项目关闭

为稳定命中交付边界，以下两项测试使用远程调试器暂停后台 pooled thread；它们验证取消/释放后的结果发布边界，不测量自然运行耗时或性能。

- **取消**：在 `CustomFileUtils.lambda$openMd$0:69` 暂停生成线程后，通过 IDEA 取消后台任务。调试日志 [native-method-scratch-cancel.log](/tmp/doc-view-native-20261022-all/debug/native-method-scratch-cancel.log) 记录 indicator `myCanceled=true`，界面显示 `Stopping`。恢复线程后，取消前清单 [scratch-before-method-cancel.json](/tmp/doc-view-native-20261022-all/scratch-before-method-cancel.json) 中两个既有 Scratch 的 SHA-256 均保持一致：`mapUser.md` 为 `f1f3ca14a0f8d5ed4cb1fb1cb4f738d12b0f461e4c29c16ef23bb66d007e37c7`，`分级.md` 为 `01bd110d89a0000357c31ae6415564f257b6d9d3ab90a8580b32b962c41dcd99`；恢复后的 Scratch 目录没有 `mapUser22` 新文件。
- **关闭项目**：在 `DocViewBackgroundTasks.invokeLaterIfNotCanceled:85`（后台 read 已完成、EDT 交付前）暂停回投线程，通过 IDEA `File > Close Project` 关闭 `doc-demo`，界面切换到 `supported` 项目；见 [iu261-method-project-closed.txt](/tmp/doc-view-native-20261022-all/iu261-method-project-closed.txt)。调试日志 [native-method-project-close.log](/tmp/doc-view-native-20261022-all/debug/native-method-project-close.log) 记录暂停与恢复。恢复线程后再次核对 Scratch 目录：仍只有上述两个文件，二者哈希与取消前清单相同，没有迟到的 `mapUser22` Scratch。

任务 3.2 的原生打开、受控取消和真实项目关闭场景均已完成。暂停仅用于稳定验证后台交付边界；成功 Enter/双击路径未暂停。本节不提供自然生成延迟、吞吐或性能结论。
