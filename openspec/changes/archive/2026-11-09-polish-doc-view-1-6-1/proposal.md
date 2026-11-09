# Proposal

## Why

1.6.0 发布后，用户发现 Javadoc 长注释淹没目录标题、模板预览割裂编辑流程，且代码高亮和部分界面样式影响阅读。以 1.6.0 已发布提交为基线进行 1.6.1 小迭代，同时补齐预览 PSI 读访问边界回归。

## What Changes

- 类与方法普通 Javadoc 标题默认只取首个非空物理行，保留英文空格；可配置标题字符上限，方法标题可关闭“仅首行”。接口描述可选择后续普通正文，排除 block tags，真实描述不截断。
- 模板在同一个内容面板切换编辑/渲染；Markdown、Velocity 变量及 JSON fenced code 使用主题适配高亮。
- 弱化预览目录边框，普通接口提示使用灰色小字，GitHub Star 与清理图标采用平台风格。
- 预览选择及全部导出使用后台捕获的 PSI-free 类名，增加回归。
- 精简 README 首屏并保留详细指南入口；更新版本、更新说明、欢迎页摘要和发布记录。
- 运行自动与原生验收，只归档本 change；按已授权流程发布 1.6.1，任何网络或验收阻塞如实记录。

## Capabilities

### New Capabilities

- `template-editing-preview`: 模板源码高亮、同面板预览与草稿生命周期。
- `preview-presentation`: 预览高亮、目录样式和 PSI-free 交互。

### Modified Capabilities

- `java-documentation-metadata`: 标题/正文拆分、字符上限及生成规则设置生命周期。

## Impact

影响 shared Javadoc/DocView 工具、Settings/GenerationRulesForm、模板与预览 UI、消息和图标资源、相关缓存指纹及测试、README/用户指南/版本发布文案。保留 Java 25、IU 263.7031.23、已有依赖及 extension；不引入生产语言或依赖，不扩展 MCP、上传或项目文档库范围。注解与显式 DocView tag 优先级、字段/参数解析、导出格式及已有 1.6.0 功能保持稳定，标题/普通 Javadoc 描述的有意变化在 contract 中明确。
