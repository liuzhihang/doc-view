# OpenAPI 3 Annotation Compatibility

## Purpose

定义 OpenAPI 3 注解在现有 Java PSI → `DocView` 模型中的兼容映射、来源优先级、fallback 和明确未支持边界。

## Requirements

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

名称来源读取本身 SHALL 保持 `DocView` 的非名称字段、参数模型、默认 Markdown 章节与模板变量、设置持久化、上传 payload shape 和注解写回行为不变；方法描述来源按本 capability 的描述 requirement 及 java-documentation-metadata 执行；该隔离约束不撤销其他已完成 capability 对字段、模板或 classDesc 的扩展。

#### Scenario: summary 与 description 同时存在
- **WHEN** 方法声明可用的 `@Operation.summary` 和 `@Operation.description`
- **THEN** `DocView.name` 来自 summary
- **AND** `DocView.desc` 按方法描述来源顺序生成
- **AND** 默认 Spring Markdown 的章节与模板变量保持不变

### Requirement: 当前已验证范围

本 capability SHALL 保留直接声明且可求值的方法级字符串 `@Operation.summary`、`@Operation.description`、`@ApiOperation.value`、`@ApiOperation.notes` 的已定义来源优先级与 fallback，并包含字段级 `@Schema.requiredMode` 及现有 Spring 直接 `Param`/String `@RequestHeader` 上 `@Parameter` 的基础元数据范围。普通 Javadoc 标题与正文遵循 java-documentation-metadata 的当前规则，不恢复旧版 15 字符限制。MUST NOT 据此宣称组合/继承注解、完整 OAS 导入导出或尚未完成验收的 Schema 写回扩展已通过。

#### Scenario: 检查 capability 声明
- **WHEN** 本 capability 的已验证范围被引用
- **THEN** 名称和描述字符串 fallback、字段级 requiredMode 与直接参数的名称、描述、示例、required 范围同时保留，不因单一 delta 覆盖而丢失
- **AND** 方法级参数列表、组合/继承注解和完整 OAS 导入导出不在保证范围内

### Requirement: Swagger 2 名称按可用来源继续 fallback

系统 SHALL 仅在 `nameUseSwagger` 开启且 `@ApiOperation.value` 可求值、非空白时采用该名称，并 SHALL 在不可用时继续既有名称来源顺序。

#### Scenario: Swagger 2 名称常量解析
- **WHEN** enabled `@ApiOperation.value` 引用可求值的字符串常量
- **THEN** `DocView.name` 使用常量的实际字符串值
- **AND** 保留该值中的前后空格、引号字符和转义后的实际内容

#### Scenario: Swagger 2 名称空白或无法求值
- **WHEN** enabled `@ApiOperation.value` 未声明、为空、仅含空白或无法求值
- **THEN** 系统依次检查启用的配置注释标签和普通 Javadoc 标题
- **AND** 普通 Javadoc 标题遵循当前标题设置：默认仅首行、默认上限 80 个 Unicode code point，可配置范围 1–200
- **AND** 没有可用注释来源时使用 Java 方法名

#### Scenario: Swagger 2 名称来源关闭
- **WHEN** `nameUseSwagger` 为 false
- **THEN** 系统忽略 `@ApiOperation.value` 并检查下一启用且可用的名称来源

### Requirement: 方法描述按可用来源继续 fallback

系统 SHALL 按启用的 Swagger 3 `@Operation.description`、Swagger 2 `@ApiOperation.notes`、启用的 Javadoc 后续正文描述的顺序选择首个可求值且非空白的字符串；若所有启用来源均无可用值，系统 SHALL 返回空描述。

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
- **AND** Javadoc 正文描述来源开启且方法首行之后存在非空普通正文
- **THEN** `DocView.desc` 使用当前 Javadoc 描述解析结果

#### Scenario: 注解描述来源关闭
- **WHEN** `descUseSwagger3` 与 `descUseSwagger` 均为 false
- **THEN** 系统忽略两个注解来源；Javadoc 正文描述来源开启时使用方法首行之后的普通正文，否则返回空字符串

### Requirement: 直接方法参数的 Parameter 元数据范围

系统 SHALL 只将方法参数上直接声明的 `@Parameter` 用于当前已生成的 Spring 直接 `Param` 与 String `@RequestHeader`；它 SHALL 不改变参数分类、字段展开或 request-body 解析。

#### Scenario: 支持当前直接参数
- **WHEN** Spring 方法的简单参数、集合或 Map 已由现有生成路径输出为 `Param`，或 String `@RequestHeader` 已输出为 `Header`，且该参数直接声明 `@Parameter`
- **THEN** 该参数可使用本 capability 定义的名称、描述、示例和 required 规则
- **AND** 输出仍属于原有 `Param` 或 `Header` 类别

