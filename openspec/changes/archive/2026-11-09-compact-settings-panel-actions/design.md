# Design

## Context

见 proposal。当前模板以 JButton 提供恢复/变量/预览；现有 CardLayout、协调器和 EditorEx 可继续复用。规则页在五分组之间独立插入上限行；GitHub 是普通 JButton。初始 checkout 干净，基于1.6.1；用户后续授权 GitHub 发布1.6.2并取消截图交付。

## Goals / Non-Goals

目标是减少模板按钮占用、清楚表达共享设置，并保持焦点和草稿行为。不得改变 PSI、DTO、生成 Markdown、存储字段、上传/导出或基线。不添加 Ask AI、悬浮遮挡编辑内容或新依赖。发布仅限既有 GitHub 仓库的小修订版本和 Release，Marketplace 明确排除。

## Decisions

- 使用 IntelliJ ActionToolbar，位于每个框架内容顶部右侧：预览 ToggleAction、变量 ToggleAction、更多 ActionGroup。选择固定内置操作栏而非覆盖内容的悬浮控件，避免遮挡文本和焦点丢失。沿用项目已有 ActionButton 键盘聚焦模式；update 只读 UI 状态，不做渲染。
- 恢复默认放在更多菜单中，原生确认对话框明确作用于 Spring/Dubbo 草稿。只有确认后调用原草稿恢复，取消不 invalidate 或切模式。保存/Reset 继续由现有 Configurable 处理。
- 上限行紧跟 Class comment，以缩进关联；明确 classes and methods。Method comment 子选项后放同一设置提示，避免两个可编辑控件引入错误独立值。保留整数输入验证和设置实例。
- GitHub 使用平台 ActionLink 加 GitHub icon；主题色由 LAF 管理，适度 icon-text gap。英语和中文页面均使用 Star on GitHub，保留既有tooltip，目标仍是 bundle 中的公开仓库。
- 测试按新原生 action 入口操作，保留旧变量/预览生命周期覆盖；新增恢复取消/确认与布局关联测试。完整门禁和两 build Verifier 实际执行，原生交互明确绑定本轮最终包；截图交付已由用户取消。

## Risks / Trade-offs

- [ActionToolbar 键盘默认聚焦受平台设置影响] → 沿用现有公开 ActionButtonComponent 聚焦模式，并原生 Tab/Space 验证。
- [菜单恢复可能误改持久化] → 原生确认与测试 dialog handler，取消完全不变，确认只改草稿。
- [图标 tooltip/状态未及时刷新] → 状态切换后更新工具栏，重复点击与框架切换回归。
- [变量目录重挂载造成焦点丢失] → 在重挂载前捕获触发按钮，重挂载后恢复仍可见的按钮焦点，原生 Tab/Space 复验。
- [工具栏尚未挂载时主动刷新] → 初次渲染由平台 addNotify 负责；后续只异步更新可见工具栏。无窗口布局测试显式初始化动作，不通过生产代码补偿测试生命周期。
- [发布渠道范围] → 已核实远端 master/develop 无 .github/workflows，GitHub API 返回0个工作流；创建 GitHub Release 不调用 publishPlugin，不上传 Marketplace。
- [参考图/截图] → 原图本机物化失败，依据父会话实际审阅反馈实施；截图保存路径受阻后用户取消交付，不再等待截图授权。
