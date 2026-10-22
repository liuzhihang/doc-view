# Design

## Context

动机见 [proposal.md](proposal.md)，可观察行为见 [spec delta](specs/openapi3-annotation-compatibility/spec.md)。目前 `DocViewUtils.isRequired(PsiField)`（`src/main/java/com/liuzhihang/doc/view/utils/DocViewUtils.java`）按项目配置的字段验证注解、`@Schema(required = true)`、Swagger 2 `@ApiModelProperty(required = true)`、启用时的 Doc View 文档 tag 判断；Swagger 3 模式属性未参与判断。`ParamPsiUtils.buildBodyParam` 与 `SpringPsiUtils.buildPramFromField` 会把字段结果放进现有 `Body.required` / `Param.required`。`writeComment` 处理已有 `@Schema` 时重建整条注解并写 deprecated `required = true`，会丢失未编辑 Schema 属性，描述也通过字符串拼接形成 Java 注解文本。

项目不依赖 swagger-annotations 运行时库；`SwaggerConstant` 使用全限定名，测试通过 Java PSI fixture 的注解桩验证。Swagger 官方 `Schema` 注解说明 `required` 自 2.2.5 起弃用，`requiredMode.AUTO` 由其解析器按启发式处理，`REQUIRED` 与 `NOT_REQUIRED` 分别强制必填/非必填；官方 `ModelResolver` 也对 `NOT_REQUIRED` 先行短路，并对 `REQUIRED` 显式加入 required 集合。本 change 采用这些三态的外部语义，但 `AUTO` 沿用 Doc View 当前可配置规则，而不复制 swagger-core 算法。

## Goals / Non-Goals

**Goals:**

- 为字段上直接声明的 `@Schema.requiredMode` 定义 `REQUIRED`、`NOT_REQUIRED` 与 `AUTO` 的行为及优先级。
- 让字段必填结果沿用 `Body` / `Param` 进入现有 Markdown 表格，并保持未受影响的文档内容稳定。
- 编辑已有 `@Schema` 时写入新的 `requiredMode`，保留其他未编辑属性并安全编码描述。
- 明确不再解析或生成 deprecated `@Schema.required`，也不自动迁移未编辑的源码。

**Non-Goals:**

- 不实现方法参数上的 OpenAPI 3 `@Parameter`；它由路线图第 9 项独立定义。
- 不实现 `@ArraySchema` 项、`requiredProperties`、组合 Schema、继承/组合注解或完整 OAS 导入导出。
- 不复刻 swagger-core 对 `Optional`、基本类型或所有 nullability 标记的自动判定，不增加 Swagger 运行时依赖或设置项。
- 不扫描、批量修改或迁移项目中的旧 `@Schema(required = ...)`。

## Decisions

### 1. 显式 requiredMode 先于所有 AUTO 来源

在 `DocViewUtils.isRequired(PsiField)` 的同一 PSI read action 中读取字段的直接 `@Schema.requiredMode`。可解析的 `REQUIRED` 立即返回 true，`NOT_REQUIRED` 立即返回 false；缺省、显式 `AUTO` 或无法解析的枚举值落入 Doc View AUTO 规则。这样 `NOT_REQUIRED` 能覆盖配置的 `@NotNull`/`@NotBlank` 等注解和 Doc View 必填 tag，也能覆盖字段同时存在的 Swagger 2 正向必填标记。

AUTO 保持当前 Doc View 的字段规则：用户配置的 required-field annotation → `@ApiModelProperty(required = true)` → 设置开启时的 `@DocView.Required` tag → false。项目默认验证列表包括 Validation 的 `@NotBlank`、`@NotEmpty`、`@NotNull` 与 Lombok `@NonNull`，comment tag 默认开启。Swagger 2 注解作为既有独立来源保留；这不是读取 deprecated `Schema.required` 的 fallback。解析只面向字段本身，不将字段属性规则外推至方法参数。

不采用反射构造 swagger-core `ModelResolver`：仓库没有 Swagger 运行时依赖，完整 resolver 会扩大 PSI 语义、依赖与平台范围。也不将 AUTO 解释成 swagger-core 全量行为；例如项目没有配置相关验证 annotation 时，`Optional` 或 primitive 类型不会因此自动改为必填。

