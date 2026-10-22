# 验证记录

## PSI fixture 与 RED 证据

- round14 初始 RED：`/tmp/doc-view-reliability-20261018/round14-red-TEST-com.liuzhihang.doc.view.contract.AnnotationTextFallbackTest.xml` 显示 4 tests、4 failures。之后的 fixture 诊断发现 `NameSettings` 失败中包含无法求值的测试常量，不能据此认定生产优先级有误。
- round16/round17 诊断：早期 light fixture 中 `PsiField.computeConstantValue()` 返回 `null`。测试改用明确的 `public static final String` 常量和 `JAVA_21` descriptor，并断言 literal 值、字段类型、`java.lang.String` 可解析及编译期常量求值。round18 这些 fixture 断言均通过，`testNameSettingsKeepTheExistingPriorityAndGates` 也通过；剩余 3 项才是有效 RED：空白名称没有继续 fallback，Swagger 2 常量引用和 OpenAPI 3 描述常量以引用文本输出。
- round18 RED：`/tmp/doc-view-reliability-20261018/round18-TEST-com.liuzhihang.doc.view.contract.AnnotationTextFallbackTest.xml` 显示 4 tests、3 failures、0 errors、0 skipped。该结果排除了早期 `OpenAPI3` 常量测试失败属于运行时代码问题的误判。

## 实现后的 targeted GREEN

- round20：`/tmp/doc-view-reliability-20261018/round20-TEST-com.liuzhihang.doc.view.contract.AnnotationTextFallbackTest.xml` 显示 4 tests、0 failures、0 errors、0 skipped。
- round20：`/tmp/doc-view-reliability-20261018/round20-TEST-com.liuzhihang.doc.view.contract.OpenApi3OperationSummaryTest.xml` 显示 7 tests、0 failures、0 errors、0 skipped；已有 summary 优先级和空值回退保持通过。
- round20：`/tmp/doc-view-reliability-20261018/round20-TEST-com.liuzhihang.doc.view.contract.SpringMarkdownContractTest.xml` 显示 1 test、0 failures、0 errors、0 skipped。新 golden 通过 `SpringDocViewServiceImpl` 和 `DocViewData.markdownText()` 验证名称/描述常量的实际值、前后空格、引号与换行转义；旧的无注解 Markdown golden 未变。

## 文档与静态检查

- 主线更新了 `docs/user-guide/settings.md`、`docs/user-guide/troubleshooting.md` 和 `CHANGELOG.md`，说明空白或无法求值的注解来源继续 fallback、字符串常量按实际值输出，以及名称/描述来源顺序；已核对相关示例与实现一致。
- `openspec validate annotation-text-fallback --strict` 通过；针对本 change 文件及相关共享路径运行的 `git diff --check` 无输出。`DocViewUtils.java` 同时包含其他已批准 change 的代码；本 change 自身只改 `getName()` 与 `getMethodDesc()` 两个方法。

## 全量门禁

- 主线执行 `./gradlew test verifyPluginProjectConfiguration buildPlugin verifyPlugin --rerun-tasks` 成功：35 个测试套件、180 tests、0 failures、0 errors、0 skipped；Gradle 执行了 18/18 项任务，构建耗时约 1 分 3 秒。
- Plugin Verifier 对 IU 261.22158.277 的实际 verdict 为 `Compatible`。JUnit 报告备份：`/tmp/doc-view-reliability-20261018/round21-fullgate-TEST-*.xml`。
- `openspec validate annotation-text-fallback --strict` 与 scoped `git diff --check` 已通过，task 3.2、3.3 已完成。

本次证据覆盖自动化测试、构建和 Plugin Verifier；未执行原生 IDE 手工验收。
