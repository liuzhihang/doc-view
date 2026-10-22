# Design

## Context

见 [proposal.md](proposal.md) 与本 change 的 [spec](specs/openapi3-annotation-compatibility/spec.md)。当前名称和描述均在 `DocViewUtils` 的 read action 中按设置选来源。名称的后续来源已经包含配置的 `@DocView.Name`、Javadoc 首行（截取 15 个字符）和 Java 方法名；描述最终回退到现有 Javadoc 解析。当前 Swagger 2 分支读取 `PsiAnnotationMemberValue.getText()` 并删去双引号，Swagger 3 描述也使用 PSI 源码文本；名称的 Swagger 3 summary 则已用 `AnnotationUtil.getStringAttributeValue` 并检查非空白。

## Goals / Non-Goals

**Goals:**

- 让所有四个方法级注解字符串属性按其实际 Java 字符串值参与现有来源选择。
- 只接受已启用、可解析且非空白的值，并保持有效值内容不变。
- 用 PSI 和 Spring 生成 golden 固定空值回退、设置开关、常量/转义求值及用户可见 Markdown。

**Non-Goals:**

- 不改类标题、DTO 字段/参数、Dubbo 自己的注解解析、Javadoc 解析规则或设置页面。
- 不增加注解别名、组合/继承注解、Swagger 依赖、通用来源框架或新的配置项。
- 不修改模板格式、上传 payload 或文档章节。

## Decisions

### 用平台字符串解析 API 取得注解值

`getName()` 与 `getMethodDesc()` 对 `@Operation.summary`、`@Operation.description`、`@ApiOperation.value` 和 `@ApiOperation.notes` 使用 `AnnotationUtil.getStringAttributeValue()`。取得值后只用 `StringUtils.isNotBlank()` 判断是否采用；不 `trim()`、不删除引号、不直接返回 PSI 节点的源码文本。这样字符串常量和 Java 转义按实际求值结果参与 fallback；无法解析或仅含空白时继续下一个来源。

替代方案是继续读取 `PsiAnnotationMemberValue.getText()` 并对文本作局部清理。该做法无法区分注解源码与实际常量值，也容易丢掉字符串中有意义的引号或转义，因此不采用。

### 保持来源顺序及设置开关

名称仍依次检查启用的 `@Operation.summary`、`@ApiOperation.value`、配置的 Doc View 名称标签、Javadoc 首行和 Java 方法名。Javadoc 名称仍受现有 15 字符限制。描述仍依次检查启用的 `@Operation.description`、`@ApiOperation.notes` 和现有 Javadoc 描述；没有可用来源时返回空字符串。来源开关只决定是否检查该来源，不改变顺序。

### 在现有 Spring 生成链验证用户可见结果

新增 light PSI fixture 覆盖字符串常量、空/空白/默认/不可解析值、来源开关、优先级与 fallback，并断言 `DocView.name` 和 `DocView.desc`。至少一条 fixture 经 `SpringDocViewServiceImpl` 与 `DocViewData.markdownText()` 输出完整 Markdown golden；使用固定模板仅呈现名称和描述，避免模板章节差异干扰字符串值断言。现有 `OpenApi3OperationSummaryTest` 与 Spring markdown contract 继续作为回归覆盖。

实现只触及 `DocViewUtils` 的两个方法及其测试，不引入新服务、依赖或存储格式。

## Risks / Trade-offs

- PSI 字符串表达式可能未解析：解析 API 返回不可用值时继续现有 fallback；fixture 覆盖已解析常量和 unresolved 表达式。
- 文本前后空格在 Markdown 中可见：这符合 contract 的原样保留要求；golden 精确固定结果。
- Javadoc 解析本身会按现有逻辑处理空格：本 change 保留该解析结果，不将 Javadoc 的文本规则扩大为注解值规则。

## Migration Plan

无持久化迁移。实现后生成结果会直接采用首个可用来源；已有设置及模板无需重配。
