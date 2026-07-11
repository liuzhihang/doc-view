## 1. 测试基础设施门禁

- [x] 1.1 执行 `./gradlew test --rerun-tasks` 记录当前零测试仍成功的基线，并确认报告未执行真实测试。
- [x] 1.2 在 `build.gradle` 声明 IntelliJ Platform test framework、开启 `failOnNoDiscoveredTests = true`，再执行 `./gradlew test --rerun-tasks` 证明零测试按预期失败。

## 2. Spring Markdown 契约

- [x] 2.1 新增最小 Spring 注解 test-support helper 与 `BasicController.java` PSI fixture，保持测试不依赖真实 Spring runtime。
- [x] 2.2 新增 `SpringMarkdownContractTest`，通过生产 `SpringDocViewServiceImpl` 断言基础 GET 场景的 `DocView` 标题、名称、描述、路径、方法、content type、Header、query 参数和 response body。
- [x] 2.3 新增 UTF-8 Markdown golden file，并让同一测试通过 `DocViewData.markdownText` 精确验证默认模板完整输出。
- [x] 2.4 在 teardown 清理 `DtoSchemaCacheService`，执行定向测试并确认测试报告中该契约测试真实执行且通过。

## 3. 构建与范围验证

- [x] 3.1 执行 `./gradlew test --rerun-tasks`，确认零测试门禁开启后完整测试任务通过。
- [x] 3.2 执行 `./gradlew buildPlugin`，检查插件 ZIP/JAR 不含测试类、testData 或测试框架依赖。
- [x] 3.3 执行 `openspec validate markdown-contract-baseline --strict --no-interactive`、`git diff --check` 和按 change 文件清单检查，确认 baseline 自身未修改生产 Java、模板、plugin.xml 与运行时资源；其他 active change 单独核验。
