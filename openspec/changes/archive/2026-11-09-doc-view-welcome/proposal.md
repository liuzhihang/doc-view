# Proposal

## Why

用户于 2026-10-19 明确：IDE 编辑区需要类似 Translation / IDE 更新页的欢迎与版本介绍页面，使用指南、模板教程和故障排查只放 GitHub。此前把完整手册打包为七章离线阅读器，偏离了这一目标。

本修订替换先前离线手册方案。Round58 已实施随包 HTML/CSS + JCEF 欢迎/更新页及 7 个 GitHub 设置帮助入口。207 项实际测试（43 suites，0 failures/errors/skipped）、build/config、IU261 Verifier 通过；262 原生矩阵及多项目、链接、草稿、关闭释放、升级提醒和普通 IDEA 恢复均已完成。JDI 证据确认 editor 与 JCEF wrapper 已释放、迟到回调未重开页面；平台未提供原始 CEF closing/closed 字段，未检查原始进程或堆泄漏。完整键盘检查按用户要求跳过；GitHub 新指南正文尚未推送，公开可达性未验。旧手册的测试和原生验收仅作为历史事实，不计为新版页面验收。

用户随后反馈宽屏内容过窄：原 1000px 居中容器在 2560px 编辑区仅占约 39%。本次追加放宽容器、保留正文字号与窄窗断点的局部样式修正，不改变页面内容或动作。

## What Changes

- 保留 Help / Find Action 的 Doc View 编辑器标签、项目内复用及首次/升级提醒策略。
- 改为随插件打包的专用 HTML/CSS 欢迎页和本版本更新说明；以品牌、简短介绍、常用入口和少量更新亮点组成低密度页面。
- 使用目标 IDE 的 JCEF 呈现 HTML，Java 负责生命周期、主题参数与受控动作；不引入前端框架、Node 运行时、JavaScript 业务实现或旧渲染器 fallback。
- **BREAKING**：取消七章离线手册、章节下拉和完整指南打包；设置页帮助直接打开对应 GitHub 文档。保留仓库已有指南正文与内部链接，不删除用户文档。
- 首页不自动访问远端，不嵌入 GitHub/语雀网页；GitHub 文档、完整历史和反馈由用户点击后在外部浏览器打开。
- 使用当前插件版本信息；更新页只呈现对应版本的重点变化，开发构建的未发布内容明确标记，不将整个 CHANGELOG 倒进首页。

## Capabilities

### New Capabilities

- `in-ide-help`：约束欢迎/更新页、GitHub 帮助入口、版本提示、主题与浏览器资源释放。名称沿用活动 change，内容以本次修订为准。

### Modified Capabilities

无。既有 main specs 未覆盖此页面。API 文档生成、模板、导出及平台 payload 的 contract 不变。

## Impact

- 调整 `ui/welcome` 的 FileEditor 与资源加载；复用项目绑定和版本决策，移除离线手册渲染链路。
- `plugin.xml` 与 Gradle 显式声明 bundled JCEF 依赖，目标仍为 IDEA 2026.1+，Java-only；新增 HTML/CSS/SVG 是静态 UI 资源，不新增生产编程语言或第三方依赖。
- 调整设置帮助入口、processResources、相关测试、README/CHANGELOG 与渠道说明。
- 验证本地无网络加载、版本准确性、深浅主题与 Compact、窄窗口、链接和设置草稿、JCEF 释放及多项目；此前手册通过项不能直接勾选新页面任务。
- Round58 已 apply；自动及原生验证范围、仍未覆盖项与证据边界见上文和验证记录。完整键盘检查按用户要求暂跳，远端正文公开检查未完成。保持不提交、不发布、不自动 sync/archive。
