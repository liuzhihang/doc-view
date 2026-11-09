# java-documentation-metadata Specification

## Purpose
定义从接口方法、参数和实体字段的 Java 注释生成可阅读文档元数据的行为，包括依赖已关联源码时的注释来源以及缺少源码时的可预测降级，保证文档标签与正文均不会被静默丢弃。

## Requirements

### Requirement: 接口与字段版本元数据
系统 SHALL 将方法级及实体字段级非空 `@since`、`@version` 展示到对应接口或参数文档，并提供接口级自定义模板变量，缺失标签不新增空段落。

#### Scenario: Spring 与 Dubbo 的标签
- **WHEN** Spring 或 Dubbo 方法及请求/响应实体字段声明 since/version
- **THEN** 默认中英文模板展示方法元数据，字段表展示各字段自身元数据
- **AND** 普通标量参数保留现有方法标签，缓存命中不丢失元数据

#### Scenario: 未声明标签与普通文本
- **WHEN** 方法没有标签或描述正文提到 `@since`/`@version` 字样
- **THEN** 无标签接口保持原有章节，普通描述文字不被清空

### Requirement: 精确参数描述
系统 SHALL 按完整 Java 参数名读取 Javadoc 参数描述，保持非空注解描述的既有优先级，并对空标签安全降级。

#### Scenario: Dubbo 多参数与参数名前缀
- **WHEN** Dubbo 方法包含 page、searchKey、viewId 等多参数，或同时包含 id 与 identifier，并有各自 @param 注释
- **THEN** 每个参数展示属于自己的完整描述，多行文字保留且不包含其他标签

#### Scenario: 不完整的注释
- **WHEN** 注释包含没有参数名的 @param 或没有描述的参数标签
- **THEN** 文档生成不抛异常，已知其他参数描述保留

### Requirement: 依赖源码字段注释
系统 SHALL 在依赖已关联有效源码时从对应源码字段读取注释与版本元数据，维持注解优先级、原类型和泛型身份；没有源码时不凭空生成注释。

#### Scenario: 已关联源码的依赖
- **WHEN** Spring 或 Dubbo 请求/返回 DTO 来自依赖 JAR，IDE 已关联 sources
- **THEN** 对应字段、继承字段、嵌套字段注释进入文档
- **AND** 先无源码生成再关联源码时后续生成能读取新注释

#### Scenario: 无源码或不可导航
- **WHEN** 依赖没有源码或导航目标不可用
- **THEN** 使用既有注解或空注释降级，生成继续，不自动下载或解压源码、不输出私有信息

### Requirement: Javadoc 标题与正文分离
系统 SHALL 将类普通 Javadoc 的首个非空物理行作为标题；方法普通 Javadoc 名称默认同样只取首行。系统 SHALL 保留单词间空格与 Unicode 字符，排除 block tags；类描述与方法描述默认使用各自首行之后的普通正文，保留段落，不截断。Swagger、显式 DocView tag 和 Java 名称 fallback 优先级保持不变；参数、字段和版本 tag 语义保持不变。

#### Scenario: 多行注释与标签
- **WHEN** Spring、Feign 或 Dubbo 的类/方法注释包含标题、空白行、两行正文、@param 和 @return
- **THEN** 默认标题仅含首个非空行，方法描述包含完整后续正文与段落，不包含 block tags
- **AND** 类后续正文独立保留为 classDesc，默认模板以类说明展示，自定义模板可使用 ${DocView.classDesc}；不混入方法 desc

#### Scenario: 空注释与单行注释
- **WHEN** 注释只有标签、空白或只有一行正文
- **THEN** 空标题按既有来源 fallback，单行注释的方法描述为空，原参数/字段描述继续正常生成

### Requirement: 标题和描述生成设置
系统 SHALL 提供普通 Javadoc 标题字符上限（默认80，有效范围1–200，按 Unicode code point计数），作用于类和方法普通注释标题；方法“仅首行”默认开启、Javadoc后续正文描述来源默认开启。关闭仅首行时方法标题取普通正文合并行后再限长；关闭正文来源且没有更高优先级描述时输出空描述。限长 MUST NOT 作用于描述、注解、显式tag或Java名称。

#### Scenario: 设置保存与取消
- **WHEN** 修改三个新设置后 Apply 或 Reset，或取消设置窗口并重新打开
- **THEN** Apply 持久化且触发相关生成缓存失效；Reset 恢复已应用值；取消不更改持久化值
- **AND** 越界或非整数输入禁止 Apply 且说明有效范围，不部分保存同页字段

#### Scenario: Unicode 上限与描述保真
- **WHEN** 普通注释标题超出用户上限且包含补充平面字符，后续描述长于上限
- **THEN** 标题在完整 Unicode code point 边界截断、不附加额外字符，后续描述完整保留
