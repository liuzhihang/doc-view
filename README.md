# Doc View

[![JetBrains Plugins](https://img.shields.io/jetbrains/plugin/v/15305-doc-view.svg)](https://plugins.jetbrains.com/plugin/15305-doc-view)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/15305-doc-view.svg)](https://plugins.jetbrains.com/plugin/15305-doc-view)
[![License](https://img.shields.io/badge/license-MIT-red.svg)](LICENSE)

从 Java 源码生成 Markdown API 文档，在 IntelliJ IDEA 中查看、复制和导出，也可按需上传到 YApi、ShowDoc 或语雀。本地生成无需启动应用，也无需配置上传账号。

[快速开始](docs/user-guide/index.md) · [模板教程](docs/user-guide/templates.md) · [平台集成](docs/user-guide/integrations.md) · [更新记录](CHANGELOG.md)

> 本页描述当前仓库分支中的功能与文档，开发分支可能包含尚未发布的更改。可安装版本和正式发布状态以 [Marketplace](https://plugins.jetbrains.com/plugin/15305-doc-view/versions) 与 [GitHub Releases](https://github.com/liuzhihang/doc-view/releases) 为准。使用已发布版本时，请从 [GitHub Tags](https://github.com/liuzhihang/doc-view/tags) 选择与插件版本一致的 tag，再查看其中的 README、用户指南和更新记录。

## 可以做什么

- **生成接口文档**：识别 Spring Controller、Feign 风格接口、Dubbo 和按设置启用的普通 Java 接口，读取 Javadoc、Validation、Swagger / OpenAPI 注解。
- **浏览与定位源码**：按模块、类和方法浏览接口；代码名配合灰色业务说明，支持搜索、源码跳转和 Markdown 编辑器打开。
- **调整模板与输出**：分别编辑 Spring、Dubbo 模板，搜索并插入变量，用固定样例预览 Markdown，再应用到当前项目。
- **管理文档文件**：复制、单项或批量导出 Markdown；项目导出生成 `docs/doc-view/` 索引与接口页，附相对源码链接并检查手改冲突。
- **编辑字段说明**：通过 Doc Editor 编辑实体字段说明和必填信息，确认后写回支持的源码注释或注解；也可复制实体 JSON。
- **连接文档平台**：按项目配置 YApi、ShowDoc 或语雀，主动选择目标后上传。凭据使用 IDEA PasswordSafe 保存。

主要界面支持英语和简体中文，跟随 IDEA 界面语言；源码注释、模板和生成文档不会自动翻译。

## 安装与首次使用

当前构建以 **IntelliJ IDEA Ultimate 2026.1（build 261）** 为基线，依赖 Java、Markdown 与 JCEF 支持。插件未设置最高 build，不表示后续所有 IDEA 版本均已验证；具体范围见[兼容性说明](docs/intellij-compatibility.md)。

1. 在 **Settings → Plugins → Marketplace** 搜索 `Doc View` 安装兼容版本；也可从 [Releases](https://github.com/liuzhihang/doc-view/releases) 下载 ZIP，通过 **Install Plugin from Disk…** 安装。macOS 可从应用菜单进入 Settings。
2. 打开 Java 项目，等待依赖解析和索引完成。在支持的接口方法内，或类声明处右键选择 **Doc View**。
3. 查看生成结果，使用 **Copy** 取得 Markdown，或使用 **Export** 保存文件。只在需要上传时配置平台账号。

可直接使用[快速开始中的示例](docs/user-guide/index.md#三步生成第一份文档)。普通实体类使用 **Doc Editor**，不会作为 HTTP / RPC 接口生成文档。

## 设置与模板

设置入口为 **Settings → Tools → Doc View**，工具窗口齿轮也可打开。配置按当前项目保存。

| 页面 | 主要用途 |
| --- | --- |
| Doc View | 行标记、普通接口识别 |
| 生成规则 / Generation Rules | 标题、名称、描述、必填与字段名来源 |
| 模板与输出 / Template and Output | 模板编辑、变量查阅、示例预览和导出选项 |
| 平台集成 / Platform Integrations | YApi、ShowDoc、语雀的地址与凭据 |

模板编辑、预览结果和输出选项分别展示，变量目录按需展开。页内 `?` 提供规则说明或指南链接，完整操作见[模板教程](docs/user-guide/templates.md)。

- **Apply / OK** 保存设置；**Revert changes / Reset** 恢复最近一次保存值；**Cancel** 丢弃尚未应用的草稿。
- **恢复默认模板** 会替换 Spring、Dubbo 两份草稿，Apply 前不更改保存值。
- 自定义模板影响 Markdown 预览、复制、导出及 ShowDoc / 语雀正文。YApi 使用独立的结构化上传逻辑，模板不会改变其上传布局。

## 欢迎页与使用文档

**Help → Doc View Welcome**（也可用 Find Action 搜索）在编辑区打开随插件提供的 HTML 欢迎页和更新摘要。满足目标 IDE 的 JCEF 要求时，页面离线可用；首次加载展示一次，后续版本更新通过可关闭的通知提醒。

完整手册在 GitHub 仓库维护，设置问号气泡里的指南链接由默认浏览器打开：

- [用户指南](docs/user-guide/index.md)：安装、源码入口、目录导航和项目文档导出。
- [设置说明](docs/user-guide/settings.md)：作用范围、来源优先级和保存行为。
- [模板教程](docs/user-guide/templates.md)：变量、内置模板与修改示例。
- [平台集成](docs/user-guide/integrations.md)：三平台配置、凭据与上传结果。
- [故障排查](docs/user-guide/troubleshooting.md)：菜单、模板、生成和导出问题。

指南以仓库 `docs/user-guide/` 为维护源，完整手册不打包为离线副本。欢迎页和设置中的指南入口由默认浏览器打开 GitHub；查看已发布版本的使用说明时，请使用对应 Git tag 下的文档。语雀的[旧版文档](https://www.yuque.com/liuzhihangs/doc-view)仅作历史参考。

## 升级前注意

- **平台凭据需重新填写**：新版不读取或迁移旧 XML 中的密钥；PasswordSafe 按项目和平台隔离，输入框留空表示保留。旧 XML 和备份不会自动清理，详见[凭据说明](docs/user-guide/integrations.md#token-与本地保存)。
- **OpenAPI 3 必填使用 `Schema.requiredMode`**：不再读取已弃用的 `Schema.required`。通过 Doc Editor 写回时，项目依赖必须提供 `requiredMode`；缺少该成员时会拒绝本次字段写回，见[必填说明](docs/user-guide/settings.md#必填)。
- **手写内容放在独立文档**：项目导出会检查生成页的所有权与内容校验标记。手改冲突会中止导出，不自动合并；源码变化后需重新导出，不会双向同步。
- **需要旧行为时使用旧插件版本**：新版不提供旧模板写法或旧 IDE 的兼容模式。升级前保留需要的自定义模板与配置，降级后的配置兼容性不作保证。

项目导出是普通 Markdown 文件，可由 IDE 或 AI 按文件读取；当前没有发布 Doc View 专用 MCP 工具、自定义 IDEA 文档索引或 Air 集成。后续方向见[路线图](docs/roadmap.md)。

## 更新与开发

[CHANGELOG](CHANGELOG.md) 按 `Unreleased` 和已发布版本记录改动；[GitHub Releases](https://github.com/liuzhihang/doc-view/releases) 提供正式发布记录与安装包。当前开发工作的验收范围见[验证记录](docs/validation-2026-10-20.md)，发布前按[检查清单](docs/release-checklist.md)完成收尾。

生产代码使用 Java 21，构建使用仓库 Gradle Wrapper。开发、验证与协作约定见 [AGENTS.md](AGENTS.md) 和[架构说明](docs/architecture.md)。

## 历史演示

以下为旧版本演示，设置布局、菜单与图标以当前版本和用户指南为准。

![历史接口文档生成演示](https://cdn.jsdelivr.net/gh/liuzhihang/oss/pic/article/122-zbZ9ps.gif)
![历史字段编辑演示](https://cdn.jsdelivr.net/gh/liuzhihang/oss/pic/article/0YyY3m-CQdmD6.gif)

[更多历史截图](https://github.com/liuzhihang/doc-view/discussions/17)

安全
----

请按照 [SECURITY.md](SECURITY.md) 私密报告疑似安全漏洞。不要在公开 Issue 中提交凭据、私有项目数据或未脱敏日志。

关于我
----

欢迎关注公众号：『 程序员小航 』

![wechat-vxgNsq](https://cdn.jsdelivr.net/gh/liuzhihang/oss/pic/article/wechat-vxgNsq.png)


小伙伴们
----

感谢以下小伙伴的参与:

[lvgo](https://github.com/lvgocc)
[知一](https://github.com/zh-d-d)
[大斌](https://github.com/dabinaa)
[ayang0422](https://github.com/ayang0422)



其他插件
----

&emsp;Toolkit: [https://github.com/liuzhihang/toolkit](https://github.com/liuzhihang/toolkit)


&emsp;copy-as-json: [https://github.com/liuzhihang/copy-as-json](https://github.com/liuzhihang/copy-as-json)

本工具使用 JetBrains IDEA 进行开发
----
<a href="https://www.jetbrains.com/?from=Toolkit"><img src="https://cdn.jsdelivr.net/gh/liuzhihang/oss/pic/article/jetbrains-logo-MrNwcp.png" width="20%" height="20%"></a><a href="https://www.jetbrains.com/?from=Toolkit"><img src="https://cdn.jsdelivr.net/gh/liuzhihang/oss/pic/article/idea-logo-XpnqgG.png" width="20%" height="20%"> </a>
