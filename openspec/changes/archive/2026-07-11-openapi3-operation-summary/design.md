## Context

`DocViewUtils.getName(PsiMethod)` 已在 IntelliJ read action 中实现统一名称 fallback，并被 Spring/Feign 与 Dubbo 文档服务复用。Swagger 3 分支当前定位到 `io.swagger.v3.oas.annotations.Operation` 后读取 `name`，但标准注解提供的是 `summary`；因此该分支永远无法消费正常声明的名称。

Markdown 契约测试已固定默认输出结构。本 change 只修正方法名称元数据，不改变 endpoint 识别、DTO、模板、设置模型、线程边界或写回逻辑。

## Goals / Non-Goals

**Goals:**

- 在 `nameUseSwagger3=true` 时返回非空 `@Operation.summary`。
- 让空、仅空白或未声明的 summary 继续进入既有 fallback 链。
- 用 IntelliJ Java PSI fixture 证明 RED 原因是读取错误属性，并验证修复后的优先级与设置开关。
- 保持 `@Operation.description`、默认 Markdown shape 和所有非名称行为不变。

**Non-Goals:**

- 不解析 `operationId`、tags、deprecated、hidden、responses、security 或组合/继承注解。
- 不处理 `@Schema.requiredMode`、参数 `@Parameter` 或任何 DTO 扩展。
- 不实现 OpenAPI 文档导入/导出，也不新增 OpenAPI/Swagger runtime 依赖。
- 不修改注解写回、Settings UI、`plugin.xml`、Velocity 模板或平台 payload shape。

## Decisions

### 选择单属性修复，而不是一次性扩展所有 OpenAPI 3 注解

考虑过三种范围：完整 OAS 模型、`Operation`/`Schema`/`Parameter` 批量兼容、只修 `Operation.summary`。完整 OAS 需要参数位置、响应码、media type、components、security 和 `$ref` 等 source-neutral 模型，明显超出现有 DTO；批量兼容会立即引入 `requiredMode` 三态优先级、参数来源与无损写回问题。选择第三种可以在不改模型的情况下交付一个独立、用户可见且可回滚的修复，后续两个注解分别建立 contract。

### 使用 PSI 的字符串属性读取与非空判断

在现有 `@Operation` 分支使用 `AnnotationUtil.getStringAttributeValue(annotation, "summary")` 读取字符串语义，再用 `StringUtils.isNotBlank` 决定是否返回。这样未声明时的默认空值、显式空字符串和仅空白字符串都不会截断 fallback；非空值按注解内容返回。未选择仅替换属性名后无条件返回 `value.getText()`，因为默认 `""` 会让接口名称变空。

名称顺序保持为：Swagger 3 非空 summary、Swagger 2 value、自定义 Javadoc tag、方法首行注释、Java 方法名。Swagger 3 开关关闭时不读取 summary。

### 测试直接定位名称规则，并保留一条服务级断言

新增 `OpenApi3OperationSummaryTest` 和仓库内 Java fixture。测试通过最小同名注解桩建立真实 PSI，不引入第三方 runtime 依赖。一个场景通过生产 Spring service 断言 `DocView.name`，其余场景直接调用 `DocViewUtils.getName`，使优先级失败定位清晰；同时断言 description 继续由现有分支读取。既有 Spring Markdown golden 作为结构回归门禁，不为单字段内容变化复制第二份完整 golden。

### 用户可见影响通过 changelog 和路线图说明

修复会改变已有 `@Operation(summary=...)` 项目的接口显示名称，因此在 Unreleased/Fixed 记录。路线图把原来的广泛 OpenAPI change 拆成 summary、schema required mode、parameter metadata 三个可独立评审切片；不改变完整 OAS 暂缓的判断。

## Risks / Trade-offs

- [已有项目的标题、导出名或平台标题发生变化] → 这是修复后的预期行为，记录 changelog；用户仍可通过 `nameUseSwagger3=false` 使用旧 fallback 来源。
- [空 summary 阻断 fallback] → 对读取结果执行 `isNotBlank`，并分别测试显式空白、未声明和关闭设置。
- [同一 summary 导致既有重复名称处理被触发] → 本 change 不改变去重算法；在后续独立 contract 中评估其随机后缀行为。
- [注解桩与库版本不一致] → 只声明标准且稳定的 `summary`/`description` 与 Swagger 2 fallback 属性，并使用真实全限定名。
- [扩大到常量表达式或组合注解形成隐式承诺] → spec 仅保证方法上的直接 `@Operation` 与普通字符串属性，其他形式保持未定义。

## Migration Plan

无需数据或设置迁移。发布后按现有开关即时生效；回滚只需撤销 `getName` 的单分支修复及对应测试/changelog。若维护者需要临时恢复 fallback，可关闭 Swagger 3 名称来源。

## Open Questions

无。本 change 的范围、优先级和 fallback 已由现有 Settings 与 DTO 能力确定。
