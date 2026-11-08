# 2026-11-08：新整合包原生续验

状态：**完成下列部分原生验收，发布条件尚未满足。未升级版本、提交、推送、打 tag、创建 Release 或上传 Marketplace。** 原工作区与 Junie 保留；正式 MCP 未推进。

## 验收对象与隔离

- 工作树：`/Users/liuzhihang/workspace/doc-view-release-integration-20261107`；生产/构建输入 230 项与 11 月 7 日最终清单一致。本轮没有修改生产代码。
- ZIP SHA-256：`3b304d5d5945de78686579adae22bdea26af839d7199dde453e761fcccabc9bd`；实际安装 JAR SHA-256：`2fb839b9bf7525c5806eeb4cb59306abd5d96b5349835856162e0c6dbf2ffb5a`。与已通过 395 项测试及两个精确 Verifier 的包一致；本轮没有重跑自动测试，不能写成今日重新全量通过。
- 包内 `1.5.0` 仅为整合验证编号，**不是已公开的 1.5.0 制品，不可直接上传或覆盖公开版本**。
- 原生目标：新建 `build/native-validation/2026-11-08/IDEA Integration Test.app`，实际 IU `263.7307.200`、JBR `25.0.4.1`、PID `96742`。独立 config/system/plugins/log 与虚构项目均在本轮目录内；无 javaagent。MCP 全程显示 Disabled。
- 新 app 的 Junie/`air-plugin` 277 个 JAR 与 `/Applications/IntelliJ IDEA.app` 逐相对路径 hash 一致，没有卸载、禁用或调用 Junie 云端。Air 在空编辑器自动准备 Codex 时取消了该后台下载，未发送对话。
- 原工作区 584 个文件 hash 与 HEAD 前后不变。未写入日常 IDEA 设置；该实例一直独立运行，不纳入本轮资源样本。

原始记录位于 ignored 的 `build/native-validation/2026-11-08/`：`identity.json`、`launch.json`、`native-results.json`、各读回 JSON、日志及 `gc/`。CUA 窗口截图在会话中实际查看；未获得额外桌面截图授权，未用其他技术绕过窗口截图限制。

## 实际通过的场景

| 范围 | 本轮观察与边界 |
| --- | --- |
| 安装加载 | 日志实际加载 `Doc View (com.liuzhihang.doc-view, 1.5.0)`；本轮会话无 ERROR/FATAL、Doc View WARN 或 Doc View 异常栈。平台其他日志不宣称零警告。 |
| 首次生成与查询 | 原生按钮生成 Spring/Feign 共 2 个 API、6 个文件；按 `findUser` 搜索得 1 条并打开对应 Markdown；再次生成显示 No content changes。 |
| 外部修改保护 | 对本轮虚构 API Markdown 追加手工说明，生成操作明确取消且没有覆盖该文件，其余文件 hash 不变。随后从搜索结果打开它，UI 报告文档与索引不一致；恢复测试文件再生成成功。这里验证的是冲突保护与恢复，不是任务中途取消。 |
| 分页 | 新建仅用于本轮验收的 `PaginationController`，含 55 个虚构端点；生成后磁盘索引为 57 个唯一 API ID。搜索 `pageEndpoint` 依次显示 20/55、40/55、55/55，末页 More results 禁用。AX 只暴露 46 个可见结果行，不以它宣称完整 55 行均逐项可见。 |
| 关闭重开 | 原生 Close Project 后从 Recent Projects 重开，UI 显示 `Freshness is unknown · 57 API(s)`；机器索引 SHA 与关闭前一致，没有把磁盘快照标成实时源码。 |
| HTTP 草稿 | 目录选择 Spring 方法，通过原生 Find Action 执行 Generate HTTP Request，生成 `GET {{host}}/api/users/{{path_id}}?q={{query_q}}` 和独立 `X-Trace` header。在旧草稿加入编辑后再次生成，实际创建 `_1.http`；关闭编辑器后磁盘读回旧草稿仍含手工编辑，新草稿未混入该内容。动作没有执行 HTTP 请求。 |
| 参数复制 | Copy as Markdown 实际复制字段表及说明；Copy JSON 实际读回 `displayName` 空字符串、`age` 为 0。复制未修改源码。 |
| Schema 写回与撤销 | 将 displayName 说明改为含双引号、反斜杠的文字，必填 false→true；Apply to Source 后源码正确转义并写入 `Schema.RequiredMode.REQUIRED`，其他字段保持不变。一次原生 Undo + Save 恢复所有原始源码 hash。 |
| 弹窗跨应用交互 | 参数弹窗及预览分别在 Pin 开/关状态切到 Finder，坐标点击可切换列表/图标视图；返回后参数草稿和预览内容保留，仍能继续操作。应用截图不能证明完整桌面合成遮挡，视觉门槛仍未通过。 |
| 预览与导出 | 三次 Spring JCEF 预览均实际观察到内容完成渲染。Copy 读回 Markdown；Export 保存至本轮 exports，`findUser.md` 为 604 bytes，SHA `a6fd207dd31704213880452682912113bc1e98be47364c60ac2dba28044f7514`。Upload 三平台子项可见，Down/Escape 取消，未真实上传。 |
| 设置说明 | 预览 Settings 打开 Doc View 设置，英语、深色、Compact 下说明文字已为较小字号和灰色；Cancel 返回源码，未保存设置。此项不覆盖完整主题、语言、窄窗矩阵。 |
| XML/Dubbo | 通过 XML 关联的 `FictionalXmlService.readXmlUser` 原生文档动作成功渲染，方法为 Dubbo、路径为 `FictionalXmlService#readXmlUser`，说明 XML/DOM 依赖路径可用。默认目录发现仍按注解枚举，不由 XML 反向枚举接口；本轮未把这一既有边界扩展成新扫描功能，也未声称 XML-only 接口已进入默认目录。 |

