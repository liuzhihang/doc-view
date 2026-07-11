# AI 协作工作流

Doc View 使用 OpenSpec 管理 change lifecycle，使用 repo-local skill 提供领域门禁。目标是让设计、实现、验证和发布可审计，不维护并行的规格系统。

## 事实来源

```text
源码 / Gradle / plugin.xml   当前实现与版本事实
openspec/specs/**           长期规范性 requirements
openspec/changes/**         单次 proposal/design/delta/tasks
docs/*.md                   人类可读指南和检查清单
AGENTS.md                   稳定入口、guardrails 和索引
.codex/skills/**            按任务触发的操作路由
```

## Change lifecycle

1. `openspec-explore`：只读调查、比较方案、澄清用户价值和风险。
2. `openspec-propose`：创建 proposal、design、delta spec 和 tasks，达到 apply-ready。
3. `openspec-apply-change`：读取 CLI 返回的 context files，按顺序实施并即时同步 checkbox。
4. 验证：按 contract 和 [发布检查清单](release-checklist.md)运行最小充分命令与手工场景。
5. `openspec-sync-specs`：需要在归档前更新长期 specs 时同步 delta。
6. `openspec-archive-change`：实现、验证和必要同步完成后归档。

低风险机械文档修正可以直接实施；新功能、行为、兼容性、架构和维护流程变化必须先走 proposal。

## Doc View skill 分工

| Skill | 何时使用 | 责任 |
| --- | --- | --- |
| `doc-view-contract-design` | PSI/DTO/Markdown/集成/UI/兼容行为设计 | 定义输入、可观察输出、不支持场景和验证 |
| `doc-view-docs-maintenance` | `AGENTS.md`、docs、OpenSpec/skill 工作流 | 维护 canonical docs、链接和文档-only 边界 |
| `doc-view-java-maintenance` | apply-ready 的 Java runtime change | 沿用架构、保护 IntelliJ 生命周期并运行验证 |
| `doc-view-release-readiness` | release candidate 或发布复核 | 检查版本、changelog、diff、构建、Verifier、手工验证和回滚 |

OpenSpec skill 不复制 Doc View 领域规则；Doc View skill 不创建第二套 proposal/design/tasks。

## 实施纪律

- 默认在 `develop` 开展日常开发；实际分支和 scope 以用户请求及当前 Git 状态为准。
- 先读取 CLI `status`/`instructions apply` 返回的全部 context files。
- 保持改动小而聚焦，不顺手重构、改名或清理既有死代码。
- 运行时 behavior 使用 TDD：先失败验证，再最小实现，再全量回归。
- 每项 task 只有在实现和验证都完成后勾选；发现设计问题先更新 artifacts。
- 文档-only change 不修改 Java、Gradle、`plugin.xml` 或 runtime resource。
- 不自动执行 `publishPlugin`、推送、GitHub Release、历史重写或外部凭据轮换，除非维护者明确授权。

## 敏感信息

- contract、fixture 和 HTTP 示例使用虚构代码、占位符和非生产 endpoint。
- IntelliJ HTTP Client 私有变量写入未跟踪的 `http-client.private.env.json`。
- 发现历史疑似凭据时先脱敏当前树；外部撤销/轮换不可由代码变更替代。
- Git 历史重写会影响分支、tag、fork 和协作者，必须单独批准并协调。

## 完成报告

报告必须说明：完成的 artifacts/tasks、修改文件、实际验证结果、未运行验证及原因、剩余风险和需要维护者授权的外部动作。不要把缓存命中、0 测试或文档检查描述成运行时行为已覆盖。
