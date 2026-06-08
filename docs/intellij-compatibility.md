# IntelliJ Compatibility

本文档记录 Doc View 与 IntelliJ Platform 的兼容策略。任何修改 `plugin.xml`、Gradle IntelliJ 配置、IDE API、action、tool window、settings、PSI 或 DOM 扩展的变更都必须参考本文档。

## 当前基线

- `platformType=IU`
- `platformVersion=2024.1`
- `pluginSinceBuild=241`
- `pluginUntilBuild=` 空，表示不主动限制上限
- IntelliJ Gradle Plugin：1.16.1
- Java source/target compatibility：17
- Bundled plugins：`com.intellij.java`、`markdown`
- `buildSearchableOptions.enabled=false`
- `updateSinceUntilBuild=false`

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

## PSI API 使用

PSI 相关兼容性要求：

- 不依赖不稳定 internal API，除非没有替代方案且 design 中说明风险。
- read/write action 边界必须清晰。
- 对 `PsiElement#isValid`、`Project#isDisposed`、containing file 等状态做防御。
- 泛型、注解、Javadoc、XML DOM 的兼容性需要在样例中体现。

## 升级流程

升级 IntelliJ 平台或 Gradle IntelliJ Plugin 时：

1. 创建独立 OpenSpec change。
2. 记录旧版本和目标版本。
3. 检查 JetBrains API 变更、废弃 API 和插件验证告警。
4. 更新 `gradle.properties` 或 `build.gradle`。
5. 运行 `./gradlew test`。
6. 运行 `./gradlew verifyPlugin`。
7. 运行 `./gradlew runIde`，手动验证核心流程。
8. 更新 `CHANGELOG.md` 和 release checklist。

## 手动验证矩阵

核心流程至少覆盖：

- 在 Java Controller 方法上触发 `Doc View`。
- 在 Dubbo Service 方法上触发 `Doc View`。
- 打开右侧 tool window 并刷新目录。
- 打开 preview、复制、导出。
- 打开 settings、template settings、YApi/ShowDoc/YuQue settings。
- 验证 line marker 显示和隐藏设置。
- 验证项目关闭或切换后无异常。

## 发布前兼容性检查

- `./gradlew verifyPlugin` 无阻断级问题。
- `pluginSinceBuild` 与目标平台匹配。
- `pluginUntilBuild` 策略有明确说明。
- 新 API 使用有最小目标版本依据。
- Marketplace 描述和 changelog 不夸大兼容范围。

## 回滚

兼容性升级出现问题时：

- 回滚平台版本、Gradle IntelliJ Plugin 版本和相关 API 修改。
- 保留可复现错误信息。
- 不在同一个修复中夹带功能重构。
