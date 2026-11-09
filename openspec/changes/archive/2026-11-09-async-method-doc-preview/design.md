# Design

## Context

见 [proposal.md](proposal.md)。`MethodNode` 的双击/Enter 入口当前直接调用 `CustomFileUtils.openMd`；该方法在调用线程同步获取服务、构建单方法 `DocView`、渲染 Markdown，再进入 Scratch 写入和编辑器打开。树交互运行在 IDEA UI 线程，因此当前 read action 并未把解析工作移出 EDT。

`DocViewBackgroundTasks.runReadTask` 已提供可取消的 Backgroundable/read-action 执行，并在项目有效且 indicator 未取消时将成功回调投递到 UI 线程。`CustomFileUtils.open` 已通过 `WriteCommandAction` 写入 Scratch，并用 `FileEditorManager` 打开文件。

## Goals / Non-Goals

**Goals:**

- 在可取消后台 read action 内完成 PSI 服务选择、单方法构建、Markdown 渲染和 Scratch 路径计算。
- 只将路径和 Markdown 字符串交给 UI 回调，不让 `DocView`、PSI 或其他 PSI 派生可变对象离开 read action。
- 取消或项目关闭发生在成功回调交付前时，丢弃结果；成功回调只负责在 EDT 写入并打开 Scratch。
- 用真实 Java PSI 测试验证方法树入口不会在 EDT 调用生成服务，成功时仍打开完整且路径不变的文档。

**Non-Goals:**

- 不改变 Markdown 模板、方法文档内容、Scratch 命名或 `.http` 打开流程。
- 不重写或扩展 `DocViewBackgroundTasks`、生成器、缓存和 Tool Window 刷新状态机。
- 不承诺取消发生在 Scratch 写命令已开始之后仍能中止该同步写入。

## Decisions

### 复用现有后台 read task

`openMd` 使用 `DocViewBackgroundTasks.runReadTask`，不自行创建 pooled thread 或新任务抽象。平台任务在后台持有 read action，使用统一 `ProgressIndicator`；生成服务已有协作取消检查。无效 class/method PSI 在读取前返回空结果。取消异常由任务入口静默处理，生成错误通过现有 Doc View 通知反馈。

替代方案是继续在 EDT 同步运行或只把 Markdown 写入移到后台。两者都不能解决 PSI/service/template 解析仍占用事件线程的问题，因此不采用。

### Read action 边界只返回文本快照

后台任务在 read action 中构造方法 `DocView`，使用同一模板渲染 Markdown，并计算现有 `node.docPath(project)`。任务返回只包含 `String path` 与 `String markdown` 的私有快照；PSI 和 `DocView` 不跨线程边界。生成未完成或取消时不产生可供 UI 发布的快照。

### Scratch 发布只在成功 UI 回调中发生

成功回调沿用 `DocViewBackgroundTasks` 的 EDT 投递及项目 disposed/indicator 检查，再调用已有 `open` 写入 Scratch 并打开编辑器。写入仍使用 `WriteCommandAction`。如果生成失败，通知错误且不进入 Scratch 打开；若写入失败，现有 `open` 按路径通知，编辑器不会打开该请求的文件。成功回调执行前发生的取消或项目关闭不会创建 Scratch。

### 失败反馈不暴露内部异常细节

后台生成失败只通知用户可理解的异常消息，不记录堆栈或 PSI 内容；`ProcessCanceledException` 不作为错误展示。写入失败继续使用现有带目标路径的 Doc View 错误通知，不新增依赖或平台集成。

## Risks / Trade-offs

- 后台 read action 仍可能等待平台写操作，但等待发生在任务线程而非 EDT；任务可取消。
- PSI 在任务开始前可能失效；读取前验证 `isValid`，且 read action 防止生成期间的 PSI 修改竞态。
- 项目关闭与取消通过任务回调边界丢弃结果；如果同步 Scratch 写命令已经在 EDT 开始，无法在其内部响应用户取消，本 change 不扩大文件写入架构。
- 自动 fixture 可验证线程、内容与任务取消边界，但不替代 IU 2026.1 原生双击/Enter、取消和项目关闭验收。

## Migration Plan

无需持久化迁移或用户设置变化。若后台路径出现回归，可回退 `openMd` 的任务调度改动；保留输出格式和原有 Scratch 写入辅助方法。