## 资源与退出

首个 `before_popup` 样本含一个打开的 Markdown 编辑器，不能当作零 JCEF 启动基线。`closed_editors` 是关闭编辑器后的补充样本，仍可能有延迟释放对象。所有样本先取 RSS/FD，再取 live `GC.class_histogram`；未运行独立 `GC.run`，NMT 未启用。cycle2 只取进程/FD。

| 样本 | IDEA RSS KiB | IDEA FD |
| --- | ---: | ---: |
| before_popup | 4,172,576 | 531 |
| closed_editors | 3,883,056 | 531 |
| cycle1 | 3,952,112 | 550 |
| cycle2 | 3,880,880 | 537 |
| cycle3 | 3,885,040 | 537 |
| quiet | 3,893,008 | 535 |

第三次 Spring 预览关闭后的 cycle3 样本尚有最后一个 PreviewForm/JCEF panel/client；约 95.3 秒后的 quiet 样本中，ParamDocEditorForm、ParamTreeTableView、PreviewForm、MarkdownJCEFHtmlPanel、RemoteBrowser、JBCefClient、CefClient 均为 0。共享 JBCefApp/CefApp 各 1；FD 没有逐轮累积。quiet 线程转储没有执行中的 Doc View 栈帧，仍打开工具窗口的 Invoker scope 名称不计为执行栈。

这是一组含真实功能操作的有限资源回归，不是无其他活动的微基准，也不证明 CEF native peer、所有 listener、长期 soak 或完整取消/关闭/卸载矩阵。quiet 发生在后续 XML/Dubbo 预览及项目重开之前，不把该样本覆盖到后续操作。

最终通过原生 Exit 正常退出；PID `96742` 及其 CEF 子进程 `96928/96931/96932/96934` 均已消失。Finder 恢复分栏视图并关闭临时窗口。虚构项目原有源码全部恢复，额外的 55 端点 fixture 保留在新隔离目录供复验。原工作区与整合生产输入再次比对均无变化。

## 首批会话结束时的缺口与发布门槛

本节许可和截图授权状态是首批会话结束时的记录；后续变化见文末同日补充，不作为最新阻塞原因。

- **HTTP 执行受许可阻塞**：实际 UI 显示 `Activate IDE license to use HTTP Client`，Run All disabled。A01/A02/A03 的 loopback 发送未运行；未复制日常许可、申请试用或规避授权检查。已向用户说明可在隔离实例激活，未收到答复。
- **独立菜单与桌面遮挡缺少可观测证据**：工具窗口 Options 和目录右键均没有被当前主窗口 AX/截图捕获。不能据此断言生产菜单故障，也不能记为通过。额外 macOS 桌面截图的询问尚未获答复，因此未执行。
- 尚需完整中英/深浅/Compact/窄窗/键盘矩阵、A/B 项目 Clear Cache 全部保护场景、HTTP 不支持/异常/在途取消/关闭/重试及冷/热各 7 次性能场景。
- 项目文档库仍需在途取消、partial publication/retry 和完整生命周期/资源验收；本轮完成的外部修改冲突不能替代它们。
- HTTP/UI 4.5、4.6、5.4、5.5、5.8、5.9、6.3–6.5，基线 R2/4.2，文档库 5.4 均保留未勾选。
- 正式 MCP 与客户端 Skill 是后续独立 change，不作为本版已交付能力；真实平台上传继续按维护者要求排除。

