## Purpose

Doc View maintains a modern IntelliJ IDEA build and compatibility baseline so maintainers can build, package, verify, and launch the plugin against the documented 2024.2+ support range.

## Requirements

### Requirement: 构建工作流基线

项目 SHALL 定义一套可重复的 Gradle 工作流，用于在选定 IntelliJ IDEA 兼容性基线下构建、打包、验证并启动 Doc View。

#### Scenario: 生成插件安装包

- **WHEN** 维护者运行文档中记录的插件打包命令
- **THEN** Gradle 生成 Doc View 插件分发产物，且不依赖未文档化的手动步骤

#### Scenario: 启动开发 IDE

- **WHEN** 维护者运行文档中记录的开发 IDE 命令
- **THEN** Gradle 使用文档中记录的平台类型、平台版本、bundled plugins 和 JVM 参数启动或尝试启动沙箱 IntelliJ IDEA

#### Scenario: 运行 Plugin Verifier

- **WHEN** 维护者运行文档中记录的 Plugin Verifier 命令
- **THEN** Gradle 按配置的 IDEA 矩阵执行 Plugin Verifier，而不是依赖隐式的单平台检查

### Requirement: IntelliJ Platform Gradle Plugin 迁移决策

项目 SHALL 记录并实现一个明确决策：兼容性基线使用 IntelliJ Platform Gradle Plugin 2.x，还是继续保留现有 `org.jetbrains.intellij` 1.x。

#### Scenario: 接受迁移

- **WHEN** 2.x 迁移符合当前项目约束，并且验证命令通过
- **THEN** Gradle 配置使用 2.x 插件 DSL，兼容性文档记录新的构建工具基线

#### Scenario: 暂缓迁移

- **WHEN** 2.x 迁移被工具链、兼容性或验证问题阻塞
- **THEN** Gradle 配置保留现有插件线，兼容性文档记录阻塞原因和重新评估条件

### Requirement: IDEA 兼容范围

项目 SHALL 记录受支持的 IntelliJ IDEA 版本范围，以及它与 `pluginSinceBuild`、`pluginUntilBuild`、开发目标和 Plugin Verifier 矩阵之间的关系。

#### Scenario: 维护者查看支持范围

- **WHEN** 维护者打开 `docs/intellij-compatibility.md`
- **THEN** 文档说明最低支持 IDEA build、选定开发目标、verifier 矩阵，以及插件是否设置显式 upper build bound

#### Scenario: 支持声明超出矩阵

- **WHEN** 未来变更声明支持文档 verifier 矩阵之外的 IDEA 版本
- **THEN** 兼容性指南要求先更新矩阵并重新运行 Plugin Verifier，然后才能接受该支持声明

### Requirement: Java 基线

项目 SHALL 记录 Gradle 运行 JDK、Java source/target compatibility 和目标 IntelliJ IDEA 平台运行时之间的关系，并确保 Java 基线与受支持 IDEA 范围一致。

#### Scenario: 支持范围包含 2024.1

- **WHEN** 兼容范围包含 IntelliJ IDEA 2024.1 / branch 241
- **THEN** Java source/target compatibility 保持 17，且文档说明 2024.2+ 的 Java 21 基线需要单独升级

#### Scenario: 支持范围从 2024.2 开始

- **WHEN** 兼容范围提升到 IntelliJ IDEA 2024.2+ / branch 242+
- **THEN** Java source/target compatibility 使用 21，`pluginSinceBuild` 至少为 242，且 verifier 矩阵不再声明覆盖 241

### Requirement: 兼容性文档

项目 SHALL 更新 `docs/intellij-compatibility.md`，记录现代 IntelliJ IDEA 版本的构建基线、验证命令、Plugin Verifier 矩阵、手动 `runIde` 烟测项和升级纪律。

#### Scenario: 文档匹配构建配置

- **WHEN** 维护者对比 `docs/intellij-compatibility.md` 与 Gradle / properties 配置
- **THEN** 文档中的平台类型、平台版本、Gradle IntelliJ 插件线、bundled plugins、Java 版本、构建命令和 verifier 矩阵与已实现基线一致

#### Scenario: 运行时行为保持不变

- **WHEN** 审查最终实现 diff
- **THEN** OpenSpec artifacts 和文档之外的变更仅限于本基线所需的构建配置和 `DocViewNotification` service 获取时机兼容性修复，且不修改生成 Markdown 模板、上传/导出行为、设置行为或 UI 可见行为

### Requirement: Startup notification service 获取

项目 SHALL 避免在 notification 工具类的类初始化阶段请求 IntelliJ service。

#### Scenario: Startup activity 触发通知

- **WHEN** `DocViewStartupNotification` 在 IntelliJ IDEA 2024.2+ 中触发 startup notification
- **THEN** `DocViewNotification` 不在 `<clinit>` 中调用 `NotificationGroupManager.getInstance()`，而是在发送通知时按需获取 notification group

#### Scenario: 通知内容保持稳定

- **WHEN** 通知 group 改为按需获取
- **THEN** startup notification 和 upload success notification 的标题、内容、图标、action 和触发条件保持不变

### Requirement: 性能基线发布验证

项目 release preparation SHALL 在发布插件前，针对受支持的 IntelliJ IDEA 兼容范围验证性能基线。

#### Scenario: 准备发布版本

- **WHEN** 性能基线实现已准备发布
- **THEN** 维护者在创建 Marketplace artifact 前更新插件版本元数据和 release notes 或 changelog entry

#### Scenario: 运行自动化发布验证

- **WHEN** 维护者准备该基线的插件产物
- **THEN** 运行 `./gradlew test`、`./gradlew buildPlugin` 和 `./gradlew verifyPlugin`，或明确记录跳过原因

#### Scenario: 运行手动 IDE 验证

- **WHEN** 维护者在 `./gradlew runIde` 中验证插件
- **THEN** 验证启动时不再出现已报告的启动错误，并验证 preview generation、refresh、cache clear、export、upload preparation、recursive DTO handling 和 Markdown rendering responsiveness

#### Scenario: 请求 Marketplace 发布

- **WHEN** 维护者发布插件到 JetBrains Marketplace
- **THEN** 只有在自动化验证、手动 IDE smoke testing、版本元数据更新完成，并且维护者明确确认且所需凭据可用后，才执行发布

### Requirement: 启动兼容性错误已修复

Doc View SHALL 避免在 IntelliJ startup 阶段因不安全的 service 请求或重型工作触发启动时报错。

#### Scenario: Startup activity 在受支持 IDEA 上运行

- **WHEN** `DocViewStartupNotification` 或任意 Doc View startup hook 在受支持 IntelliJ IDEA 版本上运行
- **THEN** 它不会在 class initialization 期间触发不安全的 service access
- **AND** 它不会启动重型 PSI scanning、DTO parsing、Markdown rendering、export、upload 或 cache prewarming

#### Scenario: Startup notification 保持稳定

- **WHEN** startup notification 满足展示条件
- **THEN** 现有 notification title、message、actions 和 trigger conditions 保持稳定，仅调整兼容性安全的 service access timing
