# 2026-11-07 原生续验与 UI 修复

## 状态与范围

本文件保留 full-13 至 full-16 的阶段证据；最新 Keymap 修复与同包原生结果见 [full-17 续验](verification-2026-11-07-full17.md)。

Mac 解锁后继续 full-13 的正式 `IU-263.7307.200` 隔离原生验收，先核对安装包和源码身份。随后根据用户反馈追加说明字体、全部图标审查，以及弹窗跨应用置顶修复；这些可见行为已先写入 [HTTP/UI change](../http-client-generation-and-ui/specs/doc-view-action-layout/spec.md)。新代码的构建与原生结果单独记录，不能沿用 full-13 的通过结论。

普通 IDEA、Junie 和生产最低 build `263.7031.23` 保留。不发布、提交、归档或同步 specs；正式 MCP 继续等待基线和文档库验收门槛。

## full-13 包身份与原生结果

隔离 PID `33806` 的安装 JAR SHA-256 为 `8140098d85e523a8c0ff0f23f2a7f1b112ae6af0c50d71deaff019459751d29e`，对应 ZIP SHA-256 `a965d5126617eca09031e3372dbc238cb09c9507877a1d11659ce2e8525587a1`；228 项冻结源码/构建输入全部匹配。`VM.command_line` 无诊断 javaagent，config/system/log/plugins 均位于隔离 profile。完整自动门禁仍引用 [full-13 的 359 tests 和两个 Verifier 记录](verification-2026-11-06.md)。

本机忽略证据目录为 `build/native-validation/2026-11-07/`。仅使用虚构 fixture，不操作普通项目。

| 场景 | 观察与边界 |
| --- | --- |
| 参数写回 | Description 输入引号和反斜杠，Required false→true，Confirm、保存后源码为正确转义的 `@Schema(description=..., requiredMode=REQUIRED)`。写回 SHA-256 `ec9b0f73bfe30a09e045d9e8eab531778e267bcdb711075c4817152da92f0a10`；一次 Undo 并保存恢复原 SHA-256 `ab9d766cf7e040391066154d5e1068093e3ff5896b93367f262b39532db65e91`。 |
| 参数表重复打开关闭 | 含写回在内共三轮；字段显示正确，Escape 可关闭，额外两轮没有提交修改。 |
| 预览 Copy | 点击 Copy 后弹窗保留；关闭后通过 IDEA 文件搜索框系统粘贴读回 findUser 的 Markdown，含 GET、路径、header、参数和响应。搜索框折叠换行，未做字节级剪贴板摘要。 |
| 预览 Export | 原生目录选择器选中独立 exports 目录并观察成功通知。`findUser.md` 为 604 bytes，SHA-256 `a6fd207dd31704213880452682912113bc1e98be47364c60ac2dba28044f7514`，与 full-12 相同。 |
| 切换应用 | Preview 未固定时切换到 Finder，IDE 的 AX 状态仍存在弹窗。此项证明弹窗仍保留，单应用截图不能证明跨应用合成后的实际遮挡层级；结合用户的置顶反馈与 SDK 默认窗口类型定位修复，不将该观察写成完整 z-order 复现。 |

## full-13 资源与日志

关闭表单、结束本轮操作后，最终 live class histogram 中 `ParamDocEditorForm`、`ParamTreeTableView`、`PreviewForm`、`MarkdownJCEFHtmlPanel` 的精确实例计数均为 0。EDT 在 `EventQueue` 等待，无 Doc View 栈帧。此前一次快照的 PreviewForm=1 已在最终样本降至 0；参数表及表单也由早期样本的 1 降至 0。

这支持本次使用后 Java 表单可回收；仍存在匿名列类、lambda 和平台 companion，不仅凭类名判断它们保留表单。未证明 JCEF native peer、监听器、项目关闭/在途卸载或长期内存和性能全矩阵通过。最终原始 histogram/Thread.print 为本机权限 0600 文件，摘要在 `gc/final-full13-summary.json`；身份与日志在 `full13-input-resource-audit.json`。当前进程启动后日志 ERROR/FATAL=0、WARN=14，IDE 错误图标的来源尚未确定。

## 新 UI 修复的根因与验证安排