#### Scenario: 不把对象参数元数据复制到展开字段
- **WHEN** Spring 方法的对象参数按现有规则展开为 DTO 字段
- **AND** 对象参数本身直接声明 `@Parameter`
- **THEN** 系统不将该参数级注解复制到任何展开字段
- **AND** 字段自身的必填规则继续由字段级来源决定

### Requirement: Spring 绑定名优先于 OpenAPI 文档名

对受支持的直接 Spring 参数，系统 SHALL 以非空 `@RequestParam`、`@PathVariable` 或 `@RequestHeader` 的显式 `name`/`value` 作为输出名称；只有没有非空 Spring 绑定名时，才使用非空 `@Parameter.name`，否则保留当前 Java 参数名。

#### Scenario: RequestParam/PathVariable 显式 name/value 与 Parameter.name 冲突
- **WHEN** `@RequestParam` 或 `@PathVariable` 的非空 Spring `name`/`value` alias 与 `@Parameter.name` 不同
- **THEN** `Param.name` 等于 Spring 绑定名
- **AND** 请求参数示例继续使用该 `Param.name`

#### Scenario: RequestHeader 显式 name/value 与 Parameter.name 冲突
- **WHEN** String `@RequestHeader` 的非空 Spring `name`/`value` alias 与 `@Parameter.name` 不同
- **THEN** `Header.name` 等于 Spring 绑定名

#### Scenario: Spring 未显式命名时使用 Parameter.name
- **WHEN** 受支持的方法参数没有非空 Spring `name`/`value`，且 `@Parameter.name` 非空
- **THEN** `Param.name` 或 `Header.name` 等于 `@Parameter.name`

#### Scenario: Spring name/value alias 冲突
- **WHEN** 同一个受支持的 Spring 绑定注解同时声明互相冲突的非空 `name` 与 `value`
- **THEN** 系统将该注解视为格式错误，回退到 Java 参数名
- **AND** 不使用 `@Parameter.name` 覆盖该冲突

#### Scenario: 注解名为空时保留 Java 参数名
- **WHEN** Spring 绑定名和 `@Parameter.name` 均未声明或为空
- **THEN** 系统使用当前 Java 参数名

### Requirement: Parameter 描述覆盖当前参数描述

系统 SHALL 将非空 `@Parameter.description` 写入当前直接 `Param` 或 `Header` 的描述；该属性不可用时 SHALL 保留相应类别的现有描述 fallback。

#### Scenario: 非空 Parameter 描述
- **WHEN** 直接 `Param` 同时有 Javadoc `@param` 描述和非空 `@Parameter.description`
- **THEN** 输出描述等于 `@Parameter.description`

#### Scenario: Parameter 描述为空
- **WHEN** `@Parameter.description` 未声明、为空或仅含空白，且存在 Javadoc `@param` 描述
- **THEN** 输出描述继续等于现有 Javadoc 描述

#### Scenario: 请求头描述为空
- **WHEN** String `@RequestHeader` 参数的 `@Parameter.description` 未声明、为空或仅含空白
- **THEN** 输出保留当前请求头的空描述

### Requirement: Parameter 示例进入现有参数示例字段

系统 SHALL 将非空 `@Parameter.example` 写入当前直接 `Param.example` 或 `Header.value`；该属性未声明、为空或仅含空白时 SHALL 保留当前生成的示例值。

#### Scenario: 普通参数示例
- **WHEN** 直接 `Param` 声明非空 `@Parameter.example`
- **THEN** 对应 Markdown 参数值和请求参数示例都使用该示例

#### Scenario: 请求头示例
- **WHEN** String `@RequestHeader` 参数声明非空 `@Parameter.example`
- **THEN** 对应 Markdown 请求头参数值显示该示例

### Requirement: Parameter.required 仅在 Spring 绑定与配置没有结论时生效

系统 SHALL 忽略未显式声明的 `@Parameter.required` 默认值；直接 Spring resolver 的有效必填规则和 Doc View 配置的 required 标记优先于显式 `@Parameter.required`。配置的 required 标记对当前支持的直接 `Param` 与 String `@RequestHeader` 生成的 `Header` 路径一致生效。

#### Scenario: 无显式 Spring binding 参数的必填元数据
- **WHEN** 当前输出为直接 `Param`，没有配置的 required 标记或受支持 Spring binding 注解，且 `@Parameter` 显式声明 `required = true` 或 `required = false`
- **THEN** 对应输出必填值分别为 true 或 false

