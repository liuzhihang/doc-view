# Proposal

## Why

Doc View 当前按字段的验证注解、`@Schema(required = true)`、Swagger 2 `@ApiModelProperty` 和 `@DocView.Required` 计算必填状态，但不识别 OpenAPI 3 的 `@Schema.requiredMode`。这使 `NOT_REQUIRED` 无法覆盖 `@NotNull` 等必填来源，字段编辑还会继续写入已弃用的 `required`，并可能丢弃同一 `@Schema` 上的其他属性。

Swagger 官方将 `requiredMode` 定义为当前字段必填控制方式：`REQUIRED` 与 `NOT_REQUIRED` 覆盖推断，`AUTO` 使用库的推断规则；`required` 自 swagger-annotations 2.2.5 起已弃用。本 change 明确 Doc View 的字段规则，并为后续参数元数据与文档索引工作建立稳定输入。

## What Changes

- 让字段级 `@Schema.requiredMode` 的 `REQUIRED`、`NOT_REQUIRED`、`AUTO` 参与 `PsiField` 必填解析，并明确与 Doc View 现有来源的优先级。
- `AUTO` 使用 Doc View 当前的验证注解、Swagger 2 字段注解与可选文档 tag 规则；不声称复刻 swagger-core 的所有自动推断。
- 停止读取和生成已弃用的 `@Schema.required`；不为旧源码写法增加兼容 fallback 或批量迁移。Swagger 2 `@ApiModelProperty(required = true)` 保持为独立现有来源。
- 写回已有 `@Schema` 时用 `requiredMode` 表示当前编辑结果，保留其他 Schema 属性，并对 description 字符串做 Java 注解字面量转义。
- 用真实 Java PSI fixture、`DocView`/Markdown 断言及注解写回断言验证；不新增 Swagger runtime 依赖或插件设置。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `openapi3-annotation-compatibility`：扩展既有 OpenAPI 3 注解契约，定义字段级 `@Schema.requiredMode`、deprecated `required` 的停止支持边界，以及字段注解写回行为。

## Impact

影响 `DocViewUtils.isRequired(PsiField)`、已有 `@Schema` 字段的 `writeComment`、经 `Body.required`/`Param.required` 生成的字段状态及对应 Markdown 内容。源码支持范围限于字段注解；方法参数上的 `@Parameter` 元数据属于独立的 `openapi3-parameter-metadata` change。生产实现继续 Java-only，Swagger 注解仅按 PSI 全限定名解析，不增加插件运行时依赖。