- 设置两段说明来自只读 JBTextArea，修复前继承 TextArea 字体和正文色。先扩展布局测试，实际 RED 为 4 tests / 1 failure，失败是说明颜色与平台 context-help 颜色不一致。随后改为 `JBUI.Fonts.smallFont()` 和 `UIUtil.getContextHelpForeground()`，沿用已有换行、只读、非焦点和设置保存行为。证据为 `settings-red.log/xml/json`。
- [全部图标引用审查](../http-client-generation-and-ui/icon-audit-2026-11-07.md)涵盖 Java、plugin.xml、form、自有 SVG、New UI 映射和 HTML。保留平台动作/节点图标和品牌资源；参数复制动作由 JSON 文件类型图标改为 Copy，配“Copy JSON / 复制 JSON”，确认图标保留并配“Apply to Source / 写回源码”。未启用的旧浮动工具栏 Save 绑定批量导出是 dormant 声明，记录为未来启用前须修正的事项，本轮不启用或扩展该功能。
- 两种弹窗拟通过公开 `ComponentPopupBuilder.setNormalWindowLevel(true)` 使用普通应用窗口层级，保留 Pin 和未确认草稿；不通过失去应用焦点即销毁弹窗来规避问题。原生切换应用、返回继续编辑及子窗口需在新包上验证。

参数动作的测试首次因未更新 ActionToolbar，未能读取实际动作而失败；该次仅属测试设置问题，原始 XML 单独保存。改用公开 toolbar place 定位并显式更新动作后，得到有效 RED：1 test / 1 failure，预期 Copy JSON、实际 Copy as JSON。随后才修改生产按钮与消息资源；不将测试设置失败记为产品缺陷。

R2、4.2 和 HTTP/UI 完整原生矩阵保持未勾选，后续结果按真实包身份补充。

## full-14 构建与原生复验

针对性回归 6 suites / 28 tests 全绿。随后 Java 25 执行 `./gradlew test buildPlugin verifyPluginProjectConfiguration verifyPlugin --rerun-tasks --console=plain`：70 suites / 360 tests / 0 failures/errors/skips，18 tasks 实际执行，1 分 55 秒。ZIP SHA-256 `add5c402cc0754c973819f67fb4b90b931fa5be963b89e83e7fd45030b25d72c`，723,538 bytes；JAR SHA-256 `196773affd9cb6de189b48d3c5fd506dfeb424f5cb9616cf2184078839aec3c1`。330 个生产 class 均 major 69，单一插件 JAR，229 项生产/构建输入冻结。

首次两个 Verifier 并发运行共用 `~/.pluginVerifier/extracted-plugins`，正式版进程因 DirectoryNotEmptyException 失败，EAP 日志也出现缓存 delegate 清理错误。这是验收流程问题，保留失败日志，未将该次作为最终通过依据。改为串行后两个精确 build 均 exitCode=0、Compatible，16 Experimental，production compatibility/internal/deprecated/override-only 均无问题；正式版 verdict/API/dependencies 报告与 full-13 相同。

隔离 PID `71361` 的安装 JAR、229 项输入和 profile 路径已独立读回匹配。Junie 的 18 个 bundled JAR 与普通安装一致；MCP Server 仍 Disabled。原生结果：

- 深色设置说明实际显示为小号、灰色 UI 字体；参数底部 Copy JSON / Apply to Source 文字及平台图标可读。
- 参数编辑中切换 Finder，操作其列表/图标视图，再返回，未写回草稿仍保留并可继续修改；Pin 切换前后分别执行。单应用截图无法单独证明桌面合成 z-order，完整遮挡结论仍保持部分验证。
- Apply to Source、正确字符串转义、Required 写回、一次 Undo 和磁盘恢复通过，hash 与上文 full-13 的相同 fixture 一致。
- Copy JSON 系统粘贴读回 `{ "displayName": "", "age": 0 }`；源码保持原 hash。
- Markdown JCEF 渲染显示接口标题、GET、路径和 header 表格；切换 Finder 后返回仍显示。Copy 系统粘贴读回完整 Markdown；Export 产生 604 bytes、SHA `a6fd207dd31704213880452682912113bc1e98be47364c60ac2dba28044f7514`。导出选择器 Cancel 返回编辑器，无成功通知。参数弹窗在未固定时打开设置、再取消，返回源码且未写回，未把这一行为说成保留设置子窗口中的草稿。
- **新发现：浅色主题设置搜索导致说明裁切。** Islands Light 字体与颜色正确，但搜索 Doc View 后左栏从约 500 px 扩至 580 px，右侧仍按旧宽排版，第一段末尾被裁切。重新打开设置后出现水平滚动条；之前只对 root 直接 setSize 的测试未覆盖外层 viewport。该布局项失败，修复后须新包复验，不标整体 UI 通过。

