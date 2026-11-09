# Proposal

## Why

Spring 接口参数目前只从 Java 参数名和 Javadoc `@param` 生成名称与描述，无法利用方法参数上的 OpenAPI 3 `@Parameter` 元数据；同时 `@RequestHeader` 与 `@RequestParam`/`@PathVariable` 的绑定名称和必填规则目前不一致。用户看到的参数文档因此可能缺少源码中已有的名称、描述、示例或必填信息。

## What Changes

- 为当前已生成 Spring `Param`/`Header` 的方法参数定义直接 `@Parameter` 的 `name`、`description`、`example`、`required` 映射，以及空值回退和 Spring 注解冲突优先级。
- 让 `@RequestParam`、`@PathVariable`、`@RequestHeader` 的显式 `name`/`value` 绑定名优先；仅在 Spring 未显式命名时将非空 `@Parameter.name` 作为文档名回退。
- 保持参数类型、当前参数分类与 Markdown 结构；对 request body、对象参数扁平化字段及方法级 OpenAPI 参数明确限制支持范围。
- 用真实 Java PSI fixture、`DocView` 断言和 Markdown golden 验证；不新增 Swagger 运行时依赖、设置项或旧注解写法适配。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `openapi3-annotation-compatibility`：扩展既有 OpenAPI 3 capability，定义 Spring 方法参数上的 `@Parameter` 元数据与绑定注解的映射和未支持边界；与字段级 `@Schema.requiredMode` 分开验证。

## Impact

影响 `SpringPsiUtils` 组装的 `Param`/`Header`、`DocViewData` 参数输出及相应 Markdown golden。仅限当前已输出为直接 `Param` 的参数和 String `@RequestHeader`；不改变 Dubbo 参数、DTO 字段级 `@Schema`、request-body 构建、参数位置建模或平台上传 payload。生产实现继续 Java-only，通过 PSI 全限定注解名解析，不引入新依赖。
