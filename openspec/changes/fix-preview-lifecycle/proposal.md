# Proposal

## Why

IU-262 原生退出日志确认，预览源码 `EditorView` 由 `PreviewForm.markdownSourcePanel()` 创建后没有随 popup 释放，注册在平台根 disposable 下。另一次 Dubbo 预览曾停在“Rendering Doc View...”，但现有日志不能确认该次的具体根因。需要为预览关闭、生成失败和取消定义明确的资源与状态边界，避免资源泄漏、过期回调和永久加载提示。

## What Changes

- 让每个预览 popup 管理本次创建的 Markdown viewer、可用的 HTML panel 和生成任务；关闭、初始化失败或 popup 未能显示时释放资源，且不影响其他预览或编辑器。
- 让文档列表加载和 Markdown 渲染的成功、空结果、失败和取消都结束对应加载状态；仅当前请求可以更新预览或报告错误，窗口关闭后忽略过期回调。
- 开始渲染新选择时清除上一文档的可复制、可导出结果，避免加载期间对当前选择操作旧内容。
- 将 Markdown 到 HTML 的字符串生成移出 EDT；Swing、editor 与 HTML panel 的最终更新仍在 EDT 执行。
- 保持 PSI 识别、DocView/Markdown 输出、Velocity 变量、上传/导出格式和现有工具栏行为不变；不增加重试按钮、UI 重排、依赖或通用任务调度架构。

## Capabilities

### New Capabilities

- `document-preview-lifecycle`: 预览 popup 的资源所有权、异步结果有效性及加载终态。

### Modified Capabilities

无。

## Impact

- 主要生产边界：`PreviewForm` 的 popup、编辑器及后台生成回调生命周期；测试使用现有 IntelliJ Java fixture 和编辑器工厂。
- 可使用目标平台公开的 `JBPopup`/`Disposable`、`EditorFactory.releaseEditor` 和 Markdown preview API；不新增平台基线、插件依赖或生产语言。
- IU-262 的停滞观察仅作为问题背景，不能当作已定位的具体故障原因；实现后仍需用受控失败/取消场景和真实 Spring/Dubbo 预览分别验证。