证据位于 `build/native-validation/2026-11-07/full14/`；其中 `native-files.json` 记录 Undo/Export 读回。浅色主题仅在隔离 profile 临时启用，后续验收结束恢复原 Islands Dark。全量中英、Compact、窄窗、键盘/菜单与完整资源矩阵仍需补齐。

full-14 原生操作刚结束时 live histogram 为 ParamDocEditorForm=1、ParamTreeTableView=1、PreviewForm=0、MarkdownJCEFHtmlPanel=0；静置数分钟后的第二个点四者均为 0，EDT 等待且没有 Doc View 栈帧。因此未将第一个点直接定性为泄漏，也未额外生成 heap dump。此次实际使用过 JCEF，但 Java panel 归零仍不等于 CEF native peer 或整个资源/性能矩阵通过。原始文件及 `idle-post-quiet-summary.json` 位于 full14/gc，权限 0600。


## full-15：自动检查通过，外层设置布局复验失败

第一轮宽度修复为设置根 panel 实现 Swing `Scrollable`，在直接 `JBScrollPane(root)` fixture 中取得 RED→GREEN。补充高度断言时，未显示的 Swing fixture 未刷新 BoxLayout 的缓存，先修正测试的 invalidate/layout 顺序，再临时恢复普通 JPanel 确认有效 RED；该测试设置问题与产品问题分别保留。

针对性回归为 2 suites / 9 tests。完整命令再次执行全部 18 tasks，1 分 56 秒；70 suites / 361 tests，failures/errors/skips 均 0。ZIP SHA-256 `0cffa13ba0d6bec3fb2cf0925ecda43085045235a50e882c7a0ee25a275076ae`，724,494 bytes；JAR SHA-256 `52ee8387dfbf151cd770a9b89bed0b8fe518717bc69c44f9084f9d653fac2f83`，331 classes 全为 major 69。229 项源码/构建输入全匹配。EAP 与正式版 Verifier 串行运行，exitCode 均 0，Compatible / 16 Experimental；verdict/API/dependencies 报告与 full-14 相同，缓存 failures=0。

隔离 PID `72962` 安装并加载该包。浅色首次打开说明为小号灰色 UI 字体，搜索后无明显裁切；但切回 Islands Dark 再搜索 Doc View，侧栏从约 500 px 变为 580 px 后第一段末尾再次裁切。关闭并重新打开设置还能看到底部水平滚动条。**因此 Scrollable 根 panel 不足以解决真实 Settings 的外层包装，full-15 原生布局验收失败，不沿用直接 viewport 测试的通过结论。** 两次尝试拖动窗口/分隔条未改变尺寸，不计入原生窄窗通过。

本轮已通过原生 UI 恢复原 Islands Dark；隔离实例正常退出，MCP Disabled，Junie 保留。本包只补验设置布局，没有重复 Copy/Export/写回或 GC；这些结果仍只归属 full-14。证据目录 `build/native-validation/2026-11-07/full15/`，后续继续修复外层布局再重建复验。


### full-15 后续根因与有效 RED

核对 263 `ConfigurableCardPanel.decorate()`：平台对运行时 class 不严格等于 JPanel/DialogPanel 的组件加普通 JPanel 包装；SettingsRootPanel 是子类，内层 Scrollable 因而不是外层 viewport 的直接 view。此前直接 JBScrollPane(root) fixture 跳过了这一层，未覆盖原生失败。

回归改为实际调用平台 `ConfigurableCardPanel.createConfigurableComponent(SettingsConfigurable)`，再缩小外层宽度；旧生产得到 1 test / 1 failure：`The settings view must follow the narrowed viewport width`。初次命令误用了重命名前的测试过滤器，0 tests / command failure 日志单列，不算产品 RED。准备通过公开 `Configurable.NoScroll` 和自有 JBScrollPane 使 viewport 直接管理 Scrollable root；平台包装类只在测试使用，不加入 production API 依赖。


## full-16 最终 UI 修复与结果

SettingsConfigurable 采用公开 `Configurable.NoScroll` 并返回去边框的自管 JBScrollPane。横向滚动策略保留默认 AS_NEEDED，没有靠强制隐藏横条掩盖溢出；Scrollable root 跟随可用宽度，垂直内容自然滚动。真实 ConfigurableCardPanel fixture 与 ConfigurationExperienceTest、参数动作测试合计 3 suites / 12 tests 全绿。

