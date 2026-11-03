# Java Documentation Metadata

## Purpose

定义从接口方法、参数和实体字段的 Java 注释生成可阅读文档元数据的行为，包括依赖已关联源码时的注释来源以及缺少源码时的可预测降级，保证文档标签与正文均不会被静默丢弃。

## ADDED Requirements

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
