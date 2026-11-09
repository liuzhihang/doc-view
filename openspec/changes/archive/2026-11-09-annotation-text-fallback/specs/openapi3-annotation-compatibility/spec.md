# Spec Delta

## MODIFIED Requirements

### Requirement: Operation summary 作为接口名称

当 Swagger 3 名称来源开启时，系统 SHALL 将方法上直接声明且可求值、非空白的 `@Operation.summary` 映射为 `DocView.name`，并 SHALL 在其他名称来源之前使用该值。系统 SHALL 保留已求值字符串的实际内容，不裁剪前后空格。

#### Scenario: summary 非空
- **WHEN** Spring 方法直接声明 `@Operation(summary = "查询用户")`
- **AND** 项目设置 `nameUseSwagger3` 为 true
- **THEN** 生产 Spring 文档服务构建的 `DocView.name` 等于 `查询用户`

#### Scenario: summary 优先于 Swagger 2
- **WHEN** 同一方法声明非空 `@Operation.summary` 和非空 `@ApiOperation.value`
- **AND** Swagger 3 与 Swagger 2 名称来源均开启
- **THEN** `DocView.name` 等于 `@Operation.summary`

### Requirement: 空 summary 保持名称 fallback

系统 MUST 将未声明、空字符串、仅空白或无法求值的 `@Operation.summary` 视为不可用，并 SHALL 按当前名称来源顺序继续选择，不得返回空接口名称。

#### Scenario: summary 仅空白
- **WHEN** 方法声明 `@Operation(summary = "   ")` 和 `@ApiOperation(value = "Swagger 2 名称")`
- **THEN** `DocView.name` 等于 `Swagger 2 名称`

#### Scenario: summary 未声明
- **WHEN** 方法声明未指定 summary、使用默认 summary，或 summary 无法求值，且存在非空的后续名称来源
- **THEN** 系统使用首个启用且可求值的后续来源

### Requirement: Swagger 3 名称设置开关

系统 SHALL 只在 `nameUseSwagger3` 为 true 时读取 `@Operation.summary`；来源关闭时 SHALL 使用下一启用且可用的名称来源。

#### Scenario: Swagger 3 名称来源关闭
- **WHEN** 方法具有非空 `@Operation.summary` 和非空 `@ApiOperation.value`
- **AND** `nameUseSwagger3` 为 false、Swagger 2 名称来源为 true
- **THEN** `DocView.name` 等于 `@ApiOperation.value`

### Requirement: 名称修复不改变其他文档契约

系统 SHALL 保持 `DocView` 的非名称字段、参数模型、默认 Markdown 章节与模板变量、设置持久化、上传 payload shape 和注解写回行为不变；方法描述来源按本 capability 的描述 requirement 执行。

#### Scenario: summary 与 description 同时存在
- **WHEN** 方法声明可用的 `@Operation.summary` 和 `@Operation.description`
- **THEN** `DocView.name` 来自 summary
- **AND** `DocView.desc` 按方法描述来源顺序生成
- **AND** 默认 Spring Markdown 的章节与模板变量保持不变

### Requirement: 当前已验证范围

本 capability 当前 SHALL 只保证方法上直接声明、使用字符串属性的 `@Operation.summary`、`@Operation.description`、`@ApiOperation.value` 与 `@ApiOperation.notes`，以及既有名称和 Javadoc fallback；不得把其他 OpenAPI 注解或完整 OAS 能力描述为已支持。

#### Scenario: 检查 capability 声明
- **WHEN** 本 capability 的已验证范围被引用
- **THEN** 它只包含上述方法级字符串属性及已定义的 fallback
- **AND** `@Schema.requiredMode`、参数 `@Parameter`、组合/继承注解、注解写回和完整 OAS 导入导出仍不属于本 capability 的范围

## ADDED Requirements

### Requirement: Swagger 2 名称按可用来源继续 fallback

系统 SHALL 仅在 `nameUseSwagger` 开启且 `@ApiOperation.value` 可求值、非空白时采用该名称，并 SHALL 在不可用时继续既有名称来源顺序。

#### Scenario: Swagger 2 名称常量解析
- **WHEN** enabled `@ApiOperation.value` 引用可求值的字符串常量
- **THEN** `DocView.name` 使用常量的实际字符串值
- **AND** 保留该值中的前后空格、引号字符和转义后的实际内容

#### Scenario: Swagger 2 名称空白或无法求值
- **WHEN** enabled `@ApiOperation.value` 未声明、为空、仅含空白或无法求值
- **THEN** 系统依次检查启用的配置注释标签和 Javadoc 首行
- **AND** Javadoc 首行仍最多保留 15 个字符
- **AND** 没有可用注释来源时使用 Java 方法名

#### Scenario: Swagger 2 名称来源关闭
- **WHEN** `nameUseSwagger` 为 false
- **THEN** 系统忽略 `@ApiOperation.value` 并检查下一启用且可用的名称来源

### Requirement: 方法描述按可用来源继续 fallback

系统 SHALL 按启用的 Swagger 3 `@Operation.description`、Swagger 2 `@ApiOperation.notes`、现有 Javadoc 描述的顺序选择首个可求值且非空白的字符串；若三者均无可用值，系统 SHALL 返回空描述。

#### Scenario: Swagger 3 描述可用
- **WHEN** `descUseSwagger3` 开启且 `@Operation.description` 可求值、非空白
- **THEN** `DocView.desc` 等于该属性的实际字符串值
- **AND** 不裁剪前后空格或丢失嵌入引号及转义后的内容

#### Scenario: Swagger 3 描述空白或无法求值
- **WHEN** `descUseSwagger3` 开启但 `@Operation.description` 未声明、为空、仅含空白或无法求值
- **AND** `descUseSwagger` 开启且 `@ApiOperation.notes` 可求值、非空白
- **THEN** `DocView.desc` 等于 notes 的实际字符串值

#### Scenario: Swagger 2 描述空白或无法求值
- **WHEN** 所有已启用的注解描述来源均未声明、为空、仅含空白或无法求值
- **AND** 方法存在非空的现有 Javadoc 描述
- **THEN** `DocView.desc` 使用当前 Javadoc 描述解析结果

#### Scenario: 注解描述来源关闭
- **WHEN** `descUseSwagger3` 与 `descUseSwagger` 均为 false
- **THEN** 系统忽略两个注解来源并使用现有 Javadoc 描述；没有 Javadoc 时返回空字符串
