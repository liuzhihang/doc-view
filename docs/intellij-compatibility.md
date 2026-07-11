# IntelliJ 兼容性

本文解释 Doc View 的 IntelliJ Platform、Java、Gradle 和 Plugin Verifier 联动规则。当前值必须从 `gradle.properties`、`build.gradle`、Gradle wrapper 和 `plugin.xml` 读取，不以本文副本替代构建事实。

## 配置来源

| 事实 | 权威来源 |
| --- | --- |
| 插件版本、平台类型/版本、since/until build、Verifier 矩阵、Java/Gradle 版本 | `gradle.properties` |
| IntelliJ Platform Gradle Plugin、bundled plugin、verification/publishing DSL | `build.gradle` |
| Gradle distribution | `gradle/wrapper/gradle-wrapper.properties` |
| plugin ID、modules、extensions、actions | `src/main/resources/META-INF/plugin.xml` |

这些值必须一起评估：提高 Java 字节码或使用新 IntelliJ API 时，不能继续声明不兼容的旧 since build；空 `pluginUntilBuild` 也不代表未来 IDE 已被验证。

## API 与线程兼容

- 优先使用公开 documented API，不新增 internal API。
- 使用新 API 前确认其最低 build；替换 deprecated API 时保持可观察 contract。
- action、tool window、settings 和 startup extension 处理 Dumb Mode、空 project/editor、文件失效与 project dispose。
- PSI read/write action 和 EDT 边界同时属于兼容性与稳定性 contract。
- 修改 `plugin.xml`、bundled plugin 或 extension 必须运行 descriptor/configuration、构建和 Verifier 验证。

## Plugin Verifier

`pluginVerifierIdeVersions` 是声明“已验证版本”的矩阵；发布支持新的 major/minor 前必须加入矩阵，并按 [发布检查清单](release-checklist.md)执行配置、构建、Verifier 和必要手工验证。

`pluginVerifierOffline=true` 只关闭 documented API change 的在线查询，不跳过二进制兼容、internal/deprecated API 扫描。

截至 2026-07-11，1.3.13 对配置的 IU 2026.1 build `261.22158.277` 验证为 Compatible，报告不再包含 compatibility、internal 或 deprecated API 问题。此前的 7 处 deprecated usage 已按同语义公开 API 清理：

- Spring、Feign、Dubbo 扫描使用 `JavaAnnotationIndex.getAnnotations(...)`，short name、module scope 和 FQN 过滤保持不变。
- tool window 使用 `uiDataSnapshot(DataSink)` 提供既有 `DataKey`，目录树搜索通过 `TreeUIHelper` 安装。
- 重复名称使用 `RandomStringUtils.secure().nextAlphabetic(5)`，随机源和名称格式保持不变。

`build.gradle` 将 compatibility problem、internal API usage、override-only API usage 和 deprecated API usage 配置为 Verifier 失败级别，后续重新引入同类调用会使 `verifyPlugin` 失败。Java 编译、测试成功不能替代这项二进制检查。

仓库中的正式验证矩阵仍由固定的 `pluginVerifierIdeVersions` 定义。额外的预发布或未来 IDE 只能通过命令行属性使用精确 build 做前向验证；一次性前向结果不自动扩大支持声明，也不替代正式矩阵的发布验证。

2026-07-11 使用命令行覆盖对 IU `261.22158.277` 与 `262.8665.176` 执行一次前向验证，两者 verdict 均为 Compatible，且未报告 deprecated、internal 或 compatibility 问题；仓库属性仍只声明 `2026.1` 正式矩阵。

## 升级流程

1. 创建兼容性 OpenSpec change，记录旧值、目标值、API 依据、非目标和回滚。
2. 更新 Gradle/plugin 配置和必要 Java API，保持 Java-only。
3. 按发布检查清单选择并运行自动验证与 `runIde` 场景。
4. 更新 changelog 和受影响的 canonical docs。
5. 在 change/release notes 记录实际目标 IDE build、验证结果、跳过项和回滚说明。

不要把 `test` 成功等同于 UI 或兼容性验证；命令和手工 smoke matrix 只在 [发布检查清单](release-checklist.md)维护。
