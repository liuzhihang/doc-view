# Design

## Context

见 `proposal.md` 的 Why。原正式构建基线为 `platformType=IU`、`platformVersion=2026.1`、`pluginSinceBuild=261`、`pluginVerifierIdeVersions=2026.1`、`javaVersion=21`，Gradle wrapper 为 9.5.0。IU `263.7031.23` 的隔离探针结果不代表生产插件已在该 build 完整回归；实际 Codex 客户端取消闭环是运行时升级的前置条件。IU 263 要求 Java 25，用户已明确批准生产源码、API 与 classfile 一并升级到 Java 25 / major 69，并使用 JDK 25 toolchain。

`pluginUntilBuild` 当前为空，沿用现有开放上限策略。本 change 的 Verifier 与原生证据只覆盖明确的 IU `263.7031.23` build，不把未来 build 或其他产品写成已验证。

## Goals / Non-Goals

**Goals:**

- 先用实际 Codex 客户端通过临时 app-server MCP 配置和 turn interrupt 证明主动取消、真实服务端通知、任务清理和同 session 恢复，再允许修改生产基线；官方 TypeScript SDK 只作补充。
- 目标平台工件固定使用 `263.7031.23-EAP-SNAPSHOT` 解析输入；最低 build 固定为 `263.7031.23`。Plugin Verifier 使用同一固定工件输入，并必须从实际解析/报告中读回 `IU-263.7031.23`。
- 将生产源码/API/字节码升级为 Java 25 / major 69，使用 JDK 25 javac toolchain，保留 Gradle 9.5.0 和当前功能行为；通过完整自动验证和隔离 IDEA 原生回归检查版本变化。
- 构建期 IntelliJ Platform Gradle Plugin 固定升级到正式发布版 `2.17.0`，以采用 build 262 起的 Java 25 映射；这是 Gradle 构建工具，不加入插件运行时依赖。
- 将旧 IDEA build 的不支持范围和实际验证范围写清楚，并保留现有 HTTP/UI 待验状态。

**Non-Goals:**

- 不在本 change 添加 Doc View 正式 MCP 搜索/详情工具或 MCP provider。
- 不增加 Kotlin、反射适配、旧 API 分支、低版本兼容或多平台双轨实现。
- 不把 HTTP Client 执行许可、双项目/清缓存、取消/性能或 UI 外观的既有缺口标为完成。
- 不发布、推送、打 tag 或自动归档 OpenSpec change。
- 不把动态热卸载作为本次发布生命周期承诺；用户批准用公开 `require-restart=true` 声明安装、更新和卸载均需重启。历史热卸载失败保留，不表述为已修复。

## Decisions

### 2026-11-05：正式版补验与 XML 依赖补齐

