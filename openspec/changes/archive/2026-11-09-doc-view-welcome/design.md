# Design

## Context

2026-10-19 用户澄清：编辑器页面负责欢迎和更新介绍，完整使用手册留在 GitHub。旧方案已实现七章 Swing 离线阅读器，但不再是目标。其 Round57 排版、链接和生命周期验收保留在 [verification](verification.md)；不继续为旧手册补功能。完整键盘验收由用户明确暂时跳过。

现有项目级虚拟文件/服务、FileEditorProvider、首次与升级决策已用于新实现。欢迎/更新正文由随包 HTML/CSS 资源提供，JCEF browser 由专用 FileEditor 持有；7 个设置帮助入口经默认浏览器打开 GitHub 文档。七章 Swing reader 已移除，不保留备用渲染路径。API Markdown 预览和生成不属于此替换范围。

## Goals / Non-Goals

- 在编辑区提供精简、美观、与 IDE 配色协调的欢迎页和本版本更新页。
- 使用与安装包一致的本地 UI 资源；完整文档由 GitHub 仓库维护。
- 不内嵌在线文档站，不自动拉取动态首页，不做远端缓存、同步、登录或遥测。
- 不为旧 IDE/JCEF、旧离线目录或旧页路由增加兼容实现；不修改模板及 API 输出。

## Decisions

### 1. 本地 HTML，而非远端首页

| 方案 | 结果与取舍 | 决策 |
| --- | --- | --- |
| 随插件打包 HTML/CSS | 首次显示不依赖网络；版本、链接与 UI 可随包测试；内容更新需要发插件版本 | 采用 |
| 远端 HTML 首页 | 可独立更新宣传内容；受网络、站点可用性和远端内容变化影响，需额外约束远端页面与 IDE 动作的权限边界 | 当前没有独立更新宣传内容的需求，不采用 |
| 本地外壳自动加载远端正文 | 引入两套状态、缓存和失败路径 | 不采用 |

本地页面仅包含产品介绍和本版本重点变化。使用指南、故障排查、详细模板说明、平台配置说明和完整历史通过明确的 GitHub 外链访问；不继续打包这些正文。断网时仍能看欢迎/更新页及打开设置，GitHub 文档不可达时由浏览器呈现网络结果。

### 2. 页面信息结构与低密度布局

欢迎页：

1. 顶部品牌图标、Doc View、已安装版本，一句说明插件用途。
2. 一项主操作“打开接口目录”，辅操作“模板设置”；只导航，不扫描或生成。
3. 三步入门摘要：找到接口 → 预览文档 → 复制/导出。每步一句，详细操作链接到 GitHub。
4. 本版本 3–5 项重点变化，每项标题加一两句影响说明；“查看更新”进入本地更新页。
5. 页尾放使用文档、源码、问题反馈和完整历史入口；更新提醒开关次要展示。

更新页沿用同一视觉外壳，突出版本、简短发布说明和分组亮点，可返回欢迎页；不显示整份多年 CHANGELOG。发布亮点从对应版本记录整理，展示版本必须与 PluginDescriptor 一致；正式发布构建不显示开发预览标识，带未发布变更的开发构建则明确显示“开发版 / 未发布变更”，不能冒充已发布版本内容。

视觉初始约束：根据宽屏反馈将内容容器上限由 1000 放宽到 1800 CSS px，宽度随编辑区伸缩；水平内边距使用 clamp(24px, 3vw, 64px)，720px/600px 断点保留原 24px 留白。正文约 16px 起并随 IDE 缩放核对；页面左右留白至少 24px，区块间距约 32–48px，正常行高。这些是设计起点，以原生效果为准。并列区域不超过两列，窄窗口转单列；短文案、轻边界、单一主操作。避免大面积宣传图、广告、密集仪表板、无意义卡片和长教程。跟随 IDE 主题而非系统主题，Compact 不自动缩小正文。焦点态、链接和按钮必须清楚。

### 3. Java FileEditor + JCEF + 静态资源

复用专用 FileEditorProvider 和项目内标签复用。JCEF browser 归当前 editor 所有；资源从插件包读取，不依赖 CDN/远程字体。优先采用内嵌 CSS 的 HTML 与内联 SVG，避免引入不必要的资源服务；如实际需要独立文件，只映射固定资源清单。Java 注入经过转义的版本与主题值；主题变化时更新当前欢迎/更新视图，不重复创建 editor。

不新增前端构建体系或 JavaScript 业务脚本。页面按钮使用固定内部动作 URL，由 Java 导航 handler 验证、取消浏览器导航并派发到 EDT；只允许首页、更新、接口目录、模板设置、平台设置及更新提醒等既定动作，绑定 editor 所属 project。提醒状态修改不 Apply 项目配置。普通外链仅响应用户点击后经 BrowserUtil 打开；页面不能成为任意本地文件或远端内容浏览器。

