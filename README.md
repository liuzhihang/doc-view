# Doc View

[![JetBrains Plugins](https://img.shields.io/jetbrains/plugin/v/15305-doc-view.svg)](https://plugins.jetbrains.com/plugin/15305-doc-view)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/15305-doc-view.svg)](https://plugins.jetbrains.com/plugin/15305-doc-view)
[![License](https://img.shields.io/badge/license-MIT-red.svg)](LICENSE)

在 IntelliJ IDEA 中，从 Java 源码生成 API 文档。支持 **Spring、Feign、Dubbo**，无需启动应用，也无需配置上传账号。

[安装插件](https://plugins.jetbrains.com/plugin/15305-doc-view) · [快速开始](docs/user-guide/index.md) · [更新记录](CHANGELOG.md) · [反馈问题](https://github.com/liuzhihang/doc-view/issues)

## 三步开始

1. 在 **Settings → Plugins → Marketplace** 搜索并安装 `Doc View`，按提示重启 IDEA。
2. 打开 Java 项目，等待索引完成，在接口方法或类声明处右键选择 **Doc View**。
3. 预览结果，使用 **Copy** 复制 Markdown，或 **Export** 保存文档。

**环境要求：** IDEA IU `263.7031.23` 起、Java 25。旧 IDEA 请选择兼容的旧插件版本。可安装版本以 [Marketplace](https://plugins.jetbrains.com/plugin/15305-doc-view/versions) 和 [Releases](https://github.com/liuzhihang/doc-view/releases) 为准；完整边界见[兼容性说明](docs/intellij-compatibility.md)。

## 功能演示

以下 GIF 展示**历史版本界面**；当前菜单、设置和操作步骤以[用户指南](docs/user-guide/index.md)为准。

**接口文档生成**

![历史版本：接口文档生成演示](https://cdn.jsdelivr.net/gh/liuzhihang/oss/pic/article/122-zbZ9ps.gif)

**字段说明编辑**

![历史版本：字段说明编辑演示](https://cdn.jsdelivr.net/gh/liuzhihang/oss/pic/article/0YyY3m-CQdmD6.gif)

## 日常使用

| 想做什么 | 从这里开始 |
| --- | --- |
| 浏览接口、跳转源码、生成项目文档库 | [用户指南](docs/user-guide/index.md) |
| 控制 Javadoc 标题长度与描述来源 | [生成规则](docs/user-guide/settings.md) |
| 编辑模板、插入变量、原位预览 | [模板教程](docs/user-guide/templates.md) |
| 编辑字段说明、复制 JSON / Markdown | [Doc Editor](docs/user-guide/index.md) |
| 生成可编辑的 `.http` 请求草稿 | [HTTP 请求](docs/user-guide/http-requests.md) |
| 上传到 YApi、ShowDoc 或语雀 | [平台集成](docs/user-guide/integrations.md) |

设置位于 **Settings → Tools → Doc View**，按项目保存。主要界面跟随 IDEA 的英语或简体中文；源码和生成文档不会自动翻译。**Help → Doc View Welcome** 提供离线入门与当前版本摘要。

## 1.6.2 的小改进

- 模板预览与变量入口收进面板右上角的紧凑图标工具栏；恢复默认模板位于更多菜单中，确认后只替换草稿。
- Javadoc 标题上限紧邻类注释选项，明确类和方法共用，默认80、范围1–200保持不变。
- GitHub 入口采用 logo 与轻量的 `Star on GitHub` 链接，键盘操作保持可用。

Apply / OK 保存，Revert changes 恢复已保存值，Cancel 放弃未应用草稿。安装、更新和停用插件后需要重启。更多说明见[模板教程](docs/user-guide/templates.md)和[更新记录](CHANGELOG.md)。

<details>
<summary>升级、文档库与故障排查</summary>

- 从 1.4.0 之前升级，请重新录入平台凭据；新版使用 PasswordSafe，不读取旧 XML 密钥，见[凭据说明](docs/user-guide/integrations.md#token-与本地保存)。
- OpenAPI 3 必填使用 `Schema.requiredMode`，详见[生成规则](docs/user-guide/settings.md#必填)。
- 项目文档输出至 `docs/doc-view/`，包含 Markdown 与 JSON 快照。源码变化后需显式更新；手改冲突会中止导出，不自动合并。
- 正式 Doc View MCP provider 和客户端 Skill 仍在规划中，见[MCP 说明](docs/user-guide/mcp.md)与[路线图](docs/roadmap.md)。
- 遇到菜单、模板或导出问题，查看[故障排查](docs/user-guide/troubleshooting.md)。阅读旧版本文档请切换到对应 [Git tag](https://github.com/liuzhihang/doc-view/tags)。

</details>

<details>
<summary>开发与贡献</summary>

构建使用 Java 25、仓库 Gradle Wrapper。协作规范见 [AGENTS.md](AGENTS.md)，模块边界见[架构](docs/architecture.md)，发布前遵循[检查清单](docs/release-checklist.md)。

[1.6.2 发布记录](docs/release-1.6.2.md) · [历史演示与截图](https://github.com/liuzhihang/doc-view/discussions/17) · [旧版语雀文档](https://www.yuque.com/liuzhihangs/doc-view)

</details>

感谢 [lvgo](https://github.com/lvgocc)、[知一](https://github.com/zh-d-d)、[大斌](https://github.com/dabinaa)、[ayang0422](https://github.com/ayang0422) 等贡献者。作者：[程序员小航](https://github.com/liuzhihang)。

安全问题请按 [SECURITY.md](SECURITY.md) 私密报告，公开 Issue 请勿提交凭据或未脱敏数据。
