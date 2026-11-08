# Spec Delta

## MODIFIED Requirements

### Requirement: 名称修复不改变其他文档契约

`@Operation.summary` 的读取 SHALL 只改变 `DocView.name`；它 SHALL 保持 `@Operation.description` 的解析、默认 Markdown 章节与模板变量、设置持久化和上传 payload shape 不变。字段级 `@Schema` 必填模式与源码写回由本 capability 中独立 requirement 定义。

#### Scenario: summary 与 description 同时存在
- **WHEN** 方法声明非空 `@Operation.summary` 和 `@Operation.description`
- **THEN** `DocView.name` 来自 summary
- **AND** `DocView.desc` 继续等于 description
- **AND** 既有 Spring Markdown golden 契约测试仍通过

### Requirement: 当前已验证范围

本 capability SHALL 保证方法上直接声明的普通字符串 `@Operation.summary`，以及字段上直接声明的 `@Schema.requiredMode` 对 `PsiField` 必填状态的映射；不得据此宣称支持参数 `@Parameter`、组合/继承注解或完整 OAS 导入导出。

#### Scenario: 检查 capability 声明
- **WHEN** 本 capability 的已验证范围被引用
- **THEN** 它包含直接字段 `@Schema.requiredMode` 和对应的字段注解写回
- **AND** 它不包含方法参数 `@Parameter` 元数据或完整 OAS 导入导出

## ADDED Requirements

### Requirement: 字段级 Schema.requiredMode 控制必填状态

对于字段上直接声明的 `@Schema`，系统 SHALL 让 `requiredMode` 的显式值优先于 Doc View 的其他字段必填来源。

#### Scenario: REQUIRED 覆盖自动来源
- **WHEN** 字段声明 `@Schema(requiredMode = REQUIRED)`，同时存在或不存在验证注解及文档必填 tag
- **THEN** 该字段在 `Body.required` 或 `Param.required` 中为 true

#### Scenario: NOT_REQUIRED 覆盖正向来源
- **WHEN** 字段声明 `@Schema(requiredMode = NOT_REQUIRED)`，同时具有 Doc View 配置的验证注解、Swagger 2 `@ApiModelProperty(required = true)` 或启用的 `@DocView.Required` tag
- **THEN** 该字段在 `Body.required` 或 `Param.required` 中为 false

#### Scenario: AUTO 或省略模式使用 Doc View 既有推断
- **WHEN** 字段的 `requiredMode` 为 `AUTO` 或未声明
- **THEN** 系统按已配置的字段验证注解、Swagger 2 `@ApiModelProperty(required = true)`、启用时的 `@DocView.Required` tag 顺序判断
- **AND** 没有任何正向来源时结果为 false

#### Scenario: requiredMode 枚举值无法解析时按 AUTO 处理
- **WHEN** 字段的 `requiredMode` 引用无法解析为 `Schema.RequiredMode` 的枚举常量
- **THEN** 系统按 AUTO 规则判断字段必填状态

#### Scenario: 不读取 deprecated Schema.required
- **WHEN** 字段只声明 `@Schema(required = true)`，且没有其他 Doc View 正向必填来源
- **THEN** 系统将该字段判为非必填
- **AND** `@Schema(required = false)` 不改变 AUTO 推断

### Requirement: AUTO 不扩大为 swagger-core 全量启发式

`requiredMode = AUTO` SHALL 使用 Doc View 当前字段来源与项目设置，而不得宣称完整实现 swagger-core 的自动必填判定算法。

#### Scenario: AUTO 不采用未实现的类型启发式
- **WHEN** 一个字段只有 Optional、基本类型或空值性语义，且没有 Doc View 配置的验证注解、Swagger 2 正向注解或启用的必填 tag
- **THEN** 本 requirement 不因该类型或空值性单独将其判为必填

### Requirement: 字段必填状态进入现有文档输出

系统 SHALL 将字段级 requiredMode 解析结果写入当前 `Body.required` 或 `Param.required`，并由既有 Markdown 模板呈现；其他字段、章节顺序、模板变量和上传 payload shape SHALL 保持不变。

#### Scenario: 请求或响应 DTO 字段
- **WHEN** 生成文档递归展开的 DTO 字段声明 `REQUIRED` 或 `NOT_REQUIRED`
- **THEN** 对应字段的 required 显示值符合模式结果
- **AND** 其类型、名称、描述、示例与层级不因 requiredMode 改变

#### Scenario: Spring 扁平字段参数
- **WHEN** Spring 表单参数展开到某个字段且该字段具有 `@Schema.requiredMode`
- **THEN** 对应 `Param.required` 符合该模式

