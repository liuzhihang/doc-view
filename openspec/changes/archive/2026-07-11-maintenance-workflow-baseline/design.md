## Context

Doc View 已在 `develop` 上完成 1.3.12 的兼容性修复，但仓库维护基础设施处于不一致状态：Speckit skill 正在删除，OpenSpec skill/config 尚未纳入版本控制，`AGENTS.md` 仍保存一次性任务白名单和 Speckit 标记，四个 Doc View skill 又依赖已经删除的文档。当前还缺少活动 OpenSpec change，`openspec/config.yaml` 仅包含默认注释，无法向 artifact 生成过程提供项目约束。

本 change 是仓库治理和安全维护，不修改插件运行行为。它必须保留维护者现有的 staged/untracked 迁移状态，且不能通过回滚或另建不包含这些状态的 worktree 丢失用户改动。

## Goals / Non-Goals

**Goals:**

- 让 OpenSpec 成为变更 lifecycle 的唯一仓库级事实来源。
- 让 `AGENTS.md` 成为稳定、简洁的入口，并由 canonical docs 承载详细规则。
- 让四个 Doc View skill 各自承担 contract、文档、Java 实施和发布门禁职责，所有引用都可解析。
- 保留并现代化历史中仍有效的架构、contract、性能、兼容性、路线图和发布知识。
- 从当前树移除 YApi HTTP 示例中的非占位符地址和 token，并定义私有变量存放方式。
- 提供能证明迁移完整、链接有效、凭据未被继续跟踪的轻量验证。

**Non-Goals:**

- 不撤销或轮换任何外部 YApi/Marketplace 凭据。
- 不重写 Git 历史，不删除远端 tag 或 release。
- 不修改 Java、Gradle、`plugin.xml`、模板、资源、DTO、Markdown、上传或 UI 行为。
- 不在本 change 中建立自动化 PSI/Markdown 测试；该工作属于后续 `markdown-contract-baseline`。
- 不执行 `publishPlugin`、合并 `master`、推送 tag 或创建 GitHub Release。

## Decisions

### 1. OpenSpec 管 lifecycle，Doc View skill 管领域门禁

OpenSpec 的 proposal/design/spec/tasks/apply/archive 负责一项 change 从决策到实施的状态；Doc View skill 不复制 artifact 内容，只规定何时必须定义 contract、Java 实施注意事项、文档维护规则和发布验证。这样既保留领域约束，也避免 Speckit/OpenSpec/skill 三套任务清单并存。

备选方案是把全部规则写入 `openspec/config.yaml`。不采用，因为 skill 需要按任务类型触发，而 OpenSpec context 会对所有 artifact 无差别注入，容易膨胀并重复 `AGENTS.md`。

### 2. 恢复七份有明确职责的 canonical docs

建立并在 `AGENTS.md` 索引以下文档：

- `docs/architecture.md`：模块、入口、数据流和架构边界。
- `docs/contract-design.md`：可观察行为、输入/输出和验证规则。
- `docs/performance-guide.md`：PSI、EDT、缓存、导出和上传性能边界。
- `docs/intellij-compatibility.md`：平台、Java、Gradle、Verifier 和 API 兼容策略。
- `docs/ai-workflow.md`：OpenSpec 与 repo-local skill 的协作流程。
- `docs/roadmap.md`：产品化顺序与独立 change 边界。
- `docs/release-checklist.md`：发布前、手工、发布后和回滚检查。

内容以删除前最后版本为事实素材，但不机械恢复 Speckit 命令、一次性验证结果或过期表述。`docs/vibe-coding-workflow.md` 改名为更明确的 `docs/ai-workflow.md`。

遗留的 `docs/contracts/2026-06-17-1.3.12-internal-api-compatibility.md` 随迁移删除：完成版本结果由 `CHANGELOG.md` 和 Git 历史保留；长期规范性 requirements 进入 `openspec/specs/**`，单次 contract/delta 进入 `openspec/changes/**`，不继续维护第二套 contract 目录。

备选方案是删除 skill 中的所有深层文档引用，仅依赖 `AGENTS.md`。不采用，因为会让入口文件重新承载过多细节，也会丢失性能、兼容性和发布检查的可维护边界。

### 3. 一次性 scope 放入 change artifact

`AGENTS.md` 只保留长期允许/禁止原则，不再记录某一次修复允许修改的具体 Java 文件。每项 change 的实际范围由用户请求和活动 proposal/design/spec/tasks 决定；行为变化必须先有 contract 和验证路径。

### 4. OpenSpec 配置只保存稳定上下文

`openspec/config.yaml` 只记录 Java-only、contract-first、中文 artifact、安全要求和 artifact 规则等稳定约束。版本、Gradle、目标 IDE、分支和验证矩阵事实必须读取 `gradle.properties`、`build.gradle`、Gradle wrapper、当前 Git 状态和 canonical release checklist，避免在多处硬编码后漂移。

### 5. HTTP 示例使用显式变量，私有值不入库

`YApiGetTest.http` 保留手工 payload 示例。请求 endpoint 与 JSON `yapiUrl` 当前属于不同 origin，因此分别改为 `{{docViewTestEndpoint}}` 和 `{{yapiBaseUrl}}`，token 改为 `{{yapiToken}}`。`.gitignore` 忽略 IntelliJ HTTP Client 的 `http-client.private.env.json`；仓库不提供真实私有环境文件。项目 ID、目录 ID 和示例 payload 继续作为非敏感示例数据保留。

外部 token 是否仍有效无法由仓库改动证明，必须由维护者在对应 YApi 实例中撤销或轮换。历史重写会改变公开 commit/tag，单独决策。

### 6. 验证按变更类型分层

本 change 至少运行 OpenSpec status、skill/doc 引用存在性检查、Speckit 残留扫描、凭据占位符检查、`git diff --check` 和 diff 范围检查。因为没有 runtime 行为变化，不把 Gradle test/build/Verifier 作为本 change 完成条件；最终全路线验证阶段仍会统一运行它们。

## Risks / Trade-offs

- [历史文档可能包含过期事实] → 对照 `AGENTS.md`、`gradle.properties`、`build.gradle` 和当前源码更新，不直接恢复旧 blob。
- [七份文档增加维护成本] → 每份只保留单一职责，`AGENTS.md` 提供索引，skill 引用 canonical docs 而不复制内容。
- [OpenSpec 与 skill 职责再次重叠] → 在 `docs/ai-workflow.md` 和 skill 中明确 lifecycle/领域门禁边界。
- [HTTP token 已进入历史] → 当前树立即脱敏并明确外部轮换；历史重写保持为显式、独立且需授权的操作。
- [工作区已有用户 staged/untracked 状态] → 精准编辑目标文件，不 reset、checkout 或重排用户现有 staging。

## Migration Plan

1. 纳入 OpenSpec config、skills 和本 change artifacts，保留已暂存的 Speckit skill 删除。
2. 重写 `AGENTS.md` 的 scope、文档索引和 OpenSpec 工作流说明。
3. 基于历史素材创建七份 canonical docs，删除遗留单次 contract 文档，并更新四个 Doc View skill 引用。
4. 脱敏 YApi HTTP 示例，增加私有 HTTP environment ignore 规则。
5. 运行链接、残留、secret 和 diff 验证；只在实际完成后勾选 tasks。

回滚时可恢复本 change 修改的文档/config/示例，但不得恢复真实 token；Speckit skill 是否恢复需作为独立治理决定。

## Open Questions

无阻塞问题。外部 token 轮换、Git 历史重写和 1.3.12 GitHub Release 收尾在本地改动完成后由维护者单独授权。
