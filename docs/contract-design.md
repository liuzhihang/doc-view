# Contract Design

Doc View 的核心价值是从 Java PSI 生成可读、可导出、可上传的 API 文档。任何行为变更都必须先定义 contract，再实现 Java 代码。

## 什么是 Contract

Contract 是对外可观察行为的稳定约定。对 Doc View 来说，contract 包括：

- 输入 Java 代码样例。
- 支持的注解、注释、类型、泛型和配置。
- 解析后 `DocView`、`Body`、`Param`、`Header` 等 DTO 形状。
- Markdown 输出结构和关键文本。
- YApi、ShowDoc、YuQue 请求/响应 payload。
- 设置默认值和持久化字段。
- IntelliJ 版本、插件依赖和 extension 行为。
- 性能、错误处理和用户通知边界。

Contract 不是实现细节。实现可以重构，但对外 contract 变化必须明确说明。

## Contract-first 流程

1. 创建或更新变更说明和 contract 记录。
2. 在 spec 中描述用户可观察行为。
3. 在 design 中说明影响的 PSI、DTO、模板、UI、集成或兼容性区域。
4. 为每个行为变化准备输入样例和预期输出。
5. 能自动化时先写测试；不能自动化时写清楚手动验证步骤。
6. 实现 Java 代码。
7. 用 contract 验证输出。
8. 更新文档和 release checklist。

## PSI 解析 Contract

每个解析 contract 至少包含：

- 框架：Spring、Feign、Dubbo、普通 interface 或 XML Dubbo definition。
- Java 输入：类、方法、参数、字段、返回值、注解、Javadoc。
- 设置输入：字段过滤、必填注解、JsonProperty、line marker、普通 interface 开关等。
- 预期：是否识别为可生成文档的方法。
- 预期 `DocView`：标题、名称、描述、路径、HTTP 方法、content type、Header、请求参数、请求体、响应体和示例。

示例模板：

```text
Given:
- Java class: ...
- Settings: ...

When:
- SpringDocViewServiceImpl.buildClassMethodDoc(...)

Then:
- path = ...
- method = ...
- contentType = ...
- reqBody fields = ...
- respBody fields = ...
```

## DTO Contract

`DocView` 是多数后续流程的核心输入。修改 DTO 字段时必须说明：

- 字段名称和语义。
- 默认值和空值策略。
- 对 Markdown 模板的影响。
- 对导出、上传、复制、编辑写回的影响。
- 对旧设置或历史缓存的兼容性。

`Body`、`Param`、`Header` 等嵌套 DTO 的字段顺序、必填标记、示例值和层级缩进会直接影响生成文档，不应随意改变。

## Markdown Contract

Markdown contract 要覆盖：

- 标题层级。
- 接口基本信息顺序。
- Header、Query/Form、Body、Response 表格字段。
- 示例 JSON 格式。
- 空字段、无参数、void 返回值、异常输入的展示。
- 用户自定义 Velocity 模板变量兼容性。

模板变更前必须确认：

- 旧模板变量是否仍可用。
- 新变量是否有默认值。
- 输出是否保持 Markdown 可读。
- 导出和复制行为是否一致。

## 平台集成 Contract

YApi、ShowDoc、YuQue contract 应包含：

- 配置项：base URL、token、项目 ID、目录 ID、页面 ID 等。
- 请求 URL、method、header 和 body。
- 响应成功判断。
- 错误码、网络失败、超时和认证失败处理。
- 用户通知文案。
- 敏感信息隐藏策略。

集成 contract 不能包含真实 token 或生产地址。

## IntelliJ 兼容 Contract

涉及 IntelliJ API 时必须说明：

- 目标平台版本和 since build。
- 是否依赖 Java 插件、Markdown 插件或 IU 专属能力。
- 是否需要 read action、write command、DumbAware 或 background task。
- 是否影响 `plugin.xml` extension、action、tool window、settings page。
- `verifyPlugin` 和 `runIde` 验证方式。

## 验证策略

优先级从高到低：

1. 单元测试或 fixture 验证 DTO。
2. 黄金文件验证 Markdown 输出。
3. 序列化测试验证平台 payload。
4. `runIde` 手动验证 UI 和 action。
5. `verifyPlugin` 验证兼容性和 descriptor。

文档-only 变更不需要新增测试，但必须说明没有 runtime 行为变化。

## 变更记录

当 contract 变化影响用户可见行为时：

- 变更说明或 contract 文档必须描述新增、修改或移除的 requirement。
- `CHANGELOG.md` 应在发布前更新。
- `docs/release-checklist.md` 应指向对应验证项。
- 破坏性变化必须给出迁移建议。
