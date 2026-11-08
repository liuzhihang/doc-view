# 卸载图标缓存 API 与 JNI 续验（2026-11-08）

本记录接续[整合包原生验收](verification-2026-11-08-integration-native.md)，只描述本轮新修复包。旧包的 HTTP、菜单、Copy/Export 和弹窗结果保留原包边界，不能自动转记到新包。

## 范围与产物

- 修改仅在独立工作树 `doc-view-release-integration-20261107`；原工作区、普通 IDEA 和 Junie 保留。
- 生产实现增加 `DocViewPluginLifecycleListener`，通过 `plugin.xml` 注册 `DynamicPluginListener`。只有 descriptor 的 plugin ID 是 `com.liuzhihang.doc-view` 时，同步调用公开 `IconLoader.clearCache()`。
- 这是对平台共享图标缓存的失效操作，不是仅删除 Doc View 条目的定向 API。只在自身卸载前调用，不用于普通刷新，也不响应其他插件的独立卸载。
- 未引入反射、Internal/TestOnly API、延迟任务、新依赖或新的生产语言。Java 25、最低 build `263.7031.23` 不变。
- 新验证 ZIP SHA-256：`cedb764d3e59176a97546dcd36c3729ddb24aa4924702d7653f017f18e5e6f04`；内含唯一 JAR SHA-256：`dd12506f62e82946a621a606584e77baed5fe28fdfb270690175a008c14b3e9a`。337 个生产 class 均为 major 69，无诊断 overlay class。
- ZIP 路径为 `build/native-validation/2026-11-08/unload-api-timing/production-candidate/Doc View-1.5.0.zip`。1.5.0 仅沿用本地验证标识，已占用公开版本不能覆盖发布。

## 为什么在 beforePluginUnload 调用

