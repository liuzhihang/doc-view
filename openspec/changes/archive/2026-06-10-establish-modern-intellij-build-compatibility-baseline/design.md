## 上下文

Doc View 当前使用 Gradle 8.5、Java 17、`org.jetbrains.intellij` 1.16.1、IntelliJ IDEA IU 2024.1、`pluginSinceBuild=241`、空 `pluginUntilBuild`，并依赖 bundled Java 和 Markdown 插件。现有兼容性文档已经记录这些值，但构建配置尚未定义显式 Plugin Verifier 矩阵，也没有形成采用 IntelliJ Platform Gradle Plugin 2.x 的决策流程。

本变更是构建与兼容性基线。它可以修改 Gradle 构建配置和文档，但不得修改 Java 生产行为、生成 Markdown 行为、上传/导出行为、设置持久化行为、UI 行为、运行时资源，或除兼容性 metadata 之外的 plugin descriptor 语义。

实现时应以 JetBrains 官方 IntelliJ Platform Plugin SDK 文档作为主要外部参考，用于核对插件开发、Gradle 插件迁移、沙箱 IDE 运行、插件打包和 Plugin Verifier 行为：https://plugins.jetbrains.com/docs/intellij/welcome.html。

## 目标 / 非目标

**目标：**

- 建立可重复执行的 Gradle 工作流，覆盖 `runIde`、`buildPlugin` 和 `verifyPlugin`。
- 决定本变更是否迁移到 IntelliJ Platform Gradle Plugin 2.x；如果暂缓迁移，需要记录原因。
- 定义覆盖受支持 IDEA 范围的 Plugin Verifier 矩阵，并使用具体 IDE build 目标。
- 明确受支持 IDEA 版本范围，以及 `sinceBuild` / `untilBuild` 策略。
- 明确 Java 基线决策，区分 Gradle 运行 JDK、Java source/target compatibility 和目标 IDEA 平台运行时。
- 更新 `docs/intellij-compatibility.md`，让未来维护者可以复现基线，并知道何时需要更强验证。
- 在本地工具允许的范围内验证构建、打包、Plugin Verifier 和开发 IDE 启动。

**非目标：**

- 不修改 Java 生产代码。
- 不修改解析、DTO、Markdown 渲染、上传、导出、复制、设置或 UI 行为。
- 不新增与 IntelliJ 构建工具无关的依赖。
- 不发布到 JetBrains Marketplace。
- 不承诺兼容文档验证矩阵之外的未来 IDEA 版本。

## 决策

### 决策：将 Plugin 2.x 迁移作为实现阶段的兼容性决策

实现必须基于 JetBrains 官方 IntelliJ Platform Plugin SDK 指南，结合当前 Gradle 版本、Java 17 基线、bundled plugin 声明、verifier 支持、changelog 集成和现有任务，评估 IntelliJ Platform Gradle Plugin 2.x。如果迁移能在不改变运行时行为的前提下通过验证，实现应采用 2.x 并记录新的 DSL；如果迁移带来过多无关改动或阻塞验证，实现必须保留当前插件线，记录阻塞原因，并继续建立 verifier 矩阵和工作流。

备选方案：强制立即迁移到 2.x。这个方案能让基线默认更现代，但如果本地验证暴露迁移阻塞，容易把兼容性基线扩大成更重的构建迁移。

### 决策：兼容性 metadata 保持明确且保守

基线必须让 `pluginSinceBuild` 与最低支持 IDEA 版本保持一致。`pluginUntilBuild` 可以在文档化 verifier 覆盖范围后继续留空，也可以在 verifier 结果要求上限时设置为最高已验证 major line。文档必须区分“验证矩阵已覆盖”和“plugin.xml 未主动限制上限”。

备选方案：继续保留空 `untilBuild`，但不解释其含义。这样 Marketplace 兼容面较宽，但未来支持声明更难审计。

### 决策：Java 基线随目标 IDEA 平台同步

实现必须根据 JetBrains build number ranges 和目标 IDEA 平台选择 Java 基线。若支持范围仍包含 2024.1，则 Java source/target compatibility 必须保持 17；若本基线切换到 2024.2+，则 Java source/target compatibility 应升级到 21，并在文档中说明这会把最低支持 build 提升到 242。