### Requirement: 编辑已有 Schema 时写回当前 requiredMode

字段描述 SHALL 按 Java 字符串语义读取。该读取规则仅适用于字段级 OpenAPI 3 Schema.description，不改变 Swagger 2 或其他注解字段的既有读取行为。

#### Scenario: 仅切换必填状态保留原始描述值
- **WHEN** 已有 Schema.description 使用带转义字符的字符串字面量，或 IntelliJ Java PSI 可直接计算的编译期字符串拼接表达式，且用户只切换字段必填状态
- **THEN** 参数编辑器显示 description 的 Java 字符串值，而不是源码转义文本
- **AND** 确认写回后 description 的 Java 字符串值与编辑前完全一致
- **AND** 引号、反斜杠和换行不会因读取与写回而丢失或重复转义

#### Scenario: 显式空描述或空白描述优先于低优先级来源
- **WHEN** 字段显式声明的 Schema.description 可由 PSI 求值为 `""` 或纯空白，且存在 Javadoc 等较低优先级描述来源，用户只切换必填状态
- **THEN** 参数编辑器使用 Schema.description 的精确字符串值，不回退到较低优先级描述
- **AND** 确认写回后 Schema.description 的 Java 字符串值仍完全一致

#### Scenario: 清空或改为空白后不恢复旧描述
- **WHEN** 字段原先具有非空 Schema.description，或编辑器显示 Javadoc 回退描述，用户将描述清空或改为纯空白后确认
- **THEN** 系统将编辑器中的精确 Java 字符串值显式写入 Schema.description
- **AND** 再次读取时返回该空串或纯空白值，不回退到 Javadoc

确认字段编辑时，系统 SHALL 对项目中声明了 `Schema.requiredMode` 的字段用 `REQUIRED` 或 `NOT_REQUIRED` 写回当前必填结果，不得生成 deprecated `required` 属性。

#### Scenario: 写回 required 字段
- **WHEN** 用户确认一个已有 `@Schema` 字段且该字段编辑结果为必填
- **THEN** 源码包含 `requiredMode = Schema.RequiredMode.REQUIRED`（或等价可解析的枚举常量）
- **AND** 本次写回不生成 `required = true`

#### Scenario: 写回 optional 字段
- **WHEN** 用户确认一个已有 `@Schema` 字段且该字段编辑结果为非必填
- **THEN** 源码包含 `requiredMode = Schema.RequiredMode.NOT_REQUIRED`
- **AND** 该模式在重新解析时覆盖验证注解等正向来源

#### Scenario: 保留其他 Schema 属性并安全写入描述
- **WHEN** 字段具有 `@Schema` 的其他成员，且编辑后的描述包含引号、反斜杠或换行
- **THEN** 系统保留未编辑的 Schema 成员
- **AND** description 仍是合法 Java 注解字符串并与编辑内容一致

#### Scenario: Schema 类型缺少 requiredMode 时拒绝写回
- **WHEN** 项目解析到的 `Schema` 注解类型不包含 `requiredMode` 成员
- **THEN** 系统显示需使用包含该成员的项目 Swagger 注解依赖版本的错误提示（官方 `swagger-annotations` 需为 2.2.5 或更新版本）
- **AND** 该字段的本次编辑失败且保持现有 `@Schema` 注解和源码原样
- **AND** 系统不写入非法的 `requiredMode`，也不回退生成 deprecated `required`

#### Scenario: 批次中任一 Schema 缺少 requiredMode 时先拒绝整批
- **WHEN** 一次字段编辑提交包含普通字段和至少一个缺少 `requiredMode` 成员的 `@Schema` 字段
- **THEN** 系统在写入该批次任何字段源码前拒绝提交并显示版本提示
- **AND** 因该能力预检失败，本批次所有字段源码均保持不变
- **AND** 本场景只规定缺少 requiredMode 的预检失败，不承诺其他异常下的整批事务原子性

### Requirement: 不迁移或兼容 deprecated Schema.required

系统 MUST 不读取 `@Schema.required` 作为 requiredMode 的兼容 fallback，也 MUST 不自动扫描或迁移未编辑字段上的旧写法；用户应将源码更新为 `requiredMode`。

#### Scenario: AUTO 遇到旧 required 写法
- **WHEN** 字段声明 `@Schema(required = true, requiredMode = AUTO)` 且没有其他正向来源
- **THEN** Doc View 忽略 deprecated `required` 并按 AUTO 规则将字段判为非必填

