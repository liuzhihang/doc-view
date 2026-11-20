# 2026-11-20 稳定版最低版本调整与远程工件验证

## 结论

1.7.2 候选的最低目标已从过期的 IU `263.7031.23` EAP 调整为正式 IntelliJ IDEA Ultimate `263.7307.200`。本 change 只声明精确最低 build；更早的 IU 不属于 1.7.2 支持范围，未来 build 也不会因开放的 `until-build` 自动成为已验证目标。已发布 1.7.1 包的 `since-build=263.7031.23` 不变。

过期 EAP 的实际启动失败见[历史记录](verification.md#最低支持-eap-原生重验受阻2026-11-20)。那次记录没有修改系统时间、许可或普通 IDE 设置。本次不再以 EAP 作为新版本最低运行目标。

## 官方 Gradle 工件与候选包

- `build.gradle` 使用 IntelliJ Platform Gradle Plugin 的 `defaultRepositories()`；本次由标准 Gradle 解析取得坐标 `com.jetbrains.intellij.idea:idea:263.7307.200`，没有使用 `local(...)` SDK override。缓存工件路径为 `/Users/liuzhihang/.gradle/caches/modules-2/files-2.1/com.jetbrains.intellij.idea/idea/263.7307.200/8fc7fb9eb86a2b630b918771298909116610dfef/idea-263.7307.200.zip`，SHA-256 `59d424bc14f47518aa7b31c7106d9e581e69c035795b36e6859a94d785f0be12`。内含 `product-info.json` 读回 `productCode=IU`、`version=2026.3`、`buildNumber=263.7307.200`。
- 使用 JetBrains Runtime `25.0.4.1`；命令以 `-PplatformType=IU` 和三个精确 build 属性 `-PplatformVersion=263.7307.200`、`-PpluginSinceBuild=263.7307.200`、`-PpluginVerifierIdeVersions=263.7307.200` 覆盖目标；完整输出见 ignored 文件 `build/openspec-baseline-263-stable-20261120/remote-build-verifier.log`。
- `verifyPluginProjectConfiguration`、`buildPlugin`、`verifyPlugin` 均通过；`BUILD SUCCESSFUL`，13 个任务实际执行，用时 9 分 27 秒。该次构建生成 1.7.2-SNAPSHOT 评估包，不含 `test` 任务。
- 评估 ZIP SHA-256 `df269892b40d71bedb0fdd5ba3e4dd608cd25712ef73d5c70cb2148f3818034d`，JAR SHA-256 `2e2a1e607f3c1e5593001c09b9a0993790b473d9eaa9615be912257d9bc643c3`。嵌套 `META-INF/plugin.xml` 读回 `since-build=263.7307.200`、`require-restart=true`；418 个 class 全部为 major 69。评估包副本位于 `build/openspec-baseline-263-stable-20261120/remote-artifacts/`。
- Plugin Verifier 1.411 在 `IU-263.7307.200` 上报告 `Compatible`，列有 16 项 Experimental API；没有插件兼容性、Internal、Deprecated 或 Override-only 问题。原始报告位于 `build/reports/pluginVerifier/IU-263.7307.200/report.html`。
- Verifier 日志另有 162 条 `Layout component ... nonexistent classPath` warning，指向下载版 IDE `product-info.json` 中列出的平台组件路径；这没有改变 Compatible verdict，但保留为需要与最终验证器运行交叉核对的环境观察。构建尾部还提示存在 Gradle 10 不兼容的弃用 feature，当前日志没有定位具体来源。

上述 1.7.2-SNAPSHOT ZIP 在本轮描述文本更新前生成，其中仍含旧的 Skill 客户端说明，因此不作为发布包。此后 `parts/description.html` 已更新至 Codex `0.169.0-alpha.11.1` 和官方 Doc View Skill `1.7.1` 资产；正式版本 `1.7.2` 的最终 ZIP 需重建并重新读回 hash、descriptor、字节码和 Verifier 结果。全量测试由主任务在最终源码/正式版本包上另行执行；本轮不能代替它。

## 前序本机 SDK 评估和原生记录

先前的本机官方 SDK 评估包 SHA-256 为 `408e598592e8657a59d5e3e5ed936b79721f6b85687cfbf9bfe0e8e66d4020c2`，曾执行 99 suites / 581 tests，0 failures/errors/skips，并完成代表性的参数写回、Undo、XML/Dubbo 预览和导出。此证据来自另一候选包及 `local(...)` SDK，不能替代本次远程 Gradle 工件解析，也不能冒充最终 1.7.2 包的全量回归。对应产物见 `build/baseline-minimum-2637307-assessment/` 和 `build/openspec-closure-20261120/stable-smoke-*`。

最终版本测试、最终 ZIP 的安装身份和完整原生矩阵仍按主任务结果记录；R2/4.2 中未执行的场景继续保持未完成。EAP 的旧原生证据不因升级最低版本而重标为稳定版结果。

## 正式版本 1.7.2 最终 R2 包的构建读回

- 在固定的官方 IU `263.7307.200` SDK 和 Java 25 上执行 `./gradlew --offline --no-configuration-cache test buildPlugin verifyPluginProjectConfiguration verifyPlugin --rerun-tasks --no-build-cache --no-parallel`。18 个任务实际执行。99 套件 / 584 项测试，0 失败、错误、跳过。构建与配置检查均通过，完整日志在 `build/release-1.7.2-20261120/release-final-r2.log`。
- Plugin Verifier 1.411 在 `IU-263.7307.200` 报告 Compatible，列有 16 项 Experimental API。162 条 IDE SDK 布局路径 warning、测试编译的 20 条弃用 warning 及 Gradle 10 弃用提示继续保留。`263.7031.23` 属于前序已过期基线，旧包的报告不计入 R2。
- ZIP SHA-256 `7eb07fa4e744efe7e30bf4026b78aae3de1d21286a1caaf386bb8c5ac226e5a5`，主 JAR SHA-256 `681eb66c2d41f316c77878ea857a1b0cd494f8c750cf5c979d2c7944ba5f1fbb`。内嵌 descriptor 为 `version=1.7.2`、`since-build=263.7307.200`、`require-restart=true`，无 `until-build`；418 个运行时类的 major 均为 69，发行包没有测试或 PoC 类。两份候选 JAR 的 Java 类字节一致，R2 只换了插件 descriptor 和中英文资源 bundle。

## 正式发行包 R3 的最终核对

发布前检查发现 R2 内嵌中英插件介绍仍写着 1.7.2 候选。只更新 `parts/description.html`，并重跑构建、全量测试、配置检查及 Plugin Verifier。首次从默认 Java 21 环境启动，Gradle 找不到 25 toolchain；显式使用 IDEA JBR `25.0.4.1` 作为 `org.gradle.java.home` 后复验通过。首次失败与成功日志分别为 `release-final-r3-jdk21-env.log`、`release-final-r3.log`，位于 `build/release-1.7.2-20261120/`。

- 18 个任务实际执行，99 suites / 584 tests，0 失败、错误或跳过；IU `263.7307.200` Verifier 为 Compatible，保留 16 项 Experimental API、162 条 SDK 布局警告及测试编译弃用警告。
- 发行 ZIP SHA-256 `c372c22e1773a0ae1487f2de4d08060edb690cf9fed0c17282c12750ac0dacbc`（951,120 bytes），主 JAR SHA-256 `f8de3a0b8dbea35c5357898d30b2f738700dd585b9f507dfae9b56a2fd7e806f`。外层 ZIP 只有该 JAR；418 个运行时类都是 major 69。
- 逐项读回确认 R3 相对 R2 仅 `META-INF/plugin.xml` 的 `<description>` 内容变化。删除该元素后两份 descriptor 一致，其他 archive entries 的内容逐字一致，包含全部 Java class、actions 注册、资源、服务依赖、版本和最低 build。R2 原生行保留其真实工件身份，不重记为 R3 原生实测。