JCEF 是单一渲染路径。按平台要求检查运行环境支持；不支持时给出简短说明和 GitHub 文档入口，不保留 Swing/Markdown reader 或备用浏览器。默认 client 随 browser 释放；显式 listener/handler 随 editor 解除，关闭项目和迟到回调不再写回。无需为小型欢迎页复用带源码/Markdown 状态的 API 预览组件。

官方依据：[Embedded Browser (JCEF)](https://plugins.jetbrains.com/docs/intellij/embedded-browser-jcef.html) 明确支持 HTML、自定义网页组件、随包资源与 Disposable 生命周期。其 2026-10-09 文档说明 2026.2 起 JCEF 独立为 bundled Web Browser 插件，`com.intellij.modules.jcef` alias 从 2025.3.1 可用。因此在当前 2026.1 基线上显式增加该 bundled 依赖，并使用正式 261 Verifier 与 262 原生环境验证；不依赖内部 What's New 实现。Translation 的 `WebView` 源码已核对使用 `JCEFHtmlPanel`，可作为嵌入式 HTML 的实现参考；产品信息结构仍依据本 change 的用户任务设计，而非从截图推断。

### 4. GitHub 文档与设置帮助

`docs/user-guide/` 保留为唯一完整教程源，短段落、步骤、示例和目录继续完善。设置页“使用指南”直接打开对应 GitHub 文档，不调用首页渲染器读取文章。外链可有稳定章节锚点，须检查实际分支路径；本地新增文档尚未推送，远端 404 是发布前待办，不能为消除此状态擅自推送。

欢迎文案是 UI 内容，不复制完整指南。CHANGELOG 保留完整历史，本地更新页只使用当前发布亮点，测试版本匹配和未发布标记。语雀不作为首页或另一套手工正文。

### 5. 首次、升级与项目边界

沿用应用级已提示版本：首次只在一个有效项目自动打开一次，升级只通知、点击进入对应更新页，同版和关闭提醒后不重复通知。手动入口随时可用，同一项目复用一个 editor，不接管普通 Java/Markdown 文件。设置动作只导航，草稿不得因打开首页/外链而 Apply、Reset 或丢失。

## Validation

- 新 requirement 先 RED：页面/版本和资源清单、GitHub 路由与内部动作、未知协议拒绝、项目绑定和释放。
- 新包只包含欢迎/更新静态资源，完整五篇指南与旧离线导航不再打包；没有远端网页、脚本或 CDN 依赖。
- 真实 JCEF 验收：无自动外网请求、欢迎/更新可读、每个按钮有效、深浅 × Compact 四组、约 600px 与宽窗、主题切换、多项目、关闭重开和销毁。浏览器原型截图不替代 IDEA 内实测。
- 静态链接检查不等于 GitHub 线上可读；线上正文在获准推送后另验。
- 执行现有 test / verifyPluginProjectConfiguration / buildPlugin / verifyPlugin，确认显式 JCEF dependency 和实际测试数量。
- 旧手册 215 tests/主题截图不算新页面验收。完整键盘专项保持用户本次“暂时跳过”，不得记为通过；新页面基本焦点可见性仍按页面验收核对。

## Migration / current state

Round58 已 apply：随包 HTML/CSS + JCEF 欢迎/更新页替换七章离线手册，7 个设置帮助入口打开 GitHub 文档。自动验证实际执行 207 项测试（43 suites，0 failures/errors/skipped），build/config 与 IU261 Verifier 通过。262 原生已覆盖两页 Light/Dark × Compact 四组、约 600 CSS px 分屏换行、多项目绑定、首次自动打开、关闭重开、6 类内部动作、4 类 GitHub URL、模板草稿保留、升级提醒和提醒关闭后的偏好持久化。JDI 后台暂停期间关闭页面，`editorDisposed=true`、`generation=2→3`、`browserWrapperDisposed=1`、`jcefClientWrapperDisposed=1`、`cefClientDisposed=true`；恢复后无迟到回调重开页面，手动重开正常。IDE 未提供原始 CEF closing/closed 字段，没有据此推断原始进程或堆无泄漏。升级 1.3.13→1.3.14 通知及提醒关闭后模拟升级的行为已核对。测试设置已恢复，doc-demo 原有 35 个文件内容未变，普通 IDEA PID 66890 已恢复打开 `doc-view – ai-context-assessment.md`。完整键盘检查按用户要求跳过；GitHub 新指南正文未推送，公开可达性未验。关于无自动远端正文加载的核对依据是随包资源、CSP、请求拦截和本地实测，没有抓包证据。Round58 包 ZIP SHA-256 `de28d0110feed290cd7be220fbe5bbc34b76e01dc9a4bddc4f7248d4ffd8e6a0`、JAR SHA-256 `e4767060319cc2eb8431544b7286c0565c3bed936861260d350c53a88e01c097`。没有提交、发布、sync 或 archive。