用户已经授权全部必需项通过后的版本升级、tag、GitHub Release 和 Marketplace 发布，无需重复请求发布授权。但当前条件未满足，未进行任何发布动作；待补齐原生门槛，再选用未占用的新版本、重建正式包、核对安装和渠道制品。发布流程依据 [Release Readiness 技能](../../../.codex/skills/doc-view-release-readiness/SKILL.md) 的 “confirm tasks are complete” 及[发布检查清单](../../../docs/release-checklist.md)。

本轮收尾检查：三个相关 change 的 `openspec validate --strict` 均通过，7 份文档的 106 个相对文件链接目标存在，`git diff --check` 通过。230 项生产输入与原工作区 584 个文件及 HEAD 再次比对无变化；本轮仅同步验收文档，没有重跑或扩大自动测试结论。读回结果留于本轮证据目录的 `closure-checks.json`。

## 同日补充：桌面截图获准与许可交接

维护者随后允许在隔离 IDEA/Finder 测试窗口前台使用 macOS 系统桌面截图，并表示有 Ultimate 授权、将亲自在隔离实例激活。再次启动同一 app/profile 与相同 JAR，PID `99683`；实际 JVM 配置路径及 JBR 读回一致。上节 PID `96742` 的退出结果仅指前一会话，新会话当前保留给用户激活；没有将“有授权”记为“已激活”。

桌面截图补齐了此前单窗口工具遗漏的可见结果：

- 工具窗口 Options 实际显示 Settings、Clear Cache 及平台菜单；键盘选择 Settings 正确打开 Doc View 设置，Cancel 返回。Clear Cache 菜单项可调用，但无确认框或可见反馈，不能以调用本身证明缓存清理与 A/B 项目隔离。
- Spring 方法右键显示 Jump to Source、Open Markdown Document、Export、Upload、Generate HTTP Request；由菜单打开正确的隔离 `config/extensions/Doc View/fixture/SpringController/findUser.md`。
- Spring 类节点只有 Jump to Source、Export、Upload；模块只有 Export、Upload；两者均无 HTTP 动作。Feign/Dubbo/空节点、中英主题等完整菜单矩阵仍未完成。
- 参数弹窗 Pin 开时，桌面图像实际捕捉到其覆盖 Finder 左下部。此前数次 CUA 向 IDEA 输入期间桌面菜单栏仍为 Finder，可能涉及后台事件/后台创建弹窗，尚需真正应用激活切换的复现与 SDK 根因核查。此项仍为未通过，不能把草稿保留和 Finder 可交互当作视觉遮挡已修复。测试草稿最终 Escape 取消，源码未写回。

已打开隔离实例 Help → Manage Subscriptions 交由用户激活；交接期间停止 UI 操作和截图。三种 HTTP 验收 fixture 与 loopback 接收脚本已在 ignored 目录准备，尚未加入源码、启动或发送。补充证据位于 `build/native-validation/2026-11-08/desktop-followup/` 的 `progress.json`、`runtime-identity.txt` 和本机 PNG，桌面图像不加入仓库或发布包。尚未修改生产代码或进行发布。

只读 SDK 核查确认当前 `setNormalWindowLevel(true)` 确实传入平台 popup 实现，show 路径设置 `alwaysOnTop=false`；JBR 仍有根据 owner 激活状态调整子窗口层级的逻辑。因此当前没有证据支持简单再加一遍窗口层级设置，也未将后台输入路径的截图直接认定为普通应用切换的生产缺陷。后续优先确认真正的 IDEA 激活→Finder 激活，再决定是否需要修复。收尾比对原工作区 584 项、生产/构建 230 项以及原虚构源码 15 项均未改变。

## 同日补充：激活后 HTTP 执行通过，热卸载失败

维护者随后回复“已激活”。恢复操作后观察到 HTTP Client 执行入口可用，但激活导致的平台动态重配置同时卸载并重新加载 Doc View。PID `99683` 在 `02:08:19.616` 出现 `Plugins that were expected to unload but are still referenced`，描述符明确为 Doc View；原生通知显示 `Doc View plugin didn't unload fully` 并建议重启。live histogram 中有两份 DocViewWindowPanel，以及分属不同加载身份的 MethodNode（117 与 57）、ClassNode、RootNode。该会话正常退出后重新启动同一 app/profile/JAR，PID `2015`；重启不作为卸载问题已修复的证据。

