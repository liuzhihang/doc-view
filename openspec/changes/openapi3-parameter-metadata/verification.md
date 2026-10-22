# 验证记录

## 前置依赖

- 本 change 在 `openapi3-schema-required-mode` 的实现与目标测试通过后启动；前置检查通过记录见该 change 的 [verification.md](../openapi3-schema-required-mode/verification.md)。

## 参数元数据 PSI 与 Markdown

- round11 初始 RED：`/tmp/doc-view-reliability-20261018/round11-TEST-com.liuzhihang.doc.view.contract.OpenApi3ParameterMetadataTest.xml` 显示 8 tests、6 failures、0 errors、0 skipped。失败覆盖绑定名称、description/example 和 required 映射；空 description 的 Javadoc fallback 与未支持范围边界测试通过。
- round12 targeted GREEN：`/tmp/doc-view-reliability-20261018/round12-TEST-com.liuzhihang.doc.view.contract.OpenApi3ParameterMetadataTest.xml` 显示 9 tests、0 failures、0 errors、0 skipped。已验证 Spring 绑定名与 `@Parameter.name` 优先级、description/example、required 矩阵、空值 fallback、边界路径，以及完整生成链上的新增 Spring Markdown golden。
- round12 在加入 `testNonEmptyCompoundParameterExamplesSuppressScalarExample` 之前执行；该复合 `examples`/`content`/`array` 抑制回归尚未运行，不能据此声称复合示例规则已验证。既有 `SpringMarkdownContractTest` 的逐字 golden 尚未在本次参数映射后重跑。
- round13 targeted GREEN：`/tmp/doc-view-reliability-20261018/round13-TEST-com.liuzhihang.doc.view.contract.OpenApi3ParameterMetadataTest.xml` 显示 10 tests、0 failures、0 errors、0 skipped，包含 `testNonEmptyCompoundParameterExamplesSuppressScalarExample`；新增 PSI 覆盖确认非空 `examples`、`content` 或 `array` 抑制标量 `example`，空复合属性仍保留标量示例。
- round13 既有 Spring Markdown GREEN：`/tmp/doc-view-reliability-20261018/round13-TEST-com.liuzhihang.doc.view.contract.SpringMarkdownContractTest.xml` 显示 1 test、0 failures、0 errors、0 skipped，原有 golden 未变。
- 用户文档已在 settings/troubleshooting 说明四项元数据、Spring 名称与必填优先级、复合元数据限制及配置标记作用；CHANGELOG 记录对应新增行为。该文档核对和本 change scoped `git diff --check` 通过。

## 全量门禁

- 主线执行 `./gradlew test verifyPluginProjectConfiguration buildPlugin verifyPlugin --rerun-tasks` 成功：35 个测试套件、180 tests、0 failures、0 errors、0 skipped；Gradle 执行了 18/18 项任务，构建耗时约 1 分 3 秒。
- Plugin Verifier 对 IU 261.22158.277 的实际 verdict 为 `Compatible`。JUnit 报告备份：`/tmp/doc-view-reliability-20261018/round21-fullgate-TEST-*.xml`。
- `openspec validate openapi3-parameter-metadata --strict` 通过（`Change is valid`）；本 change scoped `git diff --check` 通过，最终改动边界与设计一致。

JCEF/原生 IDE 验收未执行，不计入本 change 的当前测试证据。
