# Markdown Generation Contract

## Purpose

定义 Doc View 从真实 Java PSI 构建 `DocView` 并通过默认模板生成 Markdown 的自动化回归基线，以及测试发现和运行时依赖隔离要求。

## Requirements

### Requirement: 自动化测试发现门禁

仓库的 Gradle `test` 任务 SHALL 执行可发现的 Doc View 契约测试，并 MUST 在没有发现任何测试时失败。

#### Scenario: 契约测试存在
- **WHEN** 维护者执行 `./gradlew test`
- **THEN** Gradle 发现并执行至少一个 Doc View 契约测试
- **AND** 测试报告记录该测试结果

#### Scenario: 测试被意外移除
- **WHEN** 测试源码中不存在 Gradle 可发现的测试
- **THEN** `test` 任务以零测试门禁失败

### Requirement: Spring PSI 到 DocView 基础契约

契约测试 SHALL 使用 IntelliJ Java PSI 解析最小 Spring Controller fixture，并 SHALL 通过生产 `SpringDocViewServiceImpl` 构建 `DocView`，再用项目默认模板检查可观察 Markdown。不得用手工 DTO 或 mock PSI 替代该链路。fixture 中 Spring 注解默认值 SHALL 与真实 Spring Web API 一致，尤其包括 `RequestParam` 和 `RequestHeader` 完整的 `defaultValue` 未设置哨兵。

#### Scenario: 带 query 参数的基础 GET 接口
- **WHEN** fixture 声明带类级路径和方法级路径的 `@RestController`/`@GetMapping` 方法、无 `@RequestBody` 的 `@RequestParam(name = "id") String id` 以及单字段响应 DTO
- **THEN** `DocView` 的标题、名称、描述、路径 `/api/users` 和 method `GET` 符合 fixture 契约
- **AND** `DocView.contentType` 保持内部兼容值 FORM
- **AND** query Param `id` 的类型为 `String`，描述来自 `@param`，Spring 缺省 `required=true` 映射为必填
- **AND** Header 列表不包含由默认 FORM 推断出的 `Content-Type`，对应 Markdown golden 也不输出该错误 Header
- **AND** response body 包含 fixture 声明的响应字段、类型和描述

#### Scenario: 默认 Spring 绑定值不被误认为自定义 defaultValue
- **WHEN** 直接方法参数使用 `@RequestParam(name = "q")` 或 String `@RequestHeader(name = "X-Trace")`，没有显式 `required` 或有效 `defaultValue`
- **THEN** Spring 缺省 `required=true` 映射为必填
- **AND** 显式 `required=false` 或非哨兵 `defaultValue` 仍按 Spring 规则映射为可选

#### Scenario: 仅移除推断出的无 body GET/HEAD FORM Header
- **WHEN** Spring GET 或已解析为 HEAD 的方法没有 `@RequestBody`，且没有显式 `Content-Type` 请求头
- **THEN** Header 列表不生成推断的 FORM `Content-Type`
- **AND** `DocView.contentType` 不变，显式请求头、带 `@RequestBody` 的内容类型以及其他 HTTP method 的既有行为不变

### Requirement: 默认 Markdown golden 契约

契约测试 SHALL 使用项目默认 Spring Velocity 模板渲染同一个生产 `DocView`，并 SHALL 将完整 Markdown 与版本控制中的 UTF-8 golden file 精确比较。对无 body 的基础 GET，golden SHALL 不含推断出的 FORM `Content-Type` Header；除该有针对性的行为修正外，章节顺序、标题、描述、参数、请求示例、响应字段与示例 SHALL 保持既有输出。

#### Scenario: 渲染基础 GET 接口
- **WHEN** 基础 Spring GET fixture 被转换为 `DocView` 并调用 `DocViewData.markdownText`
- **THEN** Markdown 与更新后的 golden 完全一致，且 Header 区不包含推断的 `application/x-www-form-urlencoded`
- **AND** 比较仅统一仓库文件的行结束符，不忽略正文空格、空行或表格内容差异

### Requirement: 测试依赖与运行时隔离

IntelliJ Platform test framework、测试类、注解桩、fixture 和 golden data MUST 仅用于测试，MUST NOT 成为插件运行时依赖或打包内容。

#### Scenario: 构建插件分发包
- **WHEN** 维护者执行 `./gradlew buildPlugin`
- **THEN** 插件 ZIP 构建成功
- **AND** ZIP/JAR 条目不包含契约测试类、testData 或 IntelliJ 测试框架依赖

### Requirement: 基线范围保持有限

本 capability SHALL 只固定当前 Spring 基础链路，不得把未覆盖框架或数据形状描述为已验证，也不得借测试基线修改生产行为。

#### Scenario: 基线验证通过
- **WHEN** 基线测试和构建门禁通过
- **THEN** Spring、Feign、Dubbo、DTO、模板、上传、导出、设置和 UI 的生产文件保持未修改
- **AND** Feign、Dubbo、请求体、递归、泛型、集合、Map 和 OpenAPI 3 行为仍由后续独立 change 覆盖
