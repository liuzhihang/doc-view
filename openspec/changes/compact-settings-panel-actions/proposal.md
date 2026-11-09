# Proposal

## Why

1.6.1 模板面板的多个大按钮占用阅读空间，标题上限脱离关联注释选项，Star 文案也缺少目的说明。本次只细化设置 UI，让常用操作紧凑、就近且可通过键盘完成。

## What Changes

- 模板右上角内置图标操作栏提供预览/编辑、变量显隐与更多菜单；恢复默认置于更多菜单，确认后仅更新草稿，取消不变。不添加 Ask AI。
- 保留同面板预览、变量插入、框架与光标、Apply/Reset/关闭语义，补齐图标 tooltip/可访问名称与键盘焦点。
- 类与方法共用的标题上限紧跟类注释字段，并明确共用关系，方法注释旁提供简洁关联提示；仍只有一个输入和持久化值，默认80、范围1–200。
- GitHub logo 入口改为简洁的 Star on GitHub，沿用平台尺寸，不做大 CTA。
- 更新 Unreleased 与相关指南，保持1.6.1版本，不发布、不推送、不合并、不部署。提供深浅主题真实本机截图。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `template-editing-preview`：紧凑工具栏、确认恢复及键盘交互。
- `java-documentation-metadata`：共享标题上限的就近布局与关联说明，生成语义不变。
- `preview-presentation`：GitHub Star 入口文案和紧凑原生样式。

## Impact

仅 TemplateSettingForm、GenerationRulesForm、SettingsForm、消息资源、相关测试及文档/OpenSpec。不改 PSI/DTO/模板输出、持久化字段、生产语言、依赖或平台基线。以新交互 RED/GREEN、既有设置/生命周期回归、完整 test/build/config/Verifier 与隔离 IDEA 原生验收验证。
