# release-metadata-consistency Specification

## Purpose

让安装 Doc View 的用户能够从插件身份、更新说明和随包双语摘要准确辨认当前版本提供的能力，并了解能力所需的输入条件；公开发布信息必须能与实际安装包对应，避免旧版摘要或开发期状态造成误解。

## Requirements

### Requirement: Package version and change notes agree
正式安装包 MUST 使用该次发布的唯一版本号，并提供对应版本的更新说明；MUST NOT 复用已有版本号发布新的功能内容，或把其他版本的更新说明作为当前版本说明。

#### Scenario: Read back a release archive
- **WHEN** 维护者或用户读取正式发布 ZIP 内的插件 descriptor
- **THEN** 插件版本与对应 tag/Release 版本一致，change-notes 描述该版本实际交付的新增能力与修复，且保留实际兼容性基线

### Requirement: Installed version highlights describe delivered capabilities
随插件提供的英文和简体中文欢迎页及更新页 MUST 显示已安装版本，并以对应语言概述该版本实际交付的能力及必要使用条件；MUST NOT 将未交付功能、自动下载源码或未验证的未来 IDE 支持写为当前能力。

#### Scenario: Open the localized installed version summary
- **WHEN** 用户在英语或简体中文 IDE 中打开欢迎页或更新页
- **THEN** 页面中的版本与插件 descriptor 一致，更新重点与当前版本 CHANGELOG 对应，仍提供完整更新记录入口

#### Scenario: Explain dependency source documentation
- **WHEN** 当前版本的更新摘要介绍依赖 JAR 注释读取
- **THEN** 摘要说明需要 IDE 关联对应源码，不声称可以从纯字节码恢复 Javadoc 或自动下载源码
