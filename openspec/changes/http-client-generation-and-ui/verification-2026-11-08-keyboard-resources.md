# 中文、键盘、资源与 JNI 续验（2026-11-08）

接续[图标按钮验收](verification-2026-11-08-icon-buttons.md)。本轮只操作独立整合树和隔离 IDEA Integration Test / 虚构项目，未修改原工作区或普通 IDEA，Junie 保留。使用维护者已授权的 macOS 辅助功能操作；CUA 不可用。实际平台为 IU `263.7307.200` / JBR `25.0.4.1+1-b635.79`，生产最低 build 不变。

## 提示修复包的新增原生结果

ZIP `f96fd471cbbff311fbdc321f6133fb9b74c6fef0beda4ad8b2d46b48dc97c56a` / JAR `8f6b98b76b030eb59d175bb13ea64d7dbbc2733512a4068cce430312d43b9f0f`。PID 33226 英语，35684 中文；使用此前未登录的 `unload-clean` profile，未复制许可或账户。该组结果不能自动转计到后续键盘修复包。

- 英语 Islands Light、Compact 开启：800×500 和 600×500 参数弹窗三个纯图标均完整；JSON 复制为有效虚构对象。
- 简体中文 Islands Light / Islands Dark、Compact 开启、600×380：三个图标可辨认，中文列标题完整，三项 hover 均实际显示完整名称和说明。
- 中文草稿“中文窄窗验收”复制到 Markdown 后源码不变；重新编辑并写回 `Schema.description` 成功，原生 Undo + Save 恢复原源码。
- 中文设置页显示小号、灰色的非等宽说明。尝试把设置外窗设为 720×600 后，实际仍为平台最小 900×700；不得把这次尝试记成 720 宽通过。
- 通过拖动弹窗真实边框达到 600×380。AX 直接设置 popup size 被拒绝，首次拖动圆角未生效，均以实际 geometry 复核纠正，不算产品缺陷。
- 键盘失败：初始焦点为空 AXScrollArea，连续 Tab/Shift+Tab 不能到达按钮。Ctrl+Tab 回到编辑器；诊断时误输入的缩进已通过原生 Undo 恢复，磁盘回读确认。
- 目录的 Shift+F10 未打开所需方法菜单；Control+Return 触发平台“新建/快捷键冲突”。Find Action 的一次上下文菜单落在编辑器，未把它当作目录键盘菜单通过。精确平台默认 `Mac OS X 10.5+` keymap 对 `ShowPopupMenu` 定义空条目，并无默认快捷键；Shift+F10 是 Run，Control+Return 属于 NewElement/Generate。这些误键不能作为插件菜单缺陷证据。停止使用这些试探键，不新增目录快捷键；实际目录键盘菜单可达性和完整 U02 继续保留。

截图、AX 状态、复制结果、运行时属性与各阶段日志位于 ignored `build/native-validation/2026-11-08/native-ui-matrix/`。

## 资源观测的边界

PID 35684 在同一中文浅色进程中记录三轮参数弹窗/渲染预览开关。每次关闭均确认只余项目主窗口；`GC.class_histogram` 使用默认 live GC，保存过滤后的插件/JCEF 类计数，不保存普通工程内容。

| 观测 | 文件描述符数 | ParamDocEditorForm / ParamTreeTableView | CEF 渲染子进程 |
| --- | --- | --- | --- |
| 打开 Doc View UI 前 | 502 | 0 / 0 | 无 |
| 第一轮预览打开 | 523 | 1 / 1 | 1 |
| 第一轮关闭 + GC | 521 | 0 / 0 | 0 |
| 第二轮关闭 + GC | 521 | 0 / 0 | 0 |
| 第三轮关闭 + GC | 521 | 0 / 0 | 0 |

第一轮关闭后的 `RemoteBrowser`/JBCefBrowser panel 各仍有 1 个，第二轮后归零；平台 CEF server 和三个常驻 helper 保留。三轮关闭后 RSS 为 4,284,448、4,301,664、4,321,264 KiB，后续包含主题/设置操作的 GC 为 3,856,848 KiB，不能据此宣称长期内存稳定或性能改善。后续另一次参数弹窗关闭后首轮 GC 又出现表单/表格各 1，仍需静置复核；不将单次计数归零解释为完整类加载器释放。

