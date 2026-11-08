# Spec Delta

## Purpose

定义 Doc View 预览的资源所有权和异步加载、渲染结果，让关闭或切换预览时不会泄漏编辑器、暴露过期内容，或让界面永久停留在加载状态。

## ADDED Requirements

### Requirement: 预览拥有并释放其 UI 资源
每个预览 popup MUST 在关闭时释放本次创建的源码 viewer 和 HTML 预览资源；若初始化或显示失败，也 MUST 释放已创建的资源。释放一个预览 MUST 保持其他编辑器可用。

#### Scenario: 关闭已打开的 popup
- **WHEN** 已创建源码 viewer 的预览 popup 被关闭
- **THEN** viewer 和已创建的 HTML 预览资源均被释放，其他编辑器仍可使用

#### Scenario: 预览未能显示
- **WHEN** 预览初始化、popup 创建或显示失败，且此前已创建预览资源
- **THEN** 预览立即释放这些资源，不等待项目关闭

### Requirement: 加载和渲染进入可见终态
当前文档列表加载和 Markdown 渲染 MUST 各自结束于可见的成功、空结果、失败或取消状态。操作结束后 MUST 替换进度提示；当前操作失败或取消后 MUST 不再无限显示加载或渲染中。状态在源码和 HTML 预览模式下都 MUST 可见，且状态内容 MUST NOT 被当作文档 Markdown。

#### Scenario: 文档列表为空
- **WHEN** 当前文档列表加载完成且没有条目
- **THEN** 预览显示空状态，不再显示加载提示

#### Scenario: 当前渲染失败或取消
- **WHEN** popup 仍打开时，当前 Markdown 渲染失败或取消
- **THEN** 预览显示失败或取消状态，不再显示渲染中提示

#### Scenario: HTML 模式下状态替换上一次内容
- **WHEN** HTML 预览模式中开始新一轮加载或渲染，或当前操作失败/取消
- **THEN** HTML 面板显示对应状态而不是上一个文档；状态不会成为可复制或导出的文档 Markdown

### Requirement: HTML 状态符合 Markdown 预览面板结构
当 HTML 面板显示 Loading、Rendering、空结果、失败或取消状态时，Doc View MUST 使用当前集成的公开 Markdown renderer 生成面板 HTML，使其满足目标 Markdown 插件的预览结构要求。HTML 生成 MUST 在 UI 线程之外运行，面板更新 MUST 在 UI 线程运行。状态仍 MUST 只用于展示，不得写入当前文档 Markdown、复制或导出快照。

#### Scenario: HTML 状态通过平台 renderer 显示
- **WHEN** JCEF 可用且预览需要显示 Loading、Rendering、空结果、失败或取消状态
- **THEN** 面板收到由公开 Markdown renderer 生成的 HTML，UI 线程显示对应状态，且状态不成为可复制或导出的 Markdown

#### Scenario: 旧状态渲染迟到
- **WHEN** 某状态的 HTML 仍在后台生成时，新的预览正文、另一状态或 popup 关闭先发生
- **THEN** 旧状态结果不得更新面板，也不得覆盖新正文或新状态

#### Scenario: 状态渲染任务取消
- **WHEN** 新状态、正文成功或 popup 关闭取消待处理的状态 HTML promise
- **THEN** 预期取消不得记录为 HTML renderer failure warning；未取消的真实 renderer exception 仍应记录

### Requirement: 过期或已关闭的预览忽略迟到任务结果
只有最新的活动加载或渲染请求 MAY 更新预览内容、状态或报告预览错误。已被替代请求的结果/错误以及 popup 关闭后的回调 MUST 不得改变当前预览。

#### Scenario: 较早的渲染在新选择后结束
- **WHEN** 较早文档的渲染在新选择已开始渲染后报告成功、失败或取消
- **THEN** 预览保持新选择的内容或状态，且不报告较早请求的错误

#### Scenario: popup 关闭后任务回调到达
- **WHEN** 加载或渲染回调在其 popup 关闭后到达
- **THEN** 回调不修改已关闭的预览，也不报告该预览的过期任务错误

### Requirement: 生成中的内容不可复制或导出
新选择开始渲染时，上一文档的 Markdown MUST 不再能通过复制或单文档导出操作取得。进度和终态提示 MUST 不得作为生成的 Markdown。关闭 popup 的导出操作 MUST 在关闭前保留所需的数据快照。

#### Scenario: 用户切换文档并等待渲染
- **WHEN** 上一文档已生成完成，用户选择了新文档且新文档仍在渲染
- **THEN** 复制和单文档导出不能使用上一文档的 Markdown，也不能使用新文档的进度提示文本

#### Scenario: 导出操作关闭预览
- **WHEN** 用户启动一个会关闭预览 popup 的导出操作
- **THEN** 导出使用该操作关闭前捕获的文档数据，不受 popup 资源清理影响

### Requirement: Markdown 和 HTML 生成不阻塞 UI 线程
依赖 PSI 的文档生成和 Markdown 到 HTML 字符串的生成 MUST 在 UI 线程之外运行；预览 UI 组件的修改 MUST 在 UI 线程运行。

#### Scenario: 预览生成文档
- **WHEN** 预览生成文档 Markdown 及其 HTML 表示
- **THEN** 内容生成在 UI 线程之外进行，结果在 UI 线程应用到预览组件

#### Scenario: 用户选择目录中的文档
- **WHEN** 后台目录加载完成，用户在预览列表中选择一项
- **THEN** 标题继续显示该文档所属类的限定名，且列表选择回调只使用加载阶段在 read action 中捕获的不可变元数据
- **AND** 选择回调不得在 EDT 解引用 `PsiClass` 或其他 PSI 元素；Markdown/HTML 渲染仍在后台执行
