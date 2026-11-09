# Proposal

## Why

方法名称和描述按设置从注解、Doc View 注释标签或 Javadoc 依次读取；当前空白或无法求值的已启用注解值可能提前结束选择，Swagger 2 注解还可能把 PSI 源码文本而非实际字符串值写入文档。生成结果因此会出现空标题、空描述或常量名。

## What Changes

- 让已启用的注解来源仅在字符串可解析且非空白时命中；空白、缺省或无法解析时继续现有来源优先级。
- 对 `@Operation.summary`、`@Operation.description`、`@ApiOperation.value` 与 `@ApiOperation.notes` 使用 PSI 字符串求值；保留求得的非空字符串中的原始前后空格、引号字符与转义后的内容。
- 维持 `nameUse*`/`descUse*` 设置开关、名称来源顺序、Javadoc 名称 15 字符限制，以及最终方法名/Javadoc 描述 fallback。
- 用 Java PSI fixture、DocView 断言和 Spring Markdown golden 验证空白、默认值、常量求值、不可解析值、来源开关和最终 fallback。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `openapi3-annotation-compatibility`：扩展现有接口名称与描述来源的 fallback 约定，涵盖 Swagger 3 与 Swagger 2 方法注解。

## Impact

运行时范围仅限 `DocViewUtils.getName()` 与 `DocViewUtils.getMethodDesc()`。结果经现有 Spring/Dubbo 服务进入 `DocView` 和 Markdown；不修改标题、DTO 字段、参数、模板、设置持久化、上传 payload、注解写回或依赖。生产实现保持 Java-only。