此处仅覆盖三轮可重复开关、有限实例/FD/渲染子进程观测，不包含 L06 冷/热各七次、所有 native peer、监听器、任务和重开矩阵。

## 键盘修复的两层根因

1. 263 的平台 ActionButton 构造时只在屏幕阅读器开启时可聚焦。以公开 `ActionButtonComponent` 识别本参数编辑器 head/footer toolbar 的直接子组件，使用 Swing ContainerListener 处理初始和重建按钮，再设置 focusable。真实组件测试 RED 1/1 → GREEN 2/2。
2. 第一版修复包仍在原生 Tab 检查失败。AX 强制给 JSON 按钮焦点成功，但下一次 Tab 又回滚动区；强制聚焦只是诊断，不是键盘通过。精确平台 `AbstractPopup$3` 的五个 focus traversal 方法都返回首选组件，而本弹窗的首选组件是 `paramScrollPane`。参数根面板需要内部焦点循环，避免落入窗口级固定策略；继续遵循公开 Swing API，不改平台实现类、屏幕阅读器设置或全局按键分发器。

第一版键盘 ZIP `94a234c3b60bfde18e21d34e2d8cacc97ee076febdc1c2207a28b55e2e4ea5d5` / JAR `0d397cd7096524051721667d42e18359e4219e7cd54d8e3e8c15fbed1c9d8656`；77 suites / 399 tests / 0 failures/errors/skips，全构建 18 tasks 实际执行。最低及正式 build 的 Verifier 均 Compatible、16 既有 Experimental。原生 PID 41892 的 Tab/Shift+Tab 仍失败，不能因自动检查通过而关闭 5.11。

仅设置 focus-cycle root 的第二版 ZIP `f6d3a0d400bc5fba89a733bbbe4a7ac6996569d834166cbbdd27f4753e40f1af` / JAR `2dd3064ca056eb18ee9f7a4cd29c181fed651b3eb34ca9dc1edf04cb4f546d75`，399 tests、构建和最低 build Verifier 通过，但 PID 66649 原生 Tab 仍回滚动区。Java 25 `Container.getFocusTraversalPolicy()` 证实未显式指定本地策略时会继承父 cycle root 的策略，故继续增加公开 `LayoutFocusTraversalPolicy`；没有把第二版原生结果标为成功。

第三版显式 `LayoutFocusTraversalPolicy` 的 ZIP `6b3f4ec89772c975ccc2e9fd60d07a3d417bdd6b67660c63674122cf26b70813` / JAR `ca16a1595d5e8edb1ec91e0489da8eb0df0baae44765305d34969345c35b604c`，399 tests、构建及最低 build Verifier 通过。PID 67172 的初始 Shift+Tab 依次到达 Pin、Settings、Apply to Source、Copy as Markdown、Copy JSON；Space 激活得到有效 JSON 和 Markdown，剪贴板已恢复。但表格获得焦点后 Tab/Shift+Tab 保留单元格导航，Ctrl+Tab 被 IDE Switcher 接管；键盘写回路径尚未通过。该中间包未另跑正式版 Verifier，后续整合修复包需重新执行两个精确 build 的门禁。

正式版 Verifier 第一次 CLI 未指定 runtime，在产出 IDE/插件结论前停止；原日志保留为 incomplete，随后显式指定本机 JBR 串行复跑取得上述结论。

## JNI 热卸载的新增证据

对提示修复包 PID 35684 打开并展开接口目录，辅助功能读取方法节点，隐藏目录后从插件设置禁用 Doc View。`17:41:36.530` 平台报告类加载器仍被引用；live GC 仍有 DocViewWindowPanel 1。通过平台“保存内存快照”在这个未登录诊断 profile 取证，`17:42:31.263` 的最短根为：

```text
Local JNI
  → com.intellij.ide.util.treeView.NodeRenderer
  → myTree: SimpleTree
  → treeModel: AsyncTreeModel
  → model: StructureTreeModel
  → structure: DocViewWindowPanel$1
  → Class → PluginClassLoader
```

