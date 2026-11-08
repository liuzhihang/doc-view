# IntelliJ 兼容性

## 1.6.0 兼容范围

1.6.0 的最低 IDEA 为 IU `263.7031.23`，生产 Java 为25（classfile major69）；构建使用 Gradle9.5.0 与 IntelliJ Platform Gradle Plugin2.17.0。自动检查覆盖最低 build 和正式 `263.7307.200`；未设置最高build不表示未来版本均已验证。安装、更新和卸载采用 `require-restart=true`，不承诺热更新/热卸载。完整最终包证据和公开渠道状态见[1.6.0发布记录](release-1.6.0.md)。

维护者已批准本次无法补齐的原生检查留待发布后人工验证/用户反馈；未验证项仍未通过，不批量勾选活动change。旧IDE可继续选择兼容的旧插件版本。

## 历史诊断与升级过程

以下按原候选和日期保留，不作为1.6.0最终包的当前发布结论。

2026-11-09 最新状态：树 UI 修复包 `03f48336…4205b` 通过 414 tests、构建与两个精确 build 的 Verifier。原生 XML/Dubbo 目录与导航、文档库 XML freshness 和三轮资源复验通过；解除树 UI 展开监听器旧路径后，同一 PID19963 真实禁用插件，GC 插件对象记录归零、目标类消失、无类加载器保留告警。store 1.6 与基线 6.3 完成；此前 partial/retry、复制/写回、HTTP 草稿、导出和 Finder 遮挡证据仍按各包记录。HTTP L01–L03、当前包 Send、Schema/预览/凭据等独立原生门槛仍保留，尚不升级或发布。 详见[续验记录](../openspec/changes/http-client-generation-and-ui/verification-2026-11-08-remaining-native.md)。下文保留旧包历史边界。

2026-11-09 生命周期策略：用户批准源 `plugin.xml` 根声明 `require-restart="true"`，让 IDEA 对安装、更新和卸载要求重启；descriptor 测试 3/3 通过。最终 ZIP 的属性读回和隔离 IDEA 原生提示/重启后状态尚未验证。429 包及无 JDWP 对照的动态热卸载失败与 FileChooser/ActionToolbar 根链保留为历史结果，未声称修复。功能已验收后，用户允许将无法执行的原生检查在发布说明中列为未验证，并发布后收集反馈；详情见[已批准选项](../openspec/changes/intellij-263-baseline-upgrade/restart-required-option.md)。

2026-11-08 卸载 API 续验：已新增仅在 Doc View 自身 `beforePluginUnload` 调用公开 `IconLoader.clearCache()` 的修复。新包在正式 263 的无 UI 卸载对照中类加载器已释放；打开目录后仍被 macOS 无障碍 Local JNI 引用，官方新堆报告确认热卸载未通过。未找到可供插件释放该 JNI 根的公开 API 或官方已发布修复。剩余弹窗验收继续保留；构建、测试修正和两 build Verifier 的精确进度见[本轮记录](../openspec/changes/intellij-263-baseline-upgrade/verification-2026-11-08-unload-api.md)。原工作区与 Junie 保留，不发布。下方未清缓存等描述是此前旧包阶段。

本文解释 Doc View 的 IntelliJ Platform、Java、Gradle 和 Plugin Verifier 联动规则。当前值必须从 `gradle.properties`、`build.gradle`、Gradle wrapper 和 `plugin.xml` 读取，不以本文副本替代构建事实。

## 配置来源

2026-11-08 最新整合包：230 项生产/构建输入与已通过 395 tests、EAP `263.7031.23` / 正式 `263.7307.200` 两个 Verifier 的最终清单相同；在正式 build 补齐部分原生功能和有限资源回归。原生 HTTP Client 三种回环请求及参数弹窗 Pin 开启时真实切到 Finder 的遮挡场景通过。热卸载仍失败，官方本机分析分别发现无障碍 JNI、平台图标缓存保留 ClassLoader，未验证修复。诊断运行时为 `JBR-25.0.4.1+1-635.79-nomod`，未替换 JBR 或修改生产基线。完整弹窗/菜单及其余矩阵未完成，R2/4.2 未勾选，最低 build 仍为 `263.7031.23`，未发布。详见[新整合包原生记录](../openspec/changes/intellij-263-baseline-upgrade/verification-2026-11-08-integration-native.md)。下文 full-17 是旧包历史证据，不与整合包混用。

截至 2026-11-07，full-17 已修复七个 Keymap 组的中英文标题，70 suites / 362 tests、构建及 EAP `263.7031.23` / 正式 `263.7307.200` 两个 Verifier 通过，仍有 16 Experimental usages。同包原生已验中英 Keymap、参数 Copy JSON、预览 Copy/Export、中文 Schema 写回与一次 Undo、上传菜单取消、目录 Enter，以及 Pin 开/关跨应用草稿保留和前台交互。三轮关闭后静置约 150 秒，表单、Markdown JCEF panel、RemoteBrowser 和客户端 Java 对象全部归零，FD 无逐轮增长；正常重启/退出后对应 IDEA 与 CEF 进程已消失。完整桌面视觉遮挡、`…`/目录右键菜单可见结果、CEF native peer/listener/性能及其余矩阵仍未通过，R2/4.2 保持未勾选，正式 MCP 不推进。Junie 保留，临时语言与源码已恢复；最低 build 仍为 `263.7031.23`。详情见[full-17 续验记录](../openspec/changes/intellij-263-baseline-upgrade/verification-2026-11-07-full17.md)；full-13/14/16 和下方旧日期均保留为历史阶段。

