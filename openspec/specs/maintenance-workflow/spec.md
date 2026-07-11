# Maintenance Workflow Specification

## Purpose

定义 Doc View 使用 OpenSpec、canonical docs、repo-local skill、凭据安全规则和验证门禁进行可审计维护的长期仓库契约。

## Requirements

### Requirement: OpenSpec change lifecycle
仓库 MUST 使用 OpenSpec proposal、design、spec 和 tasks 记录新功能、用户可见行为、兼容性、架构或维护流程变化，并以活动 change artifact 表达一次性范围。

#### Scenario: 行为变化开始前
- **WHEN** 维护者准备修改 PSI、DTO、Markdown、上传、导出、设置、UI 或兼容性行为
- **THEN** 仓库中存在 apply-ready 的 OpenSpec change，且其 spec 定义输入、预期输出、不支持场景和验证路径

#### Scenario: 低风险机械文档修正
- **WHEN** 改动仅修正文案、链接、注释或格式且不描述运行行为变化
- **THEN** 可以不创建独立运行时 contract，但仍按文档维护规则验证 diff 范围和链接

### Requirement: Stable repository guidance
`AGENTS.md` MUST 只包含长期项目事实、稳定约束、文档索引和工作流入口，MUST NOT 保存某一次任务的具体文件白名单或已结束工具链标记。

#### Scenario: 新 change 改变允许范围
- **WHEN** 一项新 change 允许修改此前未涉及的 Java、测试、Gradle 或资源文件
- **THEN** 该范围记录在用户请求和活动 OpenSpec artifact 中，而不向 `AGENTS.md` 追加一次性白名单

### Requirement: Canonical documentation set
仓库 MUST 维护架构、contract 指南、性能、IntelliJ 兼容性、AI 工作流、路线图和发布检查的 canonical docs，且 `AGENTS.md` 与 repo-local skill 的所有文档链接 MUST 指向实际存在的文件。规范性长期 requirements MUST 写入 `openspec/specs/**`，单次 delta 和 tasks MUST 写入 `openspec/changes/**`，MUST NOT 同时维护 `docs/contracts/**` 作为第二套事实源。

#### Scenario: Codex 开始领域任务
- **WHEN** contract、文档、Java 或发布 skill 被触发
- **THEN** skill 的 Read First 文件全部存在，并能提供该任务所需的最新领域约束

#### Scenario: 文档或 skill 发生变化
- **WHEN** 维护者新增、移动、删除 canonical doc 或修改 repo-local skill
- **THEN** 同一 change 更新相关索引和引用，并运行文件存在性与链接检查

#### Scenario: 行为 contract 被批准
- **WHEN** 一项行为 change 定义新的或修改既有 requirement
- **THEN** requirement 位于对应 OpenSpec spec/delta 中，docs 只解释 contract 写法而不保存并行规范副本

### Requirement: Skill responsibility boundaries
OpenSpec skill MUST 负责 change lifecycle；Doc View skill MUST 分别负责 contract 设计、文档维护、Java 实施和发布就绪领域门禁，并 MUST NOT 复制完整 proposal/design/tasks 内容。

#### Scenario: 从设计进入实现
- **WHEN** 行为 change 的 artifacts 已 apply-ready
- **THEN** 实现过程使用 OpenSpec apply 跟踪 tasks，并同时应用相应 Doc View contract/Java/release skill 的领域约束

### Requirement: Secret-free tracked examples
仓库 MUST NOT 跟踪真实 token、cookie、账号、生产私密地址或非占位符凭据；需要本地私有值的 HTTP 示例 MUST 使用变量，并将私有 environment 文件排除出版本控制。

#### Scenario: 执行 YApi HTTP 示例
- **WHEN** 维护者需要在本地调用 YApi 示例
- **THEN** 请求从未跟踪的私有 environment 中分别解析 `docViewTestEndpoint`、`yapiBaseUrl` 和 `yapiToken`，仓库文件只保存变量引用

#### Scenario: 发现历史疑似凭据
- **WHEN** 已跟踪文件中发现非占位符凭据形态数据
- **THEN** 当前树先脱敏，外部撤销/轮换和历史重写作为单独授权动作记录，任何日志或文档都不回显原值

### Requirement: Maintenance validation
文档和工作流 change MUST 验证 OpenSpec artifact 状态、canonical doc/skill 引用、旧工具链残留、凭据占位符、diff 格式和 diff 范围，并 MUST 准确报告未运行的 runtime 验证。

#### Scenario: 文档治理 change 完成
- **WHEN** maintenance tasks 全部实施
- **THEN** `openspec status` 显示 apply-required artifacts 完成，所有引用文件存在，Speckit 残留仅存在于有意保留的历史文本中，tracked HTTP 示例不含非占位符 token，且 `git diff --check` 无错误

### Requirement: Runtime behavior remains unchanged
本维护 change MUST NOT 修改 Java 生产代码、Gradle 构建行为、plugin descriptor、运行时资源、Markdown 输出、上传 payload、导出、设置持久化或 UI 可见行为。

#### Scenario: 评审维护 change diff
- **WHEN** 维护者检查本 change 的文件列表
- **THEN** 变更仅包含仓库指导、OpenSpec/skill、canonical docs、ignore 规则、HTTP 手工示例和既有 Speckit skill 删除