### 2. deprecated Schema.required 不读取、不写入

字段出现 `@Schema(required = true)` 时，新逻辑忽略该属性；缺省/AUTO 时仍只看 Doc View AUTO 来源。字段写回使用 `requiredMode = Schema.RequiredMode.REQUIRED` 或 `NOT_REQUIRED`，不再输出 deprecated `required`。不对未编辑源码做批量迁移，也不提供兼容开关。

写回依赖项目可解析的 Schema 注解类型包含 `requiredMode` 成员（swagger-annotations 2.2.5 起提供）。若成员不存在，写回不得合成导致项目源码无法编译的参数，也不得退回到 `required = true`；`DocViewUtils.writeComment` 对该字段抛出带依赖版本提示的 `DocViewException`，`ParamDocEditorForm` 的 Confirm 动作捕获此预期错误并调用 `DocViewNotification.notifyError`。失败字段的 `@Schema` 和源码保持原样，编辑弹窗保留当前内容；成功写回后才关闭弹窗。不捕获 `Throwable`，其他程序错误继续按平台行为暴露。该限制不影响普通 DocView AUTO 读取。

用户若手工编辑或通过字段编辑器再次确认一个带有 deprecated `required` 的字段，写回只规范化这条被编辑的 `@Schema`：移除旧 `required` 成员，设置当前 `requiredMode`，不触碰其他字段。未操作的源码保持原样。

### 3. 保留 DocView DTO 与模板结构

不改 `Body`、`Param`、Velocity 变量或 Markdown 章节。请求/响应 DTO 字段继续由 `ParamPsiUtils` 生成 `Body.required`；Spring 扁平表单字段继续由 `SpringPsiUtils` 生成 `Param.required`。模板使用既有必填显示规则，字段的其他元数据和层级不变。对没有 `requiredMode` 输入的字段，除停止读取 `Schema.required` 外，其余结果继续按现存规则生成。

不把本次讨论扩展到索引格式、AI 检索、参数元数据或自定义 IDE 索引；那些能力仍按 roadmap 独立排期。

### 4. 通过 PSI 修改目标 Schema 成员

已有 `@Schema` 的编辑路径只更新 `description` 和 `requiredMode`，保留其余有效成员（如 `name`、`title`、`example`、`implementation`）。更新时删除 deprecated `required`，避免它与新状态并存。description 必须构造成 PSI 字符串字面量，不通过未经转义的字符串拼接；测试覆盖双引号、反斜杠和换行，并确认重解析后的注解合法、内容准确。Swagger 2 `@ApiModelProperty` 与普通 Javadoc 写回路径保持当前行为。

考虑过继续替换整个 `@Schema` 文本的方式；它代码较短，但会抹除用户没编辑的 Schema 属性并容易生成不合法的 Java 字符串，因此选用 PSI 成员级更新。

### 5. 以 Java 字符串语义读取 Schema.description

参数表格显示已有字段描述时，字段级 OpenAPI 3 Schema.description 通过 IntelliJ Java PSI 的 JavaPsiFacade.getConstantEvaluationHelper().computeConstantExpression 求值，支持普通字符串字面量以及 PSI 可直接计算的编译期字符串拼接。只对显式声明的 `description` 求值；若它得到任意 Java 字符串值（包括空串和纯空白），该值优先于较低优先级的描述来源。仅未声明、无法求值或结果不是字符串时才沿用既有描述来源顺序。

用户只切换必填状态时，编辑器传回该字符串值。若提交值等于当前可求值的 Schema.description，写回可以规范化源码表达式，但生成的 Java 字符串字面量必须保留相同 Java 字符串值；编辑器提交的非 null Java 字符串也始终以安全转义的合法 Java 字符串字面量写回，包括空串或纯空白。这样当编辑器将低优先级 Javadoc 描述读入后，用户清空或改成空白并确认时，显式的 Schema.description 会抑制后续读取回退到旧 Javadoc。本决定不新增 null 描述的写回行为。引号、反斜杠和 LF 不会显示成源代码转义片段，也不会在再次写回时重复转义。此决定仅限字段级 OpenAPI 3 Schema.description；Swagger 2 和其他注解字段的读取行为不在本次范围内。

