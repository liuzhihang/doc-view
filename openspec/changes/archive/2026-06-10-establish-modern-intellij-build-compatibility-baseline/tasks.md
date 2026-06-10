## 1. 基线盘点

- [x] 1.1 检查当前 Gradle、`gradle.properties`、plugin metadata 和现有 IntelliJ 兼容性文档。
- [x] 1.2 记录当前平台类型、平台版本、Java 版本、bundled plugins、`sinceBuild`、`untilBuild`、Gradle 版本和 IntelliJ Gradle 插件线。
- [x] 1.3 查阅 JetBrains 官方 IntelliJ Platform Plugin SDK 文档，核对当前构建、运行、打包和 Plugin Verifier 指南。
- [x] 1.4 确认当前可用的 Gradle 任务，包括 `runIde`、`buildPlugin` 和 `verifyPlugin`。

## 2. IntelliJ Platform Gradle Plugin 决策

- [x] 2.1 评估从 `org.jetbrains.intellij` 1.16.1 迁移到 IntelliJ Platform Gradle Plugin 2.x DSL 的路径。
- [x] 2.2 如果迁移符合项目约束且验证可以通过，则实现 2.x 迁移；否则记录阻塞原因并保留现有插件线。
- [x] 2.3 确认构建工具决策后，bundled Java 和 Markdown 插件声明仍保持等价。

## 3. 构建与 Verifier 配置

- [x] 3.1 使用选定 IDEA 类型、目标版本、沙箱行为和 JVM 参数配置或确认 `runIde`。
- [x] 3.2 基于选定 IntelliJ 构建基线配置或确认 `buildPlugin` 打包。
- [x] 3.3 配置 Plugin Verifier，使其针对显式 IDEA 兼容性矩阵运行。
- [x] 3.4 定义支持的 IDEA 范围，并决定 `pluginUntilBuild` 继续留空还是设置上限。
- [x] 3.5 根据支持范围评估并配置 Java source/target compatibility。

## 4. 文档

- [x] 4.1 更新 `docs/intellij-compatibility.md`，记录选定构建工具基线、IDEA 支持范围、verifier 矩阵和命令。
- [x] 4.2 添加手动 `runIde` 烟测项，覆盖 Controller、Dubbo Service、tool window、preview/copy/export、settings 和 line marker 可见性。
- [x] 4.3 记录新增新版 IDEA 支持时更新 verifier 矩阵的规则。

## 5. 验证

- [x] 5.1 运行 `./gradlew buildPlugin` 并记录结果。
- [x] 5.2 运行 `./gradlew verifyPlugin` 并记录结果，包括任何非阻断 warning。
- [x] 5.3 尝试运行 `./gradlew runIde`，并记录启动是否已在本地验证，或是否需要维护者手动验证。
- [x] 5.4 运行 `openspec status --change "establish-modern-intellij-build-compatibility-baseline"`，确认 change 仍处于 apply-ready 状态。
- [x] 5.5 检查 `git diff --name-only`，确认最终实现只触及预期的 OpenSpec、文档、构建兼容性文件和兼容性 Java 修复文件。

## 6. 2024.2+ 运行时兼容性修复

- [x] 6.1 修复 `DocViewNotification` 类初始化期间请求 `NotificationGroupManager` service 的问题。
- [x] 6.2 确认通知 group 改为按需获取后，通知文案、图标、action 和触发条件保持不变。