官方发布元数据与本机安装的 `product-info.json` 均为正式 `IU-263.7307.200`。先将它作为额外精确 Verifier/native 目标；原 `263.7031.23` 构建输入与最低 build 不变，历史结果不重标为正式版结果。full-10 ZIP 的正式版 Verifier 首轮报出 13 处 XML/DOM unresolved；现有 `BeansDescription` 和 `DubboPsiUtils` 使用 bundled XML API，descriptor 没有声明其 owner plugin。按官方 [Plugin Dependencies](https://plugins.jetbrains.com/docs/intellij/plugin-dependencies.html) 的构建与 descriptor 双重声明规则，将 `com.intellij.xml` 从仅 test bundled plugin 改为 production bundled plugin，并在 `plugin.xml` 声明 required dependency。不复制 XML JAR，不增加新的解析行为。必须对重建的最终 ZIP 复验两个精确 build，并重新绑定原生验收的包 hash；诊断时修改 ZIP 的候选副本不能作为最终产物。

### 1. 实际 Codex 客户端取消闭环先于任何生产平台改动

任务 1 的实际客户端准入现已完成。官方 Codex [`rust-v0.165.0-alpha.1` prerelease](https://github.com/openai/codex/releases/tag/rust-v0.165.0-alpha.1) Apple Silicon app-server 包已按官方 SHA 核验，解压可执行文件版本与 SHA 也已读回。临时 Codex MCP 配置下由模型实际调用隔离 IDEA 探针，再通过 `turn/interrupt` 中断仍在途的工具调用；没有使用自制 HTTP 客户端或手工补发取消通知。

三轮均协商 MCP `2025-06-18`，MCP session hash 为 `ffa259deefd382a3`，原 delay request ID `5`、`7`、`9` 各收到匹配的 `notifications/cancelled`。IDEA 分别记录对应顺序运行的 `cancelled(reason=request_cancelled)` 和一次 cleanup；各轮 31.005 秒观察期内均无迟到 `completed`。同一 session 后续 echo request ID `6`、`8`、`10` 均成功。`turn/interrupt` 至 app-server RPC ACK 为 102.954/103.796/102.763 ms；interrupt 至 IDEA `cancelled` 约为 6.359/8.809/4.622 ms；IDEA `started` 至 `cancelled` 为 154/73/24 ms，不能将首轮 154 ms 描述成 interrupt 延迟。原始记录、候选与产物 SHA 见[取消调查](../../../docs/mcp-cancellation.md)和[验证记录](verification.md)。

旧桌面 Codex `0.163.0-alpha.11.2` 未观察到服务端取消且在途任务正常完成的失败证据保留；`0.165.0-alpha.1` 是已通过隔离互操作的 prerelease 候选，不代表已替换日常客户端。该门槛允许进入生产基线升级，不等于 263 生产包、Verifier 或原生回归通过。以后针对其他客户端或协议版本复验时，仍按实际 initialize `protocolVersion` 判断信号：`2025-06-18` 与 `2025-11-25` 核对 `notifications/cancelled`，`2026-07-28` 核对请求级 SSE response stream 关闭。HTTP 202、源码/字节码检查、官方 TypeScript SDK 或只有服务端日志仍不足以单独通过客户端门槛；任何需要重新验证的三轮测试都必须同时关联原 request/session、一次 cleanup、无迟到成功及同 session 恢复。

### 2. 目标 build 固定且必须从构建输入读回

完成门槛后，将平台产品固定为 `IU`；`pluginSinceBuild` 使用完整 build number `263.7031.23`。平台解析和 Plugin Verifier 输入固定为官方可解析的 `263.7031.23-EAP-SNAPSHOT` 工件版本，最终必须从平台元数据和 Verifier 报告读回实际目标 `IU-263.7031.23`。官方 Maven 上裸版本 `263.7031.23` 的 POM 返回 404，不能把它当作解析坐标；也不能用 `2026.3`、滚动 EAP、`latest` 或本机未记录路径替代固定输入。若该固定输入无法解析或报告中的实际 build 不符，停止该任务并保留 261 基线，不以另一个 build 的成功替代。

2026-10-23 只读解析检查发现：裸版本 `263.7031.23` 的官方 Maven POM 返回 404，固定工件版本 `263.7031.23-EAP-SNAPSHOT` 的平台及测试框架工件可达。配置必须同时区分解析输入和实际 build：使用固定 EAP 工件输入，实际产品仍须读回 `IU-263.7031.23`。旧配置使用 IPGP 2.16.0 时，`verifyPluginProjectConfiguration` 因 Java 21 source/target 映射产生警告；该失败证据保留。官方正式发布版 IPGP 2.17.0（2026-06-26，commit `61cf376`）release notes 更新 build 262 起的 Java 映射，版本源码含 `Version(262) -> Java 25`。当前 2.17.0 下真实运行的 `verifyPluginProjectConfiguration` 已通过且无 source/target 冲突警告；生产编译器核验使用精确 JBR `25.0.4.1`、实际参数仅为 `--release 25`，未传 `-source`/`-target`，326 个生产 class 均为 major 69。该 Gradle 插件升级不增加生产 runtime dependency。完整测试、Plugin Verifier 与最终 ZIP 读回仍待完成。

打包后从插件 ZIP 读回 `META-INF/plugin.xml` 的 `since-build`，并从 Gradle 实际解析的平台 `product-info.json`/build 元数据核对 `IU-263.7031.23`。两个值不匹配即失败。沿用 `pluginUntilBuild` 为空的现有策略；这不扩展已完成的 Verifier/native 验证声明。

### 3. Java 25 是源码、API 与产物的统一基线

以项目 `javaVersion=25` 驱动 JDK/toolchain 与 Java 编译 `options.release=25`。IU `263.7031.23` 的平台运行时为 Java 25；生产源码可以使用 Java 25 语法和 API，产物 classfile major 必须为 69。用户已于 2026-10-24 明确批准这一兼容性变化，旧 Java 21 产物边界不再适用于本 change。

IPGP 的 `verifyPluginProjectConfiguration` 必须同时接受 target 平台和 `since-build=263.7031.23` 的 Java 25 级别。当前仓库的 2.16.0 映射表把 build 242+ 映射为 Java 21，不能满足 IU 263。正式发布版 2.17.0 的[发布记录](https://github.com/JetBrains/intellij-platform-gradle-plugin/releases/tag/2.17.0)明确更新 build 262 的 Java 映射；其[版本源码](https://github.com/JetBrains/intellij-platform-gradle-plugin/blob/2.17.0/src/main/kotlin/org/jetbrains/intellij/platform/gradle/utils/PlatformJavaVersions.kt)把 `Version(262)` 映射为 Java 25，因此 since-build 263 会使用该 Java 25 映射。配置检查、实际 javac 参数和已编译 classfile 已分别核验通过；最终 ZIP 仍须复核 descriptor、生产 classfile 和 test-only 内容。不得静音、改写或跳过检查。升级 `org.jetbrains.intellij.platform` 插件仅改变构建期工具，不增加生产 runtime dependency。

### 4. 自动验证和原生回归各自证明不同事项

2026-11-06 资源补验发现参数表的静态 `NODE_RENDERER` 强持有最后渲染的 `JTree`：263 `ColoredTreeCellRenderer.myTree` 渲染后不清空，树的 `myTreeTable` 指回表格；历史 MAT 的 disposed 参数表路径与当前 SDK 结构吻合。它有界地保留最近表单，不能称为已证实的持续线性增长。改为每个表格独有 renderer，在 `super(model)` 结束后安装，避免父构造阶段调用覆盖 setter 时读取未初始化字段；不在共享实例上手动清平台字段。验证两个表格 renderer 不同、原生参数写回及重复关闭后的 GC；其余 native/JCEF/listener 门槛仍单列。

2026-11-06 原生诊断观察到预览内部 ActionButton 的鼠标按下事件：组件相对点位于 popup 内，但 `MouseEvent.getLocationOnScreen()` 与组件屏幕位置不一致，平台 mouse-out Canceller 在释放事件前取消了弹窗。根因边界为该输入路径的坐标不一致，尚不能归因于物理鼠标或所有 macOS 用户。以公开 Swing 组件归属补强两种弹窗的既有取消回调：来源是内容根或后代时返回 false；其余保持 mouse-pressed 且未固定的策略。不引入坐标补偿、工具专用开关、Internal API 或新依赖。测试覆盖根组件/后代的矛盾坐标、外部点击和固定状态；重建后关闭诊断 agent，原生复验 Copy/Export、参数 Confirm/Undo，并保留输入与完整资源矩阵的未验边界。

自动门禁使用全量 `test`、`buildPlugin`、`verifyPluginProjectConfiguration` 和固定解析输入 `263.7031.23-EAP-SNAPSHOT` 对应的 `verifyPlugin`；必须从平台元数据和 Verifier 报告确认实际 IU build 为 `263.7031.23`。必须读回构建产物并检查 descriptor、bundled plugin、JDK 25 toolchain、`--release 25`、classfile major 69 及无 test-only 依赖；报告要显示实际测试数和 Plugin Verifier verdict，零测试或实际 build 不匹配均不算通过。

`IntegrationCredentialServiceTest` 使用平台真实 `InMemoryCredentialStore` 验证 KeePass 凭据隔离与清除行为。该类位于 IU 263 官方产品模块 `intellij.platform.credentialStore.impl`，精确 SDK 的 `product-info.json` 将它映射到 `lib/intellij.platform.credentialStore.impl.jar`。构建只通过 IntelliJ Platform Gradle Plugin 的 `testBundledModule("intellij.platform.credentialStore.impl")` 暴露到 test classpath；这不是 production/runtime dependency，最终插件 ZIP 必须排除该测试模块及其测试框架依赖。

263 的 Java light fixture 还需要显式的 test-scope bundled plugins：`com.intellij.xml` 提供 Java 插件 required module 依赖 `intellij.xml.parser`，`com.intellij.structureView` 提供 required module 的结构视图依赖，`intellij.java.aetherDependencyResolver.plugin` 为 `IdeaTestUtil` 的 mock JDK 下载提供平台自带 Maven resolver。使用公开 `testBundledPlugin` DSL，版本随固定 SDK；不拷贝平台 JAR、不增加第三方版本、不把测试依赖放进 production descriptor 或最终 ZIP。首轮 339 项测试中的 259 项初始化失败保留，补齐后必须复验。

`com.intellij.platform.vcs` 同样仅作为 test bundled plugin，补齐现有 `com.intellij.modules.platform` 依赖闭包中 `intellij.platform.collaborationTools` 所需的 VCS 模块。以官方 product-info/descriptor 和测试加载日志为据，不为日志中未使用的可选模块逐项加依赖，也不改变生产 descriptor。

原生回归在独立 profile、虚构/可公开样例工程和精确 IU `263.7031.23` 上检查插件安装加载、Spring/Feign/Dubbo 发现、文档生成/预览、目录交互、参数编辑、复制/导出及代表性 HTTP 请求草稿生成。真实平台上传不执行。HTTP Client 发送受许可阻塞的既有缺口只记录为 blocked/unrun；平台基线 smoke 不替代 HTTP change 的回环、双项目、取消/关闭、性能与完整菜单/主题矩阵。

### 5. 旧 build 与未验项目保持透明

`since-build=263.7031.23` 明确排除更早 build；不保留 261 安装路径、反射判断或 alternate implementation。兼容性说明要同时区分最低 build 声明、精确 Verifier 目标和实际原生 smoke build。IU 263 之后的其他 build、其他 JetBrains 产品、HTTP/UI 尚未完成的矩阵不能因最低 build 变化而自动记为通过。

### 6. 异步打开保持成功、失败与生命周期语义

263 的弃用注解指定使用 `requestOpenFile` / `FileEditorOpenRequest`，该公开替代 API 仍为 Experimental。打开过程失败通过返回的 Future 传播，外围同步 `try/catch` 和已经结束的 Backgroundable 无法接收。五处入口必须观察完成结果：项目目录/Markdown 仅在实际打开成功后显示“已打开”；HTTP 与单接口 Markdown 的创建/写入成功但打开失败应提示打开失败并保留已生成文件；欢迎页打开失败给出可见错误。项目关闭引起的取消静默结束，过期请求不得覆盖更新的 UI 状态。不得阻塞 EDT 等待 Future，也不为失败删除已成功保存的文件。测试使用受控的失败/取消 Future 验证反馈和状态，原生回归验证真实成功打开与关闭路径。此处的过期保护针对状态文案和失败通知；单接口 Markdown 每次显式打开仍独立生成并打开对应 Scratch，不因用户随后打开另一接口而丢弃先前请求的文件。多个独立文档的最终编辑器焦点顺序不在本次 API 迁移中重定义。

## Risks / Trade-offs

- [已通过的客户端 prerelease 准入被误读为生产升级完成，或官方构建输入无法精确解析至目标 build] → 生产基线与发布声明仍以任务 2–4 的证据为准；解析失败时保留 261 配置并记录阻塞原因。
- [更早 IDEA 用户无法安装基于新 `since-build` 的插件版本] → 在兼容性说明和变更记录明确最低 build；不通过双轨代码维持旧版本。
- [Plugin Verifier 通过但原生功能有回归] → 只有自动矩阵与独立 profile 原生回归分别完成后才能勾选对应任务，失败项保留为 blocked。
- [开放的 `pluginUntilBuild` 被误读为所有未来版本都通过验证] → 只把 `IU-263.7031.23` 写为已验证 build，并说明其他 build 仍待验证。
- [已有 HTTP/UI 待验项被新的基线 smoke 覆盖或覆盖状态] → 保持原 change 的 checkbox 不变，在本 change verification 中逐项引用并区分继承的未验范围。

## Migration Plan

1. 通过临时 Codex app-server 配置取得实际 Codex 客户端取消闭环证据并核对准入条件；不满足时停止后续步骤。
2. 将构建期 IntelliJ Platform Gradle Plugin 更新到已核实的 2.17.0；使用固定工件解析输入 `263.7031.23-EAP-SNAPSHOT`，读回精确实际 IU build `263.7031.23`，并同步最低 build 与获准的 Java 25 源码/API/字节码基线；不增加运行时依赖。
3. 运行完整 Gradle 与 Plugin Verifier 矩阵，读回实际解析 build 和最终插件 descriptor。
4. 在隔离 IU `263.7031.23` 中安装最终 ZIP，执行本 design 的核心功能 smoke；HTTP/UI 更大验收仍依赖其各自活动 change。
5. 记录通过、部分通过、blocked、跳过和未运行项；同步兼容性文档与 changelog。发布、tag、推送和 archive 需独立授权。

## Open Questions

无。若完整 build 的精确解析、Codex app-server client 互操作或原生环境不可用，应按任务标为 blocked，而不是改用官方 TypeScript SDK 或宽泛 EAP 目标推断成功。

## 2026-11-08：图标缓存清理时机

精确 `IU-263.7307.200` 的卸载流程在 `beforePluginUnload` 后拆除插件服务和监听器，再调用平台 `detachClassLoader`；自身监听器不会收到自己的 `pluginUnloaded`。平台 disabled-icon 缓存 `IconUtilKt.iconToDisabledIcon` 的 key 经 `CachedImageIcon.originalLoader` 保留插件 ClassLoader。公开 `IconLoader.clearCache()` 会使该缓存失效，`detachClassLoader` 单独不足以清理它。

先用 ignored 诊断包在自身 before 回调同步调用 clearCache：PID 13856 未打开 Doc View UI 的卸载对照中，调用时间先于平台拆除，live GC 后 ApplicationSettings 类不再存在，未出现 still-referenced 告警。该结果只授权采用最小公开监听器补丁继续验证，不作生产包通过结论。应用监听器按 plugin ID 精确过滤自身卸载；其他插件卸载不触发全局缓存失效。clearCache 本身具有平台全局作用，不额外 repaint、切主题或安排异步任务。同步调用完成后让平台继续正常 teardown。

回归先证明旧 descriptor 缺少清理路径而禁用图标实例仍缓存，再验证新监听器使其失效、其他 plugin ID 不失效。该段记录当时的原验证计划；2026-11-09 用户批准以 `require-restart=true` 作为安装、更新、卸载的生命周期契约，动态热卸载结果转为保留的历史诊断，不再作为发布通过条件。没有因此清除平台 JNI 根或宣称热卸载已修复。

### 2026-11-08 目录渲染器根链续验

未登录隔离 profile 的提示修复包在原生目录使用后禁用，官方内存分析报告另一条最短根：Local JNI → NodeRenderer.myTree → SimpleTree.model → AsyncTreeModel → StructureTreeModel.structure → DocViewWindowPanel$1。仅模型 dispose 不清空 structure；树的 parent 链也可返回面板。此证据支持在目录 dispose 同步 EDT 清 selection、替换为 JDK 空 DefaultTreeModel、从父容器移除树，关闭保存的 TreeSpeedSearch 弹层，并按对象归属移除自有 popup handler。状态锁仅用于取消与代次失效，不能持锁 invokeAndWait。

树搜索现有 plugin lambda 是另一条可保留加载器的 Java 边。初始化改用经两个精确 SDK 核实的公开平台 NODE_PRESENTATION_FUNCTION，并由 DocViewNode.toString 返回已有 getSearchText，使签名、FQN、业务名匹配范围不变；实施前检查 toString 调用影响，并用测试验证。不要在 dispose 重新安装 speed search：旧 listener/shortcut 仍存在，且相关 action/client-property key 是 Internal/private。该候选不增加生产依赖、不改 UI 搜索语义，不声称能清空此前 AccessibleJTreeNode.obj → Node.userObject 的独立 JNI 根；完整结果必须由新包原生卸载和 heap 根分析确认。

18:42 的整合包堆报告将剩余链定位到 `Tree$MyMouseListener.treePathUnderMouse`。两个精确 build 的源码均表明该实例只由树的 mouse listener 注册持有、mouseExited 不清该 path，也没有公开 reset API。永久销毁自有树时改用标准 `getMouseListeners/removeMouseListener` 移除全部鼠标监听器；因此不再需要单独捕获 PopupHandler。该操作仅在面板 dispose、空模型和 parent 断开路径执行，不筛选 Internal 类型、不派发合成 mouseReleased，不影响正常在用树。独立 AccessibleJTreeNode JNI 根仍未宣称解决。


### 2026-11-09：目录树 UI delegate 的残留路径

XML PSI 替代包已触发 XML、文档库、参数、HTTP 和三轮预览，再真实禁用插件；参数/面板实例归零但类加载器仍被保留。新 heap 官方最短根为 `Local JNI → NodeRenderer.myTree → SimpleTree.uiTreeExpansionListener → DefaultTreeUI$MyTreeExpansionListener.myOriginal → BasicTreeUI$Handler.pressedPath → StructureTreeModel$Node.userObject → ClassNode → PluginClassLoader`。该结果不能归因于已移除 DOM model，也不能将此前目录代表路径通过扩展到全部使用顺序。

永久销毁自有树时，必须同时解除 UI delegate 与其安装的监听器对旧路径的引用。采用公开 `JTree.setUI(null)` 结束这个不再复用的树的 UI 生命周期，并核对两个精确 SDK 中 Tree 的覆写会解除其 UI 展开监听器字段；在 EDT 和已有状态锁外完成；不得访问 `pressedPath` 私有字段、修改平台源码或派发合成释放事件。先用实际树 UI 安装的 expansion listener 和旧路径取得 RED，再验证 dispose 后公开可观测的 delegate/监听器归属及旧节点回收。完整构建、两 build Verifier 和真实卸载必须在新包重新执行；单纯测试通过不关闭任务。


### 2026-11-09：已关闭项目的 Tool Window 条目清理

421 候选在依次使用并关闭多个虚构项目后，真实热卸载再次失败。强引用 heap 分析（不把 Class 对象当根）证明：`Local JNI → ProjectViewRenderer → ProjectViewTree → ScrollPaneTracker → ToolWindowManagerImpl.idToEntry → Doc View ToolWindowImpl.stripeTitleProvider → PluginClassLoader`。该 manager 的 project `containerState=DISPOSE_COMPLETED`，map 中恰有一个对应 Doc View entry。它与此前自身目录树 UI 路径不同，不能将旧 tree-ui 候选通过外推为本场景通过。

精确 IU263.7307.200 官方源码显示 `ToolWindowManagerImpl.projectClosed()` dispose entry.disposable，但保留 idToEntry；manager.dispose 为空。插件不修改平台 map 或 JNI。候选修复在已有 `DocViewToolWindowFactory.init` 注册随 toolWindow disposable 释放的 application bus `ProjectCloseListener`，只在本项目 `projectClosing`、已通过关闭许可/保存取消检查后、project.startDispose 前，通过公开 `ToolWindow.remove()` 同步移除本窗口条目。不得在可取消的 `projectClosingBeforeSave` 阶段提前移除，不通过 createToolWindowContent 强制打开 UI；未展开窗口同样受管理。核对实际平台派发线程后在 EDT 执行，不使用 Internal API、反射、全局窗口扫描、延时清理或自定义 JNI 操作。

验证必须先有 RED/GREEN，覆盖其他项目/关闭前可取消阶段/监听器释放，之后完整构建与两 build Verifier；新包原生覆盖项目关闭和重开、双项目隔离、未展开/已使用窗口，再独立验热卸载。官方源码解释与局部清理不能代替最终类加载器 GC。

线程边界：本修复首先针对已复现的原生 UI 项目关闭/This Window 替换入口，在平台要求的 EDT 上直接调用公开 remove；不新增 invokeAndWait、异步排队或锁迁移。公开同步 close API 未声明完整线程契约，因此任意后台线程直接关闭/发布关闭事件尚未验证，不将 UI 入口实测推广到该范围。若目标原生关闭实际从后台派发，必须保留失败并重新评估公开方案，不能静默跳过清理。

### 2026-11-09：采用 require-restart 生命周期策略

用户批准在 `plugin.xml` 根 `<idea-plugin>` 声明 `require-restart="true"`，由 IDEA 将插件安装、更新和卸载按需重启处理。官方配置文档说明该属性影响这三类操作；它不清除热卸载的 JNI/ClassLoader 根，也不使 429 候选或无 JDWP 对照的热卸载失败变成通过。两份 heap 识别出的 FileChooser/ActionToolbar 创建栈继续作为该包的失败证据保留。

本次生命周期验收以 descriptor 契约及 IDEA 重启要求为目标，不再要求证明用户通过热卸载完成安装/更新/卸载。正常运行期的功能、项目关闭与组件 dispose 仍按原契约处理；已知功能失败先修复，其他无法执行的原生观察须标为未验证，不能由 descriptor 测试代替。隔离 IDEA 的安装/更新/禁用/卸载提示和重启后状态回读尚未完成；用户明确允许在功能验收通过后，把这一原生未验证项列入发布说明、发布后收集反馈，不得宣称原生验证已通过。