最终 Java 25 完整命令重新执行 18 tasks，1 分 57 秒；70 suites / 361 tests，0 failures/errors/skips。ZIP SHA-256 `44190d528720b9d589c170e823d42071f4b7207c25782e53044a564b1820c4ef`，724,731 bytes；JAR SHA-256 `67c7493b9056144cc5a33b35fed6bb49f05ea8d49189ad2725ee79a75c358ce9`。331 classes 均为 major 69，单一生产 JAR，229 项输入全部匹配。相对 full-15 生产输入仅 SettingsConfigurable 与 SettingsForm 注释变化。

EAP `263.7031.23` 与正式 `263.7307.200` Verifier 串行完成，报告均 Compatible、16 Experimental，无 production compatibility/internal/deprecated/override-only 问题；test source 编译仍有 15 条 deprecated/removal warnings，Gradle 10 弃用提醒也保留，不写全项目零警告。release CLI 日志正常完成且缓存 failures=0；会话被用户更新指令中断后 process session 已丢失，本轮未补造该 CLI 退出码，结论依据已完成的 verdict/API 报告。EAP 包含在 exitCode=0 的完整 Gradle 中。

正式版隔离 PID `75803` 加载该包。原生设置验收如下：

| 场景 | full-16 结果 |
| --- | --- |
| 深浅主题说明 | 两段帮助均为小号比例 UI 字体、主题说明灰色，低于复选框正文层级。 |
| 初次打开/搜索 | Doc View 根页与搜索结果显示完整，无底部水平滚动条。 |
| 原裁切路径 | 通过 Color Scheme 的 Change IDE Theme 切回 Islands Dark，再搜索 Doc View。侧栏从约 500 px 扩至 580 px，第一段从一行变为两行，`scanning.` 完整显示；第二段完整换行，无右侧裁切。 |
| 关闭重开 | Cancel 后重新打开设置，内容完整且无水平条，full-15 的失败不再出现。 |
| 设置草稿取消 | 临时选中 Recognize and scan ordinary interfaces，Cancel 后重开仍未选中，Apply disabled；没有保存该测试草稿。 |
| 恢复 | 原 Islands Dark 已通过 UI 恢复；MCP Disabled，Junie 保留。 |

本包新增原生范围仅为设置字体/主题/宽度/取消；没有把 full-14 的 Copy/Export、Schema 写回和 GC 重记为 full-16 实测。相关参数/预览生产代码相对 full-14 未变；full-14 的通过结果保留其包身份。未实际改变窗口尺寸的拖动尝试仍不计入原生窄窗测试。全量中文、Compact、键盘/菜单、跨应用桌面合成 z-order、JCEF native peer/listener 与完整参数/性能矩阵仍未完成。

HTTP/UI task 5.7 可按全量图标审查、字体/按钮实现、自动回归和上述原生证据完成；5.8、5.9 与基线 R2、4.2 保持未勾选。正式 MCP 不因本轮局部 UI 通过而提前实施。不提交、推送、发布、归档或 sync specs。

full-16 验收结束后已通过原生 Confirm Exit 正常退出隔离 PID 75803，退出状态再次读回；最终设置结果与 fixture hash 保存在本机 `full16/native-settings-result.json`。本轮截图为会话中的 CUA 观察，没有另存截图文件，故不提供不存在的截图路径。


最终身份读回确认 config/system/plugins/log 四个路径与隔离 launch 配置完全一致，无 javaagent，安装 JAR 匹配。对 `air-plugin` 目录递归核对的 277 个 JAR 均与普通安装对应文件一致（此前 18 个为较小的 bundled JAR 核对范围），Junie 未被卸载或替换。

日志从本次最后一条 `IDE STARTED`（13:28:41,488）到正常退出，仅统计有时间戳的日志头：INFO 1062、WARN 450、ERROR/FATAL 0。440 条 WARN 来自设置搜索进入 Keymap 时的 ActionsTreeUtil，包含 7 个 Doc View group 缺少 Keymap 可读标题、回退显示 action ID 的既有声明；其余为平台配置、连接、索引和 Junie 后台警告。该组标题问题保留在后续菜单验收范围，不写成全项目零警告或完整菜单通过。一次辅助统计把无时间戳内容也计入 ERROR/WARN，已废弃；权威摘要为 `full16/final-identity-log-readback.json`。
