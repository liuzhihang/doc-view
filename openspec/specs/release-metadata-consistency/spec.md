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

### Requirement: 发布记录区分交付范围与验证状态
发布记录 MUST 区分精确发布包中包含的能力、明确排除的工作和仍未验证的检查。MUST NOT 将历史候选的结果当作最终 1.6.0 包验收。

#### Scenario: 正式 MCP 和 Skill 不属于本版
- **WHEN** 用户阅读 1.6.0 发布记录或随包版本摘要
- **THEN** 文案不声称已交付正式 Doc View MCP 工具或可安装/更新的 Doc View Skill

#### Scenario: 一次性例外下仍有未完成原生验收
- **WHEN** 某原生验收场景未在精确的最终 1.6.0 包上执行
- **THEN** 发布记录将该场景列为未验证并说明后续安排，不根据较早候选将其标为通过

### Requirement: 准确表达需要重启的插件生命周期
公开的 1.6.0 包 MUST 声明插件生命周期需要重启；发布记录 MUST NOT 承诺热更新或热卸载。

#### Scenario: 回读最终包的生命周期元数据
- **WHEN** 维护者检查最终 1.6.0 ZIP 中的插件 descriptor
- **THEN** descriptor 含有 `require-restart="true"`，且发布记录说明安装、更新和卸载需重启 IDE 后生效
