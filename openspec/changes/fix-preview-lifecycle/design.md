# Design

## Context

动机和范围见 [proposal.md](proposal.md)。当前 `PreviewForm` 在字段初始化时创建 Markdown `Document`，构造期间布局并创建 viewer，随后立即启动文档加载；`popup()` 之后才创建并显示 `JBPopup`（[PreviewForm.java#L79](../../../src/main/java/com/liuzhihang/doc/view/ui/PreviewForm.java#L79)、[lines 186–197](../../../src/main/java/com/liuzhihang/doc/view/ui/PreviewForm.java#L186)、[lines 220–243](../../../src/main/java/com/liuzhihang/doc/view/ui/PreviewForm.java#L220)、[lines 394–420](../../../src/main/java/com/liuzhihang/doc/view/ui/PreviewForm.java#L394)）。viewer 没有释放。IU-262 原生日志已将 `EditorView` 泄漏归到 `markdownSourcePanel()`；HTML panel 仅在 JCEF 可用时创建。

文档加载和渲染使用 `DocViewBackgroundTasks`。它会静默捕获 `ProcessCanceledException`，其他错误只调用通用错误回调（[DocViewBackgroundTasks.java#L22](../../../src/main/java/com/liuzhihang/doc/view/utils/DocViewBackgroundTasks.java#L22)、[lines 51–80](../../../src/main/java/com/liuzhihang/doc/view/utils/DocViewBackgroundTasks.java#L51)）。因此当前“Loading/Rendering”提示在这些路径上没有终态。已有渲染请求编号只保护成功的 Markdown 应用；`applyMarkdown()` 仍在 EDT 内生成 HTML 字符串（[PreviewForm.java#L654](../../../src/main/java/com/liuzhihang/doc/view/ui/PreviewForm.java#L654)、[lines 702–742](../../../src/main/java/com/liuzhihang/doc/view/ui/PreviewForm.java#L702)）。单次 Dubbo 卡在“Rendering Doc View...”的观察没有对应异常或已确认根因，本 change 不将它描述为已定位的故障。

2026-11-09 的 IU 263.7307.200 原生日志发现另一项已定位的线程错误：用户在目录列表中选择预览项时，`PreviewForm.lambda$applyDocViewList$1` 从 EDT 直接调用 `currentDocView.getPsiClass().getQualifiedName()`；`idea.log` 报 `Read access is allowed from inside read-action only`，调用栈经 `JList.fireSelectionValueChanged`。该日志来自 `build/native-validation/2026-11-08/unload-clean/log/idea.log:48085` 附近。此前预览画面成功不能证明日志无线程错误。

同一候选的 JCEF 日志还在 02:28:41 与 02:58:00 记录 `MarkdownJCEFHtmlPanel - Failed to update the Markdown preview`，浏览器侧异常为 `First body child is expected to be the root of the document!`。当前 `showPreviewStatus()` 自行拼接 `<html><body><p>…</p></body></html>` 后交给 Markdown panel；平台 Markdown JCEF 的滚动同步脚本要求 renderer 生成的文档根节点/源码位置结构。第一条日志附近没有取消或关闭事件；第二条发生在一次预览取消后，说明取消会走到该状态路径，但相同错误也能独立发生，现有证据不支持将它归因为 panel dispose 竞态。Java 调用栈在异步 JS worker 处终止，没有足够信息确认每次错误对应的具体 `setHtml()` 调用；将状态 HTML 改为复用当前公开 Markdown renderer，并在新状态、正文应用或关闭时使待处理状态结果失效，再以 JCEF 日志复验。

Export 与 Export All 当前先取消 popup，再读取实例字段（[PreviewForm.java#L538](../../../src/main/java/com/liuzhihang/doc/view/ui/PreviewForm.java#L538)、[lines 590–601](../../../src/main/java/com/liuzhihang/doc/view/ui/PreviewForm.java#L590)）。关闭处理若清空这些字段，导出会读到空值，因此动作必须先取得所需快照。

## Goals / Non-Goals

**Goals:**

- 将预览资源、异步请求和关闭状态的所有权限制在 `PreviewForm`。
- 让初始化、popup 创建或显示失败时也能释放部分创建的资源。
- 让当前请求报告成功、空结果、失败或取消，并忽略过期请求和关闭后的回调。
- 在后台生成 Markdown/HTML，在 UI 线程更新 Swing、editor 和 Markdown preview 组件。
- 让 HTML 状态页采用 Markdown panel 支持的文档结构；状态生成不阻塞 UI，迟到状态不会覆盖正文或更新状态。
- 在关闭 popup 前捕获导出数据，保持 Export/Export All 可用。

**Non-Goals:**

- 推断或专门修复那次尚未定位根因的 Dubbo 停滞。
- 重排 popup UI、增加重试控件，或改变 Markdown 输出、导出格式、上传 payload/protocol 和共享任务工具语义。为遵守凭据读取的线程要求，单项 Upload 入口复用已有的 `upload(project, docView)` 后台 wrapper；凭据及集成设置行为由[集成凭据 change](../integration-credential-storage/proposal.md)定义。
- 增加通用任务调度架构或生产依赖。

## Decisions

### 延迟创建资源并由 popup 释放

将资源性初始化（`Document`、viewer、可选 HTML panel）和文档加载从构造函数移到 `popup()` 的显示流程中。尚未调用 `popup()` 的 form 不创建这些资源或后台任务。`PreviewForm` 实现可释放所有者；创建 popup 后将其注册为 popup 的子 disposable。关闭时统一取消当前任务、使请求失效、调用 `EditorFactory.releaseEditor` 释放 viewer，并释放已创建的 HTML panel。初始化、popup 构造或 `show` 抛错时走同一幂等清理路径。构造/布局途中若 viewer 已创建，则捕获异常并先清理再抛出。目标平台的 `JBPopup` 实现公开 `Disposable`；`Editor` 本身通过 `EditorFactory.releaseEditor` 释放。

替代方案：仅关闭 HTML panel 会留下原生日志确认的 `EditorView` 泄漏；只依赖项目关闭会让每个已关闭 preview 的资源保留到项目退出。延迟初始化使从未展示的 form 不持有 editor 或后台任务。

### 在 PreviewForm 内处理任务回调

使用 `PreviewForm` 内部的小型 `Task.Backgroundable` 执行器处理文档列表加载和 Markdown 渲染。每个任务的成功、取消和异常回调都携带请求编号，并在更新 UI 或报告错误前检查编号和 popup 生命周期。保存当前请求的 `ProgressIndicator`，切换选择或关闭 popup 时尝试取消旧任务；请求编号仍作为最终保护，因为取消不能保证任务立即停止。

不修改 `DocViewBackgroundTasks`。该工具被上传和导出调用，且目前吞掉取消；在此处本地接管任务回调可以保留这些调用者的现有语义。已检查本机 IU-261 和 IU-262 平台库，`Task` 均公开 `onCancel`、`onThrowable`、`onSuccess` 和 `onFinished`，满足本地终态处理需要。

替代方案：给共享工具增加可选取消回调会扩大影响范围，需要同步证明上传、导出任务行为不变；当前 change 没有必要这样做。

### 将预览操作作为请求状态管理

为文档列表加载和每次渲染分别保留请求编号，并在关闭时标记 form 已结束。新选择开始渲染时将 `currentMarkdownText` 清空，并将源码和 HTML 面板都替换为渲染中状态；仅成功状态写入可复制、可导出的 Markdown。失败或当前请求被取消时显示相应终态；切换选择导致的旧任务取消以及迟到错误均忽略。空列表显示空状态。成功状态继续展示实际 Markdown，不增加成功提示条。

状态更新需覆盖两种预览模式：源码模式写入状态文本，HTML 模式让上一个成功文档立即消失。HTML 状态文本使用当前已集成的公开 `MarkdownUtil.generateMarkdownHtml(VirtualFile, String, Project)` 生成平台兼容的 HTML；不手写 HTML DOM，也不将状态写入 `currentMarkdownText`，因此状态不会成为复制或导出内容。状态 Markdown/HTML 在后台生成，HTML panel 和源码 editor 仍只在 EDT 更新。以 form 所有的非阻塞 read task/promise 处理短状态渲染；每个状态请求使用代际标识，新正文、新状态和 dispose 均使旧状态失效并取消仍在运行的任务。应用回调再次校验代际与 form 生命周期，防止慢状态覆盖随后成功的正文或状态。状态生成不创建额外的可见 Backgroundable 进度任务。

状态 HTML promise 被新请求或 dispose 取消时属于预期终止，不能当作 renderer 错误写 `WARN`。用公开 `CancellablePromise.isCancelled()` 状态判断取消，不按平台内部 cancellation sentinel 的具体类名做分支；只有未取消且非 `ProcessCanceledException` 的 renderer 异常继续记录 warning。

Markdown 缓存命中时也需在后台生成 HTML，避免缓存路径重新在 EDT 调用转换。`markdownCache` 继续由 UI 线程读写，除非实现验证出真实并发需求。Velocity evaluate 返回 `false` 时生成的既有 `ERROR` 文本保持原输出语义；异常状态与这段 Markdown 内容分别处理。

替代方案：仅取消旧任务无法阻止已完成或迟到的错误回调覆盖新选择，因此仍需校验请求编号和 form 状态。

### 将 Markdown 和 HTML 字符串生成移出 EDT

在后台任务中计算 Markdown 与 HTML 字符串；涉及 PSI/VFS 的读取仅在所需的 read access 内执行。后台返回不可变字符串后，再在 EDT 校验请求身份并更新 `Document`、HTML panel 和状态。不得在 read action 中执行 Swing 更新或编辑器写入。

目录选择监听器也不得从 `DocView` 解引用 PSI。列表加载在已有后台 read action 中为每个文档捕获要显示的限定类名等不可变 UI 元数据；EDT 监听器只读取该元数据、选择当前 `DocView` 并调度渲染。不得为修复该访问而把 PSI 解引用或渲染工作整体包进 EDT 的 `runReadAction`。

替代方案：只把 `DocViewData.markdownText()` 放到后台会留下 `MarkdownUtil.generateMarkdownHtml()` 的同步 EDT 工作，未覆盖当前 `applyMarkdown()` 中的另一段转换。

### 导出前捕获动作快照

Export 在 `popup.cancel()` 前捕获当前文档名和已完成的 Markdown 字符串；Export All 在关闭前捕获类名和文档列表副本。由 PreviewForm 内部窄的不可变快照方法同时提供给对应动作和生命周期测试，动作将快照传给现有 `ExportUtils`。关闭清理不应改变动作已捕获的输入。

替代方案：为了导出而保留 popup 上的可变字段会延长 UI 对象图的生命周期，也无法清晰表达关闭后的所有权；局部快照让 popup 可以立即释放资源。

## Risks / Trade-offs

- [任务取消回调可能晚于用户切换选择] → 所有成功、取消、失败回调都校验请求编号和 popup 状态，并覆盖逆序完成。
- [Markdown/HTML 转换仍可能耗时] → 两段转换都在后台执行；验证 UI 组件更新仍在 EDT。
- [popup 关闭与导出发生竞态] → 关闭前捕获所有导出参数，并验证快照在资源释放后仍可用。
- [测试环境可能没有 JCEF] → viewer 释放、其他 editor 保持可用及源码状态需无条件断言；HTML 状态替换和 HTML panel 释放在 JCEF 可用的目标 IDE 上复验。

## Migration Plan

无需迁移设置、模板或持久化数据。实现预览所有者和请求状态，增加 fixture 回归测试，再验证 popup 关闭、Spring/Dubbo 内容、关闭后的导出快照及 JCEF panel 释放。回退到之前的预览生命周期不会影响任何已保存数据。

## Open Questions

无。Dubbo 停滞的根因仍未确认，但不影响本 change 的生命周期合同和实现边界。
