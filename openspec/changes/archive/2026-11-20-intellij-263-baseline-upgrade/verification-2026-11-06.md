# 2026-11-06 原生输入、参数表与资源修复

## 当前结果

full-12 已在无诊断 agent 的正式 IU `263.7307.200` 通过预览 Copy/Export、代表性 Schema Description/Required 写回及 Undo。发现静态 renderer 保留最近参数表单后再次修复，full-13 的 **359 tests、完整构建及两个精确 build 的 Verifier 通过**。full-13 已装入隔离 profile，但启动后 Mac 锁定，原生工具明确无法访问；其写回回归与重复开关后的 GC 验证仍未执行，不能继承 full-12 的原生通过结论。完整菜单、JCEF/native memory/listener 与性能矩阵仍未完成，R2、4.2 保持未勾选，正式 MCP 暂不推进。

## 范围与身份

继续 R2，最初使用 [full-11 正式版补验](verification-2026-11-05.md) 的同一 ZIP/JAR、正式 IU `263.7307.200` 与 Java `25.0.4.1`，随后按本记录重建 full-12/full-13。初始 PID `24022` 的 `jcmd VM.system_properties` 确认 config/system/log/plugins 均指向隔离 profile；普通 IDEA 和 Junie 保留。生产平台和最低 build 未改变，后续 Java 修复与新包分别记录。

本机忽略证据目录为 `build/native-validation/2026-11-06/`。`launch.json`、`native-observations-phase1.json` 与测试 XML 分别记录进程身份、原生观察和组件测试结果。后续临时事件日志仅用于诊断，不替代无诊断 instrumentation 的最终原生验收。

## 原生重现

- 参数表显示 `displayName=false`、`age=true`，与 fixture 的现代 Schema `requiredMode` 一致。
- 对 Description 的 AX 双击和截图坐标双击均只观察到单元格选择；F2、Return、Tab 未取得可确认的编辑状态。辅助功能焦点仍报告为 scroll pane，行报告 disabled。不能仅从该辅助功能状态推定整个 JTable 被禁用。
- Escape 可关闭参数弹窗并返回源码。未完成 Confirm、转义及 Undo 的源码写回。
- Spring `findUser` 预览正常显示。原生点击 Pin 后弹窗关闭；重新打开后点击预览中央内容区也关闭。该现象不限于 Copy；不能把失败直接归因于剪贴板代码。
- 未将这些尝试记为 Copy/Export 或菜单通过。

## 参数表组件测试

新增 `ParamTreeTableViewTest`，验证名称/类型只读、Required/Description 可编辑，Description 编辑器提交写入 DTO 与 `modifiedMap`，Required 下拉框提交，以及真实 Swing mouse pressed/released/clicked 双击事件分发能够启动 Description 编辑。

执行 `JAVA_HOME='/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home' ./gradlew test --tests com.liuzhihang.doc.view.ui.treeview.ParamTreeTableViewTest`；最终 **3 tests / 0 failures / 0 errors / 0 skipped**。这是组件级测试，不是 macOS 原生事件验收，也不覆盖 Confirm 后的 PSI 写回。首轮诊断中直接把 F2 KeyEvent 传给 `editCellAt` 返回 false；该调用不等同 IDEA 的 F2 action 路由，最终测试不将它当作产品缺陷或原生成功证据。

此阶段未重新执行全量测试、构建或 Plugin Verifier；full-11 的 352 项测试及两个精确 build 的 Verifier 是前一轮结果。后续修复产物单独记录，不能把两份包的证据混用。

## 只读事件诊断与根因边界

诊断 agent 仅在隔离进程注册 AWT 观察器，不修改控件、焦点或派发事件；日志只含组件类、父子关系、坐标、enabled/focusable/showing 和焦点类，不记录文本、按键值、窗口标题或剪贴板，最大 4 MiB。首次 PID `24910` 在 premain 初始化 Toolkit 时卡住，线程栈定位为诊断程序初始化时机问题；停止该测试进程后，改为 daemon 线程延迟注册，PID `25027`、`25426` 正常启动。该失败不是 Doc View 产品缺陷。

PID `25027` 参数双击日志实际显示 `ParamTreeTableView` 收到 clickCount=2，随后 `JTextField` 获得焦点；AX 树却仍显示 scroll pane 和 disabled 行。随后以截图和实际输入验证：描述写入 `Native verified "quote" \\ path`，Required 从 false 改为 true，Confirm 后源码/磁盘均为正确转义的 description 和 `Schema.RequiredMode.REQUIRED`，一次 Undo 恢复原注解。恢复后的 fixture SHA-256 为 `ab9d766cf7e040391066154d5e1068093e3ff5896b93367f262b39532db65e91`，与原文件一致。由此撤销“AX 未变化即未进入编辑”的推断；先前尝试仍保持未验，最终验收须移除 agent 复验。

