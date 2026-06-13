# IntelliJ 兼容性

本文档记录 Doc View 与 IntelliJ Platform 的构建、运行、打包和兼容性验证策略。任何修改 `plugin.xml`、Gradle IntelliJ 配置、IDE API、action、tool window、settings、PSI 或 DOM 扩展的变更都必须参考本文档。

主要外部参考：

- JetBrains IntelliJ Platform Plugin SDK：https://plugins.jetbrains.com/docs/intellij/welcome.html
- IntelliJ Platform Gradle Plugin 2.x 文档：https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
- IntelliJ Platform build number ranges：https://plugins.jetbrains.com/docs/intellij/build-number-ranges.html

## 当前基线

- `platformType=IU`
- `platformVersion=2026.1`
- `pluginSinceBuild=261`
- `pluginUntilBuild=` 空，表示 plugin.xml 不主动限制上限
- IntelliJ Platform Gradle Plugin：`org.jetbrains.intellij.platform` 2.16.0
- Gradle Wrapper：9.5.0
- Java source/target compatibility：21
- Gradle 运行 JDK：建议使用 Java 21；本地 `buildPlugin` 验证使用 Homebrew OpenJDK 21.0.7
- Bundled plugins：`com.intellij.java`、`org.intellij.plugins.markdown`
- `buildSearchableOptions=false`
- Plugin Verifier 默认矩阵：`2026.1`
- Plugin Verifier 默认使用 `pluginVerifierOffline=true`，避免本地证书链或代理环境导致 documented API change 页面抓取失败。

## 支持范围

Doc View 当前兼容基线从 IntelliJ IDEA 2026.1 / branch `261` 开始。`pluginUntilBuild` 暂时保持空值，这表示 plugin descriptor 不主动设置 upper build bound；实际已验证范围以本文档中的 Plugin Verifier 矩阵为准。

维护规则：

- 声明支持新的 IDEA major/minor 版本前，必须先把该版本加入 `pluginVerifierIdeVersions` 并运行 `./gradlew verifyPlugin`。
- 如果 Plugin Verifier 对某个未来版本报告阻断级 API 或 descriptor 问题，必须修复问题或设置明确的 `pluginUntilBuild`，不能只依赖空上限。
- 如果未来需要重新支持 2025.x 或更早版本，必须单独评估目标平台 API、Java 21 字节码、IntelliJ Platform Gradle Plugin 2.x 支持和 verifier 矩阵，不得在当前基线下直接声明兼容。
- 可按发布节奏把后续 `2026.2` 等版本逐步加入矩阵；首次加入会下载较大的 IDEA artifacts，适合在网络稳定的本地或 CI 环境执行。

## Java 基线

当前基线使用 Java 21，这是因为 IntelliJ IDEA 2026.1+ 对应 branch 261+，平台运行时保持 Java 21 基线。这里需要区分三件事：

- Gradle 运行 JDK：运行 Gradle wrapper 的 JDK，推荐 Java 21。
- Java source/target compatibility：插件编译字节码级别，当前为 21。
- 目标 IDEA 平台运行时：`platformVersion=2026.1` 及 verifier 矩阵中的 IDEA 版本。

维护规则：

- `javaVersion`、`pluginSinceBuild`、`platformVersion` 和 verifier 矩阵必须一起评估。
- Java 21 字节码不得声明兼容低于当前支持基线的 IDE 版本。
- 如果后续 IDEA 平台升级要求更高 Java 版本，必须先通过独立变更说明和 contract 定义兼容范围和验证矩阵。

## Gradle 配置

当前使用 IntelliJ Platform Gradle Plugin 2.x DSL：

- `repositories.intellijPlatform.defaultRepositories()` 提供 JetBrains 平台依赖仓库。
- `dependencies.intellijPlatform.create(platformType, platformVersion)` 解析目标 IDEA。
- `dependencies.intellijPlatform.bundledPlugin(...)` 声明 bundled Java 和 Markdown 插件。
- `intellijPlatform.pluginConfiguration.ideaVersion` 写入 `sinceBuild` / `untilBuild`。
- `intellijPlatform.pluginVerification.ides` 使用 `pluginVerifierIdeVersions` 配置验证矩阵。
- `intellijPlatform.pluginVerification.failureLevel` 当前只将 `COMPATIBILITY_PROBLEMS` 作为构建失败条件；deprecated API usage 仍需在发布前评估，internal API usage 必须修复或记录 JetBrains 明确认可的例外。
- `tasks.verifyPlugin.offline` 由 `pluginVerifierOffline` 控制，默认开启。该设置只让 Plugin Verifier 跳过在线抓取 JetBrains documented API change 页面，避免 `PKIX path building failed` 这类本地证书链错误；不会跳过插件二进制兼容性验证，也不会忽略 internal API usage。需要在线抓取 documented problems 过滤信息时，可临时运行 `./gradlew verifyPlugin -PpluginVerifierOffline=false`。

`.intellijPlatform` 是 2.x 插件使用的本地平台缓存目录，必须保留在 `.gitignore` 中。

## plugin.xml 依赖

当前 descriptor 声明：

- `com.intellij.modules.platform`
- `com.intellij.modules.java`
- `org.intellij.plugins.markdown`

维护规则：

- 新增依赖前必须说明功能需要和降级策略。
- 如果只用于开发或测试，不应加入 runtime plugin dependency。
- Markdown 能力变化要验证预览、编辑和模板相关流程。
- Java 插件依赖变化要验证 PSI 解析入口。

## Extension 和 Action 兼容性

当前注册内容包括：

