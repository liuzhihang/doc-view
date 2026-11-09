# 设计

## 背景

`SpringDocViewServiceImpl` 通过 `SpringPsiUtils.buildFormParam()` 生成 `DocView.reqParamList`，并通过 `buildHeader()` 生成请求头。当前直接生成的 `Param` 使用 Java 参数名、Doc View 配置的必填标记与 `@RequestParam.required`、Java 类型和 Javadoc `@param` 描述。String 类型的 `@RequestHeader` 生成 `Header`，目前使用 Java 参数名并将 required 固定为 `true`。`Param.example` 同时影响 Markdown 参数值和现有请求参数示例；`Header.value` 影响 Markdown 请求头值。Spring `@RequestBody` 使用独立的数据路径。

已有 OpenAPI 3 capability 为 `openapi3-annotation-compatibility`。前置的 `openapi3-schema-required-mode` change 负责字段级 `@Schema.requiredMode`；本 change 只处理方法参数上的 `@Parameter`，必须在前置 change 的实现与测试通过后再实施。

## 目标与非目标

目标：

- 将方法参数上直接声明的 `@Parameter.name`、`description`、`example`、`required` 映射到已有 Spring `Param`/`Header` 输出，并定义空值回退与冲突优先级。
- 让直接 `@RequestParam`、`@RequestHeader` 和 `@PathVariable` 的必填值遵循其 Spring resolver 语义，包括这三类注解的默认必填值及 `defaultValue` 对 `RequestParam`/`RequestHeader` 的影响。
- 通过真实 Java PSI fixture、`DocView` 断言和增量 Markdown golden 验证映射结果。

非目标：

- 导入完整 OpenAPI 文档，或建模参数位置、数组、schema、媒体类型、安全、响应与引用。
- 读取方法级 `@Parameter`、`@Parameters`、`@Operation.parameters`、Swagger `@RequestBody`、字段级 `@Parameter`、组合/继承注解或参数级 `@Schema`。
- 改变对象参数扁平化、DTO 字段规则、`Optional` 解析、未列出的 Spring resolver、Dubbo 行为、设置或上传 payload。
- 增加 Swagger 运行时依赖，或为旧注解写法提供适配。

## 设计决策

### 仅读取当前支持路径上的直接方法参数注解

使用 PSI 的全限定注解名，仅在 `PsiParameter` 上直接存在 `@Parameter` 时处理。必须等当前 Spring 生成路径确认该参数会输出为直接 `Param` 或 String `@RequestHeader` 的 `Header` 后才读取元数据。不扩展现有参数分类：目前会扁平化为 DTO 字段的对象参数，其参数级注解不得传递给子字段；Spring `@RequestBody` 仍走现有 body 路径。