PID `25426` 的预览 Copy 按下事件明确命中内部 `ActionButton`：local `(13,12)`、popup-relative `(783,486)`，popup 尺寸 `800×500`。组件屏幕位置 `(1649,929)` 加 local 应为 `(1662,941)`，事件却报告 screen `(1848,771)`；popup 屏幕位置 `(879,455)`，该 screen point 在右边界之外。随即发生 popup focus lost、showing=false，没有后续 released/clicked。该输入路径的绝对坐标不一致已证实，其来源是否为 CUA、JBR 或多显示器转换未定位，不推广为所有物理鼠标问题。

安装包 `AbstractPopup$Canceller` 用 content 屏幕矩形和 `MouseEvent.getLocationOnScreen()` 判断 mouse-out；在 pressed 且现有未固定回调返回 true 时立即 cancel。`ActionButton` 的 accessibility action 直接构造 `MOUSE_CLICKED`（ID 500）调用动作，不能用它解释日志中的 `MOUSE_PRESSED`（501）。日志和 SDK 证据位于本机 `input-diagnostics/`，未引入生产诊断依赖。

## 修复与针对性验证

先更新 proposal/design/spec，再加入包内 `PopupMouseOutPolicy`，由 Preview/Param 两个回调共用。内容根或后代的事件不请求外部取消；其余仅在 MOUSE_PRESSED 且未固定时请求取消。保持 Escape、窗口生命周期、焦点和原控件事件处理，不做坐标补偿、不调用 Internal API。

独立只读审阅未发现阻止继续的问题。`PopupMouseOutPolicyTest` 3 项和 `PreviewLifecycleTest` 12 项实际运行通过，合计 **15 tests / 0 failures / 0 errors / 0 skipped**；策略覆盖矛盾屏幕坐标的内部 root/后代、外部按下、Pin 和其他鼠标事件。这不等于 JCEF/native wrapper 或所有输入路径已验收。

## 资源与未完成项

### full-12 重建与无 agent 原生复验

Java 25 执行 `./gradlew test buildPlugin verifyPluginProjectConfiguration verifyPlugin --rerun-tasks --console=plain`，**69 suites / 358 tests / 0 failures / 0 errors / 0 skipped**，18 tasks 实际执行，2 分 55 秒。ZIP SHA-256 `0f889a87d12417c56b1bc0189ec43bdde5f3052db69068fa26c72c9fb7a426ab`，JAR SHA-256 `54d43d05e30b018ac68954b8555d91bd65e6efa3ffa9fa6697faa0ce8354cc68`，330 个生产 class 全部 major 69。EAP `263.7031.23` 与正式 `263.7307.200` 的 Verifier 都 Compatible：compatibility/internal/deprecated/override-only 各 0，Experimental 仍 16。正式版依赖报告另有 51 个产品内嵌套 failed edges（39 标 optional、12 未标 optional），Doc View 根直接依赖均解析；保留该诊断，不表述为所有依赖图节点均无问题。

PID `26316` 使用同一隔离 profile，`VM.command_line` 确认没有诊断 javaagent。原生复验结果：

- **Copy 通过**：点击预览 Copy 后弹窗保持，Escape 关闭后在 IDEA 文件搜索框用系统粘贴读回 `findUser` 的完整 Markdown 内容，包含路径、GET、header、参数和响应。搜索框将换行折成空格，不把它当成字节级剪贴板摘要。
- **预览 Export 通过**：原生选择独立 `full12/exports/`，观察到成功通知；磁盘 `findUser.md` 为 604 bytes，SHA-256 `a6fd207dd31704213880452682912113bc1e98be47364c60ac2dba28044f7514`，路径、header、query 与预览一致。
- **参数代表写回通过**：Description 输入引号/反斜杠、Required false→true、Confirm、保存、磁盘读回和一次 Undo 恢复均通过；写回文件 SHA-256 `ec9b0f73bfe30a09e045d9e8eab531778e267bcdb711075c4817152da92f0a10`，Undo 后恢复原 SHA。不替代换行、Javadoc、多层 DTO 等完整矩阵。
- 五项主工具栏可见；Options 点击仍未取得可读菜单。完整菜单/右键、主题/语言范围继续未验，不用截图或源码补成通过。

full-12 关闭操作后的 GC 样本为 Preview `0/0/0/0`、Param `1/2/2/1`、MarkdownJCEFHtmlPanel `0/0/0/0`；最后一次采样前 EDT 栈在 `EventQueue.getNextEvent` 等待。这个数量没有显示线性累积，但 SDK 和历史 MAT 路径确认静态 `NODE_RENDERER` 的 `myTree` 强持有最后渲染树，树的 `myTreeTable` 回指参数表，进而保留父组件/表单。决定移除这个错误的静态所有者，再重建复验；不在共享 renderer 上清空平台字段。full-12 已通过原生 Exit 退出，普通 IDEA 保持运行。

### full-13 renderer 修复与自动复验

