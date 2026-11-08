# 参数底部纯图标与弹窗续验（2026-11-08）

维护者已批准仅使用 macOS 激活接口切换隔离 IDEA Integration Test 与 Finder，其他点击仍用 CUA；随后依据实机截图要求去掉参数底部的常驻按钮文字。本轮仅在独立整合工作树实施，保留原工作区和 Junie。

## 实现与行为边界

参数底部按顺序保留三个动作，直接复用已有平台图标：

| 图标 | 完整动作名 | 现有处理 |
| --- | --- | --- |
| `AllIcons.FileTypes.Json` | Copy JSON / 复制 JSON | 复制字段示例 JSON |
| `AllIcons.FileTypes.Markdown` | Copy as Markdown / 复制为 Markdown | 复制当前编辑的参数表 |
| `AllIcons.Actions.Checked` | Apply to Source / 写回源码 | 将描述与必填信息写回源码 |

移除 JSON 和写回动作的 `SHOW_TEXT_IN_TOOLBAR` 常驻文字设置；三个 action 的本地化名称和说明保留，供平台 tooltip/无障碍访问使用。不新增依赖、设置或图标资源，不改变原处理函数、复制结果、写回、Undo 或 Pin 策略。公开图标字段已在目标 SDK 核对；平台建议优先[复用现有图标](https://plugins.jetbrains.com/docs/intellij/icons.html)。

本次要求取代 11 月 7 日的短文字设计，已同步 proposal/design/spec/tasks 和用户指南。原有动作展示测试同步为三个不同图标、无常驻文字、名称与说明非空；没有用删除动作来缩短工具栏。

## 自动验证与包身份

- RED：旧实现仍用 Copy 图标，更新后的既有展示测试失败，消息为 `expected fileTypes/json.svg, was actions/copy.svg`；保留 `icon-red.log/xml`。
- 第一次完整检查因会话中断而未完成，只有 Verifier 局部结果，未计全量通过；保留 `icon-full-gates.log`。
- 恢复后串行执行 `test buildPlugin verifyPluginProjectConfiguration verifyPlugin --rerun-tasks --no-build-cache`：**77 suites / 398 tests / 0 failures/errors/skips**；`BUILD SUCCESSFUL in 1m 46s`，18 个任务实际执行。测试 XML 和 `icon-full-gates-resumed.log` 已冻结。
- 最低 build `263.7031.23` 与正式 build `263.7307.200` 的 Verifier 均为 Compatible、16 项既有 Experimental usages，无 compatibility/Internal/Deprecated/Override-only 问题。正式版以冻结 ZIP 单独离线验证，逐份读取 verdict 与实际报告文件；两个 Verifier 未同时运行。
- ZIP SHA-256：`9599ca7c9204b2cce4246184820993e9937471f810beaecdec2d17c1f096167a`；唯一 JAR SHA-256：`a3a35b591ff2c30235ead35135cefcd530a3eba540f8cc04f8b5e8e7eedc9768`。337 个生产 class 均为 major 69，无诊断 overlay。

证据目录为 `build/native-validation/2026-11-08/popup-activation/`；新包在其 `icon-candidate/` 中。1.5.0 仍只是本地验证标识，未覆盖公开版本或发布新 release。

## 原生结果与剩余范围

图标修改前先将上一轮图标缓存修复包安装到主隔离 profile，启动 PID 18212；实际打开参数弹窗，截图仍是 Copy JSON / Apply to Source 的旧常驻文字。Finder 临时窗口指向虚构项目，切到列表视图。一次补充桌面截图在采集时前台已切为其他应用，不能作为 IDEA → Finder 遮挡证据，未加入仓库或报告图片。

收到按钮反馈后关闭参数弹窗并正常退出隔离实例，没有写回源码。随后 CUA 从会话工具中移除，不能继续使用它检查新图标、悬停提示、复制/写回和剩余弹窗矩阵。已有授权仅扩展应用激活，不扩展 AppleScript 点击、按键等替代控制，未扩大操作范围。

因此新图标的原生中英/主题/窄窗、三个动作点击及剩余 Pin/预览跨应用、返回草稿、设置/导出子窗口均记为 **unrun**。原生证据仍绑定各自旧包，不借新的自动通过结果覆盖旧包边界。5.11 和整体 5.8/5.9 保留未完成；JNI 热卸载问题仍按[前一轮独立记录](../intellij-263-baseline-upgrade/verification-2026-11-08-unload-api.md)保留未通过，本轮没有重验或宣称解决。

正式 MCP、版本升级、tag、GitHub Release 和 Marketplace 发布均未执行。原工作区、普通 IDEA 与 Junie 保留；临时 Finder 窗口尚未通过 CUA 恢复分栏/关闭，留待原生工具恢复后收尾。

收尾已将新 JAR 安装到关闭的主隔离 profile，并保留上一包备份；安装 hash 与上述冻结包一致，没有启动新包或修改激活配置。原工作区 584 个文件及 HEAD、原 fixture 15 个文件 hash 均未变。相对前一轮 231 项生产/发布输入，本轮仅改变 ParamDocEditorForm 和 CHANGELOG；新 manifest、安装记录和保留检查分别见 `icon-candidate/source-manifest.json`、`icon-install.json`、`preservation.json`。本轮文档和原生待验状态不能替代下一轮实际运行新包。

## 获准使用辅助功能后的原生续验

维护者随后明确允许临时使用 macOS 辅助功能接口，仅操作隔离 IDEA Integration Test 与虚构项目 Finder 窗口。CUA 仍不可用，本轮改用限定 PID/实际 bundle 路径的 AX 读取、系统鼠标/按键及前台检查；不操作普通 IDEA、账号许可或 Junie 对话。截图仍限获准的测试应用前台。复制测试临时保存并恢复原剪贴板，只有已匹配虚构字段的输出落入 ignored 证据文件。

主隔离 profile 启动 PID `23742`，确认 JAR SHA 为本页第一包 `a3a35b…e9768`；IU `263.7307.200` / JBR `25.0.4.1+1-635.79-nomod`。以下实际结果绑定该包及英语、深色、800×500 逻辑像素参数弹窗：

| 项目 | 实际结果 |
| --- | --- |
| 三个底部按钮 | 纯图标可见，JSON、Markdown、写回外形不同；AX 名称分别为 Copy JSON、Copy as Markdown、Apply to Source |
| JSON 复制 | 原生点击后获得有效 JSON，包含 displayName、age；依既有行为关闭弹窗，无源码写入 |
| Markdown 复制 | 界面编辑描述为 `Native icon draft 20261108` 后复制，字段表包含编辑值；源码哈希不变 |
| Schema 写回与 Undo | 界面将 displayName 描述和 Required 改为测试值/true；主动写回后，编辑器及磁盘均读到 description 和 REQUIRED；Undo 后哈希恢复为 `ab9d766cf7e040391066154d5e1068093e3ff5896b93367f262b39532db65e91` |
| 参数 Pin 开/关跨应用 | 两种状态分别真实激活 Finder，前台窗口无参数弹窗遮挡；返回 IDEA 的 AX 行仍含未确认草稿，源码未变；Pin 关闭另取离开按钮后的无选中背景截图 |
| 参数设置子窗口 | Settings 正常显示，Cancel 可点击，不触发写回；本项不替代预览的子窗口矩阵 |
| 浮动 tooltip | 合成鼠标能产生高亮和状态栏说明，但该方式下 Settings 对照也未浮出提示；不能宣称悬停提示已通过 |

首次过早读取异步表格几何时失败，后续独立复验才观察到成功编辑/写回；失败尝试未改变源码。`postToPid` 输入方式未可靠生效，亦不作为操作通过证据。证据及结果在 `build/native-validation/2026-11-08/native-icon-followup/` 的 `icon-v1-native-results.json`、AX 快照、截图和 fixture 输出中。进程经 Confirm Exit → Exit 正常结束。原工作区 584 文件/HEAD、原 fixture 15 文件再次核对不变。

## 提示说明的公开 API 修复

对照精确平台 [ActionButton](https://github.com/JetBrains/intellij-community/blob/idea/263.7307.200/platform/platform-impl/src/com/intellij/openapi/actionSystem/impl/ActionButton.java) 发现：匿名 AnAction 只设置 description，默认不会把它放入浮动提示。平台仅对白名单动作或实现公开 [TooltipDescriptionProvider](https://github.com/JetBrains/intellij-community/blob/idea/263.7307.200/platform/platform-api/src/com/intellij/openapi/actionSystem/ex/TooltipDescriptionProvider.java) 的动作显示说明。该缺口独立于合成鼠标不浮出提示的问题，违反本 change 已要求的“操作名及说明”。

既有展示测试增加对三个实际 footer action 的 marker 断言，旧生产实现 **1 test / 1 failure**，失败消息为 `Footer actions must opt into ActionButton's description tooltip`。随后仅让这三个动作继承实现该公开 marker 的私有 AnAction 基类，保留原处理函数、文字、图标。没有自绘 tooltip 或增加依赖。`tooltip-red.log/xml` 保留失败证据；修复后的包和复验在下节单独记录，不把上表直接转记到新包。

## 提示修复包的最终结果

- 串行执行 `test buildPlugin verifyPluginProjectConfiguration verifyPlugin --rerun-tasks --no-build-cache`，**77 suites / 398 tests / 0 failures/errors/skips**，`BUILD SUCCESSFUL in 1m 53s`，18 个任务实际执行。17 条 test-only 编译警告及 Gradle 10 迁移提示仍存在。
- 最低 `263.7031.23` 与正式 `263.7307.200` 的 Verifier 1.411 均为 Compatible、16 项既有 Experimental usages；逐份核对报告，无 compatibility/Internal/Deprecated/Override-only 问题。
- ZIP SHA-256：`f96fd471cbbff311fbdc321f6133fb9b74c6fef0beda4ad8b2d46b48dc97c56a`；唯一 JAR SHA-256：`8f6b98b76b030eb59d175bb13ea64d7dbbc2733512a4068cce430312d43b9f0f`。338 个生产 class 均为 major 69，无诊断 overlay。相对前一包的 231 项生产/发布输入，仅 ParamDocEditorForm 和 CHANGELOG 变化；全量完成后输入哈希未再改变。
- 冻结包、manifest、测试 XML、两个 Verifier 报告在 `native-icon-followup/tooltip-candidate/`、`tooltip-test-results/`、`tooltip-minimum-verifier/`、`tooltip-release-verifier/`。此前第一包的报告仍保留，不混用包身份。

安装读回哈希后启动 PID `29967`，仍使用上述正式 IDEA/JBR、激活隔离 profile 和虚构项目。**以下是修复包的实际复验**：

| 项目 | 结果与证据边界 |
| --- | --- |
| 英语深色纯图标与名称 | 通过：800×500 参数弹窗三个图标可辨，名称完整；不代表中文、浅色、Compact、窄窗或完整键盘矩阵 |
| JSON / Markdown 复制 | 通过：再次点击得到有效 JSON；编辑后的 `Tooltip package draft 20261108` 进入 Markdown 字段表，源码哈希保持原值；剪贴板测试后恢复 |
| 描述、必填写回与 Undo | 通过：主动点击写回，将 description 改为 `Tooltip package writeback 20261108`，requiredMode 改为 REQUIRED；磁盘内容取证后原生 Undo 恢复原 SHA |
| 参数 Pin 开与关 | 通过：每种状态都真实激活 Finder、截图检查无遮挡，再激活 IDEA；返回后 AX 表格保持草稿，Markdown 复制可继续执行 |
| 预览 Pin 开与关 | 通过：分别取得未选中/选中 Pin 截图，真实 Finder 往返无遮挡；返回后 findUser 文档内容与之前一致，预览操作仍可到达 |
| 预览设置/导出/上传选择取消 | 通过：设置子窗口可见并 Cancel；Select Path 可见并 Cancel，既有导出目录 1 文件哈希未变且无新增；上传三平台选择菜单可见并 Escape 取消，未选择平台、未发起上传；随后 Escape 可关闭预览 |

### 悬停提示模式对照与恢复

实际打开隔离 IDEA 的 Accessibility 设置，`Support screen readers` 为选中，平台就近说明明确写明此模式会禁用鼠标悬停 tooltip。因此此前两个包的无浮动提示不能单凭合成输入归因为产品缺陷。

仅为验证普通显示模式，临时取消隔离实例该设置，经正常退出后以同一 profile/同一 JAR 启动 PID `30708`。三个按钮分别悬停后均取得完整截图：

- Copy JSON：显示复制字段示例 JSON 的说明。
- Copy as Markdown：显示复制当前编辑字段为 Markdown 表的说明。
- Apply to Source：显示将描述和必填信息写回源码的说明，文字正常换行。

对应 `normal-json-tooltip.png`、`normal-markdown-tooltip.png`、`normal-apply-tooltip.png` 及 AX 快照。此为英语深色实测通过，不是仅用 marker 测试推断 UI。没有修改系统无障碍设置或普通 IDEA，也没有将关闭屏幕阅读当成 JNI 绕行修复。验收后恢复隔离 `Support screen readers=true`，通过 UI Apply/OK 生效并正常退出；`profile/config/options/ide.general.xml` 读回确认该值。

### 有限 GC 观察与剩余门槛

PID `30708` 关闭参数弹窗、恢复设置后，`15:00:18` 的首次 live histogram 仍有 ParamDocEditorForm 和 ParamTreeTableView 各 1；`15:01:33` 第二次 live GC 后这两类实例均为 0。保留两份仅含插件类的 histogram，未对激活 profile 抓取堆。该观察不覆盖完整性能、CEF native peer、监听器或卸载；不能由稍后实例归零声称 JNI 类加载器问题已修复。

5.8 的跨应用与子窗口取消范围完成；5.9、5.11 的中文/浅色/Compact/窄窗/键盘与完整资源矩阵继续保留未完成。JNI 目录热卸载仍按[公开 API 排查](../intellij-263-baseline-upgrade/verification-2026-11-08-unload-api.md)记为未通过，本轮未在改变屏幕阅读模式后重做或宣称热卸载通过。

收尾确认三个本轮隔离进程均已退出；Finder 虚构项目临时窗口恢复分栏后关闭；Junie/Air 与普通 IDEA 保留。原工作区 584 文件/HEAD、原 fixture 15 文件均未变，最终候选 231 项输入哈希一致，证据为 `preservation-final.json`。正式 MCP、新版本号、tag、GitHub Release 与 Marketplace 发布均未推进。
