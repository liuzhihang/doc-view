# Tasks

## 1. 方法级预览契约与回归测试

- [x] 1.1 使用真实 Spring Java PSI 与生产服务测试方法节点打开完整 Markdown Scratch；断言生成服务运行在非 EDT、文档路径和正文保持当前模板语义。Round 36 的 targeted 结果见 [verification.md](verification.md)。
- [x] 1.2 覆盖后台生成期间取消、失效 PSI 与生成异常：取消/失效时不创建或打开 Scratch，取消不通知，生成异常通知；`DocViewBackgroundTasksTest` 验证取消后的排队回调被丢弃。Round 36 的 targeted 结果见 [verification.md](verification.md)。Round 41 也通过了项目释放守卫模拟及 Scratch 创建/写入失败用例；真实 IDEA 项目关闭仍待 3.2 原生验收。

## 2. 后台生成与 Scratch 发布

- [x] 2.1 将单方法 PSI 解析、`DocView` 构建、Markdown 渲染和现有路径计算放进可取消后台 read task；确保仅字符串快照离开 read action，并由成功 EDT 回调创建/写入/打开 Scratch；通过 1.1 的正向测试。
- [x] 2.2 补齐发布边界验证：`DocViewBackgroundTasksTest.testQueuedReadResultIsDroppedWhenProjectDisposesBeforeEdtDelivery` 验证测试 `Project.isDisposed()` 守卫；`MethodDocScratchPreviewTest.testScratchCreationFailureNotifiesWithoutOpeningDocument` 与 `testScratchWriteFailureNotifiesWithoutOpeningDocument` 分别验证 Scratch 创建和写入失败会通知且不打开文档。Round 41 三项均通过；项目释放用例是模拟守卫测试，不等于真实 IDEA 项目关闭验收。结果见 [verification.md](verification.md)。

## 3. 集成验证

- [x] 3.1 运行 `./gradlew test verifyPluginProjectConfiguration buildPlugin verifyPlugin --rerun-tasks`，确认所有现有内容契约、后台任务和平台门禁通过；Round42 共40个suite、196项测试，0失败/错误/跳过，构建成功，IU261.22158.277 Verifier Compatible。
- [x] 3.2 在 IU 261.22158.277 / df21 包的 `doc-demo` 原生验收中，按 Enter 打开 `queryUserDateAccount` 文档、双击打开完整 `mapUser` Scratch；生成期间界面仍可访问。受控调试暂停下分别取消和关闭项目、恢复后台线程后，磁盘复核无迟到 Scratch；环境、文件哈希与边界见 `verification.md`。
