# 2026-11-20 MCP / AI 设置原生补验

使用 Doc View `1.7.2-SNAPSHOT` ZIP `bd0d5a404d061050d8ed7d429aa904550feea1b0956b5d5ced346f90f35999ed`、安装 JAR `f55e13cf168985f80710efa5935ee381bed66c13417cb4485b80fb08716075b3`，宿主 IntelliJ IDEA IU `263.7307.200`、Apple Silicon 和 Codex `0.169.0-alpha.11.1`。

- 中文和英文均通过 Tab 到达 MCP 快捷入口，再用 Space 进入 IDEA 内置 MCP Server 配置。只查看配置页，保持服务原状态。生成入口的可见文字分别为“生成”和 `Generate`，保留平台新建文件图标；无障碍读回“生成项目文档”和 `Generate Project Docs`。完整 tooltip 在 `CodexSkillSettingsTest` 中断言。
- 英文原生设置渲染为 1800×1400 像素（900×700 points）。`Current project`、`Install Skill`、路径与快捷入口可见，右侧设置面板无横向滚动。长路径字段从开头显示，按 ⌘A 选区可读回完整路径。更小窗口拖动未改变尺寸，早期宽窄切换另见[历史 UI 复验](verification-2026-11-18-ui.md)。
- 在单独的虚构项目 profile 停用 `com.intellij.mcpServer` 并重启，Doc View 设置仍打开。`Open MCP Server Settings` 保持可见且禁用，实际页面和无障碍帮助均显示 `MCP Server settings are unavailable. Check that the MCP Server plugin is enabled.`。IDEA 另提示其 Debugger/Coverage MCP Toolset 依赖已停用组件，未操作其他插件。
- 英文原生显示官方 `Available 1.7.1`，未安装时只露出 `Install Skill` 主操作。离线后已安装文件和最近查询记录保持的证据见[两范围离线复验](verification-2026-11-20.md)。三个操作进度仍按 7.6 验收，未以组件测试替代缺少的原生观察。
