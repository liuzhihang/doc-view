# Proposal

## Why

六项 Issue 的修复已经完成、验证并归档，但候选包仍使用已发布的 1.4.0 版本号及旧版更新摘要。维护者现已明确授权升级版本、发布、打 tag、创建 Release，并在核实交付后关闭已解决 Issue，需要让安装包和公开发布信息准确表达本轮交付。

## What Changes

- 以 1.5.0 发布本轮已实现能力，将 CHANGELOG 的本轮 Unreleased 条目整理为 2026-11-03 正式版本记录。
- 同步包版本、双语欢迎页与更新摘要、README、Marketplace 描述和发布记录；页面说明实体 Markdown 复制、文档标签、已关联依赖源码以及路由/参数层级修复。
- 核对最终 1.5.0 包的 descriptor、change-notes、双语资源和实际验证结果，按 develop → master 流程交付并核验 tag、Release 和 Marketplace 状态。
- 仅在修复已交付后关闭对应 Issue；Marketplace 待审与已公开状态分别记录。

## Capabilities

### New Capabilities
- `release-metadata-consistency`：安装包身份、当前版本更新说明与双语安装版摘要的一致性。

### Modified Capabilities
无。本次不改变六项修复已归档的功能契约。

## Impact

影响版本属性、CHANGELOG、双语 HTML 欢迎/更新资源、项目与 Marketplace 介绍以及发布证据。Java 21、IU 2026.1、生产代码、依赖和 extension 保持本轮既定基线。`open-issue-backlog` 归档中“不升级、不发布”的限制属于其当时开发阶段；本 change 记录后来获得的交付授权，不改写历史证据。原工作区未提交功能及其他未完成 OpenSpec change 不纳入本次发布范围。
