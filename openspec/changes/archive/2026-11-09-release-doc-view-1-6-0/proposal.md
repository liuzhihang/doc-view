# Proposal

## Why

Doc View 1.5.0 是当前最新的公开版本。此后获准推进的工作已升级生产基线至 Java 25 / IntelliJ IDEA 263，并加入 HTTP 请求生成修复、UI 与 OpenAPI Schema 改进以及 JSON 项目文档库。需要用一份发布计划将精确制品、兼容性元数据和真实验证证据关联起来，同时保留尚未完成的原生验收状态。

## What Changes

- 以 1.6.0 准备当前获准的交付范围：Java 25 / IDEA 263 基线、HTTP 请求生成及其 UI 精简、OpenAPI Schema 行为修复、落盘 JSON 项目文档库。
- 发布前完成最终源码和 descriptor 状态，包括 `require-restart="true"`；构建并回读唯一 ZIP/JAR，运行完整必要的测试、配置检查和 Plugin Verifier 门禁，并执行仍可完成的原生检查。
- 对 2026-11-09 一次性发布例外下仍未完成的原生验收逐项列明并保留为未验证；已知功能缺陷仍须修复并复验。不得把历史候选结果描述为最终包验收。
- 明确本版不交付正式 Doc View MCP 工具或可安装/更新的 Doc View Skill。明确安装、更新和卸载需要重启 IDE，不承诺热更新或热卸载。
- 只有最终包门禁通过后，才按正常版本、tag、GitHub Release 和 Marketplace 流程发布并回读状态。2026-11-09 的远端审计显示最新公开版为 1.5.0 且 1.6.0 尚未占用；发布前必须复查。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `release-metadata-consistency`：要求 1.6.0 发布记录区分已交付范围、明确排除的 MCP/Skill、例外下仍未验证的原生场景，并准确说明重启生效的生命周期策略。

## Impact

计划中的制品涵盖平台/Java 基线、HTTP 请求输出、UI 操作、OpenAPI Schema 和项目内 Markdown/JSON 文档。对应活动 change 仍是功能要求及未完成工作的事实来源；本发布 change 只统筹最终包身份和发布证据，不批量完成或归档它们。正式 MCP 工具与可安装 Skill 不属于本版交付。

最新远端审计时，1.6.0 尚未占用，1.5.0 仍为最新公开版本；该快照不能替代发布前重新检查。针对最终 1.6.0 候选的构建、测试、Plugin Verifier、原生验收、GitHub Release、tag 和 Marketplace 发布目前均待执行。
