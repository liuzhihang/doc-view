## Why

仓库正在从 Speckit 迁移到 OpenSpec，但当前状态同时存在已暂存的 Speckit skill 删除、未跟踪的 OpenSpec 配置与 skill、`AGENTS.md` 中的一次性变更白名单和 Speckit 残留，以及 repo-local skill 对已删除文档的失效引用。与此同时，已跟踪的 YApi HTTP 示例包含非占位符的凭据形态数据，需要先建立可执行、无断链且不保存敏感信息的维护基线，再开展运行时功能开发。

## What Changes

- 完成 Speckit 到 OpenSpec 的仓库级工作流迁移，保留 OpenSpec proposal/design/spec/tasks 作为变更事实来源。
- 将 `AGENTS.md` 调整为长期入口规则，移除一次性兼容性修复白名单和 Speckit 残留。
- 恢复或重写最小 canonical docs 集合，覆盖架构、contract、性能、IntelliJ 兼容性、AI 工作流、路线图和发布检查。
- 删除遗留的单次 `docs/contracts/**` 记录；长期规范写入 `openspec/specs/**`，单次 delta 写入 `openspec/changes/**`。
- 更新四个 Doc View repo-local skill，使其只引用实际存在的 canonical docs，并明确与 OpenSpec 生命周期的分工。
- 在 `openspec/config.yaml` 中记录稳定的项目上下文和 artifact 规则，避免 proposal/design/tasks 重复或遗漏关键约束。
- 将已跟踪的 YApi HTTP 示例改为显式占位符，不保留 token 或私有服务地址；外部凭据撤销/轮换和 Git 历史重写不在本 change 内执行。
- 不修改 Java 生产代码、Gradle 构建逻辑、`plugin.xml`、运行时资源或插件可观察行为。

## Capabilities

### New Capabilities

- `maintenance-workflow`: 定义 Doc View 使用 OpenSpec、canonical docs、repo-local skill、凭据安全规则和验证门禁进行可审计维护的仓库级契约。

### Modified Capabilities

无。当前 `openspec/specs/` 中没有既有 capability。

## Impact

- 影响 `AGENTS.md`、`docs/**/*.md`、`.codex/skills/**`、`openspec/**`、已暂存的 `.agents/skills/speckit-*` 删除、遗留 contract 文档删除，以及 `src/test/http/YApiGetTest.http`。
- 不影响插件运行时 API、PSI 解析、DTO、Markdown 输出、上传 payload、导出、设置持久化或 UI 行为。
- 验证以 OpenSpec artifact 完整性、文档/skill 链接存在性、凭据占位符检查、`git diff --check` 和 diff 范围检查为主。