| 事实 | 权威来源 |
| --- | --- |
| 插件版本、平台类型/版本、since/until build、Verifier 矩阵、Java/Gradle 版本 | `gradle.properties` |
| IntelliJ Platform Gradle Plugin、bundled plugin、verification/publishing DSL | `build.gradle` |
| Gradle distribution | `gradle/wrapper/gradle-wrapper.properties` |
| plugin ID、modules、extensions、actions | `src/main/resources/META-INF/plugin.xml` |

这些值必须一起评估：提高 Java 字节码或使用新 IntelliJ API 时，不能继续声明不兼容的旧 since build；空 `pluginUntilBuild` 也不代表未来 IDE 已被验证。

截至 2026-10-24，已发布的兼容范围仍以已发布插件及其 descriptor 为准。当前工作树已配置未发布候选 IU `263.7031.23`、Java 25 和正式 IPGP 2.17.0；用户已批准该基线调整，但新包原生验收尚未完成，因此不能把候选值描述为已发布支持范围。平台/Verifier 实际目标已读回为 `IU-263.7031.23`，Java 25 配置检查、`--release 25` 参数和 full-10 ZIP 中 329 个 major 69 生产 class 已通过核验。IPGP 2.17.0（release `61cf376`，2026-06-26）更新 build 262 起的 Java 映射；它只升级构建期工具，不增加生产运行时依赖。

`263-full-10` 自动构建成功，67 个 suite 的 352 个测试全部通过（0 failures/errors/skips），18 个 Gradle tasks 全部执行，用时 1 分 55 秒。测试源码编译另有 15 条 test-only deprecated/removal warnings，不能将结果表述为全项目零警告。Plugin Verifier 对精确目标报告 Compatible，production 报告无 deprecated、internal、override-only 或 compatibility 问题；报告仍列出 16 处 Experimental API 使用。full-10 ZIP SHA-256 为 `b238f5412d134ff4311ba5422fb78d50562bfecf80826964858984d5d69bcc0b`，大小 722,118 bytes；嵌套 JAR SHA-256 为 `b3615cd1f5a44de8dd472c3f2c8715ecd4126fe6892374fc307a5a1d7ca6f267`。产品元数据读回目标为 `IU-263.7031.23`；插件 descriptor 与构建元数据读回插件 ID/version 和 `since-build=263.7031.23`，ZIP 不含测试依赖，329 个生产 class 均为 major 69。full-9 的 350、full-8 的 349、full-7 的 344 项全绿作为历史结果保留；full-5 与 targeted-6 的失败记录也保留，targeted-7 已通过 51 个针对性测试。full-10 当时原生回归只有部分代表路径通过；其 Copy/Export 等未验描述保留为历史，后续 full-17 已补验项及剩余边界见首段，不混合不同包的完整矩阵。正式 Doc View MCP 仍受原生回归和项目文档库验收约束。详细日志和范围见[基线升级验证记录](../openspec/changes/intellij-263-baseline-upgrade/verification.md)；原生回归通过前，不将 IU 263/Java 25 写成已完成支持声明。

## 版本演进策略

2026-10-18 用户明确：新版本按新的功能与实现方向直接演进，不为旧版功能、旧代码写法或旧 JCEF 环境额外设计兼容逻辑。用户升级插件使用新版；需要旧行为时可自行回退插件版本。

- 只实现当前 change 批准的版本 contract，不额外保留旧模板渲染路径、旧变量别名、旧设置入口或旧格式适配层，也不为保持旧效果增加模式开关。
- 配置或格式改变时说明新版使用方式，需要时由用户重新配置；不建设跨版本迁移或降级读写逻辑，不承诺旧插件能读取新版产生的配置与文档。
- 需要新的 IntelliJ/JCEF API 时直接使用目标版本支持的实现，并准确声明最低 IDE 版本与依赖；不为旧 IDE 增加反射分支、备用浏览器或第二套渲染器。若运行环境不满足必要条件，按平台要求检查并明确提示即可。
- 当前版本的 Apply/Cancel、持久化、资源释放、Dumb Mode、取消和错误反馈仍属于正常功能与稳定性要求，不因取消旧版兼容而省略。
- 不要求兼容不等于每次都要改动现有模板、配置或 API；只在当前 change 的目标需要时调整。不得借此清空用户数据、扩大运行时修改范围或跳过验证。

旧的设计稿如包含额外兼容要求，按本决策修订；运行时变化仍先写入对应 OpenSpec change。当前构建参数不因这条原则自动改变，实际升级目标与验证结果在各自 change 中确定。

## API 与线程兼容

- 优先使用公开 documented API，不新增 internal API。
- 使用新 API 前确认其最低 build；替换 deprecated API 时以当前 change 的可观察 contract 为准，不为旧 API 保留平行实现。
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
