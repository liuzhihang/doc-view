# Roadmap

本文档给出 Doc View 后续 Vibe Coding 维护路线。路线图用于排序和拆分 OpenSpec change，不代表所有事项都会一次性实现。

## 维护原则

- 先建立规则，再改行为。
- 先定义 contract，再写 Java。
- 优先修复可复现的解析问题，再做大范围重构。
- 任何用户可见行为变化都需要验证路径和回滚说明。
- 生产插件代码保持 Java-only。

## 阶段 0：维护基础设施

目标：让仓库具备 Codex 可持续维护的基本规则。

- 扩展 `AGENTS.md`。
- 建立 `docs/` 文档套件。
- 建立 Doc View 专用 Codex skills。
- 明确 documentation-only 和 runtime change 的边界。
- 明确 release checklist 和验证策略。

完成信号：

- Codex 能从 `AGENTS.md` 获取项目规则。
- Contract、Java-only、性能、MCP、i18n、兼容性和发布流程都有文档入口。
- 不产生运行时行为变化。

## 阶段 1：Contract 和测试基线

目标：把历史行为沉淀为可验证契约。

- 为 Spring Controller 解析建立输入样例和期望 `DocView` 输出。
- 为 Dubbo Service 解析建立输入样例和期望 `DocView` 输出。
- 为 Markdown 模板输出建立黄金样例。
- 为 YApi、ShowDoc、YuQue payload 建立序列化契约。
- 梳理字段过滤、必填识别、JsonProperty、Swagger/Swagger3、Validation 和 Lombok 规则。
- 建立最小回归测试路径，避免解析修复引入旧行为回退。

完成信号：

- 常见注解组合有可读 contract。
- 修改 PSI 工具前能明确预期行为。
- 关键生成结果有自动或半自动验证。

## 阶段 2：解析正确性

目标：提升 Spring/Dubbo 文档生成准确性。

- 修复路径合并、HTTP 方法识别、content type 判断和 Header 解析边界。
- 完善泛型、集合、数组、递归对象和枚举字段展示。
- 改善注释、Swagger、Swagger3、Validation、Lombok 注解优先级。
- 梳理 Feign、普通 interface、XML Dubbo definition 的支持范围。
- 明确不支持场景，并在 UI 或文档中给出可理解的反馈。

完成信号：

- 每个修复都有样例和预期输出。
- Spring/Dubbo 解析共享能力被抽象到合适工具层。
- 异常输入不会导致 IDE 卡顿或未处理异常。

## 阶段 3：UI 和编辑体验

目标：让预览、编辑、导出、上传流程更稳定、更可控。

- 改善 tool window 刷新、缓存、树节点状态和空状态。
- 梳理预览页复制、导出、上传入口的一致性。
- 优化参数编辑写回注释或注解的确认流程。
- 确认所有 UI 操作遵守 EDT 和 IntelliJ action update 规范。
- 将可本地化 UI 文案迁移到 message bundle。

完成信号：

- 常用流程可在 `runIde` 中手动验证。
- 长耗时任务不会阻塞 UI。
- UI 文案有统一来源。

## 阶段 4：平台集成稳定性

目标：让上传和远端文档平台集成更容易验证和排障。

- 明确 YApi、ShowDoc、YuQue 请求/响应契约。
- 统一 HTTP 错误处理、超时、日志和用户通知。
- 避免在日志或异常中暴露 token。
- 提供非生产环境手动验证建议。
- 为上传失败提供可操作错误信息。

完成信号：

- 每个平台有契约文档和最小验证流程。
- 网络失败不影响 IDE 稳定性。
- 敏感信息不进入仓库或日志。

## 阶段 5：IntelliJ 兼容性升级

目标：可控升级目标 IDE 和 Gradle IntelliJ Plugin。

- 维护 `platformVersion=2024.1`、`pluginSinceBuild=241` 的兼容说明。
- 升级前查找 IntelliJ API 变更和废弃项。
- 使用 `verifyPlugin` 和 `runIde` 做验证。
- 记录新旧平台差异和回滚方式。

完成信号：

- 每次兼容性升级都有独立 OpenSpec change。
- 发布前能说明支持的 IDE 范围。
- 插件验证结果可追踪。

## 阶段 6：MCP 探索

目标：评估是否用 MCP 辅助文档生成、契约验证或 IDE 外部工作流。

- 先设计 MCP 能力边界，不直接引入 runtime 依赖。
- 候选能力包括 contract 查询、样例生成、文档同步、发布检查、平台上传预检。
- 明确认证、文件访问、网络访问和用户确认策略。
- 原型必须和插件 runtime 解耦。

完成信号：

- 有独立 MCP proposal。
- 安全边界和非目标明确。
- 不影响当前插件安装包和用户 IDE 性能。
