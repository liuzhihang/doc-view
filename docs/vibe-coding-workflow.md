# Vibe Coding Workflow

本文档定义 Doc View 使用 Codex 协作维护时的工作流。目标是让 AI 参与开发时仍保持可审计、可验证、可回滚。

## 基本循环

1. 读 `AGENTS.md`。
2. 判断是否需要 OpenSpec change。
3. 需要时先 propose，再 apply。
4. 实现前读取 proposal、design、spec 和 tasks。
5. 每完成一个 task 立即更新 checkbox。
6. 做最小必要验证。
7. 汇报实际完成项、验证结果和未覆盖风险。
8. 变更完成后再 archive。

## 什么时候必须用 OpenSpec

- 新功能。
- 用户可见行为变化。
- PSI 解析规则变化。
- DTO、Markdown 输出、上传 payload、导出行为变化。
- IntelliJ 兼容性升级。
- 架构边界或依赖变化。
- release 流程或 AI 协作规则变化。

小型文档修正可以直接改，但仍要保持 `AGENTS.md` 和 `docs/` 一致。

## Proposal 规则

Proposal 应回答：

- 为什么现在要改。
- 改什么，不改什么。
- 新增或修改哪些 capability。
- 影响哪些文件、系统和验证流程。
- 是否有 runtime 行为变化。

文档-only proposal 必须明确禁止夹带 runtime change。

## Apply 规则

Apply 时必须：

- 使用 OpenSpec CLI 读取状态和 apply 指令。
- 读取 CLI 返回的 context files。
- 按 tasks 顺序执行。
- 每项完成后更新 `tasks.md`。
- 遇到任务不清楚、设计问题或阻塞时暂停。
- 保持 diff 范围与 proposal/scope 一致。

## Codex 协作约束

- 不还原用户未要求还原的变更。
- 不把未验证的结果说成已通过。
- 不在文档-only 变更中修改 Java、Gradle、plugin.xml 或 runtime resources。
- 不新增生产语言，生产插件代码保持 Java-only。
- 不把 token、账号、生产平台地址写入仓库。
- 不用一次大重构掩盖小修复。

## Review Checkpoints

实现过程中至少检查：

- Diff 是否仍在允许范围内。
- Contract 是否和实现一致。
- 新文档是否链接到 `AGENTS.md` 或相关索引。
- 新 skill 是否引用规范文档，而不是复制大段内容。
- 验证命令是否与变更类型匹配。

## 文档维护

当行为变化时同步更新：

- `AGENTS.md`：入口规则或索引变化。
- `docs/architecture.md`：模块、服务、扩展点变化。
- `docs/contract-design.md`：contract 策略或样例变化。
- `docs/performance-guide.md`：性能约束变化。
- `docs/intellij-compatibility.md`：平台或 API 变化。
- `docs/release-checklist.md`：发布门禁变化。

## 完成标准

一个 Codex 协作任务完成时，应能回答：

- 哪些 tasks 被完成。
- 哪些文件被修改。
- 运行了哪些验证命令。
- 哪些验证没有运行，原因是什么。
- 是否存在 runtime 行为变化。
- 是否需要 archive OpenSpec change。