#### Scenario: Spring RequestParam 和 RequestHeader 的 required
- **WHEN** 直接参数属于 `@RequestParam` 或 `@RequestHeader`
- **THEN** `defaultValue` 非 Spring 的未设置哨兵时有效必填值为 false
- **AND** 否则显式 `required` 值优先，未显式声明时按 Spring 默认值 true
- **AND** 冲突的 `@Parameter.required` 不改变该结果

#### Scenario: Spring PathVariable 的 required
- **WHEN** 直接 `Param` 属于 `@PathVariable`
- **THEN** 显式 `required` 值优先，未显式声明时按 Spring 默认值 true
- **AND** 冲突的 `@Parameter.required` 不改变该结果

#### Scenario: 配置的 required 标记优先
- **WHEN** 当前输出是直接 `Param` 或由 String `@RequestHeader` 生成的 `Header`，其源码参数带有配置指定的 required 验证标记，且同时声明 Spring 或 `@Parameter.required = false`
- **THEN** 对应输出必填值为 true

#### Scenario: 无 Parameter 时 Spring 绑定修复仍生效
- **WHEN** 受支持的直接 `Param` 或 String `@RequestHeader` 没有直接声明 `@Parameter`，且 Spring 绑定名或有效 required 与原有显示值不同
- **THEN** 输出仍按本 requirement 定义的 Spring 名称与 required 规则生成
- **AND** Spring 绑定修复不依赖 `@Parameter` 是否存在

### Requirement: 不支持的 Parameter 结构不改变当前输出

系统 MUST 忽略本 capability 范围外的 OpenAPI 参数结构，并 MUST 保持 Spring request body 与 DTO 字段的现有解析入口。

#### Scenario: request-body 与参数注解分离
- **WHEN** Java 参数由 Spring `@RequestBody` 识别为请求体，即使该参数还声明 `@Parameter`
- **THEN** 系统不据此修改请求体描述、required、schema 或 `Param`/`Header` 列表

#### Scenario: 独立 OpenAPI RequestBody 注解
- **WHEN** 方法或 Java 参数声明 Swagger `@RequestBody`
- **THEN** 本 capability 不把其 description、required 或 content 映射到 Doc View

#### Scenario: 方法级与容器参数注解
- **WHEN** `@Parameter`/`@Parameters` 仅声明在方法级，`@Parameters` 容器声明在方法参数上，或 `@Operation.parameters` 仅声明在方法级
- **THEN** 系统不创建未绑定 Java 参数的输出条目

#### Scenario: Parameter 的其他成员
- **WHEN** `@Parameter` 声明 `in`、`schema`、`style`、`hidden` 或 `ref`
- **THEN** 系统不据此改变参数位置、类型、可见性或条目数量

#### Scenario: 复合示例成员优先于单个 example
- **WHEN** `@Parameter.example` 与非空 `examples`、`content` 或 `array` 同时声明
- **THEN** 系统不将单个 `example` 映射到参数示例字段
- **AND** 系统不解析或导入其他复合示例内容

### Requirement: 参数映射保持其他输出契约

系统 SHALL 只按本 change 定义的 Spring 绑定与 `@Parameter` 规则更改已有 `Param`/`Header` 字段，并 SHALL 保持方法路径、HTTP 方法、请求体结构、Markdown 章节顺序、模板变量与平台上传 payload shape 不变。

#### Scenario: 注解缺少元数据且 Spring 绑定结果不变
- **WHEN** Spring 方法参数没有直接声明本 capability 支持的 `@Parameter` 属性，且本 change 解析出的 Spring 绑定名和有效 required 与当前显示值一致
- **THEN** 现有 `DocView` 参数和 Markdown 输出保持不变

#### Scenario: Spring 绑定修复不改变其他输出
- **WHEN** 受支持的 Spring 绑定名或有效 required 与当前显示值不同，无论是否声明 `@Parameter`
- **THEN** 对应 `Param`/`Header` 字段依照 Spring 绑定规则更新
- **AND** 除现有请求参数示例中的对应名称或示例值外，其他章节、请求体、模板变量和平台 payload 结构保持不变

#### Scenario: 映射元数据
- **WHEN** 受支持的直接参数声明本 capability 支持的元数据
- **THEN** 仅其 `Param`/`Header` 名称、描述、示例或 required 值依照上述规则变化
- **AND** `Param` 名称与示例仅更新现有请求参数示例中的对应值
- **AND** 默认 Markdown 章节与平台 payload 结构保持不变
