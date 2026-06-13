# MCP Design

本文档是 Doc View 未来 MCP 能力的路线图和边界说明。本次基础设施变更不实现 MCP server、不引入 MCP runtime dependency、不修改插件行为。

## 目标

MCP 可用于把 Doc View 的维护流程暴露给 Codex 或其他工具，例如查询 contract、生成样例、做发布前检查或辅助文档同步。MCP 不应绕过 IDE 用户确认，也不应直接接触敏感凭据。

## 非目标

- 不在本次变更中实现 MCP server。
- 不把 MCP 依赖打进 IntelliJ 插件包。
- 不让 MCP 直接上传到 YApi、ShowDoc、YuQue。
- 不让 MCP 读取用户项目中的敏感代码或配置，除非用户明确授权。
- 不用 MCP 替代 IntelliJ plugin runtime。

## 候选能力

### Contract 查询

- 列出当前 contract capabilities。
- 查询 `docs/contract-design.md` 中的 contract 类型。
- 根据 Java 样例生成 contract 草案。
- 对比预期 `DocView` 和实际解析结果。

### 文档维护

- 检查 `AGENTS.md` 是否链接所有 docs。
- 检查 docs 是否包含必需章节。
- 为 release notes 提取文档-only 或 runtime change 摘要。

### 发布检查

- 根据 `docs/release-checklist.md` 输出待办。
- 检查 changelog、version、pluginSinceBuild、pluginUntilBuild。
- 汇总 `verifyPlugin`、`buildPlugin`、测试结果。

### 平台集成预检

- 校验 YApi、ShowDoc、YuQue payload 结构。
- 使用 mock endpoint 验证请求形状。
- 屏蔽 token 和敏感配置。

## 安全边界

- MCP 工具默认只读仓库文件。
- 写操作必须限制在用户明确允许的路径。
- 网络请求默认禁用；需要用户确认 endpoint。
- token、cookie、IDE 账号、Marketplace token 不通过 MCP 明文传递。
- 日志必须脱敏。
- destructive 操作必须二次确认。

## 架构方向

推荐分阶段探索：

1. Repo-local 只读 MCP：读取 docs、AGENTS、changelog 和 contract 记录。
2. 验证 MCP：运行有限的本地检查命令并返回结构化结果。
3. Contract MCP：辅助生成 contract 草案和 fixture 清单。
4. 集成预检 MCP：只对 mock 或用户确认的测试 endpoint 工作。

MCP server 应与 IntelliJ plugin runtime 解耦，避免影响插件启动、安装包大小和用户 IDE 性能。

## 数据模型草案

可能的资源：

- `doc-view://docs/index`
- `doc-view://changes`
- `doc-view://contracts/<capability>`
- `doc-view://release/checklist`

可能的工具：

- `list_contracts`
- `check_docs_index`
- `summarize_change`
- `build_release_checklist`
- `validate_payload_shape`

这些名称只是路线图，不是已实现接口。

## 验证策略

未来 MCP change 必须验证：

- 权限边界。
- 网络默认关闭。
- 脱敏逻辑。
- 只读工具不会修改文件。
- 写入工具只写允许路径。
- 插件 runtime 不依赖 MCP。

## 与当前变更的关系

当前变更只建立设计文档和路线图。最终 diff 中不应出现 MCP server、MCP dependency、plugin descriptor 修改、Gradle 修改或 Java runtime 修改。
