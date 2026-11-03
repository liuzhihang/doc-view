# Design

## Context

动机见 proposal.md。`open-issue-backlog` 已同步 3 项长期能力并归档，代码提交为 `9a4ee80`，原生续验记录提交为 `7c05136`。远端 develop/master 目前为 `7b2e0a8`，公开最新 GitHub Release 为 1.4.0；历史标签 v1.4.10/v1.4.11 指向更早提交，不能作为本轮功能基线。原工作区的未提交 Java 25/IU 263 等开发不属于当前发布。

## Goals / Non-Goals

**Goals:**
- 让最终安装包的版本、change-notes 与四份双语欢迎/更新页面准确对应本轮六项 Issue 交付。
- 以同一候选包身份关联自动门禁、原生烟测、GitHub Release 和 Marketplace 回读结果。
- 明确首次代码修复阶段、正式发布阶段与 Marketplace 审核状态的区别，保留既有历史证据。

**Non-Goals:**
- 不改动生产 Java、PSI/DTO/表格逻辑、依赖、平台基线、extension、上传逻辑或用户设置。
- 不将原工作区未提交功能和其他 active OpenSpec change 一并发布或归档。
- 不将 1.4.0 候选包的自动/原生结果冒充最终 1.5.0 包的验收；既有未完成场景按发布清单评估并记录。

## Decisions

1. 使用尚未使用的 1.5.0：本轮增加实体 Markdown 复制、版本标签与依赖源码注释读取，采用新的 minor 版本；保留历史 tag，不重用 1.4.0 或整理旧标签。
2. 版本仍由 `gradle.properties` 驱动 descriptor 与欢迎页版本占位符；将 Unreleased 正式化后沿用现有 changelog 构建逻辑，避免只改版本却仍打入旧 change-notes。
3. 只替换四份 HTML 的更新重点内容，保留页面结构、样式、语言选择、版本占位符与动作 URI。README 与 Marketplace 双语介绍同步能力与来源条件；用户指南沿用已实现修复中的说明。
4. 发布记录集中于 `docs/release-1.5.0.md`。最终 SHA、测试、原生结果和远端状态由实际执行后填入，准备阶段明确为待执行，不预填成功。
5. 按 develop → master PR 流程交付；tag 和 Release 指向验证过的发布树。读取 Release 与 Marketplace 元数据/制品后再关闭已交付 Issue；不重复结果不确定的发布操作。

## Risks / Trade-offs

- 新版本号与旧 change-notes 混用 → 从最终 ZIP 读回 descriptor 和四份 HTML，并核对当前版本条目。
- Marketplace 接收不等于公开 → 分别记录提交成功、更新 ID、审核与公开状态；需要回读时先查询既有更新。
- 原生验证与制品身份错配 → 最终版本重新执行适用门禁和原生确认，记录 ZIP/JAR 哈希，不汇总不同包为一次全量验收。
- 基线仍有未完场景 → 逐项评估与本次改动的关系并列出限制，不把发布授权描述为验证通过。
- 原工作区和远端并行开发 → 仅使用独立分支、发布前重新核对远端 SHA，保留其他未提交内容。

## Migration Plan

本次无需设置迁移；自定义模板需自行插入新增 since/version 变量，依赖注释需先在 IDEA 关联正确版本源码。完成最终包门禁与原生确认后，推送本轮变更至 develop，经 PR 合入 master，再创建 v1.5.0 tag、GitHub Release 并发布 Marketplace。交付回读后记录 Issue 结果，最后同步规格并归档本 change。回退可安装 1.4.0；不重写已公开 tag 或承诺降级后新增变量仍可用。
