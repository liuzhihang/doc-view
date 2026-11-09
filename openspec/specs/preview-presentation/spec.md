# preview-presentation Specification

## Purpose

定义 Doc View 预览和相关设置的可读性以及交互稳定性，让用户能够在平台主题下阅读目录、Markdown 和代码示例，使用清晰的标准操作入口，并在选择与导出文档时避免违反 IDE 的 PSI 读访问约束。

## Requirements

### Requirement: 可阅读的预览与设置
系统 SHALL 使用轻量平台风格分隔预览目录与内容，避免重复粗边框；普通接口识别说明 SHALL 使用灰色小字，GitHub Star 入口和工具栏清理操作 SHALL 使用可辨识的平台风格图标并保留可访问名称、提示和既有行为。

#### Scenario: 主题下阅读和操作
- **WHEN** 用户在浅色或深色主题打开设置与预览
- **THEN** 目录边框不形成重复粗线，说明层级弱于主选项，GitHub Star 与清理图标可辨认
- **AND** 清理仍只清理既有缓存，不删除项目生成文档

### Requirement: Markdown 代码块高亮
系统 SHALL 在预览中高亮带语言标识的 JSON fenced code（包括既有模板的 JSON 大写标识），使字符串、数字等语法在当前主题下可辨认；复制和导出仍使用原始 Markdown，不混入高亮 HTML。

#### Scenario: 原始文档与显示
- **WHEN** 用户预览、复制或导出含 JSON fenced code 的接口
- **THEN** 渲染显示语法颜色，复制和导出保留相同 Markdown 内容

### Requirement: 预览交互读访问边界
系统 MUST 在允许的 read action 中捕获选择及导出所需 PSI 名称；预览的列表选择和导出全部 UI 回调 SHALL 使用已捕获数据，关闭预览后仍能完成已准备的导出。

#### Scenario: 选择和导出全部
- **WHEN** 用户在预览中切换列表项或点击导出全部，且 UI 回调没有 read access
- **THEN** 不访问 PSI 类名或触发 read access assertion，导出文件名和文档集合正确

### Requirement: 轻量的 GitHub Star 入口
系统 SHALL 以带 GitHub logo 的轻量链接显示 Star on GitHub，保留合适的字距、命中区、键盘访问与深浅主题可读性；不使用突出的大 CTA。点击 SHALL 仅打开现有 Doc View GitHub 仓库，不自动执行 Star 或其他账户动作。

#### Scenario: 打开项目仓库
- **WHEN** 用户点击或通过键盘激活 Star on GitHub
- **THEN** 浏览器打开既有仓库链接，设置草稿与持久化值不改变
