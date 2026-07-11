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

契约测试 SHALL 使用 IntelliJ Java PSI 解析仓库内的最小 Spring Controller fixture，并 SHALL 通过生产 `SpringDocViewServiceImpl` 构建 `DocView`，不得用手工 DTO 或 mock PSI 替代该链路。

#### Scenario: 带 query 参数的基础 GET 接口
- **WHEN** fixture 声明带类级路径和方法级路径的 `@RestController`/`@GetMapping` 方法、一个必填 `@RequestParam String id` 参数以及单字段响应 DTO
- **THEN** `DocView` 的文档标题、名称和描述等于 fixture 的 Javadoc 契约值
- **AND** path 等于 `/api/users`、method 等于 `GET`、content type 等于 FORM
- **AND** Header 包含 FORM 的 `Content-Type`
- **AND** query 参数 `id` 的类型为 `String`、必填为 true、描述来自 `@param`
- **AND** response body 包含 fixture 中声明的响应字段、类型和描述

### Requirement: 默认 Markdown golden 契约

契约测试 SHALL 使用项目默认 Spring Velocity 模板渲染同一个 `DocView`，并 SHALL 将完整 Markdown 与版本控制中的 UTF-8 golden file 精确比较。

#### Scenario: 渲染基础 GET 接口
- **WHEN** 基础 Spring fixture 被转换为 `DocView` 并调用 `DocViewData.markdownText`
- **THEN** Markdown 的章节顺序、标题、描述、路径、方法、Header 表、Param 表、请求示例、返回参数表和返回 JSON 示例与 golden file 一致
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