Swagger 的 `@Parameter` 同时可声明在方法级别，但本 change 只支持绑定到 Java 参数的直接注解。Swagger 对 request body 使用独立的 `@RequestBody` 注解；Doc View 本 change 不解析它。参见 [Swagger `@Parameter` Javadoc](https://docs.swagger.io/swagger-core/v2.2.27/apidocs/io/swagger/v3/oas/annotations/Parameter.html) 与 [Swagger `@RequestBody` Javadoc](https://docs.swagger.io/swagger-core/v2.2.27/apidocs/io/swagger/v3/oas/annotations/parameters/RequestBody.html)。

### Spring 绑定名优先于文档名

对直接 `@RequestParam`、`@PathVariable` 和 `@RequestHeader`，先读取非空的 Spring `name` 或其 `value` alias；只有 Spring 未提供非空绑定名时，才使用非空 `@Parameter.name`；两者都没有时保留 Java 参数名。Spring 将 `name` 与 `value` 定义为 alias；如果源码同时给出互相冲突的非空值，将该注解视为格式错误并回退到 Java 参数名，不使用 `@Parameter.name` 遮盖冲突。接受的非空文本按原样复制，仅用空白检查判断是否为空。

Spring 对别名和绑定行为的说明见 [RequestParam](https://docs.spring.io/spring-framework/docs/6.2.19/javadoc-api/org/springframework/web/bind/annotation/RequestParam.html)、[RequestHeader](https://docs.spring.io/spring-framework/docs/6.2.19/javadoc-api/org/springframework/web/bind/annotation/RequestHeader.html) 与 [PathVariable](https://docs.spring.io/spring-framework/docs/6.1.7/javadoc-api/org/springframework/web/bind/annotation/PathVariable.html)。本 change 不增加参数位置建模，因此 PathVariable 仍输出为现有 `Param`。

### 描述与示例使用非空覆盖

非空 `@Parameter.description` 覆盖直接 `Param` 当前的 Javadoc `@param` 描述；缺失、空或仅空白时保留 Javadoc 回退。对 `Header`，非空 description 填充当前描述，空值则保留其当前空描述。非空 `@Parameter.example` 写入 `Param.example` 或 `Header.value`；空值保留当前示例。接受的字符串不裁剪。

现有请求参数示例由 `Param.name` 和 `Param.example` 生成，因此映射这两个字段时同步更新其中对应的值，但保持示例格式不变。`examples`、`content`、`array` 等复合 OpenAPI 结构不在范围内；仅在单值 `example` 可用时映射它。

### 必填值按配置、Spring resolver、显式 Parameter 的顺序决定

对于当前支持的直接方法参数，按以下顺序决定输出的 required。Doc View 当前配置的 required 验证标记对直接 `Param` 与 String `@RequestHeader` 生成的 `Header` 一致生效；Content-Type 这类非源码参数不在本规则范围内。

1. 当前 Doc View 配置指定的 required 验证标记仍是正向覆盖，结果为 `true`，包括 Spring `@RequestHeader(required = false)` 或该注解提供 `defaultValue` 的情况。
2. 支持的 Spring 绑定注解决定其 resolver 语义。`@RequestParam` 和 `@RequestHeader` 的 `defaultValue` 若不同于 Spring 未设置哨兵值，结果为 `false`；否则使用显式声明的 `required`，未显式声明时采用 Spring 默认 `true`。`@PathVariable` 使用显式 `required`，未显式声明时采用 Spring 默认 `true`。
3. 没有配置或上述 Spring 来源可决定时，只在 `@Parameter.required` 显式声明的情况下使用它。注解默认值 `false` 不视为用户指令。
4. 以上均无结论时保留当前 required 回退。

这样会修正直接 PathVariable 当前可能错误显示为非必填，以及 String RequestHeader 当前被固定为必填的问题，并处理 Spring 对 RequestParam/RequestHeader `defaultValue` 的规则。不扩展 `Optional`、任意 argument resolver 或 DTO 字段必填解析。Spring 对默认值规则见 [RequestParam](https://docs.spring.io/spring-framework/docs/6.2.19/javadoc-api/org/springframework/web/bind/annotation/RequestParam.html) 与 [RequestHeader](https://docs.spring.io/spring-framework/docs/6.2.19/javadoc-api/org/springframework/web/bind/annotation/RequestHeader.html)；[方法参数参考](https://docs.spring.io/spring-framework/reference/6.2/web/webmvc/mvc-controller/ann-methods/arguments.html) 单独说明的 `Optional` 仍不在本 change 范围内。

### 只让未受影响的既有 golden 保持逐字不变

仅写入现有 `Param`/`Header` 字段，不新增 location 字段、模板变量、Markdown 章节、配置项或 payload 属性。生产和测试通过 PSI 注解全限定名与源码值工作；测试使用小型注解 stub，不需要 Swagger 新依赖。

既有 golden 仅在本 change 解析出的 Spring 绑定名与有效 required 和当前输出相同的 fixture 中逐字保持不变。对于没有 `@Parameter` 但 Spring 显式绑定名、显式 required 或 `defaultValue` 会改变显示值的 fixture，Spring 绑定修复仍独立生效；相应测试需断言仅相关的 `Param`/`Header` 值和现有请求参数示例变化，其他结构不变。

## 风险与取舍

- Spring `name`/`value` alias 或默认值读取错误：仅解析直接声明的属性，并分别覆盖三种绑定注解的 `name`、`value`、冲突和默认场景。
- 将 Swagger `required` 默认 `false` 误认为用户选择：区分属性是否显式声明，并在 PSI fixture 中覆盖未声明与显式值。
- 字段级 Schema change 与参数级映射重叠：等待 `openapi3-schema-required-mode` 实现和测试通过；本 change 不改其 `PsiField` 路径。
- 修改参数值可能影响下游格式：断言只有预期 `Param`/`Header` 字段变化，保持章节及 payload 结构；对 Spring 绑定结果不变的无注解 fixture 保留 golden。
