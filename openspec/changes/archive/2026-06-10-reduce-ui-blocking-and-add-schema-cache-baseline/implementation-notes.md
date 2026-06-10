# 实现记录

## EDT / Startup 审计

- Startup：`DocViewStartupNotification` 只读取 `ApplicationSettings`、插件 descriptor，并在版本变化时发送 notification；未发现 Spring/Dubbo 全项目扫描、DTO schema 预热、Markdown 预渲染、export、upload 或 cache 预加载。
- Preview：`PreviewForm` 构造期间同步调用 `buildDoc()`，并在 catalog selection listener 中同步调用 `DocViewData.markdownText(...)` 和 Markdown HTML 生成；属于需要迁移的 UI 阻塞热点。
- 参数编辑器：`ParamDocEditorForm` 构造期间同步调用 `ParamPsiUtils.buildBodyList(...)`，`Copy as Json` action 同步调用 `ParamPsiUtils.getFieldsAndDefaultValue(...)`；属于递归 DTO 构建热点。
- Tool window：`DocViewWindowPanel` 构造后会触发 catalog 刷新，当前已使用 `Task.Backgroundable` 和 read action，但 `treeModel.invalidateAsync()` 与扫描逻辑混在同一 read action 中，需要收敛到统一后台 helper。
- Upload：`AbstractUploadAction` 会在 action 中同步构建 `DocView`；`DocViewUploadService` 的后台任务中再次 `executeOnPooledThread`，导致 outer task 可能提前结束，取消状态不易传递。
- Export：`ExportUtils.batchExportMarkdown` 和 `WindowExportAction` 会在循环内同步 Markdown 渲染；`WindowExportAction` 也存在后台任务中再次 `executeOnPooledThread` 的问题。
- Action update：主要 action 已声明 `ActionUpdateThread.BGT`，但 `AbstractAction.update` / `EditorAction.update` 仍会读取 PSI 判断类和方法；当前未发现 Markdown 渲染、网络调用或导出写文件。

## 迁移目标

- 用 `DocViewBackgroundTasks` 统一后台 task、read action、取消和 EDT 回投。
- Preview 和参数编辑器构造阶段先创建 UI，再后台构建 `DocView`/参数树。
- Upload/export 的 scan、DTO 构建、Markdown 渲染和 payload preparation 保持在后台任务中，避免后台任务里再开启不受进度控制的 pooled thread。

## 验证记录

- `./gradlew compileJava`：通过。
- `./gradlew test`：通过。当前测试集没有 IntelliJ PSI fixture 用于构造递归 DTO 样例，因此 task 4.4 暂未勾选。
- `./gradlew buildPlugin`：通过，生成 `build/distributions/Doc View-1.3.12.zip`。
- `./gradlew verifyPlugin`：通过；IDEA 2024.2 和 2024.3 verifier 结果均为 `Compatible`。Verifier 输出包含既有 deprecated/internal API 使用警告，并有一次 documented API changes 页面 SSL 获取失败日志，但不影响本次兼容性结论。
- `./gradlew runIde`：未运行；需要人工 IDE smoke test 验证 startup、preview、refresh、cache clear、export/upload、递归 DTO 和 cancellation/project-close 行为。
- `./gradlew publishPlugin`：未运行；虽然本地 Gradle token 已配置，但本轮未收到“立即发布”的明确确认，且发布前仍应完成手动 IDE 验证。
