# Contract 设计

Doc View 的 contract 是对用户或集成可观察行为的稳定约定。规范性 requirement 写入 `openspec/specs/**`，单次变更的 delta、design 和 tasks 写入 `openspec/changes/**`；本文只说明如何设计 contract。

## 何时必须定义

以下变化必须先有 apply-ready 的 OpenSpec change：

- Spring、Feign、Dubbo 或通用 PSI 识别/解析规则。
- `DocView`、`Body`、`Param`、`Header` 字段、默认值或层级。
- Markdown 结构、模板变量、示例 JSON、复制或导出结果。
- YApi、ShowDoc、YuQue payload、错误处理或通知。
- 设置默认值、持久化、迁移或 UI 可见行为。
- IntelliJ API、extension、线程模型、兼容范围或依赖。

纯文案、链接和格式修正可以不建立运行时 contract，但不得借文档 change 修改行为。

## Contract 输入

根据影响区域明确输入：

- Java：类、方法、参数、字段、类型、泛型、继承和注解。
- 文档：Javadoc、tag、Swagger 2/OpenAPI 3 属性和用户模板。
- 设置：字段过滤、必填策略、递归深度、模板和平台配置。
- 平台：IDE/Dumb Mode/project 生命周期、外部服务状态和网络失败。

Java 示例不得包含私有业务代码，平台示例不得包含真实 token 或生产地址。

## Contract 输出

至少描述受影响的可观察结果：

- 是否识别为 Doc View class/method。
- `DocView` 标题、名称、描述、路径、HTTP method、content type。
- Header、Query/Form、request body、response body 的字段、必填、层级和示例。
- Markdown 标题/章节/表格顺序、空值和示例 JSON。
- 上传 URL 语义、payload 字段、成功判断、错误通知与敏感信息隐藏。
- UI 状态、写回结果、设置持久化或兼容性范围。

## Requirement 场景模板

```markdown
### Requirement: OpenAPI 3 summary 优先级
系统 SHALL 在启用 Swagger3 名称来源时优先使用非空的 `@Operation.summary`。

#### Scenario: summary 非空
- **WHEN** Spring 方法声明 `@Operation(summary = "查询用户")`
- **THEN** `DocView.name` 等于 `查询用户`
- **AND** Markdown 其他章节结构保持不变

#### Scenario: summary 为空
- **WHEN** `@Operation.summary` 为空且存在 Swagger 2 或 Javadoc 名称
- **THEN** 系统继续执行既有 fallback 顺序
```

## 不支持与保持不变

每个 behavior change 都要写明：

- 本次明确不解析的注解、类型或 OpenAPI 结构。
- 不改变的 DTO/Markdown/平台 payload/设置字段。
- 遇到循环、无效 PSI、Dumb Mode、缺少配置或网络失败时的降级。
- 用户自定义 Velocity 模板变量的兼容边界。

## 验证选择

优先使用能直接证明 contract 的最小验证：

1. IntelliJ light fixture 验证 Java PSI 到 `DocView`。
2. DTO 断言和 Markdown golden sample。
3. 序列化或 mock endpoint 验证平台 payload。
4. `runIde` 验证 UI、action、写回和生命周期。
5. `verifyPlugin` 验证 descriptor/API 兼容性。

测试必须先证明旧实现不满足新增 requirement，再用最小实现使其通过。不能自动化的行为在 tasks 和发布清单中记录手动步骤与剩余风险。