当前已保存限定的线程、classloader 与类计数诊断至 `desktop-followup/activation-unload/`，没有生成完整 heap dump，也没有读取或复制许可凭据。精确 [263.7307.200 卸载策略源码](https://github.com/JetBrains/intellij-community/blob/idea/263.7307.200/platform/platform-impl/src/com/intellij/ide/plugins/AwaitClassloaderUnloadStrategy.kt) 表明该告警发生于平台等待 GC 后仍能从弱引用取回 ClassLoader。它证明卸载未完成，不能直接证明某个业务字段是根因。[官方动态插件指南](https://plugins.jetbrains.com/docs/intellij/dynamic-plugins.html) 给出的 toolbar 引用链与当前打开的工具窗口值得对照；本轮审计尚未找到确定的强引用链，未据此盲目修改代码。

在正常重启后的进程中新增仅用于验收的 `HttpAcceptanceController`，从 Doc View 方法菜单分别生成三个 `.http` 文件。先保存原始生成内容，再仅通过原生编辑器将 `{{host}}` 替换为本机回环地址，并显式点击 HTTP Client 的 Run All Requests：

| 场景 | 原生执行与接收端断言 | 结果 |
| --- | --- | --- |
| A01 GET | `/native/users/42`，解码 Query 为 `q=a&b 空格`，`X-Trace=native-get`，无 body | HTTP 200；通过 |
| A02 JSON POST | `/native/json?dryRun=true`，`X-Trace=native-json`；恰好一个 `application/json`，body 为 `displayName=""`、`age=0` | HTTP 200；通过 |
| A03 表单 POST | `/native/form` 无 Query，`X-Trace=native-form`；恰好一个 `application/x-www-form-urlencoded`，解码 body 为 `name=Alice & Bob 空格` | HTTP 200；通过 |

接收脚本只监听 `127.0.0.1:53070`，每次点击 Run 前接收数依次为 0、1、2，最终恰好 3 次；原生客户端把 GET 的空格编码规范化为 `+`，服务端解码值保持正确。没有使用 curl 代替客户端，没有发送到外部服务。原始请求、接收记录及断言结果在 `desktop-followup/A01-generated.http` 至 `A03-generated.http`、`loopback-received.jsonl` 和 `http-execution-result.json`；验收后已停止回环接收服务。

跨应用复验再次观察到：CUA 能将 Finder 窗口 Raise 并操作图标/列表视图，但桌面菜单栏仍为 IntelliJ IDEA，参数弹窗仍覆盖其一部分。CUA 的后台输入及 Raise 未证明发生正常 OS 应用激活切换，故仍不标为通过或认定新根因。当前将参数弹窗保留，等待维护者手动激活测试 Finder 窗口后再采集结果；等待期间停止桌面操作与截图。

后续顺序：先完成真实应用切换，再关闭所有 Doc View 弹窗/编辑器及工具窗口做动态卸载对照；若能复现，按单一 surface 逐项恢复定位引用来源，修复后用新包复验。热卸载、完整跨应用/菜单矩阵及其余原生门槛继续未通过，原任务 checkbox 不变。生产代码、版本与包未改变；没有 commit、push、tag、Release、Marketplace 发布或正式 MCP 实现。

本次交接前再次核对：PID `2015` 的 config/system/plugins/log 均属于本轮隔离 profile，安装 JAR hash 不变；原工作区 584 项与 HEAD、生产/构建 230 项、原 fixture 15 项均未改变。回环进程 `1620` 已退出。三个 change 的 strict validate、7 份文档 106 个相对链接及 `git diff --check` 通过，结果保存于 `desktop-followup/post-activation-checks.json`；没有重跑自动测试，也未关闭仍待真实切换观察的参数弹窗。

## 同日补充：真实 Finder 激活与热卸载引用链

维护者回复“已切到 Finder”后，先采集已授权的桌面图像，再操作窗口。`desktop-followup/param-user-activated-finder.png` 中菜单栏为“访达”、Finder 窗口处于激活状态，参数弹窗不再覆盖 Finder；后台 AX 仍能读到参数表及 Pin 按钮，说明弹窗仍在后台，没有关闭。**参数弹窗 Pin 开启、真实 IDEA → Finder 激活切换这一场景通过**。此前仅用 CUA 后台输入或 Raise 的遮挡图像不能证明正常应用切换缺陷。Pin 关闭、预览两种 Pin 状态、返回后的完整矩阵仍未通过；没有将这一项扩大为全部弹窗验收通过。

之后 Escape 关闭参数弹窗，Close All Tabs 关闭编辑器，并隐藏 Doc View 工具窗口；未写回源码。原生 Settings → Plugins 禁用 Doc View，PID `2015` 于 `02:32:03.984` 再次报告 ClassLoader 未释放，live histogram 仍有 DocViewWindowPanel 1、MethodNode 60、ClassNode 4。这说明问题不只发生在激活许可时。证据在 `desktop-followup/unload-controls/hidden-*`。

为避免在已激活 profile 中生成完整堆快照，另建 `unload-clean/`：独立 config/system/plugins/log、`user.home` 和 HOME，仅复制虚构源码、测试 SDK 定义和同一 final-v2 JAR；没有复制账户或许可配置。UI 为 Unlock Ultimate。app 仍为 IU `263.7307.200`，运行时为 `JBR-25.0.4.1+1-635.79-nomod`，诊断 VM 开关仅用于本次取证，不改变生产基线或包。Junie/Air 保留，未发送代理对话，MCP Disabled。

| 对照 | 实际结果 | 解释边界 |
| --- | --- | --- |
| PID 5340：使用目录，不打开参数/预览/HTTP | 目录扫描 60 个方法，关闭 welcome 编辑器后禁用插件；仍保留 37 行 Doc View 实例类型。官方分析报告 Local JNI → AccessibleJTreeNode → StructureTreeModel.Node → ClassNode → PluginClassLoader | 已取得目录无障碍引用链；不能由此排除其他场景的不同根 |
| PID 5983：目录显示后仅截图，直接进入插件设置，未主动请求目录 AX 树 | 卸载仍失败，live histogram 有 36 行 Doc View 实例类型 | CUA 仍用于其他界面操作，未证明底层工具或其他 AX 客户端完全没有读取该树；不作为严格的无 AX 对照 |
| PID 6147：不打开任何 Doc View 界面 | 卸载前仅有 ApplicationSettings 2、StartupNotification 1、ToolWindowFactory 1；卸载后 Doc View 实例行数为 0，但仍有未卸载告警，`VM.class_hierarchy` 仍能找到 ApplicationSettings | 对象实例归零不等于类加载器回收；官方第二份报告发现图标缓存引用链，不能把全部热卸载失败都归于无障碍树 |

在未登录的诊断 profile 中，通过 IDEA 原生 **Save Memory Snapshot** 及自动强引用分析获得两份报告（`unload-clean/official-unload-roots.txt`；原日志时间分别为 `02:38:00.958`、`02:46:51.287`）：

```text
Local JNI
  → JTree.AccessibleJTree.AccessibleJTreeNode.obj
  → StructureTreeModel.Node.userObject
  → Doc View ClassNode
  → PluginClassLoader

Global JNI → 平台 PathClassLoader → IconUtilKt.iconToDisabledIcon
  → Caffeine cache → CachedImageIcon（缓存 key）
  → ImageDataByPathResourceLoader（originalLoader）
  → PluginClassLoader
```

第二条链的官方摘要将 Class 后的字段显示为 `classLoader`，该摘要未给出具体 owner。随后只读取诊断堆的类型、静态字段和平台缓存引用边，实际定位为 `com.intellij.ui.icons.IconUtilKt.iconToDisabledIcon`，匹配缓存 key 的 `originalLoader.classLoader` 与 `icons.DocViewIcons` 的定义 ClassLoader；详见 `unload-clean/icon-cache-owner-matches.json`。这是禁用图标缓存，并非业务任务或 Doc View 服务对象。未读取业务字符串或凭据值。完整快照仅留在 ignored 的隔离证据目录，没有上传、纳入仓库或发布包；没有在已激活 profile 取堆快照。

修复判断以已观察到的引用链为准：

- 工具窗口 content 的 disposer 已正确绑定面板；`Content(..., false)` 不会跳过显式 `setDisposer(panel)`。关闭 UI 后仍失败，不能靠重复注册 disposer 宣称解决。
- 精确 SDK 的 `StructureTreeModel.dispose()` 会清根、parent/children，但不会清旧节点的 userObject；`AsyncTreeModel.dispose()` 移除模型 listener。替换当前 JTree model 不会解除已被原生 AX 对象持有的旧节点。没有发现受支持的公开 API 能直接解除这条 JNI 引用；未加入反射、Internal API 或破坏无障碍支持的清理分支。
- [JetBrains Runtime #646](https://github.com/JetBrains/JetBrainsRuntime/issues/646) 报告了同型 macOS Accessibility / Local JNI 卸载保留链。2026-11-08 读取时为 Open、0 comments、无关联修复；这是上游仓库中的用户报告，不能写成厂商已确认本机精确 build 的根因或已提供修复。
- 插件的三个品牌图标均用常规 `IconLoader.getIcon(path, DocViewIcons.class)`，没有自建全局图标缓存。精确 [DynamicPluginsCachesCleanup 源码](https://github.com/JetBrains/intellij-community/blob/idea/263.7307.200/platform/platform-impl/src/com/intellij/ide/plugins/DynamicPluginsCachesCleanup.kt) 已在卸载中调用公开 `IconLoader.detachClassLoader`，不能把重复调用它当作插件漏清理的修复。
- [iconUtil.kt](https://github.com/JetBrains/intellij-community/blob/idea/263.7307.200/platform/util/ui/src/com/intellij/ui/icons/iconUtil.kt) 中 `iconToDisabledIcon` 是强 key、10 分钟访问到期的缓存，并注册了 transform cache cleaner。[IconLoader.kt](https://github.com/JetBrains/intellij-community/blob/idea/263.7307.200/platform/util/ui/src/com/intellij/openapi/util/IconLoader.kt) 的公开 `clearCache()` 会触发该 cleaner；`detachClassLoader()` 则处理另一份 `iconCache`。因此存在公开清理路径，但尚未证明插件卸载时应在哪个生命周期位置调用、是否会被随后 UI 更新重新填入；没有加入全局清理生产补丁、反射或 TestOnly 调用。

本轮未修改生产代码、版本或最低 build；没有可宣称已修复并复验通过的热卸载补丁。热卸载、完整弹窗/菜单及其余原生门槛保持未通过，R2/4.2 和其他未完成 checkbox 不变，正式 MCP 与发布继续等待。

另用同一未登录 profile 启动 PID `6996`，仍不打开 Doc View 界面。先原生禁用插件，等待卸载失败告警，live GC 后 `VM.class_hierarchy` 仍能找到 ApplicationSettings；再通过原生 **Find Action → Theme → Islands Light** 执行主题变换。随后再次 live GC 与同一类层级查询，原先滞留的 ApplicationSettings 已不再出现。原来的 Restart advised 通知仍保留，不把通知变化当成回收证据。此结果与禁用图标缓存引用链和 transform cleaner 源码一致，但主题变化涉及更多平台操作，**不是对 `IconLoader.clearCache()` 单独调用的隔离验证，也不是无障碍 JNI 路径或完整热卸载已修复**。证据在 `unload-clean/icon-transform-control.json`、`icon-transform-before/after-class.txt` 及对应 histogram。

收尾：主隔离 profile 和诊断 profile 的 Doc View 均已恢复启用；PID `2015/5340/5983/6147/6996` 已退出，诊断实例原生 Exit 后由进程检查确认。最后一次对照的 Islands Light 已恢复为原 Islands Dark，UI 读回确认。主 profile 的激活配置保留，普通 IDEA 与 Junie 未改动。Finder 临时窗口恢复分栏并关闭；原始日志、两份堆快照和对照结果保留供后续定位。

文档与保留检查：三个 change 的 strict validate、7 份文档的 106 个相对文件链接及 `git diff --check` 通过。原工作区 584 项与 HEAD、整合生产/构建输入 230 项、原 fixture 15 项 hash 均未变，两个隔离 profile 的安装 JAR 与 final-v2 一致。检查结果见 `desktop-followup/finder-unload-checks.json`；诊断摘要见 `unload-clean/result-summary.json`。本轮没有重跑构建、自动测试或 Verifier。


## 后续新修复包

本记录以上均为 final-v2 包及其诊断对照。后续已经验证公开 `IconLoader.clearCache()` 在自身 `beforePluginUnload` 的调用时机并加入生产修复；新包无 UI 卸载通过，打开目录后的 JNI 卸载仍失败。准确包身份、修复后测试/Verifier 和剩余弹窗边界见[卸载 API 续验](verification-2026-11-08-unload-api.md)，不以历史“未修改生产代码”描述当前状态。