#### Scenario: 写回时移除被编辑字段上的 deprecated required
- **WHEN** 用户编辑并确认一个同时带有 `required` 与 `requiredMode` 的 `@Schema` 字段
- **THEN** 写回后的注解包含当前 `requiredMode`
- **AND** 写回不保留该字段上的 deprecated `required` 属性
- **AND** 不编辑其他源码字段以做批量迁移

### Requirement: Schema requiredMode 与参数 Parameter 元数据分开

本 capability SHALL 只定义 `PsiField` 上的 `@Schema.requiredMode`；方法参数上的 `@Parameter(required = ...)` 由独立参数元数据 capability 定义。

#### Scenario: 方法参数带 Parameter 注解
- **WHEN** 方法参数声明 `@Parameter(required = false)` 或 `required = true`
- **THEN** 本 capability 不以该注解改变 `isRequired(PsiParameter)`、Query/Form 参数或 request-body 参数


### Requirement: 描述编辑过程不丢失多行文本
参数表的描述单元格 SHALL 在用户输入、粘贴、查看、提交及复制/写回之间保留精确字符串，包括真实 LF、连续/尾部空行、引号与反斜杠；不得静默将换行替换为空格。共享编辑器不改变各注解的解析和源码写回规则。

#### Scenario: 原生粘贴多行描述并写回 Schema
- **WHEN** 用户在描述单元格输入含真实换行、引号与反斜杠的文本并提交后确认写回
- **THEN** 表格保存精确草稿，且可查看/编辑多行；Schema.description 作为合法 Java 字符串写回后求值与草稿逐字符一致
- **AND** requiredMode 与未编辑 Schema 成员按既有规则处理，原生一次 Undo 可恢复此次写回前的源码

#### Scenario: 取消描述单元格编辑
- **WHEN** 用户编辑多行描述后取消该 cell edit 或关闭整个参数弹窗而未写回
- **THEN** 取消 cell edit 不提交新的字段草稿，关闭弹窗不改源码
- **AND** 编辑器保留既有表格导航、取消和到复制/写回按钮的键盘可达性，不依赖全局快捷键或新增设置

### Requirement: 多行参数描述不得破坏 Markdown 表格
系统 SHALL 在共享参数 Markdown 表格的描述单元格中将每个逻辑换行输出为 `<br>`，保持字段位于单个物理表格行。系统 SHALL 保留原始 DTO、编辑草稿和 Schema 写回字符串；单行描述的输出 SHALL 保持不变。

#### Scenario: 复制尚未提交的多行描述
- **WHEN** 用户在描述单元格输入含 LF、连续或尾部空行、引号及反斜杠的草稿并复制 Markdown
- **THEN** 复制动作先提交该单元格，描述中的每个换行投影为 `<br>`，字段和必填状态仍在原表格行，源码保持原样

#### Scenario: 不同换行格式和原始值保留
- **WHEN** 参数描述含 CRLF、LF 或独立 CR 并生成默认参数表格
- **THEN** CRLF 只计一次换行，其余各计一次；连续和尾部换行不丢失，原 DTO 字符串保持逐字不变

### Requirement: 一次确认的字段批次可以整体撤销
系统 SHALL 将一次成功的参数编辑确认中的字段写回纳入同一个 IDE 撤销单元。Schema 能力预检失败时 SHALL 不修改任何字段，不以该保证承诺未知异常的事务回滚。

#### Scenario: 双字段写回后一次撤销和重做
- **WHEN** 用户在一次确认中修改两个 Schema 字段的描述和必填状态，写回后执行一次 Undo
- **THEN** 两个字段的源码均恢复到确认前，再执行一次 Redo 时两字段均恢复该次写回结果

#### Scenario: 缺少 requiredMode 时无部分写回
- **WHEN** 同批字段包含普通字段以及无法提供 requiredMode 的 Schema 字段
- **THEN** 确认前的全部源码保持不变，草稿和现有错误提示保留

#### Scenario: 原生 Escape 仅取消正在编辑的单元格
- **WHEN** 用户在描述单元格编辑未提交文本并按下不带修饰键的 Escape
- **THEN** 当前单元格恢复编辑前值，弹窗与其他已提交草稿保留；随后非编辑状态再次 Escape 时沿用平台关闭行为

#### Scenario: 系统主提交快捷键
- **WHEN** 用户在 macOS 按 Command+Enter，或在 Windows/Linux 按 Ctrl+Enter
- **THEN** 当前描述单元格提交；macOS Ctrl+Enter 不作为本插件提交键，也不抢占 IDEA 的原快捷键
