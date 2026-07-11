## Why

当前仓库没有可由 Gradle 发现并执行的自动化测试，PSI 解析、`DocView` 构建和默认 Markdown 输出在维护时缺少可重复验证。OpenAPI 3 注解兼容等后续行为变更必须先有一条真实、稳定的契约测试链路，避免在修复解析规则时无意改变 Markdown。

## What Changes

- 为测试源码引入 IntelliJ Platform 测试框架，并让 `test` 任务在未发现测试时失败。
- 新增最小 Spring Controller PSI fixture，覆盖 Java 源码到 `DocView` 的基础解析路径。
- 对 `DocView` 的名称、描述、路径、HTTP 方法和基础请求信息做显式断言。
- 使用 golden file 固定默认 Spring 模板生成的 Markdown 文本。
- 本 change 不修改生产解析代码、DTO、默认模板或任何用户可见行为，也暂不覆盖 Feign、Dubbo、递归 DTO 和 OpenAPI 3 修复。

## Capabilities

### New Capabilities

- `markdown-generation-contract`: 定义 Spring Java PSI 到 `DocView` 及默认 Markdown 输出的自动化契约测试基线。

### Modified Capabilities

无。

## Impact

- 影响 `build.gradle` 的 test-scope IntelliJ Platform 依赖和测试发现门禁。
- 新增 `src/test/java` 下的 IntelliJ light fixture 测试以及 `src/test/testData` 下的 Java fixture/golden Markdown。
- 测试依赖不得进入插件运行时产物；生产代码、插件描述符和用户配置保持不变。
- 验证以定向测试、完整 `test`、`buildPlugin` 及插件 ZIP 内容检查为主。