精确 [263.7307.200 的卸载实现](https://github.com/JetBrains/intellij-community/blob/idea/263.7307.200/platform/platform-impl/src/com/intellij/ide/plugins/DynamicPluginsSupportImpl.kt)先在 EDT 发送 `beforePluginUnload`，随后取消插件 scope、注销 descriptor/service/listener、断开插件连接，再清平台缓存并发送 `pluginUnloaded`，最后等待类加载器释放。插件自己的 listener 在后一个事件前已经注销，因此不能依赖自己的 `pluginUnloaded` 做清理。

[DynamicPluginListener](https://github.com/JetBrains/intellij-community/blob/idea/263.7307.200/platform/core-api/src/com/intellij/ide/plugins/DynamicPluginListener.kt)的单插件 before 回调可公开使用。本轮没有使用标记 Experimental 的批量回调。

此前实际堆定位的 `IconUtilKt.iconToDisabledIcon` 是强 key、10 分钟访问到期缓存；其 key 的 `CachedImageIcon.originalLoader` 保留插件加载器。[iconUtil.kt](https://github.com/JetBrains/intellij-community/blob/idea/263.7307.200/platform/util/ui/src/com/intellij/ui/icons/iconUtil.kt)注册 transform cache cleaner，而公开 [IconLoader.clearCache()](https://github.com/JetBrains/intellij-community/blob/idea/263.7307.200/platform/util/ui/src/com/intellij/openapi/util/IconLoader.kt)触发该 cleaner。平台卸载已有的 `detachClassLoader()` 处理另一份图标缓存，重复调用它不能解释或消除已观察到的禁用图标链。

先在未登录隔离 profile 的旧 JAR 上加入 ignored 诊断 overlay：PID 13856 日志记录 `own.before.begin` 和 `own.before.cleared`，无自身 after 回调；不打开 Doc View UI 时卸载后 live GC 和类层级检查均无残留。随后退出并移除 overlay，正式加入最小生产实现。诊断结果与下面无 overlay 的生产包结果分开保存。

## 自动验证

新增三个测试分别检查真实 descriptor 注册、自身 before 回调确实使禁用图标缓存失效、其他插件卸载不触发清理。实现前 registration 断言失败（不是行为测试已证明失败），实现后 3/3 通过。

初次完整执行 `test buildPlugin verifyPluginProjectConfiguration verifyPlugin --rerun-tasks --no-build-cache`，得到 77 suites / 398 tests / 0 failures/errors/skips 及构建成功。但其中发现旧的预览晚返回测试先等待超时再关闭 UI，也可能通过；该测试必须修正并重跑，不能用初次绿色结果证明这条生命周期顺序。

最低 `263.7031.23` 与正式 `263.7307.200` 均使用上述冻结 ZIP，串行离线 Plugin Verifier 1.411 结果均为 Compatible、16 项既有 Experimental usages，无 compatibility、Internal、Deprecated 或 Override-only 问题。逐份读回了 verdict 和问题报告，未将 exit 0 单独作为通过依据。正式版联网 CLI 等待未产出结论，已结束后离线重跑；最低 build 初次 Verifier 的共享解包缓存关闭异常未在串行复跑中出现，原始日志保留，不能由此断言确切根因。

预览测试已改为由受控后台 observer 等待服务启动，再在 modal event loop 中关闭表单并释放服务；验证服务不是超时退出、关闭发生在返回之前，且迟到的成功/异常结果不再改变已释放的文档与导出状态。修正前 RED、修正后单测 GREEN 均保存。随后一次定向复验与全量运行重叠，造成 Gradle `in-progress-results-generic.bin` 丢失；该次全量明确失败，保留 `final-tests.log`，随后停止重叠运行并串行重跑。

最终 `test --rerun-tasks --no-build-cache`：**77 suites / 398 tests / 0 failures/errors/skips**，14 个任务实际执行，`BUILD SUCCESSFUL in 1m 36s`。晚返回用例同时覆盖成功与异常分支，用时 0.437 秒；实际 XML 和最终测试源码 SHA 已保存于 `final-tests-serial/`、`final-test-result.json`。该次不存在并发 Gradle 或中途源码修改。仍有 17 条 test-only 编译过时/待移除警告及 Gradle 10 迁移提示，不混同为生产包 Verifier 的 Deprecated API 问题。

本轮生产构建在第一次完整 gates 已成功；后续仅修正测试夹具与记录，冻结 ZIP/JAR 无变化。新 manifest 为 231 项：相对 final-v2，仅新增生产 listener、修改其 descriptor 注册和 CHANGELOG；其他冻结输入未改变。基线任务 6.1 完成，6.2 和完整原生门槛继续未完成。

## 新生产包原生卸载

运行 IU `263.7307.200` / `JBR-25.0.4.1+1-635.79-nomod`。使用未登录的 `unload-clean` profile，只含虚构项目；没有从已激活 profile 复制许可或账户，也没有对其抓取堆。Junie/Air 保留，MCP Disabled，无代理对话发送。

| 场景 | 新生产包结果 | 边界 |
| --- | --- | --- |
| PID 15881，不打开 Doc View UI，原生禁用插件 | 通过：live histogram 中 Doc View 实例归零，`VM.class_hierarchy` 中 ApplicationSettings 消失，无仍被引用告警 | 证明该入口及调用时机能解除已复现的无 UI 图标缓存保留；不证明所有 UI 场景不会重新填充缓存 |
| 同进程原生重新启用，打开目录并读取 60 个方法，隐藏目录后禁用 | 未通过：`12:59:30.078` 出现卸载失败，GC 后仍有 DocViewWindowPanel 1、MethodNode 60、ClassNode 4 | 新加载器仍被引用，完整热卸载继续失败 |
| 通过官方 Save Memory Snapshot 分析第二场景 | `13:02:59.728` 报告 Local JNI → AccessibleJTreeNode → StructureTreeModel.Node → MethodNode → PluginClassLoader | 确认新包 JNI 链仍存在；这一最短根报告不等于枚举全部强引用根 |

本轮 heap 为 `unload-com.liuzhihang.doc-view-08.11.2026_13.02.49.hprof`，806,956,861 bytes，仅留在 ignored 本机证据目录。没有上传、提交或打包。生产原生诊断在 `unload-api-timing/candidate-no-ui-*`、`candidate-tree-*`；overlay 对照在 `before-*`。

诊断结束后通过原生设置恢复 Doc View 启用，再正常 Exit；进程检查确认 PID 15881 已退出。重启或进程退出不作为 JNI 热卸载修复。

## JNI 的公开 API 与官方修复状态

2026-11-08 核查 [JetBrainsRuntime #646](https://github.com/JetBrains/JetBrainsRuntime/issues/646)：仍 Open，0 comments，无负责人、里程碑或关联修复 PR。其报告环境是 IDEA 262 / JBR 25.0.4；本地 IDEA 263 / JBR 25.0.4.1 的同型引用链是独立复现，不能表述为厂商已确认本机根因。报告作者关于缺少 JNI local-reference 释放的解释也是尚未证实的推测。

[动态插件指南](https://plugins.jetbrains.com/docs/intellij/dynamic-plugins.html)、公开的插件生命周期接口和 [Java 25 AccessibleContext](https://docs.oracle.com/en/java/javase/25/docs/api/java.desktop/javax/accessibility/AccessibleContext.html)未提供释放 JBR/macOS bridge 所持 JNI 根的插件 API；本轮未找到官方已发布修复。公开 Swing 的 `MutableTreeNode.setUserObject(null)` 也不构成受支持的平台树节点卸载方案，不能未经复验修改平台所有的节点。没有加入反射、Internal API、关闭无障碍或修改 JBR 的绕行代码。

## 剩余弹窗和发布门槛

本轮用旧 final-v2 包打开参数弹窗并观察 Pin 开启，但未成功建立可观察的草稿编辑，也没有完成真正的应用切换，因此无新增弹窗通过项。已关闭弹窗退出，未写回源码。CUA 的后台输入和 Raise 不足以证明 OS 激活切换；已请求本轮仅允许通过 macOS 应用激活接口切换隔离 IDEA/Finder，尚未收到答复，未执行替代激活技术。桌面截图授权仍仅限这两个测试窗口前台。

参数 Pin 关闭、预览两种 Pin 状态、返回草稿和完整菜单/资源矩阵仍待补；旧包参数 Pin 开启真实 Finder 切换的单项通过保留在上一份记录。JNI 热卸载失败，R2/4.2、6.2 及其他原生门槛不勾选。不推进正式 MCP，不升级发布版本，不执行 commit、push、tag、GitHub Release 或 Marketplace 发布。

收尾核对：原工作区 584 个文件及 HEAD、原虚构项目 15 个文件均保持原 hash；独立工作树的未相关改动保留。三个相关 change 的 strict validation、8 份验收/路线文档中 114 个相对链接目标及 `git diff --check` 通过。未登录诊断 profile 保留启用状态和本轮生产包，主激活 profile 保留原 final-v2 包及激活配置；均不替换普通 IDEA 或移除 Junie。证据摘要在 `unload-api-timing/closure-checks.json`。


## 后续授权与按钮反馈

维护者已允许仅通过 macOS 应用激活接口切换隔离 IDEA/Finder，旧的待授权状态已解除。后续实机反馈要求参数底部去除常驻文字，改为可区分的图标。此修改及新包验收见 [HTTP/UI 记录](../http-client-generation-and-ui/verification.md)；本页的 ZIP、398 tests 和卸载证据仍仅属于本页冻结包。原生工具随后不可用，剩余弹窗验收继续未通过，原因已变为工具不可用，不重复请求已有授权。

## 公开树节点释放路径的进一步核对

维护者随后明确允许通过 macOS 辅助功能接口操作隔离 IDEA 与虚构项目 Finder 窗口；工具缺失不再阻塞这些限定的原生操作。参数新图标、复制/写回及应用切换结果见[按钮续验](../http-client-generation-and-ui/verification-2026-11-08-icon-buttons.md)。

本轮对实际隔离 IU `263.7307.200` 的 `intellij.platform.ui.tree.jar` 做字节码核对，其 SHA-256 为 `1efd4c19922222c2a3c1587e134c4d8ec7ec5d3e88b94205407f0d443072a3c9`，与正式安装包同文件一致，未修改普通 IDEA：

- `StructureTreeModel.Node` 是包内类；继承公开接口的 `setUserObject(Object)` 实现直接抛出 `UnsupportedOperationException("cannot modify node")`，不能通过该入口清空 descriptor。
- `StructureTreeModel.dispose()` 清理根、父子关系及监听器，但节点保留的 userObject/descriptor 不会被清空。`NodeDescriptor` 没有公开的 element 释放入口；`AsyncTreeModel.dispose()` 的监听器清理也不能释放 JNI 持有的节点。
- 即使提前到父组件 dispose 前执行，也无法绕过上述公开入口的拒绝语义。不能把不存在的可写路径当作生产修复；本轮未加入反射、Internal API 或关闭无障碍的措施。

证据为 ignored `build/native-validation/2026-11-08/native-icon-followup/jni-public-api-audit.txt`。官方 [JBR #646](https://github.com/JetBrains/JetBrainsRuntime/issues/646) 本轮再次读取仍为 Open；未找到适用于该精确运行时的已发布修复。此结论是公开 API 排查结果，不是新的热卸载通过证明；目录经辅助功能读取后的 JNI 热卸载仍保留未通过，发布门槛不变。

## 后续进展：目录释放代表场景已通过

后续公开 API 修复进一步断开树模型、父组件和平台鼠标监听器缓存的旧 TreePath；最新包同进程两轮使用目录后的热卸载均实例归零、类层级查询无目录面板类、无保留告警，四种 Pin 状态 Finder 往返也已复验。402 tests 与两 build Verifier 通过。精确包、旧失败根与完整待验边界见[中文/键盘/JNI 续验](../http-client-generation-and-ui/verification-2026-11-08-keyboard-resources.md)。这里不将历史旧包热卸载失败继续描述为最新结果，也不声称所有 JBR JNI 行为或完整发布矩阵均已通过。