- application service 和 project service
- startup activity
- Java line marker provider
- project configurable
- tool window
- notification group
- DOM metadata 和 scoped search
- editor popup、upload、preview、tool window toolbar、catalog menu actions

维护规则：

- 修改 extension 前检查目标平台 API 是否废弃。
- action `update` 必须轻量，并处理 Dumb Mode、无项目、无编辑器、文件失效。
- tool window factory 应处理项目生命周期。
- settings configurable 变更要验证打开、保存、取消、默认值和持久化。
- 工具类的 class initializer 不得请求 IntelliJ service；例如 notification group 应在实际发送通知时按需获取。

## PSI API 使用

PSI 相关兼容性要求：

- 不依赖不稳定 internal API，除非没有替代方案且 design 中说明风险。
- read/write action 边界必须清晰。
- 对 `PsiElement#isValid`、`Project#isDisposed`、containing file 等状态做防御。
- 泛型、注解、Javadoc、XML DOM 的兼容性需要在样例中体现。

## 1.3.12 兼容性 Contract

本次兼容性修复的外部输入是 JetBrains Marketplace Plugin Verifier 报告的 internal API usage：`ActionToolbarImpl` 以及 `ActionToolbarImpl.setForceMinimumSize(boolean)`。

期望结果：

- 生产代码不再 import 或强转 `com.intellij.openapi.actionSystem.impl.ActionToolbarImpl`。
- 预览弹窗和参数编辑弹窗继续通过 `ActionManager#createActionToolbar(...)` 创建 toolbar，并只依赖公开的 `ActionToolbar`、`ToolbarLayoutStrategy` 和 Swing component API。
- `pluginSinceBuild=261`，`platformVersion=2026.1`，声明兼容范围从 IntelliJ IDEA 2026.1+ 开始。
- 不改变 PSI 解析、DTO、Markdown、上传、导出、设置持久化或模板行为。

验证路径：

- 静态检查 `ActionToolbarImpl` 和 `setForceMinimumSize` 不再出现于 `src/main/java`。
- 运行 `./gradlew test`、`./gradlew buildPlugin`、`./gradlew verifyPlugin`。
- 发布前在 `./gradlew runIde` 中手动覆盖 preview、参数编辑、复制、导出、上传入口和 settings 打开流程。

## 常用验证命令

```bash
# 确认 Gradle、Java、任务注册和 2.x DSL
./gradlew --version
./gradlew tasks --all
./gradlew verifyPluginProjectConfiguration

# 编译并运行自动化测试；当前 test sources 是手动示例类，允许 0 个自动测试被发现
./gradlew test

# 构建插件安装包
./gradlew buildPlugin

# 按配置矩阵运行 Plugin Verifier
./gradlew verifyPlugin

# 如需启用在线 documented API change 过滤
./gradlew verifyPlugin -PpluginVerifierOffline=false

# 启动沙箱 IDE，进行手动烟测
./gradlew runIde
```

在本机验证时建议确保 Gradle 使用可加载 AWT/JBR 相关 native library 的 Java 21。例如，本机 `buildPlugin` 使用 Homebrew OpenJDK 21.0.7 验证通过：

```bash
JAVA_HOME=/opt/homebrew/Cellar/openjdk@21/21.0.7/libexec/openjdk.jdk/Contents/Home ./gradlew buildPlugin
```

如果某个 JDK 的 `libawt.dylib` 被 macOS system policy 拒绝加载，`instrumentCode` 可能失败；应切换到可正常加载 native library 的 Java 21 发行版后重跑验证。

## 手动 runIde 烟测矩阵

`./gradlew runIde` 启动沙箱 IDE 后，至少覆盖：

- 在 Java Controller 方法上触发 `Doc View`。
- 在 Dubbo Service 方法上触发 `Doc View`。
- 打开右侧 tool window 并刷新目录。
- 打开 preview、复制、导出。
- 打开 settings、template settings、YApi/ShowDoc/YuQue settings。
- 验证 line marker 显示和隐藏设置。
- 验证项目关闭、重新打开或切换后无异常。

## 升级流程

升级 IntelliJ 平台、Gradle Wrapper、Java 基线或 IntelliJ Platform Gradle Plugin 时：

1. 创建独立变更说明和 contract。
2. 记录旧版本和目标版本。
3. 检查 JetBrains API 变更、废弃 API、Java runtime 要求和插件验证告警。
4. 更新 `gradle.properties`、`build.gradle`、Gradle wrapper 或相关文档。
5. 运行 `./gradlew test`。
6. 运行 `./gradlew buildPlugin`。
7. 运行 `./gradlew verifyPlugin`。
8. 运行 `./gradlew runIde`，手动验证核心流程。
9. 更新 `CHANGELOG.md` 和 release checklist。

## 发布前兼容性检查

- `./gradlew buildPlugin` 成功生成插件安装包。
- `./gradlew verifyPlugin` 无阻断级问题。
- Plugin Verifier 报告中的 deprecated/internal API usage 已记录，且不包含 compatibility problem。
- `pluginSinceBuild` 与 Java 字节码和目标平台匹配。
- `pluginUntilBuild` 策略有明确说明。
- Plugin Verifier 矩阵覆盖计划声明支持的 IDEA 版本。
- 新 API 使用有最小目标版本依据。
- Marketplace 描述和 changelog 不夸大兼容范围。

## 回滚

兼容性升级出现问题时：

- 回滚 Gradle Wrapper、IntelliJ Platform Gradle Plugin 版本、平台版本、Java 基线和相关 API 修改。
- 保留可复现错误信息。
- 不在同一个修复中夹带功能重构。
