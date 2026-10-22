# Proposal

## Why

英文 IDEA 中的 Doc View 正文仍主要显示中文，菜单、设置与帮助页面语言混杂；生成规则帮助还按中文标点切段。用户已明确要求本轮完成插件主要界面的中英化，并继续原生验收。

## What Changes

- 使用平台当前 UI locale 和随包消息资源提供英语及简体中文；其它语言回退英语，不新增语言开关或第三方翻译服务。
- 覆盖常规/生成规则/模板/平台设置，目录状态、动作与上传菜单、预览和参数编辑弹窗、用户通知及欢迎/更新 HTML。
- 将上传平台身份与翻译后的显示名分离；所有本地化只改变呈现，不改变平台路由、设置字段、模板草稿和生成结果。
- 保持已有默认模板和生成文档内容；不翻译用户输入、源码业务名、第三方错误原文或日志注释，不自动重写保存值。
- 根据原生反馈，将设置指南统一为原生问号及悬浮链接，恢复默认模板移入编辑区；保持草稿保存行为。
- 增加资源覆盖、locale/参数格式、路由和页面测试；使用 doc-demo 检查英文与简中原生页面、窄布局和帮助气泡。

## Capabilities

### New Capabilities

- `plugin-ui-localization`: 插件主要界面的 locale 解析、资源覆盖、显示文本与稳定业务身份边界。

### Modified Capabilities

无。现有生成与输出 contract 不变。

## Impact

涉及 DocViewBundle 消息资源、Java UI/actions/通知、plugin.xml 呈现属性、欢迎页面与测试。生产保持 Java-only，使用现有 DynamicBundle/IntelliJ API，无新增依赖、语言包 extension、平台基线或网络请求。原生测试隔离 profile，结束后恢复普通 IDEA；不提交、不发布、不归档。
