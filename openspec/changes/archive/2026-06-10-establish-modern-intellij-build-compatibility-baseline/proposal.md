## 背景

Doc View 当前以 IntelliJ IDEA 2024.1 为目标平台，并使用旧版 `org.jetbrains.intellij` Gradle 插件。为了持续兼容新版 IDEA，需要建立一套明确、可重复、可文档化的构建与兼容性基线。

在继续开展更深入的功能开发前，先稳定构建、沙箱 IDE 运行、插件打包、Plugin Verifier 覆盖范围和 IDEA 支持声明，可以降低后续升级风险。

## 变更内容

- 检查当前 Gradle IntelliJ 配置，并记录当前基线、缺口和兼容性假设。
- 评估是否从 `org.jetbrains.intellij` 1.16.1 迁移到 IntelliJ Platform Gradle Plugin 2.x。
- 为选定基线配置或确认可靠的 `runIde`、`buildPlugin`、`verifyPlugin` 工作流。
- 定义覆盖受支持 IDEA 构建范围的 Plugin Verifier 验证矩阵。
- 明确支持的 IDEA 版本范围，包括 `sinceBuild` 和 `untilBuild` 策略。
- 评估 Java 基线是否需要从 17 升级到 21，并区分 Gradle 运行 JDK、编译 target 和目标 IDEA 平台运行时要求。
- 修复 2024.2+ 下 startup notification 触发的 service 静态初始化兼容性问题。
- 更新 `docs/intellij-compatibility.md`，记录最终兼容性基线、验证命令、手动检查项和升级规则。
- 除非兼容性所必需的构建配置调整明确要求，否则不引入运行时功能变化。

## 能力

### 新增能力
- `intellij-build-compatibility-baseline`：定义维护 Doc View 面向现代 IntelliJ IDEA 版本时所需的构建、运行、打包、Plugin Verifier 和 IDEA 兼容性基线。

### 修改能力

无。

## 影响

- 如果实现阶段接受 IntelliJ Platform Gradle Plugin 2.x 迁移，Gradle 构建配置可能发生变化。
- `gradle.properties` 中的兼容性字段可能会根据选定 IDEA 基线和验证矩阵调整。
- 如果支持范围移动到 IDEA 2024.2+，Java 编译基线可能从 17 调整到 21。
- `DocViewNotification` 会延迟获取 `NotificationGroupManager`，避免类初始化阶段依赖 IntelliJ service。
- `docs/intellij-compatibility.md` 将更新支持的 IDEA 范围和验证流程。
- OpenSpec artifacts 会在任何构建系统变更前定义实现和验证任务。
- 本基线工作不计划修改 Java 生产代码、测试代码、插件运行时行为、模板、图标、message bundle 或 UI 行为。