备选方案：继续使用 Java 17 同时声明支持 2024.2+。这个方案减少构建改动，但与官方 2024.2+ Java 21 平台基线不一致，后续 verifier 和 IDE 运行风险更高。

### 决策：同时验证自动化构建任务和手动 IDE 启动

实现必须使用 `./gradlew buildPlugin` 和 `./gradlew verifyPlugin` 作为自动化验收检查。`./gradlew runIde` 必须完成配置并尝试启动开发沙箱 IDE；因为该流程带有交互性，实现必须在文档中记录手动验证清单，以及 agent 环境无法完整验证时的本地限制。

备选方案：只依赖 Plugin Verifier。Plugin Verifier 能发现 API 兼容性问题，但不能证明插件能在开发沙箱中启动，也不能证明核心 tool window 和 action 可用。

### 决策：通知 group 按需获取，避免类初始化依赖 service

IntelliJ 2024.2+ 会在类初始化期间检查 service 获取行为。`DocViewNotification` 不得在 `static` 字段初始化时调用 `NotificationGroupManager.getInstance()`；应保留 notification group id 常量，并在实际发送通知时按需获取 `NotificationGroup`。这样不改变通知内容、图标、action 或触发条件，只消除 startup activity 中的 class initializer service 依赖。

备选方案：移除 startup notification。该方案能避开报错，但会改变已有用户可见行为，不符合本基线的最小兼容性修复目标。

### 决策：在兼容性文档中记录 verifier 矩阵

`docs/intellij-compatibility.md` 必须包含 Plugin Verifier 使用的 IDE 版本/build、运行矩阵的命令、支持的 IDEA 范围，以及更新矩阵的发布纪律。矩阵应包含最低支持 major line、当前开发目标，以及所选构建工具能够验证的最新目标 IDEA line。

备选方案：只在 Gradle 配置中保留矩阵。Gradle 文件能证明任务可以执行，但不能解释支持策略、升级节奏或手动验证预期。

## 风险 / 权衡

- Plugin 2.x 迁移可能需要重写 Gradle DSL 和任务配置 -> 接受迁移前，使用 Gradle task discovery、插件打包和 Plugin Verifier 做验证。
- Java 21 会提高最低运行平台并结束 2024.1 兼容范围 -> 将 `pluginSinceBuild`、`platformVersion`、verifier 矩阵和文档一起更新，避免兼容声明与字节码级别不一致。
- Verifier 下载可能在受限或离线环境失败 -> 文档中记录预期命令，并记录本地执行是否成功以及是否存在网络阻塞。
- 空 `pluginUntilBuild` 可能让 Marketplace 兼容声明看起来宽于 verifier 矩阵 -> 文档中明确两者区别，并要求声明支持新版 IDEA 前先更新矩阵。
- `runIde` 验证有一部分必须手动完成 -> 保持自动启动配置稳定，并在 `docs/intellij-compatibility.md` 中记录具体烟测项。
- 构建配置变更可能意外影响插件打包内容 -> 对比插件包预期，避免无关依赖、资源或 descriptor 变更。
- 通知兼容性修复可能改变 startup notification 行为 -> 只移动 service 获取时机，不修改通知文案、action、图标或触发条件。

## 迁移计划

1. 检查当前 Gradle 任务、IntelliJ 插件配置、plugin metadata 和兼容性文档。
2. 在聚焦的构建变更中评估 IntelliJ Platform Gradle Plugin 2.x 迁移路径。
3. 在选定 Gradle 插件线下配置或确认 `runIde`、`buildPlugin` 和 `verifyPlugin`。
4. 为受支持 IDEA 版本配置 Plugin Verifier 矩阵。
5. 更新 `docs/intellij-compatibility.md`，记录当前基线、支持范围、命令、矩阵、手动检查项和升级规则。
6. 运行 `./gradlew buildPlugin` 和 `./gradlew verifyPlugin`；尝试 `./gradlew runIde`，或记录为什么完整手动验证需要本地 IDE session。
7. 如果迁移失败或引入不可接受的改动，回滚 Gradle 插件迁移部分，同时保留已确认的发现以及安全的 verifier / 工作流改进。

## 待确认问题

- 初始 verifier 矩阵应以哪个最新 IDEA release 作为上限？
- 文档化 verifier 覆盖范围后，`pluginUntilBuild` 应继续留空，还是限制到最高已验证 major line？
