## 1. 失败契约测试

- [x] 1.1 扩展 test-support 注解桩并新增 `OperationSummaryController.java` fixture，声明 summary、description、Swagger 2 fallback、空值与设置开关场景。
- [x] 1.2 新增 `OpenApi3OperationSummaryTest`，通过生产 Spring service 和 `DocViewUtils` 覆盖 summary 映射、优先级、空值 fallback、设置开关及 description 保持不变。
- [x] 1.3 执行 `./gradlew test --tests '*OpenApi3OperationSummaryTest' --rerun-tasks`，确认修复前只有非空 summary 映射/优先级场景按预期失败，并记录实际 fallback 值。

## 2. 最小生产修复

- [x] 2.1 仅修改 `DocViewUtils.getName` 的 Swagger 3 分支，读取 `summary` 并只在非空白时返回，不改变后续 fallback。
- [x] 2.2 重跑 `./gradlew test --tests '*OpenApi3OperationSummaryTest' --rerun-tasks`，确认所有 OpenAPI 3 名称契约通过。

## 3. 文档与回归验证

- [x] 3.1 更新 `CHANGELOG.md` 的 Unreleased/Fixed，说明 `@Operation.summary` 名称修复及可见影响。
- [x] 3.2 更新 `docs/roadmap.md`，将 OpenAPI 3 annotation compatibility 拆分为 summary、schema required mode 和 parameter metadata 三个独立切片。
- [x] 3.3 执行 `./gradlew test --rerun-tasks`，确认既有 Spring Markdown golden 与全部测试通过。
- [x] 3.4 执行 `./gradlew buildPlugin`，确认生产插件仍可打包且没有新增 runtime 依赖。
- [x] 3.5 执行 `openspec validate openapi3-operation-summary --strict --no-interactive`、`git diff --check` 和范围检查，确认生产改动仅限 `DocViewUtils.getName` 且 DTO、模板、设置、plugin.xml 与 payload 实现未修改。