### 6. 对缺少 requiredMode 的字段编辑批次先做能力预检

`writeComment` 在修改任何字段源码前，先检查本次提交中的每个直接声明 `@Schema` 字段，其解析到的注解类型是否包含 `requiredMode`。只要任一字段不支持该成员，就抛出既有版本提示；此特定拒绝路径不得先写入同批次中的普通 Javadoc 字段或其他字段。该保证仅覆盖 requiredMode 能力预检失败，不承诺任意 PSI、文件写入或其他运行时异常下的整批事务原子性，也不改变 Swagger 2 写回路径。

## Risks / Trade-offs

- [旧源码只用 `@Schema(required = true)` 的字段生成结果会变化] → 这是明确的新版本边界；用户需改为 `requiredMode = REQUIRED`。不自动修改未编辑源码或保留旧 fallback。
- [AUTO 与 swagger-core 结果不完全相同] → 文档和 spec 明示 Doc View 的当前配置型规则及未支持类型启发式，避免把映射描述为完整 OpenAPI schema 解析。
- [项目 Swagger 注解版本不含 `requiredMode`] → 不生成无法编译的注解，给出可理解的失败提示；不借旧成员回退写入。
- [注解重写丢失属性或描述转义错误] → PSI 成员级更新，并用源码重读断言保留未修改成员及复杂描述。
- [空/空白 Schema.description 被当作缺省或能力错误导致部分写回] → 只读取显式声明值并按字符串语义保留空/空白；确认编辑时把编辑器字符串值（包括空/空白）写回为显式成员，防止旧 Javadoc 重新出现；写回前对本批次全部 Schema 字段做 requiredMode 能力预检。该预检不承诺其他错误的事务原子性。
- [必填变化修改生成 Markdown] → 用 DTO 和字段表格断言具体 required 值，再更新专用 golden；同时运行既有 Spring Markdown contract 证明其他场景未变。

## Migration Plan

没有插件设置或持久化数据迁移。使用 `@Schema(required = ...)` 的源码需由用户改写成 `requiredMode`；本 change 不批量改源码。通过字段编辑器明确保存的 `@Schema` 字段会在该次编辑中写入所选 mode 并移除该字段上的 deprecated `required`。

## Validation

主要验证位于 `OpenApi3SchemaRequiredModeTest`：使用 IntelliJ light Java fixture、真实 PSI 注解与生产字段解析，检查三种模式、来源冲突、旧 `required` 被忽略、字段写回保留与转义。追加显式空串/空白且与 Javadoc 竞争的 mode-only 往返场景、用户清空 Javadoc 回退值或改为空白后仍精确读回的写回场景，以及按 `LinkedHashMap` 排列“普通字段先写、缺少 requiredMode 的 Schema 字段后失败”的批次预检场景。批次场景只证明特定能力错误发生前没有任何源码写入，不作为任意异常整批事务保证。构建 Spring request/response DTO 与扁平字段参数，断言 `DocView` required 状态并对完整 Markdown 输出使用专用 golden；再运行既有 `SpringMarkdownContractTest`。缺少 `requiredMode` 时测试断言写回异常提示版本且字段源码未变；实现中的 Confirm 窄错误分支负责显示通知并保留弹窗，不捕获通用异常掩盖失败。测试 stub 仅声明项目源码所需注解形状，不引入 Swagger dependency。

## 外部依据

- [swagger-core `Schema.java`](https://github.com/swagger-api/swagger-core/blob/master/modules/swagger-annotations/src/main/java/io/swagger/v3/oas/annotations/media/Schema.java)：deprecated `required` 的替代项、三种 mode 的官方注释与 Since 版本。
- [swagger-core `ModelResolver.java`](https://github.com/swagger-api/swagger-core/blob/master/modules/swagger-core/src/main/java/io/swagger/v3/core/jackson/ModelResolver.java)：required 集合处理、`NOT_REQUIRED` 覆盖与 `REQUIRED` 显式加入。
- [Swagger OpenAPI 3 支持的 Schema 关键字](https://swagger.io/docs/specification/v3_0/data-models/keywords/)：`required` 属于 OpenAPI Schema 支持关键字。
