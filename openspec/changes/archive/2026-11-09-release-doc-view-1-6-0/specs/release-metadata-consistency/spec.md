# Spec Delta

## ADDED Requirements

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
