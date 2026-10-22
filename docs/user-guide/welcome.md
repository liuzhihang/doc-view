# 欢迎使用 Doc View

从 Java 源码生成接口文档，在 IDEA 中查看、复制和导出 Markdown。本地生成不需要平台账号，也不需要启动应用服务。

IDE 内的 **Doc View Welcome** 页面随插件提供，离线时也可查看简短入门与本次更新摘要。

完整使用指南统一维护在 GitHub；IDE 设置中的帮助入口会在默认浏览器打开相应页面，因此阅读完整步骤需要网络。`develop` 上的文档可能先于公开插件版本；文档尚未推送时，对应 GitHub 页面可能暂时返回 404。

## 开始使用

1. 打开 Spring Controller、Feign 接口或 Dubbo 服务接口。
2. 将光标放在方法中，右键选择 **Doc View**，查看该方法的文档。放在类声明处可查看多个方法。
3. 选择 **Copy** 复制 Markdown，或选择 **Export** 保存到指定目录。

[完整快速开始与示例](index.md) · [设置说明](settings.md) · [故障排查](troubleshooting.md)

## 把模板改成团队需要的样式

Spring 与 Dubbo 分别使用各自的模板。通过变量插入接口名称、请求参数、响应和示例，先生成示例结果，再 Apply 保存。模板与配置按项目生效。

[模板变量与操作教程](templates.md)

## 按需连接文档平台

Doc View 支持 ShowDoc、YApi 和语雀。先核对目标项目、路径和凭据，再主动上传；本地预览和导出不依赖这些账号。

[平台配置指南](integrations.md)

## 更新与反馈

- **当前分支的变更记录**：[CHANGELOG](../../CHANGELOG.md)
- **完整使用指南**：[GitHub 用户指南](https://github.com/liuzhihang/doc-view/tree/master/docs/user-guide)
- **反馈问题**：[项目 Issues](https://github.com/liuzhihang/doc-view/issues)

提交反馈前请移除 Token、私有地址和不适合公开的源码。
