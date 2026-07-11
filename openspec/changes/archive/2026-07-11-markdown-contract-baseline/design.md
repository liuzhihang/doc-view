## Context

Doc View 的核心结果由 IntelliJ PSI、项目级设置、`SpringDocViewServiceImpl`、内部 DTO 和 Velocity 模板共同决定。仓库目前只有手工 HTTP 示例，没有 Gradle 能发现的自动化测试；`test` 任务还显式允许零测试，因此后续解析修复缺少回归基线。

本 change 跨越 Gradle 测试依赖、IntelliJ light fixture、Spring PSI fixture 和 Markdown golden，但不改变任何生产实现。测试必须使用真实 Java PSI 与项目服务，不能用手工拼装 `DocView` 代替主链路。

## Goals / Non-Goals

**Goals:**

- 建立可由 `./gradlew test` 稳定发现的 IntelliJ Java light fixture。
- 固定一条 Spring GET 基础链路的关键 `DocView` 字段和默认 Markdown 完整文本。
- 让零测试成为构建失败，并证明新增测试在完整 `test` 中执行。
- 保证测试框架和 fixture 只存在于测试作用域，不进入插件 ZIP。

**Non-Goals:**

- 不修复或扩展 PSI、Swagger/OpenAPI、DTO、模板、上传、导出、设置或 UI 行为。
- 不在首个基线中覆盖 Feign、Dubbo、请求体、递归、泛型、集合、Map 或异常 PSI。
- 不引入 Spring 运行时依赖、JUnit 5 或 mock `DocView`/PSI 对象。
- 不把当前输出是否理想与是否稳定混为一件事；行为改进由后续独立 change 定义。

## Decisions

### 使用 Platform 与 Java plugin test framework

在现有 `intellijPlatform` 依赖块中同时声明 `TestFrameworkType.Platform` 与 `TestFrameworkType.Plugin.Java`，并显式添加 test-scope JUnit 4。Platform framework 提供通用测试基础设施，Java plugin framework 提供 `LightJavaCodeInsightFixtureTestCase` 等 Java PSI fixture，JUnit 4 依赖同时提供该 JUnit 3 风格基类所需的 `junit.framework.TestCase`。Java test framework 已提供 Hamcrest 2.2，因此从 JUnit 依赖中排除旧 `hamcrest-core`，避免测试 classpath 出现重复类。这与当前 IntelliJ Platform Gradle Plugin 的依赖模型一致。未选择 JUnit 5 base class，因为首个契约不需要参数化或扩展模型，增加另一种测试写法只会扩大基线范围。

### fixture 使用最小 Spring 注解桩

测试启动时通过 test-support helper 向 light fixture 注册 `RestController`、`RequestMapping`、`GetMapping` 和 `RequestParam` 的最小同名注解。业务输入保存在 `src/test/testData`，包含类级路径、方法级路径、必填 query 参数、Javadoc 名称/描述和单字段响应 DTO。未引入真实 Spring 依赖，因为生产解析只依赖注解全限定名和 PSI 属性。

### 同一测试同时验证 DTO 断言与 Markdown golden

测试从 fixture 中取得 `PsiClass`/`PsiMethod`，调用 `SpringDocViewServiceImpl.buildClassMethodDoc`，先断言文档标题、名称、描述、路径、HTTP method、content type、header、query 参数和响应字段，再调用 `DocViewData.markdownText`。默认模板输出与 UTF-8 golden file 按 LF 逐行精确比较；golden 文件末尾的仓库换行由测试显式处理，不放宽正文空白。

这种顺序能在失败时区分“PSI/DTO 解析变化”和“模板渲染变化”。未拆成多个重复构建 `DocView` 的测试，以减少 light fixture 启动和缓存噪声。

### 显式治理测试发现与缓存隔离

新增真实测试后将 `failOnNoDiscoveredTests` 设为 `true`。测试 teardown 清理 `DtoSchemaCacheService`，避免未来同 JVM fixture 之间共享 schema 结果。测试不修改持久化设置和默认模板，因此无需为生产设置增加测试专用接口。

## Risks / Trade-offs

- [IntelliJ 平台升级改变 fixture API 或依赖坐标] → Platform/Java test framework 通过当前 Gradle 插件的 `testFramework` DSL 声明，JUnit 仅使用 test scope，并由依赖图、完整 `test`/`buildPlugin` 验证。
- [golden 固定了当前模板中的空章节或排版细节] → 这是契约基线的目的；未来有意调整 Markdown 时必须在独立 change 中同时评审 spec、断言和 golden。
- [最小注解桩与真实 Spring 声明不完全一致] → 只声明本场景读取的属性并保持真实全限定名；涉及 meta-annotation 或更多属性的后续场景另行扩展。
- [测试依赖意外进入发布包] → 构建插件后检查 ZIP/JAR 条目中不存在测试类、fixture 数据和测试框架依赖。
- [单一 Spring 场景覆盖面有限] → 将其作为可扩展基线，不以一个测试声称覆盖全部生成行为。