移除 `ParamTreeTableView` 静态 `NODE_RENDERER` 和覆盖的 setter，改在父构造结束后创建并安装各表格独有的 renderer。字段名称渲染规则不变，不手动清空平台字段，不新增 API、依赖或兼容分支。新增测试验证两个表格的实际 renderer/渲染组件独立，分别指向自己的树；参数表 4 项、popup policy 3 项及 Preview lifecycle 12 项针对性测试实际通过。测试初次把平台 renderer wrapper 强转为 NodeRenderer 失败，改为检查实际渲染组件后通过；这是测试断言修正，不是第二个产品故障。组件测试与独立静态审阅不能代替关闭后 GC 验收。

Java 25 再次运行 `./gradlew test buildPlugin verifyPluginProjectConfiguration verifyPlugin --rerun-tasks --console=plain`，结果为 **69 suites / 359 tests / 0 failures / 0 errors / 0 skipped**，18 tasks executed，1 分 54 秒。保留 test-only deprecated/removal、JDK native-access 和 Gradle 后续版本警告，不宣称全项目零警告。

| full-13 产物 | 实际读回 |
| --- | --- |
| ZIP SHA-256 | `a965d5126617eca09031e3372dbc238cb09c9507877a1d11659ce2e8525587a1` |
| ZIP 大小 / 内容 | 722,810 bytes；一个插件 JAR，无测试依赖 |
| JAR SHA-256 | `8140098d85e523a8c0ff0f23f2a7f1b112ae6af0c50d71deaff019459751d29e` |
| 生产 class | 330 个，全部 major 69 |
| Verifier | `IU-263.7031.23`、`IU-263.7307.200` 均 Compatible |
| API 报告 | compatibility/internal/deprecated/override-only 各 0；Experimental 16，与 full-12 相同 |
| 正式版依赖诊断 | 51 个嵌套 failed edges（39 optional、12 未标 optional），集合与 full-12 相同；根直接依赖均解析 |

证据在 `build/native-validation/2026-11-06/full13/`：`build.log`、`tests.json`、`package.json`、`source-manifest.json` 和 `release-verifier/`。228 个生产/构建输入已冻结；生产最低 build 和平台输入保持 `263.7031.23`。

full-13 安装到已停止的隔离 profile 后启动 PID `33806`，JBR `25.0.4.1`，config/system/log/plugins 均为隔离目录，`VM.command_line` 无 `-javaagent:`。首次原生连接返回 “The Mac is locked and automatic unlock could not unlock it”，因此未操作参数或预览。启动基线 live histogram 的 `PreviewForm`、`ParamDocEditorForm` 与 `MarkdownJCEFHtmlPanel` 均为 0；这个启动前样本不能证明使用后释放。解锁后应在同一包重新完成参数 Confirm/Undo、代表 Copy/Export、重复打开关闭与 EDT 空闲后的 live GC 采样，再决定 renderer 修复是否原生通过。

### full-11 历史阶段采样

PID `24022` 在参数与预览关闭操作后的 live class histogram 显示 `ParamDocEditorForm=1`、`PreviewForm=1`、`MarkdownJCEFHtmlPanel=0`。单一样本不证明长期泄漏或资源完全释放；等状态循环、EDT 空闲后的引用分析和 JCEF/native memory 基线仍未完成。

该实例通过原生 Confirm Exit 退出，随后进程检查确认 PID `24022` 已不存在，普通 IDEA PID `14893` 仍运行。R2、4.2 继续未勾选，正式 MCP 仍等待原生与文档库门槛。

## 本轮范围与收尾

full-13 的 228 个生产/构建输入 hash 与当前工作树全部一致；相对本轮开始冻结快照，15 个变更路径均为这两处修复、针对性测试及对应 OpenSpec/发布说明/状态文档，HEAD 未改变，未 stage、commit、push、publish、sync 或 archive。OpenSpec strict validation 和 `git diff --check` 通过，94 条本地 Markdown 链接可解析。发现一条历史 `/tmp` 原生报告已不存在，已改成明确标注不可复读的历史路径，未补造证据。

普通 IDEA PID `14893` 保持运行；其 profile 的 3,622 个冻结文件中 3,621 个不变，只有运行中的 Air session-identities 文件变化，无缺失，未恢复或覆盖该运行态文件。普通与隔离安装的 18 个 Junie JAR hash 全部相同，Junie 保留。虚构 Schema 文件已恢复到原 SHA-256 `ab9d766cf7e040391066154d5e1068093e3ff5896b93367f262b39532db65e91`。诊断 agent 仅用于先前隔离进程，full-13 无该启动参数；本轮未启用 MCP Server。

Mac 锁定期间保留隔离 PID `33806`，供解锁后继续使用同一候选包；不把这个待验实例描述为已完成原生 Exit 或资源释放。机器可交互后，下一步先复验参数编辑/Confirm/Undo、Copy/Export，再完成重复开关与 GC，之后补菜单可达性和其余资源边界。
