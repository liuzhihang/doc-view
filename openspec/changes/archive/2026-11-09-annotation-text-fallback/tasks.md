# Tasks

## 1. PSI 行为回归与 RED

- [x] 1.1 用 Java PSI fixture 覆盖 `@Operation`/`@ApiOperation` 四个字符串属性的省略默认值、空串、纯空白、已解析常量、嵌入引号/转义和无法解析引用；断言当前输入能到达指定方法，并保存目标测试 XML。使用 `JAVA_21` light-project descriptor；round18 确认常量 initializer、类型、`java.lang.String` 解析和编译期求值均有效。
- [x] 1.2 覆盖名称顺序、两个 Swagger 名称开关、`@DocView.Name`、Javadoc 首行 15 字符限制、方法名 fallback，以及描述顺序、注解来源开关、Javadoc/空描述最终 fallback；round18 在旧实现上取得预期 RED，round20 新测试全绿。

## 2. 求值与 Markdown contract

- [x] 2.1 在 `DocViewUtils.getName()` 与 `getMethodDesc()` 中使用平台字符串求值结果，并只采用可解析且非空白的值；保留有效值内容及既有来源顺序、设置开关和 Javadoc fallback。round20 `AnnotationTextFallbackTest` 4/4 与 `OpenApi3OperationSummaryTest` 7/7 通过。
- [x] 2.2 通过 `SpringDocViewServiceImpl` 和 `DocViewData.markdownText()` 生成至少一份名称/描述 golden，包含常量求值后的实际文字、前后空格、嵌入引号和转义内容；round20 `SpringMarkdownContractTest` 1/1 通过，既有无注解输出保持不变。

## 3. 用户说明与集成验证

- [x] 3.1 主线已更新用户指南与 `CHANGELOG.md`，说明注解空白/未解析时继续 fallback，常量按实际值输出并保留有效字符串内容；已核对名称/描述来源顺序及指南链接。
- [x] 3.2 运行全量测试并检查报告：主线执行 `./gradlew test verifyPluginProjectConfiguration buildPlugin verifyPlugin --rerun-tasks`，35 个测试套件、180 tests 均通过，无失败、错误或跳过。
- [x] 3.3 运行 `verifyPluginProjectConfiguration`、`buildPlugin`、`verifyPlugin`；Plugin Verifier 对 IU 261.22158.277 的 verdict 为 `Compatible`。
- [x] 3.4 运行 `openspec validate annotation-text-fallback --strict` 与 scoped `git diff --check`；复核本 change 对 `DocViewUtils` 的修改只在 `getName()`/`getMethodDesc()`，fixture/golden 与 OpenSpec 文档在授权范围内。