这与上一轮 `AccessibleJTreeNode.obj → Node.userObject → MethodNode` 是不同的最短根；不表示旧链不存在，也不表示已枚举全部根。heap `unload-com.liuzhihang.doc-view-08.11.2026_17.42.18.hprof` 仅在 ignored 未登录 profile 中，未上传、提交或打包。退出前已恢复 Doc View 启用，PID 35684 正常退出。

本轮核对 [JBR #646](https://github.com/JetBrains/JetBrainsRuntime/issues/646) 仍 Open，未关联修复 PR；[精确 JBR release](https://github.com/JetBrains/JetBrainsRuntime/releases/tag/jbr-release-25.0.4.1b635.79) 未列出对应修复。新的 renderer→tree 路径继续检查公开释放方式，尚未通过。正式 MCP、版本升级和发布继续等待全部必需门槛。

## 新引用链的插件侧释放修复

`DocViewWindowPanel` 不再向 TreeSpeedSearch 保存插件 lambda，改用公开 `NODE_PRESENTATION_FUNCTION`；`DocViewNode.toString()` 保留现有 `getSearchText()`，避免丢失类名、完整签名和业务描述。目录销毁时先停止自身任务，然后在 EDT 关闭搜索 popup、清 selection、替换为空 JDK 模型、移除自身安装的 PopupHandler 并断开 tree parent。同步切到 EDT 时不持有 panel 的 synchronized 锁。没有使用 Internal API、反射或关闭系统辅助功能。

两项针对性回归先取得 RED（formatter 丢失代码名、dispose 保留原模型），再用隔离 JBR 25 取得 GREEN。自动检查只证明这些插件侧引用被断开；仍需新整合包原生卸载后重新检查类加载器和实际 JNI 根。

## 整合修复包自动门禁与键盘复验

ZIP `6fb61cf3686bf01fdc7003a264dfcd7c586e1909767e3f4ccd390f6bf6421ee9` / JAR `3183f4afc6a802576579d21b80a72f86318c5ccf7a9a0452260670e1b0a53dcd`，340 个生产 class 均为 major 69；仍只用于本地 1.5.0 验证，不覆盖已发布版本。`test verifyPluginProjectConfiguration buildPlugin verifyPlugin --rerun-tasks --no-build-cache` 为 77 suites / 402 tests / 0 failures/errors/skips、18 tasks 实际执行；全量重建 ZIP 与已安装冻结包相同。最低 263.7031.23 及正式 263.7307.200 的 Verifier 均 Compatible、16 既有 Experimental，无 Deprecated/Internal/API compatibility 问题。构建保留既有测试源码弃用提示和 Gradle 10 迁移提示。

两个表格局部快捷键在 popup 生命周期内注册，普通 Tab 单元格导航保留；仅表格/编辑器后代上下文启用，编辑拒绝提交时不转移焦点，popup dispose 后解绑。新增测试先因未实现注册而 RED；首轮实现测试因未显示 Swing 组件未提供上下文而失败，改为显式测试输入后单类 4/4 GREEN，未把该测试环境失败描述为原生缺陷。

PID 89027，英语 Islands Dark，Compact 开启，600×380：

- 初始 Tab 进入表格，Ctrl+Shift+Tab 到 Pin；单元格编辑时 Ctrl+Tab 到 Copy JSON，Tab 依次经过 Markdown、Apply to Source。
- 从描述编辑器退出后 Space 写回 `Keyboard source acceptance`，实际源文件读回成功；原生 Undo + Save 恢复 SHA-256 `ab9d766cf7e040391066154d5e1068093e3ff5896b93367f262b39532db65e91`。
- Shift+Tab 定位后 Space 复制取得有效 JSON（39 字符）与 Markdown（122 字符）；两次复制均恢复原剪贴板，未修改源码。
- 新包首次打开时 AX 窗口尚未就绪，脚本未找到 Doc Editor 动作；重新读取可用项目窗口后复跑成功。这个测试启动失败单独保留，没有算作产品通过。

中文、主题、资源及新包 JNI 结果在后续小节记录；上述键盘成功不代替完整矩阵。

## 整合包三轮资源与静置 GC

PID 89027 完成英语深色的三轮参数弹窗和实际 JCEF Preview 开关。复制/写回之后、循环之前的一次 live GC 尚有参数表单/表格各 2 个，第一轮结束后归零；不得省略这次暂留。

| 整合包观测 | FD | RSS KiB | 参数表单 / 表格 | CEF 渲染进程 |
| --- | --- | --- | --- | --- |
| 第 1 轮关闭 + GC | 503 | 3,381,056 | 0 / 0 | 0 |
| 第 2 轮关闭 + GC | 509 | 3,441,072 | 0 / 0 | 0 |
| 第 3 轮关闭 + GC | 509 | 3,453,344 | 1 / 1 | 0 |
| 静置约 35 秒，再次 GC | 514 | 3,484,624 | 0 / 0 | 0 |

第三轮首个 GC 的 RemoteBrowser/JBCefBrowser panel 各 1，静置后也归零；平台 CEF server 和 3 个 helper 常驻。FD/RSS 存在变化，未把它们称为稳定基线或性能改善。此组只证明已操作表单、渲染实例与 renderer 进程在观测窗口内可释放；L06 七次冷/热采样、所有监听器/native peer/在途关闭等仍未完成。

## 整合包中文与目录续验

同一 JAR 在 PID 90278 切换简体中文：Islands Light、Compact 开启、600×380 三个按钮完整，悬停均同时显示中文操作名与说明；中文草稿“键盘写回验收”通过 Ctrl+Tab → Tab → Space 写回，再 Undo + Save 恢复基线哈希。Islands Dark、Compact 关闭时，600×380 与 800×500 仍完整；Shift+Tab 定位 Markdown 按钮后 Space 复制 122 字符成功，剪贴板恢复。测试后恢复 Compact 开启，参数外窗恢复 800×500。

目录展开并实际呈现 60 个方法节点；从目录通过 Find Action 的“显示上下文菜单”按 Enter 打开 Doc View 的源码、Markdown、导出、上传、HTTP 菜单，再 Escape 取消。平台 macOS 默认没有该动作的快捷键，本次没有更改 Keymap。目录直接输入 nativeForm 时出现中文输入法候选窗，未据此标记原生搜索通过；该项仍需另验。

## 整合包热卸载：面板已释放，仍有鼠标缓存路径

PID 90278 从原生插件设置禁用并点击确定后，`18:41:41.355` 日志确认开始动态卸载；`18:42:00.156` 再次报告类加载器仍被引用。live GC 的 DocViewWindowPanel、ParamDocEditorForm 和 ParamTreeTableView 均为 0，但 MethodNode 60、ClassNode 4、ModuleNode/RootNode 各 1 仍存在，VM.class_hierarchy 也仍能找到插件类。

在未登录诊断 profile 通过平台“保存内存快照”，`18:42:35.665` 的报告得到新最短根：

```text
Local JNI → NodeRenderer.myTree → SimpleTree.mouseListener
→ AWTEventMulticaster → Tree$MyMouseListener.treePathUnderMouse
→ CachingTreePath.lastPathComponent → StructureTreeModel.Node.userObject
→ MethodNode → Class → PluginClassLoader
```

这证明树模型和 parent 清理尚不足以完成热卸载；不能将面板实例归零写成 ClassLoader 已回收。新 heap 仅留在 ignored `unload-clean/log/unload-com.liuzhihang.doc-view-08.11.2026_18.42.25.hprof`，未上传或提交。鼠标缓存链继续检查公开释放方式，原有独立 AccessibleJTreeNode 链也未宣布修复。

精确 Tree 源码继续核查发现该缓存来自 mousePressed，只有 mouseReleased 清空，mouseExited 不清；未找到公开 reset API。下一版仅在永久销毁自有树时用公开 Component API 移除所有 MouseListener，避免通过 private 类型或合成鼠标释放事件介入平台实现。此修复与新包复验尚在进行。

## 鼠标监听器清理包：两轮目录热卸载通过

ZIP `667df0aedc65f7ac368055c79f7e4b8d601b8cbb5f12ce9461d05d2b97ef61fb` / JAR `bd129fd6b7fdc21ca3ff8a4bca97ea24cb48e4691327127d1ba10f7cbddd1495`，340 个 class，major 69。相对上一整合包，JAR 差异只有 DocViewWindowPanel 及其内部 class，参数、预览、图标和消息资源字节不变；前面的中文/主题/三轮资源证据仍按原包身份保留。永久销毁树时移除全部 MouseListener 的新增断言先 RED，再 GREEN；无需捕获私有类型或派发合成事件。

最新包完整执行 77 suites / 402 tests / 0 failures/errors/skips，配置检查和构建成功，18 tasks 实际执行；全量重建 ZIP 与冻结安装包相同。两精确 build Verifier 均 Compatible、16 既有 Experimental，无 Deprecated/Internal/API compatibility 问题。日志与 XML 分别在 `listener-full-gates.log`、`listener-candidate/`、`listener-release-verifier/`。

原生 PID 96015，英语 Islands Dark / Compact 开启，仍为未登录的 unload-clean profile：

1. 键盘描述写回与 Undo 再验通过，原源码 hash 恢复；目录展开 60 个方法。选择 nativeForm 后通过 Find Action → Show Context Menu → Enter 展示菜单，Escape 后对方法 Enter，实际打开 `nativeForm.md`，读回 `/native/form` 与 POST，正常目录操作未被销毁逻辑影响。
2. 第一轮原生禁用于 `18:54:17.932` 开始；随后 live GC 中 Doc View 实例行数为 0，`VM.class_hierarchy DocViewWindowPanel` 不再返回该类，未出现 still referenced 告警。无需重启或换主题才能释放。
3. 在**同一 PID**重新启用，目录再次刷新/展开，并对 nativeForm 使用原生 AXPress。又完成参数/实际 JCEF 预览各 Pin 开、关两组 Finder 往返。四张桌面合成截图均无 Doc View 覆盖 Finder，且已验证 Finder 与 popup 的矩形有重叠；Finder 路径读回为本次 integration 下的 fictional-project。返回 IDEA 后参数草稿保留，预览控件仍可用；未写回的 Finder 草稿已取消。Pin 恢复开启。
4. 第二轮原生禁用后，`19:05:23` live GC 再次为 Doc View 实例行数 0，类层级查询再次无 DocViewWindowPanel，无新的 still referenced 告警。两轮 CEF renderer 均为 0；平台 server/helper 常驻不等于 Doc View 泄漏。

两轮结果证明当前 Mac / 精确 IDEA+JBR / 已执行目录与弹窗操作中的插件类加载器可以回收，**不证明 JBR 自身的所有 JNI 引用已修复、其他平台/LAF 或未执行操作都通过**。旧 AccessibleJTreeNode 以及中间包的失败 heap 保留为历史，不伪造“上游已发布修复”。最新包本轮没有产生新的卸载失败 heap。

## 收尾与仍待验范围

Doc View 已恢复启用，隔离 PID 96015 正常 Exit；语言恢复英语、Islands Dark、Compact 开启、参数窗口 800×500，剪贴板及两种 Pin 状态恢复。原工作区 584 个冻结文件与 HEAD、虚构项目 15 个基线文件均无变化；Junie/Air 未卸载或禁用。没有 commit、push、tag、Release、Marketplace、sync/archive，也未推进正式 MCP。

本轮图标/键盘功能与目录热卸载代表场景已有通过证据；完整发布验收仍未完成：目录搜索的原生输入复验、L04/L06 的取消/关闭与七次冷/热资源性能矩阵、U03 双项目 Clear Cache 和其他既有未勾选场景保留。前一包的中文布局和三轮资源结果不冒充最后 ZIP 的每一组合均已重跑。具体状态以 HTTP/UI 与基线 tasks 为准，只有全部发布门槛通过后才升级公开版本和发布。

收尾校验：两个活动 change 的 strict validate、修改文档相对链接和 `git diff --check` 通过。同步 CHANGELOG 后再次执行 buildPlugin，ZIP hash 仍与上述 listener 候选完全相同；没有把文档收尾称为再次执行全量测试。HTTP/UI 为 33/41，基线为 18/20；任务完成计数不代表所有发布验收均完成。
