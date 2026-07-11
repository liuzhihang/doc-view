# OpenAPI 3 Annotation Compatibility

## Purpose

定义 OpenAPI 3 注解在现有 Java PSI → `DocView` 模型中的兼容映射、来源优先级、fallback 和明确未支持边界。

## Requirements

### Requirement: Operation summary 作为接口名称

当 Swagger 3 名称来源开启时，系统 SHALL 将方法上直接声明的非空 `@Operation.summary` 映射为 `DocView.name`，并 SHALL 在其他名称来源之前使用该值。

#### Scenario: summary 非空
- **WHEN** Spring 方法直接声明 `@Operation(summary = "查询用户")`
- **AND** 项目设置 `nameUseSwagger3` 为 true
- **THEN** 生产 Spring 文档服务构建的 `DocView.name` 等于 `查询用户`

#### Scenario: summary 优先于 Swagger 2
- **WHEN** 同一方法声明非空 `@Operation.summary` 和非空 `@ApiOperation.value`
- **AND** Swagger 3 与 Swagger 2 名称来源均开启
- **THEN** `DocView.name` 等于 `@Operation.summary`

### Requirement: 空 summary 保持名称 fallback

系统 MUST 将未声明、空字符串或仅空白的 `@Operation.summary` 视为不可用，并 SHALL 继续执行既有名称 fallback，不得返回空接口名称。

#### Scenario: summary 仅空白
- **WHEN** 方法声明 `@Operation(summary = "   ")` 和 `@ApiOperation(value = "Swagger 2 名称")`
- **THEN** `DocView.name` 等于 `Swagger 2 名称`

#### Scenario: summary 未声明
- **WHEN** 方法声明使用默认 summary 的 `@Operation`，且存在非空的后续名称来源
- **THEN** 系统使用该后续来源

### Requirement: Swagger 3 名称设置开关

系统 SHALL 只在 `nameUseSwagger3` 为 true 时读取 `@Operation.summary`。

#### Scenario: Swagger 3 名称来源关闭
- **WHEN** 方法具有非空 `@Operation.summary` 和非空 `@ApiOperation.value`
- **AND** `nameUseSwagger3` 为 false、Swagger 2 名称来源为 true
- **THEN** `DocView.name` 等于 `@ApiOperation.value`

### Requirement: 名称修复不改变其他文档契约

系统 SHALL 保持 `@Operation.description` 的既有解析、`DocView`/参数模型、默认 Markdown 章节与模板变量、设置持久化、上传 payload shape 和注解写回行为不变。

#### Scenario: summary 与 description 同时存在
- **WHEN** 方法声明非空 `@Operation.summary` 和 `@Operation.description`
- **THEN** `DocView.name` 来自 summary
- **AND** `DocView.desc` 继续等于 description
- **AND** 既有 Spring Markdown golden 契约测试仍通过

### Requirement: 当前已验证范围

本 capability 当前 SHALL 只保证方法上直接声明、使用普通字符串属性的 `@Operation.summary`；不得把其他 OpenAPI 注解或完整 OAS 能力描述为已支持。

#### Scenario: 检查 capability 声明
- **WHEN** Operation summary 契约通过
- **THEN** `@Schema.requiredMode`、参数 `@Parameter`、组合/继承注解、注解写回和完整 OAS 导入导出仍不属于本 capability 的已验证范围
