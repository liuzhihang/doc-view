## Context

目标 IU 2026.1 的 Plugin Verifier 当前报告 7 处 deprecated API usage：三个调用方法使用 `JavaAnnotationIndex.get(...)`，tool window 覆盖并调用 `UiCompatibleDataProvider.getData(String)`，树搜索使用 deprecated 构造器，以及重复名称使用 `RandomStringUtils.randomAlphabetic(int)`。这些调用分布在 PSI 扫描、tool window 和文档名称生成三条路径，但都存在不改变外部行为的公开替代。

本 change 只处理兼容性债务。生产代码继续使用 Java 21，不新增依赖，不修改 `plugin.xml`、平台基线、Markdown、payload、设置或产品功能。PSI 扫描仍要求在 smart mode/read action 上下文中执行；既有 Dumb Mode 竞态不在本次范围内。

## Goals / Non-Goals

**Goals:**

- 清零当前配置目标 IDE 报告的 7 处 deprecated API usage。
- 保持 Spring、Feign、Dubbo 的候选注解、module scope、FQN 过滤和返回集合语义。
- 保持 tool window 的四个自定义 `DataKey`、父类 Quick Action 数据和树键盘搜索体验。
- 保持重复文档名称的首项、随机字符数量/字符集和列表索引后缀格式。
- 让后续 deprecated API usage 直接导致 `verifyPlugin` 失败。

**Non-Goals:**

- 不改为确定性文档名称，不修正既有随机后缀的碰撞模型。
- 不排序、去重或改写 Spring/Feign/Dubbo 扫描规则，也不新增 Dumb Mode fallback。
- 不提交 2026.2 预发布版本到正式 Verifier 矩阵，不改变 `sinceBuild`/`untilBuild`。
- 不处理 OpenAPI 3 其他注解、性能优化、导航功能或平台上传稳定性。

## Decisions

### 1. 注解索引使用同一实现的公开替代

将五个源码调用从 `JavaAnnotationIndex.get(...)` 改为 `getAnnotations(...)`。目标平台中旧方法仅委托给新方法，因此 short name、project、`GlobalSearchScope.moduleScope(module)` 与 `JavaSourceFilterScope` 行为保持一致。

Spring 和 Dubbo 需要合并两个索引结果。公开 API 只承诺 `Collection`，不承诺返回值可修改，因此先复制到 `ArrayList` 再 `addAll`；Feign 只遍历返回集合。不会改用 `AnnotatedElementsSearch`，因为它需要先解析注解类并改变当前“短名候选集 + FQN 业务过滤”路径。

### 2. Tool window 迁移到 `UiDataProvider`

`DocViewWindowPanel` 覆盖 `uiDataSnapshot(DataSink)`，先调用 `super.uiDataSnapshot(sink)`，再通过 `sink.set(...)` 提供 `WINDOW_PANE`、`WINDOW_ROOT_NODE`、`WINDOW_CATALOG_TREE` 和 `WINDOW_TOOLBAR`。调用父类可保留 `SimpleToolWindowPanel` 提供的 Quick Action 数据，也允许平台未来继续贡献父类数据。

树搜索通过 API module 中的 `TreeUIHelper.getInstance().installTreeSpeedSearch(catalogTree)` 安装。目标平台实现委托给 `TreeSpeedSearch.installOn(JTree)`，与旧构造器使用相同的默认展示文本、比较器和不自动展开语义。不会直接依赖 `TreeSpeedSearch.installOn`，以避免继续绑定 implementation module。

### 3. 重复名称只替换 deprecated 入口

将 `RandomStringUtils.randomAlphabetic(5)` 改为 `RandomStringUtils.secure().nextAlphabetic(5)`。目标平台捆绑的 Commons Lang 3.18.0 中旧静态方法正是委托到该实例方法，因此随机源、五字符 alphabetic 格式和列表索引后缀均保持不变。

不会改用 `insecure()`、`secureStrong()`、JDK helper 或确定性序号：这些方案分别会改变随机源、潜在阻塞特性或用户可见名称。

### 4. Plugin Verifier 作为 RED/GREEN 门禁

在 `build.gradle` 的 `failureLevel` 中加入 `DEPRECATED_API_USAGES`。实施顺序为：先加入门禁并运行 `verifyPlugin`，确认它因当前 7 处报告失败；再修改生产代码并确认相同命令恢复通过且报告不再包含 deprecated usage。

提交配置继续固定 `pluginVerifierIdeVersions=2026.1`，保证发布门可重复。使用命令行属性额外对本地已有的 `262.8665.176` 做一次前向验证并记录结果，但该验证不代表正式扩大支持矩阵。

### 5. 自动契约与手工 UI 验证分工

- 真实 IntelliJ light fixture 覆盖 Spring、Feign、Dubbo 注解索引扫描和同短名 FQN 噪声，按 qualified-name 集合断言，不固化 Stub Index 顺序。IU light fixture 明确禁止增加第二 module，因此跨 module scope 由保持原始 `GlobalSearchScope.moduleScope(module)` 调用的代码审查和 `runIde` 多模块烟测覆盖。
- `DocViewService` 契约测试覆盖首个重复名称不变、后续名称匹配五个 ASCII 字母加全局零基索引、非重复名称不变。
- `verifyPlugin` 直接证明 deprecated API 门禁。
- `runIde` 验证四个 `DataKey` 的 action 消费路径、刷新/展开/折叠/右键菜单以及键盘速度搜索；不伪造标记为 non-extendable 的 `DataSink`。

## Risks / Trade-offs

- [索引返回集合未来不可修改] → Spring/Dubbo 在合并前复制到自身 `ArrayList`。
- [漏调用父类 snapshot 导致 Quick Action 数据消失] → contract 和实现明确要求先调用 `super.uiDataSnapshot`，并通过 `runIde` 检查工具栏动作。
- [随机测试出现概率性失败] → 只断言名称格式、索引和本次结果唯一性，不比较两次随机输出。
- [light fixture 不能自动建立第二 module，自动测试无法证明跨 module scope] → 保持原始 module scope 参数不变，并在 `runIde` 多模块样例中检查扫描隔离。
- [自动测试不能完整证明 Swing 数据上下文与交互] → 保留 `runIde` 手工 smoke matrix，并把未执行项作为剩余风险报告。
- [动态 Verifier 目标导致发布结果漂移] → 提交矩阵保持固定；前向版本仅用精确 build 的命令行覆盖验证。

## Migration Plan

1. 建立 deprecated usage 的失败门禁并保存 RED 证据。
2. 添加行为契约测试，确认现有扫描和命名基线。
3. 按 PSI、UI、名称三个局部边界替换 API，并运行针对性测试。
4. 运行完整测试、打包、项目配置检查、正式矩阵和前向 build 的 Plugin Verifier。
5. 执行可用的 `runIde` 手工烟测，更新兼容性文档和 changelog。

回滚时同时撤销 Java API 替换和 deprecated failure level；不需要数据、设置或 descriptor 迁移。

## Open Questions

无。确定性名称、Dumb Mode 竞态和正式 2026.2 支持矩阵均作为后续独立 change 处理。
